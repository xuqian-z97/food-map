// suite：把一个目录下的全部 scenario 当成一次运行，聚合出一份结果。

import { readdir, stat } from 'node:fs/promises';
import { join, relative, resolve } from 'node:path';
import { loadScenario } from './scenario.mjs';
import { runScenario } from './runner.mjs';

const SKIP_DIRS = new Set(['node_modules', '.git', 'evidence', '.build']);

/**
 * 递归收集目录下的 scenario 文件。
 * @param {string} dir 目录
 * @returns {Promise<string[]>} 按路径排序的绝对路径列表
 */
export async function collectScenarios(dir) {
  const root = resolve(dir);
  const files = [];
  async function walk(current) {
    const entries = await readdir(current, { withFileTypes: true });
    for (const entry of entries.sort((a, b) => a.name.localeCompare(b.name))) {
      if (entry.name.startsWith('.')) continue;
      const full = join(current, entry.name);
      if (entry.isDirectory()) {
        if (SKIP_DIRS.has(entry.name)) continue;
        await walk(full);
        continue;
      }
      if (/\.ya?ml$/i.test(entry.name)) files.push(full);
    }
  }
  const info = await stat(root);
  if (!info.isDirectory()) return [root];
  await walk(root);
  return files;
}

/**
 * 依次执行目录下的 scenario，聚合结果。
 * @param {string} dir scenario 目录
 * @param {{only?: string, configPath?: string, vars?: Record<string,string>, log?: (line: string) => void}} [options]
 * @returns {Promise<Record<string, any>>} 聚合结果
 */
export async function runSuite(dir, options = {}) {
  const log = options.log || (() => {});
  const root = resolve(dir);
  const files = await collectScenarios(root);
  const only = (options.only || '').trim();
  const selected = only ? files.filter((file) => file.includes(only)) : files;
  if (selected.length === 0) {
    const error = new Error(`目录 ${root} 下没有匹配的 scenario${only ? `（--only ${only}）` : ''}`);
    error.exitCode = 2;
    error.hint = '确认目录里有 .yml 场景，或调整 --only。';
    throw error;
  }

  const startedAt = new Date().toISOString();
  const started = Date.now();
  const results = [];
  for (const file of selected) {
    log(`\n=== ${relative(root, file) || file} ===`);
    let result;
    try {
      const scenario = await loadScenario(file, { configPath: options.configPath, vars: options.vars });
      result = await runScenario(scenario, { log });
    } catch (error) {
      result = {
        schemaVersion: 2,
        name: relative(root, file),
        source: file,
        ok: false,
        exitCode: error.exitCode === 2 ? 2 : 1,
        loadError: error.message,
        hint: error.hint || '',
        steps: [],
        summary: { total: 0, passed: 0, failed: 1, skipped: 0, setupFailed: 0, teardownFailed: 0 },
        durationMs: 0,
      };
    }
    results.push(result);
  }

  const failed = results.filter((item) => !item.ok);
  const exitCode = results.some((item) => item.exitCode === 2) ? 2 : failed.length > 0 ? 1 : 0;
  return {
    schemaVersion: 2,
    kind: 'suite',
    name: relative(process.cwd(), root) || root,
    root,
    ok: exitCode === 0,
    exitCode,
    startedAt,
    finishedAt: new Date().toISOString(),
    durationMs: Date.now() - started,
    summary: {
      scenarios: results.length,
      passed: results.length - failed.length,
      failed: failed.length,
      steps: results.reduce((sum, item) => sum + (item.summary?.total || 0), 0),
    },
    scenarios: results,
  };
}
