// xcode 适配器：把 xcodebuild 的 build / test / install 变成可断言的步骤。
//
// 项目差异（scheme、工程路径、模拟器 UDID）全部来自 config；核心代码不写死任何项目名。

import { spawn } from 'node:child_process';
import { mkdtemp, readFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';

const VERBS = new Set(['build', 'test', 'install']);

function configOf(context) {
  const raw = context.config.xcode;
  if (!raw || typeof raw !== 'object') {
    const error = new Error('xcode 步骤缺少 config.xcode 配置');
    error.hint = '在 scenario.config 中设置 xcode: { project, scheme, udid } 等字段。';
    throw error;
  }
  return raw;
}

function timeoutOf(config, verb) {
  const candidate = config.timeoutMs ?? (verb === 'test' ? 900000 : 600000);
  const value = Number(candidate);
  if (!Number.isFinite(value) || value <= 0) {
    const error = new Error(`config.xcode.timeoutMs 必须是正数，实际是 ${JSON.stringify(candidate)}`);
    error.hint = '在 scenario.config.xcode 中设置 timeoutMs（毫秒）。';
    throw error;
  }
  return value;
}

/**
 * 构造 xcodebuild 参数：把 config 里的默认值补进 args，但不覆盖调用方显式写的参数。
 * @param {string} verb build / test / install
 * @param {string[]} args 步骤里写的原始参数
 * @param {Record<string, any>} config scenario.config.xcode
 * @param {string} cwd 工作目录
 * @returns {string[]} 完整参数列表
 */
export function buildXcodeArgs(verb, args, config, cwd) {
  const out = [verb];
  if (!args.some((item) => item === '-project' || item === '-workspace')) {
    out.push('-project', config.project || join(cwd, 'FoodMapApp.xcodeproj'));
  }
  if (config.scheme && !args.some((item) => item.startsWith('-scheme'))) {
    out.push('-scheme', config.scheme);
  }
  if (config.udid && !args.some((item) => item.startsWith('-destination'))) {
    out.push('-destination', `platform=iOS Simulator,id=${config.udid}`);
  }
  if (config.derivedDataPath) {
    out.push('-derivedDataPath', resolve(cwd, config.derivedDataPath));
  }
  if (config.codeSignIdentity && !args.some((item) => item.startsWith('CODE_SIGN_IDENTITY'))) {
    out.push(`CODE_SIGN_IDENTITY=${config.codeSignIdentity}`);
  }
  out.push(...args);
  return out;
}

function parseStep(step) {
  const text = String(step.xcode || '').trim();
  const [verb, ...rest] = text.split(/\s+/);
  if (!VERBS.has(verb)) {
    const error = new Error(`xcode 动作必须是 build / test / install，实际是 ${JSON.stringify(verb)}`);
    error.hint = '写法：xcode: test -scheme FoodMapApp -only-testing:FoodMapAppUITests/LoginFlowTests';
    throw error;
  }
  return { verb, args: rest };
}

function run(binary, args, cwd, timeoutMs, env) {
  return new Promise((resolvePromise, reject) => {
    let child;
    try {
      child = spawn(binary, args, { cwd, env: { ...process.env, ...env }, stdio: ['ignore', 'pipe', 'pipe'] });
    } catch (error) {
      const failure = new Error(`启动 ${binary} 失败：${error.message}`);
      failure.hint = '检查 config.xcode.xcodebuildPath / xcrunPath 是否可执行。';
      failure.actual = { error: error.message };
      reject(failure);
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
      const failure = new Error(`执行 ${binary} 失败：${error.message}`);
      failure.hint = '确认已安装 Xcode 并执行过 xcode-select。';
      failure.actual = { command: `${binary} ${args.join(' ')}`, error: error.message };
      reject(failure);
    });
    child.on('close', (code, signal) => {
      clearTimeout(timer);
      if (timedOut) {
        const failure = new Error(`${binary} 超时（${timeoutMs}ms）`);
        failure.hint = '调大 config.xcode.timeoutMs，或先用命令行手动跑一次确认耗时。';
        failure.actual = { exitCode: code, signal, stdout: tail(stdout), stderr: tail(stderr) };
        reject(failure);
        return;
      }
      resolvePromise({ exitCode: code, signal, stdout, stderr });
    });
  });
}

function tail(text, limit = 8000) {
  const value = String(text || '');
  return value.length > limit ? value.slice(value.length - limit) : value;
}

/** 把 testNodes 树拍平成用例列表。 */
function flattenTests(node, out = []) {
  if (!node || typeof node !== 'object') return out;
  if (node.nodeType === 'Test Case') {
    out.push({
      name: node.name || '',
      identifier: node.nodeIdentifier || '',
      status: node.result || '',
      durationMs: Math.round(Number(node.durationInSeconds || 0) * 1000),
      failure: node.failureMessage || '',
    });
    return out;
  }
  for (const child of node.children || []) flattenTests(child, out);
  return out;
}

function firstFailure(tests, summary) {
  const failed = tests.find((item) => item.status && item.status !== 'Passed' && item.status !== 'Skipped');
  if (failed && failed.failure) return `${failed.identifier || failed.name}: ${failed.failure}`;
  const fromSummary = (summary.testFailures || [])[0];
  if (fromSummary) return `${fromSummary.testName || ''} ${fromSummary.failureText || ''}`.trim();
  return '';
}

/**
 * 执行 xcode 步骤。
 * @param {Record<string, any>} step 已插值的步骤
 * @param {{config: Record<string, any>, scenario: Record<string, any>}} context 运行上下文
 * @returns {Promise<Record<string, any>>} 适配器结果
 */
export async function runXcode(step, context) {
  const config = configOf(context);
  const { verb, args } = parseStep(step);
  const xcodebuild = config.xcodebuildPath || 'xcodebuild';
  const xcrun = config.xcrunPath || 'xcrun';
  const cwd = context.scenario?.dir ? resolve(context.scenario.dir, config.cwd || '.') : process.cwd();
  const timeoutMs = timeoutOf(config, verb);

  const buildArgs = buildXcodeArgs(verb, args, config, cwd);

  let resultBundle = '';
  if (verb === 'test') {
    const dir = await mkdtemp(join(tmpdir(), 'verify-xcresult-'));
    resultBundle = join(dir, 'run.xcresult');
    buildArgs.push('-resultBundlePath', resultBundle);
  }

  const runResult = await run(xcodebuild, buildArgs, cwd, timeoutMs, { ...(config.env || {}), ...(step.env || {}) });
  const display = `xcode ${verb}${config.scheme ? ` (${config.scheme})` : ''}`;
  const base = {
    display,
    actual: {
      exitCode: runResult.exitCode,
      stdout: tail(runResult.stdout, 4000),
      stderr: tail(runResult.stderr, 4000),
      stdoutTail: runResult.stdout,
      content: `${runResult.stdout}\n${runResult.stderr}`,
      value: runResult.exitCode,
      command: `${xcodebuild} ${buildArgs.join(' ')}`,
    },
    captureRoot: null,
  };

  if (verb !== 'test') {
    if (verb === 'install') {
      if (runResult.exitCode !== 0) return base;
      if (!config.udid) {
        const error = new Error('xcode install 需要 config.xcode.udid');
        error.hint = '在 scenario.config.xcode.udid 中指定模拟器 UDID。';
        throw error;
      }
      const appPath = resolve(
        cwd,
        config.derivedDataPath || '',
        'Build/Products',
        config.configuration || 'Debug-iphonesimulator',
        `${config.scheme || 'App'}.app`,
      );
      const installed = await run(xcrun, ['simctl', 'install', String(config.udid), appPath], cwd, 120000, {});
      base.actual.appPath = appPath;
      base.actual.installExitCode = installed.exitCode;
      base.actual.installStderr = tail(installed.stderr, 2000);
      base.actual.exitCode = installed.exitCode;
      base.actual.value = installed.exitCode;
      base.actual.content = `${runResult.stdout}\n${installed.stdout}\n${installed.stderr}`;
    }
    base.summary = `exit ${runResult.exitCode}`;
    return base;
  }

  // test：解析 xcresult，失败用例数作为 value
  let tests = [];
  let summaryJson = {};
  try {
    const summaryRun = await run(
      xcrun,
      ['xcresulttool', 'get', 'test-results', 'summary', '--path', resultBundle],
      cwd,
      60000,
      {},
    );
    if (summaryRun.exitCode === 0) summaryJson = JSON.parse(summaryRun.stdout);
    const testsRun = await run(
      xcrun,
      ['xcresulttool', 'get', 'test-results', 'tests', '--path', resultBundle],
      cwd,
      60000,
      {},
    );
    if (testsRun.exitCode === 0) {
      const tree = JSON.parse(testsRun.stdout);
      for (const node of tree.testNodes || []) flattenTests(node, tests);
    }
  } catch (error) {
    base.actual.parseError = error.message;
  }

  const failed = summaryJson.failedTests ?? tests.filter((item) => item.status !== 'Passed' && item.status !== 'Skipped').length;
  const passed = summaryJson.passedTests ?? tests.filter((item) => item.status === 'Passed').length;
  const skipped = summaryJson.skippedTests ?? tests.filter((item) => item.status === 'Skipped').length;

  const actual = {
    ...base.actual,
    result: summaryJson.result || '',
    total: passed + failed + skipped,
    passed,
    failed,
    skipped,
    tests,
    value: failed,
    resultBundle,
  };
  if (runResult.exitCode !== 0 || actual.total === 0 || actual.passed + actual.failed === 0) {
    const error = new Error(runResult.exitCode !== 0
      ? `xcodebuild test 失败（退出码 ${runResult.exitCode}），不能用 failed=0 判定通过`
      : 'XCUITest 未执行任何可判定的用例，不能判定通过');
    error.actual = actual;
    error.hint = '检查 actual.stderr、xcresult 测试运行器启动/权限错误；不能以 0 passed / 0 failed 作为成功证据。';
    throw error;
  }
  const failure = firstFailure(tests, summaryJson);
  if (failure) actual.failureMessage = failure;

  const summary = `${passed} passed / ${failed} failed`;
  return {
    display,
    actual,
    captureRoot: actual,
    summary,
    hint: failure || '',
  };
}
