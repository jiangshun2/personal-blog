/**
 * 前台评论接口（公开）
 */
import request from '@/utils/request'

/**
 * 文章评论分页
 * @param {Object} params { articleId, page, size }
 */
export function getCommentPage(params) {
  return request.get('/comment/page', { params })
}

/**
 * 发表评论 / 回复
 * @param {Object} data { articleId, content, nickname?, email?, parentId? }
 */
export function createComment(data) {
  return request.post('/comment', data)
}
