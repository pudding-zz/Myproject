# 2026-08-17：后端接入 springdoc-openapi（Swagger UI）

## 背景

需要可视化调试 REST 接口，Spring Boot 3.x 使用 springdoc，而非 springfox。

## 修改内容

| 位置 | 变更 |
|------|------|
| `backend/pom.xml` | 增加 `springdoc-openapi-starter-webmvc-ui` 2.6.0；补充 UTF-8 编码属性 |
| `backend/.../config/OpenApiConfig.java` | 新增，文档标题「望月 API」 |
| `backend/src/main/resources/application.yml` | 固定 `springdoc` 的 api-docs / swagger-ui 路径 |

## 访问地址

- Swagger UI：`http://localhost:8080/api/swagger-ui.html`
- OpenAPI JSON：`http://localhost:8080/api/v3/api-docs`

（均在 `context-path=/api` 之下。）

## 影响范围

仅文档与依赖；业务接口逻辑不变。无 Spring Security，UI 默认可访问。
