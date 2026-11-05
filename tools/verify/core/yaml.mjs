// 受限 YAML 子集解析器。
//
// 基于仓库中已有的受限 YAML 子集实现扩展而来，保持零依赖、可整体搬移。
// 支持：嵌套映射、块列表、流式映射/列表（{...} / [...]）、单/双引号字符串、
// 数字/布尔/null 标量、# 注释。不支持：块标量（|、>）、锚点、别名、多文档、
// 复杂键和 YAML 标签。越界写法直接报错，避免把配置静默解析成错误结构。

const KEY_LINE = /^([A-Za-z0-9_][A-Za-z0-9_.-]*):(?:[ \t]+(.*))?$/;
const FLOW_END = new Set([',', ']', '}']);

function stripComment(line) {
  let quote = null;
  for (let i = 0; i < line.length; i += 1) {
    const ch = line[i];
    if (quote) {
      if (quote === '"' && ch === '\\') {
        i += 1;
        continue;
      }
      if (ch === quote) {
        if (quote === "'" && line[i + 1] === "'") {
          i += 1;
          continue;
        }
        quote = null;
      }
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
    if (first === '"' && last === '"') return decodeDoubleQuoted(text.slice(1, -1));
    if (first === "'" && last === "'") return text.slice(1, -1).replace(/''/g, "'");
  }
  if (text === 'true') return true;
  if (text === 'false') return false;
  if (text === 'null' || text === '~') return null;
  if (/^-?\d+$/.test(text)) return Number(text);
  if (/^-?\d+\.\d+$/.test(text)) return Number(text);
  return text;
}

function decodeDoubleQuoted(text) {
  return text.replace(/\\(u[0-9a-fA-F]{4}|x[0-9a-fA-F]{2}|["\\/bfnrt])/g, (match, code) => {
    if (code.length === 1) {
      return { '"': '"', '\\': '\\', '/': '/', b: '\b', f: '\f', n: '\n', r: '\r', t: '\t' }[code];
    }
    return String.fromCharCode(Number.parseInt(code.slice(1), 16));
  });
}

function isSequenceLine(text) {
  return text === '-' || text.startsWith('- ');
}

function looksLikeEntry(text) {
  if (text.startsWith('{') || text.startsWith('[') || text.startsWith('"') || text.startsWith("'")) return false;
  return KEY_LINE.test(text);
}

class FlowParser {
  constructor(text, lineNo) {
    this.text = text;
    this.pos = 0;
    this.lineNo = lineNo;
  }

  error(message) {
    throw new Error(`第 ${this.lineNo} 行流式集合解析失败：${message}`);
  }

  skip() {
    while (this.pos < this.text.length && /\s/.test(this.text[this.pos])) this.pos += 1;
  }

  parseValue() {
    this.skip();
    const ch = this.text[this.pos];
    if (ch === undefined) this.error('缺少值');
    if (ch === '{') return this.parseMap();
    if (ch === '[') return this.parseList();
    if (ch === '"' || ch === "'") return this.parseQuoted();
    return this.parsePlain();
  }

  parseQuoted() {
    const quote = this.text[this.pos];
    this.pos += 1;
    let out = '';
    while (this.pos < this.text.length) {
      const ch = this.text[this.pos];
      if (quote === '"' && ch === '\\') {
        const next = this.text[this.pos + 1];
        if (next === undefined) this.error('字符串以反斜杠结尾');
        if (next === 'u' || next === 'x') {
          const size = next === 'u' ? 4 : 2;
          const digits = this.text.slice(this.pos + 2, this.pos + 2 + size);
          if (!new RegExp(`^[0-9a-fA-F]{${size}}$`).test(digits)) this.error('转义序列非法');
          out += String.fromCharCode(Number.parseInt(digits, 16));
          this.pos += 2 + size;
          continue;
        }
        const mapped = { '"': '"', '\\': '\\', '/': '/', b: '\b', f: '\f', n: '\n', r: '\r', t: '\t' }[next];
        if (mapped === undefined) this.error(`不支持的转义 \\${next}`);
        out += mapped;
        this.pos += 2;
        continue;
      }
      if (quote === "'" && ch === "'" && this.text[this.pos + 1] === "'") {
        out += "'";
        this.pos += 2;
        continue;
      }
      if (ch === quote) {
        this.pos += 1;
        return out;
      }
      out += ch;
      this.pos += 1;
    }
    this.error('字符串缺少结束引号');
  }

  parsePlain() {
    const start = this.pos;
    while (this.pos < this.text.length) {
      const ch = this.text[this.pos];
      // ${...} 是变量占位符，其中的 } 不能当流式集合的结束符。
      if (ch === '$' && this.text[this.pos + 1] === '{') {
        const close = this.text.indexOf('}', this.pos + 2);
        if (close < 0) this.error('变量占位符缺少 }');
        this.pos = close + 1;
        continue;
      }
      if (FLOW_END.has(ch)) break;
      this.pos += 1;
    }
    return scalar(this.text.slice(start, this.pos));
  }

  parseKey() {
    this.skip();
    if (this.text[this.pos] === '"' || this.text[this.pos] === "'") return this.parseQuoted();
    const start = this.pos;
    while (this.pos < this.text.length && this.text[this.pos] !== ':') this.pos += 1;
    if (this.pos >= this.text.length) this.error('映射项缺少冒号');
    const key = this.text.slice(start, this.pos).trim();
    if (!key) this.error('映射键不能为空');
    return key;
  }

  parseMap() {
    this.pos += 1; // {
    const out = {};
    this.skip();
    if (this.text[this.pos] === '}') {
      this.pos += 1;
      return out;
    }
    while (this.pos < this.text.length) {
      const key = this.parseKey();
      if (this.text[this.pos] !== ':') this.error(`键 ${key} 后缺少冒号`);
      this.pos += 1;
      if (Object.hasOwn(out, key)) this.error(`重复的键 ${key}`);
      out[key] = this.parseValue();
      this.skip();
      const ch = this.text[this.pos];
      if (ch === ',') {
        this.pos += 1;
        this.skip();
        if (this.text[this.pos] === '}') {
          this.pos += 1;
          return out;
        }
        continue;
      }
      if (ch === '}') {
        this.pos += 1;
        return out;
      }
      this.error(`映射项 ${key} 后应为 , 或 }，实际为 ${ch === undefined ? '行尾' : ch}`);
    }
    this.error('映射缺少结束符 }');
  }

  parseList() {
    this.pos += 1; // [
    const out = [];
    this.skip();
    if (this.text[this.pos] === ']') {
      this.pos += 1;
      return out;
    }
    while (this.pos < this.text.length) {
      out.push(this.parseValue());
      this.skip();
      const ch = this.text[this.pos];
      if (ch === ',') {
        this.pos += 1;
        this.skip();
        if (this.text[this.pos] === ']') {
          this.pos += 1;
          return out;
        }
        continue;
      }
      if (ch === ']') {
        this.pos += 1;
        return out;
      }
      this.error(`列表项后应为 , 或 ]，实际为 ${ch === undefined ? '行尾' : ch}`);
    }
    this.error('列表缺少结束符 ]');
  }
}

function parseInline(raw, lineNo) {
  const text = raw.trim();
  if (text.startsWith('{') || text.startsWith('[')) {
    const parser = new FlowParser(text, lineNo);
    const value = parser.parseValue();
    parser.skip();
    if (parser.pos !== text.length) {
      throw new Error(`第 ${lineNo} 行流式集合后存在多余内容：${text.slice(parser.pos)}`);
    }
    return value;
  }
  return scalar(text);
}

function splitLines(text) {
  const source = text.split(/\r?\n/);
  const lines = [];
  for (let i = 0; i < source.length; i += 1) {
    const raw = source[i];
    const clean = stripComment(raw);
    if (clean.trim() === '') continue;
    const indent = clean.length - clean.trimStart().length;
    if (clean.slice(0, indent).includes('\t')) {
      throw new Error(`第 ${i + 1} 行缩进包含 Tab；请只使用空格缩进`);
    }
    lines.push({ indent, text: clean.trim(), lineNo: i + 1 });
  }
  return lines;
}

function parseBlock(lines, start, indent) {
  if (start >= lines.length || lines[start].indent !== indent) {
    throw new Error(`第 ${start < lines.length ? lines[start].lineNo : '文件末尾'} 行缩进不符合预期`);
  }
  return isSequenceLine(lines[start].text)
    ? parseList(lines, start, indent)
    : parseMap(lines, start, indent);
}

function parseEntryValue(rest, lines, index, parentIndent) {
  if (rest !== undefined && rest.trim() !== '') {
    return { value: parseInline(rest, lines[index - 1].lineNo), next: index };
  }
  if (index < lines.length && lines[index].indent > parentIndent) {
    const parsed = parseBlock(lines, index, lines[index].indent);
    return { value: parsed.value, next: parsed.next };
  }
  return { value: null, next: index };
}

function parseMapFields(lines, start, indent, seed = null) {
  const out = {};
  let i = start;
  if (seed) {
    if (Object.hasOwn(out, seed.key)) throw new Error(`第 ${seed.lineNo} 行重复的键 ${seed.key}`);
    const entry = parseEntryValue(seed.rest, lines, i, indent);
    out[seed.key] = entry.value;
    i = entry.next;
  }
  while (i < lines.length) {
    const line = lines[i];
    if (line.indent < indent) break;
    if (line.indent > indent) {
      throw new Error(`第 ${line.lineNo} 行缩进错误：期望 ${indent} 个空格，实际 ${line.indent} 个空格`);
    }
    if (isSequenceLine(line.text)) break;
    const match = line.text.match(KEY_LINE);
    if (!match) throw new Error(`第 ${line.lineNo} 行不是 key: value：${line.text}`);
    const key = match[1];
    if (Object.hasOwn(out, key)) throw new Error(`第 ${line.lineNo} 行重复的键 ${key}`);
    i += 1;
    const entry = parseEntryValue(match[2], lines, i, indent);
    out[key] = entry.value;
    i = entry.next;
  }
  return { value: out, next: i };
}

function parseMap(lines, start, indent) {
  return parseMapFields(lines, start, indent);
}

function parseList(lines, start, indent) {
  const items = [];
  let i = start;
  while (i < lines.length) {
    const line = lines[i];
    if (line.indent < indent) break;
    if (line.indent > indent) {
      throw new Error(`第 ${line.lineNo} 行缩进错误：期望 ${indent} 个空格，实际 ${line.indent} 个空格`);
    }
    if (!isSequenceLine(line.text)) break;
    const body = line.text.slice(1).trim();
    i += 1;
    if (body === '') {
      if (i < lines.length && lines[i].indent > indent) {
        const parsed = parseBlock(lines, i, lines[i].indent);
        items.push(parsed.value);
        i = parsed.next;
      } else {
        items.push(null);
      }
      continue;
    }
    if (isSequenceLine(body)) {
      throw new Error(`第 ${line.lineNo} 行不支持嵌套内联列表，请换行缩进书写`);
    }
    if (looksLikeEntry(body)) {
      const match = body.match(KEY_LINE);
      const mapIndent = indent + 2;
      const seed = { key: match[1], rest: match[2], lineNo: line.lineNo };
      const parsed = parseMapFields(lines, i, mapIndent, seed);
      items.push(parsed.value);
      i = parsed.next;
      continue;
    }
    items.push(parseInline(body, line.lineNo));
    if (i < lines.length && lines[i].indent > indent) {
      throw new Error(`第 ${lines[i].lineNo} 行缩进错误：标量列表项不能有子字段`);
    }
  }
  return { value: items, next: i };
}

/**
 * 解析受限 YAML 文本。
 * @param {string} text 文件内容
 * @returns {Record<string, unknown>} 顶层键值对象
 */
export function parseYaml(text) {
  if (typeof text !== 'string') throw new Error('YAML 输入必须是字符串');
  const normalized = text.charCodeAt(0) === 0xFEFF ? text.slice(1) : text;
  const lines = splitLines(normalized);
  if (lines.length === 0) throw new Error('YAML 内容为空');
  if (isSequenceLine(lines[0].text)) throw new Error('顶层必须是映射（key: value），不能是列表');
  const parsed = parseBlock(lines, 0, lines[0].indent);
  if (parsed.next < lines.length) {
    throw new Error(`第 ${lines[parsed.next].lineNo} 行无法归属到任何层级：${lines[parsed.next].text}`);
  }
  return parsed.value;
}
