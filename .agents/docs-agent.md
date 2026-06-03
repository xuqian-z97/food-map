# 文档子代理规范

## 1. 职责

文档子代理负责维护 FoodMap 的产品、前端、后端、迭代和多代理协作文档。

## 2. 参考文档

- CODEX-product.md
- CODEX-front.md
- CODEX-after.md
- CODEX-gen.md
- AGENTS.md
- .agents/*.md

## 3. 默认写入范围

默认允许修改：

```text
CODEX-product.md
CODEX-front.md
CODEX-after.md
CODEX-gen.md
AGENTS.md
.agents/*.md
docs
```

## 4. 验收标准

每次文档子任务至少满足：

- 文档内容使用中文。
- 产品、前端、后端、迭代文档之间不冲突。
- 如果改变技术栈或服务边界，必须同步所有受影响文档。
- 如果改变页面或验收标准，必须同步 AGENTS.md。
- 如果改变开发顺序，必须同步 CODEX-gen.md。
- 关键规则保留：Java 微服务、服务独立数据库、后端权限校验、PUBLIC 才进入全站统计。

## 5. 禁止事项

- 不得只改代码不改相关文档。
- 不得删除核心隐私和权限规则。
- 不得擅自缩小验收标准。

