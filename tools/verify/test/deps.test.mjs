import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, writeFile, readFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { execFile } from 'node:child_process';
import { promisify } from 'node:util';
import { inferRequirements, loadDependencyManifest, probeDependencies } from '../core/deps.mjs';
import { loadScenario } from '../core/scenario.mjs';
import { runSuite } from '../core/suite.mjs';
import { toJUnit } from '../core/junit.mjs';
const exec = promisify(execFile);
const cli = fileURLToPath(new URL('../cli.mjs', import.meta.url));
async function temp() { return mkdtemp(join(tmpdir(), 'verify-deps-')); }
async function scenario(dir, file, text) { await writeFile(join(dir, file), text); }
async function manifest(dir, specs) {
  const path = join(dir, 'checks.json');
  await writeFile(path, JSON.stringify(specs));
  return path;
}
const ready = { checks: [{ type: 'exec', argv: [process.execPath, '-e', 'console.log("ready")'], contains: 'ready' }] };
const down = { checks: [{ type: 'exec', argv: [process.execPath, '-e', 'process.exit(3)'], label: 'fixture unavailable' }] };

test('requires 补充 command 隐藏依赖，动作依赖不能被空声明移除', () => {
  assert.deepEqual(inferRequirements({ requires: ['sql', 'log'], setup: [{ http: 'GET /' }], steps: [{ command: 'docker exec example' }], teardown: [{ app: 'terminate' }] }), ['device', 'log', 'network', 'sql']);
  assert.deepEqual(inferRequirements({ requires: [], steps: [{ redis: 'ping' }, { xcode: 'test' }] }), ['device', 'redis']);
});

test('预检检查真实命令结果，权限/失败信息不含原始输出或秘密', async () => {
  const dir = await temp();
  const file = await manifest(dir, { sql: ready, network: down });
  const result = await probeDependencies(await loadDependencyManifest(file), ['sql', 'network', 'unknown']);
  assert.equal(result[0].ready, true);
  assert.equal(result[1].ready, false);
  assert.deepEqual(result[1].reasons, ['fixture unavailable']);
  assert.equal(result[2].ready, false);
});

test('部分依赖就绪：只运行可用场景，跳过 setup 和 teardown，不虚增 passed', async () => {
  const dir = await temp();
  await scenario(dir, 'a.yml', 'name: independent\nsteps:\n  - sleep: 0\n');
  await scenario(dir, 'b.yml', 'name: dependent\nrequires: [sql]\nsetup:\n  - command: exit 99\nsteps:\n  - command: exit 99\nteardown:\n  - command: exit 99\n');
  const result = await runSuite(dir, { dependenciesPath: await manifest(dir, { sql: down }) });
  assert.deepEqual(result.summary, { scenarios: 2, passed: 1, failed: 0, skipped: 1, steps: 1 });
  assert.equal(result.status, 'partial');
  assert.equal(result.ok, false);
  assert.equal(result.exitCode, 0);
  assert.equal(result.scenarios[1].steps.length, 0);
  assert.match(result.scenarios[1].skipReason, /fixture unavailable/);
  const xml = toJUnit(result);
  assert.match(xml, /<skipped message=/);
  assert.match(xml, /skipped="1"/);
});

test('严格模式任何跳过都返回1，缺全部依赖不是全部通过', async () => {
  const dir = await temp();
  await scenario(dir, 'a.yml', 'name: skipped\nrequires: [sql]\nsteps:\n  - sleep: 0\n');
  const options = { dependenciesPath: await manifest(dir, { sql: down }) };
  const normal = await runSuite(dir, options);
  assert.equal(normal.summary.passed, 0);
  assert.equal(normal.summary.skipped, 1);
  assert.equal(normal.ok, false);
  const strict = await runSuite(dir, { ...options, requireFull: true });
  assert.equal(strict.exitCode, 1);
});

test('依赖齐全：实际执行全部场景，显式 --skip-deps 也遵守严格模式', async () => {
  const dir = await temp();
  await scenario(dir, 'a.yml', 'name: ready\nrequires: [sql]\nsteps:\n  - sleep: 0\n');
  const result = await runSuite(dir, { dependenciesPath: await manifest(dir, { sql: ready }), requireFull: true });
  assert.equal(result.exitCode, 0);
  assert.equal(result.status, 'passed');
  assert.equal(result.summary.passed, 1);
  assert.equal(result.summary.skipped, 0);
  const skipped = await runSuite(dir, { skipDeps: ['sql'], requireFull: true });
  assert.equal(skipped.exitCode, 1);
});

test('真实断言失败不能因另一个场景跳过而被隐藏', async () => {
  const dir = await temp();
  await scenario(dir, 'a.yml', 'name: failure\nsteps:\n  - command: printf no\n    expect: { stdoutContains: yes }\n');
  await scenario(dir, 'b.yml', 'name: skipped\nrequires: [sql]\nsteps:\n  - sleep: 0\n');
  const result = await runSuite(dir, { skipDeps: ['sql'] });
  assert.equal(result.exitCode, 1);
  assert.equal(result.summary.failed, 1);
  assert.equal(result.summary.skipped, 1);
});

test('缺密码可因依赖不就绪跳过；dry-run 无须真实密码但真实运行仍校验', async () => {
  const dir = await temp();
  await scenario(dir, 'a.yml', 'name: missing-env\nrequires: [fixture-password]\nsteps:\n  - command: "printf \'%s\' ${env:VERIFY_DEPS_MISSING_PASSWORD}"\n');
  await assert.rejects(loadScenario(join(dir, 'a.yml')), /环境变量/);
  const result = await runSuite(dir, { skipDeps: ['fixture-password'] });
  assert.equal(result.summary.skipped, 1);
  const output = await exec(process.execPath, [cli, 'run', dir, '--dry-run', '--json']);
  assert.equal(JSON.parse(output.stdout).dryRun, true);
});

test('非法 requires / 非法场景先报配置错误，不执行之前的有效场景', async () => {
  const dir = await temp();
  const marker = join(dir, 'must-not-exist');
  await scenario(dir, 'a.yml', `name: early\nsteps:\n  - command: "touch ${marker}"\n`);
  await scenario(dir, 'b.yml', 'name: invalid\nrequires: not-a-list\nsteps:\n  - sleep: 0\n');
  await assert.rejects(runSuite(dir), /requires/);
  await assert.rejects(readFile(marker), { code: 'ENOENT' });
});

test('依赖清单错误不触发测试，单场景拒绝静默忽略 skip 参数', async () => {
  const dir = await temp();
  await scenario(dir, 'a.yml', 'name: one\nsteps:\n  - sleep: 0\n');
  await assert.rejects(loadDependencyManifest(await manifest(dir, { sql: { checks: [{ type: 'arbitrary' }] } })), /未知/);
  await assert.rejects(exec(process.execPath, [cli, 'run', join(dir, 'a.yml'), '--skip-deps', 'sql', '--json']), error => error.code === 2);
});

test('CLI 的 stdout、txt、JSON、JUnit 都明确保留跳过，strict 返回1', async () => {
  const dir = await temp();
  await scenario(dir, 'a.yml', 'name: db\nrequires: [sql]\nsteps:\n  - sleep: 0\n');
  const output = await exec(process.execPath, [cli, 'run', dir, '--skip-deps', 'sql', '--json', '--evidence-dir', join(dir, 'ev'), '--junit', join(dir, 'result.xml')]);
  const result = JSON.parse(output.stdout);
  assert.equal(result.summary.passed, 0);
  assert.equal(result.summary.skipped, 1);
  assert.match(output.stderr, /SKIP/);
  assert.match(await readFile(result.scenarios[0].evidence.text, 'utf8'), /SKIP/);
  const { readdir } = await import('node:fs/promises');
  for (const file of (await readdir(join(dir, 'ev'))).filter(file => file.includes('suite-') && file.endsWith('.txt'))) {
    assert.match(await readFile(join(dir, 'ev', file), 'utf8'), /PARTIAL/);
  }
  assert.match(await readFile(join(dir, 'result.xml'), 'utf8'), /<skipped/);
  await assert.rejects(exec(process.execPath, [cli, 'run', dir, '--skip-deps', 'sql', '--require-full', '--json', '--evidence-dir', join(dir, 'ev')]), error => error.code === 1);
});

test('Xcode 退出65或零执行不能以 failed=0 假绿', async () => {
  const { runXcode } = await import('../adapters/xcode.mjs');
  const dir = await temp();
  const builder = join(dir, 'build');
  const parser = join(dir, 'parse');
  await writeFile(parser, `#!/bin/sh\necho '{"passedTests":0,"failedTests":0,"skippedTests":0}'\n`, { mode: 0o755 });
  for (const exitCode of [65, 0]) {
    await writeFile(builder, `#!/bin/sh\nexit ${exitCode}\n`, { mode: 0o755 });
    await assert.rejects(runXcode({ xcode: 'test' }, {
      config: { xcode: { xcodebuildPath: builder, xcrunPath: parser } }, scenario: { dir },
    }), error => /不能/.test(error.message) && error.actual.total === 0);
  }
});

test('JUnit 写入失败必须让结果、证据和退出码同时失败', async (t) => {
  const { execFile } = await import('node:child_process');
  const { promisify } = await import('node:util');
  const run = promisify(execFile);
  const dir = await temp();
  await scenario(dir, 'ok.yml', 'name: ok\nsteps:\n  - command: exit 0\n    expect: { exitCode: 0 }\n');
  const evidenceDir = join(dir, 'ev');

  await assert.rejects(
    run(process.execPath, [cli, 'run', dir, '--json', '--evidence-dir', evidenceDir, '--junit', join(dir, 'absent/report.xml')]),
    (error) => {
      const printed = JSON.parse(error.stdout);
      assert.equal(error.code, 2);
      assert.equal(printed.exitCode, 2);
      assert.equal(printed.ok, false);
      assert.equal(printed.status, 'failed');
      assert.match(printed.junitError, /JUnit 写入失败/);
      return true;
    },
  );

  const { readdir } = await import('node:fs/promises');
  const saved = (await readdir(evidenceDir)).filter((name) => name.includes('suite-') && name.endsWith('.json'));
  assert.ok(saved.length > 0, '应保留套件证据');
  const disk = JSON.parse(await readFile(join(evidenceDir, saved[0]), 'utf8'));
  assert.equal(disk.ok, false);
  assert.equal(disk.exitCode, 2);
  assert.equal(disk.status, 'failed');
});

test('JUnit 失败重写证据时仍保留场景自定义脱敏规则', async (t) => {
  const { execFile } = await import('node:child_process');
  const { promisify } = await import('node:util');
  const run = promisify(execFile);
  const dir = await temp();
  const marker = 'QA_MARKER_CUSTOM_8917';
  await scenario(dir, 'redact.yml', `name: custom redaction\nconfig:\n  redact:\n    - "${marker}"\nsteps:\n  - command: "printf ${marker}"\n    expect: { exitCode: 0 }\n`);
  const evidenceDir = join(dir, 'ev');

  await assert.rejects(
    run(process.execPath, [cli, 'run', dir, '--json', '--evidence-dir', evidenceDir, '--junit', join(dir, 'absent/report.xml')]),
    error => error.code === 2,
  );

  const { readdir } = await import('node:fs/promises');
  const files = await readdir(evidenceDir);
  const suiteJson = files.find(name => name.includes('suite-') && name.endsWith('.json'));
  const content = await readFile(join(evidenceDir, suiteJson), 'utf8');
  assert.doesNotMatch(content, new RegExp(marker));
  assert.match(content, /REDACTED/);
});
