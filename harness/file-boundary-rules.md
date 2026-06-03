# 文件边界规则

## 1. 目的

多代理并行开发时，每个子代理必须有明确文件边界，避免覆盖他人改动。

## 2. 通用规则

- 子代理只能修改主代理明确授权的文件或目录。
- 子代理不得回滚他人改动。
- 子代理发现需要跨范围修改时，必须停止并向主代理说明。
- 主代理负责整合跨范围改动。

## 3. 默认边界

### 前端子代理

默认允许：

```text
ios/FoodMapApp
```

默认禁止：

```text
backend
CODEX-after.md
```

### 后端子代理

默认允许主代理指定的单个后端服务，例如：

```text
backend/foodmap-auth-service
backend/foodmap-common
```

默认禁止：

```text
ios
其他未授权 backend 服务
```

### API 契约子代理

默认允许：

```text
docs/api
backend/**/dto
ios/FoodMapApp/**/Models
```

实际任务必须由主代理进一步收窄范围。

### QA 子代理

默认允许：

```text
backend/**/src/test
ios/FoodMapApp/**/Tests
docs/testing
harness
```

修改生产代码必须得到主代理授权。

### 文档子代理

默认允许：

```text
CODEX-product.md
CODEX-front.md
CODEX-after.md
CODEX-gen.md
AGENTS.md
.agents/*.md
skills/*/SKILL.md
harness
docs
```

默认禁止：

```text
backend
ios
```

## 4. 冲突处理

如果两个子代理需要修改同一文件：

1. 主代理重新拆分任务。
2. 或指定一个子代理负责编辑，另一个只提供建议。
3. 或由主代理本地完成该文件修改。

