# 子代理任务模板

主代理分派子代理任务时，必须尽量使用本模板。

## 1. 任务名称

填写一句清晰的任务名称。

## 2. 任务背景

说明本任务属于哪个迭代、解决什么问题、和当前产品目标的关系。

## 3. 必读文档

列出子代理必须读取的文档：

- `CODEX-product.md`
- `CODEX-front.md`
- `CODEX-after.md`
- `CODEX-gen.md`
- `AGENTS.md`
- `.agents/<agent>.md`
- `skills/<skill>/SKILL.md`
- `harness/*.md`

## 4. 允许修改范围

明确列出允许修改的文件或目录。

示例：

```text
允许修改：
- after/foodmap-auth-service
- after/foodmap-common
- docs/api/auth.md
```

## 5. 禁止修改范围

明确列出禁止修改的文件或目录。

示例：

```text
禁止修改：
- front/
- after/foodmap-store-service
- CODEX-product.md，除非主代理重新授权
```

## 6. 具体交付物

列出必须产出的文件、接口、页面、测试或文档。

## 7. 验收标准

列出本任务的验收标准。

必须包含：

- 文件边界是否遵守。
- 文档是否需要同步。
- 构建或测试是否执行。
- 权限、可见范围、PUBLIC 统计口径是否涉及。

## 8. 汇报格式

子代理完成后必须汇报：

```text
完成内容：
- ...

修改文件：
- ...

验证结果：
- 已执行：...
- 未执行：...，原因：...

风险和后续：
- ...
```

