# 个人博客系统

> 一个前后端分离的个人博客系统（入门级全栈项目），完整实现「需求 → 设计 → 编码 → 测试」流程，并保留部署上线所需配置。

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Spring Boot 3.2.5 + MyBatis-Plus 3.5.7 + MySQL 8 + Redis 7 + Caffeine + Spring Security + JWT + Knife4j |
| 前端 | Vue 3 + Vite 5 + Vue Router 4 + Pinia + Axios + Element Plus + markdown-it + highlight.js |
| 部署 | Docker Compose + Nginx（阶段五） |

## 项目结构

    ├─ blog-backend/    # Spring Boot 后端（单模块）
    │  └─ src/main/java/com/blog/
    │     ├─ config/    # 配置（MP 分页、Redis、Caffeine、Security、CORS、Knife4j、数据初始化）
    │     ├─ controller/# 接口层（公开接口 + admin 后台接口）
    │     ├─ service/   # 业务层（接口 + impl）
    │     ├─ mapper/    # MyBatis-Plus Mapper
    │     ├─ entity/    # 实体
    │     ├─ dto/ vo/   # 入参 / 出参
    │     ├─ cache/     # 多级缓存、Lua 限流、浏览量
    │     ├─ security/  # JWT 工具与过滤器
    │     └─ common/    # 统一返回、异常、工具
    ├─ blog-web/        # Vue3 + Vite 前端（前台 + 后台路由）
    │  └─ src/
    │     ├─ views/front/   # 前台 6 个页面
    │     ├─ views/admin/   # 后台 8 个页面
    │     ├─ router/ stores/ api/ utils/ layouts/ components/ assets/
    ├─ sql/             # schema.sql（建库建表）/ data.sql（初始化数据）
    ├─ scripts/         # 测试脚本（smoke-test.ps1 / functional-test.mjs）
    ├─ docs/            # 开发计划、API 契约、测试报告
    ├─ nginx/           # Nginx 配置（阶段五）
    └─ docker-compose.yml（阶段五）

## 开发计划

详见 [docs/开发计划文档.md](docs/开发计划文档.md)，接口契约见 [docs/API接口文档.md](docs/API接口文档.md)，测试报告见 [docs/测试用例与报告.md](docs/测试用例与报告.md)。

## 快速开始

### 1. 准备数据库

1. 启动本机 MySQL 8，依次执行：
   - sql/schema.sql（建库 + 7 张表）
   - sql/data.sql（初始化分类、标签、站点设置）
2. 创建 blog-backend/src/main/resources/application-local.yml，写入本机数据库与 Redis 配置：

       spring:
         datasource:
           url: jdbc:mysql://localhost:3306/blog?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
           username: root
           password: "你的MySQL密码"
         data:
           redis:
             host: localhost
             port: 6379
             password: "你的Redis密码（没有可留空）"
             database: 0

   该文件已被 .gitignore 忽略，不会上传到 GitHub。

### 2. 启动后端

    cd blog-backend
    mvn spring-boot:run

- 服务地址：http://localhost:8080
- 接口文档：http://localhost:8080/doc.html
- 首次启动自动创建管理员：admin / 123456

### 3. 启动前端

    cd blog-web
    npm install          # 慢时加 --registry https://registry.npmmirror.com
    npm run dev

- 前台：http://localhost:5173/
- 后台：http://localhost:5173/admin/login

### 4. 运行测试

    # 接口冒烟测试（需后端已启动）
    powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1

    # 功能测试（Node 原生 UTF-8，推荐）
    node scripts/functional-test.mjs

## Git 提交规范

提交信息格式：<type>: <描述>，type 取 feat / fix / docs / refactor / test / chore。

## 许可证

MIT
