// 证据文件：同名 .txt（步骤表）和 .json（结构化结果）。

import { mkdir, writeFile } from 'node:fs/promises';
import { join } from 'node:path';
import { formatValue } from './assert.mjs';

function pad(number) {
  return String(number).padStart(2, '0');
}

function localStamp(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}-${pad(date.getMinutes())}`;
}

function safeName(name) {
  const cleaned = String(name)
    .trim()
    .replace(/[\\/:*?"<>|\u0000-\u001f]+/g, '_')
    .replace(/^\.+/, '')
    .slice(0, 80);
  return cleaned || 'scenario';
}

function renderText(result) {
  const lines = [];
  lines.push(`验证场景: ${result.name}`);
  lines.push(`Run ID: ${result.runId}`);
  lines.push(`Scenario: ${result.source}`);
  lines.push(`开始: ${result.startedAt}`);
  lines.push(`结束: ${result.finishedAt}`);
  lines.push(`耗时: ${result.durationMs}ms`);
  lines.push(`结果: ${result.ok ? 'PASS' : 'FAIL'} (${result.summary.passed}/${result.summary.total})`);
  lines.push('');
  lines.push('#   结果  耗时     步骤');
  for (const step of result.steps) {
    lines.push(`${String(step.index).padEnd(4)}${(step.ok ? 'ok' : 'FAIL').padEnd(6)}${`${step.durationMs}ms`.padEnd(9)}${step.display}`);
  }
  const failed = result.steps.filter((step) => !step.ok);
  if (failed.length > 0) {
    lines.push('');
    lines.push('失败详情:');
    for (const step of failed) {
      lines.push(`[${step.index}] ${step.display}`);
      lines.push(`  expected: ${formatValue(step.expected)}`);
      lines.push(`  actual: ${formatValue(step.actual)}`);
      if (step.error) lines.push(`  error: ${step.error}`);
      if (step.hint) lines.push(`  hint: ${step.hint}`);
      for (const check of step.checks.filter((item) => !item.ok)) {
        lines.push(`  ${check.name}: expected ${formatValue(check.expected)} / actual ${formatValue(check.actual)}`);
        if (check.error) lines.push(`    detail: ${check.error}`);
      }
    }
  }
  return `${lines.join('\n')}\n`;
}

/**
 * 写入证据文件，并把路径挂到 result.evidence。
 * @param {Record<string, any>} result runScenario 的结果
 * @param {string} evidenceDir 证据目录
 * @returns {Promise<{text: string, json: string}>} 两个文件绝对路径
 */
export async function writeEvidence(result, evidenceDir) {
  const date = new Date(result.startedAt);
  const base = `${localStamp(Number.isNaN(date.getTime()) ? new Date() : date)}-${safeName(result.name)}`;
  const paths = {
    text: join(evidenceDir, `${base}.txt`),
    json: join(evidenceDir, `${base}.json`),
  };
  await mkdir(evidenceDir, { recursive: true });
  result.evidence = paths;
  await writeFile(paths.text, renderText(result), 'utf8');
  await writeFile(paths.json, `${JSON.stringify(result, null, 2)}\n`, 'utf8');
  return paths;
}

export { renderText };
