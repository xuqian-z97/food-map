// 日志/文件适配器：读取单个文件或简单 glob（*、?、**）。
//
// 相对路径默认相对 scenario 文件所在目录，可用 config.logRoot 改变根目录。

import { readFile, readdir, stat } from 'node:fs/promises';
import { isAbsolute, join, parse, resolve } from 'node:path';

function hasMagic(segment) {
  return segment.includes('*') || segment.includes('?');
}

function segmentMatcher(segment) {
  let pattern = '^';
  for (const ch of segment) {
    if (ch === '*') pattern += '.*';
    else if (ch === '?') pattern += '.';
    else pattern += ch.replace(/[\\^$.*+?()[\]{}|]/g, '\\$&');
  }
  return new RegExp(`${pattern}$`);
}

async function statOrNull(file) {
  try {
    return await stat(file);
  } catch {
    return null;
  }
}

async function collect(base, segments, index, out) {
  if (index >= segments.length) {
    const info = await statOrNull(base);
    if (info && info.isFile()) out.add(base);
    return;
  }
  const segment = segments[index];
  if (segment === '..' || segment === '.') {
    await collect(resolve(base, segment), segments, index + 1, out);
    return;
  }
  if (segment === '**') {
    await collect(base, segments, index + 1, out);
    let entries;
    try {
      entries = await readdir(base, { withFileTypes: true });
    } catch {
      return;
    }
    for (const entry of entries) {
      if (entry.isDirectory()) {
        await collect(join(base, entry.name), segments, index, out);
      } else if (entry.isFile() && index + 1 >= segments.length) {
        out.add(join(base, entry.name));
      }
    }
    return;
  }
  if (hasMagic(segment)) {
    let entries;
    try {
      entries = await readdir(base, { withFileTypes: true });
    } catch {
      return;
    }
    const matcher = segmentMatcher(segment);
    for (const entry of entries) {
      if (!matcher.test(entry.name)) continue;
      const candidate = join(base, entry.name);
      if (index + 1 >= segments.length) {
        if (entry.isFile()) out.add(candidate);
      } else if (entry.isDirectory()) {
        await collect(candidate, segments, index + 1, out);
      }
    }
    return;
  }
  const candidate = join(base, segment);
  const info = await statOrNull(candidate);
  if (!info) return;
  if (index + 1 >= segments.length) {
    if (info.isFile()) out.add(candidate);
    return;
  }
  if (info.isDirectory()) await collect(candidate, segments, index + 1, out);
}

function normalize(parent, raw) {
  if (isAbsolute(raw)) {
    const root = parse(raw).root;
    return { root, segments: raw.slice(root.length).split(/[\\/]+/).filter(Boolean) };
  }
  return { root: parent, segments: raw.split(/[\\/]+/).filter((part) => part !== '') };
}

/**
 * 执行 logfile 步骤。
 * @param {Record<string, any>} step 步骤
 * @param {{config: Record<string, any>, scenario: Record<string, any>}} context 执行上下文
 * @returns {Promise<{display: string, actual: Record<string, any>, summary: string}>}
 */
export async function runLogfile(step, context) {
  const pattern = String(step.logfile).trim();
  if (pattern === '') {
    const error = new Error('logfile 步骤的路径为空');
    error.hint = '写法：logfile: path/to/app.log 或 logfile: "logs/**/*.log"。';
    throw error;
  }
  const parent = context.config.logRoot
    ? resolve(context.scenario.dir, String(context.config.logRoot))
    : context.scenario.dir;
  const { root, segments } = normalize(parent, pattern);
  const found = new Set();
  try {
    await collect(root, segments, 0, found);
  } catch (error) {
    const failure = new Error(`logfile 遍历失败：${pattern}（根目录 ${root}）：${error.message}`);
    failure.hint = '检查 config.logRoot、路径权限和 glob 写法。';
    failure.actual = { pattern, root, error: error.message };
    throw failure;
  }
  const files = [...found].sort();
  if (files.length === 0) {
    const failure = new Error(`logfile 未找到文件：${pattern}（解析根目录 ${root}）`);
    failure.hint = '检查文件是否已生成、config.logRoot 是否指向正确目录，或 glob 是否匹配（支持 *、?、**）。';
    failure.actual = { pattern, root, files: [] };
    throw failure;
  }

  const chunks = [];
  for (const file of files) {
    try {
      chunks.push(await readFile(file, 'utf8'));
    } catch (error) {
      const failure = new Error(`logfile 读取失败：${file}：${error.message}`);
      failure.hint = '检查文件权限和编码；本工具按 UTF-8 读取。';
      failure.actual = { pattern, file, error: error.message };
      throw failure;
    }
  }
  const content = chunks.join('\n');
  return {
    display: `logfile ${pattern}`,
    actual: {
      pattern,
      root,
      files,
      content,
      value: content,
      bytes: Buffer.byteLength(content, 'utf8'),
    },
    summary: files.length === 1 ? files[0].split('/').pop() : `${files.length} files`,
  };
}
