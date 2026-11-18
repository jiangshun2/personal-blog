# ============================================================
# 个人博客系统 - 接口冒烟测试脚本（阶段四 / M6 联调用）
# 用法：
#   1. 先启动后端（cd blog-backend; mvn spring-boot:run）
#   2. 在项目根目录执行：
#      powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
#   说明：逐个调用关键接口，打印 PASS/FAIL，最后汇总；有失败则退出码为 1
# ============================================================
param(
    [string]$BaseUrl = "http://localhost:8080/api",
    [string]$Username = "admin",
    [string]$Password = "123456"
)

$script:pass = 0
$script:fail = 0

function Assert-Result($name, $ok, $detail) {
    if ($ok) {
        $script:pass++
        Write-Host ("[PASS] " + $name) -ForegroundColor Green
    } else {
        $script:fail++
        Write-Host ("[FAIL] " + $name + " -> " + $detail) -ForegroundColor Red
    }
}

Write-Host "===== 1. 认证 =====" -ForegroundColor Cyan
$token = $null
try {
    $body = @{ username = $Username; password = $Password } | ConvertTo-Json
    $r = Invoke-RestMethod -Uri ($BaseUrl + "/auth/login") -Method Post -ContentType "application/json" -Body $body
    Assert-Result "登录 admin" ($r.code -eq 0 -and [bool]$r.data.token) ("code=" + $r.code)
    $token = $r.data.token
} catch {
    Assert-Result "登录 admin" $false $_.Exception.Message
}
if (-not $token) {
    Write-Host "登录失败，后续用例跳过" -ForegroundColor Red
    Write-Host ("汇总：PASS=" + $script:pass + " FAIL=" + $script:fail)
    exit 1
}
$headers = @{ Authorization = ("Bearer " + $token) }

Write-Host "===== 2. 前台公开接口 =====" -ForegroundColor Cyan
$checks = @(
    @{ name = "文章分页";  url = "/article/page?page=1&size=10" },
    @{ name = "分类列表";  url = "/category/list" },
    @{ name = "标签列表";  url = "/tag/list" },
    @{ name = "归档";      url = "/article/archive" },
    @{ name = "站点配置";  url = "/site/config" }
)
foreach ($c in $checks) {
    try {
        $r = Invoke-RestMethod -Uri ($BaseUrl + $c.url) -Method Get
        Assert-Result $c.name ($r.code -eq 0) ("code=" + $r.code)
    } catch {
        Assert-Result $c.name $false $_.Exception.Message
    }
}

Write-Host "===== 3. 后台需鉴权接口 =====" -ForegroundColor Cyan
$checks2 = @(
    @{ name = "仪表盘统计";    url = "/admin/dashboard/stats" },
    @{ name = "后台文章分页";  url = "/admin/article/page?page=1&size=10" },
    @{ name = "后台分类列表";  url = "/admin/category/list" },
    @{ name = "后台标签列表";  url = "/admin/tag/list" },
    @{ name = "后台评论分页";  url = "/admin/comment/page?page=1&size=10" }
)
foreach ($c in $checks2) {
    try {
        $r = Invoke-RestMethod -Uri ($BaseUrl + $c.url) -Method Get -Headers $headers
        Assert-Result $c.name ($r.code -eq 0) ("code=" + $r.code)
    } catch {
        Assert-Result $c.name $false $_.Exception.Message
    }
}

Write-Host "===== 4. 安全性 =====" -ForegroundColor Cyan
try {
    Invoke-RestMethod -Uri ($BaseUrl + "/admin/dashboard/stats") -Method Get | Out-Null
    Assert-Result "未登录访问后台返回 401" $false "竟然成功了"
} catch {
    $status = 0
    try { $status = [int]$_.Exception.Response.StatusCode } catch { }
    Assert-Result "未登录访问后台返回 401" ($status -eq 401) ("status=" + $status)
}

Write-Host "===== 5. 接口文档 =====" -ForegroundColor Cyan
try {
    $d = Invoke-WebRequest -Uri "http://localhost:8080/doc.html" -UseBasicParsing
    Assert-Result "Knife4j /doc.html" ($d.StatusCode -eq 200) ("status=" + $d.StatusCode)
} catch {
    Assert-Result "Knife4j /doc.html" $false $_.Exception.Message
}

Write-Host ""
Write-Host ("========== 汇总：PASS=" + $script:pass + "  FAIL=" + $script:fail + " ==========") -ForegroundColor Yellow
if ($script:fail -gt 0) { exit 1 } else { exit 0 }
