/**
 * 后台管理接口（全部需要登录，请求头带 JWT）
 * 与 docs/API接口文档.md 第二章一一对应
 */
import request from '@/utils/request'

/* ==================== 2.1 文章管理 ==================== */

/** 后台文章分页：{ page, size, keyword?, status? } */
export function getAdminArticlePage(params) {
  return request.get('/admin/article/page', { params })
}

/** 后台文章详情（额外包含 content 与 tagIds） */
export function getAdminArticle(id) {
  return request.get('/admin/article/' + id)
}

/** 新建文章 */
export function createArticle(data) {
  return request.post('/admin/article', data)
}

/** 更新文章 */
export function updateArticle(id, data) {
  return request.put('/admin/article/' + id, data)
}

/** 删除文章 */
export function deleteArticle(id) {
  return request.delete('/admin/article/' + id)
}

/** 切换置顶状态 */
export function updateArticleTop(id, isTop) {
  return request.put('/admin/article/' + id + '/top', { isTop })
}

/* ==================== 2.2 分类管理 ==================== */

/** 后台分类列表 */
export function getAdminCategoryList() {
  return request.get('/admin/category/list')
}

/** 新增分类：{ name, description, sort } */
export function createCategory(data) {
  return request.post('/admin/category', data)
}

/** 更新分类 */
export function updateCategory(id, data) {
  return request.put('/admin/category/' + id, data)
}

/** 删除分类（分类下有文章时后端会拒绝） */
export function deleteCategory(id) {
  return request.delete('/admin/category/' + id)
}

/* ==================== 2.3 标签管理 ==================== */

/** 后台标签列表 */
export function getAdminTagList() {
  return request.get('/admin/tag/list')
}

/** 新增标签：{ name } */
export function createTag(data) {
  return request.post('/admin/tag', data)
}

/** 更新标签 */
export function updateTag(id, data) {
  return request.put('/admin/tag/' + id, data)
}

/** 删除标签（同时清理文章-标签关联） */
export function deleteTag(id) {
  return request.delete('/admin/tag/' + id)
}

/* ==================== 2.4 评论管理 ==================== */

/** 后台评论分页：{ page, size, articleId?, status? } */
export function getAdminCommentPage(params) {
  return request.get('/admin/comment/page', { params })
}

/** 更新评论状态：status 1 显示 / 0 隐藏 */
export function updateCommentStatus(id, status) {
  return request.put('/admin/comment/' + id + '/status', { status })
}

/** 删除评论 */
export function deleteComment(id) {
  return request.delete('/admin/comment/' + id)
}

/* ==================== 2.5 站点设置 ==================== */

/** 更新站点配置（需要登录） */
export function updateSiteConfig(data) {
  return request.put('/admin/site/config', data)
}

/* ==================== 2.6 仪表盘 ==================== */

/** 统计数据：文章数 / 评论数 / 分类数 / 标签数 / 总浏览量 + 最近文章 */
export function getDashboardStats() {
  return request.get('/admin/dashboard/stats')
}
