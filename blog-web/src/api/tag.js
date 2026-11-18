/**
 * 前台标签接口（公开）
 */
import request from '@/utils/request'

/** 标签列表：id, name, articleCount */
export function getTagList() {
  return request.get('/tag/list')
}
