// 断言实现：把 expect 声明和适配器返回的 actual 字段做比较。
//
// 字段约定：status/exitCode 是状态，stdout 是命令标准输出，value 是被测标量，
// content 是文件/日志正文，json 是 HTTP 解析后的 JSON。字符串比较一律 trim。

const MAX_DISPLAY = 500;

function isPlainObject(value) {
  return value !== null && typeof value === 'object' && !Array.isArray(value);
}

function sameValue(actual, expected) {
  if (typeof actual === 'string' && typeof expected === 'string') return actual.trim() === expected.trim();
  if (Array.isArray(actual) && Array.isArray(expected)) {
    return actual.length === expected.length && actual.every((item, index) => sameValue(item, expected[index]));
  }
  if (isPlainObject(actual) && isPlainObject(expected)) {
    const keys = Object.keys(expected);
    if (keys.length !== Object.keys(actual).length) return false;
    return keys.every((key) => Object.hasOwn(actual, key) && sameValue(actual[key], expected[key]));
  }
  return Object.is(actual, expected);
}

function subset(actual, expected) {
  if (isPlainObject(expected) && isPlainObject(actual)) {
    return Object.entries(expected).every(([key, value]) => Object.hasOwn(actual, key) && subset(actual[key], value));
  }
  return sameValue(actual, expected);
}

function resolvePath(root, path) {
  if (path === '') return { found: true, value: root };
  let current = root;
  for (const part of path.split('.')) {
    if (current === null || current === undefined || !Object.hasOwn(Object(current), part)) {
      return { found: false, value: undefined };
    }
    current = current[part];
  }
  return { found: true, value: current };
}

function compileRegex(pattern) {
  const literal = pattern.match(/^\/([\s\S]*)\/([dgimsuy]*)$/);
  return literal ? new RegExp(literal[1], literal[2]) : new RegExp(pattern);
}

function actualOf(name, actual) {
  if (name === 'status') return actual.status;
  if (name === 'exitCode') return actual.exitCode;
  if (name === 'stdoutContains') return actual.stdout;
  if (name === 'json') return actual.json;
  if (name === 'contains' || name === 'matches') {
    return actual.content !== undefined ? actual.content : actual.value;
  }
  return actual.value;
}

function checkOne(name, expected, actual) {
  const target = actualOf(name, actual);
  if (name === 'status' || name === 'exitCode') {
    if (target === undefined || target === null) return { ok: false, expected, actual: target };
    const want = Number(expected);
    const got = Number(target);
    return { ok: Number.isFinite(got) && got === want, expected, actual: target };
  }
  if (name === 'equals') {
    return { ok: sameValue(target, expected), expected, actual: target };
  }
  if (name === 'contains') {
    const wanted = Array.isArray(expected) ? expected : [expected];
    if (Array.isArray(target)) {
      const ok = wanted.every((item) => target.some((candidate) => sameValue(candidate, item)));
      return { ok, expected, actual: target };
    }
    if (target === undefined || target === null) return { ok: false, expected, actual: target };
    const text = String(target).trim();
    return { ok: wanted.every((item) => text.includes(item.trim())), expected, actual: target };
  }
  if (name === 'matches') {
    if (target === undefined || target === null) return { ok: false, expected, actual: target };
    const text = String(target).trim();
    const patterns = Array.isArray(expected) ? expected : [expected];
    let ok = true;
    let failure = '';
    for (const pattern of patterns) {
      try {
        if (!compileRegex(pattern).test(text)) {
          ok = false;
          failure = pattern;
          break;
        }
      } catch (error) {
        ok = false;
        failure = `${pattern}（无效正则：${error.message}）`;
        break;
      }
    }
    return { ok, expected, actual: target, error: ok ? undefined : failure };
  }
  if (name === 'stdoutContains') {
    if (target === undefined || target === null) return { ok: false, expected, actual: target };
    const wanted = Array.isArray(expected) ? expected : [expected];
    const text = String(target).trim();
    return { ok: wanted.every((item) => text.includes(item.trim())), expected, actual: target };
  }
  if (name === 'gte' || name === 'lte') {
    let measured = target;
    let got = typeof measured === 'string' ? Number(measured.trim()) : Number(measured);
    // HTTP 的 value 通常是 JSON 对象；数值断言在这种时候回退到 status。
    if (!Number.isFinite(got) && actual.status !== undefined) {
      measured = actual.status;
      got = Number(measured);
    }
    const want = Number(expected);
    if (!Number.isFinite(got)) return { ok: false, expected, actual: target };
    return { ok: name === 'gte' ? got >= want : got <= want, expected, actual: measured };
  }
  if (name === 'json') {
    const failures = [];
    for (const [path, want] of Object.entries(expected)) {
      const found = resolvePath(target, path);
      if (!found.found || !subset(found.value, want)) failures.push(path);
    }
    return { ok: failures.length === 0, expected, actual: target, error: failures.length > 0 ? `JSON 路径不匹配：${failures.join(', ')}` : undefined };
  }
  return { ok: false, expected, actual: target, error: `未知断言 ${name}` };
}

/**
 * 执行一组 expect 断言。
 * @param {Record<string, unknown>} expect 断言声明
 * @param {Record<string, unknown>} actual 适配器实际值
 * @returns {{ok: boolean, checks: Array<Record<string, unknown>>}}
 */
export function evaluateExpectations(expect, actual) {
  const checks = [];
  for (const [name, expected] of Object.entries(expect || {})) {
    const result = checkOne(name, expected, actual);
    checks.push({
      name,
      expected,
      actual: result.actual,
      ok: result.ok,
      error: result.error || '',
      message: result.ok ? '' : `期望 ${formatValue(expected)}，实际 ${formatValue(result.actual)}${result.error ? `（${result.error}）` : ''}`,
    });
  }
  return { ok: checks.every((check) => check.ok), checks };
}

/**
 * 把值格式化为单行、可读、有上限的文本，供 CLI 和证据文件使用。
 * @param {unknown} value 任意值
 * @returns {string}
 */
export function formatValue(value) {
  let text;
  if (value === undefined) return '<undefined>';
  if (value === null) return 'null';
  if (typeof value === 'string') text = value.trim() === '' ? '(empty)' : value;
  else if (typeof value === 'number' || typeof value === 'boolean') text = String(value);
  else {
    try {
      text = JSON.stringify(value);
    } catch {
      text = String(value);
    }
  }
  text = String(text).replace(/\r?\n/g, '\\n');
  return text.length > MAX_DISPLAY ? `${text.slice(0, MAX_DISPLAY)}…` : text;
}

/**
 * 汇总失败断言，供日志和证据使用。
 * @param {Array<Record<string, any>>} checks 断言结果
 * @returns {string[]}
 */
export function failureLines(checks) {
  return checks.filter((check) => !check.ok).map((check) => `${check.name}: ${check.message}`);
}
