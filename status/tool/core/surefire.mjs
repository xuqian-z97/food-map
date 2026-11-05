// Surefire 报告解析：把 Maven 测试结果变成可入库的测试证据。
// 只读 target/surefire-reports，不触发构建。

import { readFileSync, existsSync, readdirSync, statSync } from 'node:fs';
import { join } from 'node:path';

function attr(tag, name) {
  const match = tag.match(new RegExp(`${name}="([^"]*)"`));
  return match ? Number(match[1]) : 0;
}

function walk(dir, out) {
  if (!existsSync(dir)) return out;
  for (const name of readdirSync(dir)) {
    const full = join(dir, name);
    if (statSync(full).isDirectory()) walk(full, out);
    else if (/^TEST-.*\.xml$/.test(name)) out.push(full);
  }
  return out;
}

/**
 * 汇总各模块 Surefire 报告。
 * @param {string} root 仓库根目录
 * @param {string} globPattern 配置中的 surefireGlob，形如 after 下各模块的 surefire-reports/TEST 文件
 * @returns {{total: number, passed: number, failed: number, skipped: number, perModule: Record<string, number>, reports: string[]}}
 */
export function importSurefire(root, globPattern) {
  const match = globPattern.match(/^([^*]+)\/\*/);
  const base = match ? match[1] : 'after';
  const reports = walk(join(root, base), []);
  const perModule = {};
  let total = 0;
  let failed = 0;
  let skipped = 0;
  for (const file of reports) {
    const xml = readFileSync(file, 'utf8');
    const tag = xml.match(/<testsuite\b[^>]*>/);
    if (!tag) continue;
    const tests = attr(tag[0], 'tests');
    const failures = attr(tag[0], 'failures');
    const errors = attr(tag[0], 'errors');
    const skip = attr(tag[0], 'skipped');
    total += tests;
    failed += failures + errors;
    skipped += skip;
    const parts = file.replace(`${root}/`, '').split('/');
    const module = parts.length > 2 ? parts[1] : parts[0];
    perModule[module] = (perModule[module] || 0) + tests;
  }
  return { total, passed: total - failed - skipped, failed, skipped, perModule, reports: reports.map((f) => f.replace(`${root}/`, '')) };
}
