# OrbStack 日志链路问题记录

## 1. 问题总览

| 编号 | 问题 | 严重级别 | 责任侧 | 复测 |
| --- | --- | --- | --- | --- |
| LOG-ORB-001 | Docker Hub 直连 EOF，旧 Bitnami Kafka 标签 404 | Major | 环境/部署 | 用本机代理获取并校验官方 ARM64 镜像，改 Apache Kafka 3.9.1 后健康 |
| LOG-ORB-002 | 宿主机文件不在 Fluent Bit 命名卷内 | Major | 部署 | 新增 host Compose 覆盖，共享同一绝对目录后采集成功 |
| LOG-ORB-003 | Logstash 参数 template_management 无效 | Major | 部署 | 改 manage_template 后管道启动成功 |
| LOG-ORB-004 | 归档关闭时 Noop Bean 缺失，日志服务启动失败 | Major | 后端 | 改为显式开关条件并加 3 个组件扫描测试，18089 健康且入库成功 |
| LOG-ORB-005 | 数字 @timestamp 导致 ES 文档带时间解析失败标签 | Major | 采集 | 输出改 collectorTimestamp，业务 timestamp 经 ISO8601 转换后新请求无失败标签 |
| LOG-ORB-006 | iOS UI 运行器无法建立 Pseudo Terminal | Major | 代理执行环境权限 | 用户放宽权限后，原 UI 场景与严格全套连续通过；本轮关闭，不改测试断言 |

## 2. 复现步骤、修复依据与复测结果

001：docker pull bitnami/kafka:3.7 返回 registry EOF；经本机代理访问官方 registry 的 manifests/3.7 为 404。官方 Apache 3.9.1 存在 ARM64 manifest。镜像各 blob 经 sha256 校验后导入 Docker，无全局代理变更。

002：IDEA 写宿主机目录，Fluent Bit 原只读 named volume 无文件。叠加 docker-compose.logging-host.yml 并将应用与采集配置到 /tmp/foodmap-orbstack-logs 后，容器 tail 发现两个服务文件。

003：启动日志 `Unknown setting 'template_management' for elasticsearch`，改正确插件参数后重启。JSON message 首 token 提取 eventName，保留旧 log 回退。

004：服务日志 `No qualifying bean of type LogArchiveExportClient`。组件上的 ConditionalOnMissingBean 被扫描顺序影响；Noop 改对应功能开关 false/missing 条件。默认与显式关闭时创建占位 Bean，显式开启时不回退占位。

005：requestId=orb-log-20261109174758 在 ES 与日志库均存在，但 ES tags 含 _timestampparsefailure；Fluent Bit Kafka output 默认追加数字 @timestamp。改 Timestamp_Key collectorTimestamp 后，requestId=orb-clean-20261109174911 无失败标签，数据库事件/状态/路径正确。

006：全套场景 b1-ui-login 中 xcodebuild 退出 65，错误摘要：`Failed to install or launch the test runner`，`Pseudo Terminal Setup Error. ErrorCode: 7 Errno: 1`，`Operation not permitted`。后续“登录成功记录存在”断言也失败。未改变断言或将其改为跳过。修复范围应为宿主机 Xcode/终端权限或在具备权限的终端复跑；本轮不修改 iOS 工程。当时尚未到登录 HTTP 请求阶段，没有可关联的 requestId/traceId。

2026-11-10 权限调整后的复测：仅将代理运行权限从 workspace-write 调整为 danger-full-access，未修改业务代码、场景或断言。原 UI 单场景退出 0，16/16 步骤通过，其中 XCUITest 为 1 passed / 0 failed；随后设置 VERIFY_REQUIRE_FULL=1 的原套件退出 0，6 通过 / 0 失败 / 0 跳过。UI 登录的数据库 SUCCESS 记录与后端日志 200 均通过断言，teardown 清理本轮账号。由对照结果确认之前的代理执行权限限制阻断了测试运行器，现已在当前权限下解除。新证据为 evidence/2026-11-10T09-27-suite-6e5c3f7b-6322-4a33-bf61-244087105750-harness_scenarios.json；历史失败证据与运行记录保留，不覆盖。

### LOG-ORB-007 提交前复测暴露访问日志到达顺序竞争

2026-11-10 提交前全套复测出现 5 通过 / 1 失败 / 0 跳过；失败为日志场景探针 `StopIteration`。ES 中同一个 requestId 的 SQL/application 日志可能先到，原等待条件 `docs and rows` 误以为访问日志已可查询。修复为必须等待 `logType=api-access` 文档和数据库摘要同时存在；不删减字段、位点或隐私断言。历史失败证据保留于 evidence/2026-11-10T09-36-suite-81840d2d-973f-4968-b6e2-44a188977b99-harness_scenarios.json。修复后以完整严格套件重新验证，运行结果只追加到 status/data/runs/。

## 3. 数据与隐私

证据只保留状态、requestId、traceId、Kafka 分区位点和脱敏摘要；不记录密码和 Token。新增场景有密码/Token 泄露断言。既有 IP 原值问题属于 T-046，当前不把全部日志隐私能力标为验收通过。
