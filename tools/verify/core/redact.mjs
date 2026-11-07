// 证据脱敏：把 Token、密码、手机号等敏感内容替换掉再落盘。
//
// 默认规则覆盖最常见的泄漏形态；scenario 的 config.redact 可以追加项目自有规则。

const DEFAULT_RULES = [
  // Authorization: Bearer xxx
  { name: 'bearer', pattern: /(bearer\s+)[A-Za-z0-9._~+/=-]{8,}/gi, replacement: '$1[REDACTED]' },
  // JWT（header.payload.signature）
  { name: 'jwt', pattern: /\beyJ[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\b/g, replacement: '[REDACTED_JWT]' },
  // 中国大陆手机号
  { name: 'phone', pattern: /(?<!\d)1[3-9]\d{9}(?!\d)/g, replacement: '[REDACTED_PHONE]' },
  // JSON 里的 password / token 字段
  { name: 'json-secret', pattern: /("(?:password|passwordHash|accessToken|refreshToken|token)"\s*:\s*")[^"]{4,}(")/gi, replacement: '$1[REDACTED]$2' },
];

function toRule(entry, index) {
  if (typeof entry === 'string') {
    return { name: `custom-${index + 1}`, pattern: new RegExp(entry, 'g'), replacement: '[REDACTED]' };
  }
  if (entry && typeof entry === 'object') {
    if (typeof entry.pattern !== 'string' || entry.pattern.trim() === '') {
      throw new Error(`config.redact[${index}] 需要 pattern 字符串`);
    }
    return {
      name: entry.name || `custom-${index + 1}`,
      pattern: new RegExp(entry.pattern, entry.flags || 'g'),
      replacement: entry.replacement === undefined ? '[REDACTED]' : String(entry.replacement),
    };
  }
  throw new Error(`config.redact[${index}] 必须是字符串或 { pattern, replacement } 对象`);
}

/**
 * 构造脱敏器。
 * @param {Array<string|Record<string, string>>} [customRules] scenario 里追加的规则
 * @returns {{ redact: (text: string) => string, redactValue: (value: unknown) => unknown, ruleCount: number }}
 */
export function createRedactor(customRules = []) {
  const rules = [...DEFAULT_RULES, ...(customRules || []).map(toRule)];
  const redact = (text) => {
    if (typeof text !== 'string' || text === '') return text;
    let out = text;
    for (const rule of rules) {
      rule.pattern.lastIndex = 0;
      out = out.replace(rule.pattern, rule.replacement);
    }
    return out;
  };
  // 结构化对象里，敏感字段按“键名”整体替换：不透明 Token 和密码没有固定格式，只能靠键名识别。
  const SECRET_KEYS = /(token|password|passwd|secret|credential|authorization|cookie|sessionid)/i;
  const redactValue = (value, key = '') => {
    if (typeof value === 'string') {
      if (SECRET_KEYS.test(key) && value.trim() !== '') return '[REDACTED]';
      return redact(value);
    }
    if (Array.isArray(value)) return value.map((item) => redactValue(item, key));
    if (value && typeof value === 'object') {
      const out = {};
      for (const [name, item] of Object.entries(value)) out[name] = redactValue(item, name);
      return out;
    }
    return value;
  };
  return { redact, redactValue, ruleCount: rules.length };
}

export { DEFAULT_RULES };
