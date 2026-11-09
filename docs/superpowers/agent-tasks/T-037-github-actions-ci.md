# T-037 GitHub Actions CI 流水线

- 功能点：F-021｜优先级：P1｜波次：W3｜组：E（第 3 步，可与 T-035 并行）｜预估：1d｜依赖：T-034 已合并

## 1. 任务背景

仓库没有 `.github`，没有任何 CI；质量门禁完全依赖开发者自觉。

## 2. 必读文档

`AGENTS.md` 6.3.1、`harness/README.md`、`harness/scripts/run-all.sh`、`.agents/qa-agent.md`。

## 3. 允许修改范围

```text
.github/workflows/backend.yml（新建）
```

## 4. 禁止修改范围

```text
after/、front/、deploy/、harness/、status/**
```

## 5. 具体交付物

1. PR 与 push 到 main 触发；Java 21（`actions/setup-java`，`cache: maven`）。
2. 步骤：`cd after && mvn -B verify`、`./harness/scripts/run-all.sh`、`verify.sh --dry-run`、镜像构建（不推送）。
3. 依赖漏洞扫描（Trivy 或 OWASP dependency-check），高危使流水线失败。
4. Action 版本固定到明确版本；不得写入任何密钥，测试所需变量使用 CI 专用假值。
5. 需要真实服务的 verify 场景不在 CI 中强制执行，必须在文件注释写明原因。

## 6. 验收标准

- [ ] workflow YAML 语法校验通过（如 `actionlint`，无则写明未执行）。
- [ ] 失败时阻断合并的分支保护说明写入汇报。
- [ ] 构建缓存配置正确。
- [ ] 无明文密钥。

## 7. 汇报格式

同 `harness/agent-task-template.md` 第 8 节。
