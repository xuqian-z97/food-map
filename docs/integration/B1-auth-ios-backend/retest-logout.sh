#!/usr/bin/env sh
# B1 登出链路接口级复测：注册 → 登录 → 当前用户 → 登出 → 旧 Access/Refresh 失效
# 用法：bash docs/integration/B1-auth-ios-backend/retest-logout.sh [gateway地址]
# 前置：gateway/auth/user 三服务与 PostgreSQL、Redis、Nacos 已启动
# 说明：只打印状态码与业务码，不打印任何 Token 明文
set -u

BASE="${1:-http://127.0.0.1:8080}"
ACCOUNT="zxq_retest_$(date +%H%M%S)"
PASSWORD="Foodmap@123"
TMP="$(mktemp -d)"

field() { python3 -c "import sys,json;d=json.load(sys.stdin);v=d.get('$1','');print(v if v is not None else '')"; }
deep() { python3 -c "import sys,json;print(json.load(sys.stdin)['data']['$1'])"; }

printf '== 1) 注册（phone/email 留空，验证可选字段）==\n'
REG=$(curl -s -X POST "$BASE/api/auth/register" -H 'Content-Type: application/json' \
  -d "{\"accountName\":\"$ACCOUNT\",\"phone\":\"\",\"email\":\"\",\"password\":\"$PASSWORD\",\"nickname\":\"retest\",\"registeredChannel\":\"IOS\"}")
printf 'status=%s code=%s\n' "$(printf '%s' "$REG" | field status)" "$(printf '%s' "$REG" | field code)"

printf '== 2) 登录 ==\n'
LOGIN=$(curl -s -X POST "$BASE/api/auth/login" -H 'Content-Type: application/json' \
  -d "{\"loginIdentifier\":\"$ACCOUNT\",\"password\":\"$PASSWORD\"}")
printf 'status=%s code=%s\n' "$(printf '%s' "$LOGIN" | field status)" "$(printf '%s' "$LOGIN" | field code)"
ACCESS=$(printf '%s' "$LOGIN" | deep accessToken)
REFRESH=$(printf '%s' "$LOGIN" | deep refreshToken)
printf 'accessToken 长度=%s refreshToken 长度=%s\n' "${#ACCESS}" "${#REFRESH}"

printf '== 3) 携带 Access 访问当前用户（期望 200）==\n'
printf 'http=%s body=%s\n' \
  "$(curl -s -o "$TMP/me1.json" -w '%{http_code}' "$BASE/api/users/me" -H "Authorization: Bearer $ACCESS")" \
  "$(field code < "$TMP/me1.json")"

printf '== 4) 登出：同时携带 Access 与 Refresh（期望 200）==\n'
printf 'http=%s body=%s\n' \
  "$(curl -s -o "$TMP/logout.json" -w '%{http_code}' -X POST "$BASE/api/auth/logout" \
    -H 'Content-Type: application/json' -H "Authorization: Bearer $ACCESS" \
    -d "{\"refreshToken\":\"$REFRESH\"}")" \
  "$(field code < "$TMP/logout.json")"

printf '== 5) 旧 Access 访问当前用户（期望 401）==\n'
printf 'http=%s body=%s\n' \
  "$(curl -s -o "$TMP/me2.json" -w '%{http_code}' "$BASE/api/users/me" -H "Authorization: Bearer $ACCESS")" \
  "$(field code < "$TMP/me2.json")"

printf '== 6) 旧 Refresh 刷新（期望 401）==\n'
printf 'http=%s body=%s\n' \
  "$(curl -s -o "$TMP/refresh.json" -w '%{http_code}' -X POST "$BASE/api/auth/refresh" \
    -H 'Content-Type: application/json' -d "{\"refreshToken\":\"$REFRESH\"}")" \
  "$(field code < "$TMP/refresh.json")"

printf '== 7) 无 Token 访问当前用户（期望 401）==\n'
printf 'http=%s\n' "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/users/me")"

printf '== 8) 外部访问内部接口（期望 403）==\n'
printf 'http=%s\n' "$(curl -s -o /dev/null -w '%{http_code}' "$BASE/internal/users/bootstrap")"

printf '测试账号：%s\n' "$ACCOUNT"
rm -rf "$TMP"
