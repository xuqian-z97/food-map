// 任意 shell 命令适配器：只使用 node:child_process，不引入 shelljs 等依赖。

import { spawn } from 'node:child_process';
import { resolve } from 'node:path';

function timeoutOf(config) {
  const raw = config.commandTimeoutMs ?? 30000;
  const value = Number(raw);
  if (!Number.isFinite(value) || value <= 0) {
    const error = new Error(`config.commandTimeoutMs 必须是正数，实际是 ${JSON.stringify(raw)}`);
    error.hint = '在 scenario.config 或 --config 中设置 commandTimeoutMs（毫秒）。';
    throw error;
  }
  return value;
}

function shellOf(config) {
  if (config.shell) return { command: String(config.shell), windows: process.platform === 'win32' };
  return process.platform === 'win32'
    ? { command: 'cmd.exe', windows: true }
    : { command: '/bin/sh', windows: false };
}

function workingDirectoryOf(config, scenarioDir) {
  const base = scenarioDir || process.cwd();
  if (!config.cwd) return base;
  return resolve(base, String(config.cwd));
}

function execute(shell, commandText, cwd, timeoutMs) {
  const args = shell.windows
    ? ['/d', '/s', '/c', commandText]
    : ['-c', commandText];
  return new Promise((resolvePromise, reject) => {
    let child;
    try {
      child = spawn(shell.command, args, { cwd, env: process.env, stdio: ['ignore', 'pipe', 'pipe'] });
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
      const failure = new Error(`启动 shell 失败（${shell.command}）：${error.message}`);
      failure.hint = '检查 config.shell 是否可执行，以及 config.cwd 是否存在。';
      failure.actual = { shell: shell.command, cwd, error: error.message };
      reject(failure);
    });
    child.on('close', (code, signal) => {
      clearTimeout(timer);
      if (timedOut) {
        const failure = new Error(`command 执行超时（${timeoutMs}ms）`);
        failure.hint = '检查命令是否阻塞；可调整 config.commandTimeoutMs，或给命令自身加超时。';
        failure.actual = { exitCode: code, signal, stdout, stderr, cwd };
        reject(failure);
        return;
      }
      resolvePromise({ code, signal, stdout, stderr, cwd });
    });
  });
}

function compact(value, limit = 90) {
  const text = String(value).replace(/\s+/g, ' ').trim();
  return text.length > limit ? `${text.slice(0, limit)}…` : text;
}

/**
 * 执行 command 步骤。
 * @param {Record<string, any>} step 步骤
 * @param {{config: Record<string, any>, scenario: Record<string, any>}} context 执行上下文
 * @returns {Promise<{display: string, actual: Record<string, any>, summary: string}>}
 */
export async function runCommand(step, context) {
  const commandText = String(step.command);
  const shell = shellOf(context.config);
  const cwd = workingDirectoryOf(context.config, context.scenario && context.scenario.dir);
  const timeoutMs = timeoutOf(context.config);
  const result = await execute(shell, commandText, cwd, timeoutMs);
  return {
    display: `command ${compact(commandText)}`,
    actual: {
      exitCode: result.code,
      signal: result.signal,
      stdout: result.stdout,
      stderr: result.stderr,
      value: result.stdout.trim(),
      content: result.stdout,
      cwd,
    },
    summary: result.code === null ? `signal ${result.signal || 'unknown'}` : `exit ${result.code}`,
  };
}
