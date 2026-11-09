// 门禁与对账校验：errors 阻断提交，warnings 只提示。
// 规则编号与 status/data/README.md §9 一一对应。

import { readFileSync, existsSync } from 'node:fs';
import { createHash } from 'node:crypto';
import { relative } from 'node:path';
import { FEATURE_STATES, TASK_STATES, VERDICTS, PRIORITIES, LAYERS } from './project.mjs';

function push(list, rule, entity, message) {
  list.push({ rule, entity, message });
}

/**
 * 计算台账数据摘要，用于判断 dist 是否落后于 data。
 * @param {Record<string, any>} project loadProject 的结果
 * @returns {string} 12 位摘要
 */
export function dataDigest(project) {
  const files = [...project.features, ...project.tasks, ...project.integrations, ...project.acceptances, ...project.runs]
    .map((entry) => entry.file)
    .sort();
  const hash = createHash('sha1');
  for (const file of files) hash.update(relative(project.root, file)).update('\0').update(readFileSync(file)).update('\0');
  return hash.digest('hex').slice(0, 12);
}

/**
 * 读取已生成看板中记录的摘要。
 * @param {string} distPath 看板文件路径
 * @returns {string | null}
 */
export function distDigest(distPath) {
  if (!existsSync(distPath)) return null;
  const html = readFileSync(distPath, 'utf8');
  const match = html.match(/<meta name="status-digest" content="([^"]+)"/);
  return match ? match[1] : null;
}

/**
 * 执行全部校验。
 * @param {Record<string, any>} project loadProject 的结果
 * @param {Record<string, any>} view buildView 的结果
 * @param {string} distPath 看板文件绝对路径
 * @param {{ignoreDist?: boolean}} [options] ignoreDist 用于生成阶段跳过看板同步检查
 * @returns {{errors: Array, warnings: Array, digest: string, distDigest: string | null}}
 */
export function validate(project, view, distPath, options = {}) {
  const errors = [];
  const warnings = [];
  const featureMap = new Map(view.features.map((f) => [f.id, f]));
  const taskMap = new Map(view.tasks.map((t) => [t.id, t]));
  const acceptanceMap = new Map(view.acceptances.map((a) => [a.id, a]));
  const integrationMap = new Map(view.integrations.map((i) => [i.id, i]));

  for (const feature of view.features) {
    const { id, raw } = feature;
    if (!/^F-\d{3}$/.test(id)) push(errors, 0, id, `功能点 id 必须是 F-三位数字，当前为 ${id}`);
    if (raw.schemaVersion !== 1) push(errors, 0, id, `schemaVersion 必须是 1，当前为 ${raw.schemaVersion}`);
    if (!feature.file.endsWith(`${id}.yml`)) push(errors, 0, id, `文件名必须与 id 一致：${feature.file}`);
    if (!FEATURE_STATES.includes(feature.status)) push(errors, 0, id, `未知状态 ${feature.status}`);
    if (!PRIORITIES.includes(feature.priority)) push(errors, 0, id, `未知优先级 ${feature.priority}，只允许 P0–P3`);
    if (!LAYERS.includes(feature.layer)) push(errors, 0, id, `未知 layer ${feature.layer}`);
    if (!feature.title) push(errors, 0, id, '缺少 title');

    for (const ref of raw.tasks || []) {
      if (!taskMap.has(ref)) push(errors, 1, id, `tasks 引用了不存在的任务 ${ref}`);
    }
    if (raw.acceptance && !acceptanceMap.has(raw.acceptance)) push(errors, 1, id, `acceptance 引用了不存在的验收单 ${raw.acceptance}`);
    if (raw.integrationCase && !integrationMap.has(raw.integrationCase)) push(errors, 1, id, `integrationCase 引用了不存在的 case ${raw.integrationCase}`);
    for (const ref of raw.dependsOn || []) {
      if (!featureMap.has(ref)) push(errors, 1, id, `dependsOn 引用了不存在的功能点 ${ref}`);
    }

    if (feature.status === 'cancelled' && !raw.cancelReason) push(errors, 2, id, 'cancelled 必须填写 cancelReason');
    if (['planned', 'in_progress', 'implemented', 'testing', 'accepted'].includes(feature.status) && feature.tasks.length === 0) {
      push(errors, 2, id, `状态为 ${feature.status} 但没有关联任何任务`);
    }

    if (feature.status === 'accepted') {
      if (!feature.acceptance) {
        push(errors, 3, id, 'accepted 但没有验收单');
      } else if (!['pass', 'conditional'].includes(feature.acceptance.data.verdict)) {
        push(errors, 2, id, `accepted 但验收单结论为 ${feature.acceptance.data.verdict}`);
      }
      if (feature.runs.length === 0) push(errors, 2, id, 'accepted 但没有任何测试运行记录覆盖该功能点');
      if (feature.integration && feature.integration.data.needChain === true && feature.integration.data.review !== 'approved') {
        push(errors, 2, id, `accepted 但联调 case ${feature.integration.data.id} 未评审通过`);
      }
    }

    if (['implemented', 'testing', 'accepted'].includes(feature.status)) {
      if (!feature.code) {
        push(warnings, 5, id, '状态已到 implemented 及以上，但 paths 内没有任何提交');
      } else if (feature.stale) {
        push(warnings, 5, id, `状态为 ${feature.status}，但 paths 最近 ${view.staleCommitDays} 天无提交（最后 ${feature.code.shortSha} @ ${feature.code.date.slice(0, 10)}）`);
      }
      const latestRunAt = feature.runs
        .map((run) => Date.parse((run && run.data && run.data.at) || ''))
        .filter((value) => Number.isFinite(value))
        .sort((a, b) => b - a)[0];
      const latestCommitAt = Date.parse(feature.code.date || '');
      if (Number.isFinite(latestRunAt) && Number.isFinite(latestCommitAt) && latestRunAt < latestCommitAt) {
        push(warnings, 8, id, `代码提交（${String(feature.code.date).slice(0, 10)}）晚于最近一次测试运行（${new Date(latestRunAt).toISOString().slice(0, 10)}），请重新验证并记录`);
      }
    }

    if (feature.missingTasks) push(warnings, 6, id, `未完成且没有安排任务（界面标记为未排期）`);
  }

  for (const task of view.tasks) {
    const { id, raw } = task;
    if (!/^T-\d{3}$/.test(id)) push(errors, 0, id, `任务 id 必须是 T-三位数字`);
    if (!TASK_STATES.includes(task.status)) push(errors, 0, id, `未知任务状态 ${task.status}`);
    if (!featureMap.has(task.feature)) push(errors, 1, id, `任务引用了不存在的功能点 ${task.feature}`);
    if (task.priority && !PRIORITIES.includes(task.priority)) push(errors, 0, id, `未知优先级 ${task.priority}`);
    if (raw.schemaVersion !== 1) push(errors, 0, id, 'schemaVersion 必须是 1');
    if (task.status === 'done' && !task.doneAt && !task.commit) push(warnings, 7, id, 'done 但既没有 doneAt 也没有 commit');
  }

  for (const acceptance of view.acceptances) {
    if (!/^A-\d{3}$/.test(acceptance.id)) push(errors, 0, acceptance.id, `验收单 id 必须是 A-三位数字`);
    if (!VERDICTS.includes(acceptance.verdict)) push(errors, 0, acceptance.id, `未知验收结论 ${acceptance.verdict}`);
    if (!featureMap.has(acceptance.feature)) push(errors, 1, acceptance.id, `验收单引用了不存在的功能点 ${acceptance.feature}`);
    for (const runId of acceptance.testRuns || []) {
      if (!view.runs.some((run) => run.id === runId)) push(errors, 1, acceptance.id, `testRuns 引用了不存在的运行记录 ${runId}`);
    }
    if (acceptance.verdict === 'conditional' && (acceptance.openIssues || []).length === 0) {
      push(warnings, 8, acceptance.id, '有条件通过但没有列出 openIssues');
    }
    for (const section of ['## 1', '## 2', '## 3', '## 4', '## 5']) {
      if (!acceptance.body || !acceptance.body.includes(section)) {
        push(warnings, 8, acceptance.id, `验收单正文缺少 ${section} 小节`);
        break;
      }
    }
  }

  for (const integration of view.integrations) {
    if (!/^IT-\d{3}$/.test(integration.id)) push(errors, 0, integration.id, `联调 case id 必须是 IT-三位数字`);
    for (const featureId of integration.features || []) {
      if (!featureMap.has(featureId)) push(errors, 1, integration.id, `引用了不存在的功能点 ${featureId}`);
    }
    if (integration.needChain === true && integration.review !== 'approved') {
      push(warnings, 2, integration.id, `需要打通链路但评审状态为 ${integration.review}`);
    }
  }

  const digest = dataDigest(project);
  const stored = distDigest(distPath);
  if (!options.ignoreDist) {
    if (stored === null) {
      push(errors, 4, 'dist', '还没有生成看板，请执行 status build');
    } else if (stored !== digest) {
      push(errors, 4, 'dist', `看板已落后于台账（dist ${stored} ≠ data ${digest}），请重新执行 status build`);
    }
  }

  return { errors, warnings, digest, distDigest: stored };
}
