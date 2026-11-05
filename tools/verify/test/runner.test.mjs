import { test } from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import { mkdtemp, writeFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { loadScenario } from '../core/scenario.mjs';
import { runScenario } from '../core/runner.mjs';

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

async function tempDir(t) {
  const dir = await mkdtemp(join(tmpdir(), 'verify-runner-'));
  t.after(async () => {
    await rm(dir, { recursive: true, force: true });
  });
  return dir;
}

test('http/command/logfile/sleep 全部通过，并支持 capture 变量', async (t) => {
  const dir = await tempDir(t);
  await writeFile(join(dir, 'probe.log'), 'boot\nprobe.ready\n', 'utf8');
  let seen;
  const server = http.createServer((req, res) => {
    const chunks = [];
    req.on('data', (chunk) => chunks.push(chunk));
    req.on('end', () => {
      seen = {
        method: req.method,
        url: req.url,
        headers: req.headers,
        body: Buffer.concat(chunks).toString('utf8'),
      };
      res.writeHead(200, { 'content-type': 'application/json' });
      res.end(JSON.stringify({ code: 'OK', data: { accessToken: 'token-123' } }));
    });
  });
  const port = await listen(server);
  t.after(() => close(server));

  const scenarioPath = join(dir, 'scenario.yml');
  await writeFile(scenarioPath, `name: 本地通过路径
config:
  baseUrl: http://127.0.0.1:${port}
vars:
  account: demo
steps:
  - http: POST /api/login
    json: { accountName: \${account} }
    headers: { X-Trace: \${runId} }
    expect:
      status: 200
      json: { code: OK, data.accessToken: token-123 }
      contains: token-123
    capture:
      access: data.accessToken
  - command: "printf '%s' '\${access}'"
    expect:
      exitCode: 0
      stdoutContains: token-123
  - logfile: probe.log
    expect:
      contains: ["probe.ready"]
      matches: ["probe\\\\.ready"]
  - sleep: 5
`, 'utf8');

  const scenario = await loadScenario(scenarioPath);
  const lines = [];
  const result = await runScenario(scenario, { log: (line) => lines.push(line) });

  assert.equal(result.ok, true);
  assert.equal(result.exitCode, 0);
  assert.equal(result.summary.total, 4);
  assert.equal(result.summary.passed, 4);
  assert.equal(result.steps.length, 4);
  assert.equal(result.steps.every((step) => typeof step.durationMs === 'number'), true);

  assert.equal(seen.method, 'POST');
  assert.equal(seen.url, '/api/login');
  assert.equal(seen.headers['x-trace'], scenario.runId);
  assert.deepEqual(JSON.parse(seen.body), { accountName: 'demo' });
  assert.equal(result.steps[0].actual.status, 200);
  assert.equal(result.steps[0].checks.every((check) => check.ok), true);
  assert.equal(result.steps[1].actual.stdout, 'token-123');
  assert.match(result.steps[2].actual.content, /probe\.ready/);
  assert.match(lines[0], /^\[1\/4\] http POST \/api\/login … ok 200 \(\d+ms\)$/);
  assert.equal(lines.some((line) => line.includes('FAIL')), false);
});

test('http/command 断言失败时记录 expected/actual 并返回退出码 1', async (t) => {
  const dir = await tempDir(t);
  const server = http.createServer((req, res) => {
    res.writeHead(503, { 'content-type': 'application/json' });
    res.end(JSON.stringify({ code: 'DOWN' }));
  });
  const port = await listen(server);
  t.after(() => close(server));

  const scenarioPath = join(dir, 'fail.yml');
  await writeFile(scenarioPath, `name: 本地失败路径
config:
  baseUrl: http://127.0.0.1:${port}
steps:
  - http: GET /health
    expect:
      status: 200
      json: { code: OK }
  - command: "printf hello"
    expect:
      exitCode: 7
      stdoutContains: bye
`, 'utf8');

  const scenario = await loadScenario(scenarioPath);
  const lines = [];
  const result = await runScenario(scenario, { log: (line) => lines.push(line) });

  assert.equal(result.ok, false);
  assert.equal(result.exitCode, 1);
  assert.equal(result.summary.failed, 2);
  assert.equal(result.steps[0].ok, false);
  assert.equal(result.steps[0].checks.find((check) => check.name === 'status').actual, 503);
  assert.equal(result.steps[1].checks.find((check) => check.name === 'exitCode').actual, 0);

  const text = lines.join('\n');
  assert.match(text, /\[1\/2\].*… FAIL \(\d+ms\)/);
  assert.match(text, /status: expected 200 \/ actual 503/);
  assert.match(text, /stdoutContains: expected bye \/ actual hello/);
  assert.match(text, /FAIL/);
});
