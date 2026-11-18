/**
 * 前台文章接口（公开）
 */
import request from '@/utils/request'

/**
 * 分页查询文章
 * @param {Object} params { page, size, categoryId?, tagId?, keyword? }
 * @returns {Promise<{records: Array, total: number, current: number, size: number}>}
 */
export function getArticlePage(params) {
  return request.get('/article/page', { params })
}

/**
 * 文章详情（每次调用浏览量 +1）
 * @param {number|string} id 文章 ID
 */
export function getArticleDetail(id) {
  return request.get('/article/' + id)
}

/** 文章归档（按年份倒序） */
export function getArchive() {
  return request.get('/article/archive')
}
