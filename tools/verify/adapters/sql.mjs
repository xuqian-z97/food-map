// SQL 适配器：通过 psql 命令行执行，返回首列/单值文本。
//
// 不内置数据库驱动，所有连接信息都来自 scenario.config 或 --config。

import { spawn } from 'node:child_process';

function settingsOf(config) {
  const raw = config.psql;
  if (raw === undefined) return { url: config.databaseUrl };
  if (typeof raw === 'string') return { url: raw };
  if (raw === null || typeof raw !== 'object' || Array.isArray(raw)) {
    const error = new Error(`config.psql 必须是字符串或映射，实际是 ${JSON.stringify(raw)}`);
    error.hint = '例如 psql: { host, port, user, database, passwordEnv } 或 databaseUrl: postgresql://...';
    throw error;
  }
  return { ...raw, url: raw.url || config.databaseUrl };
}

function buildCommand(settings, sql) {
  const command = settings.command || 'psql';
  const hasTarget = Boolean(settings.url || settings.database || settings.host);
  if (!hasTarget) {
    const error = new Error('sql 步骤缺少数据库连接配置：未找到 config.databaseUrl、config.psql.url、config.psql.database 或 config.psql.host');
    error.hint = '在 scenario.config 或 --config 中配置 databaseUrl，或 psql: { host, port, user, database, passwordEnv }；本工具不会连接默认数据库。';
    throw error;
  }
  const args = ['-X', '-q', '-A', '-t', '-v', 'ON_ERROR_STOP=1'];
  if (settings.url) {
    args.push('--dbname', String(settings.url));
  } else {
    if (settings.host) args.push('--host', String(settings.host));
    if (settings.port) args.push('--port', String(settings.port));
    if (settings.user) args.push('--username', String(settings.user));
    if (settings.database) args.push('--dbname', String(settings.database));
  }
  if (Array.isArray(settings.args)) args.push(...settings.args.map(String));
  args.push('-c', sql);
  return { command, args };
}

function buildEnv(settings) {
  const env = { ...process.env };
  if (settings.passwordEnv) {
    const value = process.env[settings.passwordEnv];
    if (value === undefined) {
      const error = new Error(`sql 密码环境变量 ${settings.passwordEnv} 未设置`);
      error.hint = `在运行前 export ${settings.passwordEnv}=...，或改用 psql.password（不推荐把密码写进文件）。`;
      throw error;
    }
    env.PGPASSWORD = value;
  } else if (settings.password !== undefined) {
    env.PGPASSWORD = String(settings.password);
  }
  return env;
}

function timeoutOf(config) {
  const raw = config.sqlTimeoutMs ?? 15000;
  const value = Number(raw);
  if (!Number.isFinite(value) || value <= 0) {
    const error = new Error(`config.sqlTimeoutMs 必须是正数，实际是 ${JSON.stringify(raw)}`);
    error.hint = '在 scenario.config 或 --config 中设置 sqlTimeoutMs（毫秒）。';
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
        const failure = new Error(`无法启动 psql（${command}）：找不到命令`);
        failure.hint = '安装 PostgreSQL 客户端并确保 psql 在 PATH 中，或用 config.psql.command 指定绝对路径。';
        failure.actual = { command, error: error.message };
        reject(failure);
        return;
      }
      const failure = new Error(`启动 psql 失败：${error.message}`);
      failure.actual = { command, error: error.message };
      reject(failure);
    });
    child.on('close', (code, signal) => {
      clearTimeout(timer);
      if (timedOut) {
        const failure = new Error(`sql 执行超时（${timeoutMs}ms）`);
        failure.hint = '检查数据库连通性和 SQL 是否被锁；可调整 config.sqlTimeoutMs。';
        failure.actual = { exitCode: code, signal, stdout, stderr };
        reject(failure);
        return;
      }
      resolve({ code, signal, stdout, stderr });
    });
  });
}

function compact(value, limit = 80) {
  const text = String(value).replace(/\s+/g, ' ').trim();
  return text.length > limit ? `${text.slice(0, limit)}…` : text;
}

/**
 * 执行 sql 步骤。
 * @param {Record<string, any>} step 步骤
 * @param {{config: Record<string, any>}} context 执行上下文
 * @returns {Promise<{display: string, actual: Record<string, any>, summary: string}>}
 */
export async function runSql(step, context) {
  const sql = String(step.sql);
  const settings = settingsOf(context.config);
  const { command, args } = buildCommand(settings, sql);
  const env = buildEnv(settings);
  const timeoutMs = timeoutOf(context.config);
  const result = await execute(command, args, env, timeoutMs);
  if (result.code !== 0) {
    const detail = result.stderr.trim() || result.stdout.trim() || '(无输出)';
    const failure = new Error(`sql 执行失败（psql 退出码 ${result.code}）：${detail}`);
    failure.hint = '检查 config.databaseUrl/psql 连接参数、数据库权限和 SQL 语法。';
    failure.actual = { exitCode: result.code, stdout: result.stdout, stderr: result.stderr };
    throw failure;
  }
  const value = result.stdout.trim();
  return {
    display: `sql ${compact(sql)}`,
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
