# 个人博客系统

> 一个前后端分离的个人博客系统，作为入门级全栈项目，完整实践「需求 → 设计 → 编码 → 测试 → 部署」全流程。

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Spring Boot 3 + MyBatis-Plus + MySQL + Redis + Caffeine + JWT |
| 前端 | Vue 3 + Vite + Vue Router + Pinia + Axios + Element Plus |
| 部署 | Docker Compose + Nginx |

## 项目结构

    ├─ blog-backend/   # Spring Boot 后端（单模块）
    ├─ blog-web/       # Vue 3 + Vite 前端（前台 + 后台路由）
    ├─ sql/            # 数据库脚本（schema.sql / data.sql）
    ├─ nginx/          # Nginx 配置
    ├─ docs/           # 项目文档
    │  └─ 开发计划文档.md
    ├─ docker-compose.yml
    └─ README.md

## 开发计划

详见 [docs/开发计划文档.md](docs/开发计划文档.md)。按瀑布模型分 5 个阶段，编码阶段拆为 M1~M6 里程碑。

参考项目：[WeBlog](https://github.com/weiwosuoai/WeBlog)（后端架构与数据库设计），并补充 Redis 多级缓存与 Lua 限流。

## 环境要求

- JDK 17
- Maven 3.8+
- MySQL 8.0
- Redis 7.x
- Node.js 18+ / npm

## 快速开始（M1：后端骨架）

### 1. 准备数据库

1. 启动本机 MySQL 8，依次执行：
   - sql/schema.sql（建库 + 7 张表）
   - sql/data.sql（初始化分类、标签、站点设置）
2. 修改 blog-backend/src/main/resources/application.yml 中的 spring.datasource.username / password 为你本机的 MySQL 账号密码。

### 2. 启动后端

    cd blog-backend
    mvn spring-boot:run

启动后：
- 服务地址：http://localhost:8080
- 接口文档：http://localhost:8080/doc.html
- 首次启动会自动创建管理员账号：admin / 123456

### 3. 验证登录

打开接口文档 http://localhost:8080/doc.html，在「认证管理 → 登录」里用 admin / 123456 测试，返回 code 为 0 且带 token 即表示成功。

## Git 提交规范

提交信息格式：<type>: <描述>，type 取 feat / fix / docs / refactor / test / chore。

## 许可证

MIT
