/**
 * 日期 / 数字格式化工具
 */

function pad(value) {
  return value < 10 ? '0' + value : String(value)
}

/**
 * 把后端返回的时间值转成 Date 对象
 * 兼容：ISO 字符串、'YYYY-MM-DD HH:mm:ss'、时间戳
 */
export function parseDate(value) {
  if (!value && value !== 0) return null
  if (value instanceof Date) return value

  if (typeof value === 'number') {
    return new Date(value)
  }

  const text = String(value).trim()
  if (!text) return null

  // 'YYYY-MM-DD HH:mm:ss' 在部分浏览器解析不稳定，统一换成 ISO 的 T 分隔
  const normalized = text.indexOf('T') > -1 ? text : text.replace(' ', 'T')
  const date = new Date(normalized)
  return isNaN(date.getTime()) ? null : date
}

/** 格式化为 YYYY-MM-DD */
export function formatDate(value) {
  const date = parseDate(value)
  if (!date) return ''
  return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate())
}

/** 格式化为 YYYY-MM-DD HH:mm */
export function formatDateTime(value) {
  const date = parseDate(value)
  if (!date) return ''
  return (
    formatDate(date) +
    ' ' +
    pad(date.getHours()) +
    ':' +
    pad(date.getMinutes())
  )
}

/** 数字千分位：12345 -> 12,345 */
export function formatNumber(value) {
  const num = Number(value)
  if (isNaN(num)) return '0'
  return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

/** 取昵称首字符作为头像占位 */
export function avatarText(name) {
  const text = (name || '').trim()
  return text ? text.charAt(0).toUpperCase() : '匿'
}
