#!/usr/bin/env node
// 通用验证工具 CLI：加载 scenario、执行、输出、写证据。
//
// 零 npm 依赖；所有项目差异都来自 scenario / --config / --var。

import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { loadScenario } from './core/scenario.mjs';
import { runScenario } from './core/runner.mjs';
import { writeEvidence } from './core/evidence.mjs';

const DEFAULT_EVIDENCE_DIR = fileURLToPath(new URL('./evidence/', import.meta.url));
const ARGV = process.argv.slice(2);
const JSON_REQUESTED = ARGV.includes('--json');
const USAGE = `通用验证工具（Node.js >= 20，零 npm 依赖）

用法:
  node tools/verify/cli.mjs run <scenario.yml> [选项]

选项:
  --var <k=v>           覆盖 scenario.vars，可重复
  --config <cfg.yml>    覆盖 scenario.config（重复指定时最后一个生效）
  --evidence-dir <dir>  证据目录，默认 tools/verify/evidence
  --json                把结构化结果输出到 stdout（进度和错误走 stderr）
  -h, --help            显示帮助

退出码:
  0  全部步骤通过
  1  至少一个步骤失败
  2  scenario/配置/CLI 用法错误，或证据写入失败
`;

function usageError(message) {
  const error = new Error(message);
  error.exitCode = 2;
  error.hint = '运行 node tools/verify/cli.mjs --help 查看用法。';
  return error;
}

function parseArgs(argv) {
  if (argv.length === 0) return { help: true };
  if (argv[0] === '-h' || argv[0] === '--help' || argv[0] === 'help') return { help: true };
  if (argv[0] !== 'run') throw usageError(`未知命令 ${JSON.stringify(argv[0])}，只支持 run`);
  const options = { command: 'run', scenarioPath: '', vars: {}, configPath: '', evidenceDir: '', json: false };
  for (let i = 1; i < argv.length; i += 1) {
    const arg = argv[i];
    if (arg === '-h' || arg === '--help') return { help: true };
    if (arg === '--json') {
      options.json = true;
      continue;
    }
    if (arg === '--var') {
      const value = argv[i + 1];
      if (value === undefined) throw usageError('--var 缺少 k=v');
      const equals = value.indexOf('=');
      if (equals <= 0) throw usageError(`--var 需要 k=v，实际是 ${JSON.stringify(value)}`);
      const key = value.slice(0, equals).trim();
      if (!/^[A-Za-z_][A-Za-z0-9_.-]*$/.test(key)) throw usageError(`--var 的变量名非法：${key}`);
      options.vars[key] = value.slice(equals + 1);
      i += 1;
      continue;
    }
    if (arg === '--config') {
      const value = argv[i + 1];
      if (value === undefined || value.startsWith('--')) throw usageError('--config 缺少文件路径');
      options.configPath = value;
      i += 1;
      continue;
    }
    if (arg === '--evidence-dir') {
      const value = argv[i + 1];
      if (value === undefined || value.startsWith('--')) throw usageError('--evidence-dir 缺少目录路径');
      options.evidenceDir = value;
      i += 1;
      continue;
    }
    if (arg.startsWith('-')) throw usageError(`未知选项 ${arg}`);
    if (options.scenarioPath) throw usageError(`只能指定一个 scenario，实际又遇到 ${arg}`);
    options.scenarioPath = arg;
  }
  if (!options.scenarioPath) throw usageError('缺少 scenario 文件路径');
  return options;
}

function printError(error, jsonMode) {
  const message = error && error.message ? error.message : String(error);
  const hint = error && error.hint ? error.hint : '';
  if (jsonMode) {
    process.stdout.write(`${JSON.stringify({ ok: false, exitCode: 2, error: { message, hint } }, null, 2)}\n`);
  }
  process.stderr.write(`错误: ${message}\n`);
  if (hint) process.stderr.write(`提示: ${hint}\n`);
  if (process.env.VERIFY_DEBUG === '1' && error && error.stack) process.stderr.write(`${error.stack}\n`);
}

async function main() {
  let options;
  try {
    options = parseArgs(ARGV);
  } catch (error) {
    printError(error, JSON_REQUESTED);
    process.exitCode = 2;
    return;
  }
  if (options.help) {
    process.stdout.write(USAGE);
    process.exitCode = 0;
    return;
  }

  const progress = (line) => {
    if (options.json) process.stderr.write(`${line}\n`);
    else process.stdout.write(`${line}\n`);
  };

  let scenario;
  try {
    scenario = await loadScenario(options.scenarioPath, {
      configPath: options.configPath,
      vars: options.vars,
    });
  } catch (error) {
    printError(error, options.json);
    process.exitCode = 2;
    return;
  }

  let result;
  try {
    result = await runScenario(scenario, { log: progress });
  } catch (error) {
    printError(error, options.json);
    process.exitCode = 2;
    return;
  }

  const evidenceDir = options.evidenceDir ? resolve(options.evidenceDir) : DEFAULT_EVIDENCE_DIR;
  let evidence;
  try {
    evidence = await writeEvidence(result, evidenceDir);
  } catch (error) {
    const failure = new Error(`证据写入失败（${evidenceDir}）：${error.message}`);
    failure.hint = '检查 --evidence-dir 的路径和写权限。';
    result.evidenceError = failure.message;
    result.exitCode = 2;
    process.stderr.write(`错误: ${failure.message}\n`);
    process.stderr.write(`提示: ${failure.hint}\n`);
    if (process.env.VERIFY_DEBUG === '1' && error.stack) process.stderr.write(`${error.stack}\n`);
  }

  if (options.json) {
    process.stdout.write(`${JSON.stringify(result, null, 2)}\n`);
  } else {
    const summary = result.ok
      ? `通过 ${result.summary.passed}/${result.summary.total}，耗时 ${result.durationMs}ms`
      : `失败 ${result.summary.failed}/${result.summary.total}，耗时 ${result.durationMs}ms`;
    if (result.evidenceError) process.stdout.write(`证据失败: ${result.evidenceError}\n`);
    process.stdout.write(`${summary}\n`);
    if (evidence) {
      process.stdout.write(`证据: ${evidence.text}\n`);
      process.stdout.write(`      ${evidence.json}\n`);
    }
  }
  process.exitCode = result.exitCode;
}

main().catch((error) => {
  printError(error, JSON_REQUESTED);
  process.exitCode = 2;
});
