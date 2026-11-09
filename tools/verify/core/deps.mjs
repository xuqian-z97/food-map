// 依赖名是项目配置，不猜测 shell 命令内容。动作推断只补充通用依赖。
import { readFile } from 'node:fs/promises';
import { resolve, dirname } from 'node:path';
import { execFile } from 'node:child_process';
import { promisify } from 'node:util';
const exec = promisify(execFile);
const ACTION_DEPS = { http: 'network', sql: 'sql', redis: 'redis', app: 'device', xcode: 'device' };

/** 显式 requires 与所有阶段的通用动作依赖取并集。command 的隐藏依赖由作者声明。 */
export function inferRequirements(scenario) {
  const found = new Set(scenario.requires || []);
  for (const step of [...(scenario.setup || []), ...(scenario.steps || []), ...(scenario.teardown || [])]) {
    for (const [action, dependency] of Object.entries(ACTION_DEPS)) {
      if (Object.hasOwn(step, action)) found.add(dependency);
    }
  }
  return [...found].sort();
}

/** 校验并读取受信项目的只读预检清单。命令使用 argv，不经 shell。 */
export async function loadDependencyManifest(file) {
  const source = resolve(file);
  const raw = JSON.parse(await readFile(source, 'utf8'));
  if (!raw || typeof raw !== 'object' || Array.isArray(raw)) throw new Error('依赖清单必须是对象');
  for (const [name, spec] of Object.entries(raw)) {
    if (!/^[a-z][a-z0-9_-]*$/.test(name) || !spec || typeof spec !== 'object') throw new Error(`非法依赖：${name}`);
    if (!Array.isArray(spec.checks) || !spec.checks.length) throw new Error(`${name}.checks 必须是非空列表`);
    for (const check of spec.checks) {
      if (!['http', 'exec', 'file', 'env'].includes(check.type)) throw new Error(`${name}: 未知预检类型`);
      if (check.type === 'exec' && (!Array.isArray(check.argv) || !check.argv.length || check.argv.some(v => typeof v !== 'string'))) throw new Error(`${name}: exec 需要 argv 字符串数组`);
      if (check.type === 'http' && (typeof check.url !== 'string' || !/^https?:\/\//.test(check.url))) throw new Error(`${name}: http 需要 URL`);
      if (check.type === 'file' && typeof check.path !== 'string') throw new Error(`${name}: file 需要 path`);
      if (check.type === 'env' && typeof check.name !== 'string') throw new Error(`${name}: env 需要 name`);
      if (check.contains !== undefined && typeof check.contains !== 'string') throw new Error(`${name}: contains 必须是字符串`);
      if (check.timeoutMs !== undefined && (!Number.isFinite(check.timeoutMs) || check.timeoutMs <= 0)) throw new Error(`${name}: timeoutMs 必须是正数`);
    }
  }
  return { specs: raw, dir: dirname(source) };
}

/** 仅检查需要的依赖；不回传命令原始输出或环境变量值。 */
export async function probeDependencies(manifest, names) {
  const results = [];
  for (const name of names) {
    const spec = manifest.specs[name];
    const reasons = [];
    if (!spec) reasons.push('未配置预检，无法确认就绪');
    for (const check of spec?.checks || []) {
      let ready = false;
      try {
        if (check.type === 'http') {
          const response = await fetch(check.url, { signal: AbortSignal.timeout(check.timeoutMs || 3000), redirect: 'error' });
          ready = response.status === (check.status ?? 200);
          await response.body?.cancel();
        } else if (check.type === 'exec') {
          const { stdout } = await exec(check.argv[0], check.argv.slice(1), { timeout: check.timeoutMs || 5000, maxBuffer: 1024 * 1024 });
          ready = check.contains === undefined || stdout.includes(check.contains);
        } else if (check.type === 'file') {
          const handle = await import('node:fs/promises').then(fs => fs.open(resolve(manifest.dir, check.path), 'r'));
          await handle.close();
          ready = true;
        } else if (check.type === 'env') ready = Boolean(process.env[check.name]);
      } catch { /* 缺命令、权限、连接与超时均不可视为就绪，不能误报服务一定未启动。 */ }
      if (!ready) reasons.push(check.label || `${check.type} 预检失败（可能是未就绪、缺工具或权限受限）`);
    }
    results.push({ name, ready: reasons.length === 0, reasons });
  }
  return results;
}
