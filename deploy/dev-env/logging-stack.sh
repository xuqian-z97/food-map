#!/usr/bin/env sh
# 日志闭环备用实例：18081 认证 + 18089 日志消费（不替换 8080/8081/8082）。
#
#   ./deploy/dev-env/logging-stack.sh start    # 启动（缺 classpath 时自动生成）
#   ./deploy/dev-env/logging-stack.sh status   # 只读检查两个端口
#   ./deploy/dev-env/logging-stack.sh stop     # 停止本脚本启动的进程
#
# 依赖：本机 PostgreSQL/Redis/Kafka/Elasticsearch 容器已运行；Maven 可离线或联网解析依赖。
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
cd "$ROOT_DIR"

LOG_DIR="${FOODMAP_LOGGING_DIR:-/tmp/foodmap-orbstack-logs}"
RUN_DIR="${FOODMAP_DEV_ENV_RUN_DIR:-/tmp/foodmap-dev-env}"
MVN="${MVN:-mvn}"
REPO_DIR="${FOODMAP_MAVEN_REPO:-$HOME/.m2/repository}"
AUTH_PORT=18081
LOG_PORT=18089

mkdir -p "$LOG_DIR" "$RUN_DIR"

alive() { curl -fsS -m 3 -o /dev/null "$1" 2>/dev/null; }

ensure_classpath() {
  module="$1"
  if [ -s "after/$module/target/cp.txt" ] && [ -d "after/$module/target/classes" ]; then
    return 0
  fi
  printf '生成 %s 的 classpath（首次或 target 被清理时）\n' "$module"
  ( cd after && "$MVN" -q -Dmaven.repo.local="$REPO_DIR" -N install \
    && "$MVN" -q -Dmaven.repo.local="$REPO_DIR" -pl foodmap-common -DskipTests install \
    && "$MVN" -q -Dmaven.repo.local="$REPO_DIR" -pl "$module" dependency:build-classpath -Dmdep.outputFile=target/cp.txt )
}

start_one() {
  console="$1"; module="$2"; main_class="$3"; port="$4"; health="$5"; shift 5
  pidfile="$RUN_DIR/$console.pid"
  if alive "$health"; then
    printf '%-10s 已在运行：%s\n' "$console" "$health"
    return 0
  fi
  if [ -f "$pidfile" ] && kill -0 "$(cat "$pidfile")" 2>/dev/null; then
    printf '%-10s 已在运行（pid %s）\n' "$console" "$(cat "$pidfile")"
    return 0
  fi
  ensure_classpath "$module"
  env "$@" nohup java -cp "after/$module/target/classes:$(cat "after/$module/target/cp.txt")" \
    "$main_class" --spring.profiles.active=local > "$LOG_DIR/$console.dev-env.log" 2>&1 &
  echo $! > "$pidfile"
  printf '%-10s 启动中（pid %s，端口 %s）\n' "$console" "$(cat "$pidfile")" "$port"
  i=0
  while [ "$i" -lt 40 ]; do
    if alive "$health"; then
      printf '%-10s 就绪：%s\n' "$console" "$health"
      return 0
    fi
    sleep 2
    i=$((i + 1))
  done
  printf '%-10s 启动超时，查看 %s/%s.dev-env.log\n' "$console" "$LOG_DIR" "$console" >&2
  return 1
}

case "${1:-status}" in
  start)
    start_one auth foodmap-auth-service com.foodmap.auth.AuthServiceApplication "$AUTH_PORT" \
      "http://127.0.0.1:$AUTH_PORT/internal/auth/health" \
      "FOODMAP_LOGGING_DIR=$LOG_DIR" "SERVER_PORT=$AUTH_PORT" "NACOS_DISCOVERY_ENABLED=false" \
      "AUTH_USER_SERVICE_URL=http://127.0.0.1:8082"
    start_one log-service foodmap-log-service com.foodmap.log.LogServiceApplication "$LOG_PORT" \
      "http://127.0.0.1:$LOG_PORT/actuator/health" \
      "FOODMAP_LOGGING_DIR=$LOG_DIR" "SERVER_PORT=$LOG_PORT" "NACOS_DISCOVERY_ENABLED=false" \
      "LOG_SERVICE_API_ACCESS_CONSUMER_ENABLED=true" "LOG_KAFKA_BROKERS=127.0.0.1:9094"
    ;;
  status)
    for pair in "auth:$AUTH_PORT:http://127.0.0.1:$AUTH_PORT/internal/auth/health" \
                "log-service:$LOG_PORT:http://127.0.0.1:$LOG_PORT/actuator/health"; do
      name="${pair%%:*}"; rest="${pair#*:}"; port="${rest%%:*}"; url="${rest#*:}"
      if alive "$url"; then
        printf '%-10s 端口 %s 可达\n' "$name" "$port"
      else
        printf '%-10s 端口 %s 不可达\n' "$name" "$port"
      fi
    done
    ;;
  stop)
    for name in auth log-service; do
      pidfile="$RUN_DIR/$name.pid"
      if [ -f "$pidfile" ]; then
        pid="$(cat "$pidfile")"
        if kill -0 "$pid" 2>/dev/null; then
          kill "$pid" 2>/dev/null || true
          printf '%-10s 已停止（pid %s）\n' "$name" "$pid"
        fi
        rm -f "$pidfile"
      else
        printf '%-10s 没有由本脚本记录的进程\n' "$name"
      fi
    done
    ;;
  *)
    printf '用法: %s {start|status|stop}\n' "$0" >&2
    exit 2
    ;;
esac
