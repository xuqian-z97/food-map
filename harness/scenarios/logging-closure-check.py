#!/usr/bin/env python3
"""真实请求日志闭环探针；只输出非敏感摘要，密码仅驻留进程内存。"""
import datetime
import json
import os
from pathlib import Path
import secrets
import subprocess
import time
import urllib.error
import urllib.request

BASE = os.environ.get('LOGGING_TEST_BASE_URL', 'http://127.0.0.1:18081')
ES = os.environ.get('LOGGING_TEST_ES_URL', 'http://127.0.0.1:9200')
LOG_DIR = Path(os.environ.get('FOODMAP_LOGGING_DIR', '/tmp/foodmap-orbstack-logs'))
RUN = 'log-e2e-' + datetime.datetime.now().strftime('%Y%m%d%H%M%S') + '-' + secrets.token_hex(3)


def request(path, rid, payload=None, response_data=None):
    """发起真实请求，并要求响应透传指定流水号。"""
    body = None if payload is None else json.dumps(payload).encode()
    req = urllib.request.Request(BASE + path, data=body,
        headers={'Content-Type': 'application/json', 'X-Request-Id': rid, 'X-Trace-Id': rid})
    try:
        response = urllib.request.urlopen(req, timeout=10)
    except urllib.error.HTTPError as ex:
        response = ex
    with response:
        assert response.headers.get('X-Request-Id') == rid, 'response requestId mismatch'
        if response_data is not None:
            response_data.update(json.load(response))
        return response.status


def es_hits(rid):
    """按流水号查询真实 Elasticsearch 索引。"""
    req = urllib.request.Request(ES + '/foodmap-logs-*/_search',
        data=json.dumps({'query': {'term': {'requestId': rid}}, 'size': 20}).encode(),
        headers={'Content-Type': 'application/json'})
    with urllib.request.urlopen(req, timeout=10) as response:
        return json.load(response)['hits']['hits']


def sql_rows(rid):
    """仅访问日志库，读取访问摘要与 Kafka 消费位点。"""
    query = "select row_to_json(t) from (select request_id,trace_id,service_name,event_name,http_method,request_path,http_status,duration_ms,source_topic,source_partition,source_offset from api_access_log where request_id='" + rid + "') t"
    result = subprocess.run(['docker', 'exec', 'foodmap-postgres', 'psql', '-U', 'foodmap',
        '-d', 'foodmap_log_db', '-tAc', query], capture_output=True, text=True, check=True)
    return [json.loads(line) for line in result.stdout.splitlines() if line.strip()]


def verify(rid, status, method, path):
    """等待异步链路完成，断言文件、ES、数据库字段及消费位点一致。"""
    for _ in range(45):
        docs = es_hits(rid)
        rows = sql_rows(rid)
        # SQL/application 日志可能先于 api-access 到达 ES，不能以任意命中提前结束等待。
        access_docs = [h['_source'] for h in docs if h['_source'].get('logType') == 'api-access']
        if access_docs and rows:
            break
        time.sleep(1)
    else:
        raise AssertionError('async closure timeout for ' + rid)
    source = access_docs[0]
    assert source['requestId'] == rid and source['traceId'] == rid
    assert source['eventName'] == 'api.access.completed'
    assert not set(source.get('tags', [])) & {'_timestampparsefailure', 'foodmap_timestamp_parse_failed'}
    row = rows[0]
    assert row['http_status'] == status and row['http_method'] == method and row['request_path'] == path
    assert row['event_name'] == 'api.access.completed' and row['duration_ms'] >= 0
    assert row['source_topic'] == 'foodmap.logs.api-access' and row['source_offset'] >= 0
    logs = [json.loads(line) for file in LOG_DIR.glob('*.log') for line in file.read_text().splitlines() if rid in line]
    access = next(line for line in logs if line.get('logType') == 'api-access')
    assert access['serviceName'] == row['service_name'] == source['serviceName']
    assert access['message'] == source['message']
    assert row['trace_id'] == rid == access['traceId']
    fields = dict(token.split('=', 1) for token in access['message'].split() if '=' in token)
    assert int(fields['durationMs']) == row['duration_ms']
    # 按摘要保存的精确分区和位点直接消费 Kafka，证明同一条日志真正经过中间件。
    kafka = subprocess.run(['docker', 'exec', 'foodmap-kafka', '/opt/kafka/bin/kafka-console-consumer.sh',
        '--bootstrap-server', 'localhost:9092', '--topic', row['source_topic'],
        '--partition', str(row['source_partition']), '--offset', str(row['source_offset']),
        '--max-messages', '1', '--timeout-ms', '10000'], capture_output=True, text=True, check=True, timeout=20)
    event = json.loads(kafka.stdout.splitlines()[0])
    assert event['requestId'] == rid and event['message'] == access['message']
    assert 'status=' + str(status) in access['message']
    return {'requestId': rid, 'status': status, 'path': path, 'eventName': row['event_name'],
        'durationMs': row['duration_ms'], 'kafkaPartition': row['source_partition'],
        'kafkaOffset': row['source_offset'], 'esHits': len(docs), 'databaseRows': len(rows)}


health_rid = RUN + '-health'
login_rid = RUN + '-login'
password = secrets.token_urlsafe(30)
assert request('/internal/auth/health', health_rid) == 200
assert request('/api/auth/login', login_rid,
    {'loginIdentifier': RUN + '-nonexistent', 'password': password}) == 401
results = [verify(health_rid, 200, 'GET', '/internal/auth/health'),
           verify(login_rid, 401, 'POST', '/api/auth/login')]
for file in LOG_DIR.glob('*.log'):
    assert password not in file.read_text(), 'sensitive input leaked to file log'
for rid in [health_rid, login_rid]:
    assert password not in json.dumps(es_hits(rid)), 'sensitive input leaked to ES'
# 真实注册和成功登录，凭据与返回 Token 只驻留内存；注销本轮会话。
account = RUN + '-account'
register_rid = RUN + '-register'
success_rid = RUN + '-success'
logout_rid = RUN + '-logout'
assert request('/api/auth/register', register_rid, {'accountName': account, 'password': password,
    'nickname': 'logging-closure', 'registeredChannel': 'IOS'}) == 200
session = {}
assert request('/api/auth/login', success_rid,
    {'loginIdentifier': account, 'password': password}, response_data=session) == 200
assert request('/api/auth/logout', logout_rid,
    {'refreshToken': session['data']['refreshToken']}) == 200
results.extend([verify(register_rid, 200, 'POST', '/api/auth/register'),
    verify(success_rid, 200, 'POST', '/api/auth/login'),
    verify(logout_rid, 200, 'POST', '/api/auth/logout')])
for file in LOG_DIR.glob('*.log'):
    text = file.read_text()
    assert password not in text
    assert session['data']['accessToken'] not in text
    assert session['data']['refreshToken'] not in text
for rid in [register_rid, success_rid, logout_rid]:
    text = json.dumps(es_hits(rid))
    assert password not in text and session['data']['accessToken'] not in text
    assert session['data']['refreshToken'] not in text
print(json.dumps({'result': 'pass', 'checks': results, 'passwordLeak': False, 'tokenLeak': False}, ensure_ascii=False))
