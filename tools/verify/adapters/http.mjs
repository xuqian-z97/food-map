// HTTP 适配器：使用 Node 内置 fetch，不依赖 axios 等第三方库。

function headerValue(headers, name) {
  const wanted = name.toLowerCase();
  for (const [key, value] of Object.entries(headers)) {
    if (key.toLowerCase() === wanted) return value;
  }
  return undefined;
}

function timeoutOf(config) {
  const raw = config.httpTimeoutMs ?? 10000;
  const value = Number(raw);
  if (!Number.isFinite(value) || value <= 0) {
    const error = new Error(`config.httpTimeoutMs 必须是正数，实际是 ${JSON.stringify(raw)}`);
    error.hint = '在 scenario.config 或 --config 中设置 httpTimeoutMs（毫秒）。';
    throw error;
  }
  return value;
}

/**
 * 执行 http 步骤。
 * @param {Record<string, any>} step 步骤
 * @param {{config: Record<string, any>}} context 执行上下文
 * @returns {Promise<{display: string, actual: Record<string, any>, captureRoot: unknown, summary: string}>}
 */
export async function runHttp(step, context) {
  const action = String(step.http).trim();
  const match = action.match(/^([A-Za-z]+)\s+(.+)$/);
  if (!match) {
    const error = new Error(`http 动作格式错误，期望 "METHOD path"，实际是 ${JSON.stringify(step.http)}`);
    error.hint = '例如 "POST /api/auth/login"。';
    throw error;
  }
  const method = match[1].toUpperCase();
  const target = match[2].trim();
  const baseUrl = context.config.baseUrl ? String(context.config.baseUrl) : '';
  let url;
  try {
    url = /^[a-z][a-z0-9+.-]*:\/\//i.test(target)
      ? new URL(target).toString()
      : new URL(target, baseUrl).toString();
  } catch (error) {
    const failure = new Error(`http 无法构造 URL：target=${target}，baseUrl=${baseUrl || '<empty>'}`);
    failure.hint = '在 scenario.config 或 --config 中提供 baseUrl，或让 http 动作使用绝对 URL。';
    failure.actual = { target, baseUrl, error: error.message };
    throw failure;
  }

  const headers = {};
  for (const [key, value] of Object.entries(step.headers || {})) headers[key] = String(value);
  let body;
  if (step.json !== undefined) {
    body = JSON.stringify(step.json);
    if (headerValue(headers, 'content-type') === undefined) headers['content-type'] = 'application/json';
  } else if (step.body !== undefined) {
    body = step.body;
  }

  const timeoutMs = timeoutOf(context.config);
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  const started = Date.now();
  let response;
  let text = '';
  try {
    response = await fetch(url, { method, headers, body, signal: controller.signal });
    text = await response.text();
  } catch (error) {
    const reason = controller.signal.aborted
      ? `超时（${timeoutMs}ms）`
      : (error && error.cause && error.cause.message) || (error && error.message) || String(error);
    const stage = response ? '读取响应体' : '请求';
    const failure = new Error(`http ${method} ${url} ${stage}失败：${reason}`);
    failure.hint = '检查 config.baseUrl、服务是否启动、端口/网络是否可达，必要时调整 httpTimeoutMs。';
    failure.actual = { method, url, status: response ? response.status : undefined, error: reason, timeoutMs };
    throw failure;
  } finally {
    clearTimeout(timer);
  }

  const durationMs = Date.now() - started;
  const contentType = response.headers.get('content-type') || '';
  let json;
  let jsonParsed = false;
  let jsonError = '';
  if (text.trim() !== '' && (contentType.toLowerCase().includes('json') || /^[\[{]/.test(text.trim()))) {
    try {
      json = JSON.parse(text);
      jsonParsed = true;
    } catch (error) {
      jsonError = error.message;
    }
  }
  const actual = {
    method,
    url,
    status: response.status,
    statusText: response.statusText,
    headers: Object.fromEntries(response.headers.entries()),
    body: text,
    json,
    jsonParsed,
    jsonError,
    value: jsonParsed ? json : text,
    content: text,
    durationMs,
  };
  return {
    display: `http ${method} ${target}`,
    actual,
    captureRoot: jsonParsed ? json : { body: text },
    summary: String(response.status),
  };
}
