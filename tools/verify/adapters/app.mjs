// iOS/Android 设备适配器。
//
// iOS 通过 xcrun simctl 操作模拟器，Android 通过 adb 操作设备/模拟器。
// app 动作只负责 launch / terminate / readlog：它不能点击 UI，也不能理解业务页面。

import { spawn } from 'node:child_process';
import { readFile } from 'node:fs/promises';
import { isAbsolute, resolve, sep } from 'node:path';

function tokenize(text) {
  const tokens = [];
  const re = /"((?:\\.|[^"\\])*)"|'([^']*)'|(\S+)/g;
  let match;
  while ((match = re.exec(text)) !== null) {
    if (match[1] !== undefined) {
      tokens.push(match[1].replace(/\\(["\\])/g, '$1'));
    } else if (match[2] !== undefined) {
      tokens.push(match[2]);
    } else {
      tokens.push(match[3]);
    }
  }
  return tokens;
}

function timeoutOf(config) {
  const raw = config.appTimeoutMs ?? 30000;
  const value = Number(raw);
  if (!Number.isFinite(value) || value <= 0) {
    const error = new Error(`config.appTimeoutMs 必须是正数，实际是 ${JSON.stringify(raw)}`);
    error.hint = '在 scenario.config 或 --config 中设置 appTimeoutMs（毫秒）。';
    throw error;
  }
  return value;
}

function execute(command, args, options = {}) {
  const { env = process.env, timeoutMs = 30000 } = options;
  return new Promise((resolvePromise, reject) => {
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
        const failure = new Error(`无法启动 ${command}：找不到命令`);
        failure.hint = 'iOS 需要 Xcode 命令行工具（xcode-select --install）；Android 需要 platform-tools。'
          + ' 也可以分别用 config.xcrunPath / config.adbCommand 指定可执行文件路径。';
        failure.actual = { command, args, error: error.message };
        reject(failure);
        return;
      }
      const failure = new Error(`启动 ${command} 失败：${error.message}`);
      failure.actual = { command, args, error: error.message };
      reject(failure);
    });
    child.on('close', (code, signal) => {
      clearTimeout(timer);
      if (timedOut) {
        const failure = new Error(`设备命令超时（${timeoutMs}ms）：${command} ${args.join(' ')}`);
        failure.hint = '检查模拟器/设备是否响应；可调整 config.appTimeoutMs。';
        failure.actual = { command, args, exitCode: code, signal, stdout, stderr };
        reject(failure);
        return;
      }
      resolvePromise({ code, signal, stdout, stderr, command, args });
    });
  });
}

function parseAction(step, platform) {
  const tokens = tokenize(String(step.app).trim());
  const action = (tokens[0] || '').toLowerCase();
  if (!['launch', 'terminate', 'readlog'].includes(action)) {
    const error = new Error(`app 动作必须是 launch / terminate / readlog，实际是 ${JSON.stringify(tokens[0] || '')}`);
    error.hint = '写法：app: launch <udid> <bundleId>；app: readlog <udid> <bundleId> <logPath>。';
    throw error;
  }
  if (tokens.length < 3) {
    const error = new Error(`app ${action} 参数不足：需要 <udid> <bundleId>${action === 'readlog' ? ' <logPath>' : ''}`);
    error.actual = { platform, tokens };
    throw error;
  }
  const udid = tokens[1];
  const bundleId = tokens[2];
  const logPath = tokens.slice(3).join(' ');
  if (action !== 'readlog' && tokens.length > 3) {
    const error = new Error(`app ${action} 参数过多：${tokens.slice(3).join(' ')}`);
    error.hint = `写法：app: ${action} <udid> <bundleId>。`;
    throw error;
  }
  if (action === 'readlog' && logPath === '') {
    const error = new Error('app readlog 缺少日志路径');
    error.hint = platform === 'ios'
      ? 'iOS 示例：app: readlog <udid> <bundleId> Documents/probe.log；路径相对 App data 容器。'
      : 'Android 示例：app: readlog <udid> <bundleId> files/probe.log；路径相对 App 私有目录。';
    throw error;
  }
  return { action, udid, bundleId, logPath, tokens };
}

function assertSuccess(result, action) {
  if (result.code === 0) return;
  const detail = result.stderr.trim() || result.stdout.trim() || '(无输出)';
  const failure = new Error(`app ${action} 失败（退出码 ${result.code}）：${detail}`);
  failure.hint = '检查 UDID/设备是否已启动、bundleId 是否已安装、路径是否正确。';
  failure.actual = {
    command: result.command,
    args: result.args,
    exitCode: result.code,
    stdout: result.stdout,
    stderr: result.stderr,
  };
  throw failure;
}

function launchEnv(step, platform) {
  if (!step.env) return process.env;
  if (platform !== 'ios') {
    const error = new Error('app.env 目前只支持 iOS launch（通过 SIMCTL_CHILD_* 注入），Android 无法给已安装应用注入环境变量');
    error.hint = '删除 app.env，改为让 App 通过自身调试开关/参数写探针日志；或把环境变量逻辑做进构建配置。';
    throw error;
  }
  const env = { ...process.env };
  for (const [key, value] of Object.entries(step.env)) env[`SIMCTL_CHILD_${key}`] = String(value);
  return env;
}

async function runIos(step, context, parsed, timeoutMs) {
  const { action, udid, bundleId, logPath } = parsed;
  const xcrun = context.config.xcrunPath || 'xcrun';
  if (action === 'launch') {
    const result = await execute(xcrun, ['simctl', 'launch', udid, bundleId], {
      env: launchEnv(step, 'ios'),
      timeoutMs,
    });
    assertSuccess(result, action);
    return {
      display: `app launch ${udid} ${bundleId}`,
      actual: { exitCode: result.code, stdout: result.stdout, stderr: result.stderr, value: result.stdout.trim(), udid, bundleId, platform: 'ios' },
      summary: 'launched',
    };
  }
  if (action === 'terminate') {
    if (step.env) throw new Error('app.env 只能用于 launch，terminate 不接受 env');
    const result = await execute(xcrun, ['simctl', 'terminate', udid, bundleId], { timeoutMs });
    assertSuccess(result, action);
    return {
      display: `app terminate ${udid} ${bundleId}`,
      actual: { exitCode: result.code, stdout: result.stdout, stderr: result.stderr, value: result.stdout.trim(), udid, bundleId, platform: 'ios' },
      summary: 'terminated',
    };
  }

  if (step.env) throw new Error('app.env 只能用于 launch，readlog 不接受 env');
  const containerResult = await execute(xcrun, ['simctl', 'get_app_container', udid, bundleId, 'data'], { timeoutMs });
  assertSuccess(containerResult, 'get_app_container');
  const container = containerResult.stdout.trim();
  if (container === '') {
    const error = new Error(`app readlog 无法获取 App data 容器：${udid} ${bundleId}`);
    error.hint = '确认模拟器已启动、App 已安装，且 bundleId 正确。';
    throw error;
  }
  const file = isAbsolute(logPath) ? logPath : resolve(container, logPath);
  if (file !== container && !file.startsWith(`${container}${sep}`)) {
    const error = new Error(`app readlog 路径越出 App data 容器：${logPath}`);
    error.hint = '使用相对 App data 容器的路径，例如 Documents/probe.log。';
    error.actual = { container, logPath };
    throw error;
  }
  let content;
  try {
    content = await readFile(file, 'utf8');
  } catch (error) {
    const failure = new Error(`app readlog 读取失败：${file}：${error.message}`);
    failure.hint = '确认 App 已写入探针日志，且路径相对 App data 容器（如 Documents/probe.log）。';
    failure.actual = { container, file, error: error.message };
    throw failure;
  }
  return {
    display: `app readlog ${udid} ${bundleId} ${logPath}`,
    actual: {
      platform: 'ios',
      udid,
      bundleId,
      container,
      files: [file],
      content,
      value: content,
      bytes: Buffer.byteLength(content, 'utf8'),
    },
    summary: `${Buffer.byteLength(content, 'utf8')} bytes`,
  };
}

async function runAndroid(step, context, parsed, timeoutMs) {
  const { action, udid, bundleId, logPath } = parsed;
  const adb = context.config.adbCommand || 'adb';
  if (action === 'launch') {
    if (step.env) throw new Error('app.env 目前只支持 iOS launch，Android 无法给已安装应用注入环境变量');
    const result = await execute(adb, ['-s', udid, 'shell', 'monkey', '-p', bundleId, '-c', 'android.intent.category.LAUNCHER', '1'], { timeoutMs });
    assertSuccess(result, action);
    return {
      display: `app launch ${udid} ${bundleId}`,
      actual: { exitCode: result.code, stdout: result.stdout, stderr: result.stderr, value: result.stdout.trim(), udid, bundleId, platform: 'android' },
      summary: 'launched',
    };
  }
  if (action === 'terminate') {
    if (step.env) throw new Error('app.env 只能用于 launch，terminate 不接受 env');
    const result = await execute(adb, ['-s', udid, 'shell', 'am', 'force-stop', bundleId], { timeoutMs });
    assertSuccess(result, action);
    return {
      display: `app terminate ${udid} ${bundleId}`,
      actual: { exitCode: result.code, stdout: result.stdout, stderr: result.stderr, value: result.stdout.trim(), udid, bundleId, platform: 'android' },
      summary: 'terminated',
    };
  }

  if (step.env) throw new Error('app.env 只能用于 launch，readlog 不接受 env');
  const result = await execute(adb, ['-s', udid, 'exec-out', 'run-as', bundleId, 'cat', logPath], { timeoutMs });
  assertSuccess(result, 'readlog');
  return {
    display: `app readlog ${udid} ${bundleId} ${logPath}`,
    actual: {
      platform: 'android',
      udid,
      bundleId,
      files: [logPath],
      content: result.stdout,
      value: result.stdout,
      bytes: Buffer.byteLength(result.stdout, 'utf8'),
      stdout: result.stdout,
      stderr: result.stderr,
      exitCode: result.code,
    },
    summary: `${Buffer.byteLength(result.stdout, 'utf8')} bytes`,
  };
}

/**
 * 执行 app 步骤。
 * @param {Record<string, any>} step 步骤
 * @param {{config: Record<string, any>}} context 执行上下文
 * @returns {Promise<{display: string, actual: Record<string, any>, summary: string}>}
 */
export async function runApp(step, context) {
  const platform = String(context.config.platform || 'ios').toLowerCase();
  if (!['ios', 'android'].includes(platform)) {
    const error = new Error(`config.platform 必须是 ios 或 android，实际是 ${JSON.stringify(context.config.platform)}`);
    error.hint = '在 scenario.config 或 --config 中设置 platform: ios 或 platform: android。';
    throw error;
  }
  const parsed = parseAction(step, platform);
  const timeoutMs = timeoutOf(context.config);
  return platform === 'ios'
    ? runIos(step, context, parsed, timeoutMs)
    : runAndroid(step, context, parsed, timeoutMs);
}
