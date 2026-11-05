import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtemp, writeFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { parseYaml } from '../core/yaml.mjs';
import { ScenarioError, interpolate, loadScenario, variableScope } from '../core/scenario.mjs';
import { evaluateExpectations, failureLines } from '../core/assert.mjs';

async function tempDir(t) {
  const dir = await mkdtemp(join(tmpdir(), 'verify-scenario-'));
  t.after(async () => {
    await rm(dir, { recursive: true, force: true });
  });
  return dir;
}

test('YAML 子集支持嵌套 map、块列表、流式集合和引号标量', () => {
  const doc = parseYaml(`name: demo
config:
  nested:
    enabled: true
    port: 8080
  empty:
vars:
  account: "demo user"
steps:
  - http: POST /x
    headers: { X-Trace: \${runId}, Accept: 'application/json' }
    expect:
      status: 200
      contains:
        - one
        - "two"
      json: { code: OK }
`);

  assert.equal(doc.name, 'demo');
  assert.equal(doc.config.nested.enabled, true);
  assert.equal(doc.config.nested.port, 8080);
  assert.equal(doc.config.empty, null);
  assert.equal(doc.vars.account, 'demo user');
  assert.equal(doc.steps[0].headers['X-Trace'], '${runId}');
  assert.equal(doc.steps[0].headers.Accept, 'application/json');
  assert.deepEqual(doc.steps[0].expect.contains, ['one', 'two']);
  assert.deepEqual(doc.steps[0].expect.json, { code: 'OK' });
});

test('interpolate 替换对象/列表中的所有字符串并保留原类型', () => {
  const value = interpolate({
    json: { accountName: '${account}', attempt: 2 },
    headers: ['${runId}', 'static'],
  }, { account: 'demo', runId: 'run-1' }, 'steps[0]');

  assert.deepEqual(value, {
    json: { accountName: 'demo', attempt: 2 },
    headers: ['run-1', 'static'],
  });
});

test('未定义变量报错，指出变量名和具体位置', () => {
  assert.throws(
    () => interpolate('${missing}', {}, 'steps[1].json.password'),
    (error) => {
      assert.equal(error instanceof ScenarioError, true);
      assert.match(error.message, /missing/);
      assert.match(error.message, /steps\[1\]\.json\.password/);
      assert.match(error.hint, /--var missing=/);
      return true;
    },
  );
});

test('loadScenario 静态拒绝未定义变量', async (t) => {
  const dir = await tempDir(t);
  const file = join(dir, 'bad.yml');
  await writeFile(file, `name: bad
steps:
  - http: GET /x
    headers: { X-Missing: \${missing} }
`, 'utf8');

  await assert.rejects(
    () => loadScenario(file),
    (error) => {
      assert.equal(error instanceof ScenarioError, true);
      assert.equal(error.exitCode, 2);
      assert.match(error.message, /变量 missing 未定义/);
      assert.match(error.message, /steps\[0\]\.headers\.X-Missing/);
      return true;
    },
  );
});

test('--var 覆盖 scenario.vars，config 顶层标量可作为 ${var} 兜底', async (t) => {
  const dir = await tempDir(t);
  const file = join(dir, 'vars.yml');
  await writeFile(file, `name: vars
config:
  udid: from-config
vars:
  account: demo
steps:
  - command: "echo \${account} \${udid}"
`, 'utf8');

  const scenario = await loadScenario(file, { vars: { account: 'alice' } });
  assert.equal(scenario.vars.account, 'alice');
  assert.equal(scenario.config.udid, 'from-config');
  assert.equal(
    interpolate(scenario.steps[0].command, variableScope(scenario), 'steps[0]'),
    'echo alice from-config',
  );
});

test('断言失败信息包含 expected/actual', () => {
  const { ok, checks } = evaluateExpectations(
    { equals: 'REVOKED', json: { code: 'OK' } },
    { value: 'ACTIVE', json: { code: 'DOWN' } },
  );
  assert.equal(ok, false);
  const text = failureLines(checks).join('\n');
  assert.match(text, /equals/);
  assert.match(text, /REVOKED/);
  assert.match(text, /ACTIVE/);
  assert.match(text, /JSON 路径不匹配：code/);
});
