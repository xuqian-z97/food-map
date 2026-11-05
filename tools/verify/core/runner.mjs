// runner：顺序执行步骤、收集 expected/actual、决定退出码。
//
// 所有适配器错误都在这里转成可读的步骤失败，不把堆栈抛给 CLI。

import { performance } from 'node:perf_hooks';
import { evaluateExpectations, formatValue } from './assert.mjs';
import { interpolate, variableScope } from './scenario.mjs';
import { runHttp } from '../adapters/http.mjs';
import { runSql } from '../adapters/sql.mjs';
import { runRedis } from '../adapters/redis.mjs';
import { runCommand } from '../adapters/command.mjs';
import { runLogfile } from '../adapters/logfile.mjs';
import { runApp } from '../adapters/app.mjs';

const ACTIONS = ['http', 'sql', 'redis', 'command', 'sleep', 'logfile', 'app'];

function actionOf(step) {
  return ACTIONS.find((action) => Object.hasOwn(step, action));
}

function compact(text, limit = 110) {
  const value = String(text).replace(/\s+/g, ' ').trim();
  return value.length > limit ? `${value.slice(0, limit)}…` : value;
}

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function getByPath(root, path) {
  let current = root;
  for (const part of path.split('.')) {
    if (current === null || current === undefined) return { found: false, value: undefined };
    if (Array.isArray(current) && /^\d+$/.test(part)) {
      const index = Number(part);
      if (index >= current.length) return { found: false, value: undefined };
      current = current[index];
      continue;
    }
    if (!Object.hasOwn(Object(current), part)) return { found: false, value: undefined };
    current = current[part];
  }
  return { found: true, value: current };
}

async function executeStep(step, action, context) {
  if (action === 'http') return runHttp(step, context);
  if (action === 'sql') return runSql(step, context);
  if (action === 'redis') return runRedis(step, context);
  if (action === 'command') return runCommand(step, context);
  if (action === 'logfile') return runLogfile(step, context);
  if (action === 'app') return runApp(step, context);
  if (action === 'sleep') {
    const ms = Number(step.sleep);
    if (!Number.isFinite(ms) || ms < 0) {
      const error = new Error(`sleep 需要非负毫秒数，实际是 ${JSON.stringify(step.sleep)}`);
      error.hint = '写法：sleep: 200。';
      throw error;
    }
    await delay(ms);
    return {
      display: `sleep ${ms}ms`,
      actual: { value: ms, durationMs: ms },
      summary: `${ms}ms`,
    };
  }
  const error = new Error(`未知动作 ${action}`);
  error.hint = '检查 scenario 步骤格式。';
  throw error;
}

function failedCaptureChecks(step, result) {
  const checks = [];
  const captured = [];
  for (const [name, path] of Object.entries(step.capture || {})) {
    const root = Object.hasOwn(result, 'captureRoot') ? result.captureRoot : result.actual;
    const found = getByPath(root, path);
    if (!found.found || found.value === undefined || found.value === null) {
      checks.push({
        name: `capture ${name}`,
        expected: `${name} <- ${path}`,
        actual: '<missing>',
        ok: false,
        error: `capture 路径不存在：${path}`,
        message: `capture ${name} 失败：期望路径 ${path} 存在，实际未取到值`,
      });
      continue;
    }
    captured.push({ name, value: found.value });
  }
  return { checks, captured };
}

function logFailure(log, checks, error) {
  if (error) {
    log(`      error: ${error.message}`);
    log(`      expected: 步骤可执行`);
    log(`      actual: ${formatValue(error.actual !== undefined ? error.actual : error.message)}`);
    if (error.hint) log(`      hint: ${error.hint}`);
  }
  for (const check of checks.filter((item) => !item.ok)) {
    log(`      ${check.name}: expected ${formatValue(check.expected)} / actual ${formatValue(check.actual)}`);
    if (check.error) log(`      detail: ${check.error}`);
  }
}

/**
 * 执行 scenario。无论步骤失败与否都会继续执行剩余步骤。
 * @param {Record<string, any>} scenario loadScenario 的结果
 * @param {{log?: (line: string) => void}} [options]
 * @returns {Promise<Record<string, any>>} 结构化结果
 */
export async function runScenario(scenario, options = {}) {
  const log = options.log || (() => {});
  if (!scenario.vars) scenario.vars = {};
  const startedDate = new Date();
  const started = performance.now();
  const total = scenario.steps.length;
  const steps = [];
  let failed = 0;

  for (let index = 0; index < total; index += 1) {
    const indexStarted = performance.now();
    const rawStep = scenario.steps[index];
    let step;
    let action = scenario.actions[index] || actionOf(rawStep);
    let display = `${action} <未能解析>`;
    let actual = null;
    let expected = null;
    let checks = [];
    let error = null;
    let summary = '';
    let ok = false;

    try {
      step = interpolate(rawStep, variableScope(scenario), `steps[${index}]`);
      expected = step.expect || null;
      const result = await executeStep(step, action, { config: scenario.config, scenario });
      display = result.display;
      actual = result.actual;
      summary = result.summary || '';
      const assertion = evaluateExpectations(step.expect || {}, actual);
      checks = [...assertion.checks];
      const captures = failedCaptureChecks(step, result);
      checks.push(...captures.checks);
      for (const capture of captures.captured) scenario.vars[capture.name] = capture.value;
      ok = assertion.ok && captures.checks.length === 0;
    } catch (caught) {
      error = caught instanceof Error ? caught : new Error(String(caught));
      actual = error.actual !== undefined ? error.actual : { error: error.message };
      if (!display || display.endsWith('<未能解析>')) {
        const fallbackStep = step || rawStep;
        const fallbackAction = fallbackStep ? actionOf(fallbackStep) : '';
        display = fallbackAction ? `${fallbackAction} ${compact(fallbackStep[fallbackAction])}` : display;
      }
      ok = false;
    }

    const durationMs = Math.round(performance.now() - indexStarted);
    if (!ok) failed += 1;
    const stepRecord = {
      index: index + 1,
      action,
      display,
      expected,
      actual,
      checks,
      captures: step && step.capture ? Object.keys(step.capture) : [],
      ok,
      durationMs,
      error: error ? error.message : '',
      hint: error && error.hint ? error.hint : '',
    };
    steps.push(stepRecord);

    const status = ok ? `ok${summary ? ` ${summary}` : ''}` : 'FAIL';
    log(`[${index + 1}/${total}] ${display} … ${status} (${durationMs}ms)`);
    if (!ok) logFailure(log, checks, error);
  }

  const finishedDate = new Date();
  const durationMs = Math.round(performance.now() - started);
  const passed = total - failed;
  const result = {
    schemaVersion: 1,
    name: scenario.name,
    description: scenario.description || '',
    runId: scenario.runId,
    source: scenario.source,
    ok: failed === 0,
    exitCode: failed === 0 ? 0 : 1,
    startedAt: startedDate.toISOString(),
    finishedAt: finishedDate.toISOString(),
    durationMs,
    summary: { total, passed, failed },
    variableNames: Object.keys(scenario.vars).sort(),
    steps,
  };
  return result;
}
