// 套件先校验全部场景，再只读预检，最后顺序执行；部分完成不等同于通过。
import { readdir, stat } from 'node:fs/promises';
import { join, relative, resolve } from 'node:path';
import { randomUUID } from 'node:crypto';
import { loadScenario } from './scenario.mjs';
import { runScenario } from './runner.mjs';
import { createRedactor } from './redact.mjs';
import { inferRequirements, loadDependencyManifest, probeDependencies } from './deps.mjs';

const SKIP_DIRS = new Set(['node_modules', '.git', 'evidence', '.build']);

/** 递归收集按路径排序的 YAML 场景，不追随符号链接。 */
export async function collectScenarios(dir) {
  const root = resolve(dir);
  const files = [];
  async function walk(current) {
    for (const entry of (await readdir(current, { withFileTypes: true })).sort((a, b) => a.name.localeCompare(b.name))) {
      if (entry.name.startsWith('.')) continue;
      const full = join(current, entry.name);
      if (entry.isDirectory()) {
        if (!SKIP_DIRS.has(entry.name)) await walk(full);
      } else if (entry.isFile() && /\.ya?ml$/i.test(entry.name)) files.push(full);
    }
  }
  if (!(await stat(root)).isDirectory()) return [root];
  await walk(root);
  return files;
}

/**
 * dependenciesPath 启用只读预检；skipDeps 是人工指定不可用依赖。
 * 普通模式有 SKIP 可退出 0，但 ok=false、status=partial；requireFull 时 SKIP 返回 1。
 * 无效场景/配置返回 2，任何预检前校验错误均不执行业务步骤。
 */
export async function runSuite(dir, options = {}) {
  const log = options.log || (() => {});
  const root = resolve(dir);
  const files = await collectScenarios(root);
  const selected = options.only ? files.filter(file => file.includes(options.only)) : files;
  if (!selected.length) throw Object.assign(new Error(`目录 ${root} 下没有匹配的 scenario`), { exitCode: 2 });
  const startedAt = new Date().toISOString();
  const started = Date.now();
  // 只延期检查运行时环境变量，不放宽语法、未知变量、依赖名校验。
  const scenarios = [];
  for (const file of selected) scenarios.push(await loadScenario(file, { ...options, deferEnv: true }));
  const names = [...new Set(scenarios.flatMap(inferRequirements))].sort();
  const preflight = options.dependenciesPath
    ? await probeDependencies(await loadDependencyManifest(options.dependenciesPath), names) : [];
  const unavailable = new Map(preflight.filter(item => !item.ready).map(item => [item.name, item.reasons.join('；')]));
  for (const name of options.skipDeps || []) unavailable.set(name, '由 --skip-deps 显式跳过');
  for (const item of preflight) log(`预检 ${item.ready ? 'READY' : 'UNAVAILABLE'} ${item.name}${item.reasons.length ? ` — ${item.reasons.join('；')}` : ''}`);
  const results = [];
  for (const preview of scenarios) {
    const missing = inferRequirements(preview).filter(dep => unavailable.has(dep));
    if (missing.length) {
      const skipReason = missing.map(dep => `${dep}: ${unavailable.get(dep)}`).join('；');
      log(`SKIP ${relative(root, preview.source)} — ${skipReason}`);
      results.push({ schemaVersion: 2, name: preview.name, source: preview.source, runId: preview.runId,
        startedAt, finishedAt: new Date().toISOString(), ok: false, skipped: true, skipReason,
        exitCode: options.requireFull ? 1 : 0, durationMs: 0, steps: [],
        summary: { total: 0, passed: 0, failed: 0, skipped: 0 } });
      continue;
    }
    const scenario = await loadScenario(preview.source, options);
    const rules = scenario.config.redact || [];
    const redactor = createRedactor(rules);
    if (options.collectRules) options.collectRules.push(...rules);
    log(`\n=== ${relative(root, scenario.source)} ===`);
    results.push(await runScenario(scenario, { log: line => log(redactor.redact(String(line))) }));
  }
  const failed = results.filter(item => !item.ok && !item.skipped).length;
  const skipped = results.filter(item => item.skipped).length;
  const exitCode = failed || (options.requireFull && skipped) ? 1 : 0;
  return {
    schemaVersion: 2, kind: 'suite', name: relative(process.cwd(), root) || root, root,
    runId: `suite-${randomUUID()}`, ok: failed === 0 && skipped === 0,
    status: failed ? 'failed' : skipped ? 'partial' : 'passed', exitCode,
    requireFull: Boolean(options.requireFull), preflight,
    startedAt, finishedAt: new Date().toISOString(), durationMs: Date.now() - started,
    summary: { scenarios: results.length, passed: results.filter(item => item.ok).length, failed, skipped,
      steps: results.reduce((sum, item) => sum + (item.summary?.total || 0), 0) }, scenarios: results,
  };
}
