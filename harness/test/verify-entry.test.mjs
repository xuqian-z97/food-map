import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, writeFile, mkdir, copyFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { execFile } from 'node:child_process';
import { promisify } from 'node:util';
const exec = promisify(execFile);
const repository = fileURLToPath(new URL('../../', import.meta.url));
async function temp() { return mkdtemp(join(tmpdir(), 'verify-entry-')); }

test('POSIX 包装兼容 macOS Bash 3 空参数和带空格参数，不接触外部环境', async () => {
  const dir = await temp();
  await writeFile(join(dir, 'node'), '#!/bin/sh\nprintf "%s\\n" "$@"\n', { mode: 0o755 });
  const wrapper = resolve(repository, 'harness/scripts/verify.sh');
  for (const shell of ['/bin/bash', '/bin/sh']) {
    const empty = await exec(shell, [wrapper], { env: { ...process.env, PATH: `${dir}:${process.env.PATH}` } });
    assert.match(empty.stdout, /--dependencies/);
    const spaced = await exec(shell, [wrapper, '--only', 'contains space'], { env: { ...process.env, PATH: `${dir}:${process.env.PATH}` } });
    assert.match(spaced.stdout, /contains space\n$/);
  }
});

test('run-all 调用依赖感知入口并透传严格模式失败', async () => {
  const dir = await temp();
  const scripts = join(dir, 'harness/scripts');
  await mkdir(scripts, { recursive: true });
  await copyFile(resolve(repository, 'harness/scripts/run-all.sh'), join(scripts, 'run-all.sh'));
  for (const name of ['docs', 'api', 'backend', 'logging', 'integration', 'ios', 'git']) {
    await writeFile(join(scripts, `validate-${name}.sh`), '#!/bin/sh\nexit 0\n', { mode: 0o755 });
  }
  await writeFile(join(scripts, 'verify.sh'), '#!/bin/sh\necho fixture-verify-called\n[ "${VERIFY_REQUIRE_FULL:-0}" != 1 ]\n', { mode: 0o755 });
  const output = await exec('/bin/sh', [join(scripts, 'run-all.sh')]);
  assert.match(output.stdout, /fixture-verify-called/);
  await assert.rejects(exec('/bin/sh', [join(scripts, 'run-all.sh')], { env: { ...process.env, VERIFY_REQUIRE_FULL: '1' } }), error => error.code === 1);
});
