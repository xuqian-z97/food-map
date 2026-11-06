// JUnit XML 输出：给 CI 和只认 JUnit 的 agent 用。
//
// 一个 scenario 渲染为一个 <testsuite>，一个步骤渲染为一个 <testcase>。

function escapeXml(text) {
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;')
    .replace(/[\u0000-\u0008\u000b\u000c\u000e-\u001f]/g, '');
}

function renderFailure(step) {
  const details = [];
  if (step.error) details.push(step.error);
  for (const check of step.checks || []) {
    if (check.ok) continue;
    details.push(`${check.name}: expected ${JSON.stringify(check.expected)} / actual ${JSON.stringify(check.actual)}`);
    if (check.error) details.push(`  ${check.error}`);
  }
  if (details.length === 0) details.push(step.display || 'step failed');
  return details.join('\n');
}

function renderScenario(result) {
  const steps = result.steps || [];
  const failures = steps.filter((step) => !step.ok).length + (result.loadError ? 1 : 0);
  const time = ((result.durationMs || 0) / 1000).toFixed(3);
  const lines = [
    `  <testsuite name="${escapeXml(result.name)}" tests="${steps.length + (result.loadError ? 1 : 0)}" failures="${failures}" time="${time}">`,
  ];
  if (result.loadError) {
    lines.push(`    <testcase name="load scenario" classname="${escapeXml(result.name)}" time="0">`);
    lines.push(`      <failure message="${escapeXml(result.loadError)}">${escapeXml(result.hint || '')}</failure>`);
    lines.push('    </testcase>');
  }
  for (const step of steps) {
    const caseName = escapeXml(`${step.phase || 'step'} ${step.index} ${step.action} ${step.display || ''}`.trim());
    const caseTime = (((step.durationMs || 0)) / 1000).toFixed(3);
    if (step.ok) {
      lines.push(`    <testcase name="${caseName}" classname="${escapeXml(result.name)}" time="${caseTime}"/>`);
    } else {
      const message = step.error || `${step.action} 断言未通过`;
      lines.push(`    <testcase name="${caseName}" classname="${escapeXml(result.name)}" time="${caseTime}">`);
      lines.push(`      <failure message="${escapeXml(message)}">${escapeXml(renderFailure(step))}</failure>`);
      lines.push('    </testcase>');
    }
  }
  lines.push('  </testsuite>');
  return lines.join('\n');
}

/**
 * 生成 JUnit XML。
 * @param {Record<string, any>} result 单个 scenario 结果或 suite 结果
 * @returns {string} XML 文本
 */
export function toJUnit(result) {
  const scenarios = result.kind === 'suite' ? result.scenarios || [] : [result];
  const tests = scenarios.reduce((sum, item) => sum + (item.steps || []).length + (item.loadError ? 1 : 0), 0);
  const failures = scenarios.reduce(
    (sum, item) => sum + (item.steps || []).filter((step) => !step.ok).length + (item.loadError ? 1 : 0),
    0,
  );
  const time = ((result.durationMs || 0) / 1000).toFixed(3);
  const header = `<?xml version="1.0" encoding="UTF-8"?>\n<testsuites name="${escapeXml(result.name)}" tests="${tests}" failures="${failures}" time="${time}">`;
  const body = scenarios.map(renderScenario).join('\n');
  return `${header}\n${body}\n</testsuites>\n`;
}
