#!/usr/bin/env node
// FoodMap 进度台账 CLI —— agent 与人工共用的唯一写入层。
// 用法：node status/tool/cli.mjs <command> [options]
// 所有命令支持 --json 输出，便于 agent 解析。

import { readFileSync, writeFileSync, mkdirSync, existsSync } from 'node:fs';
import { resolve, dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { parseYaml, stringifyYaml } from './core/yaml.mjs';
import {
  loadProject, buildView, FEATURE_KEYS, TASK_KEYS, PRIORITIES, FEATURE_STATES, STATE_LABELS,
  priorityRank,
} from './core/project.mjs';
import { validate } from './core/validate.mjs';
import { renderBoard } from './core/render.mjs';
import { importSurefire } from './core/surefire.mjs';
import { commitDate, commitExists } from './core/git.mjs';

const HERE = dirname(fileURLToPath(import.meta.url));
const ROOT = resolve(HERE, '../..');

const USAGE = `FoodMap 进度台账

用法：node status/tool/cli.mjs <command> [--json]

读取
  list [--layer=backend|frontend] [--state=<状态>] [--priority=P0]
  check                      门禁与对账检查；有错误时退出码 1
  build [--check]            生成 status/dist/index.html；--check 只比较摘要

写入（写后自动跑检查，不通过则回滚）
  feature set <F-001> [--status=testing] [--priority=P0] [--note=...]
  task add <F-001> --title=... [--priority=P1] [--owner=agent] [--plan=docs/...]
  task done <T-001> [--commit=<sha>] [--at=<ISO 时间>]

测试证据
  test import [--covers=F-001,F-002]   读取 surefire 报告并写入 runs/

说明
  --json 输出机器可读结果；状态取值：${FEATURE_STATES.join(' / ')}`;

function parseArgs(argv) {
  const positional = [];
  const flags = {};
  for (const arg of argv) {
    if (arg.startsWith('--')) {
      const [key, ...rest] = arg.slice(2).split('=');
      flags[key] = rest.length === 0 ? true : rest.join('=');
    } else {
      positional.push(arg);
    }
  }
  return { positional, flags };
}

function nowIso() {
  const date = new Date();
  const pad = (n) => String(n).padStart(2, '0');
  const offset = -date.getTimezoneOffset();
  const sign = offset >= 0 ? '+' : '-';
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}${sign}${pad(Math.floor(Math.abs(offset) / 60))}:${pad(Math.abs(offset) % 60)}`;
}

function runId(scope) {
  return `${nowIso().slice(0, 16).replace('T', 'T').replace(':', '-')}-${scope}`;
}

function load() {
  const project = loadProject(ROOT);
  const view = buildView(project);
  view.generatedAt = nowIso();
  return { project, view };
}

function distPath(project) {
  return resolve(ROOT, project.config.paths.dist || 'status/dist/index.html');
}

function report(checks) {
  for (const item of checks.errors) console.error(`错误 [规则 ${item.rule}] ${item.entity}: ${item.message}`);
  for (const item of checks.warnings) console.log(`提示 [规则 ${item.rule}] ${item.entity}: ${item.message}`);
  console.log(`检查结果：${checks.errors.length} 错误 / ${checks.warnings.length} 提示`);
}

/**
 * 写入实体文件并在写后校验；校验失败则恢复原内容。
 * @param {string} file 目标文件
 * @param {string} next 新内容
 * @param {() => void} afterValidate 可选的额外校验
 * @returns {boolean} 是否写入成功
 */
function errorKeys(checks) {
  return new Set(checks.errors.map((item) => `${item.rule}|${item.entity}|${item.message}`));
}

function writeWithGate(file, next) {
  const previous = existsSync(file) ? readFileSync(file, 'utf8') : null;
  const baseline = (() => {
    const { project, view } = load();
    return errorKeys(validate(project, view, distPath(project), { ignoreDist: true }));
  })();
  mkdirSync(dirname(file), { recursive: true });
  writeFileSync(file, next);
  const { project, view } = load();
  const checks = validate(project, view, distPath(project), { ignoreDist: true });
  const introduced = checks.errors.filter((item) => !baseline.has(`${item.rule}|${item.entity}|${item.message}`));
  if (introduced.length > 0) {
    if (previous === null) writeFileSync(file, '');
    else writeFileSync(file, previous);
    for (const item of introduced) console.error(`错误 [规则 ${item.rule}] ${item.entity}: ${item.message}`);
    console.error('写入已回滚：本次改动引入了新的阻断项。');
    return false;
  }
  console.log(`已写入 ${file.replace(`${ROOT}/`, '')}（${introduced.length} 新增阻断项）`);
  return true;
}

function nextId(entries, prefix) {
  const max = entries.reduce((acc, entry) => {
    const num = Number(String(entry.data.id).replace(`${prefix}-`, ''));
    return Number.isFinite(num) ? Math.max(acc, num) : acc;
  }, 0);
  return `${prefix}-${String(max + 1).padStart(3, '0')}`;
}

function cmdList(flags) {
  const { view } = load();
  let features = view.features;
  if (flags.layer) features = features.filter((feature) => feature.layer === flags.layer);
  if (flags.state) features = features.filter((feature) => feature.status === flags.state);
  if (flags.priority) features = features.filter((feature) => feature.priority === flags.priority);
  if (flags.json) {
    console.log(JSON.stringify({
      head: view.head,
      stats: view.stats,
      features: features.map((feature) => ({
        id: feature.id,
        title: feature.title,
        layer: feature.layer,
        phase: feature.phase,
        priority: feature.priority,
        status: feature.status,
        modules: [...feature.backendModules, ...feature.frontendModules],
        tasks: feature.tasks.map((item) => item.id),
        missingTasks: feature.missingTasks,
        blockers: feature.blockers,
        code: feature.code ? feature.code.shortSha : null,
      })),
      tasks: view.tasks.map((task) => ({
        id: task.id, title: task.title, feature: task.feature, status: task.status,
        priority: task.priority, doneAt: task.doneAt, commit: task.commit,
      })),
    }, null, 2));
    return 0;
  }
  console.log(`HEAD ${view.head.shortSha} · 完成 ${view.stats.accepted}/${view.stats.total} · 待验收 ${view.stats.pendingAcceptance} · 未排任务 ${view.stats.missingTasks} · P0 未完成 ${view.stats.p0Unfinished}`);
  console.log('');
  for (const feature of features) {
    const flagsText = [
      STATE_LABELS[feature.status] || feature.status,
      feature.priority,
      feature.missingTasks ? '未排任务' : '',
    ].filter(Boolean).join(' · ');
    console.log(`${feature.id}  ${feature.title}  [${flagsText}]`);
  }
  return 0;
}

function cmdCheck(flags) {
  const { project, view } = load();
  const checks = validate(project, view, distPath(project));
  if (flags.json) {
    console.log(JSON.stringify(checks, null, 2));
  } else {
    report(checks);
  }
  return checks.errors.length > 0 ? 1 : 0;
}

function cmdBuild(flags) {
  const { project, view } = load();
  const checks = validate(project, view, distPath(project), { ignoreDist: true });
  const target = distPath(project);
  const digest = checks.digest;
  if (flags.check) {
    const stored = checks.distDigest;
    if (stored === digest) {
      console.log(`看板已是最新（${digest}）`);
      return 0;
    }
    console.error(`看板落后：dist ${stored || '缺失'} ≠ data ${digest}`);
    return 1;
  }
  const html = renderBoard(view, checks, digest);
  mkdirSync(dirname(target), { recursive: true });
  writeFileSync(target, html);
  console.log(`已生成 ${target.replace(`${ROOT}/`, '')}（${(html.length / 1024).toFixed(1)} KB，摘要 ${digest}）`);
  if (checks.errors.length > 0) console.log(`注意：当前有 ${checks.errors.length} 条阻断项，看板中已标出。`);
  return 0;
}

function cmdFeatureSet(positional, flags) {
  const id = positional[2];
  if (!id) throw new Error('缺少功能点 id');
  const { project, view } = load();
  const feature = view.features.find((item) => item.id === id);
  if (!feature) throw new Error(`找不到功能点 ${id}`);
  const data = { ...feature.raw };
  if (flags.status) {
    if (!FEATURE_STATES.includes(flags.status)) throw new Error(`未知状态 ${flags.status}`);
    data.status = flags.status;
  }
  if (flags.priority) {
    if (!PRIORITIES.includes(flags.priority)) throw new Error(`未知优先级 ${flags.priority}`);
    data.priority = flags.priority;
  }
  if (flags.note) data.notes = flags.note;
  if (data.status === 'cancelled' && !data.cancelReason) data.cancelReason = flags.note || '未说明原因';
  return writeWithGate(feature.file, stringifyYaml(data, FEATURE_KEYS)) ? 0 : 1;
}

function cmdTaskAdd(positional, flags) {
  const featureId = positional[2];
  if (!featureId) throw new Error('缺少功能点 id');
  if (!flags.title) throw new Error('缺少 --title');
  const { project, view } = load();
  const feature = view.features.find((item) => item.id === featureId);
  if (!feature) throw new Error(`找不到功能点 ${featureId}`);
  const id = nextId(project.tasks, 'T');
  const file = join(resolve(ROOT, project.config.paths.data), 'tasks', `${id}.yml`);
  const task = {
    schemaVersion: 1,
    id,
    feature: featureId,
    title: flags.title,
    status: 'todo',
    priority: flags.priority || feature.priority,
    owner: flags.owner || 'agent',
    planRef: flags.plan || undefined,
  };
  const ok = writeWithGate(file, stringifyYaml(task, TASK_KEYS));
  if (!ok) return 1;
  const updated = { ...feature.raw, tasks: [...(feature.raw.tasks || []), id] };
  return writeWithGate(feature.file, stringifyYaml(updated, FEATURE_KEYS)) ? 0 : 1;
}

function cmdTaskDone(positional, flags) {
  const id = positional[2];
  if (!id) throw new Error('缺少任务 id');
  const { project, view } = load();
  const task = view.tasks.find((item) => item.id === id);
  if (!task) throw new Error(`找不到任务 ${id}`);
  const data = { ...task.raw, status: 'done' };
  if (flags.commit) {
    if (!commitExists(ROOT, flags.commit)) throw new Error(`提交不存在：${flags.commit}`);
    data.commit = flags.commit;
  }
  data.doneAt = flags.at || commitDate(ROOT, data.commit) || nowIso();
  const file = join(resolve(ROOT, project.config.paths.data), 'tasks', `${id}.yml`);
  return writeWithGate(file, stringifyYaml(data, TASK_KEYS)) ? 0 : 1;
}

function cmdTaskSet(positional, flags) {
  const id = positional[2];
  if (!id) throw new Error('缺少任务 id');
  const { project, view } = load();
  const task = view.tasks.find((item) => item.id === id);
  if (!task) throw new Error(`找不到任务 ${id}`);
  const data = { ...task.raw };
  if (flags.status) {
    if (!['todo', 'doing', 'done', 'dropped'].includes(flags.status)) throw new Error(`未知任务状态 ${flags.status}`);
    data.status = flags.status;
    if (flags.status === 'done' && !data.doneAt) data.doneAt = flags.at || commitDate(ROOT, data.commit) || nowIso();
  }
  if (flags.priority) {
    if (!PRIORITIES.includes(flags.priority)) throw new Error(`未知优先级 ${flags.priority}`);
    data.priority = flags.priority;
  }
  if (flags.owner) data.owner = flags.owner;
  if (flags.note) data.notes = flags.note;
  const file = join(resolve(ROOT, project.config.paths.data), 'tasks', `${id}.yml`);
  return writeWithGate(file, stringifyYaml(data, TASK_KEYS)) ? 0 : 1;
}

function cmdTestImport(flags) {
  const { project, view } = load();
  const covers = flags.covers ? String(flags.covers).split(',').map((item) => item.trim()).filter(Boolean) : [];
  for (const featureId of covers) {
    if (!view.features.some((feature) => feature.id === featureId)) throw new Error(`未知功能点 ${featureId}`);
  }
  const modules = flags.modules ? String(flags.modules).split(',').map((item) => item.trim()).filter(Boolean) : [];
  const summary = importSurefire(ROOT, project.config.backend.surefireGlob, modules);
  if (summary.total === 0) {
    console.error('没有找到 surefire 报告，先执行 Maven 测试。');
    return 1;
  }
  const id = runId(flags.scope || 'backend');
  const record = {
    schemaVersion: 1,
    id,
    at: nowIso(),
    kind: 'unit',
    command: project.config.backend.testCommand,
    env: 'local',
    result: { total: summary.total, passed: summary.passed, failed: summary.failed, skipped: summary.skipped },
    perModule: summary.perModule,
    modules: modules.length > 0 ? modules : Object.keys(summary.perModule),
    covers,
    evidence: [summary.reports[0] ? dirname(summary.reports[0]) : 'after'],
  };
  const file = join(resolve(ROOT, project.config.paths.data), 'runs', `${id}.json`);
  mkdirSync(dirname(file), { recursive: true });
  writeFileSync(file, `${JSON.stringify(record, null, 2)}\n`);
  console.log(`已记录测试运行 ${id}：${summary.passed}/${summary.total} 通过，覆盖 ${covers.join(',') || '未指定功能点'}`);
  return 0;
}

function main() {
  const { positional, flags } = parseArgs(process.argv.slice(2));
  const command = positional[0];
  if (!command || command === 'help' || flags.help) {
    console.log(USAGE);
    return 0;
  }
  try {
    switch (command) {
      case 'list': return cmdList(flags);
      case 'check': return cmdCheck(flags);
      case 'build': return cmdBuild(flags);
      case 'feature':
        if (positional[1] === 'set') return cmdFeatureSet(positional, flags);
        throw new Error('未知 feature 子命令');
      case 'task':
        if (positional[1] === 'add') return cmdTaskAdd(positional, flags);
        if (positional[1] === 'done') return cmdTaskDone(positional, flags);
        if (positional[1] === 'set') return cmdTaskSet(positional, flags);
        throw new Error('未知 task 子命令');
      case 'test':
        if (positional[1] === 'import') return cmdTestImport(flags);
        throw new Error('未知 test 子命令');
      default:
        console.error(`未知命令 ${command}\n`);
        console.log(USAGE);
        return 1;
    }
  } catch (error) {
    console.error(`失败：${error.message}`);
    return 1;
  }
}

process.exitCode = main();
