/**
 * 站点配置接口（公开）
 */
import request from '@/utils/request'

/** 站点配置：Map<String,String>，包含 site_name / site_description / site_avatar / github / email */
export function getSiteConfig() {
  return request.get('/site/config')
}
