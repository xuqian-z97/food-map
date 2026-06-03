---
name: foodmap-doc-sync
description: Use when FoodMap requirements, architecture, milestones, agent rules, visibility rules, service boundaries, frontend pages, backend APIs, or acceptance criteria change and project documents must be synchronized before code changes.
---

# FoodMap 文档同步 Skill

## 使用时机

当用户提出以下变更时使用本 skill：

- 产品范围、MVP、用户流程、可见范围变化。
- iOS 页面、导航、技术栈、SDK、验收标准变化。
- Java 微服务、数据库归属、API、消息事件、基础设施变化。
- 多代理职责、子代理边界、验收标准变化。
- 迭代顺序、GitHub 同步、项目结构变化。

## 必读文档

按需读取：

- `CODEX-product.md`
- `CODEX-front.md`
- `CODEX-after.md`
- `CODEX-gen.md`
- `AGENTS.md`
- `.agents/*.md`

## 工作流程

1. 判断变更影响范围。
2. 先更新受影响文档，再修改代码。
3. 保持文档之间一致：
   - 产品行为写入 `CODEX-product.md`。
   - 前端页面、iOS 架构、SDK 写入 `CODEX-front.md`。
   - 后端服务、数据库、API、基础设施写入 `CODEX-after.md`。
   - 迭代顺序、项目结构、同步规则写入 `CODEX-gen.md`。
   - 代理职责、文件边界、验收标准写入 `AGENTS.md` 和 `.agents/*.md`。
4. 检查关键规则没有丢失。
5. 输出变更摘要和未决事项。

## 必须保留的项目规则

- 后端采用 Java 微服务。
- 每个微服务拥有独立数据库。
- 服务不能直接访问其他服务的数据表。
- 推荐内容可见范围由后端强制校验。
- 只有 `PUBLIC / 全部公开` 推荐进入全站社区统计。
- 菜名必须是纯文字。
- 项目重要迭代需要提交并推送到 GitHub。

## 验收标准

- 所有受影响文档已同步。
- 文档中没有互相冲突的技术栈、服务边界或页面定义。
- 如果新增代码生成规则，`CODEX-gen.md` 或 `AGENTS.md` 已更新。
- 如果新增子代理职责，`.agents/*.md` 已更新。

