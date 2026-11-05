// 受限 YAML 子集解析与序列化。
//
// 只支持 status/data/README.md §1 规定的写法：key: value、块列表、列表项内标量字段、
// # 注释、单/双引号。不支持流式集合、块标量、锚点、多文档。
//
// 设计取舍：不引入通用 YAML 依赖，避免把 node_modules 带进 Java/Swift 仓库；
// 代价是语法受限，遇到越界写法直接抛错，而不是静默解析成错误结构。

const KEY = /^([A-Za-z0-9_][A-Za-z0-9_.-]*):(?:\s+(.*))?$/;

function stripComment(line) {
  let quote = null;
  for (let i = 0; i < line.length; i += 1) {
    const ch = line[i];
    if (quote) {
      if (ch === quote) quote = null;
      continue;
    }
    if (ch === '"' || ch === "'") {
      quote = ch;
      continue;
    }
    if (ch === '#' && (i === 0 || /\s/.test(line[i - 1]))) return line.slice(0, i);
  }
  return line;
}

function scalar(raw) {
  const text = raw.trim();
  if (text === '') return null;
  if (text.length >= 2) {
    const first = text[0];
    const last = text[text.length - 1];
    if ((first === '"' && last === '"') || (first === "'" && last === "'")) {
      return text.slice(1, -1);
    }
  }
  if (text === 'true') return true;
  if (text === 'false') return false;
  if (text === 'null' || text === '~') return null;
  if (/^-?\d+$/.test(text)) return Number(text);
  if (/^-?\d+\.\d+$/.test(text)) return Number(text);
  return text;
}

function indentOf(text) {
  return text.length - text.trimStart().length;
}

function parseList(lines, start) {
  const items = [];
  let i = start;
  while (i < lines.length) {
    const clean = stripComment(lines[i]);
    if (clean.trim() === '') {
      i += 1;
      continue;
    }
    if (indentOf(clean) === 0) break;
    const text = clean.trim();
    if (!text.startsWith('-')) {
      throw new Error(`第 ${i + 1} 行无法归属到任何列表项：${text}`);
    }
    const body = text.slice(1).trim();
    const head = body.match(KEY);
    if (!head) {
      items.push(scalar(body));
      i += 1;
      continue;
    }
    const record = {};
    record[head[1]] = head[2] === undefined || head[2].trim() === '' ? null : scalar(head[2]);
    i += 1;
    while (i < lines.length) {
      const cont = stripComment(lines[i]);
      if (cont.trim() === '') {
        i += 1;
        continue;
      }
      const contIndent = indentOf(cont);
      const contText = cont.trim();
      if (contIndent <= 2 || contText.startsWith('-')) break;
      const field = contText.match(KEY);
      if (!field) throw new Error(`第 ${i + 1} 行不是列表项的字段：${contText}`);
      record[field[1]] = field[2] === undefined || field[2].trim() === '' ? null : scalar(field[2]);
      i += 1;
    }
    items.push(record);
  }
  return { items, next: i };
}

/**
 * 解析受限 YAML 文本。
 * @param {string} text 文件内容
 * @returns {Record<string, unknown>} 顶层键值对象
 */
export function parseYaml(text) {
  const lines = text.split(/\r?\n/);
  const root = {};
  let i = 0;
  while (i < lines.length) {
    const clean = stripComment(lines[i]);
    if (clean.trim() === '') {
      i += 1;
      continue;
    }
    if (indentOf(clean) !== 0) {
      throw new Error(`第 ${i + 1} 行缩进错误，顶层字段必须顶格：${clean.trim()}`);
    }
    const match = clean.trim().match(KEY);
    if (!match) throw new Error(`第 ${i + 1} 行不是 key: value：${clean.trim()}`);
    const key = match[1];
    const rest = match[2] === undefined ? '' : match[2].trim();
    if (rest !== '') {
      root[key] = scalar(rest);
      i += 1;
      continue;
    }
    const next = lines.slice(i + 1).findIndex((line) => stripComment(line).trim() !== '');
    if (next >= 0) {
      const probe = stripComment(lines[i + 1 + next]);
      if (/^\s*-\s/.test(probe)) {
        const { items, next: after } = parseList(lines, i + 1);
        root[key] = items;
        i = after;
        continue;
      }
    }
    root[key] = null;
    i += 1;
  }
  return root;
}

const RESERVED = new Set(['true', 'false', 'null', '~', '']);

function formatScalar(value) {
  const text = String(value);
  if (RESERVED.has(text.toLowerCase())) return `"${text}"`;
  if (/^-?\d+(\.\d+)?$/.test(text)) return `"${text}"`;
  if (/^[A-Za-z0-9_.@*+=-][A-Za-z0-9_./@*+=#: -]*$/.test(text) && !text.includes(': ') && !text.includes(' #') && !text.trimStart().startsWith('-')) {
    return text;
  }
  if (/^[\p{L}\p{N}_][\p{L}\p{N}_ .\p{Script=Han}-]*$/u.test(text)) return text;
  return `"${text.replace(/\\/g, '\\\\').replace(/"/g, '\\"')}"`;
}

/**
 * 按固定键序序列化受限 YAML；null/undefined/空列表会被省略。
 * @param {Record<string, unknown>} obj 实体对象
 * @param {string[]} keyOrder 期望的字段顺序
 * @returns {string} 文件内容
 */
export function stringifyYaml(obj, keyOrder = []) {
  const keys = [...keyOrder.filter((key) => key in obj), ...Object.keys(obj).filter((key) => !keyOrder.includes(key))];
  const out = [];
  for (const key of keys) {
    const value = obj[key];
    if (value === undefined || value === null) continue;
    if (Array.isArray(value)) {
      if (value.length === 0) continue;
      out.push(`${key}:`);
      for (const item of value) {
        if (item !== null && typeof item === 'object') {
          const entries = Object.entries(item).filter(([, v]) => v !== null && v !== undefined);
          if (entries.length === 0) continue;
          const [first, ...rest] = entries;
          out.push(`  - ${first[0]}: ${formatScalar(first[1])}`);
          for (const [k, v] of rest) out.push(`    ${k}: ${formatScalar(v)}`);
        } else {
          out.push(`  - ${formatScalar(item)}`);
        }
      }
      continue;
    }
    if (typeof value === 'object') throw new Error(`不支持嵌套映射字段：${key}`);
    out.push(`${key}: ${typeof value === 'number' ? value : formatScalar(value)}`);
  }
  return `${out.join('\n')}\n`;
}
