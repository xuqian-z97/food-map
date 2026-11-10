#!/usr/bin/env python3
"""内部签名真实链路回归；密钥仅从环境读取，输出只有状态码与非敏感摘要。

管理员日志查询正向用例自 T-033 起改为先经后台登录取得 Access Token，再直连管理后台验证
admin -> log 的真实内部签名调用；账号按时间戳创建并在结束时清理。
"""
import base64
import hashlib
import hmac
import json
import os
import secrets
import subprocess
import time
import urllib.error
import urllib.request

SECRET = os.environ['FOODMAP_INTERNAL_SECRET'].encode()
PATH = '/internal/logs/api-access'
CHECKS = []


def headers(service, path, method='GET', body=b'', timestamp=None, nonce=None):
    """生成与 common 一致的六行签名，允许构造过期、重放和越权请求。"""
    timestamp = timestamp if timestamp is not None else int(time.time() * 1000)
    nonce = nonce or secrets.token_hex(16)
    canonical = '\n'.join([service, str(timestamp), nonce, method, path,
                           hashlib.sha256(body).hexdigest()])
    signature = base64.urlsafe_b64encode(hmac.new(SECRET, canonical.encode(), hashlib.sha256).digest()).decode().rstrip('=')
    return {'X-FoodMap-Service': service, 'X-FoodMap-Timestamp': str(timestamp),
            'X-FoodMap-Nonce': nonce, 'X-FoodMap-Signature': signature}


def check(name, url, expected, identity=None, body=None, expected_code=None):
    """发出请求并断言 HTTP 与统一业务码；不输出请求头、密钥或响应正文。"""
    request_headers = {'Content-Type': 'application/json', **(identity or {})}
    request = urllib.request.Request(url, data=body, headers=request_headers)
    try:
        response = urllib.request.urlopen(request, timeout=10)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        status = response.status
        raw = response.read()
    assert status == expected, f'{name}: expected HTTP {expected}, actual {status}'
    payload = json.loads(raw) if raw else None
    if expected_code is not None:
        assert payload['code'] == expected_code, f'{name}: unexpected business code'
    CHECKS.append({'name': name, 'status': status})
    return payload


# 关闭 locator 后的外部攻击面与显式健康检查。
check('locator-disabled', 'http://127.0.0.1:8080/foodmap-log-service' + PATH, 404)
check('explicit-auth-health', 'http://127.0.0.1:8080/internal/auth/health', 200)
check('explicit-user-health', 'http://127.0.0.1:8080/internal/users/health', 200)
check('user-missing-signature', 'http://127.0.0.1:8082/internal/users/provision', 401,
      body=b'{}', expected_code='UNAUTHORIZED')
check('log-missing-signature', 'http://127.0.0.1:8089' + PATH, 401, expected_code='UNAUTHORIZED')
valid = headers('foodmap-admin-service', PATH)
check('log-valid-signature', 'http://127.0.0.1:8089' + PATH + '?pageSize=1', 200, valid, expected_code='OK')
check('log-replayed-nonce', 'http://127.0.0.1:8089' + PATH, 401, valid, expected_code='UNAUTHORIZED')
check('log-expired-timestamp', 'http://127.0.0.1:8089' + PATH, 401,
      headers('foodmap-admin-service', PATH, timestamp=int(time.time() * 1000) - 600000), expected_code='UNAUTHORIZED')
check('log-wrong-service', 'http://127.0.0.1:8089' + PATH, 403,
      headers('foodmap-auth-service', PATH), expected_code='FORBIDDEN')
provision_path = '/internal/users/provision'
check('provision-wrong-service', 'http://127.0.0.1:8082' + provision_path, 403,
      headers('foodmap-admin-service', provision_path, 'POST', b'{}'), b'{}', 'FORBIDDEN')
tampered = headers('foodmap-admin-service', PATH)
tampered['X-FoodMap-Signature'] = 'invalid'
check('log-tampered-signature', 'http://127.0.0.1:8089' + PATH, 401, tampered, expected_code='UNAUTHORIZED')
check('user-health-exempt', 'http://127.0.0.1:8082/internal/users/health', 200)
def _psql(sql):
    """在管理后台库执行受控 SQL，返回去空白文本。"""
    result = subprocess.run(
        ['docker', 'exec', 'foodmap-postgres', 'psql', '-U', 'foodmap', '-d', 'foodmap_admin_db', '-tAc', sql],
        capture_output=True, text=True, timeout=30)
    if result.returncode != 0:
        raise AssertionError('psql failed: ' + result.stderr.strip()[:200])
    return result.stdout.strip()


def create_probe_admin():
    """创建本轮探针管理员并授予 OPS_VIEWER，密码哈希运行时按 FM_TEST_PW 计算。"""
    username = 'zxq_t032_' + secrets.token_hex(6)
    salt = secrets.token_bytes(16)
    digest = hashlib.pbkdf2_hmac('sha256',
                                 (os.environ['FM_TEST_PW'] + 'foodmap-admin-local-pepper').encode(),
                                 salt, 120000, dklen=32)
    password_hash_value = 'PBKDF2WithHmacSHA256$120000$%s$%s' % (
        base64.b64encode(salt).decode(), base64.b64encode(digest).decode())
    raw = _psql(
        "insert into admin_users (created_time, updated_time, is_delete, admin_user_id, username, "
        "password_hash, display_name, admin_status, permission_version) values "
        "(now(), now(), 0, nextval('admin_user_id_seq'), '%s', '%s', 'verify', 'ACTIVE', 1) "
        "returning admin_user_id" % (username, password_hash_value))
    admin_id = [line for line in raw.splitlines() if line.strip().isdigit()][0].strip()
    _psql("insert into admin_user_roles (created_time, updated_time, is_delete, relation_id, "
          "admin_user_id, role_id, granted_by, granted_time) select now(), now(), 0, "
          "nextval('admin_user_role_relation_id_seq'), %s, role_id, null, now() "
          "from admin_roles where role_code='OPS_VIEWER' and is_delete=0" % admin_id)
    return {'id': admin_id, 'username': username}


def admin_login(admin):
    """直连管理后台登录并返回 Access Token。"""
    body = json.dumps({'username': admin['username'], 'password': os.environ['FM_TEST_PW']}).encode()
    req = urllib.request.Request('http://127.0.0.1:8088/api/admin/auth/login', data=body,
                                 headers={'Content-Type': 'application/json'}, method='POST')
    with urllib.request.urlopen(req, timeout=15) as response:
        payload = json.loads(response.read())
    assert payload['code'] == 'OK', 'admin login failed: %s' % payload.get('code')
    return payload['data']['accessToken']


def cleanup_probe_admin(admin_id):
    """只删除本轮探针管理员相关行并断言清零。"""
    for table in ('admin_operation_logs', 'admin_access_sessions', 'admin_user_roles', 'admin_users'):
        _psql('delete from %s where admin_user_id=%s' % (table, admin_id))
    assert _psql('select count(*) from admin_users where admin_user_id=%s' % admin_id) == '0', \
        'probe admin cleanup failed'


# T-033 后：管理员必须先登录取得后台 Token，旧 X-Admin-* 请求头不再具备授权能力。
marker = 'internal-auth-probe-' + secrets.token_hex(12)
admin = create_probe_admin()
try:
    token = admin_login(admin)
    page = check('admin-login-then-query-log',
                 'http://127.0.0.1:8088/api/admin/logs/api-access?requestId=' + marker + '&pageSize=1',
                 200, {'Authorization': 'Bearer ' + token}, expected_code='OK')
    assert page['data']['total'] == 0, 'admin query filter was not forwarded'
    check('legacy-admin-header-rejected',
          'http://127.0.0.1:8088/api/admin/logs/api-access?requestId=' + marker + '&pageSize=1',
          401, {'X-Admin-User-Id': '1', 'X-Admin-Permissions': 'LOG_ACCESS_READ'},
          expected_code='UNAUTHORIZED')
finally:
    cleanup_probe_admin(admin['id'])
print(json.dumps({'result': 'pass', 'checks': CHECKS, 'adminRbacValidated': True}, ensure_ascii=False))
