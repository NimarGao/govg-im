# IM 即时通讯系统 (Netty + Spring Boot)

本项目是一款基于 **Netty** 和 **Spring Boot** 开发的高性能即时通讯（IM）系统。
它不仅支持**私有 TCP 协议**（原生 App），还完美支持 **WebSocket 协议**（Web/H5/小程序），并提供了 HTTP API 用于文件上传等业务。

## ✨ 核心特性

*   **双协议支持**:
    *   **TCP**: 自定义二进制协议，高性能，适合原生客户端。
    *   **WebSocket**: 标准 WS 协议，适合 Web/H5/Uniapp 接入。
*   **分布式服务化升级 (IM-as-a-Service)**:
    *   支持跨物理实例路由广播、Redis Pub/Sub 单聊广播。
    *   全局用户在线状态即时探测与物理实例定位。
    *   长连接 Token 二次鉴权安全闭环与全局 Redis Session 挂钩。
*   **高可靠性与消息高级功能**:
    *   **指数退避重连**: 指数级退避弱网断线自动重连算法。
    *   **离线收纳箱 (Offline Hub)**: 智能离线消息全局 Redis 挂起并于登录时无感原装下发。
    *   **已读双勾与已读回执**: 捕获已读回执实现高保真双勾，群聊气泡提供 `X人已读` 浮窗及真实已读名单。
    *   **撤回与编辑**: 2 分钟双向平滑撤回（移除指示器并替换为系统灰字）及二次编辑（`已编辑` 标志）。
    *   **引用与 Thread 回复**: Aa 输入框上预览，引用气泡高斯模糊卡片及点击平滑滚动高亮定位。
*   **社交微交互与富媒体卡片 (TIMCustomElem)**:
    *   支持对消息进行六大表情回应（👍 ❤️ 😂 😮 😢 🙏），支持表情小药丸聚合、多人投票 Tooltip。
    *   支持分享名片 (User Card)、商品卡片 (Product Card) 精美卡片渲染与测试沙盒推送。
*   **音视频通话与多人会议 (TUICallKit / TUIRoomKit)**:
    *   一对一音视频通话（Redis 占线锁保护、高斯拉丝呼叫舱 `TUICallOverlay`、Canvas 动效正弦波）。
    *   多人会议协同舱（`TUIMeetingOverlay`、2x2 高斯网络、Canvas 动态声纹呼吸灯）。
*   **内容安全过滤与审核引擎 (AC 自动机)**:
    *   内置高性能 AC 自动机及 Trie 树，支持在线热重构词库及配置模式（MASK / BLOCK）。
    *   若触发阻断规则，强行拦截发送，前端捕获后消息气泡剧烈颤抖并以红色警告警示。
*   **离线推送与系统级 Web Notification 通知总线**:
    *   系统级 HTML5 浏览器 Notification 消息总线及 TIMPush 可视化离线推送控制中心。
*   **智能客服与分流评价系统 (TIMDesk)**:
    *   固定客服入口（呼吸光环），FAQ 机器人富文本互动，5 星流光评价卡片交互流。
    *   客服席位繁忙度最少连接数分流，无感由 Bot 专员转换过渡到客服人工席位。
*   **关系链与黑名单拦截 (TUIContact)**:
    *   好友添加、红点提示、玻璃盒申请审批列表。在捕获拉黑 `5002` 错误包时实时拦截并阻断发信。
*   **Instagram 像素级 UI/UX 体验重塑**:
    *   **全局配色与品牌登录**: HSL-like 配色、Grand Hotel 艺术 Logo 登录页、灰色四角胶囊表单及流光登录按钮。
    *   **极简导航栏与 3 层故事环**: Ins Outline 导航栏图标，头像嵌套 3 层 Stories 彩虹极光圈及 Hover 旋转特效。
    *   **横滑故事轨与未读蓝点**: 会话顶部横滑故事流轨，未读消息换装为高亮发光未读圆点 (`#0095f6`)。
    *   **Direct 渐变非对称气泡**: 消息气泡他人左下角变直薄边框，自发右下角变直并应用 Ins 签名渐变。
    *   **Direct 胶囊输入栏**: 饱满灰色高圆角胶囊输入框，输入字符时多媒体健半透明淡化，胶囊外浮现亮蓝“发送”纯文字按钮。
    *   **拉黑专属警告框**: 捕获 `5002` 时，本地气泡剧烈颤抖，屏幕中央弹起高精仿 Ins 原生单线分割确定警告框。

## 🛠️ 技术栈

*   **核心框架**: Spring Boot 3.x
*   **网络框架**: Netty 4.1
*   **数据库**: MySQL (用户/消息/群组持久化)
*   **缓存/消息**: Redis (会话管理), RabbitMQ (可选，集群消息同步)
*   **工具**: Lombok, FastJSON2

## ⚙️ 配置文件 (application.yml)

核心配置项说明：

```yaml
server:
  port: 10085  # HTTP API 端口 (上传/业务接口)

im:
  server:
    port: 10086       # Netty TCP 端口
    websocket-port: 10087 # Netty WebSocket 端口
  upload:
    path: ./uploads   # 图片存储路径 (建议生产环境改为绝对路径)
    url-prefix: /uploads
```

## 🚀 部署指南

### 1. 打包
```bash
mvn clean package -DskipTests
```

### 2. 启动命令 (示例)
推荐在启动时通过命令行参数覆盖配置，确保安全性与灵活性。

```bash
java -jar im-system-1.0-SNAPSHOT.jar \
  -Dfile.encoding=UTF-8 \
  --server.port=10085 \
  --im.server.websocket-port=10087 \
  --im.upload.path=/www/server/im/uploads \
  --MYSQL_HOST=127.0.0.1 \
  --MYSQL_USERNAME=im_db \
  --MYSQL_PASSWORD=your_password
```

### 3. Nginx 配置建议
生产环境建议使用 Nginx 反向代理，并配置 SSL。

```nginx
# 代理 WebSocket
location /ws {
    proxy_pass http://127.0.0.1:10087;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "Upgrade";
}

# 代理 HTTP API & 图片
location / {
    proxy_pass http://127.0.0.1:10085;
    proxy_set_header Host $host;
    
    # 允许跨域 (如果后端没生效)
    # add_header Access-Control-Allow-Origin *;
}
```

## 🔌 协议指令定义

| 指令码 | 描述 | 数据包结构 |
| :--- | :--- | :--- |
| 1 | 登录请求 | `LoginRequestPacket` |
| 2 | 登录响应 | `LoginResponsePacket` |
| 3 | 单聊消息 | `MessageRequestPacket` |
| 4 | 消息响应 | `MessageResponsePacket` |
| 11 | 群聊消息 | `GroupMessageRequestPacket` |
| 12 | 群聊响应 | `GroupMessageResponsePacket` |
| 13 | ACK 确认 | `AckPacket` |
| 16 | 已读回执请求 | `ReadReceiptRequestPacket` |
| 17 | 已读回执响应 | `ReadReceiptResponsePacket` |
| 18 | 正在输入请求 | `TypingRequestPacket` |
| 19 | 正在输入响应 | `TypingResponsePacket` |
| 21 | 撤回消息请求 | `RecallRequestPacket` |
| 22 | 撤回消息响应 | `RecallResponsePacket` |
| 26 | 编辑消息请求 | `EditRequestPacket` |
| 27 | 编辑消息响应 | `EditResponsePacket` |
| 28 | 群已读请求 | `GroupReadReceiptRequestPacket` |
| 29 | 群已读响应 | `GroupReadReceiptResponsePacket` |
| 30 | 表情回应请求 | `MessageReactionRequestPacket` |
| 31 | 表情回应响应 | `MessageReactionResponsePacket` |
| 40 | 音视频通话请求 | `VoiceCallRequestPacket` |
| 41 | 音视频通话响应 | `VoiceCallResponsePacket` |
| 42 | 用户状态订阅请求 | `UserStatusRequestPacket` |
| 43 | 用户状态订阅响应 | `UserStatusResponsePacket` |
| 44 | 审核配置热重构请求 | `AuditConfigRequestPacket` |
| 45 | 审核配置热重构响应 | `AuditConfigResponsePacket` |
| 46 | 智能客服与分流请求 | `DeskRequestPacket` |
| 47 | 智能客服与分流响应 | `DeskResponsePacket` |
| 48 | 多人视频会议请求 | `MultiVoiceCallRequestPacket` |
| 49 | 多人视频会议响应 | `MultiVoiceCallResponsePacket` |
| 50 | 好友添加申请请求 | `FriendAddRequestPacket` |
| 51 | 好友添加申请响应 | `FriendAddResponsePacket` |
| 52 | 好友关系/黑名单操作请求 | `RelationActionRequestPacket` |
| 53 | 好友关系/黑名单操作响应 | `RelationActionResponsePacket` |

## 📁 目录说明
*   `com.im.server`: Netty 服务端核心逻辑 (Handler, Channel, ServerBootstrap)
*   `com.im.protocol`: 协议包编解码与指令定义
*   `com.im.controller`: HTTP 接口 (如文件上传)
*   `com.im.config`: 全局配置 (CORS, WebMvc, Redis)
