# blog-web 前端

个人博客系统的前端工程，使用 **Vue 3 + Vite 5** 从零搭建，前台（访客浏览）与后台（博主管理）在同一个 SPA 内通过 Vue Router 区分。

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.x | Composition API，页面统一使用 `<script setup>` |
| Vite | 5.x | 开发服务器与构建 |
| Vue Router | 4.x | 前台路由 + 后台路由（含登录守卫） |
| Pinia | 2.x | 用户登录态、站点配置等全局状态 |
| Axios | 1.x | HTTP 请求，拦截器统一注入 token / 处理错误 |
| Element Plus | 2.x | 后台管理界面 |
| markdown-it | 14.x | 前台文章正文渲染 |
| highlight.js | 11.x | Markdown 代码块高亮 |

## 目录结构

    blog-web/
    ├─ index.html
    ├─ package.json
    ├─ vite.config.js              # 开发代理 /api -> http://localhost:8080
    ├─ README.md
    └─ src/
       ├─ main.js                  # 入口：Pinia + Router + Element Plus
       ├─ App.vue
       ├─ assets/styles/
       │  ├─ global.css            # 设计变量、重置、工具类
       │  ├─ front.css             # 前台自定义响应式样式（浅色博客风格）
       │  └─ admin.css             # 后台布局与页面样式
       ├─ api/                     # 按模块封装的接口（与接口文档一一对应）
       │  ├─ auth.js               # 登录 / 当前用户
       │  ├─ article.js            # 前台文章分页、详情、归档
       │  ├─ category.js           # 前台分类列表
       │  ├─ tag.js                # 前台标签列表
       │  ├─ comment.js            # 前台评论分页、发表评论
       │  ├─ site.js               # 站点配置
       │  └─ admin.js              # 后台文章 / 分类 / 标签 / 评论 / 站点 / 仪表盘
       ├─ components/
       │  ├─ ArticleCard.vue       # 文章卡片
       │  ├─ Pagination.vue        # 前台自定义分页
       │  └─ CommentItem.vue       # 评论（支持二级回复，递归渲染）
       ├─ layouts/
       │  ├─ FrontLayout.vue       # 前台布局：顶部导航 + 内容 + 页脚
       │  └─ AdminLayout.vue       # 后台布局：侧边菜单 + 顶栏 + 内容区
       ├─ router/index.js          # 前后台路由 + 登录守卫
       ├─ stores/
       │  ├─ user.js               # token / userInfo / login / logout
       │  └─ site.js               # 站点配置缓存
       ├─ utils/
       │  ├─ request.js            # axios 封装（统一响应、401 跳转）
       │  ├─ markdown.js           # markdown-it + highlight.js 渲染
       │  └─ format.js             # 日期 / 数字格式化
       └─ views/
          ├─ front/                # 前台页面
          │  ├─ Home.vue
          │  ├─ ArticleDetail.vue
          │  ├─ Category.vue
          │  ├─ Tag.vue
          │  ├─ Archive.vue
          │  └─ About.vue
          └─ admin/                # 后台页面
             ├─ Login.vue
             ├─ Dashboard.vue
             ├─ ArticleList.vue
             ├─ ArticleEdit.vue
             ├─ CategoryManage.vue
             ├─ TagManage.vue
             ├─ CommentManage.vue
             └─ SiteConfig.vue

## 快速开始

### 1. 安装依赖

    cd blog-web
    npm install

网络较慢时使用国内镜像：

    npm install --registry https://registry.npmmirror.com

### 2. 启动后端（另开一个终端）

前端所有接口都走 /api，开发时由 Vite 代理到 http://localhost:8080，因此需要先启动 Spring Boot 后端：

    cd blog-backend
    mvn spring-boot:run

### 3. 启动前端开发服务器

    npm run dev

浏览器访问 http://localhost:5173 ：

- 前台首页：http://localhost:5173/
- 后台登录：http://localhost:5173/admin/login
- 默认管理员账号：admin / 123456（后端首次启动时自动创建）

### 4. 构建生产包

    npm run build

构建产物输出到 blog-web/dist/，可交由 Nginx 托管；/api/** 反向代理到后端即可。

本地预览构建产物：

    npm run preview

## 页面与路由

### 前台（无需登录）

| 路由 | 页面 | 说明 |
|------|------|------|
| / | Home.vue | 文章卡片列表、分页、分类 / 标签 / 关键词筛选、置顶标识 |
| /article/:id | ArticleDetail.vue | Markdown 正文渲染 + 代码高亮、浏览量、上一篇 / 下一篇、评论与回复 |
| /category | Category.vue | 分类列表，点击分类筛选文章 |
| /tag | Tag.vue | 标签云（按文章数调整字号），点击标签筛选文章 |
| /archive | Archive.vue | 按年份归档的文章时间线 |
| /about | About.vue | 站点信息、联系方式、技术栈 |

### 后台（需登录）

| 路由 | 页面 | 说明 |
|------|------|------|
| /admin/login | Login.vue | 登录，成功后保存 token 并跳转仪表盘 |
| /admin/dashboard | Dashboard.vue | 统计卡片（文章 / 评论 / 分类 / 标签 / 总浏览量）+ 最近文章 |
| /admin/article | ArticleList.vue | 文章分页表格、搜索、状态筛选、置顶切换、编辑 / 删除 |
| /admin/article/edit | ArticleEdit.vue | 新建文章：Markdown 编辑 + 实时预览 |
| /admin/article/edit/:id | ArticleEdit.vue | 编辑文章：加载详情并回填 |
| /admin/category | CategoryManage.vue | 分类增删改查 |
| /admin/tag | TagManage.vue | 标签增删改查 |
| /admin/comment | CommentManage.vue | 评论分页、显示 / 隐藏、删除 |
| /admin/site | SiteConfig.vue | 站点名称、描述、头像、GitHub、邮箱配置 |

后台路由统一设置 meta.requiresAuth，全局前置守卫在无 token 时跳转 /admin/login，并记录 redirect 参数，登录后回到原页面。

## 接口与状态约定

- 统一响应：{ code, message, data }，code === 0 表示成功；request.js 成功时直接返回 data。
- code !== 0 时用 ElMessage 提示并 reject；HTTP 401 时清除 localStorage 中的 token 并跳转 /admin/login。
- 登录态：token 与 userInfo 分别存放在 localStorage 的 token、userInfo 键中；请求头自动携带 Authorization: Bearer <token>。
- 分页结构：{ records, total, current, size }。
- 接口契约详见 docs/API接口文档.md。

## 说明

- 前台未引入额外 UI 框架，样式全部写在 src/assets/styles/front.css，浅色博客风格并适配手机 / 平板。
- 后台使用 Element Plus，布局与页面样式在 src/assets/styles/admin.css。
- 前端使用 JavaScript（非 TypeScript），页面统一采用 <script setup> 写法。
