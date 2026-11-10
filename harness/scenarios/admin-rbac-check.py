#!/usr/bin/env python3
"""T-033 后台 RBAC 真实链路验证。

管理员账号由本轮 psql 按时间戳创建，密码哈希在运行时按 FM_TEST_PW 计算（本地开发 pepper 常量与
local/orbstack profile 一致）；teardown 只删除本轮产生的 admin_user_id，不按前缀清理历史数据。
输出只有状态码、业务码与非敏感摘要，不输出 Token、密码或密钥。
"""
import base64
import hashlib
import json
import os
import secrets
import subprocess
import sys
import time
import urllib.error
import urllib.request

GATEWAY = 'http://127.0.0.1:8080'
ADMIN_DIRECT = 'http://127.0.0.1:8088'
LOGS_PATH = '/api/admin/logs/api-access'
PEPPER = 'foodmap-admin-local-pepper'
ITERATIONS = 120000
DB = ['docker', 'exec', 'foodmap-postgres', 'psql', '-U', 'foodmap', '-d', 'foodmap_admin_db', '-tAc']
CHECKS = []


def psql(sql):
    """执行管理后台库只读/受控 SQL，返回去空白文本。"""
    result = subprocess.run(DB + [sql], capture_output=True, text=True, timeout=30)
    if result.returncode != 0:
        raise AssertionError('psql failed: ' + result.stderr.strip()[:200])
    return result.stdout.strip()


def password_hash(password):
    """按后台密码组件格式生成 PBKDF2 哈希（ASCII 密码与 Java PBEKeySpec 等价）。"""
    salt = secrets.token_bytes(16)
    digest = hashlib.pbkdf2_hmac('sha256', (password + PEPPER).encode(), salt, ITERATIONS, dklen=32)
    encode = base64.b64encode
    return 'PBKDF2WithHmacSHA256$%d$%s$%s' % (ITERATIONS, encode(salt).decode(), encode(digest).decode())


def create_admin(username, with_role):
    """创建本轮测试管理员并可选授予 OPS_VIEWER，返回 admin_user_id。"""
    raw = psql(
        "insert into admin_users (created_time, updated_time, is_delete, admin_user_id, username, "
        "password_hash, display_name, admin_status, permission_version) values "
        "(now(), now(), 0, nextval('admin_user_id_seq'), '%s', '%s', 'verify', 'ACTIVE', 1) "
        "returning admin_user_id" % (username, password_hash(os.environ['FM_TEST_PW'])))
    admin_user_id = [line for line in raw.splitlines() if line.strip().isdigit()][0].strip()
    if with_role:
        psql("insert into admin_user_roles (created_time, updated_time, is_delete, relation_id, "
             "admin_user_id, role_id, granted_by, granted_time) select now(), now(), 0, "
             "nextval('admin_user_role_relation_id_seq'), %s, role_id, null, now() "
             "from admin_roles where role_code='OPS_VIEWER' and is_delete=0" % admin_user_id)
    return admin_user_id


def request(method, url, token=None, body=None, extra_headers=None):
    """发起 HTTP 请求并返回 (status, payload, headers)。"""
    headers = {'Content-Type': 'application/json'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    headers.update(extra_headers or {})
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        response = urllib.request.urlopen(req, timeout=15)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        status = response.status
        raw = response.read()
        resp_headers = dict(response.headers)
    payload = json.loads(raw) if raw else None
    return status, payload, resp_headers


def check(name, method, url, expected_status, expected_code, token=None, body=None, extra_headers=None):
    """断言 HTTP 与统一业务码，登记可观测摘要。"""
    status, payload, _ = request(method, url, token, body, extra_headers)
    assert status == expected_status, '%s: expected HTTP %d, actual %d' % (name, expected_status, status)
    if expected_code is not None:
        assert payload['code'] == expected_code, '%s: unexpected code %s' % (name, payload.get('code'))
    CHECKS.append({'name': name, 'status': status, 'code': expected_code})
    return payload


def login(base, username):
    """登录后台管理员并断言响应契约。"""
    status, payload, headers = request('POST', base + '/api/admin/auth/login',
                                       body={'username': username, 'password': os.environ['FM_TEST_PW']})
    assert status == 200 and payload['code'] == 'OK', 'login %s failed: HTTP %d' % (username, status)
    assert payload['data']['tokenType'] == 'Bearer', 'tokenType must be Bearer'
    assert payload['data']['accessToken'], 'accessToken missing'
    cache_control = headers.get('Cache-Control', headers.get('cache-control', ''))
    assert 'no-store' in cache_control, 'login response must be no-store'
    CHECKS.append({'name': 'login-' + username, 'status': status, 'code': 'OK'})
    return payload['data']['accessToken']


def main():
    stamp = secrets.token_hex(6)
    permitted_user = 'zxq_t033_a_' + stamp
    unpermitted_user = 'zxq_t033_b_' + stamp
    permitted_id = create_admin(permitted_user, True)
    unpermitted_id = create_admin(unpermitted_user, False)
    marker = 't033-' + stamp

    try:
        # 1. 授权管理员登录并经网关查询日志。
        token_a1 = login(GATEWAY, permitted_user)
        token_a2 = login(GATEWAY, permitted_user)
        page = check('permitted-query-via-gateway', 'GET',
                     GATEWAY + LOGS_PATH + '?requestId=' + marker + '&pageSize=1', 200, 'OK', token=token_a1)
        assert page['data']['total'] == 0, 'marker filter must be forwarded to log service'
        assert 'items' in page['data'] and 'hasMore' in page['data'], 'page contract fields missing'

        # 2. 已认证但无权限：伪造权限头也不能绕过服务端授权。
        token_b1 = login(GATEWAY, unpermitted_user)
        check('unpermitted-query-forbidden', 'GET', GATEWAY + LOGS_PATH, 403, 'FORBIDDEN', token=token_b1)
        check('spoofed-admin-header-forbidden', 'GET', GATEWAY + LOGS_PATH, 403, 'FORBIDDEN',
              token=token_b1, extra_headers={'X-Admin-Permissions': 'LOG_ACCESS_READ',
                                             'x-admin-user-id': '1'})

        # 3. 未登录、伪造头、普通 App Token 形态的凭证与后台 Token 越界。
        check('missing-token-unauthorized', 'GET', GATEWAY + LOGS_PATH, 401, 'UNAUTHORIZED')
        check('garbage-bearer-unauthorized', 'GET', GATEWAY + LOGS_PATH, 401, 'UNAUTHORIZED', token='not-a-jwt')
        check('direct-forged-header-unauthorized', 'GET', ADMIN_DIRECT + LOGS_PATH, 401, 'UNAUTHORIZED',
              extra_headers={'X-Admin-User-Id': permitted_id, 'X-Admin-Permissions': 'LOG_ACCESS_READ'})
        check('admin-token-on-app-api-unauthorized', 'GET', GATEWAY + '/api/users/me', 401,
              'UNAUTHORIZED', token=token_a2)
        check('tampered-admin-token-unauthorized', 'GET', GATEWAY + LOGS_PATH, 401, 'UNAUTHORIZED',
              token=token_a2[:-3] + 'xyz')

        # 直连后台服务必须执行同一套校验：有效 Token 直连可查。
        check('direct-valid-token-allowed', 'GET', ADMIN_DIRECT + LOGS_PATH + '?pageSize=1', 200,
              'OK', token=token_a2)

        # 4. 登出撤销：原 Token 401，另一有效会话仍可查。
        check('logout-ok', 'POST', GATEWAY + '/api/admin/auth/logout', 200, 'OK', token=token_a1)
        check('logout-token-rejected', 'GET', GATEWAY + LOGS_PATH, 401, 'UNAUTHORIZED', token=token_a1)
        check('other-session-still-valid', 'GET', GATEWAY + LOGS_PATH + '?pageSize=1', 200, 'OK',
              token=token_a2)

        # 5. 账号停用：既有 Token 立即 401；恢复后重新登录无权限返回 403。
        psql("update admin_users set admin_status='DISABLED' where admin_user_id=%s" % unpermitted_id)
        check('disabled-admin-token-rejected', 'GET', GATEWAY + LOGS_PATH, 401, 'UNAUTHORIZED',
              token=token_b1)
        psql("update admin_users set admin_status='ACTIVE' where admin_user_id=%s" % unpermitted_id)
        token_b2 = login(GATEWAY, unpermitted_user)
        check('relogin-without-role-forbidden', 'GET', GATEWAY + LOGS_PATH, 403, 'FORBIDDEN',
              token=token_b2)

        # 6. 授权撤销：回收角色并递增权限版本后，旧 Token 401。
        psql("update admin_user_roles set is_delete=1 where admin_user_id=%s" % permitted_id)
        psql("update admin_users set permission_version=permission_version+1 where admin_user_id=%s"
             % permitted_id)
        check('permission-version-bump-rejects-old-token', 'GET', GATEWAY + LOGS_PATH, 401,
              'UNAUTHORIZED', token=token_a2)

        # 7. Flyway 种子与审计可追溯。
        roles = psql("select count(*) from admin_roles where role_code='OPS_VIEWER' and is_delete=0")
        permissions = psql("select count(*) from admin_permissions where permission_code='LOG_ACCESS_READ' and is_delete=0")
        assert roles == '1' and permissions == '1', 'V2 seed missing: roles=%s permissions=%s' % (roles, permissions)
        login_audit = psql("select count(*) from admin_operation_logs where admin_user_id in (%s,%s) "
                           "and operation_type='LOGIN' and operation_result='SUCCESS'"
                           % (permitted_id, unpermitted_id))
        logout_audit = psql("select count(*) from admin_operation_logs where admin_user_id=%s "
                            "and operation_type='LOGOUT' and operation_result='SUCCESS'" % permitted_id)
        assert int(login_audit) >= 3, 'login audit rows missing: %s' % login_audit
        assert int(logout_audit) >= 1, 'logout audit row missing: %s' % logout_audit
        CHECKS.append({'name': 'audit-rows-present', 'status': 200, 'code': 'OK'})
    finally:
        # teardown：只删除本轮生成的管理员主键相关行，并断言清理干净。
        for table in ('admin_operation_logs', 'admin_access_sessions', 'admin_user_roles', 'admin_users'):
            psql('delete from %s where admin_user_id in (%s,%s)' % (table, permitted_id, unpermitted_id))
        remaining = psql('select count(*) from admin_users where admin_user_id in (%s,%s)'
                         % (permitted_id, unpermitted_id))
        assert remaining == '0', 'teardown failed: %s rows remaining' % remaining

    print(json.dumps({'result': 'pass', 'checks': CHECKS,
                      'admins': [permitted_id, unpermitted_id]}, ensure_ascii=False))


if __name__ == '__main__':
    try:
        main()
    except AssertionError as failure:
        print(json.dumps({'result': 'fail', 'reason': str(failure)[:500]}, ensure_ascii=False))
        sys.exit(1)
