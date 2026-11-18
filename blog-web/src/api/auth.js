/**
 * 认证接口（M1 已实现）
 * POST /api/auth/login  登录
 * GET  /api/auth/me     当前登录用户信息
 */
import request from '@/utils/request'

/** 登录：参数 { username, password }，返回 { token, userInfo } */
export function login(data) {
  return request.post('/auth/login', data)
}

/** 获取当前登录用户信息 */
export function getMe() {
  return request.get('/auth/me')
}
