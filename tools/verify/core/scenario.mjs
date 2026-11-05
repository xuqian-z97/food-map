// scenario 加载、静态校验与变量替换。
//
// scenario 是唯一允许出现项目细节的地方：端口、账号、SQL、Redis key、设备 UDID
// 都写在 scenario 或其 --config 文件中，核心代码只消费解析后的结构。

import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { randomUUID } from 'node:crypto';
import { parseYaml } from './yaml.mjs';

export const ACTIONS = ['http', 'sql', 'redis', 'command', 'sleep', 'logfile', 'app'];
export const ASSERTIONS = ['status', 'equals', 'contains', 'matches', 'exitCode', 'stdoutContains', 'gte', 'lte', 'json'];

const ROOT_KEYS = new Set(['name', 'description', 'config', 'vars', 'steps']);
const STEP_FIELDS = new Set([...ACTIONS, 'json', 'headers', 'body', 'expect', 'capture', 'env']);
const VARIABLE = /\$\{([A-Za-z_][A-Za-z0-9_.-]*)\}/g;
const CAPTURE_NAME = /^[A-Za-z_][A-Za-z0-9_.-]*$/;

export class ScenarioError extends Error {
  constructor(message, hint = '') {
    super(message);
    this.name = 'ScenarioError';
    this.hint = hint;
    this.exitCode = 2;
  }
}

function isPlainObject(value) {
  return value !== null && typeof value === 'object' && !Array.isArray(value);
}

function typeOf(value) {
  if (value === null) return 'null';
  if (Array.isArray(value)) return 'list';
  return typeof value;
}

function assertPlainObject(value, where) {
  if (!isPlainObject(value)) {
    throw new ScenarioError(`${where} 必须是映射，实际是 ${typeOf(value)}`);
  }
}

function readYamlFile(file, label) {
  let text;
  try {
    text = readFileSync(file, 'utf8');
  } catch (error) {
    throw new ScenarioError(`无法读取${label} ${file}：${error.message}`, '检查路径、权限和文件是否存在。');
  }
  try {
    return parseYaml(text);
  } catch (error) {
    throw new ScenarioError(`${label} YAML 解析失败（${file}）：${error.message}`, '检查缩进、冒号和引号，本工具只支持受限 YAML 子集。');
  }
}

function deepMerge(base, override) {
  if (!isPlainObject(base) || !isPlainObject(override)) return override;
  const out = { ...base };
  for (const [key, value] of Object.entries(override)) {
    out[key] = isPlainObject(value) && isPlainObject(base[key]) ? deepMerge(base[key], value) : value;
  }
  return out;
}

function scalarConfig(config) {
  const out = {};
  for (const [key, value] of Object.entries(config)) {
    if (value === null || ['string', 'number', 'boolean'].includes(typeof value)) out[key] = value;
  }
  return out;
}

function checkVariableValues(vars) {
  assertPlainObject(vars, 'vars');
  for (const [key, value] of Object.entries(vars)) {
    if (!['string', 'number', 'boolean'].includes(typeof value)) {
      throw new ScenarioError(`vars.${key} 必须是字符串、数字或布尔值，实际是 ${typeOf(value)}`, '复杂结构请放进 config。');
    }
  }
}

function asNumeric(value, where) {
  if (typeof value === 'number' && Number.isFinite(value)) return value;
  if (typeof value === 'string' && value.trim() !== '' && Number.isFinite(Number(value))) return Number(value);
  throw new ScenarioError(`${where} 必须是数字，实际是 ${JSON.stringify(value)}`);
}

function checkStringList(value, where) {
  const list = Array.isArray(value) ? value : [value];
  if (list.length === 0) throw new ScenarioError(`${where} 不能是空列表`);
  for (const item of list) {
    if (typeof item !== 'string') {
      throw new ScenarioError(`${where} 的每一项必须是字符串，实际有一项是 ${typeOf(item)}`);
    }
  }
}

function checkExpectation(name, value, where) {
  if (name === 'status' || name === 'exitCode') {
    if (typeof value === 'string' && value.includes('${')) return;
    asNumeric(value, where);
    return;
  }
  if (name === 'gte' || name === 'lte') {
    if (typeof value === 'string' && value.includes('${')) return;
    asNumeric(value, where);
    return;
  }
  if (name === 'contains' || name === 'matches' || name === 'stdoutContains') {
    checkStringList(value, where);
    if (name === 'matches') {
      for (const pattern of Array.isArray(value) ? value : [value]) {
        if (pattern.includes('${')) continue;
        try {
          new RegExp(pattern);
        } catch (error) {
          throw new ScenarioError(`${where} 不是合法正则：${error.message}`, `检查正则：${pattern}`);
        }
      }
    }
    return;
  }
  if (name === 'json') {
    assertPlainObject(value, where);
    return;
  }
  // equals 接受任意可 JSON 化的标量/结构。
}

function validateStep(step, index) {
  const where = `steps[${index}]`;
  assertPlainObject(step, where);
  const unknown = Object.keys(step).filter((key) => !STEP_FIELDS.has(key));
  if (unknown.length > 0) {
    throw new ScenarioError(`${where} 包含未知字段：${unknown.join(', ')}`, `可用字段：${[...STEP_FIELDS].join(', ')}`);
  }
  const actions = ACTIONS.filter((action) => Object.hasOwn(step, action));
  if (actions.length === 0) {
    throw new ScenarioError(`${where} 缺少动作，期望以下之一：${ACTIONS.join(', ')}`);
  }
  if (actions.length > 1) {
    throw new ScenarioError(`${where} 只能包含一个动作，实际包含：${actions.join(', ')}`);
  }
  const action = actions[0];

  if (action === 'http') {
    if (typeof step.http !== 'string' || !/^[A-Za-z]+\s+\S/.test(step.http.trim())) {
      throw new ScenarioError(`${where}.http 必须是 "METHOD path" 形式，实际是 ${JSON.stringify(step.http)}`, '例如 "POST /api/auth/login" 或 "GET http://127.0.0.1:8080/health"。');
    }
  } else if (action === 'sleep') {
    const value = step.sleep;
    if (typeof value === 'string' && value.includes('${')) {
      // 运行时再校验替换后的值。
    } else {
      step.sleep = asNumeric(value, `${where}.sleep`);
      if (step.sleep < 0) throw new ScenarioError(`${where}.sleep 不能为负数`);
    }
  } else if (typeof step[action] !== 'string' || step[action].trim() === '') {
    throw new ScenarioError(`${where}.${action} 必须是非空字符串，实际是 ${JSON.stringify(step[action])}`);
  }

  if (step.headers !== undefined) {
    if (action !== 'http') throw new ScenarioError(`${where}.headers 只能用于 http 步骤`);
    assertPlainObject(step.headers, `${where}.headers`);
    for (const [name, value] of Object.entries(step.headers)) {
      if (!['string', 'number', 'boolean'].includes(typeof value)) {
        throw new ScenarioError(`${where}.headers.${name} 必须是标量，实际是 ${typeOf(value)}`);
      }
    }
  }
  if (step.json !== undefined && action !== 'http') {
    throw new ScenarioError(`${where}.json 只能用于 http 步骤`);
  }
  if (step.body !== undefined) {
    if (action !== 'http') throw new ScenarioError(`${where}.body 只能用于 http 步骤`);
    if (typeof step.body !== 'string') throw new ScenarioError(`${where}.body 必须是字符串`);
  }
  if (step.body !== undefined && step.json !== undefined) {
    throw new ScenarioError(`${where} 不能同时设置 body 和 json`, 'json 会自动序列化并设置 content-type；纯文本/表单请用 body。');
  }
  if (step.env !== undefined) {
    if (action !== 'app') throw new ScenarioError(`${where}.env 只能用于 app 步骤`);
    assertPlainObject(step.env, `${where}.env`);
    for (const [name, value] of Object.entries(step.env)) {
      if (!['string', 'number', 'boolean'].includes(typeof value)) {
        throw new ScenarioError(`${where}.env.${name} 必须是标量，实际是 ${typeOf(value)}`);
      }
    }
  }
  if (step.expect !== undefined) {
    assertPlainObject(step.expect, `${where}.expect`);
    for (const [name, value] of Object.entries(step.expect)) {
      if (!ASSERTIONS.includes(name)) {
        throw new ScenarioError(`${where}.expect 包含未知断言 ${name}`, `支持：${ASSERTIONS.join(', ')}`);
      }
      checkExpectation(name, value, `${where}.expect.${name}`);
    }
  }
  if (step.capture !== undefined) {
    assertPlainObject(step.capture, `${where}.capture`);
    for (const [name, path] of Object.entries(step.capture)) {
      if (!CAPTURE_NAME.test(name)) {
        throw new ScenarioError(`${where}.capture 的变量名非法：${name}`, '变量名需以字母或下划线开头，只含字母、数字、_、.、-。');
      }
      if (typeof path !== 'string' || path.trim() === '') {
        throw new ScenarioError(`${where}.capture.${name} 必须是非空路径字符串`);
      }
    }
  }
  return action;
}

function validateScenario(raw) {
  assertPlainObject(raw, 'scenario 根节点');
  const unknown = Object.keys(raw).filter((key) => !ROOT_KEYS.has(key));
  if (unknown.length > 0) {
    throw new ScenarioError(`scenario 根节点包含未知字段：${unknown.join(', ')}`, `可用字段：${[...ROOT_KEYS].join(', ')}`);
  }
  if (typeof raw.name !== 'string' || raw.name.trim() === '') {
    throw new ScenarioError('scenario.name 必须是非空字符串');
  }
  if (raw.config !== undefined) assertPlainObject(raw.config, 'config');
  if (raw.vars !== undefined) checkVariableValues(raw.vars);
  if (!Array.isArray(raw.steps) || raw.steps.length === 0) {
    throw new ScenarioError(`scenario.steps 必须是非空列表，实际是 ${typeOf(raw.steps)}`);
  }
  const actions = raw.steps.map((step, index) => validateStep(step, index));
  return { actions };
}

function collectCaptures(steps) {
  const names = new Set();
  for (const step of steps) {
    for (const name of Object.keys(step.capture || {})) names.add(name);
  }
  return names;
}

function collectPlaceholders(value, where, out) {
  if (typeof value === 'string') {
    for (const match of value.matchAll(VARIABLE)) out.push({ name: match[1], where });
    return;
  }
  if (Array.isArray(value)) {
    value.forEach((item, index) => collectPlaceholders(item, `${where}[${index}]`, out));
    return;
  }
  if (isPlainObject(value)) {
    for (const [key, item] of Object.entries(value)) collectPlaceholders(item, `${where}.${key}`, out);
  }
}

function validatePlaceholders(steps, known, captures) {
  const found = [];
  steps.forEach((step, index) => collectPlaceholders(step, `steps[${index}]`, found));
  for (const { name, where } of found) {
    if (known.has(name) || captures.has(name)) continue;
    throw new ScenarioError(
      `变量 ${name} 未定义（位置：${where}）`,
      `在 scenario.vars 中定义 ${name}，或用 --var ${name}=... 传入；也可以放在顶层 config 中。`,
    );
  }
}

/**
 * 替换字符串中的 ${name}。变量可来自 vars/CLI --var，也可来自顶层 config 标量字段。
 * @param {unknown} value 待替换值
 * @param {Record<string, unknown>} scope 变量作用域
 * @param {string} [where] 出错位置
 * @returns {unknown} 替换后的值
 */
export function interpolate(value, scope, where = 'value') {
  if (typeof value === 'string') {
    return value.replace(VARIABLE, (match, name) => {
      if (!Object.hasOwn(scope, name) || scope[name] === undefined) {
        throw new ScenarioError(
          `变量 ${name} 未定义（位置：${where}）`,
          `检查 scenario.vars、--var ${name}=... 或顶层 config；capture 变量必须在引用前成功产生。`,
        );
      }
      return String(scope[name]);
    });
  }
  if (Array.isArray(value)) return value.map((item, index) => interpolate(item, scope, `${where}[${index}]`));
  if (isPlainObject(value)) {
    const out = {};
    for (const [key, item] of Object.entries(value)) out[key] = interpolate(item, scope, `${where}.${key}`);
    return out;
  }
  return value;
}

/**
 * 为一步构造运行时变量作用域：顶层 config 标量兜底，vars 优先。
 * @param {{config: Record<string, unknown>, vars: Record<string, unknown>}} scenario
 * @returns {Record<string, unknown>}
 */
export function variableScope(scenario) {
  return { ...scalarConfig(scenario.config || {}), ...(scenario.vars || {}) };
}

function defaultRunId() {
  const stamp = new Date().toISOString().replace(/[-:]/g, '').replace(/\.\d{3}Z$/, 'Z');
  return `run-${stamp}-${randomUUID().slice(0, 8)}`;
}

/**
 * 加载并校验 scenario，合并 --config，计算变量作用域。
 * @param {string} file scenario 路径
 * @param {{configPath?: string, vars?: Record<string, string>, runId?: string}} [options]
 * @returns {Promise<Record<string, any>>} 归一化 scenario
 */
export async function loadScenario(file, options = {}) {
  const source = resolve(file);
  const raw = readYamlFile(source, 'scenario 文件');
  const { actions } = validateScenario(raw);
  const scenarioDir = dirname(source);

  let override = {};
  if (options.configPath) {
    const configSource = resolve(options.configPath);
    override = readYamlFile(configSource, '--config 文件');
    assertPlainObject(override, '--config 根节点');
  }
  const mergedConfig = deepMerge(raw.config || {}, override); // --config 覆盖 scenario.config

  const vars = {
    runId: defaultRunId(),
    ...(raw.vars || {}),
    ...(options.vars || {}),
  };
  const config = interpolate(mergedConfig, vars, 'config');
  const captures = collectCaptures(raw.steps);
  const known = new Set([...Object.keys(scalarConfig(config)), ...Object.keys(vars)]);
  validatePlaceholders(raw.steps, known, captures);

  return {
    name: raw.name.trim(),
    description: raw.description || '',
    config,
    vars,
    steps: raw.steps,
    actions,
    captures,
    source,
    dir: scenarioDir,
    runId: vars.runId,
  };
}
