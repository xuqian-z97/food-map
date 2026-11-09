# T-044 全局异常映射补全

- 功能点：F-029｜优先级：P2｜波次：W4｜组：F（独立）｜预估：0.5d｜依赖：无

## 1. 任务背景

`FoodMapGlobalExceptionHandler` 未覆盖 `MissingRequestHeaderException`、`HttpMediaTypeNotSupportedException`、`MissingPathVariableException`、`HandlerMethodValidationException`。`UserController.me` 缺 `X-FoodMap-User-Id` 头时落到兜底返回 500。

## 2. 必读文档

`AGENTS.md` 8.5（统一响应与 401/403 区分）、`harness/api-contract-checklist.md`。

## 3. 允许修改范围

```text
after/foodmap-common/src/main/java/com/foodmap/common/web/FoodMapGlobalExceptionHandler.java
after/foodmap-common/src/test/java/com/foodmap/common/web/**
```

## 4. 禁止修改范围

```text
其他 after 模块、front/、deploy/、status/**
```

## 5. 具体交付物

1. 缺身份头返回 401（`UNAUTHORIZED`）；其他缺失头返回 400。
2. 不支持的 Content-Type 返回 415。
3. 缺路径变量与方法参数校验异常返回 4xx。
4. 响应不含异常类名、堆栈、SQL、内部地址。

## 6. 验收标准

- [ ] MockMvc 覆盖每个新增分支。
- [ ] 现有 `FoodMapGlobalExceptionHandlerTest` 不回归。
- [ ] 4xx 不再被兜底归为 500。
- [ ] `mvn -q -B -pl foodmap-common test` 通过。

## 7. 汇报格式

同 `harness/agent-task-template.md` 第 8 节。
