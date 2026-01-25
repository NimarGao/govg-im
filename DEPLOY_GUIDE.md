# IM 系统服务端部署指南

本文档包含**通用服务器部署**与**宝塔面板 (BT Panel) 部署**两种方案。

## 1. 准备工作

### 1.1 打包 JAR 文件
在本地开发环境（IDEA）中进行打包：
1. 打开右侧 **Maven** 面板。
2. 依次点击 `Lifecycle` -> `clean` -> `package`。
3. 打包完成后，在 `target` 目录下会生成 `im-system-1.0-SNAPSHOT.jar`。

---

## 2. 方案一：宝塔面板 (BT Panel) 部署（推荐新手）

### 2.1 环境安装
登录宝塔面板，进入【软件商店】，安装以下软件：
1. **Java 项目管理器** (或直接安装 JDK 17)
   - 必须安装 **JDK 17** 版本。
2. **MySQL** (推荐 5.7 或 8.0)
3. **Redis**
4. **RabbitMQ** (在软件商店搜索安装，如果宝塔没有直接提供，可能需要通过 Docker 安装或手动安装 Erlang+RabbitMQ)
   - *注意：如果宝塔装 RabbitMQ 比较麻烦，建议使用 Docker 方式部署中间件，或者直接跳到方案二。*

### 2.2 数据库配置
1. 进入【数据库】->【添加数据库】。
2. 数据库名：`im_db`
3. 用户名/密码：记下你设置的账号密码（稍后要填）。
4. 访问权限：本地服务器。

### 2.3 开放端口
进入【安全】->【添加端口规则】，放行以下端口：
- **10085** (Web HTTP 接口)
- **10086** (TCP Socket 端口)
- **10087** (WebSocket 端口)

### 2.4 上传与部署
1. 进入【文件】，在 `/www/wwwroot` 下新建文件夹 `im-server`。
2. 将本地打包好的 `im-system-1.0-SNAPSHOT.jar` 上传到该目录。
3. 在同级目录下创建一个 `application-prod.yml` (生产环境配置)，填入你的宝塔环境信息：

```yaml
spring:
  datasource:
    # 宝塔数据库一般是 localhost，密码填你 2.2 步设置的
    url: jdbc:mysql://localhost:3306/im_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: im_db_user
    password: your_password
  redis:
    host: localhost
    port: 6379
  rabbitmq:
    host: localhost
    port: 5672
    username: guest  # 默认是 guest，如果改了请同步
    password: guest

im:
  zookeeper:
    enabled: false # 如果你没装 Zookeeper，请务必设为 false
```

### 2.5 启动服务
你可以使用【Java 项目管理器】添加项目，或者直接使用终端启动。

**终端启动命令（推荐）：**
```bash
cd /www/wwwroot/im-server

# 启动并指定配置文件
nohup /usr/local/java/jdk-17.0.x/bin/java -jar im-system-1.0-SNAPSHOT.jar --spring.config.location=application-prod.yml > server.log 2>&1 &
```
*注意：请将 `/usr/local/java/jdk-17.0.x/bin/java` 替换为你宝塔实际安装的 JDK 17 路径，或者如果已经配好环境变量直接用 `java`。*

---

## 3. 方案二：Docker 容器化部署（最快）

如果你服务器安装了 Docker 和 Docker Compose，这是最简单的方法。

### 3.1 上传文件
将项目中的以下文件上传到服务器同一个目录（例如 `/opt/im-server`）：
- `im-system-1.0-SNAPSHOT.jar` (改名为 `app.jar` 或者修改 Dockerfile)
- `Dockerfile`
- `docker-compose.yml`

*注意：你需要修改一下 `Dockerfile`，确保它复制的是你上传的 jar 包名。*

### 3.2 启动
在目录下执行：
```bash
docker-compose up -d --build
```

Docker 会自动安装 MySQL, Redis, RabbitMQ, Zookeeper 并启动你的 Java 服务。

---

## 4. 常见问题

**Q: 启动报错 "Connection refused" 连接 Zookeeper 失败？**
A: 请检查配置文件中 `im.zookeeper.enabled` 是否为 `false`。除非你真的安装并启动了 Zookeeper，否则不要开启。

**Q: 前端连不上 WebSocket？**
A: 
1. 检查服务器防火墙（阿里云/腾讯云的安全组）是否开放了 `10087` 端口。
2. 检查宝塔面板的【安全】里是否放行了 `10087`。
3. 确认前端代码里的连接地址是 `ws://你的服务器IP:10087/ws`。
