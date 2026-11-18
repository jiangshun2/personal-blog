# 使用 Alibaba Cloud Workbench CLI 部署踩坑总结

> 记录本次把「个人博客系统（Spring Boot + Vue3 + MySQL + Redis）」从本地部署到阿里云 ECS 全过程中，使用 **alibabacloud-workbench-cli** 遇到的问题、根因与解决办法。
> 并分析：**哪些问题换一台电脑 / 换一个阿里云账户也可能遇到**，方便他人复用。

---

## 一、部署环境与目标

| 项 | 内容 |
|----|------|
| 本地 | Windows 11（家庭版 23H2）+ JDK 21 + Maven 3.9 + Node + Python 3.14 |
| 远端 | 阿里云 ECS（cn-hangzhou，2 核 2G，Ubuntu 22.04，已有公网 IP） |
| 工具 | workbench CLI v1.0.4（执行命令 / 上传文件） |
| 目标 | Docker Compose 部署（Nginx + Spring Boot + MySQL + Redis），绑定域名并启用 HTTPS |

---

## 二、问题清单（共 27 条，按类别）

> 「其他人是否易遇到」列说明：
> - 🔴 **极易**：换任何电脑/账户基本都会遇到
> - 🟠 **较易**：国内网络环境或常见配置下会遇到
> - 🟡 **一般**：特定条件下才会遇到

### 2.1 Workbench CLI 使用类

| # | 现象 | 原因 | 解决 | 其他人是否易遇到 |
|---|------|------|------|------------------|
| 1 | 命令行输入 workbench 提示不是内部或外部命令 | CLI 装到了用户目录，未加入系统 PATH | 用完整路径调用（如 C:\Users\<用户名>\AppData\Local\Programs\workbench\workbench.exe），或手动加 PATH | 🔴 极易 |
| 2 | 某次 exec 超时后，后续所有 exec 一直报 Connection timed out | 上一次执行中断，留下一个 BROKEN 会话卡住通道 | workbench session list 找到 BROKEN 会话，workbench session close <id>，再重试 | 🔴 极易 |
| 3 | 执行 apt install 等耗时命令时本地报超时，远端安装被中断 | exec 默认超时 30s（可 --timeout 调大，但本地调用也可能先超时） | 长任务改用「nohup 后台执行 + 轮询日志」：nohup bash script.sh > /tmp/x.log 2>&1 & | 🔴 极易 |
| 4 | 解析 list ecs 返回值报错 | 返回结构是 { "instances": [ ... ] }，不是裸数组；且必须带 --region | 按对象解析 .instances；先用常见地域挨个探测，或从控制台确认地域 | 🔴 极易 |
| 5 | upload 上传时卡住/被中断 | 目标文件已存在时 CLI 会交互询问是否覆盖；自动化场景无法应答 | 上传前先 ls 检查远端文件，或先删除/改名再上传 | 🟠 较易 |
| 6 | 上传进度输出到 stderr，PowerShell 误判为报错 | 进度条/日志走 stderr，PowerShell 把 stderr 当 NativeCommandError | 不要只看退出码，检查输出里是否有「Upload complete」 | 🟠 较易 |

### 2.2 网络与镜像类（国内环境高发）

| # | 现象 | 原因 | 解决 | 其他人是否易遇到 |
|---|------|------|------|------------------|
| 7 | docker pull 卡住或失败 | 国内直连 Docker Hub 基本不通 | 配置 registry mirror（本次用 https://docker.m.daocloud.io），重启 docker | 🔴 极易 |
| 8 | apt 安装 Docker CE 极慢（8 分钟只下 8MB） | 阿里云公网镜像站慢；内网镜像站又没有 docker-ce | 后台 nohup 安装 + 轮询日志；或改用系统源 docker.io | 🟠 较易 |
| 9 | 构建镜像时卡在 apk add tzdata | Alpine 软件源在国内很慢/超时 | 去掉 tzdata，改用 JVM 参数 -Duser.timezone=Asia/Shanghai（Java 自带时区库） | 🟠 较易 |
| 10 | git push 偶发 Connection was reset | GitHub 直连不稳定 | 隔段时间重试；或配代理 | 🟠 较易 |
| 11 | 本机 curl 访问自己服务器偶发连接重置 | 移动宽带多出口/网络抖动 | 换 Node/Python 发请求验证；服务器侧 curl localhost 确认服务正常 | 🟡 一般 |

### 2.3 云资源与安全组类

| # | 现象 | 原因 | 解决 | 其他人是否易遇到 |
|---|------|------|------|------------------|
| 12 | 控制台里明明加了 80/443 规则，公网还是打不开 | 规则加到了「另一个安全组」，实例实际绑定的安全组没有这两条 | 用 API 查实例的 SecurityGroupIds，再查对应安全组规则；把规则加到实例真正绑定的那个 | 🔴 极易（多实例/多安全组时） |
| 13 | 用 DescribeSecurityGroups 查规则返回空 | 该接口只返回安全组元数据，不含规则明细 | 改用 DescribeSecurityGroupAttribute（带 SecurityGroupId + Direction） | 🔴 极易（写脚本时） |
| 14 | 不知道怎么判断是服务问题还是网络问题 | 缺少分层排查 | 分端口测：Test-NetConnection 22/80/443；服务器内 curl localhost 对比 | 🟠 较易 |
| 15 | 把 22 收紧到「仅我的 IP」后，Workbench CLI 全部超时 | Workbench CLI 是走 22 端口 SSH 连实例的，出口 IP 不一定等于你查到的公网 IP | 临时放开 22 或加白名单；动态 IP 用户建议慎收紧 22 | 🔴 极易（动态 IP / 多出口） |
| 16 | 探测公网 IP 得到 IPv6 和 IPv4 两个值 | 移动/家宽常同时有双栈，出口不一致 | 安全组规则用 IPv4；以 IPv4 为准（本次：117.179.245.129） | 🟠 较易 |
| 17 | 域名访问不了，查不到 A 记录 | 新域名只有 _dnsauth 的 TXT 记录，未配 A 记录 | 在 Alidns 添加 A 记录：@ 和 www 指向实例公网 IP | 🔴 极易 |
| 18 | 域名解析后本机仍打不开 | 运营商/本机 DNS 缓存 | ipconfig /flushdns；或用公共 DNS 223.5.5.5 验证；curl --resolve 强制解析 | 🟠 较易 |
| 19 | 国内服务器绑域名访问异常 | 大陆 ECS 使用域名对外提供 Web 服务需 ICP 备案 | 提前完成备案，或先只用 IP 访问 | 🟠 较易 |

### 2.4 应用与数据类

| # | 现象 | 原因 | 解决 | 其他人是否易遇到 |
|---|------|------|------|------------------|
| 20 | 网站中文显示成「å‰ç«¯å¼€å‘」这类乱码 | MySQL 客户端默认字符集是 latin1，初始化 SQL 被按 latin1 导入，造成 UTF-8 双重编码 | 加 my.cnf（[client]/[mysql] default-character-set=utf8mb4），用 utf8mb4 重新导入；已有数据也可 CONVERT 修复 | 🔴 极易（用 Docker MySQL 初始化中文 SQL 的人） |
| 21 | 修正数据库后页面仍乱码 | 后端有多级缓存（Caffeine 本地 + Redis），旧数据还在缓存里 | FLUSHDB 清 Redis + 重启 backend，重新加载 | 🟠 较易 |
| 22 | 2G 内存机器上 MySQL 起不来/被 OOM | MySQL 8 默认内存占用较高，无 swap | 加 2G swap；MySQL 加 --innodb-buffer-pool-size=128M --performance-schema=OFF；限制 JVM 内存 | 🟠 较易（小规格 ECS） |
| 23 | 配置文件启动失败，报 YAML ScannerException | 值以 @ 开头（如密码 @xxx）在 YAML 里必须加引号 | 写成 "@xxx"（加英文双引号包裹）；敏感配置放 application-local.yml | 🟠 较易 |

### 2.5 Windows 本地工具链类

| # | 现象 | 原因 | 解决 | 其他人是否易遇到 |
|---|------|------|------|------------------|
| 24 | PowerShell 发 JSON 请求，后端收到的中文变成 ? | Windows PowerShell 发送 body 时未按 UTF-8 编码 | 显式 UTF-8 字节发送，或改用 Node.js 的 fetch 发请求 | 🔴 极易（Windows 下） |
| 25 | PowerShell 脚本执行报解析错误（中文乱码、引号错位） | 脚本是无 BOM 的 UTF-8，被按 ANSI/GBK 解析 | 保存为 UTF-8 with BOM；或改用 Node 写测试脚本 | 🟠 较易 |
| 26 | 运行 .ps1 被拒绝：未对文件进行数字签名 | 执行策略限制 | powershell -ExecutionPolicy Bypass -File xxx.ps1 | 🔴 极易 |
| 27 | curl 访问 HTTPS 报 CRYPT_E_REVOCATION_OFFLINE | Windows schannel 做证书吊销检查时连不上吊销服务器 | 加 --ssl-no-revoke；浏览器一般不受影响 | 🟡 一般 |
| 28 | Invoke-RestMethod -SkipCertificateCheck 报参数不存在 | 该参数只在 PowerShell 7+ 有，5.1 没有 | 用 curl --insecure 或升级到 PowerShell 7 | 🟡 一般 |

---

## 三、普遍性分析（换电脑/换账户会不会遇到）

### 🔴 几乎必然遇到（通用问题，建议提前准备）
1. CLI 不在 PATH（用全路径）
2. BROKEN 会话导致后续 exec 全超时（先 close）
3. 长命令需要 nohup 后台执行
4. list ecs 必须带 --region 且返回对象结构
5. 安全组规则加错安全组（多实例时）
6. DescribeSecurityGroups 查不到规则（要用 Attribute 接口）
7. 收紧 22 后 Workbench CLI 断连（它走 22）
8. 域名没有 A 记录
9. MySQL 客户端 latin1 导致中文双重编码
10. PowerShell 发 JSON 中文变 ?

### 🟠 国内网络/常见配置下大概率遇到
11. Docker Hub 直连不通 → 需要镜像加速
12. Docker CE 公网源很慢
13. Alpine apk 卡住 → 避开 tzdata
14. 2G 内存需要 swap + MySQL 低内存参数
15. 域名解析/缓存延迟
16. GitHub push 偶发失败
17. PowerShell 脚本需要 UTF-8 BOM / 执行策略

### 🟡 条件性遇到（特定环境）
18. 本机网络抖动（移动宽带多出口）
19. curl 证书吊销检查失败
20. Invoke-RestMethod 版本差异
21. ICP 备案拦截

### 本次特有（换人不一定遇到）
- 具体实例 ID / 安全组 ID / 域名 / 服务器密码等，属于本次环境
- 本机 Edge 无头截图方案属于交付增强，不属于部署必需

---

## 四、可复用的部署排查清单

### 部署前
- [ ] workbench CLI 已安装，能用全路径运行；daemon 状态正常
- [ ] 确认实例 ID、地域、操作系统（workbench list ecs --region xxx）
- [ ] 确认实例实际绑定的安全组，并提前放行 80/443
- [ ] 确认域名已添加 A 记录（或先用 IP 验证）
- [ ] 国内服务器确认是否需要 ICP 备案

### 部署中
- [ ] 长命令一律 nohup 后台执行，再轮询日志（不要指望一次 exec 跑完）
- [ ] 每次 exec 失败先看 session list，清理 BROKEN 会话
- [ ] Docker 配置 registry-mirrors 后再拉镜像
- [ ] 数据库字符集统一 utf8mb4（server + client 都要）
- [ ] 小内存机器先加 swap、限制 MySQL/JVM 内存

### 部署后
- [ ] 服务器内 curl localhost 确认服务正常（排除应用问题）
- [ ] 外部用 Test-NetConnection 分端口确认（排除安全组问题）
- [ ] 域名 + HTTPS 验证（curl --resolve 绕过本机 DNS）
- [ ] 清一次 Redis + 重启后端，排除缓存导致的数据不一致
- [ ] 截图/记录最终状态

---

## 五、最佳实践建议

1. **长任务后台化**：apt / docker pull / 构建，一律 nohup + 日志轮询，避免 exec 超时与中断。
2. **会话自愈**：exec 超时后先 workbench session list / close，再重试。
3. **安全组用 API 核对**：不要只看控制台页面，用 DescribeInstances + DescribeSecurityGroupAttribute 确认「实例真正绑定的安全组」及其规则。
4. **22 端口策略要谨慎**：Workbench CLI 依赖 22；动态 IP 用户建议保持 22 可用，或同时保留控制台 VNC 作为兜底。
5. **镜像加速必备**：国内 ECS 先配 registry-mirrors（如 docker.m.daocloud.io）。
6. **数据库字符集三件套**：server utf8mb4 + client utf8mb4 + 导入时 --default-character-set=utf8mb4。
7. **缓存与数据修复同步**：改完数据库记得清 Redis + 重启后端。
8. **脚本用 Node 代替 PowerShell 做中文相关测试**：避免 Windows 编码坑。
9. **敏感信息分离**：application-local.yml / .env 本地与服务端各自保存，不进 Git。
10. **交付留记录**：把本次踩坑与排查命令沉淀成文档（就是这份）。

---

## 六、常用命令速查

    # 版本 / 状态
    workbench version
    workbench daemon status
    workbench session list
    workbench session close <SESSION_ID>

    # 查实例（必须带 region）
    workbench list ecs --region cn-hangzhou --output json

    # 远程执行（长任务请后台化）
    workbench exec -i <INSTANCE_ID> --timeout 60 --command "whoami"
    workbench exec -i <INSTANCE_ID> --command "nohup bash /tmp/job.sh > /tmp/job.log 2>&1 & echo STARTED"

    # 上传 / 下载
    workbench upload ./deploy-bundle.tar.gz /tmp/ --instance-id <INSTANCE_ID>
    workbench download /var/log/app.log ./ --instance-id <INSTANCE_ID>

    # 服务器内自检
    docker compose ps
    docker compose logs --tail 50 backend
    curl -s -o /dev/null -w "%{http_code}" http://localhost/

    # 本地网络自检
    Test-NetConnection 121.41.164.72 -Port 443 -InformationLevel Quiet
    nslookup 域名 223.5.5.5
    curl.exe -s -o NUL -w "%{http_code}" --ssl-no-revoke --resolve 域名:443:IP https://域名/

---

## 七、结论

这次部署共遇到 **28 个问题**，其中：

- **10 个是通用问题**（换任何电脑/账户都可能遇到）
- **7 个来自国内网络环境**（镜像、GitHub、域名解析等）
- **4 个是条件性/特定环境问题**
- 其余为本次特定环境

**最容易浪费时间的三个坑**：
1. 安全组规则加错安全组（查了半天端口不通）
2. exec 会话 BROKEN 导致所有命令超时（误以为网络坏了）
3. MySQL latin1 导致中文乱码（数据库、缓存、前后端都要查）

提前知道这三点，能省下大量排查时间。
