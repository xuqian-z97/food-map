# T-030 日志闭环端到端 verify 场景

- 功能点：F-019｜优先级：P0｜波次：W1｜组：D（第 3 步）｜预估：1d｜依赖：T-028、T-029 已合并

## 1. 任务背景

日志平台至今只有「代码写了、单测过了」的证据。本任务用可观测事实证明数据真的流动：文件、Kafka→ES、日志 PostgreSQL 三处都能按 requestId 查到。

## 2. 必读文档

`AGENTS.md` 6.3 与 6.3.1、`tools/verify/README.md`、`harness/scenarios/README.md`、`harness/scenarios/b1-logout.yml`（写法参照）、`harness/verify-dependencies.json`。

## 3. 允许修改范围

```text
harness/scenarios/b1-logging-closure.yml（新建）
harness/verify-dependencies.json（仅新增依赖分组，如 es、kafka、log-db）
harness/scenarios/README.md（补一行场景说明）
```

## 4. 禁止修改范围

```text
after/、front/、deploy/、tools/verify/ 核心代码
status/**（运行记录由主代理用 verify-record.sh 写入）
```

## 5. 具体交付物

场景步骤：

1. 用时间戳生成账号名，注册并登录；密码来自环境变量 `FM_TEST_PW`，场景中不得出现明文。
2. 捕获响应头 requestId。
3. 轮询（间隔与超时可配）查 `foodmap_log_db.api_access_log` 存在该 requestId 的行。
4. 查 Elasticsearch：按 requestId 检索至少 1 条日志。
5. 断言命中日志含 traceId、serviceName，且正文不含密码与完整 Token。
6. 依赖通过 `requires` 声明，缺依赖时按方案 D 显式 SKIP 并写明原因。

## 6. 验收标准

- [ ] `./harness/scripts/verify.sh --dry-run` 通过。
- [ ] 依赖就绪时 `VERIFY_REQUIRE_FULL=1 ./harness/scripts/verify.sh` 通过。
- [ ] 场景只断言可观测事实，不含主观判断。
- [ ] 无 Token、密码、完整手机号出现在场景文件中。
- [ ] 依赖不可用时汇报写明「未执行 + 原因」。

## 7. 汇报格式

```text
完成内容：
修改文件：
验证结果：
- 已执行：
- 未执行：…，原因：…
风险和后续：（建议主代理执行 verify-record.sh 登记 runs）
```
