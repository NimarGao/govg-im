<template>
  <div v-if="activeTab === 'console'" class="full-height flex-col">
    <div class="sidebar-header">
      <span class="sidebar-title">Dev Console</span>
    </div>
    <div class="list-scroll-view pad-15">
      
      <!-- Simulator 1: Token Ticket Generator -->
      <div class="console-glass-card">
        <span class="console-card-title">🔑 接口 1: Token 登录票据生成器</span>
        <span class="console-card-desc">模拟外部项目后台，调用本 IM 接口获取授权凭证。</span>
        
        <div class="mini-form">
          <input class="mini-input" v-model="localSimTokenUserId" placeholder="输入用户 ID" />
          <input class="mini-input" v-model="localSimTokenUsername" placeholder="输入用户昵称" />
          <button class="mini-btn" @click="$emit('sim-generate-token', { userId: localSimTokenUserId, username: localSimTokenUsername })">调用申请 Token</button>
        </div>

        <div v-if="generatedToken" class="console-terminal">
          <span class="terminal-text-cyan">Token 申请成功！</span>
          <span class="terminal-text-white">Token: {{ generatedToken }}</span>
          <button class="terminal-action-btn" @click="$emit('copy-and-use-token', generatedToken)">复制并使用该 Token 登录</button>
        </div>
      </div>

      <!-- Simulator 2: System Message Pusher -->
      <div class="console-glass-card">
        <span class="console-card-title">🔔 接口 2: 三方系统消息推送模拟</span>
        <span class="console-card-desc">直接调用 REST API，利用 Redis 订阅机制跨实例强推送消息给客户端。</span>
        
        <div class="mini-form">
          <input class="mini-input" v-model="localSimPushToUserId" placeholder="推送目标用户 ID (默认为我自己)" />
          <input class="mini-input" v-model="localSimPushSender" placeholder="模拟三方推送系统名称" />
          <textarea class="mini-textarea" v-model="localSimPushMsg" placeholder="请输入推送卡片内容..."></textarea>
          <button class="mini-btn primary-glow" @click="$emit('sim-trigger-push', { toUserId: localSimPushToUserId, senderName: localSimPushSender, message: localSimPushMsg })">触发外部系统推送</button>
        </div>
      </div>

      <!-- Simulator 3: Online Status Scanner -->
      <div class="console-glass-card">
        <span class="console-card-title">🛰️ 接口 3: 全局在线状态探测仪</span>
        <span class="console-card-desc">实时向 IM 查询任意用户的全局在线实例 and 路由状态。</span>
        
        <div class="mini-form">
          <input class="mini-input" v-model="localSimCheckUserId" placeholder="输入待查询用户 ID" />
          <button class="mini-btn" @click="$emit('sim-check-online', localSimCheckUserId)">查询在线状态</button>
        </div>

        <div v-if="onlineCheckResult" class="console-terminal">
          <pre class="terminal-json">{{ onlineCheckResult }}</pre>
        </div>
      </div>

    </div>
  </div>

  <div v-else-if="activeTab === 'settings'" class="full-height flex-col">
    <div class="sidebar-header">
      <span class="sidebar-title">Settings</span>
    </div>
    <div class="list-scroll-view pad-15">
      <!-- Theme Personalization Settings -->
      <div class="console-glass-card">
        <span class="console-card-title">🎨 主题个性化配置</span>
        <span class="console-card-desc">切换 Messenger 的界面视觉风格，体验极致视觉。</span>
        <div class="theme-switch-row" style="display: flex; gap: 10px; margin-top: 12px;">
          <button class="theme-btn-rect" :class="{ active: currentTheme === 'light' }" @click="$emit('set-theme', 'light')">☀️ 经典明亮</button>
          <button class="theme-btn-rect" :class="{ active: currentTheme === 'dark' }" @click="$emit('set-theme', 'dark')">🌙 极客暗黑</button>
        </div>
      </div>

      <div class="console-glass-card">
        <span class="console-card-title">⚙️ IM 服务物理节点配置</span>
        <span class="console-card-desc">前端连接及调用外部接口的服务器地址参数。</span>
        
        <div class="glass-label-group">
          <span class="glass-mini-label">HTTP REST API 地址</span>
          <input class="mini-input" v-model="localServerHttpUrl" />
        </div>
        
        <div class="glass-label-group">
          <span class="glass-mini-label">WebSocket 协议地址</span>
          <input class="mini-input" v-model="localServerWsUrl" />
        </div>

        <div class="glass-label-group">
          <span class="glass-mini-label">模拟三方调用的 API Key</span>
          <input class="mini-input" v-model="localExternalApiKey" placeholder="对应后端 application.yml 密钥" />
        </div>

        <button class="glass-btn primary-glow margin-top-15" @click="$emit('save-settings', { serverHttpUrl: localServerHttpUrl, serverWsUrl: localServerWsUrl, externalApiKey: localExternalApiKey })">保存网络配置</button>
      </div>

      <div class="console-glass-card">
        <span class="console-card-title">📱 关于 govg-im Client</span>
        <div class="about-item">当前用户: <span class="font-bold text-white">{{ userId || '未登录' }}</span></div>
        <div class="about-item">连接状态: <span :class="isConnected ? 'text-green' : 'text-red'">{{ isConnected ? 'ONLINE' : 'OFFLINE' }}</span></div>
        <div class="about-item">开发框架: Vue 3 (Facebook Messenger High-Fidelity UI)</div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'TUIDevConsole',
  props: {
    activeTab: {
      type: String,
      default: 'console'
    },
    generatedToken: {
      type: String,
      default: ''
    },
    onlineCheckResult: {
      type: String,
      default: ''
    },
    currentTheme: {
      type: String,
      default: 'light'
    },
    serverHttpUrl: {
      type: String,
      default: 'http://127.0.0.1:10085'
    },
    serverWsUrl: {
      type: String,
      default: 'ws://127.0.0.1:10087/ws'
    },
    externalApiKey: {
      type: String,
      default: 'your-default-secure-key'
    },
    isConnected: {
      type: Boolean,
      default: false
    },
    userId: {
      type: String,
      default: ''
    }
  },
  emits: [
    'sim-generate-token',
    'copy-and-use-token',
    'sim-trigger-push',
    'sim-check-online',
    'set-theme',
    'save-settings'
  ],
  data() {
    return {
      localSimTokenUserId: 'dev_user_777',
      localSimTokenUsername: '体验账号',
      localSimPushToUserId: '',
      localSimPushSender: '财务结算中心',
      localSimPushMsg: '【财务通知】您好，您申请的 320.00 元退款已原路打回原支付账户，请查收账单。',
      localSimCheckUserId: '',
      localServerHttpUrl: this.serverHttpUrl,
      localServerWsUrl: this.serverWsUrl,
      localExternalApiKey: this.externalApiKey
    };
  },
  watch: {
    serverHttpUrl(val) {
      this.localServerHttpUrl = val;
    },
    serverWsUrl(val) {
      this.localServerWsUrl = val;
    },
    externalApiKey(val) {
      this.localExternalApiKey = val;
    }
  }
};
</script>

<style scoped>
  .full-height {
    height: 100%;
  }
  .flex-col {
    display: flex;
    flex-direction: column;
  }
  .pad-15 {
    padding: 15px;
  }
  .margin-top-15 {
    margin-top: 15px;
  }
  .font-bold {
    font-weight: bold;
  }
  .text-white {
    color: #ffffff !important;
  }
  .text-green {
    color: var(--active-green) !important;
  }
  .text-red {
    color: #f3565d !important;
  }

  .sidebar-header {
    padding: 24px 20px 12px 20px;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .sidebar-title {
    font-size: 24px;
    font-weight: 800;
    color: var(--text-title);
    letter-spacing: -0.5px;
  }

  .list-scroll-view {
    flex: 1;
    overflow-y: auto;
  }

  /* Dev console card design */
  .console-glass-card {
    background: var(--input-bg);
    border: 1px solid var(--border-color);
    border-radius: 12px;
    padding: 16px;
    margin-bottom: 16px;
    box-sizing: border-box;
  }

  .console-card-title {
    font-size: 14px;
    font-weight: 700;
    color: var(--text-title);
    display: block;
    margin-bottom: 6px;
  }

  .console-card-desc {
    font-size: 12px;
    color: var(--text-desc);
    display: block;
    margin-bottom: 12px;
    line-height: 1.4;
  }

  .mini-form {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .mini-input, .mini-textarea {
    background: var(--bg-board);
    border: 1px solid var(--border-color);
    border-radius: 8px;
    padding: 8px 12px;
    font-size: 13px;
    color: var(--text-title);
    outline: none;
    box-sizing: border-box;
    width: 100%;
  }
  .mini-textarea {
    height: 60px;
    resize: none;
  }
  .mini-input:focus, .mini-textarea:focus {
    border-color: var(--active-blue);
  }

  .mini-btn {
    background: var(--btn-hover);
    color: var(--text-title);
    border: 1px solid var(--border-color);
    padding: 8px 12px;
    border-radius: 8px;
    cursor: pointer;
    font-size: 13px;
    font-weight: 600;
    transition: all 0.2s ease;
  }
  .mini-btn:hover {
    background: var(--btn-active);
  }

  .console-terminal {
    background: var(--terminal-bg);
    border-left: 3px solid var(--active-blue);
    padding: 10px;
    border-radius: 6px;
    margin-top: 12px;
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .terminal-text-cyan {
    font-size: 12px;
    font-weight: bold;
    color: var(--active-blue);
  }

  .terminal-text-white {
    font-size: 11px;
    word-break: break-all;
    color: var(--terminal-text);
    font-family: monospace;
  }

  .terminal-action-btn {
    background: var(--active-blue);
    color: #ffffff;
    border: none;
    padding: 6px 12px;
    border-radius: 6px;
    cursor: pointer;
    font-size: 12px;
    font-weight: 600;
    align-self: flex-start;
  }

  .terminal-json {
    font-family: monospace;
    font-size: 11px;
    background: transparent;
    color: var(--terminal-text);
    margin: 0;
    white-space: pre-wrap;
    word-break: break-all;
  }

  .theme-switch-row {
    display: flex;
    gap: 8px;
    margin-top: 10px;
  }

  .theme-btn-rect {
    flex: 1;
    background: var(--btn-hover);
    border: 1px solid var(--border-color);
    color: var(--text-title);
    padding: 10px;
    border-radius: 8px;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s ease;
  }
  .theme-btn-rect.active {
    background: var(--active-blue);
    color: #ffffff;
    border-color: var(--active-blue);
  }

  .glass-label-group {
    display: flex;
    flex-direction: column;
    gap: 4px;
    margin-bottom: 12px;
  }

  .glass-mini-label {
    font-size: 11px;
    font-weight: bold;
    color: var(--text-desc);
    text-transform: uppercase;
  }

  .about-item {
    font-size: 13px;
    color: var(--text-desc);
    margin-bottom: 6px;
  }

  .glass-btn {
    background: var(--active-blue);
    color: #ffffff;
    border: none;
    border-radius: 10px;
    font-size: 16px;
    height: 48px;
    line-height: 48px;
    font-weight: 600;
    cursor: pointer;
    text-align: center;
    transition: all 0.2s ease;
    width: 100%;
    box-shadow: 0 4px 12px rgba(0, 132, 255, 0.2);
  }
  .glass-btn:hover {
    filter: brightness(1.08);
    transform: translateY(-1px);
  }
  .glass-btn:active {
    transform: translateY(0);
    filter: brightness(0.95);
  }

  .primary-glow {
    background: var(--active-blue) !important;
  }
</style>
