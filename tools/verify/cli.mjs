#!/usr/bin/env node
// 通用验证工具 CLI：加载 scenario（或目录）、执行、输出、写证据。
//
// 零 npm 依赖；所有项目差异都来自 scenario / --config / --var。

import { statSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { loadScenario } from './core/scenario.mjs';
import { runScenario } from './core/runner.mjs';
import { collectScenarios, runSuite } from './core/suite.mjs';
import { writeEvidence, writeSuiteEvidence } from './core/evidence.mjs';
import { createRedactor } from './core/redact.mjs';
import { toJUnit } from './core/junit.mjs';

const DEFAULT_EVIDENCE_DIR = fileURLToPath(new URL('./evidence/', import.meta.url));
const ARGV = process.argv.slice(2);
const JSON_REQUESTED = ARGV.includes('--json');
const USAGE = `通用验证工具（Node.js >= 20，零 npm 依赖）

用法:
  node tools/verify/cli.mjs run <scenario.yml | 目录> [选项]

选项:
  --var <k=v>           覆盖 scenario.vars，可重复
  --config <cfg.yml>    覆盖 scenario.config（重复指定时最后一个生效）
  --evidence-dir <dir>  证据目录，默认 tools/verify/evidence
  --only <关键字>       只跑路径里包含该关键字的场景（目录模式）
  --junit <file>        额外写 JUnit XML
  --list                只列出将被执行的场景，不执行
  --dry-run             只做加载和静态校验，不执行任何步骤
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

function nextValue(argv, index, option) {
  const value = argv[index + 1];
  if (value === undefined || value.startsWith('--')) throw usageError(`${option} 缺少取值`);
  return value;
}

function parseArgs(argv) {
  if (argv.length === 0) return { help: true };
  if (argv[0] === '-h' || argv[0] === '--help' || argv[0] === 'help') return { help: true };
  if (argv[0] !== 'run') throw usageError(`未知命令 ${JSON.stringify(argv[0])}，只支持 run`);
  const options = {
    command: 'run',
    target: '',
    vars: {},
    configPath: '',
    evidenceDir: '',
    junitPath: '',
    only: '',
    list: false,
    dryRun: false,
    json: false,
  };
  for (let i = 1; i < argv.length; i += 1) {
    const arg = argv[i];
    if (arg === '-h' || arg === '--help') return { help: true };
    if (arg === '--json') {
      options.json = true;
      continue;
    }
    if (arg === '--list') {
      options.list = true;
      continue;
    }
    if (arg === '--dry-run') {
      options.dryRun = true;
      continue;
    }
    if (arg === '--var') {
      const value = nextValue(argv, i, '--var');
      const equals = value.indexOf('=');
      if (equals <= 0) throw usageError(`--var 需要 k=v，实际是 ${JSON.stringify(value)}`);
      const key = value.slice(0, equals).trim();
      if (!/^[A-Za-z_][A-Za-z0-9_.-]*$/.test(key)) throw usageError(`--var 的变量名非法：${key}`);
      options.vars[key] = value.slice(equals + 1);
      i += 1;
      continue;
    }
    if (arg === '--config') {
      options.configPath = nextValue(argv, i, '--config');
      i += 1;
      continue;
    }
    if (arg === '--evidence-dir') {
      options.evidenceDir = nextValue(argv, i, '--evidence-dir');
      i += 1;
      continue;
    }
    if (arg === '--only') {
      options.only = nextValue(argv, i, '--only');
      i += 1;
      continue;
    }
    if (arg === '--junit') {
      options.junitPath = nextValue(argv, i, '--junit');
      i += 1;
      continue;
    }
    if (arg.startsWith('-')) throw usageError(`未知选项 ${arg}`);
    if (options.target) throw usageError(`只能指定一个 scenario 或目录，实际又遇到 ${arg}`);
    options.target = arg;
  }
  if (!options.target) throw usageError('缺少 scenario 文件或目录路径');
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

function isDirectory(path) {
  try {
    return statSync(path).isDirectory();
  } catch {
    return false;
  }
}

function attachRedactor(target, redactor) {
  Object.defineProperty(target, '__redactor', { value: redactor, enumerable: false });
  return target;
}

/** stdout、场景证据、套件证据使用同一套脱敏，避免 --json 把 Token 打到终端或 CI 日志。 */
function printable(result) {
  const own = result.__redactor || createRedactor([]);
  const clone = JSON.parse(JSON.stringify(result));
  if (result.kind === 'suite') {
    clone.scenarios = (result.scenarios || []).map((item) => {
      const redactor = item.__redactor || own;
      return redactor.redactValue(JSON.parse(JSON.stringify(item)));
    });
  }
  return own.redactValue(clone);
}

function printSummary(result, evidence) {
  const summary = result.summary || {};
  if (result.kind === 'suite') {
    const line = result.ok
      ? `通过 ${summary.passed}/${summary.scenarios} 个场景，耗时 ${result.durationMs}ms`
      : `失败 ${summary.failed}/${summary.scenarios} 个场景，耗时 ${result.durationMs}ms`;
    process.stdout.write(`${line}\n`);
    for (const item of result.scenarios.filter((entry) => !entry.ok)) {
      process.stdout.write(`  FAIL ${item.name}${item.loadError ? ` — ${item.loadError}` : ''}\n`);
    }
    if (evidence) {
      process.stdout.write(`证据: ${evidence.text}\n`);
      process.stdout.write(`      ${evidence.json}\n`);
    }
    return;
  }
  const line = result.ok
    ? `通过 ${summary.passed}/${summary.total}，耗时 ${result.durationMs}ms`
    : `失败 ${summary.failed}/${summary.total}，耗时 ${result.durationMs}ms`;
  if (result.evidenceError) process.stdout.write(`证据失败: ${result.evidenceError}\n`);
  process.stdout.write(`${line}\n`);
  if (evidence) {
    process.stdout.write(`证据: ${evidence.text}\n`);
    process.stdout.write(`      ${evidence.json}\n`);
  }
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

  // 进度和失败详情也可能带出响应体，统一先脱敏再打印（包含 stderr 的断言失败分支）。
  let activeRedactor = createRedactor([]);
  const progress = (line) => {
    const safe = activeRedactor.redact(String(line));
    if (options.json) process.stderr.write(`${safe}\n`);
    else process.stdout.write(`${safe}\n`);
  };
  const evidenceDir = options.evidenceDir ? resolve(options.evidenceDir) : DEFAULT_EVIDENCE_DIR;
  const directoryMode = isDirectory(options.target);

  if (options.list) {
    const files = directoryMode ? await collectScenarios(options.target) : [resolve(options.target)];
    const selected = options.only ? files.filter((file) => file.includes(options.only)) : files;
    for (const file of selected) process.stdout.write(`${file}\n`);
    process.exitCode = selected.length > 0 ? 0 : 2;
    return;
  }

  if (options.dryRun) {
    let results = [];
    try {
      if (directoryMode) {
        const files = await collectScenarios(options.target);
        const selected = options.only ? files.filter((file) => file.includes(options.only)) : files;
        for (const file of selected) {
          const scenario = await loadScenario(file, { configPath: options.configPath, vars: options.vars });
          results.push({ file, name: scenario.name, steps: scenario.steps.length, setup: scenario.setup.length, teardown: scenario.teardown.length });
        }
      } else {
        const scenario = await loadScenario(options.target, { configPath: options.configPath, vars: options.vars });
        results.push({ file: resolve(options.target), name: scenario.name, steps: scenario.steps.length, setup: scenario.setup.length, teardown: scenario.teardown.length });
      }
    } catch (error) {
      printError(error, options.json);
      process.exitCode = 2;
      return;
    }
    if (options.json) process.stdout.write(`${JSON.stringify({ ok: true, dryRun: true, scenarios: results }, null, 2)}\n`);
    else for (const item of results) process.stdout.write(`ok  ${item.name}（setup ${item.setup} / steps ${item.steps} / teardown ${item.teardown}）\n`);
    process.exitCode = 0;
    return;
  }

  let result;
  let evidence = null;
  try {
    if (directoryMode) {
      const suiteRules = [];
      result = await runSuite(options.target, {
        only: options.only,
        configPath: options.configPath,
        vars: options.vars,
        log: progress,
        collectRules: suiteRules,
      });
      for (const scenarioResult of result.scenarios) {
        try {
          const scenario = await loadScenario(scenarioResult.source, { configPath: options.configPath, vars: options.vars });
          const redactor = createRedactor(scenario.config.redact || []);
          attachRedactor(scenarioResult, redactor);
          await writeEvidence(scenarioResult, evidenceDir, { redactor });
        } catch (error) {
          scenarioResult.evidenceError = error.message;
        }
      }
      attachRedactor(result, createRedactor([]));
      evidence = await writeSuiteEvidence(result, evidenceDir, { redactor: createRedactor(suiteRules) });
    } else {
      const scenario = await loadScenario(options.target, { configPath: options.configPath, vars: options.vars });
      const redactor = createRedactor(scenario.config.redact || []);
      activeRedactor = redactor;
      result = attachRedactor(await runScenario(scenario, { log: progress }), redactor);
      try {
        evidence = await writeEvidence(result, evidenceDir, { redactor });
      } catch (error) {
        result.evidenceError = `证据写入失败（${evidenceDir}）：${error.message}`;
        result.exitCode = 2;
        process.stderr.write(`错误: ${result.evidenceError}\n`);
        process.stderr.write('提示: 检查 --evidence-dir 的路径和写权限。\n');
      }
    }
  } catch (error) {
    printError(error, options.json);
    process.exitCode = 2;
    return;
  }

  if (options.junitPath) {
    try {
      writeFileSync(resolve(options.junitPath), toJUnit(result), 'utf8');
    } catch (error) {
      process.stderr.write(`错误: JUnit 写入失败（${options.junitPath}）：${error.message}\n`);
      result.exitCode = 2;
    }
  }

  if (options.json) {
    process.stdout.write(`${JSON.stringify(printable(result), null, 2)}\n`);
  } else {
    printSummary(result, evidence);
  }
  process.exitCode = result.exitCode;
}

main().catch((error) => {
  printError(error, JSON_REQUESTED);
  process.exitCode = 2;
});
