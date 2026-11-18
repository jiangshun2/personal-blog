/**
 * 前台分类接口（公开）
 */
import request from '@/utils/request'

/** 分类列表：id, name, description, sort, articleCount */
export function getCategoryList() {
  return request.get('/category/list')
}
