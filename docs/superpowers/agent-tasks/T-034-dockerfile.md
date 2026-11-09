# T-034 各服务多阶段 Dockerfile

- 功能点：F-021｜优先级：P0｜波次：W3｜组：E（第 1 步）｜预估：1.5d｜依赖：T-025 已合并

## 1. 任务背景

全仓库没有 Dockerfile，`foodmap/foodmap-gateway-service:latest` 无法从仓库构建。密钥注入（T-025）合并后才能打出可运行镜像。环境：Java 21、Maven 3.9.7、Docker 27.5.1，父 pom 为 Spring Boot 3.3.7。

## 2. 必读文档

`AGENTS.md`、`CODEX-after.md`（部署约束）、`deploy/README.md`、`deploy/architecture.md`、`.agents/backend-agent.md`。

## 3. 允许修改范围

```text
after/Dockerfile（新建，通过 --build-arg MODULE=foodmap-xxx-service 复用）
after/.dockerignore（新建）
```

## 4. 禁止修改范围

```text
after/**/src、after/**/pom.xml、front/、status/**、deploy/（compose 属 T-035）
```

## 5. 具体交付物

1. 多阶段构建：`maven:3.9-eclipse-temurin-21` 构建，`eclipse-temurin:21-jre-alpine` 运行。
2. 非 root 用户运行；`JAVA_OPTS` 可注入；暴露 `SERVER_PORT`。
3. 镜像内不含源码与 Maven 缓存；构建利用依赖层缓存。
4. 支持 11 个模块（common 不单独出镜像）。
5. `.dockerignore` 排除 `target/`、`.git`、`*.md`。

## 6. 验收标准

- [ ] 10 个服务镜像均可构建成功（有 Docker 时执行）。
- [ ] 容器内 `id -u` 非 0。
- [ ] `docker run` 后 `/actuator/health` 可达（连测试依赖或使用 local 配置）。
- [ ] 无 Docker 环境时汇报写明「未执行 + 原因」。
- [ ] 文件边界已遵守；不涉及权限与可见范围。

## 7. 汇报格式

同 `harness/agent-task-template.md` 第 8 节：完成内容、修改文件、验证结果（已执行/未执行+原因）、风险和后续。
