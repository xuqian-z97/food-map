// 台账加载与派生视图：把散在 features/tasks/acceptance/integration/runs 的文件
// 组合成看板需要的结构，并补上 git 事实（不写入任何数据）。

import { readFileSync, existsSync, readdirSync } from 'node:fs';
import { join, resolve } from 'node:path';
import { parseYaml } from './yaml.mjs';
import { lastCommitForPaths, headInfo } from './git.mjs';

export const FEATURE_KEYS = [
  'schemaVersion', 'id', 'title', 'layer', 'phase', 'priority', 'status',
  'summary', 'backendModules', 'frontendModules', 'paths', 'tasks',
  'integrationCase', 'acceptance', 'dependsOn', 'cancelReason', 'notes',
];

export const TASK_KEYS = [
  'schemaVersion', 'id', 'feature', 'title', 'status', 'priority', 'owner',
  'startedAt', 'doneAt', 'commit', 'planRef', 'notes',
];

export const INTEGRATION_KEYS = [
  'schemaVersion', 'id', 'title', 'features', 'services', 'needChain',
  'review', 'reviewedAt', 'reviewNote', 'scenarios',
];

export const ACCEPTANCE_KEYS = [
  'schemaVersion', 'id', 'feature', 'level', 'verdict', 'signedBy', 'signedAt',
  'build', 'testRuns', 'evidence', 'openIssues',
];

export const FEATURE_STATES = ['todo', 'planned', 'in_progress', 'implemented', 'testing', 'accepted', 'cancelled'];
export const TASK_STATES = ['todo', 'doing', 'done', 'dropped'];
export const VERDICTS = ['pending', 'pass', 'conditional', 'fail', 'blocked'];
export const PRIORITIES = ['P0', 'P1', 'P2', 'P3'];
export const LAYERS = ['backend', 'frontend', 'fullstack'];

export const STATE_LABELS = {
  todo: '未开始',
  planned: '已排期',
  in_progress: '进行中',
  implemented: '已实现·待测试',
  testing: '待验收',
  accepted: '已完成',
  cancelled: '已取消',
};

export const PRIORITY_LABELS = { P0: 'P0 最高', P1: 'P1 高', P2: 'P2 中', P3: 'P3 低' };

const PRIORITY_RANK = { P0: 0, P1: 1, P2: 2, P3: 3 };

export function priorityRank(priority) {
  return priority in PRIORITY_RANK ? PRIORITY_RANK[priority] : 9;
}

function readDirFiles(dir, pattern) {
  if (!existsSync(dir)) return [];
  return readdirSync(dir)
    .filter((name) => pattern.test(name))
    .sort()
    .map((name) => join(dir, name));
}

function readFrontMatter(text) {
  const match = text.match(/^---\r?\n([\s\S]*?)\r?\n---\r?\n?/);
  if (!match) return { meta: {}, body: text };
  return { meta: parseYaml(match[1]), body: text.slice(match[0].length) };
}

/**
 * 读取配置。配置路径相对仓库根。
 * @param {string} root 仓库根目录
 * @param {string} [configPath] 相对路径
 * @returns {Record<string, any>}
 */
export function loadConfig(root, configPath = 'status/config/foodmap.json') {
  const file = resolve(root, configPath);
  const text = readFileSync(file, 'utf8');
  return file.endsWith('.json') ? JSON.parse(text) : parseYaml(text);
}

/**
 * 加载全部台账实体。文件缺失时返回空集合，由校验器报告。
 * @param {string} root 仓库根目录
 * @param {string} [configPath] 相对路径
 * @returns {Record<string, any>}
 */
export function loadProject(root, configPath) {
  const config = loadConfig(root, configPath);
  const dataDir = resolve(root, config.paths.data || 'status/data');

  const features = readDirFiles(join(dataDir, 'features'), /^F-.*\.yml$/).map((file) => ({
    file,
    data: parseYaml(readFileSync(file, 'utf8')),
  }));
  const tasks = readDirFiles(join(dataDir, 'tasks'), /^T-.*\.yml$/).map((file) => ({
    file,
    data: parseYaml(readFileSync(file, 'utf8')),
  }));
  const integrations = readDirFiles(join(dataDir, 'integration'), /^IT-.*\.yml$/).map((file) => ({
    file,
    data: parseYaml(readFileSync(file, 'utf8')),
  }));
  const acceptances = readDirFiles(join(dataDir, 'acceptance'), /^A-.*\.md$/).map((file) => {
    const text = readFileSync(file, 'utf8');
    const { meta, body } = readFrontMatter(text);
    return { file, data: meta, body };
  });
  const runs = readDirFiles(join(dataDir, 'runs'), /\.json$/).map((file) => ({
    file,
    data: JSON.parse(readFileSync(file, 'utf8')),
  }));

  return { root, config, dataDir, features, tasks, integrations, acceptances, runs };
}

function indexBy(entries) {
  const map = new Map();
  for (const entry of entries) map.set(entry.data.id, entry);
  return map;
}

/**
 * 派生看板视图：解析引用、采集 git 事实、计算"为什么还没完成"。
 * @param {Record<string, any>} project loadProject 的结果
 * @returns {Record<string, any>}
 */
export function buildView(project) {
  const { root, config } = project;
  const featureMap = indexBy(project.features);
  const taskMap = indexBy(project.tasks);
  const integrationMap = indexBy(project.integrations);
  const acceptanceMap = indexBy(project.acceptances);
  const staleCommitDays = (config.checks && config.checks.staleCommitDays) || 30;

  const runsByFeature = new Map();
  for (const run of project.runs) {
    for (const featureId of run.data.covers || []) {
      if (!runsByFeature.has(featureId)) runsByFeature.set(featureId, []);
      runsByFeature.get(featureId).push(run);
    }
  }

  const features = [...featureMap.values()].map((entry) => {
    const feature = entry.data;
    const code = lastCommitForPaths(root, feature.paths || []);
    const acceptance = feature.acceptance ? acceptanceMap.get(feature.acceptance) : null;
    const integration = feature.integrationCase ? integrationMap.get(feature.integrationCase) : null;
    const taskRefs = (feature.tasks || []).map((id) => ({ id, task: taskMap.get(id) }));
    const featureRuns = runsByFeature.get(feature.id) || [];
    const blockers = [];

    if (feature.status !== 'accepted' && feature.status !== 'cancelled') {
      if (!acceptance) blockers.push('缺验收单');
      else if (!['pass', 'conditional'].includes(acceptance.data.verdict)) blockers.push(`验收单结论为 ${acceptance.data.verdict}`);
      if (featureRuns.length === 0) blockers.push('缺测试运行证据');
      if (integration && integration.data.needChain === true && integration.data.review !== 'approved') {
        blockers.push(`联调 case ${integration.data.id} 未评审通过`);
      }
      if (!code) blockers.push('paths 内无提交');
    }

    return {
      id: feature.id,
      title: feature.title || '',
      layer: feature.layer || 'backend',
      phase: feature.phase || '',
      priority: feature.priority || 'P3',
      status: feature.status || 'todo',
      summary: feature.summary || '',
      backendModules: feature.backendModules || [],
      frontendModules: feature.frontendModules || [],
      paths: feature.paths || [],
      dependsOn: feature.dependsOn || [],
      raw: feature,
      file: entry.file,
      code,
      acceptance,
      integration,
      runs: featureRuns,
      tasks: taskRefs,
      missingTasks: taskRefs.length === 0 && feature.status !== 'accepted' && feature.status !== 'cancelled',
      blockers,
      stale: ['implemented', 'testing', 'accepted'].includes(feature.status)
        && code
        && (Date.now() - Date.parse(code.date)) / 86400000 > staleCommitDays,
    };
  });

  features.sort((a, b) => priorityRank(a.priority) - priorityRank(b.priority) || a.phase.localeCompare(b.phase) || a.id.localeCompare(b.id));

  const tasks = [...taskMap.values()].map((entry) => {
    const task = entry.data;
    const owner = featureMap.get(task.feature);
    return {
      id: task.id,
      feature: task.feature,
      featureTitle: owner ? owner.data.title : '',
      title: task.title || '',
      status: task.status || 'todo',
      priority: task.priority || 'P3',
      ownerType: task.owner || '',
      startedAt: task.startedAt || '',
      doneAt: task.doneAt || '',
      commit: task.commit || '',
      planRef: task.planRef || '',
      raw: task,
      file: entry.file,
    };
  });

  tasks.sort((a, b) => {
    const doneRank = (t) => (t.status === 'done' ? 1 : 0);
    return doneRank(a) - doneRank(b)
      || priorityRank(a.priority) - priorityRank(b.priority)
      || (a.doneAt || '').localeCompare(b.doneAt || '')
      || a.id.localeCompare(b.id);
  });

  const counts = features.reduce((acc, f) => {
    acc[f.status] = (acc[f.status] || 0) + 1;
    return acc;
  }, {});
  const unfinished = features.filter((f) => f.status !== 'accepted' && f.status !== 'cancelled');

  return {
    config,
    head: headInfo(root),
    features,
    tasks,
    integrations: [...integrationMap.values()].map((entry) => ({ id: entry.data.id, ...entry.data, file: entry.file })),
    acceptances: [...acceptanceMap.values()].map((entry) => ({ ...entry.data, file: entry.file, body: entry.body })),
    runs: project.runs.map((entry) => ({ ...entry.data, file: entry.file })),
    counts,
    unfinished,
    stats: {
      total: features.length,
      accepted: counts.accepted || 0,
      pendingAcceptance: features.filter((f) => f.status === 'testing' || f.status === 'implemented').length,
      missingTasks: unfinished.filter((f) => f.missingTasks).length,
      p0Unfinished: unfinished.filter((f) => f.priority === 'P0').length,
      tasksDone: tasks.filter((t) => t.status === 'done').length,
      tasksTotal: tasks.length,
    },
    staleCommitDays,
  };
}
