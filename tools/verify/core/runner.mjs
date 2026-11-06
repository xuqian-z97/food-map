// runner：按 setup → steps → teardown 顺序执行、收集 expected/actual、决定退出码。
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
import { runXcode } from '../adapters/xcode.mjs';

const ACTIONS = ['http', 'sql', 'redis', 'command', 'sleep', 'logfile', 'app', 'xcode'];

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
  if (action === 'xcode') return runXcode(step, context);
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

function parseRetry(step) {
  const retry = step && step.retry;
  if (!retry) return null;
  const max = Number(retry.max);
  const intervalMs = retry.intervalMs === undefined ? 500 : Number(retry.intervalMs);
  return {
    max: Number.isFinite(max) && max > 0 ? Math.floor(max) : 1,
    intervalMs: Number.isFinite(intervalMs) && intervalMs >= 0 ? Math.floor(intervalMs) : 500,
  };
}

async function attemptStep(step, action, context, index) {
  const result = await executeStep(step, action, context);
  const assertion = evaluateExpectations(step.expect || {}, result.actual);
  const checks = [...assertion.checks];
  const captures = failedCaptureChecks(step, result);
  checks.push(...captures.checks);
  return {
    result,
    checks,
    captured: captures.captured,
    ok: assertion.ok && captures.checks.length === 0,
  };
}

/**
 * 执行单个步骤（含重试），返回步骤记录。
 * @param {Record<string, any>} rawStep 原始步骤
 * @param {number} index 全局序号（从 0 开始）
 * @param {'setup'|'step'|'teardown'} phase 所属阶段
 * @param {Record<string, any>} scenario 归一化 scenario
 * @param {(line: string) => void} log 输出函数
 * @returns {Promise<Record<string, any>>} 步骤记录
 */
async function runOneStep(rawStep, index, phase, scenario, log, totalInPhase) {
  const indexStarted = performance.now();
  let step = null;
  let action = actionOf(rawStep) || '';
  let display = `${action} <未能解析>`;
  let actual = null;
  let expected = null;
  let checks = [];
  let error = null;
  let summary = '';
  let ok = false;
  let attempts = 0;

  try {
    step = interpolate(rawStep, variableScope(scenario), `${phase}[${index}]`);
    expected = step.expect || null;
    const retry = parseRetry(step);
    const maxAttempts = retry ? retry.max : 1;

    while (attempts < maxAttempts) {
      attempts += 1;
      try {
        const outcome = await attemptStep(step, action, { config: scenario.config, scenario }, index);
        display = outcome.result.display;
        actual = outcome.result.actual;
        summary = outcome.result.summary || '';
        checks = outcome.checks;
        for (const capture of outcome.captured) scenario.vars[capture.name] = capture.value;
        ok = outcome.ok;
        error = null;
      } catch (caught) {
        error = caught instanceof Error ? caught : new Error(String(caught));
        actual = error.actual !== undefined ? error.actual : { error: error.message };
        ok = false;
      }
      if (ok || attempts >= maxAttempts) break;
      if (error) log(`      retry ${attempts}/${maxAttempts - 1}: ${compact(error.message)}`);
      await delay(retry.intervalMs);
    }
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
  const stepRecord = {
    index: index + 1,
    phase,
    action,
    display,
    expected,
    actual,
    checks,
    captures: step && step.capture ? Object.keys(step.capture) : [],
    attempts,
    ok,
    durationMs,
    error: error ? error.message : '',
    hint: error && error.hint ? error.hint : '',
  };

  const label = phase === 'step' ? `${index + 1}/${totalInPhase}` : `${phase} ${index + 1}/${totalInPhase}`;
  const status = ok ? `ok${summary ? ` ${summary}` : ''}` : 'FAIL';
  log(`[${label}] ${display} … ${status} (${durationMs}ms)`);
  if (!ok) logFailure(log, checks, error);
  return stepRecord;
}

/**
 * 执行 scenario：setup（失败即停）→ steps（失败继续）→ teardown（无论成败都执行）。
 * @param {Record<string, any>} scenario loadScenario 的结果
 * @param {{log?: (line: string) => void}} [options]
 * @returns {Promise<Record<string, any>>} 结构化结果
 */
export async function runScenario(scenario, options = {}) {
  const log = options.log || (() => {});
  if (!scenario.vars) scenario.vars = {};
  const startedDate = new Date();
  const started = performance.now();
  const steps = [];
  const setupSteps = scenario.setup || [];
  const mainSteps = scenario.steps || [];
  const teardownSteps = scenario.teardown || [];
  const total = setupSteps.length + mainSteps.length + teardownSteps.length;
  let failed = 0;
  let setupFailed = 0;
  let teardownFailed = 0;
  let skipped = 0;

  for (const [position, rawStep] of setupSteps.entries()) {
    const record = await runOneStep(rawStep, position, 'setup', scenario, log, setupSteps.length);
    if (record.ok) record.index = steps.length + 1;
    steps.push(record);
    if (!record.ok) {
      setupFailed += 1;
      failed += 1;
      const remaining = setupSteps.length - position - 1;
      if (remaining > 0) {
        skipped += remaining;
        log(`      setup 失败，跳过剩余 ${remaining} 个 setup 步骤与全部 steps`);
      }
      break;
    }
  }

  if (setupFailed === 0) {
    for (const [position, rawStep] of mainSteps.entries()) {
      const record = await runOneStep(rawStep, position, 'step', scenario, log, mainSteps.length);
      steps.push(record);
      if (!record.ok) failed += 1;
    }
  } else {
    skipped += mainSteps.length;
  }

  for (const [position, rawStep] of teardownSteps.entries()) {
    const record = await runOneStep(rawStep, position, 'teardown', scenario, log, teardownSteps.length);
    steps.push(record);
    if (!record.ok) {
      teardownFailed += 1;
      failed += 1;
    }
  }

  const finishedDate = new Date();
  const durationMs = Math.round(performance.now() - started);
  const passed = steps.filter((step) => step.ok).length;
  const result = {
    schemaVersion: 2,
    name: scenario.name,
    description: scenario.description || '',
    runId: scenario.runId,
    source: scenario.source,
    ok: failed === 0 && skipped === 0,
    exitCode: failed === 0 && skipped === 0 ? 0 : 1,
    startedAt: startedDate.toISOString(),
    finishedAt: finishedDate.toISOString(),
    durationMs,
    summary: { total, passed, failed, skipped, setupFailed, teardownFailed },
    variableNames: Object.keys(scenario.vars).sort(),
    steps,
  };
  return result;
}
