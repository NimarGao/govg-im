<template>
  <div class="app-container" :class="'theme-' + currentTheme">
    <!-- Background Decorative Lights -->
    <div class="glow-orb orb-1"></div>
    <div class="glow-orb orb-2"></div>

    <!-- ================= LOGIN SCREEN ================= -->
    <div v-if="!isLogin" class="login-wrapper">
      <div class="login-glass-card">
        <div class="login-header">
          <div class="logo-badge">💬</div>
          <span class="login-title">govg-im 开放服务化体验仓</span>
          <span class="login-subtitle">基于 Spring Boot 3 & Netty 的高性能即时通讯平台</span>
        </div>

        <!-- Tab Switches for Login Modes -->
        <div class="login-tab-header">
          <div class="login-tab" :class="{ active: loginMode === 'token' }" @click="loginMode = 'token'">
            🔑 Token 安全登录
          </div>
          <div class="login-tab" :class="{ active: loginMode === 'legacy' }" @click="loginMode = 'legacy'">
            👤 游客直接登录
          </div>
        </div>

        <!-- Forms -->
        <div class="login-form">
          <!-- Token Login -->
          <div v-if="loginMode === 'token'" class="form-fade">
            <div class="glass-input-group">
              <span class="input-icon">🔑</span>
              <input class="glass-input" v-model="secureTokenInput" placeholder="请粘贴由开放接口申请到的 Token" @keyup.enter="login" />
            </div>
            <div class="input-tip-box" style="display: flex; justify-content: space-between; align-items: center; gap: 8px;">
              <span>💡 提示：支持通过外部系统申请的安全 Token。</span>
              <a href="javascript:void(0)" class="quick-generate-link" @click="quickGenerateLoginToken" style="flex-shrink: 0;">⚡ 一键生成并填入测试 Token</a>
            </div>
          </div>

          <!-- Legacy Login -->
          <div v-else class="form-fade">
            <div class="glass-input-group">
              <span class="input-icon">👤</span>
              <input class="glass-input" v-model="userIdInput" placeholder="请输入用户 ID (英文/数字)" @keyup.enter="login" />
            </div>
            <div class="glass-input-group">
              <span class="input-icon">📛</span>
              <input class="glass-input" v-model="username" placeholder="请输入昵称 (不填同 ID)" @keyup.enter="login" />
            </div>
          </div>

          <button class="glass-btn primary-glow" @click="login">进入系统</button>
        </div>

        <div class="connection-status-bar" :class="{ active: isConnected }">
          <span class="status-dot"></span>
          {{ isConnected ? '已建立 WebSocket 网络连接' : '正在建立安全通道中...' }}
        </div>
      </div>
    </div>

    <!-- ================= MAIN APP BOARD ================= -->
    <div v-else class="app-glass-board">
      <!-- 1. LEFT NARROW NAVIGATION BAR -->
      <div class="nav-sidebar">
        <div class="sidebar-top-group">
          <div class="user-avatar-wrapper" @click="copyId" title="点击复制用户 ID">
            <div class="user-avatar-gradient">
              <span class="avatar-letter">{{ username ? username[0].toUpperCase() : 'U' }}</span>
            </div>
            <span class="online-indicator active"></span>
          </div>

          <div class="nav-menu">
            <div class="nav-item" :class="{ active: activeTab === 'chats' }" @click="activeTab = 'chats'">
              <span class="nav-icon">💬</span>
              <span class="nav-label">聊天</span>
              <div v-if="totalUnread > 0" class="unread-badge-mini">{{ totalUnread }}</div>
            </div>
            <div class="nav-item" :class="{ active: activeTab === 'contacts' }" @click="activeTab = 'contacts'">
              <span class="nav-icon">👥</span>
              <span class="nav-label">联系人</span>
            </div>
            <div class="nav-item" :class="{ active: activeTab === 'console' }" @click="activeTab = 'console'">
              <span class="nav-icon">🛠️</span>
              <span class="nav-label">测试台</span>
            </div>
            <div class="nav-item" :class="{ active: activeTab === 'settings' }" @click="activeTab = 'settings'">
              <span class="nav-icon">⚙️</span>
              <span class="nav-label">设置</span>
            </div>
          </div>
        </div>

        <div class="sidebar-bottom-group">
          <!-- Theme Switch Button -->
          <div class="theme-toggle-sidebar-btn" @click="toggleTheme" :title="currentTheme === 'dark' ? '切换为明亮模式' : '切换为暗黑模式'">
            <span class="theme-toggle-icon">{{ currentTheme === 'dark' ? '☀️' : '🌙' }}</span>
          </div>

          <div class="logout-btn" @click="logout" title="退出系统">
            <span class="logout-icon">🚪</span>
          </div>
        </div>
      </div>

      <!-- 2. MIDDLE LISTING AREA -->
      <div class="middle-sidebar" :class="{ 'mobile-hidden': view === 'chat' }">
        
        <!-- Tab 1: Session Chat list -->
        <div v-if="activeTab === 'chats'" class="full-height flex-col" style="position: relative;">
          <div class="sidebar-header">
            <span class="sidebar-title">Chats</span>
            <div class="header-actions">
              <div class="circle-action-btn" @click="showStartChat = true" title="发起私聊">💬</div>
              <div class="circle-action-btn" @click="showCreateGroup = true" title="群组操作">+</div>
            </div>
          </div>
          <div class="search-box">
            <input class="glass-search" placeholder="搜索会话..." />
          </div>

          <!-- Horizontal Active Friends Scroll Rail (Facebook Messenger style) -->
          <div class="active-friends-rail">
            <div class="active-friend-item" v-for="friend in friendList" :key="friend.id" @click="startChatWith(friend.id)">
              <div class="active-friend-avatar">
                <span class="friend-letter">{{ friend.name[0].toUpperCase() }}</span>
                <span class="online-indicator-dot" :class="{ active: friend.online }"></span>
              </div>
              <span class="active-friend-name">{{ friend.name.split(' ')[0] }}</span>
            </div>
          </div>

          <div class="list-scroll-view">
            <div v-if="sessions.length === 0" class="empty-list-tip">暂无活动会话</div>
            <div v-for="(session, index) in sessions" :key="index" 
                class="session-glass-card" :class="{ active: currentSession && currentSession.id === session.id }"
                @click="openChat(session)">
              <div class="avatar-circle">
                {{ session.name[0].toUpperCase() }}
                <span class="online-indicator active"></span>
              </div>
              <div class="session-info">
                <div class="session-header-row">
                  <span class="session-name">{{ session.name }}</span>
                  <span class="session-time">{{ session.time }}</span>
                </div>
                <div class="session-body-row">
                  <span class="session-preview">{{ session.lastMsg }}</span>
                  <div v-if="session.unread > 0" class="unread-bubble">{{ session.unread }}</div>
                </div>
              </div>
            </div>
          </div>

          <!-- Floating Action Button (Messenger Style) -->
          <div class="messenger-fab-container" :class="{ active: showFabMenu }" @mouseleave="showFabMenu = false">
            <div class="fab-submenu">
              <div class="fab-submenu-item" @click="showCreateGroup = true; showFabMenu = false" title="群聊操作">
                <span class="fab-submenu-label">创建或加入群聊</span>
                <div class="fab-submenu-icon">👥</div>
              </div>
              <div class="fab-submenu-item" @click="showStartChat = true; showFabMenu = false" title="发起私聊">
                <span class="fab-submenu-label">发起新私聊</span>
                <div class="fab-submenu-icon">💬</div>
              </div>
            </div>
            <div class="messenger-main-fab" @click="showFabMenu = !showFabMenu" :title="showFabMenu ? '关闭菜单' : '新建聊天/群组'">
              <span class="main-fab-icon">+</span>
            </div>
          </div>
        </div>


        <!-- Tab 2: Contacts list -->
        <div v-if="activeTab === 'contacts'" class="full-height flex-col">
          <div class="sidebar-header">
            <span class="sidebar-title">Contacts</span>
          </div>
          <div class="search-box">
            <input class="glass-search" placeholder="搜索好友..." />
          </div>
          <div class="list-scroll-view">
            <div class="contact-section-title">我的好友列表</div>
            <div v-for="friend in friendList" :key="friend.id" class="contact-glass-card" @click="startChatWith(friend.id)">
              <div class="avatar-circle font-bold">
                {{ friend.name[0].toUpperCase() }}
                <span class="online-indicator" :class="{ active: friend.online }"></span>
              </div>
              <div class="contact-info">
                <span class="contact-name">{{ friend.name }}</span>
                <span class="contact-status" :class="{ online: friend.online }">
                  {{ friend.online ? '在线 (' + friend.node + ')' : '离线' }}
                </span>
              </div>
              <div class="quick-chat-action">💬</div>
            </div>
          </div>
        </div>

        <!-- Tab 3: Developer Platform Console -->
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
                <input class="mini-input" v-model="simTokenUserId" placeholder="输入用户 ID" />
                <input class="mini-input" v-model="simTokenUsername" placeholder="输入用户昵称" />
                <button class="mini-btn" @click="simGenerateToken">调用申请 Token</button>
              </div>

              <div v-if="generatedToken" class="console-terminal">
                <span class="terminal-text-cyan">Token 申请成功！</span>
                <span class="terminal-text-white">Token: {{ generatedToken }}</span>
                <button class="terminal-action-btn" @click="copyAndUseToken">复制并使用该 Token 登录</button>
              </div>
            </div>

            <!-- Simulator 2: System Message Pusher -->
            <div class="console-glass-card">
              <span class="console-card-title">🔔 接口 2: 三方系统消息推送模拟</span>
              <span class="console-card-desc">直接调用 REST API，利用 Redis 订阅机制跨实例强推送消息给客户端。</span>
              
              <div class="mini-form">
                <input class="mini-input" v-model="simPushToUserId" placeholder="推送目标用户 ID (默认为我自己)" />
                <input class="mini-input" v-model="simPushSender" placeholder="模拟三方推送系统名称" />
                <textarea class="mini-textarea" v-model="simPushMsg" placeholder="请输入推送卡片内容..."></textarea>
                <button class="mini-btn primary-glow" @click="simTriggerPush">触发外部系统推送</button>
              </div>
            </div>

            <!-- Simulator 3: Online Status Scanner -->
            <div class="console-glass-card">
              <span class="console-card-title">🛰️ 接口 3: 全局在线状态探测仪</span>
              <span class="console-card-desc">实时向 IM 查询任意用户的全局在线实例和路由状态。</span>
              
              <div class="mini-form">
                <input class="mini-input" v-model="simCheckUserId" placeholder="输入待查询用户 ID" />
                <button class="mini-btn" @click="simCheckOnline">查询在线状态</button>
              </div>

              <div v-if="onlineCheckResult" class="console-terminal">
                <pre class="terminal-json">{{ onlineCheckResult }}</pre>
              </div>
            </div>

          </div>
        </div>

        <!-- Tab 4: System Parameters Settings -->
        <div v-if="activeTab === 'settings'" class="full-height flex-col">
          <div class="sidebar-header">
            <span class="sidebar-title">Settings</span>
          </div>
          <div class="list-scroll-view pad-15">
            <!-- Theme Personalization Settings -->
            <div class="console-glass-card">
              <span class="console-card-title">🎨 主题个性化配置</span>
              <span class="console-card-desc">切换 Messenger 的界面视觉风格，体验极致视觉。</span>
              <div class="theme-switch-row" style="display: flex; gap: 10px; margin-top: 12px;">
                <button class="theme-btn-rect" :class="{ active: currentTheme === 'light' }" @click="setTheme('light')">☀️ 经典明亮</button>
                <button class="theme-btn-rect" :class="{ active: currentTheme === 'dark' }" @click="setTheme('dark')">🌙 极客暗黑</button>
              </div>
            </div>

            <div class="console-glass-card">
              <span class="console-card-title">⚙️ IM 服务物理节点配置</span>
              <span class="console-card-desc">前端连接及调用外部接口的服务器地址参数。</span>
              
              <div class="glass-label-group">
                <span class="glass-mini-label">HTTP REST API 地址</span>
                <input class="mini-input" v-model="serverHttpUrl" />
              </div>
              
              <div class="glass-label-group">
                <span class="glass-mini-label">WebSocket 协议地址</span>
                <input class="mini-input" v-model="serverWsUrl" />
              </div>

              <div class="glass-label-group">
                <span class="glass-mini-label">模拟三方调用的 API Key</span>
                <input class="mini-input" v-model="externalApiKey" placeholder="对应后端 application.yml 密钥" />
              </div>

              <button class="glass-btn primary-glow margin-top-15" @click="saveSettings">保存网络配置</button>
            </div>

            <div class="console-glass-card">
              <span class="console-card-title">📱 关于 govg-im Client</span>
              <div class="about-item">当前用户: <span class="font-bold text-white">{{ userId || '未登录' }}</span></div>
              <div class="about-item">连接状态: <span :class="isConnected ? 'text-green' : 'text-red'">{{ isConnected ? 'ONLINE' : 'OFFLINE' }}</span></div>
              <div class="about-item">开发框架: Vue 3 (Facebook Messenger High-Fidelity UI)</div>
            </div>
          </div>
        </div>

      </div>

      <!-- 3. RIGHT CORE CHAT AREA -->
      <div class="chat-viewport" :class="{ 'mobile-active': view === 'chat' }">
        <div v-if="currentSession" class="chat-frame">
          <!-- Chat Header -->
          <div class="chat-header-bar">
            <div class="back-action-btn" @click="view = 'list'">
              <span class="back-arrow">❮</span>
              <span class="back-label">返回</span>
              <span v-if="totalUnread > 0" class="header-unread-count">{{ totalUnread }}</span>
            </div>
            
            <div class="header-user-info">
              <span class="header-user-name">{{ currentSession.name }}</span>
              <div class="header-user-status">
                <span class="status-dot"></span>
                Active Now
              </div>
            </div>

            <div class="header-actions">
              <div class="header-icon-btn" @click="showRightPanel = !showRightPanel" title="会话信息">ℹ️</div>
            </div>
          </div>

          <!-- Messages Scroller -->
          <div class="chat-messages-scroller" @scroll="onScroll">
            <div v-for="(msg, index) in currentMsgs" :key="index" class="msg-bubble-wrapper" 
                :class="{ 'self-msg': msg.isSelf, 'system-push-msg': msg.sender === 'system_service' }" :id="'msg-'+index">
              
              <!-- Time Break -->
              <div class="msg-time-divider" v-if="shouldShowTime(index)">{{ msg.time }}</div>
              
              <!-- 1. System Notification (Gray Centered Bubble) -->
              <div v-if="msg.sender === 'system_notification'" class="system-notification-row animate-pop">
                <span class="system-notification-text">{{ msg.content }}</span>
              </div>

              <!-- 2. Beautiful System Push Message Card -->
              <div v-else-if="msg.sender === 'system_service'" class="system-push-card animate-pop">
                <div class="system-card-header">
                  <span class="system-card-icon">🔔</span>
                  <div class="system-card-meta">
                    <span class="system-card-title">{{ msg.senderName || '三方系统通知' }}</span>
                    <span class="system-card-time">{{ msg.time }}</span>
                  </div>
                  <span class="system-badge">外部 API 推送</span>
                </div>
                <div class="system-card-body">
                  {{ msg.content }}
                </div>
              </div>

              <!-- 3. Normal Message Bubble Row -->
              <div v-else class="msg-row">
                <div class="msg-avatar-circle" v-if="!msg.isSelf">{{ msg.sender[0].toUpperCase() }}</div>
                <div class="msg-bubble-content">
                  <span class="sender-name-label" v-if="!msg.isSelf && currentSession.type === 'group'">{{ msg.sender }}</span>
                  
                  <!-- Image rendering -->
                  <img v-if="msg.msgType === 2" :src="formatImageUrl(msg.content)" class="bubble-image-card" @click="previewImage(msg.content)" />
                  <!-- Text rendering -->
                  <span v-else :class="msg.content === '👍' ? 'giant-emoji-msg' : 'bubble-text-card'">{{ msg.content }}</span>
                </div>
              </div>

            </div>
            <div id="scroll-bottom"></div>
          </div>

          <!-- Bottom Input Tray (Facebook Messenger High-Fidelity Style) -->
          <div class="bottom-input-tray">
            <div class="tray-left-actions">
              <div class="tray-circle-btn" @click="showStartChat = true" title="新会话">➕</div>
              <div class="tray-circle-btn" @click="chooseImage" title="发送图片">📷</div>
              <div class="tray-circle-btn" @click="msgContent += '😀'" title="Emoji">😀</div>
            </div>
            <div class="messenger-input-wrapper">
              <input class="messenger-message-input" v-model="msgContent" placeholder="Aa" @keyup.enter="sendMsg" />
            </div>
            <div class="tray-right-actions">
              <button v-if="msgContent.trim()" class="messenger-send-btn" @click="sendMsg">
                <span class="send-arrow-icon">➔</span>
              </button>
              <div v-else class="messenger-thumbs-btn" @click="sendThumbsUp" title="发送点赞">👍</div>
            </div>
          </div>
        </div>

        <!-- Empty Chat View -->
        <div v-else class="chat-empty-fallback">
          <div class="fallback-glow-badge">💬</div>
          <span class="fallback-title">欢迎使用 govg-im Messenger</span>
          <span class="fallback-desc">点击左侧会话发起聊天，或者在“测试台”调用系统 API 模拟即时通讯。</span>
        </div>
      </div>

      <!-- 4. RIGHT CHAT DETAIL SIDEBAR (INFO PANEL) -->
      <div v-if="currentSession && showRightPanel" class="right-info-sidebar">
        <div class="full-height flex-col pad-20" style="overflow-y: auto;">
          <div class="right-profile-header">
            <div class="big-profile-avatar">{{ currentSession.name[0].toUpperCase() }}</div>
            <span class="right-profile-name">{{ currentSession.name }}</span>
            <span class="right-profile-id">ID: {{ currentSession.id }}</span>
          </div>

          <div class="right-divider"></div>

          <div class="right-section">
            <span class="right-section-title">⚙️ 会话操作</span>
            <div class="glass-menu-item" @click="clearHistory">🗑️ 清除聊天记录</div>
            <div class="glass-menu-item" @click="copySessionId">📋 复制会话 ID</div>
          </div>

          <div class="right-section" v-if="currentSession.type === 'group'">
            <span class="right-section-title">👥 群成员 (Mock)</span>
            <div class="member-list-mini">
              <div class="member-mini-card">
                <span class="member-mini-avatar">我</span>
                <span class="member-mini-name">我 (创建者)</span>
              </div>
            </div>
          </div>

          <div class="right-section">
            <span class="right-section-title">🖼️ 共享的图片媒体</span>
            <div class="shared-media-grid">
              <div v-for="(img, idx) in sharedImages" :key="idx" class="shared-img-card" @click="previewImage(img)">
                <img :src="img" class="shared-img-thumb" style="object-fit: cover;" />
              </div>
              <div v-if="sharedImages.length === 0" class="no-media-tip">暂无共享图片</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ================= START CHAT MODAL ================= -->
    <div v-if="showStartChat" class="modal-backdrop">
      <div class="modal-glass-card">
        <span class="modal-title">发起新聊天</span>
        <input class="mini-input" v-model="newChatId" placeholder="请输入目标用户 ID" @keyup.enter="startNewChat" />
        <div class="modal-actions-row">
          <button class="glass-btn primary-glow" @click="startNewChat">确认发起</button>
          <button class="glass-btn secondary-dark" @click="showStartChat = false">取消</button>
        </div>
      </div>
    </div>

    <!-- ================= GROUP MODAL ================= -->
    <div v-if="showCreateGroup" class="modal-backdrop">
      <div class="modal-glass-card">
        <span class="modal-title">群组加入与创建</span>
        <input class="mini-input" v-model="targetGroupId" placeholder="请输入群组 ID 加入" @keyup.enter="joinGroup" />
        <button class="glass-btn primary-glow margin-bottom-15" @click="joinGroup">加入该群组</button>
        <div class="modal-divider">或</div>
        <button class="glass-btn cyan-glow margin-bottom-15" @click="createGroup">创建一个新群组</button>
        <button class="glass-btn secondary-dark" @click="showCreateGroup = false">取消</button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      // Server Configuration
      serverHttpUrl: 'http://127.0.0.1:10085',
      serverWsUrl: 'ws://127.0.0.1:10087/ws',
      externalApiKey: 'your-default-secure-key',

      // Socket Connection
      socketTask: null,
      isConnected: false,

      // Auth Login States
      isLogin: false,
      loginMode: 'token', // 'token' or 'legacy'
      secureTokenInput: '', // Input token
      userIdInput: '', // Input legacy ID
      username: '', // Legacy username
      userId: '', // Actual Logged-in ID

      // Sidebar UI states
      activeTab: 'chats', // 'chats' | 'contacts' | 'console' | 'settings'
      view: 'list', // 'list' | 'chat' (for responsive mobile)
      showCreateGroup: false,
      showFabMenu: false,
      showStartChat: false,
      showRightPanel: false,

      // Data Models
      sessions: [], // { id, name, type, lastMsg, time, unread }
      messages: {}, // { sessionId: [ { sender, content, msgType, time, isSelf } ] }
      currentSession: null,

      // Simulated Platform Tools Data
      simTokenUserId: 'dev_user_777',
      simTokenUsername: '体验账号',
      generatedToken: '',

      simPushToUserId: '',
      simPushSender: '财务结算中心',
      simPushMsg: '【财务通知】您好，您申请的 320.00 元退款已原路打回原支付账户，请查收账单。',

      simCheckUserId: '',
      onlineCheckResult: '',

      // Input Box
      msgContent: '',
      targetGroupId: '',
      newChatId: '',

      // Scroll states
      scrollTop: 0,
      isAtBottom: true,
      newMsgCount: 0,

      // Hardcoded Friends List (Simulating contacts, status checkable)
      friendList: [
        { id: 'admin', name: '系统管理员', online: false, node: 'N/A' },
        { id: 'user1', name: '测试用户 1', online: false, node: 'N/A' },
        { id: 'user2', name: '测试用户 2', online: false, node: 'N/A' },
        { id: 'user3', name: '测试用户 3', online: false, node: 'N/A' }
      ],
      statusCheckTimer: null,
      currentTheme: 'light'
    }
  },
  computed: {
    currentMsgs() {
      if (!this.currentSession) return [];
      return this.messages[this.currentSession.id] || [];
    },
    totalUnread() {
      return this.sessions.reduce((sum, s) => sum + (s.unread || 0), 0);
    },
    sharedImages() {
      if (!this.currentSession) return [];
      const list = this.messages[this.currentSession.id] || [];
      return list.filter(m => m.msgType === 2).map(m => this.formatImageUrl(m.content));
    }
  },
  mounted() {
    // Restore theme cache
    const cachedTheme = uni.getStorageSync('im_theme');
    if (cachedTheme) {
      this.currentTheme = cachedTheme;
    } else {
      this.currentTheme = 'light';
    }

    // Restore Network settings
    const httpSetting = uni.getStorageSync('im_server_http');
    const wsSetting = uni.getStorageSync('im_server_ws');
    const keySetting = uni.getStorageSync('im_api_key');
    if (httpSetting) this.serverHttpUrl = httpSetting;
    if (wsSetting) this.serverWsUrl = wsSetting;
    if (keySetting) this.externalApiKey = keySetting;

    // Restore user profile cache
    const cachedUser = uni.getStorageSync('im_user_info');
    if (cachedUser) {
      this.userIdInput = cachedUser.userId;
      this.username = cachedUser.username;
    } else {
      this.userIdInput = 'user' + Math.floor(Math.random() * 1000);
      this.username = '访客' + this.userIdInput.substring(4);
    }

    // Restore chat logs
    try {
      const cachedSessions = uni.getStorageSync('im_sessions');
      const cachedMessages = uni.getStorageSync('im_messages');
      if (cachedSessions) this.sessions = cachedSessions;
      if (cachedMessages) this.messages = cachedMessages;
    } catch (e) {
      console.error('Failed to load local DB caches', e);
    }

    this.connectSocket();

    // Auto query online status for friends periodically
    this.startFriendStatusScanner();
  },
  beforeUnmount() {
    if (this.socketTask) {
      this.socketTask.close();
    }
    this.stopFriendStatusScanner();
  },
  methods: {
    // Save network settings
    saveSettings() {
      uni.setStorageSync('im_server_http', this.serverHttpUrl);
      uni.setStorageSync('im_server_ws', this.serverWsUrl);
      uni.setStorageSync('im_api_key', this.externalApiKey);
      uni.showToast({ title: '配置保存成功，稍后生效！', icon: 'success' });
    },

    // Scroller tracking
    onScroll(e) {
      const scrollEl = e.target;
      const viewHeight = scrollEl.clientHeight;
      const scrollHeight = scrollEl.scrollHeight;
      const scrollTop = scrollEl.scrollTop;
      if (scrollHeight - scrollTop - viewHeight < 60) {
        this.isAtBottom = true;
        this.newMsgCount = 0;
      } else {
        this.isAtBottom = false;
      }
    },

    formatImageUrl(url) {
      if (url.startsWith('/')) {
        return this.serverHttpUrl + url;
      }
      return url;
    },

    saveData() {
      uni.setStorage({ key: 'im_sessions', data: this.sessions });
      uni.setStorage({ key: 'im_messages', data: this.messages });
    },

    formatTime(date) {
      return date.getHours() + ':' + (date.getMinutes() < 10 ? '0' : '') + date.getMinutes();
    },

    shouldShowTime(index) {
      if (index === 0) return true;
      return index % 5 === 0;
    },

    copyId() {
      if (!this.userId) return;
      uni.setClipboardData({
        data: this.userId,
        success: () => uni.showToast({ title: '已复制您的用户 ID', icon: 'none' })
      });
    },

    copySessionId() {
      if (!this.currentSession) return;
      uni.setClipboardData({
        data: this.currentSession.id,
        success: () => uni.showToast({ title: '已复制会话 ID', icon: 'none' })
      });
    },

    // Connection socket
    connectSocket() {
      let url = this.serverWsUrl;
      console.log('Connecting to WebSocket server: ' + url);

      this.socketTask = uni.connectSocket({
        url: url,
        success: () => console.log('Socket initialized: ' + url),
        fail: (err) => console.error('Socket init failed: ' + JSON.stringify(err))
      });

      if (!this.socketTask) return;

      this.socketTask.onOpen((res) => {
        this.isConnected = true;
        if (this.userId) {
          this.send({
            command: 1,
            userId: this.userId,
            username: this.username || this.userId
          });
        }
      });

      this.socketTask.onMessage((res) => {
        this.handleMessage(res.data);
      });

      this.socketTask.onClose(() => {
        this.isConnected = false;
        this.isLogin = false;
        setTimeout(() => this.connectSocket(), 3000);
      });
    },

    handleMessage(data) {
      const packet = JSON.parse(data);

      if (packet.command === undefined) {
        if (packet.success !== undefined && packet.userId) packet.command = 2;
        else if (packet.msgId && packet.fromUserId === 'SERVER') packet.command = 13;
        else if (packet.message && packet.fromUserId && !packet.fromGroupId) packet.command = 4;
        else if (packet.message && packet.fromGroupId) packet.command = 12;
      }

      if (packet.command === 2) { // LOGIN_RESPONSE
        if (packet.success) {
          this.isLogin = true;
          this.userId = packet.userId;
          if (packet.username) this.username = packet.username;
          
          this.simPushToUserId = this.userId;
          
          uni.showToast({ title: '成功安全接入 IM 系统', icon: 'success' });
          
          // Auto-select last active session to avoid initial blank screen
          setTimeout(() => {
            if (this.sessions && this.sessions.length > 0) {
              this.openChat(this.sessions[0]);
            }
          }, 300);
        } else {
          uni.showModal({
            title: '连接拒绝',
            content: packet.reason || '登录凭证无效或过期',
            showCancel: false
          });
        }
      }
      else if (packet.command === 4) { // MESSAGE_RESPONSE
        const sessionId = packet.fromUserId;
        const sessionName = packet.fromUserName || packet.fromUserId;
        this.onReceiveMessage(sessionId, sessionName, 'user', packet.message, packet.fromUserId, packet.msgType, packet.fromUserName);
        this.sendAck(packet.msgId);
      }
      else if (packet.command === 12) { // GROUP_MESSAGE_RESPONSE
        const sessionId = 'g:' + packet.fromGroupId;
        const senderName = packet.fromUser;
        this.onReceiveMessage(sessionId, `群组 ${packet.fromGroupId}`, 'group', packet.message, senderName, packet.msgType);
      }
      else if (packet.command === 8) { // CREATE_GROUP_RESPONSE
        if (packet.success) {
          uni.showToast({ title: '群组创建成功: ' + packet.groupId, icon: 'success' });
          const sessionId = 'g:' + packet.groupId;
          this.startSession(sessionId, '群组 ' + packet.groupId, 'group');
          
          if (!this.messages[sessionId]) this.messages[sessionId] = [];
          this.messages[sessionId].push({
            sender: 'system_notification',
            senderName: '系统通知',
            content: `🎉 您成功创建了群组 ${packet.groupId}！复制 ID 邀请好友加入吧。`,
            msgType: 1,
            time: this.formatTime(new Date()),
            isSelf: false
          });
          this.saveData();
          this.showCreateGroup = false;
        }
      }
      else if (packet.command === 10) { // JOIN_GROUP_RESPONSE
        if (packet.success) {
          uni.showToast({ title: '已加入群组 ' + packet.groupId, icon: 'success' });
          const sessionId = 'g:' + packet.groupId;
          this.startSession(sessionId, '群组 ' + packet.groupId, 'group');
          
          if (!this.messages[sessionId]) this.messages[sessionId] = [];
          this.messages[sessionId].push({
            sender: 'system_notification',
            senderName: '系统通知',
            content: `👋 您已成功加入群组 ${packet.groupId}。开始聊天吧！`,
            msgType: 1,
            time: this.formatTime(new Date()),
            isSelf: false
          });
          this.saveData();
          this.showCreateGroup = false;
        }
      }
    },

    onReceiveMessage(sessionId, sessionName, type, content, senderName, msgType = 1, fromUserName = null) {
      if (!this.messages[sessionId]) {
        this.messages[sessionId] = [];
      }
      this.messages[sessionId].push({
        sender: senderName,
        senderName: fromUserName,
        content: content,
        msgType: msgType,
        time: this.formatTime(new Date()),
        isSelf: false
      });

      let session = this.sessions.find(s => s.id === sessionId);
      if (!session) {
        session = {
          id: sessionId,
          name: sessionName,
          type: type,
          lastMsg: '',
          time: '',
          unread: 0
        };
        this.sessions.unshift(session);
      }

      session.lastMsg = msgType === 2 ? '[图片]' : content;
      session.time = this.formatTime(new Date());

      if (!this.currentSession || this.currentSession.id !== sessionId) {
        session.unread++;
      } else {
        if (this.isAtBottom) {
          setTimeout(() => this.scrollToBottom(), 50);
        } else {
          this.newMsgCount++;
        }
      }
      this.saveData();
    },

    startSession(id, name, type) {
      let session = this.sessions.find(s => s.id === id);
      if (!session) {
        session = {
          id: id,
          name: name,
          type: type,
          lastMsg: '新会话',
          time: this.formatTime(new Date()),
          unread: 0
        };
        this.sessions.unshift(session);
        this.messages[id] = [];
      }
      this.openChat(session);
      this.saveData();
    },

    openChat(session) {
      this.currentSession = session;
      session.unread = 0;
      this.view = 'chat';
      this.scrollToBottom();
    },

    startNewChat() {
      if (!this.newChatId) return;
      this.startSession(this.newChatId.trim(), this.newChatId.trim(), 'user');
      this.showStartChat = false;
      this.newChatId = '';
    },

    startChatWith(userId) {
      this.startSession(userId, userId, 'user');
      this.activeTab = 'chats';
    },

    login() {
      if (!this.isConnected) {
        uni.showToast({ title: '正在连接服务器，请稍后...', icon: 'none' });
        this.connectSocket();
        return;
      }

      if (this.loginMode === 'token') {
        if (!this.secureTokenInput) {
          uni.showToast({ title: '请输入登录 Token', icon: 'none' });
          return;
        }
        this.send({
          command: 1,
          token: this.secureTokenInput.trim()
        });
      } else {
        if (!this.userIdInput) {
          uni.showToast({ title: '请输入用户 ID', icon: 'none' });
          return;
        }
        this.send({
          command: 1,
          userId: this.userIdInput.trim(),
          username: this.username || this.userIdInput
        });

        uni.setStorageSync('im_user_info', {
          userId: this.userIdInput,
          username: this.username || this.userIdInput
        });
      }
    },

    logout() {
      uni.showModal({
        title: '注销登录',
        content: '确认断开并退出当前通讯系统吗？',
        success: (res) => {
          if (res.confirm) {
            if (this.socketTask) this.socketTask.close();
            this.isLogin = false;
            this.userId = '';
            this.secureTokenInput = '';
            uni.showToast({ title: '已注销网络通道' });
          }
        }
      });
    },

    sendMsg() {
      if (!this.msgContent) return;
      this.doSend(this.msgContent, 1);
      this.msgContent = '';
    },

    chooseImage() {
      uni.chooseImage({
        count: 1,
        success: (res) => {
          const tempFilePaths = res.tempFilePaths;
          uni.showLoading({ title: '图片上传中...' });
          
          uni.uploadFile({
            url: this.serverHttpUrl + '/api/upload',
            filePath: tempFilePaths[0],
            name: 'file',
            success: (uploadRes) => {
              uni.hideLoading();
              try {
                const data = JSON.parse(uploadRes.data);
                if (data.success) {
                  this.doSend(data.url, 2);
                } else {
                  uni.showToast({ title: '上传失败: ' + (data.message || 'Error'), icon: 'none' });
                }
              } catch (e) {
                uni.showToast({ title: '解析上传数据错误', icon: 'none' });
              }
            },
            fail: () => {
              uni.hideLoading();
              uni.showToast({ title: '网络上传错误', icon: 'none' });
            }
          });
        }
      });
    },

    doSend(content, msgType) {
      const session = this.currentSession;
      const isGroup = session.type === 'group';
      const targetId = isGroup ? session.id.substring(2) : session.id;

      const msgId = 'msg-' + Date.now();
      const packet = {
        command: isGroup ? 11 : 3,
        [isGroup ? 'toGroupId' : 'toUserId']: targetId,
        message: content,
        msgId: msgId,
        msgType: msgType
      };

      this.send(packet);

      if (!this.messages[session.id]) {
        this.messages[session.id] = [];
      }
      this.messages[session.id].push({
        sender: '我',
        content: content,
        msgType: msgType,
        time: this.formatTime(new Date()),
        isSelf: true
      });

      session.lastMsg = msgType === 2 ? '[图片]' : content;
      session.time = this.formatTime(new Date());
      this.saveData();

      setTimeout(() => this.scrollToBottom(), 50);
    },

    previewImage(url) {
      uni.previewImage({
        urls: [url]
      });
    },

    createGroup() {
      this.send({ command: 7, userIdList: [] });
    },

    joinGroup() {
      if (!this.targetGroupId) return;
      this.send({ command: 9, groupId: this.targetGroupId });
    },

    sendAck(msgId) {
      this.send({ command: 13, msgId: msgId, fromUserId: this.userId });
    },

    send(packet) {
      if (this.socketTask && this.isConnected) {
        this.socketTask.send({ data: JSON.stringify(packet) });
      }
    },

    scrollToBottom() {
      this.newMsgCount = 0;
      this.isAtBottom = true;
      this.$nextTick(() => {
        const scroller = document.querySelector('.chat-messages-scroller');
        if (scroller) {
          scroller.scrollTop = scroller.scrollHeight;
        }
      });
    },

    clearHistory() {
      uni.showModal({
        title: '清除聊天',
        content: '确定要清空与当前用户的聊天记录吗？',
        success: (res) => {
          if (res.confirm) {
            this.messages[this.currentSession.id] = [];
            const session = this.sessions.find(s => s.id === this.currentSession.id);
            if (session) session.lastMsg = '聊天记录已清空';
            this.saveData();
          }
        }
      });
    },

    quickGenerateLoginToken() {
      const randId = 'user_' + Math.floor(Math.random() * 10000);
      const randName = '快捷访客_' + randId.substring(5);
      uni.showLoading({ title: '正在向后端申请测试 Token...' });
      uni.request({
        url: this.serverHttpUrl + '/api/external/auth/token',
        method: 'POST',
        header: {
          'Authorization': 'Bearer ' + this.externalApiKey,
          'Content-Type': 'application/json'
        },
        data: {
          userId: randId,
          username: randName
        },
        success: (res) => {
          uni.hideLoading();
          if (res.statusCode === 200 && res.data.success) {
            this.secureTokenInput = res.data.token;
            uni.showToast({ title: '已自动填入测试 Token！', icon: 'success' });
          } else {
            uni.showModal({
              title: '生成失败',
              content: res.data.message || '请检查后端服务是否启动以及 API Key 配置',
              showCancel: false
            });
          }
        },
        fail: () => {
          uni.hideLoading();
          uni.showToast({ title: '无法连接到 IM 服务，请检查后端是否运行', icon: 'none' });
        }
      });
    },

    // ==========================================
    // 🛠️ DEVELOPER TESTING CONSOLE METHODS 🛠️
    // ==========================================

    simGenerateToken() {
      if (!this.simTokenUserId) {
        uni.showToast({ title: '请输入生成 Token 的用户 ID', icon: 'none' });
        return;
      }
      uni.request({
        url: this.serverHttpUrl + '/api/external/auth/token',
        method: 'POST',
        header: {
          'Authorization': 'Bearer ' + this.externalApiKey,
          'Content-Type': 'application/json'
        },
        data: {
          userId: this.simTokenUserId,
          username: this.simTokenUsername
        },
        success: (res) => {
          if (res.statusCode === 200 && res.data.success) {
            this.generatedToken = res.data.token;
            uni.showToast({ title: 'Token 申请成功！', icon: 'success' });
          } else {
            uni.showModal({
              title: '请求失败',
              content: res.data.message || '请检查 API Key 配置是否正确',
              showCancel: false
            });
          }
        },
        fail: (err) => {
          uni.showToast({ title: '无法连接到 REST 服务器', icon: 'none' });
        }
      });
    },

    copyAndUseToken() {
      if (!this.generatedToken) return;
      uni.setClipboardData({
        data: this.generatedToken,
        success: () => {
          uni.showToast({ title: '已复制，自动填入输入框', icon: 'success' });
          this.secureTokenInput = this.generatedToken;
          this.loginMode = 'token';
          if (this.isLogin) {
            this.logout(); // trigger logout to let user re-login
          }
        }
      });
    },

    simTriggerPush() {
      const toId = this.simPushToUserId || this.userId;
      if (!toId) {
        uni.showToast({ title: '请指定推送的目标用户 ID', icon: 'none' });
        return;
      }
      if (!this.simPushMsg) {
        uni.showToast({ title: '请输入推送卡片的内容', icon: 'none' });
        return;
      }

      uni.request({
        url: this.serverHttpUrl + '/api/external/message/send',
        method: 'POST',
        header: {
          'Authorization': 'Bearer ' + this.externalApiKey,
          'Content-Type': 'application/json'
        },
        data: {
          toUserId: toId,
          fromUserId: 'system_service',
          fromUserName: this.simPushSender || '三方推送中心',
          message: this.simPushMsg,
          msgType: 1
        },
        success: (res) => {
          if (res.statusCode === 200 && res.data.success) {
            uni.showToast({ title: '推送指令成功发送！', icon: 'success' });
          } else {
            uni.showModal({
              title: '推送指令失败',
              content: res.data.message || '请检查 API Key',
              showCancel: false
            });
          }
        },
        fail: () => {
          uni.showToast({ title: '发送失败，网络错误', icon: 'none' });
        }
      });
    },

    simCheckOnline() {
      if (!this.simCheckUserId) {
        uni.showToast({ title: '请输入要查询的用户 ID', icon: 'none' });
        return;
      }

      uni.request({
        url: this.serverHttpUrl + '/api/external/user/status?userId=' + this.simCheckUserId,
        method: 'GET',
        header: {
          'Authorization': 'Bearer ' + this.externalApiKey
        },
        success: (res) => {
          this.onlineCheckResult = JSON.stringify(res.data, null, 2);
        },
        fail: () => {
          this.onlineCheckResult = "网络连结失败！";
        }
      });
    },

    startFriendStatusScanner() {
      this.scanFriendsOnlineStatus();
      this.statusCheckTimer = setInterval(() => {
        this.scanFriendsOnlineStatus();
      }, 10000);
    },

    stopFriendStatusScanner() {
      if (this.statusCheckTimer) {
        clearInterval(this.statusCheckTimer);
        this.statusCheckTimer = null;
      }
    },

    scanFriendsOnlineStatus() {
      this.friendList.forEach(friend => {
        uni.request({
          url: this.serverHttpUrl + '/api/external/user/status?userId=' + friend.id,
          method: 'GET',
          header: {
            'Authorization': 'Bearer ' + this.externalApiKey
          },
          success: (res) => {
            if (res.statusCode === 200 && res.data.success) {
              friend.online = res.data.online;
              friend.node = res.data.server || 'N/A';
            }
          }
        });
      });
    },

    toggleTheme() {
      this.currentTheme = this.currentTheme === 'dark' ? 'light' : 'dark';
      uni.setStorageSync('im_theme', this.currentTheme);
    },
    setTheme(theme) {
      this.currentTheme = theme;
      uni.setStorageSync('im_theme', theme);
    },
    sendThumbsUp() {
      this.doSend('👍', 1);
    }
  }
}
</script>

<style>
  /* ==========================================
     FACEBOOK MESSENGER PREMIUM DESIGN SYSTEM
     ========================================== */

  /* Global scrollbar styling - premium sleek minimalist */
  ::-webkit-scrollbar {
    width: 6px;
    height: 6px;
  }
  ::-webkit-scrollbar-track {
    background: transparent;
  }
  ::-webkit-scrollbar-thumb {
    background: var(--scroll-thumb);
    border-radius: 4px;
  }
  ::-webkit-scrollbar-thumb:hover {
    background: var(--text-desc);
  }

  /* Reset & Container setup */
  #app, page {
    margin: 0;
    padding: 0;
    height: 100%;
    width: 100%;
    background-color: var(--bg-app);
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    overflow: hidden;
    transition: background-color 0.3s ease;
  }

  .app-container {
    position: relative;
    width: 100vw;
    height: 100vh;
    background-color: var(--bg-app);
    color: var(--text-primary);
    overflow: hidden;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: background-color 0.3s ease;
  }

  /* Hide decorative glow-orbs in clean solid design */
  .glow-orb {
    display: none !important;
  }

  /* Core Palette Tokens (Light / Dark) */
  .app-container.theme-light {
    --bg-app: #f0f2f5;
    --bg-board: #ffffff;
    --bg-sidebar: #ffffff;
    --bg-middle: #ffffff;
    --bg-chat-viewport: #ffffff;
    --bg-chat-header: #ffffff;
    --border-color: #e4e6eb;
    --text-title: #050505;
    --text-desc: #65676b;
    --text-primary: #050505;
    --input-bg: #f0f2f5;
    --input-placeholder: #8a8d91;
    --msg-self-bg: #0084ff;
    --msg-self-text: #ffffff;
    --msg-other-bg: #e4e6eb;
    --msg-other-text: #050505;
    --msg-time: #bcc0c4;
    --scroll-thumb: #bcc0c4;
    --active-blue: #0084ff;
    --active-green: #31a24c;
    --btn-hover: #f2f2f2;
    --btn-active: #e4e6eb;
    --accent-cyan: #00a400;
    --terminal-bg: #f0f2f5;
    --terminal-text: #050505;
    --shadow-premium: 0 12px 28px rgba(0, 0, 0, 0.08), 0 8px 16px rgba(0, 0, 0, 0.04);
    --shadow-modal: 0 24px 64px rgba(0, 0, 0, 0.2);
  }

  .app-container.theme-dark {
    --bg-app: #09090b;
    --bg-board: #242526;
    --bg-sidebar: #18191a;
    --bg-middle: #18191a;
    --bg-chat-viewport: #242526;
    --bg-chat-header: #242526;
    --border-color: #3e4042;
    --text-title: #e4e6eb;
    --text-desc: #b0b3b8;
    --text-primary: #e4e6eb;
    --input-bg: #3a3b3c;
    --input-placeholder: #b0b3b8;
    --msg-self-bg: #0084ff;
    --msg-self-text: #ffffff;
    --msg-other-bg: #3e4042;
    --msg-other-text: #e4e6eb;
    --msg-time: #8a8d91;
    --scroll-thumb: #4e4f50;
    --active-blue: #0084ff;
    --active-green: #31a24c;
    --btn-hover: #3a3b3c;
    --btn-active: #4e4f50;
    --accent-cyan: #26c281;
    --terminal-bg: #18191a;
    --terminal-text: #e4e6eb;
    --shadow-premium: 0 12px 28px rgba(0, 0, 0, 0.25), 0 8px 16px rgba(0, 0, 0, 0.15);
    --shadow-modal: 0 24px 64px rgba(0, 0, 0, 0.45);
  }

  /* Utility classes */
  .font-bold { font-weight: bold; }
  .pad-15 { padding: 15px; }
  .pad-20 { padding: 20px; }
  .full-height { height: 100%; }
  .flex-col { display: flex; flex-direction: column; }
  .text-white { color: #ffffff !important; }
  .text-green { color: var(--active-green) !important; }
  .text-red { color: #f3565d !important; }
  .margin-bottom-15 { margin-bottom: 15px; }
  .margin-top-15 { margin-top: 15px; }

  @keyframes scaleUpPop {
    0% { transform: scale(0.6); opacity: 0; }
    50% { transform: scale(1.1); }
    100% { transform: scale(1); opacity: 1; }
  }

  @keyframes slideUp {
    0% { transform: translateY(15px); opacity: 0; }
    100% { transform: translateY(0); opacity: 1; }
  }

  .animate-pop {
    animation: scaleUpPop 0.28s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
  }

  /* MESSENGER LOGIN SCREEN STYLING */
  .login-wrapper {
    z-index: 10;
    width: 90%;
    max-width: 440px;
    animation: slideUp 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  }

  .login-glass-card {
    background: var(--bg-board);
    border: 1px solid var(--border-color);
    border-radius: 16px;
    padding: 40px 30px;
    box-shadow: var(--shadow-premium);
    display: flex;
    flex-direction: column;
    transition: background-color 0.3s ease, border-color 0.3s ease;
  }

  .login-header {
    text-align: center;
    margin-bottom: 30px;
  }

  .logo-badge {
    font-size: 56px;
    margin-bottom: 15px;
    display: inline-block;
    filter: drop-shadow(0 4px 8px rgba(0, 132, 255, 0.2));
  }

  .login-title {
    display: block;
    font-size: 24px;
    font-weight: 800;
    color: var(--text-title);
    margin-bottom: 10px;
    letter-spacing: -0.5px;
  }

  .login-subtitle {
    display: block;
    font-size: 14px;
    color: var(--text-desc);
    line-height: 1.5;
  }

  .login-tab-header {
    display: flex;
    background: var(--input-bg);
    border-radius: 10px;
    padding: 3px;
    margin-bottom: 25px;
  }

  .login-tab {
    flex: 1;
    text-align: center;
    font-size: 14px;
    padding: 10px;
    color: var(--text-desc);
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.2s ease;
    font-weight: 500;
  }

  .login-tab.active {
    background: var(--bg-board);
    color: var(--text-title);
    font-weight: 600;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
  }

  .login-form {
    display: flex;
    flex-direction: column;
    margin-bottom: 20px;
  }

  .form-fade {
    animation: slideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  }

  .glass-input-group {
    display: flex;
    align-items: center;
    background: var(--input-bg);
    border: 1px solid transparent;
    border-radius: 10px;
    padding: 0 16px;
    height: 48px;
    margin-bottom: 16px;
    transition: all 0.2s ease;
  }
  .glass-input-group:focus-within {
    border-color: var(--active-blue);
    background: var(--bg-board);
    box-shadow: 0 0 0 3px rgba(0, 132, 255, 0.15);
  }

  .input-icon {
    font-size: 18px;
    margin-right: 12px;
    color: var(--text-desc);
  }

  .glass-input {
    flex: 1;
    background: transparent;
    border: none;
    outline: none;
    color: var(--text-title);
    font-size: 15px;
    width: 100%;
  }
  .glass-input::placeholder {
    color: var(--input-placeholder);
  }

  .input-tip-box {
    font-size: 12px;
    color: var(--text-desc);
    line-height: 1.5;
    margin-bottom: 20px;
    padding: 0 4px;
  }

  .quick-generate-link {
    color: var(--active-blue);
    text-decoration: none;
    font-weight: 600;
    cursor: pointer;
  }
  .quick-generate-link:hover {
    text-decoration: underline;
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

  .connection-status-bar {
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    color: var(--text-desc);
    gap: 8px;
    margin-top: 10px;
  }
  .connection-status-bar.active {
    color: var(--active-green);
  }
  .status-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #bcc0c4;
    display: inline-block;
  }
  .connection-status-bar.active .status-dot {
    background: var(--active-green);
    box-shadow: 0 0 8px var(--active-green);
  }

  /* MAIN APPLICATION WORKSPACE BOARD */
  .app-glass-board {
    width: 94vw;
    height: 92vh;
    max-width: 1440px;
    max-height: 900px;
    background: var(--bg-board);
    border: 1px solid var(--border-color);
    border-radius: 16px;
    box-shadow: var(--shadow-premium);
    display: flex;
    overflow: hidden;
    z-index: 10;
    transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  }

  /* 1. NARROW SIDEBAR (LEFT NAVIGATION PANEL) */
  .nav-sidebar {
    width: 76px;
    background: var(--bg-sidebar);
    border-right: 1px solid var(--border-color);
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    align-items: center;
    padding: 20px 0;
    box-sizing: border-box;
    flex-shrink: 0;
    transition: background-color 0.3s ease;
  }

  .sidebar-top-group, .sidebar-bottom-group {
    width: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 20px;
  }

  .user-avatar-wrapper {
    position: relative;
    cursor: pointer;
    transition: transform 0.2s ease;
  }
  .user-avatar-wrapper:hover {
    transform: scale(1.05);
  }

  .user-avatar-gradient {
    width: 44px;
    height: 44px;
    border-radius: 50%;
    background: linear-gradient(135deg, var(--active-blue) 0%, #7e22ce 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    color: #ffffff;
    font-weight: 700;
    font-size: 16px;
    border: 2px solid var(--bg-sidebar);
  }

  .online-indicator {
    position: absolute;
    bottom: -1px;
    right: -1px;
    width: 13px;
    height: 13px;
    border-radius: 50%;
    background: var(--active-green);
    border: 2px solid var(--bg-sidebar);
  }

  .nav-menu {
    width: 100%;
    display: flex;
    flex-direction: column;
    gap: 8px;
    align-items: center;
  }

  .nav-item {
    width: 48px;
    height: 48px;
    border-radius: 50%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    position: relative;
    transition: all 0.2s ease;
    color: var(--text-desc);
  }
  .nav-item:hover {
    background: var(--btn-hover);
    color: var(--text-title);
  }
  .nav-item.active {
    background: var(--btn-hover);
    color: var(--active-blue);
  }

  .nav-icon {
    font-size: 22px;
  }
  .nav-label {
    display: none; /* Hide descriptions to look modern */
  }

  .unread-badge-mini {
    position: absolute;
    top: 2px;
    right: 2px;
    background: #f3565d;
    color: #ffffff;
    font-size: 11px;
    font-weight: bold;
    min-width: 16px;
    height: 16px;
    line-height: 16px;
    text-align: center;
    border-radius: 10px;
    padding: 0 4px;
    border: 2px solid var(--bg-sidebar);
  }

  /* Sidebar bottom tools */
  .theme-toggle-sidebar-btn, .logout-btn {
    width: 44px;
    height: 44px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    font-size: 20px;
    color: var(--text-desc);
    transition: all 0.2s ease;
  }
  .theme-toggle-sidebar-btn:hover, .logout-btn:hover {
    background: var(--btn-hover);
    color: var(--text-title);
    transform: rotate(15deg);
  }

  /* 2. MIDDLE SIDEBAR (SESSION LIST AREA) */
  .middle-sidebar {
    width: 360px;
    background: var(--bg-middle);
    border-right: 1px solid var(--border-color);
    display: flex;
    flex-direction: column;
    flex-shrink: 0;
    box-sizing: border-box;
    transition: background-color 0.3s ease, border-color 0.3s ease;
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

  .header-actions {
    display: flex;
    gap: 8px;
  }

  .circle-action-btn {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background: var(--btn-hover);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 16px;
    color: var(--text-title);
    cursor: pointer;
    transition: all 0.2s ease;
  }
  .circle-action-btn:hover {
    background: var(--btn-active);
  }

  .search-box {
    padding: 4px 20px 12px 20px;
  }

  .glass-search {
    width: 100%;
    height: 36px;
    border-radius: 20px;
    background: var(--input-bg);
    border: none;
    outline: none;
    padding: 0 16px;
    color: var(--text-title);
    font-size: 14px;
    box-sizing: border-box;
  }
  .glass-search::placeholder {
    color: var(--input-placeholder);
  }

  /* Facebook Messenger horizontal scroll bar: Active Friends */
  .active-friends-rail {
    display: flex;
    gap: 16px;
    padding: 4px 20px 16px 20px;
    overflow-x: auto;
    border-bottom: 1px solid var(--border-color);
    margin-bottom: 8px;
    flex-shrink: 0;
  }
  .active-friends-rail::-webkit-scrollbar {
    display: none;
  }

  .active-friend-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
    cursor: pointer;
    width: 56px;
    flex-shrink: 0;
  }

  .active-friend-avatar {
    position: relative;
    width: 48px;
    height: 48px;
    border-radius: 50%;
    background: var(--btn-hover);
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: bold;
    color: var(--active-blue);
    font-size: 16px;
    transition: transform 0.2s ease;
  }
  .active-friend-item:hover .active-friend-avatar {
    transform: scale(1.04);
  }

  .friend-letter {
    text-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  }

  .online-indicator-dot {
    position: absolute;
    bottom: 2px;
    right: 2px;
    width: 11px;
    height: 11px;
    border-radius: 50%;
    background: #bcc0c4;
    border: 2px solid var(--bg-middle);
  }
  .online-indicator-dot.active {
    background: var(--active-green);
  }

  .active-friend-name {
    font-size: 12px;
    color: var(--text-desc);
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    text-align: center;
  }

  .list-scroll-view {
    flex: 1;
    overflow-y: auto;
  }

  .empty-list-tip {
    text-align: center;
    color: var(--text-desc);
    font-size: 14px;
    padding: 40px 20px;
  }

  /* Session Card List */
  .session-glass-card {
    display: flex;
    align-items: center;
    padding: 12px 16px;
    margin: 0 8px;
    border-radius: 10px;
    cursor: pointer;
    transition: all 0.2s ease;
    gap: 12px;
  }
  .session-glass-card:hover {
    background: var(--btn-hover);
  }
  .session-glass-card.active {
    background: var(--btn-hover);
  }

  .avatar-circle {
    width: 48px;
    height: 48px;
    border-radius: 50%;
    background: linear-gradient(135deg, #0084ff 0%, #00c6ff 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
    font-weight: bold;
    color: #ffffff;
    position: relative;
    flex-shrink: 0;
  }

  .session-info {
    flex: 1;
    min-width: 0;
  }

  .session-header-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 4px;
  }

  .session-name {
    font-size: 15px;
    font-weight: 600;
    color: var(--text-title);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .session-time {
    font-size: 12px;
    color: var(--text-desc);
  }

  .session-body-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .session-preview {
    font-size: 13px;
    color: var(--text-desc);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    flex: 1;
    margin-right: 8px;
  }

  .unread-bubble {
    background: var(--active-blue);
    color: #ffffff;
    font-size: 11px;
    font-weight: bold;
    min-width: 18px;
    height: 18px;
    line-height: 18px;
    border-radius: 9px;
    text-align: center;
    padding: 0 4px;
    box-sizing: border-box;
  }

  /* Contact Tab Styles */
  .contact-section-title {
    font-size: 12px;
    font-weight: bold;
    color: var(--text-desc);
    text-transform: uppercase;
    padding: 16px 20px 8px 20px;
    letter-spacing: 0.5px;
  }

  .contact-glass-card {
    display: flex;
    align-items: center;
    padding: 12px 16px;
    margin: 0 8px;
    border-radius: 10px;
    cursor: pointer;
    transition: all 0.2s ease;
    gap: 12px;
  }
  .contact-glass-card:hover {
    background: var(--btn-hover);
  }

  .contact-info {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .contact-name {
    font-size: 15px;
    font-weight: 600;
    color: var(--text-title);
  }

  .contact-status {
    font-size: 12px;
    color: var(--text-desc);
  }
  .contact-status.online {
    color: var(--active-green);
  }

  .quick-chat-action {
    font-size: 16px;
    color: var(--text-desc);
    opacity: 0;
    transition: opacity 0.2s ease;
  }
  .contact-glass-card:hover .quick-chat-action {
    opacity: 1;
  }

  /* Developer console cards inside standard panels */
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

  /* 3. RIGHT MAIN CHAT VIEWPORT PANEL */
  .chat-viewport {
    flex: 1;
    background: var(--bg-chat-viewport);
    display: flex;
    flex-direction: column;
    position: relative;
    min-width: 0;
    transition: background-color 0.3s ease;
  }

  .chat-frame {
    display: flex;
    flex-direction: column;
    height: 100%;
    min-width: 0;
  }

  .chat-header-bar {
    height: 72px;
    border-bottom: 1px solid var(--border-color);
    background: var(--bg-chat-header);
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 24px;
    flex-shrink: 0;
    transition: background-color 0.3s ease, border-color 0.3s ease;
  }

  .back-action-btn {
    display: none;
    align-items: center;
    gap: 6px;
    cursor: pointer;
    font-weight: 600;
    color: var(--active-blue);
    font-size: 15px;
    margin-right: 12px;
  }

  .header-user-info {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }

  .header-user-name {
    font-size: 17px;
    font-weight: 700;
    color: var(--text-title);
  }

  .header-user-status {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 13px;
    color: var(--text-desc);
  }
  .header-user-status .status-dot {
    width: 8px;
    height: 8px;
    background: var(--active-green);
  }

  .header-icon-btn {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background: var(--btn-hover);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 16px;
    cursor: pointer;
    color: var(--active-blue);
    transition: all 0.2s ease;
  }
  .header-icon-btn:hover {
    background: var(--btn-active);
  }

  /* Message List Scroller Area */
  .chat-messages-scroller {
    flex: 1;
    overflow-y: auto;
    padding: 20px 24px;
    display: flex;
    flex-direction: column;
    gap: 6px;
    box-sizing: border-box;
  }

  .msg-bubble-wrapper {
    display: flex;
    flex-direction: column;
    max-width: 65%;
    align-self: flex-start;
    animation: slideUp 0.25s cubic-bezier(0.16, 1, 0.3, 1) forwards;
  }
  .msg-bubble-wrapper.self-msg {
    align-self: flex-end;
  }

  .msg-time-divider {
    align-self: center;
    font-size: 12px;
    font-weight: 500;
    color: var(--msg-time);
    margin: 16px 0 8px 0;
    text-align: center;
    width: 100%;
  }

  .msg-row {
    display: flex;
    align-items: flex-end;
    gap: 8px;
  }
  .self-msg .msg-row {
    flex-direction: row-reverse;
  }

  .msg-avatar-circle {
    width: 28px;
    height: 28px;
    border-radius: 50%;
    background: var(--btn-hover);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 11px;
    font-weight: bold;
    color: var(--active-blue);
    flex-shrink: 0;
    margin-bottom: 2px;
  }

  .msg-bubble-content {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .sender-name-label {
    font-size: 11px;
    color: var(--text-desc);
    margin-left: 12px;
    margin-bottom: 2px;
  }

  /* Bubble design - capsule style with rich text rules */
  .bubble-text-card {
    background: var(--msg-other-bg);
    color: var(--msg-other-text);
    padding: 10px 16px;
    border-radius: 18px;
    font-size: 15px;
    line-height: 1.4;
    word-break: break-word;
    box-shadow: 0 1px 2px rgba(0,0,0,0.02);
    display: inline-block;
  }

  .self-msg .bubble-text-card {
    background: var(--msg-self-bg);
    color: var(--msg-self-text);
    box-shadow: 0 1px 3px rgba(0, 132, 255, 0.15);
  }

  .bubble-image-card {
    max-width: 240px;
    max-height: 180px;
    border-radius: 12px;
    cursor: pointer;
    box-shadow: 0 2px 8px rgba(0,0,0,0.1);
    transition: transform 0.2s ease;
  }
  .bubble-image-card:hover {
    transform: scale(1.02);
  }

  /* Giant Thumbs Up! Single thumbs up emoji scales and pops out of background bubbles */
  .giant-emoji-msg {
    font-size: 48px !important;
    background: transparent !important;
    color: inherit !important;
    border: none !important;
    padding: 0 !important;
    box-shadow: none !important;
    display: inline-block;
    user-select: none;
    animation: scaleUpPop 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275) forwards;
    cursor: default;
  }

  /* Premium system push message card */
  .system-push-card {
    background: var(--input-bg);
    border: 1px solid var(--border-color);
    border-radius: 12px;
    padding: 14px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    margin: 10px 0;
    max-width: 100%;
    width: 320px;
  }
  .system-push-msg {
    align-self: center;
    max-width: 100%;
  }

  .system-card-header {
    display: flex;
    align-items: center;
    margin-bottom: 8px;
    gap: 8px;
    position: relative;
  }

  .system-card-icon {
    font-size: 20px;
  }

  .system-card-meta {
    display: flex;
    flex-direction: column;
    min-width: 0;
    flex: 1;
  }

  .system-card-title {
    font-size: 14px;
    font-weight: 700;
    color: var(--text-title);
  }

  .system-card-time {
    font-size: 11px;
    color: var(--text-desc);
  }

  .system-badge {
    background: var(--active-blue);
    color: #ffffff;
    font-size: 9px;
    font-weight: bold;
    padding: 2px 6px;
    border-radius: 4px;
    text-transform: uppercase;
  }

  .system-card-body {
    font-size: 13px;
    color: var(--text-desc);
    line-height: 1.5;
    word-break: break-all;
  }

  /* Bottom input bar: standard Aa text box with icon options */
  .bottom-input-tray {
    padding: 16px 24px;
    border-top: 1px solid var(--border-color);
    background: var(--bg-chat-header);
    display: flex;
    align-items: center;
    gap: 12px;
    flex-shrink: 0;
    transition: background-color 0.3s ease, border-color 0.3s ease;
  }

  .tray-left-actions, .tray-right-actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
  }

  .tray-circle-btn {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
    color: var(--active-blue);
    cursor: pointer;
    transition: all 0.15s ease;
  }
  .tray-circle-btn:hover {
    background: var(--btn-hover);
    transform: scale(1.05);
  }

  .messenger-input-wrapper {
    flex: 1;
    min-width: 0;
  }

  .messenger-message-input {
    width: 100%;
    height: 38px;
    background: var(--input-bg);
    border: 1px solid transparent;
    border-radius: 20px;
    padding: 0 16px;
    color: var(--text-title);
    font-size: 14px;
    outline: none;
    box-sizing: border-box;
    transition: all 0.2s ease;
  }
  .messenger-message-input:focus {
    background: var(--bg-chat-viewport);
    border-color: var(--active-blue);
  }

  /* Interactive Send/Thumbs-up buttons with animations */
  .messenger-send-btn {
    background: var(--active-blue);
    color: #ffffff;
    border: none;
    width: 36px;
    height: 36px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: all 0.2s ease;
    animation: scaleUpPop 0.2s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
  }
  .messenger-send-btn:hover {
    transform: scale(1.08);
  }

  .send-arrow-icon {
    font-size: 16px;
    line-height: 1;
  }

  .messenger-thumbs-btn {
    font-size: 24px;
    cursor: pointer;
    user-select: none;
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s ease;
  }
  .messenger-thumbs-btn:hover {
    transform: scale(1.18) rotate(-10deg);
  }

  /* Chat Fallback background when no session is loaded */
  .chat-empty-fallback {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: 40px;
    text-align: center;
    background: var(--bg-chat-viewport);
  }

  .fallback-glow-badge {
    font-size: 80px;
    margin-bottom: 20px;
    animation: scaleUpPop 0.6s cubic-bezier(0.16, 1, 0.3, 1) infinite alternate;
  }

  .fallback-title {
    font-size: 22px;
    font-weight: 800;
    color: var(--text-title);
    margin-bottom: 8px;
  }

  .fallback-desc {
    font-size: 14px;
    color: var(--text-desc);
    max-width: 360px;
    line-height: 1.5;
  }

  /* 4. RIGHT CHAT DETAIL SIDEBAR (INFO PANEL) */
  .right-info-sidebar {
    width: 320px;
    background: var(--bg-sidebar);
    border-left: 1px solid var(--border-color);
    display: flex;
    flex-direction: column;
    flex-shrink: 0;
    transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  }

  .right-profile-header {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 32px 16px 20px 16px;
    text-align: center;
  }

  .big-profile-avatar {
    width: 80px;
    height: 80px;
    border-radius: 50%;
    background: linear-gradient(135deg, var(--active-blue) 0%, #a855f7 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 32px;
    font-weight: 800;
    color: #ffffff;
    margin-bottom: 16px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  }

  .right-profile-name {
    font-size: 18px;
    font-weight: 700;
    color: var(--text-title);
    margin-bottom: 4px;
  }

  .right-profile-id {
    font-size: 12px;
    color: var(--text-desc);
  }

  .right-divider {
    height: 1px;
    background: var(--border-color);
    margin: 0 20px;
  }

  .right-section {
    padding: 16px 20px;
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .right-section-title {
    font-size: 13px;
    font-weight: 700;
    color: var(--text-desc);
    text-transform: uppercase;
    letter-spacing: 0.5px;
    margin-bottom: 4px;
  }

  .glass-menu-item {
    display: flex;
    align-items: center;
    padding: 10px 12px;
    border-radius: 8px;
    font-size: 14px;
    color: var(--text-title);
    cursor: pointer;
    transition: all 0.15s ease;
    font-weight: 500;
  }
  .glass-menu-item:hover {
    background: var(--btn-hover);
  }

  .member-list-mini {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .member-mini-card {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 6px;
  }

  .member-mini-avatar {
    width: 24px;
    height: 24px;
    border-radius: 50%;
    background: var(--btn-hover);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 10px;
    font-weight: bold;
    color: var(--active-blue);
  }

  .member-mini-name {
    font-size: 13px;
    color: var(--text-title);
  }

  .shared-media-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
    margin-top: 4px;
  }

  .shared-img-card {
    aspect-ratio: 1;
    border-radius: 6px;
    overflow: hidden;
    cursor: pointer;
    background: var(--btn-hover);
    transition: transform 0.15s ease;
  }
  .shared-img-card:hover {
    transform: scale(1.05);
  }

  .shared-img-thumb {
    width: 100%;
    height: 100%;
  }

  .no-media-tip {
    grid-column: span 3;
    text-align: center;
    font-size: 12px;
    color: var(--text-desc);
    padding: 20px 0;
  }

  /* MESSENGER CLEAN DIALOG MODAL BOXES */
  .modal-backdrop {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.4);
    backdrop-filter: blur(4px);
    z-index: 1000;
    display: flex;
    align-items: center;
    justify-content: center;
    animation: pop 0.25s ease-out;
  }

  .modal-glass-card {
    width: 90%;
    max-width: 400px;
    background: var(--bg-board);
    border: 1px solid var(--border-color);
    border-radius: 14px;
    padding: 24px;
    box-shadow: var(--shadow-modal);
    display: flex;
    flex-direction: column;
    animation: scaleUpPop 0.25s cubic-bezier(0.16, 1, 0.3, 1) forwards;
  }

  .modal-title {
    font-size: 18px;
    font-weight: 700;
    color: var(--text-title);
    margin-bottom: 16px;
  }

  .modal-actions-row {
    display: flex;
    gap: 12px;
    margin-top: 20px;
  }

  .modal-divider {
    font-size: 12px;
    color: var(--text-desc);
    text-align: center;
    margin: 12px 0;
    position: relative;
  }
  .modal-divider::before, .modal-divider::after {
    content: '';
    position: absolute;
    top: 50%;
    width: 40%;
    height: 1px;
    background: var(--border-color);
  }
  .modal-divider::before { left: 0; }
  .modal-divider::after { right: 0; }

  .secondary-dark {
    background: var(--btn-hover) !important;
    color: var(--text-title) !important;
    box-shadow: none !important;
  }
  .secondary-dark:hover {
    background: var(--btn-active) !important;
  }

  .cyan-glow {
    background: #0084ff !important;
  }

  /* RESPONSIVE PHONE LAYOUT STYLING */
  @media (max-width: 960px) {
    .app-glass-board {
      width: 100vw;
      height: 100vh;
      border-radius: 0;
      border: none;
    }
  }

  @media (max-width: 768px) {
    .nav-sidebar {
      width: 60px;
      padding: 12px 0;
    }
    .user-avatar-gradient {
      width: 38px;
      height: 38px;
      font-size: 14px;
    }
    .nav-item {
      width: 40px;
      height: 40px;
      font-size: 18px;
    }

    .middle-sidebar {
      width: calc(100vw - 60px);
      flex: 1;
    }
    .middle-sidebar.mobile-hidden {
      display: none !important;
    }

    .chat-viewport {
      display: none !important;
      width: calc(100vw - 60px);
      position: absolute;
      top: 0;
      left: 60px;
      right: 0;
      bottom: 0;
      z-index: 50;
    }
    .chat-viewport.mobile-active {
      display: flex !important;
    }

    .back-action-btn {
      display: flex;
    }

    .header-unread-count {
      background: #f3565d;
      color: #ffffff;
      font-size: 10px;
      font-weight: bold;
      padding: 2px 6px;
      border-radius: 8px;
      margin-left: 6px;
    }

    .right-info-sidebar {
      display: none !important;
    }
  }

  /* ================= MESSENGER FAB STYLE ================= */
  .messenger-fab-container {
    position: absolute;
    right: 24px;
    bottom: 24px;
    z-index: 999;
    display: flex;
    flex-direction: column;
    align-items: flex-end;
  }

  .messenger-main-fab {
    width: 50px;
    height: 50px;
    border-radius: 50%;
    background: linear-gradient(135deg, #0084ff, #00c6ff);
    color: #ffffff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 26px;
    font-weight: 300;
    cursor: pointer;
    box-shadow: 0 4px 16px rgba(0, 132, 255, 0.45);
    transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
    user-select: none;
  }

  .messenger-main-fab:hover {
    transform: scale(1.1) rotate(90deg);
    box-shadow: 0 6px 22px rgba(0, 132, 255, 0.65);
  }

  .messenger-main-fab:active {
    transform: scale(0.92) rotate(90deg);
  }

  .fab-submenu {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 12px;
    margin-bottom: 14px;
    opacity: 0;
    pointer-events: none;
    transform: translateY(25px) scale(0.75);
    transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  }

  .messenger-fab-container.active .fab-submenu {
    opacity: 1;
    pointer-events: auto;
    transform: translateY(0) scale(1);
  }

  .fab-submenu-item {
    display: flex;
    align-items: center;
    gap: 12px;
    cursor: pointer;
    background: transparent;
    border: none;
    padding: 0;
  }

  .fab-submenu-icon {
    width: 42px;
    height: 42px;
    border-radius: 50%;
    background: var(--bg-middle);
    border: 1px solid var(--border-color);
    box-shadow: 0 4px 14px rgba(0, 0, 0, 0.18);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
    transition: all 0.25s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  }

  .fab-submenu-label {
    background: var(--bg-middle);
    color: var(--text-title);
    font-size: 13px;
    font-weight: 600;
    padding: 6px 14px;
    border-radius: 18px;
    box-shadow: 0 4px 14px rgba(0, 0, 0, 0.15);
    border: 1px solid var(--border-color);
    white-space: nowrap;
    opacity: 0;
    transform: translateX(12px);
    transition: all 0.25s ease;
    pointer-events: none;
  }

  .fab-submenu-item:hover .fab-submenu-icon {
    transform: scale(1.15);
    background: var(--btn-hover);
    box-shadow: 0 6px 18px rgba(0, 0, 0, 0.25);
  }

  .fab-submenu-item:hover .fab-submenu-label {
    opacity: 1;
    transform: translateX(0);
  }

  /* ================= SYSTEM NOTIFICATION STYLE ================= */
  .system-notification-row {
    display: flex;
    justify-content: center;
    align-items: center;
    margin: 18px 0;
    padding: 0 24px;
    width: 100%;
    box-sizing: border-box;
  }

  .system-notification-text {
    background: var(--btn-hover);
    color: var(--text-desc);
    font-size: 12px;
    font-weight: 500;
    padding: 6px 18px;
    border-radius: 16px;
    border: 1px solid var(--border-color);
    text-align: center;
    box-shadow: 0 2px 10px rgba(0, 0, 0, 0.04);
    letter-spacing: 0.2px;
    max-width: 85%;
    word-break: break-all;
    line-height: 1.4;
  }
</style>
