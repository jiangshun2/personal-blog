// ============================================================
// 个人博客系统 - 功能测试脚本（阶段四，Node.js 版）
// 前置：后端已启动（8080）
// 用法：node scripts/functional-test.mjs
// 说明：用 Node 原生 fetch，天然 UTF-8，避免 PowerShell 中文编码问题
// ============================================================
const BASE = 'http://localhost:8080/api'

let pass = 0
let fail = 0
function assert(name, ok, detail) {
  if (ok) { pass++; console.log('[PASS] ' + name) }
  else { fail++; console.log('[FAIL] ' + name + ' -> ' + (detail === undefined ? '' : detail)) }
}

async function req(method, path, body, token) {
  const headers = {}
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  if (token) headers['Authorization'] = 'Bearer ' + token
  const res = await fetch(BASE + path, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })
  const text = await res.text()
  let json = null
  try { json = JSON.parse(text) } catch (e) { json = { raw: text } }
  return { status: res.status, json }
}

function enc(s) { return encodeURIComponent(s) }

async function main() {
  const stamp = String(Date.now()).slice(-6)
  const createdArticleIds = []

  // ---------- 登录 ----------
  let r = await req('POST', '/auth/login', { username: 'admin', password: '123456' })
  const token = r.json && r.json.data && r.json.data.token
  assert('登录', !!token, 'code=' + (r.json && r.json.code))
  if (!token) { console.log('登录失败，终止'); process.exit(1) }

  // ---------- 清理历史测试文章 ----------
  r = await req('GET', '/admin/article/page?page=1&size=200', undefined, token)
  for (const rec of (r.json.data.records || [])) {
    const t = String(rec.title || '')
    if (t.startsWith('?') || t.includes('测试') || t.startsWith('已更新标题') || t.startsWith('DebugTest')) {
      try { await req('DELETE', '/admin/article/' + rec.id, undefined, token) } catch (e) {}
    }
  }

  // ---------- 分类 / 标签 ----------
  const cats = await req('GET', '/category/list')
  const catId = cats.json.data[0].id
  const tagsRes = await req('GET', '/tag/list')
  const tagId1 = tagsRes.json.data[0].id
  const tagId2 = tagsRes.json.data[1].id
  assert('取分类/标签', catId > 0 && tagId1 > 0, 'cat=' + catId)

  // ---------- 新增已发布文章（中文） ----------
  const title1 = '自动化测试文章-' + stamp
  r = await req('POST', '/admin/article', {
    title: title1, summary: '测试摘要', content: '# 测试标题 这是正文', cover: '',
    categoryId: catId, tagIds: [tagId1, tagId2], status: 1, isTop: false,
  }, token)
  assert('新增文章(中文标题)', r.json.code === 0, 'code=' + r.json.code)

  r = await req('GET', '/admin/article/page?page=1&size=5&keyword=' + enc(title1), undefined, token)
  const artId = (r.json.data.records[0] || {}).id
  createdArticleIds.push(artId)
  assert('后台可查到新文章', !!artId, 'id=' + artId)
  if (artId) {
    const foundTitle = r.json.data.records[0].title
    assert('中文标题正确保存', foundTitle === title1, 'title=' + foundTitle)
  }

  // ---------- 详情 + 标签 + 浏览量 ----------
  const d1 = await req('GET', '/article/' + artId)
  const d2 = await req('GET', '/article/' + artId)
  assert('详情含2个标签', (d1.json.data.tags || []).length === 2, 'tags=' + (d1.json.data.tags || []).length)
  assert('浏览量递增', d2.json.data.viewCount > d1.json.data.viewCount,
    'v1=' + d1.json.data.viewCount + ' v2=' + d2.json.data.viewCount)

  // ---------- 前台列表可见 ----------
  const fp = await req('GET', '/article/page?page=1&size=10&keyword=' + enc(title1))
  assert('前台列表可见已发布文章', (fp.json.data.records || []).length >= 1, 'total=' + fp.json.data.total)

  // ---------- 修改文章 -> 缓存失效 ----------
  const title2 = '已更新标题-' + stamp
  await req('PUT', '/admin/article/' + artId, {
    title: title2, summary: '更新摘要', content: '# 更新后正文', cover: '',
    categoryId: catId, tagIds: [tagId1], status: 1, isTop: false,
  }, token)
  const d3 = await req('GET', '/article/' + artId)
  assert('修改后缓存失效', d3.json.data.title === title2, 'title=' + d3.json.data.title)

  // ---------- 草稿隔离 ----------
  const draftTitle = '草稿测试-' + stamp
  await req('POST', '/admin/article', {
    title: draftTitle, summary: '草稿', content: '草稿正文', cover: '',
    categoryId: catId, tagIds: [], status: 0, isTop: false,
  }, token)
  const fp2 = await req('GET', '/article/page?page=1&size=10&keyword=' + enc(draftTitle))
  assert('草稿不出现在前台', fp2.json.data.total === 0, 'front total=' + fp2.json.data.total)
  const ap2 = await req('GET', '/admin/article/page?page=1&size=10&keyword=' + enc(draftTitle), undefined, token)
  assert('草稿出现在后台', ap2.json.data.total >= 1, 'admin total=' + ap2.json.data.total)
  if ((ap2.json.data.records || []).length > 0) createdArticleIds.push(ap2.json.data.records[0].id)

  // ---------- 评论 ----------
  const cr = await req('POST', '/comment', { articleId: artId, content: '自动化测试评论', nickname: 'tester', email: 't@test.com' })
  assert('发表评论', cr.json.code === 0, 'code=' + cr.json.code)
  const cp = await req('GET', '/comment/page?articleId=' + artId + '&page=1&size=10')
  const commentId = (cp.json.data.records[0] || {}).id
  assert('评论出现在列表', (cp.json.data.records || []).length >= 1, 'count=' + (cp.json.data.records || []).length)

  // ---------- 分类删除保护 ----------
  const del = await req('DELETE', '/admin/category/' + catId, undefined, token)
  assert('有文章的分类禁止删除', del.json.code !== 0, 'code=' + del.json.code)

  // ---------- 评论状态切换 ----------
  if (commentId) {
    const st = await req('PUT', '/admin/comment/' + commentId + '/status', { status: 0 }, token)
    assert('评论隐藏操作', st.json.code === 0, 'code=' + st.json.code)
  }

  // ---------- 登录限流（最后） ----------
  let limited = false
  for (let i = 0; i < 8; i++) {
    const x = await req('POST', '/auth/login', { username: 'admin', password: 'wrong-pwd' })
    if ((x.json && x.json.code === 4290) || x.status === 429) { limited = true; break }
  }
  assert('登录限流生效(4290/429)', limited, '连续错误登录应被限流')

  // ---------- 清理 ----------
  for (const id of createdArticleIds) {
    try { await req('DELETE', '/admin/article/' + id, undefined, token) } catch (e) {}
  }

  console.log('')
  console.log('========== 功能测试汇总：PASS=' + pass + '  FAIL=' + fail + ' ==========')
  process.exit(fail > 0 ? 1 : 0)
}

main().catch((e) => { console.error('测试异常:', e); process.exit(1) })
