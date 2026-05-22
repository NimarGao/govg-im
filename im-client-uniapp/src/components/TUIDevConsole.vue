<template>
  <div v-if="activeTab === 'console'" class="full-height flex-col">
    <div class="sidebar-header">
      <span class="sidebar-title">Dev Console</span>
    </div>

    <!-- Segmented Sub-tab switcher -->
    <div class="dev-tabs-container">
      <div class="dev-tabs-list">
        <button class="dev-tab-btn" :class="{ active: devSubTab === 'simulators' }" @click="devSubTab = 'simulators'">
          🔌 接口模拟
        </button>
        <button class="dev-tab-btn" :class="{ active: devSubTab === 'push' }" @click="devSubTab = 'push'; loadPushHistory()">
          🚀 TIMPush
        </button>
        <button class="dev-tab-btn" :class="{ active: devSubTab === 'desk' }" @click="devSubTab = 'desk'">
          🧑‍💼 客服席
        </button>
        <button class="dev-tab-btn" :class="{ active: devSubTab === 'safety' }" @click="devSubTab = 'safety'">
          🛡️ 安全
        </button>
      </div>
    </div>

    <!-- SUB-TAB 1: SIMULATORS -->
    <div v-if="devSubTab === 'simulators'" class="list-scroll-view pad-15">
      
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

      <!-- Simulator 4: Custom Card Message Pusher -->
      <div class="console-glass-card">
        <span class="console-card-title">🃏 接口 4: 自定义分享卡片消息推送模拟</span>
        <span class="console-card-desc">一键模拟推送高档个人名片与好物推荐卡片消息 (msgType = 3)。</span>
        
        <div class="mini-form">
          <input class="mini-input" v-model="localSimPushToUserId" placeholder="推送目标用户 ID (默认为我自己)" />
          
          <div style="display: flex; gap: 8px;">
            <button class="mini-btn" style="flex: 1;" @click="pushUserCard">推送个人名片分享</button>
            <button class="mini-btn" style="flex: 1;" @click="pushProductCard">推送商品推荐分享</button>
          </div>
        </div>
      </div>

    </div>

    <!-- SUB-TAB 2: TIMPush Offline Push Center -->
    <div v-else-if="devSubTab === 'push'" class="list-scroll-view pad-15">

      <div class="console-glass-card">
        <span class="console-card-title">🔔 TIMPush 系统通知权限管理</span>
        <span class="console-card-desc">授权浏览器发送系统级通知，如屏幕以后据仍可弹出气泡提醒。</span>
        <div style="display: flex; gap: 8px; margin-top: 8px;">
          <button id="btn-request-notification" class="mini-btn" style="flex: 1;" @click="requestNotifPermission">
            {{ notifPermission === 'granted' ? '✅ 已获取权限' : '🔔 申请浏览器通知权限' }}
          </button>
          <button id="btn-test-notification" class="mini-btn primary-glow" style="flex: 1;" @click="testNotification" :disabled="notifPermission !== 'granted'">
            ✨ 发送测试通知
          </button>
        </div>
        <div v-if="notifPermission" class="console-terminal" style="margin-top: 10px;">
          <span class="terminal-text-cyan">香权限状态: {{ notifPermission === 'granted' ? '✅ 已授权' : notifPermission === 'denied' ? '❌ 已拒绝' : '⏳ 待审批' }}</span>
        </div>
      </div>

      <div class="console-glass-card">
        <span class="console-card-title">📊 离线推送监控日志</span>
        <span class="console-card-desc">实时拉取后端离线消息推送记录，展示推送漏斗数据。</span>
        <div style="display: flex; gap: 8px; margin-bottom: 10px;">
          <input class="mini-input" style="flex:1;" v-model="pushHistoryUserId" placeholder="过滤用户 ID（空则全部）" />
          <button id="btn-load-push-history" class="mini-btn" @click="loadPushHistory">🔄 刷新</button>
        </div>
        <div v-if="pushLogs && pushLogs.length > 0" class="push-log-list">
          <div v-for="(log, i) in pushLogs" :key="i" class="push-log-item" :id="'push-log-item-' + i">
            <div class="push-log-header">
              <span class="push-log-sender">📨 {{ log.senderName || log.senderId }}</span>
              <span class="push-log-status" :class="log.status === 'SENT' ? 'status-sent' : 'status-pending'">{{ log.status }}</span>
            </div>
            <div class="push-log-title">{{ log.title }}</div>
            <div class="push-log-body">{{ log.body }}</div>
            <div class="push-log-time">→ To: {{ log.userId }} &nbsp;|&nbsp; {{ log.createTime }}</div>
          </div>
        </div>
        <div v-else class="empty-tags-tip" style="padding: 20px 0;">暂无离线推送记录</div>
      </div>
    </div>

    <!-- SUB-TAB 3: Agent Workbench -->
    <div v-else-if="devSubTab === 'desk'" class="list-scroll-view pad-15">

      <div class="console-glass-card">
        <span class="console-card-title">📈 客服席位在线状态</span>
        <span class="console-card-desc">当前模拟客服席位（登录后可接单）。</span>
        <div v-for="agent in agentList" :key="agent.id" class="agent-card" :class="{ active: currentAgentId === agent.id }">
          <div class="agent-info">
            <span class="agent-avatar">{{ agent.name[0] }}</span>
            <div class="agent-meta">
              <span class="agent-name">{{ agent.name }}</span>
              <span class="agent-id">{{ agent.id }}</span>
            </div>
          </div>
          <div style="display: flex; gap: 6px;">
            <button class="mini-btn" :id="'btn-agent-login-' + agent.id"
                    :class="{ 'primary-glow': currentAgentId !== agent.id }"
                    @click="loginAsAgent(agent)">{{ currentAgentId === agent.id ? '已登录' : '模拟登录' }}</button>
          </div>
        </div>
      </div>

      <div v-if="currentAgentId" class="console-glass-card agent-inbox">
        <span class="console-card-title">📨 接单提醒区</span>
        <span class="console-card-desc">当用户转人工时，这里会显示连接请求。</span>
        <div v-for="(req, ri) in pendingCustomers" :key="ri" class="incoming-request animate-pop" :id="'agent-incoming-' + ri">
          <div class="request-user-info">
            <span class="request-avatar">{{ (req.userName || req.userId || '?')[0] }}</span>
            <div class="request-meta">
              <span class="request-name">{{ req.userName || req.userId }}</span>
              <span class="request-desc">已转入您的席位</span>
            </div>
          </div>
          <button class="mini-btn primary-glow" :id="'btn-agent-serve-' + ri" @click="startServingCustomer(req)">开始服务</button>
        </div>
        <div v-if="pendingCustomers.length === 0" class="empty-tags-tip" style="padding: 16px 0;">待命中，暂无世居接入...</div>
      </div>

      <div v-if="agentActiveCustomerId" class="console-glass-card">
        <span class="console-card-title">💬 服务中：{{ agentActiveCustomerId }}</span>
        <div class="agent-chat-log">
          <div v-for="(m, mi) in agentChatLog" :key="mi" class="agent-chat-msg" :class="{ 'from-customer': !m.fromAgent }" :id="'agent-chat-msg-' + mi">
            <span class="agent-msg-sender">{{ m.fromAgent ? (currentAgentName || '客服') : (agentActiveCustomerId) }}: </span>
            <span>{{ m.content }}</span>
          </div>
        </div>
        <div class="mini-form" style="margin-top: 10px;">
          <textarea class="mini-textarea" v-model="agentReplyContent" id="agent-reply-input" placeholder="输入回复内容..."></textarea>
          <div style="display: flex; gap: 8px;">
            <button id="btn-agent-send-reply" class="mini-btn primary-glow" style="flex:1;" @click="sendAgentReply">发送回复</button>
            <button id="btn-agent-end-session" class="mini-btn" style="flex:1; color: #ff453a; border-color: rgba(255,69,58,0.4);" @click="endAgentSession">结束服务</button>
          </div>
        </div>
      </div>
    </div>

    <!-- SUB-TAB 2: STATUS & SAFETY -->
    <div v-else-if="devSubTab === 'safety'" class="list-scroll-view pad-15">
      
      <!-- Section 1: User Status Configuration -->
      <div class="console-glass-card">
        <span class="console-card-title">✨ 我的多端在线状态 (User Status)</span>
        <span class="console-card-desc">设置您本人的状态呼吸灯类型与自定义心情文字签名，向所有好友实时广播同步。</span>
        
        <div class="status-selector-row">
          <button class="status-pill-btn online" :class="{ active: localStatusType === 'online' }" @click="localStatusType = 'online'">
            <span class="status-indicator-dot online"></span> 在线
          </button>
          <button class="status-pill-btn busy" :class="{ active: localStatusType === 'busy' }" @click="localStatusType = 'busy'">
            <span class="status-indicator-dot busy"></span> 忙碌
          </button>
          <button class="status-pill-btn away" :class="{ active: localStatusType === 'away' }" @click="localStatusType = 'away'">
            <span class="status-indicator-dot away"></span> 离开
          </button>
        </div>

        <div class="glass-label-group margin-top-15">
          <span class="glass-mini-label">自定义状态签名 (如: ☕ 摸鱼中)</span>
          <input class="mini-input" v-model="localStatusText" placeholder="写一句您此刻的签名状态吧..." />
        </div>

        <button class="glass-btn primary-glow margin-top-15" @click="updateStatus">
          🚀 广播我的最新状态
        </button>
      </div>

      <!-- Section 2: Cloud Auditing Configuration -->
      <div class="console-glass-card">
        <span class="console-card-title">🛡️ 智能内容安全过滤 (Cloud Auditing)</span>
        <span class="console-card-desc">热插拔配置敏感词汇过滤算法与拦截模式，保障内容符合安全规范。</span>
        
        <div class="policy-selector-row">
          <button class="policy-tab-btn" :class="{ active: localAuditPolicy === 'MASK' }" @click="localAuditPolicy = 'MASK'">
            🎭 MASK 掩码替换
          </button>
          <button class="policy-tab-btn block-btn" :class="{ active: localAuditPolicy === 'BLOCK' }" @click="localAuditPolicy = 'BLOCK'">
            🚫 BLOCK 拦截发送
          </button>
        </div>

        <div class="glass-label-group margin-top-15">
          <span class="glass-mini-label">敏感词库管理 (点击 × 可删除敏感词)</span>
          <div class="sensitive-tags-container">
            <span v-for="word in localSensitiveWords" :key="word" class="sensitive-word-tag animate-pop">
              {{ word }}
              <span class="tag-close-btn" @click="removeSensitiveWord(word)">×</span>
            </span>
            <span v-if="localSensitiveWords.length === 0" class="empty-tags-tip">当前敏感词库为空</span>
          </div>
        </div>

        <div class="glass-label-group margin-top-15">
          <span class="glass-mini-label">新增敏感过滤词</span>
          <div class="add-word-row">
            <input class="mini-input" style="flex: 1;" v-model="newSensitiveWord" placeholder="输入敏感词后按 Enter 键添加" @keyup.enter="addSensitiveWord" />
            <button class="mini-btn add-btn" @click="addSensitiveWord">+</button>
          </div>
        </div>

        <button class="glass-btn primary-glow danger-gradient margin-top-15" @click="syncAuditSettings">
          ⚡ 一键同步过滤策略
        </button>
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
    },
    myStatusType: {
      type: String,
      default: 'online'
    },
    myStatusText: {
      type: String,
      default: ''
    },
    auditPolicy: {
      type: String,
      default: 'BLOCK'
    },
    sensitiveWords: {
      type: Array,
      default: () => []
    }
  },
  emits: [
    'sim-generate-token',
    'copy-and-use-token',
    'sim-trigger-push',
    'sim-check-online',
    'set-theme',
    'save-settings',
    'update-status',
    'sync-audit',
    'agent-login',
    'agent-send-reply',
    'agent-end-session'
  ],
  data() {
    return {
      devSubTab: 'simulators',
      localSimTokenUserId: 'dev_user_777',
      localSimTokenUsername: '体验账号',
      localSimPushToUserId: '',
      localSimPushSender: '财务结算中心',
      localSimPushMsg: '【财务通知】您好，您申请的 320.00 元退款已原路打回原支付账户，请查收账单。',
      localSimCheckUserId: '',
      localServerHttpUrl: this.serverHttpUrl,
      localServerWsUrl: this.serverWsUrl,
      localExternalApiKey: this.externalApiKey,
      localStatusType: this.myStatusType,
      localStatusText: this.myStatusText,
      localAuditPolicy: this.auditPolicy,
      localSensitiveWords: [...this.sensitiveWords],
      newSensitiveWord: '',
      // TIMPush
      notifPermission: typeof Notification !== 'undefined' ? Notification.permission : 'default',
      pushLogs: [],
      pushHistoryUserId: '',
      // Agent Workbench
      agentList: [
        { id: 'agent_001', name: '金牌客服-安琳' },
        { id: 'agent_002', name: '资深客服-阿力' }
      ],
      currentAgentId: null,
      currentAgentName: '',
      pendingCustomers: [],
      agentActiveCustomerId: null,
      agentChatLog: [],
      agentReplyContent: ''
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
    },
    myStatusType(val) {
      this.localStatusType = val;
    },
    myStatusText(val) {
      this.localStatusText = val;
    },
    auditPolicy(val) {
      this.localAuditPolicy = val;
    },
    sensitiveWords(val) {
      this.localSensitiveWords = [...val];
    }
  },
  methods: {
    pushUserCard() {
      const userCardJson = JSON.stringify({
        cardType: 'user',
        userId: 'nimar_im_vip',
        username: '腾讯云即时通信大师',
        bio: '精通 TUIKit / Netty 架构设计与高级微交互研发。欢迎深入交流！'
      });
      this.$emit('sim-trigger-push', {
        toUserId: this.localSimPushToUserId,
        senderName: '分享助理',
        message: userCardJson,
        msgType: 3
      });
    },
    pushProductCard() {
      const productCardJson = JSON.stringify({
        cardType: 'product',
        title: '2026款 高保真玻璃态智能降噪无线耳机',
        price: '￥1,299.00',
        image: 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400',
        desc: '支持-48dB主动混合降噪，高解析度空间音频，60小时长续航。'
      });
      this.$emit('sim-trigger-push', {
        toUserId: this.localSimPushToUserId,
        senderName: '好物推荐中心',
        message: productCardJson,
        msgType: 3
      });
    },
    updateStatus() {
      this.$emit('update-status', {
        statusType: this.localStatusType,
        statusText: this.localStatusText
      });
    },
    addSensitiveWord() {
      const word = this.newSensitiveWord.trim();
      if (!word) return;
      if (!this.localSensitiveWords.includes(word)) {
        this.localSensitiveWords.push(word);
      }
      this.newSensitiveWord = '';
    },
    removeSensitiveWord(word) {
      this.localSensitiveWords = this.localSensitiveWords.filter(w => w !== word);
    },
    syncAuditSettings() {
      this.$emit('sync-audit', {
        policy: this.localAuditPolicy,
        sensitiveWords: this.localSensitiveWords
      });
    },
    // TIMPush methods
    async requestNotifPermission() {
      if (typeof Notification === 'undefined') {
        alert('您的浏览器不支持通知 API');
        return;
      }
      const perm = await Notification.requestPermission();
      this.notifPermission = perm;
    },
    testNotification() {
      if (this.notifPermission !== 'granted') return;
      new Notification('🚀 TIMPush 测试通知', {
        body: '这是一条系统级测试推送，点击即可唤醒应用！',
        icon: 'https://img.icons8.com/color/48/chat.png'
      });
    },
    async loadPushHistory() {
      const httpUrl = this.localServerHttpUrl || 'http://127.0.0.1:10085';
      let url = `${httpUrl}/api/external/push/history?limit=50`;
      if (this.pushHistoryUserId) url += `&userId=${encodeURIComponent(this.pushHistoryUserId)}`;
      try {
        const resp = await fetch(url);
        const data = await resp.json();
        if (data.success) this.pushLogs = data.logs || [];
      } catch (e) {
        console.error('Failed to load push history:', e);
      }
    },
    // Agent workbench methods
    loginAsAgent(agent) {
      this.currentAgentId = agent.id;
      this.currentAgentName = agent.name;
      this.pendingCustomers = [];
      this.agentChatLog = [];
      this.agentActiveCustomerId = null;
      this.$emit('agent-login', { agentId: agent.id, agentName: agent.name });
    },
    startServingCustomer(req) {
      this.agentActiveCustomerId = req.userId || req.userName;
      this.agentChatLog = [{ fromAgent: false, content: `客户【${req.userId || req.userName}】已接入。` }];
      this.pendingCustomers = this.pendingCustomers.filter(r => r !== req);
    },
    sendAgentReply() {
      if (!this.agentReplyContent.trim() || !this.agentActiveCustomerId) return;
      this.agentChatLog.push({ fromAgent: true, content: this.agentReplyContent });
      this.$emit('agent-send-reply', {
        agentId: this.currentAgentId,
        toUserId: this.agentActiveCustomerId,
        message: this.agentReplyContent
      });
      this.agentReplyContent = '';
    },
    endAgentSession() {
      this.$emit('agent-end-session', {
        agentId: this.currentAgentId,
        customerId: this.agentActiveCustomerId
      });
      this.agentChatLog.push({ fromAgent: false, content: '【系统提示】服务已结束。' });
      setTimeout(() => {
        this.agentActiveCustomerId = null;
        this.agentChatLog = [];
      }, 2000);
    },
    addIncomingCustomer(req) {
      this.pendingCustomers.push(req);
    },
    addAgentMessage(content, fromAgent = false) {
      this.agentChatLog.push({ fromAgent, content });
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

  /* Premium Segmented Tabs */
  .dev-tabs-container {
    padding: 0 15px 12px 15px;
  }
  .dev-tabs-list {
    display: flex;
    background: var(--input-bg);
    border: 1px solid var(--border-color);
    border-radius: 20px;
    padding: 2px;
  }
  .dev-tab-btn {
    flex: 1;
    background: transparent;
    border: none;
    border-radius: 18px;
    color: var(--text-desc);
    font-size: 13px;
    font-weight: 600;
    padding: 8px 12px;
    cursor: pointer;
    transition: all 0.25s ease;
  }
  .dev-tab-btn.active {
    background: var(--bg-board);
    color: var(--text-title);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
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
    font-size: 14px;
    height: 40px;
    line-height: 40px;
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

  .danger-gradient {
    background: linear-gradient(135deg, #ff453a 0%, #ff9f0a 100%) !important;
    box-shadow: 0 4px 12px rgba(255, 69, 58, 0.3) !important;
  }

  /* User status edit visual controls */
  .status-selector-row {
    display: flex;
    gap: 8px;
    margin-top: 8px;
  }
  .status-pill-btn {
    flex: 1;
    background: var(--bg-board);
    border: 1px solid var(--border-color);
    color: var(--text-title);
    padding: 8px 6px;
    border-radius: 20px;
    cursor: pointer;
    font-size: 12px;
    font-weight: 600;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    transition: all 0.2s ease;
  }
  .status-pill-btn:hover {
    background: var(--btn-hover);
  }
  .status-pill-btn.active.online {
    background: rgba(48, 209, 88, 0.15);
    border-color: #30d158;
    color: #30d158;
  }
  .status-pill-btn.active.busy {
    background: rgba(255, 159, 10, 0.15);
    border-color: #ff9f0a;
    color: #ff9f0a;
  }
  .status-pill-btn.active.away {
    background: rgba(255, 214, 10, 0.15);
    border-color: #ffd60a;
    color: #ffd60a;
  }
  .status-indicator-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
  }
  .status-indicator-dot.online { background: #30d158; }
  .status-indicator-dot.busy { background: #ff9f0a; }
  .status-indicator-dot.away { background: #ffd60a; }

  /* Cloud Auditing Visual controls */
  .policy-selector-row {
    display: flex;
    background: var(--bg-board);
    border: 1px solid var(--border-color);
    border-radius: 8px;
    padding: 2px;
  }
  .policy-tab-btn {
    flex: 1;
    background: transparent;
    border: none;
    border-radius: 6px;
    color: var(--text-desc);
    font-size: 12px;
    font-weight: 600;
    padding: 8px;
    cursor: pointer;
    transition: all 0.2s ease;
  }
  .policy-tab-btn.active {
    background: var(--input-bg);
    color: var(--text-title);
  }
  .policy-tab-btn.active.block-btn {
    background: rgba(255, 69, 58, 0.15);
    color: #ff453a;
  }

  .sensitive-tags-container {
    background: var(--bg-board);
    border: 1px solid var(--border-color);
    border-radius: 8px;
    padding: 8px;
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    min-height: 48px;
    align-items: center;
  }
  .sensitive-word-tag {
    background: var(--input-bg);
    border: 1px solid var(--border-color);
    border-radius: 12px;
    padding: 3px 8px;
    font-size: 11px;
    color: var(--text-title);
    display: inline-flex;
    align-items: center;
    gap: 4px;
  }
  .tag-close-btn {
    color: #ff453a;
    font-weight: bold;
    font-size: 12px;
    cursor: pointer;
    padding: 0 2px;
  }
  .tag-close-btn:hover {
    transform: scale(1.2);
  }
  .empty-tags-tip {
    font-size: 11px;
    color: var(--text-desc);
    width: 100%;
    text-align: center;
  }

  .add-word-row {
    display: flex;
    gap: 8px;
  }
  .add-btn {
    width: 36px;
    height: 36px;
    padding: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
    font-weight: bold;
  }

  /* Push Log Styles */
  .push-log-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
    max-height: 320px;
    overflow-y: auto;
  }
  .push-log-item {
    background: var(--bg-board);
    border: 1px solid var(--border-color);
    border-radius: 8px;
    padding: 10px 12px;
    display: flex;
    flex-direction: column;
    gap: 4px;
  }
  .push-log-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .push-log-sender {
    font-size: 12px;
    font-weight: 600;
    color: var(--active-blue);
  }
  .push-log-status {
    font-size: 10px;
    font-weight: bold;
    padding: 2px 6px;
    border-radius: 6px;
  }
  .status-sent {
    background: rgba(48, 209, 88, 0.15);
    color: #30d158;
    border: 1px solid rgba(48, 209, 88, 0.3);
  }
  .status-pending {
    background: rgba(255, 159, 10, 0.15);
    color: #ff9f0a;
    border: 1px solid rgba(255, 159, 10, 0.3);
  }
  .push-log-title {
    font-size: 12px;
    font-weight: 600;
    color: var(--text-title);
  }
  .push-log-body {
    font-size: 11px;
    color: var(--text-desc);
    line-height: 1.4;
  }
  .push-log-time {
    font-size: 10px;
    color: var(--text-desc);
    font-family: monospace;
  }

  /* Agent Workbench Styles */
  .agent-card {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px;
    border: 1px solid var(--border-color);
    border-radius: 10px;
    margin-top: 10px;
    background: var(--bg-board);
    transition: all 0.2s ease;
  }
  .agent-card.active {
    background: rgba(0, 132, 255, 0.08);
    border-color: var(--active-blue);
  }
  .agent-info {
    display: flex;
    align-items: center;
    gap: 10px;
  }
  .agent-avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background: linear-gradient(135deg, #0084ff, #00c6ff);
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: bold;
    font-size: 14px;
    flex-shrink: 0;
  }
  .agent-meta {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }
  .agent-name {
    font-size: 13px;
    font-weight: 600;
    color: var(--text-title);
  }
  .agent-id {
    font-size: 10px;
    color: var(--text-desc);
    font-family: monospace;
  }

  .agent-inbox {
    border: 1px solid rgba(255, 159, 10, 0.3) !important;
    background: rgba(255, 159, 10, 0.04) !important;
  }

  .incoming-request {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 10px;
    background: var(--bg-board);
    border: 1px solid rgba(255, 159, 10, 0.3);
    border-radius: 8px;
    margin-top: 8px;
  }
  .request-user-info {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  .request-avatar {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: linear-gradient(135deg, #ff9f0a, #ff453a);
    color: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: bold;
    font-size: 12px;
    flex-shrink: 0;
  }
  .request-meta {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }
  .request-name {
    font-size: 12px;
    font-weight: 600;
    color: var(--text-title);
  }
  .request-desc {
    font-size: 10px;
    color: #ff9f0a;
  }

  .agent-chat-log {
    background: var(--bg-board);
    border: 1px solid var(--border-color);
    border-radius: 8px;
    padding: 10px;
    max-height: 180px;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 8px;
  }
  .agent-chat-msg {
    font-size: 12px;
    color: var(--text-title);
    display: flex;
    gap: 4px;
    text-align: left;
  }
  .agent-chat-msg.from-customer {
    color: var(--active-blue);
  }
  .agent-msg-sender {
    font-weight: 600;
    flex-shrink: 0;
  }

  .animate-pop {
    animation: pop 0.25s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  }
  @keyframes pop {
    0% { transform: scale(0.8); opacity: 0; }
    100% { transform: scale(1); opacity: 1; }
  }
</style>
