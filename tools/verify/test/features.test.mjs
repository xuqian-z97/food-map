import { test } from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import { mkdtemp, writeFile, rm, readdir, readFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { loadScenario } from '../core/scenario.mjs';
import { runScenario } from '../core/runner.mjs';
import { runSuite } from '../core/suite.mjs';
import { createRedactor } from '../core/redact.mjs';
import { writeEvidence } from '../core/evidence.mjs';
import { toJUnit } from '../core/junit.mjs';

async function listen(server) {
  await new Promise((resolve, reject) => {
    server.once('error', reject);
    server.listen(0, '127.0.0.1', resolve);
  });
  return server.address().port;
}

async function close(server) {
  await new Promise((resolve) => server.close(resolve));
}

async function tempDir(t, prefix = 'verify-extra-') {
  const dir = await mkdtemp(join(tmpdir(), prefix));
  t.after(async () => {
    await rm(dir, { recursive: true, force: true });
  });
  return dir;
}

async function scenarioFrom(t, text) {
  const dir = await tempDir(t);
  const file = join(dir, 'scenario.yml');
  await writeFile(file, text, 'utf8');
  return loadScenario(file);
}

/** 一个前 N 次失败、之后成功的本地服务，用来验证 retry。 */
async function flakyServer(failures) {
  let calls = 0;
  const server = http.createServer((req, res) => {
    calls += 1;
    if (calls <= failures) {
      res.writeHead(503, { 'content-type': 'application/json' });
      res.end(JSON.stringify({ code: 'DOWN' }));
      return;
    }
    res.writeHead(200, { 'content-type': 'application/json' });
    res.end(JSON.stringify({ code: 'OK' }));
  });
  const port = await listen(server);
  return { server, port, calls: () => calls };
}

test('retry 让前两次失败的请求最终通过', async (t) => {
  const flaky = await flakyServer(2);
  t.after(() => close(flaky.server));
  const scenario = await scenarioFrom(t, `name: retry
config:
  baseUrl: http://127.0.0.1:${flaky.port}
steps:
  - http: GET /health
    retry: { max: 3, intervalMs: 10 }
    expect: { status: 200 }
`);

  const result = await runScenario(scenario);

  assert.equal(result.ok, true);
  assert.equal(result.steps[0].attempts, 3);
  assert.equal(flaky.calls(), 3);
});

test('retry 耗尽后步骤失败并记录尝试次数', async (t) => {
  const flaky = await flakyServer(99);
  t.after(() => close(flaky.server));
  const scenario = await scenarioFrom(t, `name: retry-exhausted
config:
  baseUrl: http://127.0.0.1:${flaky.port}
steps:
  - http: GET /health
    retry: { max: 2, intervalMs: 10 }
    expect: { status: 200 }
`);

  const result = await runScenario(scenario);

  assert.equal(result.ok, false);
  assert.equal(result.exitCode, 1);
  assert.equal(result.steps[0].attempts, 2);
  assert.match(result.steps[0].checks[0].message || '', /503/);
});

test('setup 失败时跳过 steps，但 teardown 仍然执行', async (t) => {
  const dir = await tempDir(t);
  const marker = join(dir, 'teardown.txt');
  const scenario = await scenarioFrom(t, `name: setup-fail
setup:
  - command: "exit 3"
    expect: { exitCode: 0 }
steps:
  - command: "printf should-not-run"
    capture: { seen: stdout }
teardown:
  - command: "printf done > ${marker}"
`);

  const result = await runScenario(scenario);
  const phases = result.steps.map((step) => step.phase);

  assert.equal(result.ok, false);
  assert.equal(result.summary.setupFailed, 1);
  assert.equal(result.summary.skipped, 1);
  assert.deepEqual(phases, ['setup', 'teardown']);
  assert.equal((await readFile(marker, 'utf8')).trim(), 'done');
  assert.equal(scenario.vars.seen, undefined);
});

test('setup 的 capture 在 steps 中可用', async (t) => {
  const scenario = await scenarioFrom(t, `name: setup-capture
setup:
  - command: "printf 42"
    capture: { userId: stdout }
    expect: { exitCode: 0 }
steps:
  - command: "printf '\${userId}'"
    expect: { stdoutContains: "42" }
`);

  const result = await runScenario(scenario);

  assert.equal(result.ok, true);
  assert.equal(result.steps[0].phase, 'setup');
  assert.equal(scenario.vars.userId, '42');
});

test('teardown 失败会计入失败数', async (t) => {
  const scenario = await scenarioFrom(t, `name: teardown-fail
steps:
  - command: "printf ok"
teardown:
  - command: "exit 1"
    expect: { exitCode: 0 }
`);

  const result = await runScenario(scenario);

  assert.equal(result.ok, false);
  assert.equal(result.summary.teardownFailed, 1);
  assert.equal(result.summary.failed, 1);
});

test('脱敏器替换 Bearer、JWT、手机号和自定义规则', () => {
  const redactor = createRedactor(['secret-[0-9]+']);
  const text = 'Authorization: Bearer abcdefgh12345678 phone=13812345678 token=secret-42';

  const redacted = redactor.redact(text);

  assert.doesNotMatch(redacted, /abcdefgh12345678/);
  assert.doesNotMatch(redacted, /13812345678/);
  assert.doesNotMatch(redacted, /secret-42/);
  assert.match(redacted, /\[REDACTED\]/);
});

test('脱敏器递归处理对象并保留结构', () => {
  const redactor = createRedactor([]);
  const input = { data: { phone: '13812345678', nested: ['Bearer abcdefgh12345678'] } };

  const output = redactor.redactValue(input);

  assert.equal(output.data.nested.length, 1);
  assert.doesNotMatch(JSON.stringify(output), /13812345678/);
  assert.doesNotMatch(JSON.stringify(output), /abcdefgh12345678/);
});

test('证据文件名带 runId，同名场景两次运行不会互相覆盖', async (t) => {
  const dir = await tempDir(t);
  const makeResult = (runId) => ({
    schemaVersion: 2,
    name: 'same-name',
    runId,
    source: 'inline',
    ok: true,
    exitCode: 0,
    startedAt: '2026-11-06T05:00:00.000Z',
    finishedAt: '2026-11-06T05:00:01.000Z',
    durationMs: 1000,
    summary: { total: 0, passed: 0, failed: 0 },
    steps: [],
  });

  await writeEvidence(makeResult('run-aaaa1111'), dir);
  await writeEvidence(makeResult('run-bbbb2222'), dir);

  const files = (await readdir(dir)).filter((name) => name.endsWith('.json'));
  assert.equal(files.length, 2);
});

test('证据落盘前应用脱敏规则', async (t) => {
  const dir = await tempDir(t);
  const redactor = createRedactor([]);
  const result = {
    schemaVersion: 2,
    name: 'redacted',
    runId: 'run-cccc3333',
    source: 'inline',
    ok: false,
    exitCode: 1,
    startedAt: '2026-11-06T05:00:00.000Z',
    finishedAt: '2026-11-06T05:00:01.000Z',
    durationMs: 10,
    summary: { total: 1, passed: 0, failed: 1 },
    steps: [{ index: 1, ok: false, display: 'http login', expected: {}, actual: { body: 'Bearer abcdefgh12345678' }, checks: [] }],
  };

  await writeEvidence(result, dir, { redactor });
  const json = await readFile(join(dir, (await readdir(dir)).find((name) => name.endsWith('.json'))), 'utf8');

  assert.doesNotMatch(json, /abcdefgh12345678/);
});

test('suite 聚合多个场景并给出退出码', async (t) => {
  const dir = await tempDir(t);
  await writeFile(join(dir, 'a-pass.yml'), 'name: pass\nsteps:\n  - command: "printf ok"\n', 'utf8');
  await writeFile(
    join(dir, 'b-fail.yml'),
    'name: fail\nsteps:\n  - command: "printf ok"\n    expect: { stdoutContains: nope }\n',
    'utf8',
  );
  await writeFile(join(dir, 'README.md'), 'not a scenario\n', 'utf8');

  const suite = await runSuite(dir);

  assert.equal(suite.kind, 'suite');
  assert.equal(suite.summary.scenarios, 2);
  assert.equal(suite.summary.passed, 1);
  assert.equal(suite.summary.failed, 1);
  assert.equal(suite.exitCode, 1);
  assert.deepEqual(suite.scenarios.map((item) => item.name).sort(), ['fail', 'pass']);
});

test('suite 在目录没有场景时报退出码 2', async (t) => {
  const dir = await tempDir(t);

  await assert.rejects(() => runSuite(dir), (error) => error.exitCode === 2);
});

test('suite 的 --only 只保留匹配的场景', async (t) => {
  const dir = await tempDir(t);
  await writeFile(join(dir, 'login.yml'), 'name: login\nsteps:\n  - command: "printf ok"\n', 'utf8');
  await writeFile(join(dir, 'logout.yml'), 'name: logout\nsteps:\n  - command: "printf ok"\n', 'utf8');

  const suite = await runSuite(dir, { only: 'logout' });

  assert.equal(suite.summary.scenarios, 1);
  assert.equal(suite.scenarios[0].name, 'logout');
});

test('JUnit 输出包含 testsuite 与失败用例', () => {
  const result = {
    kind: 'suite',
    name: 'demo',
    durationMs: 1200,
    scenarios: [
      {
        name: 'logout',
        durationMs: 600,
        steps: [
          { index: 1, phase: 'step', action: 'http', display: 'http POST /logout', ok: true, durationMs: 10, checks: [] },
          {
            index: 2,
            phase: 'step',
            action: 'redis',
            display: 'redis count',
            ok: false,
            durationMs: 20,
            error: '',
            checks: [{ name: 'equals', expected: 3, actual: 2, ok: false }],
          },
        ],
      },
    ],
  };

  const xml = toJUnit(result);

  assert.match(xml, /<testsuites name="demo" tests="2" failures="1"/);
  assert.match(xml, /<testsuite name="logout"/);
  assert.match(xml, /<failure message=/);
  assert.match(xml, /expected 3/);
});

test('场景语法错误时 setup/retry 的非法写法被静态拒绝', async (t) => {
  await assert.rejects(
    () => scenarioFrom(t, 'name: bad\nsteps:\n  - command: "printf ok"\n    retry: { max: 0 }\n'),
    (error) => error.exitCode === 2 && /retry\.max/.test(error.message),
  );
});

test('未知的 retry 字段被拒绝', async (t) => {
  await assert.rejects(
    () => scenarioFrom(t, 'name: bad\nsteps:\n  - command: "printf ok"\n    retry: { retries: 2 }\n'),
    (error) => error.exitCode === 2 && /未知字段/.test(error.message),
  );
});

test('${env:NAME} 解析环境变量，未设置时给出可读错误', async (t) => {
  process.env.VERIFY_TEST_TOKEN = 'from-env';
  t.after(() => {
    delete process.env.VERIFY_TEST_TOKEN;
  });
  const scenario = await scenarioFrom(t, `name: env-var
steps:
  - command: "printf '%s'\${env:VERIFY_TEST_TOKEN}"
    expect: { stdoutContains: from-env }
`);

  const result = await runScenario(scenario);

  assert.equal(result.ok, true);
});

test('环境变量缺失时加载阶段就报错并指出变量名', async (t) => {
  await assert.rejects(
    () => scenarioFrom(t, `name: env-missing
steps:
  - command: "printf '%s'\${env:VERIFY_NOT_SET_XYZ}"
`),
    (error) => error.exitCode === 2 && /VERIFY_NOT_SET_XYZ/.test(error.message),
  );
});

test('buildXcodeArgs 补默认值且不覆盖显式参数', async () => {
  const { buildXcodeArgs } = await import('../adapters/xcode.mjs');
  const config = { scheme: 'Demo', udid: 'AAA-BBB', derivedDataPath: '.build/dd', codeSignIdentity: '-' };

  const filled = buildXcodeArgs('test', ['-only-testing:DemoTests/Login'], config, '/tmp/proj');
  const explicit = buildXcodeArgs('test', ['-scheme', 'Other', '-destination', 'platform=iOS Simulator,id=CCC'], config, '/tmp/proj');

  assert.deepEqual(filled.slice(0, 5), ['test', '-project', '/tmp/proj/FoodMapApp.xcodeproj', '-scheme', 'Demo']);
  assert.ok(filled.includes('-destination'));
  assert.ok(filled.includes('-derivedDataPath'));
  assert.ok(filled.includes('CODE_SIGN_IDENTITY=-'));
  assert.equal(explicit.filter((item) => item === '-scheme').length, 1);
  assert.equal(explicit.filter((item) => item === '-destination').length, 1);
  assert.ok(!explicit.includes('Demo'));
  assert.ok(!explicit.includes('AAA-BBB'));
});

test('--json stdout 也会脱敏，不能把 Token 打到终端', async (t) => {
  const { execFile } = await import('node:child_process');
  const { promisify } = await import('node:util');
  const run = promisify(execFile);
  const dir = await tempDir(t);
  const server = http.createServer((req, res) => {
    res.writeHead(200, { 'content-type': 'application/json' });
    res.end(JSON.stringify({ data: { accessToken: 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.signature123456' } }));
  });
  const port = await listen(server);
  t.after(() => close(server));
  const scenarioFile = join(dir, 'token.yml');
  await writeFile(scenarioFile, `name: token\nsteps:\n  - http: GET http://127.0.0.1:${port}/login\n    capture: { token: data.accessToken }\n`, 'utf8');

  const { stdout } = await run(process.execPath, ['tools/verify/cli.mjs', 'run', scenarioFile, '--json', '--evidence-dir', join(dir, 'ev')]);

  assert.doesNotMatch(stdout, /eyJhbGciOiJIUzI1NiJ9/);
  assert.match(stdout, /REDACTED/);
});

test('结构化对象里的不透明 Token 和密码按键名脱敏', () => {
  const redactor = createRedactor([]);
  const out = redactor.redactValue({ data: { accessToken: 'opaque-abc', password: 'p@ss', note: 'ok' } });

  assert.equal(out.data.accessToken, '[REDACTED]');
  assert.equal(out.data.password, '[REDACTED]');
  assert.equal(out.data.note, 'ok');
});

test('密码值包含转义双引号时整段被遮盖', () => {
  const body = JSON.stringify({ data: { password: 'ab"cdMARKER', accessToken: 'opq' } });
  const out = createRedactor([]).redact(body);

  assert.doesNotMatch(out, /MARKER/);
  assert.doesNotMatch(out, /opq/);
});
