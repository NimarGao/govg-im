# IM 即时通讯系统 (Netty + Spring Boot)

本项目是一款基于 **Netty** 和 **Spring Boot** 开发的高性能即时通讯（IM）系统。
它不仅支持**私有 TCP 协议**（原生 App），还完美支持 **WebSocket 协议**（Web/H5/小程序），并提供了 HTTP API 用于文件上传等业务。

## ✨ 核心特性

*   **双协议支持**:
    *   **TCP**: 自定义二进制协议，高性能，适合原生客户端。
    *   **WebSocket**: 标准 WS 协议，适合 Web/H5/Uniapp 接入。
*   **全功能聊天**:
    *   **单聊**: 点对点消息即时投递。
    *   **群聊**: 创建群、加入群、群消息广播。
    *   **多媒体**: 支持图片上传与发送。
*   **高可靠性**:
    *   **ACK 机制**: 应用层消息确认，防止消息丢失。
    *   **心跳检测**: 自动检测并断开无效连接。
*   **Web 增强**:
    *   **CORS**: 完善的跨域配置 (`GlobalCorsFilter`)，支持前后端分离开发。
    *   **静态资源映射**: 自动映射上传目录，支持图片直链访问。

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

## 📁 目录说明
*   `com.im.server`: Netty 服务端核心逻辑 (Handler, Channel, ServerBootstrap)
*   `com.im.protocol`: 协议包编解码与指令定义
*   `com.im.controller`: HTTP 接口 (如文件上传)
*   `com.im.config`: 全局配置 (CORS, WebMvc, Redis)
