// Redis 适配器：通过 redis-cli 执行。
//
// 除普通 Redis 命令外，额外支持 count "<pattern>"：用 --scan 统计匹配的 key 数，
// 并可用 config.redisPrefix 自动补全项目 key 前缀。

import { spawn } from 'node:child_process';

function tokenize(text) {
  const tokens = [];
  const re = /"((?:\\.|[^"\\])*)"|'([^']*)'|(\S+)/g;
  let match;
  while ((match = re.exec(text)) !== null) {
    if (match[1] !== undefined) {
      tokens.push(match[1].replace(/\\(["\\nrt])/g, (all, ch) => ({ '"': '"', '\\': '\\', n: '\n', r: '\r', t: '\t' }[ch])));
    } else if (match[2] !== undefined) {
      tokens.push(match[2]);
    } else {
      tokens.push(match[3]);
    }
  }
  return tokens;
}

function settingsOf(config) {
  const raw = config.redis;
  if (raw === undefined && config.redisUrl === undefined) {
    const error = new Error('redis 步骤缺少连接配置：未找到 config.redis 或 config.redisUrl');
    error.hint = '在 scenario.config 或 --config 中配置 redis: { host, port, db, passwordEnv }，或 redisUrl: redis://...。';
    throw error;
  }
  const settings = typeof raw === 'string' ? { url: raw } : { ...(raw || {}) };
  settings.url = settings.url || config.redisUrl;
  if (!settings.url && settings.host === undefined) {
    const error = new Error('redis 配置不完整：需要 url 或 host');
    error.hint = '例如 redis: { host: 127.0.0.1, port: 6379, db: 0 }。';
    throw error;
  }
  return settings;
}

function buildBase(settings) {
  const command = settings.command || 'redis-cli';
  const args = [];
  if (settings.url) args.push('-u', String(settings.url));
  else {
    if (settings.host) args.push('-h', String(settings.host));
    if (settings.port) args.push('-p', String(settings.port));
    if (settings.db !== undefined && settings.db !== null) args.push('-n', String(settings.db));
    if (settings.user) args.push('--user', String(settings.user));
  }
  if (settings.tls === true) args.push('--tls');
  const env = { ...process.env };
  if (settings.passwordEnv) {
    const password = process.env[settings.passwordEnv];
    if (password === undefined) {
      const error = new Error(`redis 密码环境变量 ${settings.passwordEnv} 未设置`);
      error.hint = `在运行前 export ${settings.passwordEnv}=...，或改用 redis.password（不推荐把密码写进文件）。`;
      throw error;
    }
    env.REDISCLI_AUTH = password;
  } else if (settings.password !== undefined) {
    env.REDISCLI_AUTH = String(settings.password);
  }
  return { command, args, env };
}

function timeoutOf(config) {
  const raw = config.redisTimeoutMs ?? 10000;
  const value = Number(raw);
  if (!Number.isFinite(value) || value <= 0) {
    const error = new Error(`config.redisTimeoutMs 必须是正数，实际是 ${JSON.stringify(raw)}`);
    error.hint = '在 scenario.config 或 --config 中设置 redisTimeoutMs（毫秒）。';
    throw error;
  }
  return value;
}

function execute(command, args, env, timeoutMs) {
  return new Promise((resolve, reject) => {
    let child;
    try {
      child = spawn(command, args, { env, stdio: ['ignore', 'pipe', 'pipe'] });
    } catch (error) {
      reject(error);
      return;
    }
    let stdout = '';
    let stderr = '';
    let timedOut = false;
    const timer = setTimeout(() => {
      timedOut = true;
      child.kill('SIGKILL');
    }, timeoutMs);
    child.stdout.setEncoding('utf8');
    child.stderr.setEncoding('utf8');
    child.stdout.on('data', (chunk) => { stdout += chunk; });
    child.stderr.on('data', (chunk) => { stderr += chunk; });
    child.on('error', (error) => {
      clearTimeout(timer);
      if (error.code === 'ENOENT') {
        const failure = new Error(`无法启动 redis-cli（${command}）：找不到命令`);
        failure.hint = '安装 Redis 客户端并确保 redis-cli 在 PATH 中，或用 config.redis.command 指定绝对路径。';
        failure.actual = { command, error: error.message };
        reject(failure);
        return;
      }
      const failure = new Error(`启动 redis-cli 失败：${error.message}`);
      failure.actual = { command, error: error.message };
      reject(failure);
    });
    child.on('close', (code, signal) => {
      clearTimeout(timer);
      if (timedOut) {
        const failure = new Error(`redis 执行超时（${timeoutMs}ms）`);
        failure.hint = '检查 Redis 连通性和网络；可调整 config.redisTimeoutMs。';
        failure.actual = { exitCode: code, signal, stdout, stderr };
        reject(failure);
        return;
      }
      resolve({ code, signal, stdout, stderr });
    });
  });
}

function assertRedisSuccess(result) {
  if (result.code !== 0) {
    const detail = result.stderr.trim() || result.stdout.trim() || '(无输出)';
    const failure = new Error(`redis 执行失败（redis-cli 退出码 ${result.code}）：${detail}`);
    failure.hint = '检查 config.redis/redisUrl 连接信息、认证和 Redis 服务状态。';
    failure.actual = { exitCode: result.code, stdout: result.stdout, stderr: result.stderr };
    throw failure;
  }
  const output = `${result.stdout}\n${result.stderr}`;
  if (/(^|\n)\(error\)/.test(output)) {
    const detail = output.trim();
    const failure = new Error(`redis 返回错误：${detail}`);
    failure.hint = '检查 Redis 命令、认证和 key 前缀。';
    failure.actual = { exitCode: result.code, stdout: result.stdout, stderr: result.stderr };
    throw failure;
  }
}

function compact(value, limit = 60) {
  const text = String(value).replace(/\s+/g, ' ').trim();
  return text.length > limit ? `${text.slice(0, limit)}…` : text;
}

/**
 * 执行 redis 步骤。
 * @param {Record<string, any>} step 步骤
 * @param {{config: Record<string, any>}} context 执行上下文
 * @returns {Promise<{display: string, actual: Record<string, any>, summary: string}>}
 */
export async function runRedis(step, context) {
  const action = String(step.redis).trim();
  const tokens = tokenize(action);
  if (tokens.length === 0) {
    const error = new Error('redis 步骤的命令为空');
    error.hint = '例如 "count \\"session:*\\"" 或 "get session:1"。';
    throw error;
  }
  const settings = settingsOf(context.config);
  const { command, args: baseArgs, env } = buildBase(settings);
  const timeoutMs = timeoutOf(context.config);

  if (tokens[0].toLowerCase() === 'count') {
    if (tokens.length !== 2) {
      const error = new Error(`redis count 需要且只需要一个 pattern，实际参数：${tokens.slice(1).join(' ') || '<无>'}`);
      error.hint = '写法：redis: count "session:*"；如配置了 redisPrefix，工具会自动补全前缀。';
      throw error;
    }
    let pattern = tokens[1];
    const prefix = context.config.redisPrefix ? String(context.config.redisPrefix).replace(/:+$/, '') : '';
    if (prefix && pattern !== prefix && !pattern.startsWith(`${prefix}:`)) pattern = `${prefix}:${pattern}`;
    const result = await execute(command, [...baseArgs, '--scan', '--pattern', pattern], env, timeoutMs);
    assertRedisSuccess(result);
    const count = result.stdout.split(/\r?\n/).filter((line) => line !== '').length;
    return {
      display: `redis count "${pattern}"`,
      actual: {
        exitCode: result.code,
        stdout: result.stdout,
        stderr: result.stderr,
        count,
        value: count,
        content: result.stdout,
      },
      summary: String(count),
    };
  }

  const result = await execute(command, [...baseArgs, ...tokens], env, timeoutMs);
  assertRedisSuccess(result);
  const value = result.stdout.trim();
  return {
    display: `redis ${compact(action, 70)}`,
    actual: {
      exitCode: result.code,
      stdout: result.stdout,
      stderr: result.stderr,
      value,
      content: result.stdout,
    },
    summary: value === '' ? 'empty' : compact(value, 40),
  };
}
