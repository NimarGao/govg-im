# IM Client Demo (Uniapp)

这是一个基于 Uniapp 开发的 H5 前端 Demo，配合 Spring Boot + Netty 后端，实现了一个功能完备的即时通讯系统。

## ✨ 主要功能

*   **仿微信 UI**: 绿色气泡、吸顶标题栏、吸底输入框、无滚动条设计。
*   **核心聊天**: 支持**单聊**和**群聊**，实时收发消息。
*   **多媒体消息**: 支持**发送图片**，自动上传至后端服务器。
*   **消息状态**:
    *   **未读消息气泡**: 浏览历史消息时，右下角悬浮提示新消息数量。
    *   **全局未读数**: 左上角返回按钮显示应用总未读数。
    *   **自动滚动**: 发送消息或点击新消息提示时，自动平滑滚动到底部。
*   **本地持久化**: 使用 LocalStorage 缓存用户信息、会话列表和聊天记录，刷新页面不丢失。
*   **环境隔离**: 支持开发环境 (Dev) 和生产环境 (Prod) 自动切换配置。

## 🛠️ 目录结构

*   `pages/index/index.vue`: 核心页面（集成了登录、列表、聊天详情所有逻辑）。
*   `.env.development`: 开发环境配置文件（配置本地/测试服务器地址）。
*   `.env.production`: 生产环境配置文件（配置线上域名）。
*   `manifest.json`: 应用配置。

## 🚀 快速开始

### 1. 环境准备
确保已安装 [Node.js](https://nodejs.org/) 和 [HBuilderX](https://www.dcloud.io/hbuilderx.html) (可选)。

### 2. 安装依赖
```bash
cd im-client-uniapp
npm install
```

### 3. 配置环境变量
项目根目录下有两个环境文件：

*   **`.env.development`** (本地开发用):
    ```properties
    VUE_APP_WS_URL=ws://your-ip:10087/ws //your-ip：你的ip地址
    VUE_APP_API_URL=http://your-ip:10085
    ```
*   **`.env.production`** (线上部署用):
    ```properties
    VUE_APP_WS_URL=wss://im.your-domain.com/ws //im.your-domain.com：你的im域名
    VUE_APP_API_URL=https://im.your-domain.com
    ```

### 4. 运行项目
**使用 CLI:**
```bash
# 运行开发环境 (自动读取 .env.development)
npm run serve

# 打包生产环境 (自动读取 .env.production)
npm run build
```

**使用 HBuilderX:**
直接点击菜单栏 **运行** -> **运行到浏览器**。
*注意：HBuilderX 可能不会自动读取 .env 文件，建议手动检查代码中的默认 fallback 值。*

## 📦 部署说明

### Nginx 配置 (HTTPS 环境)
为了解决 HTTPS 页面加载 HTTP 图片的 Mixed Content 问题，以及跨域问题，建议在 Nginx 配置反向代理：

```nginx
server {
    listen 443 ssl;
    server_name im.your-domain.com;
    
    # 代理 WebSocket
    location /ws {
        proxy_pass http://127.0.0.1:10087;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "Upgrade";
    }
    
    # 代理 API (上传)
    location /api {
        proxy_pass http://127.0.0.1:10085;
        # 添加 CORS 头
        if ($request_method = 'OPTIONS') {
            add_header 'Access-Control-Allow-Origin' '*' always;
            add_header 'Access-Control-Allow-Methods' 'GET, POST, OPTIONS' always;
            return 204;
        }
    }
    
    # 代理图片资源 (解决 HTTPS 加载 HTTP 图片问题)
    location /uploads {
        proxy_pass http://127.0.0.1:10085;
    }
}
```

## 📝 注意事项

1.  **图片显示**: 如果在 HTTPS 环境下看不到图片，请检查 Nginx 是否配置了 `/uploads` 的反向代理。前端代码已内置逻辑，在 HTTPS 环境下会自动将图片域名替换为 `https://im.your-domain.com`。
2.  **跨域问题**: 后端已配置 `GlobalCorsFilter`，通常情况下直连也不会跨域。但在生产环境中，建议通过 Nginx 统一处理跨域。
3.  **消息丢失**: 目前消息仅缓存在本地 LocalStorage，清除浏览器缓存后消息会丢失。正式环境应接入后端历史消息 API。
