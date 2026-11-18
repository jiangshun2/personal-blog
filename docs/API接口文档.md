# API 接口文档（M2-M6）

> 基础地址：http://localhost:8080/api
> 统一响应：{ "code": 0, "message": "成功", "data": ... }（code=0 成功）
> 分页结构：{ "records": [], "total": 0, "current": 1, "size": 10 }
> 鉴权：后台接口需请求头 Authorization: Bearer <token>；未登录返回 HTTP 401

## 一、前台公开接口

### 1.1 文章

GET /article/page
  参数：page=1, size=10, categoryId(可选), tagId(可选), keyword(可选)
  data：PageResult<ArticleListVO>
  ArticleListVO：id, title, summary, cover, categoryId, categoryName, tags(String[]), viewCount, isTop, createTime

GET /article/{id}
  data：ArticleDetailVO
  字段：id, title, summary, content, cover, categoryId, categoryName, tags(String[]),
        viewCount, createTime, prevId, prevTitle, nextId, nextTitle
  说明：每次调用浏览量 +1（Redis 计数）

GET /article/archive
  data：List<ArchiveVO>
  ArchiveVO：year(Integer), articles(List<ArchiveItem>)
  ArchiveItem：id, title, createTime
  说明：按年份倒序返回

### 1.2 分类

GET /category/list
  data：List<CategoryVO>
  CategoryVO：id, name, description, sort, articleCount

### 1.3 标签

GET /tag/list
  data：List<TagVO>
  TagVO：id, name, articleCount

### 1.4 评论

GET /comment/page
  参数：articleId(必填), page=1, size=10
  data：PageResult<CommentVO>
  CommentVO：id, articleId, userId, parentId, content, nickname, email, status, createTime, children(List<CommentVO>)

POST /comment
  body：{ "articleId": 1, "content": "评论内容", "nickname": "游客昵称(可选)", "email": "邮箱(可选)", "parentId": null }
  data：CommentVO
  说明：限流 每 IP 每分钟 10 次；超限 code=4290

### 1.5 站点配置

GET /site/config
  data：Map<String,String>
  包含键：site_name, site_description, site_avatar, github, email

## 二、后台接口（需登录）

### 2.1 文章管理

GET /admin/article/page
  参数：page=1, size=10, keyword(可选), status(可选: 0草稿/1发布)
  data：PageResult<ArticleAdminVO>
  ArticleAdminVO：id, title, summary, cover, categoryId, categoryName, tags(String[]),
                  status, viewCount, isTop, createTime, updateTime

GET /admin/article/{id}
  data：ArticleAdminVO（额外包含 content 与 tagIds(Long[])）

POST /admin/article
  body：{ "title":"", "summary":"", "content":"", "cover":"", "categoryId":1,
          "tagIds":[1,2], "status":1, "isTop":false }
  data：无

PUT /admin/article/{id}
  body：同 POST
  data：无

DELETE /admin/article/{id}
  data：无

PUT /admin/article/{id}/top
  body：{ "isTop": true }
  data：无

### 2.2 分类管理

GET /admin/category/list
  data：List<CategoryVO>

POST /admin/category
  body：{ "name":"", "description":"", "sort":0 }
  data：无

PUT /admin/category/{id}
  body：同 POST
  data：无

DELETE /admin/category/{id}
  data：无
  说明：分类下仍有文章时禁止删除（code=5000 或 4000，message 提示）

### 2.3 标签管理

GET /admin/tag/list
  data：List<TagVO>

POST /admin/tag
  body：{ "name":"" }
  data：无

PUT /admin/tag/{id}
  body：{ "name":"" }
  data：无

DELETE /admin/tag/{id}
  data：无
  说明：删除标签同时清理文章-标签关联

### 2.4 评论管理

GET /admin/comment/page
  参数：page=1, size=10, articleId(可选), status(可选)
  data：PageResult<CommentVO>

PUT /admin/comment/{id}/status
  body：{ "status": 1 }
  data：无

DELETE /admin/comment/{id}
  data：无

### 2.5 站点设置

PUT /admin/site/config
  body：{ "site_name":"", "site_description":"", "site_avatar":"", "github":"", "email":"" }
  data：无

### 2.6 仪表盘

GET /admin/dashboard/stats
  data：{
    "articleCount": 0, "commentCount": 0, "categoryCount": 0, "tagCount": 0,
    "totalViewCount": 0,
    "recentArticles": [ { "id":1, "title":"", "viewCount":0, "createTime":"" } ]
  }

## 三、缓存约定（M5）

| 缓存键 | 内容 | TTL |
|--------|------|-----|
| blog:article:{id} | 文章详情 | 30 分钟 |
| blog:article:page:{page}:{size}:{categoryId}:{tagId}:{keyword} | 文章分页 | 5 分钟 |
| blog:category:list | 分类列表 | 30 分钟 |
| blog:tag:list | 标签列表 | 30 分钟 |
| blog:site:config | 站点配置 | 1 小时 |
| blog:view:{articleId} | 浏览量增量 | 不设 |

写操作（文章/分类/标签/站点增删改）后需精准删除对应缓存；文章列表类缓存按前缀 blog:article:page: 清理。

## 四、错误码

| code | 含义 |
|------|------|
| 0 | 成功 |
| 4000 | 参数错误 |
| 4001 | 用户名或密码错误 |
| 4010 | 未登录 / 登录过期（HTTP 401） |
| 4030 | 无权限 |
| 4040 | 资源不存在 |
| 4290 | 请求过于频繁（限流） |
| 5000 | 系统异常 |
