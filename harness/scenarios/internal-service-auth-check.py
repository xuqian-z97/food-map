#!/usr/bin/env python3
"""内部签名真实链路回归；密钥仅从环境读取，输出只有状态码与非敏感摘要。"""
import base64
import hashlib
import hmac
import json
import os
import secrets
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
# 当前管理员身份仍是 T-033 待整改的请求头骨架；此处只验证 admin -> log 自动签名。
marker = 'internal-auth-probe-' + secrets.token_hex(12)
page = check('admin-http-client-to-log',
             'http://127.0.0.1:8088/api/admin/logs/api-access?requestId=' + marker + '&pageSize=1',
             200, {'X-Admin-User-Id': '1', 'X-Admin-Permissions': 'LOG_ACCESS_READ'}, expected_code='OK')
assert page['data']['total'] == 0, 'admin query filter was not forwarded'
print(json.dumps({'result': 'pass', 'checks': CHECKS, 'adminRbacValidated': False}, ensure_ascii=False))
