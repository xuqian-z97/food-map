# FoodMap Harness

## 1. 目的

`harness/` 是 FoodMap 多代理开发模式下的约束和验收脚手架。

它用于帮助主代理和子代理在同一套规则下工作，避免：

- 子代理修改不该修改的文件。
- 文档和代码漂移。
- 前后端 API / DTO 不一致。
- 权限、隐私、可见范围规则遗漏。
- PUBLIC 统计口径错误。
- 迭代完成但未统一验收。

## 2. 使用原则

当前阶段采用轻量 harness：

- 先用文档和清单约束开发。
- 再用脚本做基础自动检查。
- 后端和 iOS 工程已经生成部分代码，脚本会按当前落地范围逐步增强。
- 对已落地模块执行更具体检查，对尚未形成稳定构建命令的模块保留提示型检查。

## 3. 文件说明

```text
harness
├── README.md
├── agent-task-template.md
├── acceptance-checklist.md
├── file-boundary-rules.md
├── api-contract-checklist.md
└── scripts
    ├── run-all.sh
    ├── validate-docs.sh
    ├── validate-backend.sh
    ├── validate-integration.sh
    ├── validate-ios.sh
    ├── validate-api.sh
    └── validate-git.sh
```

## 4. 推荐使用流程

1. 主代理根据 `agent-task-template.md` 拆分任务。
2. 子代理按 `file-boundary-rules.md` 执行。
3. 接口任务按 `api-contract-checklist.md` 设计和验收。
4. 前后端联调前，在 `docs/integration/<iteration>-<feature>/` 下生成 `integration-plan.md` 和 `issue-log.md`。
5. 联调完成前按 `integration-plan.md`、`issue-log.md` 和 `acceptance-checklist.md` 检查。
6. 执行：

```sh
./harness/scripts/run-all.sh
```

## 5. 当前脚本策略

当前项目已经生成部分后端和 iOS 代码，因此脚本会：

- 强制检查文档、skills、agents、harness 是否存在。
- 强制检查核心规则是否保留。
- 对 `after/` 后端工程执行基础 Maven、模块、日志平台和联调文档检查。
- 对 `front/FoodMapApp` iOS 工程执行轻量结构检查；项目 scheme、签名和可重复构建命令稳定后，再增强为真实 iOS build/test。
- 对 `docs/integration/` 执行联调说明、问题记录、联调安全点和 `requestId/traceId` 基线检查。
- 对尚未实现的业务模块保持轻量检查，避免把未开始的功能误判为失败。

## 6. 核心规则

harness 必须始终保护以下规则：

- 后端采用 Java 微服务。
- 每个后端服务独立数据库。
- 服务之间不能直接访问彼此数据库表。
- 后端强制校验推荐可见范围。
- 只有 `PUBLIC / 全部公开` 推荐进入全站统计。
- 菜名必须是纯文字。
- 重要迭代需要提交并推送 GitHub。

## 7. 本地依赖感知验证（方案 D）

验证纪律以 [AGENTS.md §6.3.1](../AGENTS.md#631-验证纪律) 为准。

```sh
# 日常：只读预检 → 能跑的真跑，其余打印 SKIP 并写证据
./harness/scripts/verify.sh
# 只校验场景，不访问 Docker/网络/模拟器，不需要真实密码
./harness/scripts/verify.sh --dry-run
# 验收：先在进程环境设置 FM_TEST_PW；任何跳过都失败
VERIFY_REQUIRE_FULL=1 ./harness/scripts/verify.sh --json
# 统一检查包含上述日常入口；严格模式也可用于整个 run-all
./harness/scripts/run-all.sh
```

- 预检依据 [依赖清单](verify-dependencies.json)：网关路由、数据库 `select 1`、Redis PING、**指定**模拟器/已安装 App、Xcode、认证日志文件、密码环境变量。权限受限与服务未起都只记为“不可确认就绪”，不自动启动服务或删除数据。
- 环境准备：先在 OrbStack 中启动本地依赖，启动 Gateway/Auth/User，将认证服务日志输出到 `/tmp/verify-auth.log`；按场景配置启动指定模拟器并安装 Debug App。密码只通过 `FM_TEST_PW` 注入，不打印、不自动填默认值。
- 日常模式返回 0 只说明“已执行部分无失败”；`ok=false,status=partial` 和 `summary.skipped>0` 明确表示**未全量通过**。严格模式跳过返回 1；配置/语法错误返回 2。文本、JSON 和 JUnit 均保留 SKIP。
- 场景 `requires` 声明 shell 隐藏依赖，例如 `requires: [sql, redis]`；通用工具只补充动作显式依赖，不猜 shell 中的业务。新场景的自定义依赖必须在清单配置只读检查，未配置则跳过，严格验收失败。
- 包装脚本兼容 macOS Bash 3.2 与 POSIX sh，不使用空 Bash 数组。`--json` 保持 stdout 纯 JSON；预检与进度到 stderr。
- `--evidence-dir` 可覆盖默认 `/tmp/verify-evidence`；`--junit /tmp/verify.xml` 生成机器报告。`--dependencies <JSON>` 可显式覆盖预检清单。跳过不生成业务验收通过结论；真实全量通过后按规则写台账。
- 普通的 `node tools/verify/cli.mjs run harness/scenarios/` **不自动预检/跳过**，保持原有全部执行语义。
