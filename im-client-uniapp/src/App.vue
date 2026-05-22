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
        <t-u-i-conversation
          v-if="activeTab === 'chats'"
          :sessions="sessions"
          :current-session="currentSession"
          :friend-list="friendList"
          v-model:showFabMenu="showFabMenu"
          @open-chat="openChat"
          @show-create-group="showCreateGroup = true"
          @show-start-chat="showStartChat = true"
          @start-chat-with="startChatWith"
        />

        <!-- Tab 2: Contacts list -->
        <t-u-i-contact
          v-if="activeTab === 'contacts'"
          :friend-list="friendList"
          @start-chat-with="startChatWith"
        />

        <!-- Tab 3 & 4: Developer Platform Console & Settings -->
        <t-u-i-dev-console
          v-if="activeTab === 'console' || activeTab === 'settings'"
          :active-tab="activeTab"
          :generated-token="generatedToken"
          :online-check-result="onlineCheckResult"
          :current-theme="currentTheme"
          :server-http-url="serverHttpUrl"
          :server-ws-url="serverWsUrl"
          :external-api-key="externalApiKey"
          :is-connected="isConnected"
          :user-id="userId"
          @sim-generate-token="simGenerateToken"
          @copy-and-use-token="copyAndUseToken"
          @sim-trigger-push="simTriggerPush"
          @sim-check-online="simCheckOnline"
          @set-theme="setTheme"
          @save-settings="saveSettings"
        />

      </div>

      <!-- 3. RIGHT CORE CHAT AREA -->
      <t-u-i-chat
        ref="tuiChat"
        :current-session="currentSession"
        :current-msgs="currentMsgs"
        :peer-typing="peerTyping"
        :current-group-members="currentGroupMembers"
        :user-id="userId"
        :username="username"
        :server-http-url="serverHttpUrl"
        :view="view"
        :total-unread="totalUnread"
        @back-to-list="view = 'list'"
        @show-start-chat="showStartChat = true"
        @choose-image="chooseImage"
        @send-thumbs-up="sendThumbsUp"
        @send-message="handleSendMessageEvent"
        @send-edit-request="handleSendEditRequestEvent"
        @send-recall-request="sendRecallRequest"
        @clear-history="clearHistory"
        @handle-input="handleInput"
      />

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
import TUIConversation from './components/TUIConversation.vue';
import TUIContact from './components/TUIContact.vue';
import TUIDevConsole from './components/TUIDevConsole.vue';
import TUIChat from './components/TUIChat.vue';

export default {
  components: {
    TUIConversation,
    TUIContact,
    TUIDevConsole,
    TUIChat
  },
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

      // Data Models
      sessions: [], // { id, name, type, lastMsg, time, unread }
      messages: {}, // { sessionId: [ { sender, content, msgType, time, isSelf } ] }
      currentSession: null,
      currentGroupMembers: [],

      // Simulated Platform Tools Data
      simTokenUserId: 'dev_user_777',
      simTokenUsername: '体验账号',
      generatedToken: '',

      simPushToUserId: '',
      simPushSender: '财务结算中心',
      simPushMsg: '【财务通知】您好，您申请的 320.00 元退款已原路打回原支付账户，请查收账单。',

      simCheckUserId: '',
      onlineCheckResult: '',

      // Inputs
      targetGroupId: '',
      newChatId: '',

      // Scroll and state variables
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
      currentTheme: 'light',
      peerTyping: false,
      typingTimer: null,
      lastTypingTime: 0,
      
      // Temporary quote reference for doSend
      quotingMessage: null
    };
  },
  computed: {
    currentMsgs() {
      if (!this.currentSession) return [];
      return this.messages[this.currentSession.id] || [];
    },
    totalUnread() {
      return this.sessions.reduce((sum, s) => sum + (s.unread || 0), 0);
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
    saveSettings({ serverHttpUrl, serverWsUrl, externalApiKey }) {
      this.serverHttpUrl = serverHttpUrl;
      this.serverWsUrl = serverWsUrl;
      this.externalApiKey = externalApiKey;
      uni.setStorageSync('im_server_http', this.serverHttpUrl);
      uni.setStorageSync('im_server_ws', this.serverWsUrl);
      uni.setStorageSync('im_api_key', this.externalApiKey);
      uni.showToast({ title: '配置保存成功，稍后生效！', icon: 'success' });
    },

    formatTime(date) {
      return date.getHours() + ':' + (date.getMinutes() < 10 ? '0' : '') + date.getMinutes();
    },

    saveData() {
      uni.setStorage({ key: 'im_sessions', data: this.sessions });
      uni.setStorage({ key: 'im_messages', data: this.messages });
    },

    copyId() {
      if (!this.userId) return;
      uni.setClipboardData({
        data: this.userId,
        success: () => uni.showToast({ title: '已复制您的用户 ID', icon: 'none' })
      });
    },

    // Connection socket
    connectSocket() {
      if (this.reconnectTimer) {
        clearTimeout(this.reconnectTimer);
        this.reconnectTimer = null;
      }

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
        
        this.reconnectAttempts = (this.reconnectAttempts || 0) + 1;
        const delay = Math.min(30000, 1000 * Math.pow(2, this.reconnectAttempts));
        console.warn(`WebSocket closed. Reconnecting in ${delay}ms (attempt ${this.reconnectAttempts})...`);
        
        if (this.reconnectTimer) clearTimeout(this.reconnectTimer);
        this.reconnectTimer = setTimeout(() => this.connectSocket(), delay);
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
          this.reconnectAttempts = 0; // Reset reconnection attempts on success
          
          uni.showToast({ title: '成功安全接入 IM 系统', icon: 'success' });
          
          // If there is already an active session, sync history immediately on reconnect
          if (this.currentSession) {
            console.log(`Syncing active session history on reconnect: ${this.currentSession.id}`);
            this.fetchMessageHistory(this.currentSession);
          } else {
            // Auto-select last active session to avoid initial blank screen
            setTimeout(() => {
              if (this.sessions && this.sessions.length > 0) {
                this.openChat(this.sessions[0]);
              }
            }, 300);
          }
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
        this.onReceiveMessage(sessionId, sessionName, 'user', packet.message, packet.fromUserId, packet.msgType, packet.fromUserName, packet.msgId, packet.quoteMsgId, packet.quoteSender, packet.quoteContent);
        this.sendAck(packet.msgId);

        // Clear typing status from peer when a real message arrives
        if (this.currentSession && this.currentSession.id === sessionId) {
          this.peerTyping = false;
          if (this.typingTimer) {
            clearTimeout(this.typingTimer);
            this.typingTimer = null;
          }
          this.reportReadReceipt(sessionId);
        }
      }
      else if (packet.command === 12) { // GROUP_MESSAGE_RESPONSE
        const sessionId = 'g:' + packet.fromGroupId;
        const senderName = packet.fromUser;
        this.onReceiveMessage(sessionId, `群组 ${packet.fromGroupId}`, 'group', packet.message, senderName, packet.msgType, null, packet.msgId, packet.quoteMsgId, packet.quoteSender, packet.quoteContent);
        
        if (this.currentSession && this.currentSession.id === sessionId) {
          this.reportGroupReadReceipt(sessionId, packet.msgId);
        }
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
      else if (packet.command === 13) { // ACK_RESPONSE
        const msgId = packet.msgId;
        if (msgId) {
          for (let sId in this.messages) {
            const list = this.messages[sId];
            const found = list.find(m => m.msgId === msgId);
            if (found) {
              found.status = 'sent';
              this.saveData();
              break;
            }
          }
        }
      }
      else if (packet.command === 17) { // READ_RECEIPT_RESPONSE
        const peerUserId = packet.fromUserId;
        const lastReadMsgId = packet.lastReadMsgId;
        const list = this.messages[peerUserId];
        if (list && lastReadMsgId) {
          let updated = false;
          let targetIndex = list.findIndex(m => m.msgId === lastReadMsgId);
          if (targetIndex !== -1) {
            for (let i = 0; i <= targetIndex; i++) {
              if (list[i].isSelf && list[i].status !== 'read') {
                list[i].status = 'read';
                updated = true;
              }
            }
          }
          if (updated) {
            this.saveData();
            this.$forceUpdate();
          }
        }
      }
      else if (packet.command === 19) { // TYPING_RESPONSE
        const fromUserId = packet.fromUserId;
        if (this.currentSession && this.currentSession.id === fromUserId) {
          this.peerTyping = true;
          if (this.typingTimer) {
            clearTimeout(this.typingTimer);
          }
          this.typingTimer = setTimeout(() => {
            this.peerTyping = false;
          }, 4000);
          
          if (this.isAtBottom) {
            setTimeout(() => this.scrollToBottom(), 50);
          }
        }
      }
      else if (packet.command === 22) { // RECALL_RESPONSE
        const msgId = packet.msgId;
        const toGroupId = packet.toGroupId;
        const fromUserId = packet.fromUserId;
        const success = packet.success;
        
        if (success && msgId) {
          const sessionId = toGroupId ? 'g:' + toGroupId : fromUserId;
          const list = this.messages[sessionId];
          if (list) {
            const found = list.find(m => m.msgId === msgId);
            if (found) {
              found.status = 'recalled';
              found.content = '[撤回消息]';
              this.saveData();
              this.$forceUpdate();
            }
          }
        }
      }
      else if (packet.command === 27) { // EDIT_RESPONSE
        const msgId = packet.msgId;
        const toGroupId = packet.toGroupId;
        const fromUserId = packet.fromUserId;
        const newContent = packet.newContent;
        const success = packet.success;
        
        if (success && msgId) {
          const sessionId = toGroupId ? 'g:' + toGroupId : fromUserId;
          const list = this.messages[sessionId];
          if (list) {
            const found = list.find(m => m.msgId === msgId);
            if (found) {
              found.content = newContent;
              found.status = 'edited';
              found.isEdited = true;
              this.saveData();
              this.$forceUpdate();
            }
          }
        }
      }
      else if (packet.command === 29) { // GROUP_READ_RECEIPT_RESPONSE
        const groupId = packet.groupId;
        const msgId = packet.msgId;
        const readCount = packet.readCount;
        const readUserIds = packet.readUserIds || [];
        
        if (groupId && msgId) {
          const sessionId = 'g:' + groupId;
          const list = this.messages[sessionId];
          if (list) {
            const found = list.find(m => m.msgId === msgId);
            if (found) {
              found.readCount = readCount;
              found.readUserIds = readUserIds;
              this.saveData();
              this.$forceUpdate();
            }
          }
        }
      }
    },

    onReceiveMessage(sessionId, sessionName, type, content, senderName, msgType = 1, fromUserName = null, msgId = null, quoteMsgId = null, quoteSender = null, quoteContent = null) {
      if (!this.messages[sessionId]) {
        this.messages[sessionId] = [];
      }
      this.messages[sessionId].push({
        msgId: msgId,
        sender: senderName,
        senderName: fromUserName || senderName,
        content: content,
        msgType: msgType,
        time: this.formatTime(new Date()),
        createTimeMs: Date.now(),
        isSelf: false,
        quoteMsgId: quoteMsgId,
        quoteSender: quoteSender,
        quoteContent: quoteContent
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
      
      if (this.typingTimer) {
        clearTimeout(this.typingTimer);
        this.typingTimer = null;
      }
      this.peerTyping = false;

      // Pull latest history messages from MySQL on opening chat to sync state
      this.fetchMessageHistory(session);

      if (session.type === 'group') {
        this.fetchGroupMembers();
        // Sync group read status
        setTimeout(() => {
          const list = this.messages[session.id] || [];
          if (list.length > 0) {
            const lastMsg = list[list.length - 1];
            if (!lastMsg.isSelf && lastMsg.msgId) {
              this.reportGroupReadReceipt(session.id, lastMsg.msgId);
            }
          }
        }, 800);
      } else {
        this.reportReadReceipt(session.id);
      }
    },

    fetchMessageHistory(session, beforeTime = null, limit = 20) {
      if (!session) return;
      const peerId = session.type === 'group' ? session.id.substring(2) : session.id;
      const type = session.type === 'group' ? 2 : 1;
      const beforeTimeParam = beforeTime ? `&beforeTime=${beforeTime}` : '';
      
      uni.request({
        url: `${this.serverHttpUrl}/api/external/message/history?userId=${this.userId}&peerId=${peerId}&type=${type}${beforeTimeParam}&limit=${limit}`,
        method: 'GET',
        header: {
          'Authorization': 'Bearer ' + this.externalApiKey
        },
        success: (res) => {
          if (res.statusCode === 200 && res.data.success) {
            const list = res.data.messages || [];
            
            const formatted = list.map(m => {
              const isSelf = m.fromUserId === this.userId;
              const statusMap = ['sending', 'sent', 'read', 'recalled'];
              return {
                msgId: m.msgId,
                sender: isSelf ? '我' : m.fromUserId,
                senderName: isSelf ? this.username : m.fromUserId,
                content: m.content,
                msgType: m.msgType || 1,
                time: this.formatTime(new Date(m.createTime)),
                createTimeMs: new Date(m.createTime).getTime(),
                isSelf: isSelf,
                status: m.status !== undefined ? (statusMap[m.status] || 'read') : 'read',
                quoteMsgId: m.quoteMsgId,
                quoteSender: m.quoteSender,
                quoteContent: m.quoteContent
              };
            });

            if (!this.messages[session.id]) {
              this.messages[session.id] = [];
            }

            if (beforeTime) {
              const existingIds = new Set(this.messages[session.id].map(m => m.msgId));
              const filtered = formatted.filter(m => !existingIds.has(m.msgId));
              this.messages[session.id] = [...filtered, ...this.messages[session.id]];
            } else {
              const existingIds = new Set(this.messages[session.id].map(m => m.msgId));
              const filtered = formatted.filter(m => !existingIds.has(m.msgId));
              this.messages[session.id] = [...this.messages[session.id], ...filtered];
              this.messages[session.id].sort((a, b) => (a.createTimeMs || 0) - (b.createTimeMs || 0));
              setTimeout(() => this.scrollToBottom(), 50);
            }
            this.saveData();
            this.$forceUpdate();
          }
        },
        fail: (err) => {
          console.error('Failed to load message history', err);
        }
      });
    },

    fetchGroupMembers() {
      if (!this.currentSession || this.currentSession.type !== 'group') return;
      const rawId = this.currentSession.id.substring(2); // remove 'g:'
      uni.request({
        url: this.serverHttpUrl + '/api/external/group/members?groupId=' + rawId,
        method: 'GET',
        header: {
          'Authorization': 'Bearer ' + this.externalApiKey
        },
        success: (res) => {
          if (res.statusCode === 200 && res.data.success) {
            this.currentGroupMembers = res.data.members;
          } else {
            console.error('Failed to load group members', res.data.message);
          }
        },
        fail: (err) => {
          console.error('Network error requesting group members', err);
        }
      });
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

    handleSendMessageEvent({ content, quote }) {
      if (quote) {
        this.quotingMessage = {
          msgId: quote.msgId,
          sender: quote.sender,
          senderName: quote.sender,
          content: quote.content
        };
      } else {
        this.quotingMessage = null;
      }
      this.doSend(content, 1);
    },

    handleSendEditRequestEvent({ msg, newContent }) {
      this.sendEditRequest(msg, newContent);
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

      if (this.quotingMessage) {
        packet.quoteMsgId = this.quotingMessage.msgId;
        packet.quoteSender = this.quotingMessage.senderName || this.quotingMessage.sender;
        packet.quoteContent = this.quotingMessage.content;
      }

      this.send(packet);

      if (!this.messages[session.id]) {
        this.messages[session.id] = [];
      }
      
      const newMsg = {
        msgId: msgId,
        sender: '我',
        senderName: this.username,
        content: content,
        msgType: msgType,
        time: this.formatTime(new Date()),
        createTimeMs: Date.now(),
        isSelf: true,
        status: 'sending'
      };

      if (this.quotingMessage) {
        newMsg.quoteMsgId = this.quotingMessage.msgId;
        newMsg.quoteSender = this.quotingMessage.senderName || this.quotingMessage.sender;
        newMsg.quoteContent = this.quotingMessage.content;
        this.quotingMessage = null;
      }

      this.messages[session.id].push(newMsg);

      session.lastMsg = msgType === 2 ? '[图片]' : content;
      session.time = this.formatTime(new Date());
      this.saveData();

      setTimeout(() => this.scrollToBottom(), 50);
    },

    createGroup() {
      this.send({ command: 7, userIdList: [] });
    },

    joinGroup() {
      if (!this.targetGroupId) return;
      this.send({ command: 9, groupId: this.targetGroupId });
    },

    reportReadReceipt(sessionId) {
      if (!this.isConnected || !this.isLogin) return;
      if (sessionId.startsWith('g:')) return;
      
      const messages = this.messages[sessionId];
      if (!messages || messages.length === 0) return;

      let lastPeerMsg = null;
      for (let i = messages.length - 1; i >= 0; i--) {
        if (!messages[i].isSelf && messages[i].msgId) {
          lastPeerMsg = messages[i];
          break;
        }
      }

      if (lastPeerMsg) {
        this.send({
          command: 16,
          toUserId: sessionId,
          lastReadMsgId: lastPeerMsg.msgId
        });
        console.log(`[Read Receipt] Reported read up to ${lastPeerMsg.msgId} for session ${sessionId}`);
      }
    },

    handleInput() {
      if (!this.isConnected || !this.isLogin || !this.currentSession || this.currentSession.type === 'group') return;
      
      const now = Date.now();
      if (now - this.lastTypingTime > 3000) {
        this.lastTypingTime = now;
        this.send({
          command: 18,
          toUserId: this.currentSession.id
        });
        console.log(`[Typing] Sent typing status to ${this.currentSession.id}`);
      }
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

    clearHistory(session) {
      const activeSession = session || this.currentSession;
      if (!activeSession) return;
      
      uni.showModal({
        title: '清除聊天',
        content: '确定要清空与当前用户的聊天记录吗？',
        success: (res) => {
          if (res.confirm) {
            this.messages[activeSession.id] = [];
            const sess = this.sessions.find(s => s.id === activeSession.id);
            if (sess) sess.lastMsg = '聊天记录已清空';
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

    simGenerateToken({ userId, username }) {
      this.simTokenUserId = userId;
      this.simTokenUsername = username;
      if (!userId) {
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
          userId: userId,
          username: username
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

    copyAndUseToken(token) {
      if (!token) return;
      uni.setClipboardData({
        data: token,
        success: () => {
          uni.showToast({ title: '已复制，自动填入输入框', icon: 'success' });
          this.secureTokenInput = token;
          this.loginMode = 'token';
          if (this.isLogin) {
            this.logout(); // trigger logout to let user re-login
          }
        }
      });
    },

    simTriggerPush({ toUserId, senderName, message }) {
      const toId = toUserId || this.userId;
      if (!toId) {
        uni.showToast({ title: '请指定推送的目标用户 ID', icon: 'none' });
        return;
      }
      if (!message) {
        uni.showToast({ title: '请输入推送卡片的内容', icon: 'none' });
        return;
      }

      this.simPushToUserId = toUserId;
      this.simPushSender = senderName;
      this.simPushMsg = message;

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
          fromUserName: senderName || '三方推送中心',
          message: message,
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

    simCheckOnline(userId) {
      this.simCheckUserId = userId;
      if (!userId) {
        uni.showToast({ title: '请输入要查询的用户 ID', icon: 'none' });
        return;
      }

      uni.request({
        url: this.serverHttpUrl + '/api/external/user/status?userId=' + userId,
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
    },

    sendEditRequest(msg, newContent) {
      if (!msg || !newContent) return;
      const session = this.currentSession;
      const isGroup = session.type === 'group';
      const targetId = isGroup ? session.id.substring(2) : session.id;
      
      const packet = {
        command: 26, // EDIT_REQUEST
        msgId: msg.msgId,
        [isGroup ? 'toGroupId' : 'toUserId']: targetId,
        newContent: newContent
      };
      
      this.send(packet);
      
      // Update locally
      msg.content = newContent;
      msg.isEdited = true;
      msg.status = 'edited';
      this.saveData();
      uni.showToast({ title: '消息已编辑', icon: 'success' });
    },

    sendRecallRequest(msg) {
      if (!msg) return;
      const timeDiff = Date.now() - (msg.createTimeMs || 0);
      if (timeDiff > 120000) {
        uni.showModal({
          title: '撤回失败',
          content: '发送时间已超过 2 分钟，无法撤回',
          showCancel: false
        });
        return;
      }
      
      const session = this.currentSession;
      const isGroup = session.type === 'group';
      const targetId = isGroup ? session.id.substring(2) : session.id;
      
      const packet = {
        command: 21, // RECALL_REQUEST
        msgId: msg.msgId,
        [isGroup ? 'toGroupId' : 'toUserId']: targetId
      };
      
      this.send(packet);
      
      // Update locally
      msg.status = 'recalled';
      msg.content = '[撤回消息]';
      this.saveData();
      uni.showToast({ title: '已撤回消息', icon: 'success' });
    },

    reportGroupReadReceipt(sessionId, msgId) {
      if (!this.isConnected || !this.isLogin) return;
      if (!sessionId.startsWith('g:') || !msgId) return;
      const groupId = sessionId.substring(2);
      this.send({
        command: 28, // GROUP_READ_RECEIPT_REQUEST
        groupId: groupId,
        msgId: msgId
      });
      console.log(`[Group Read Receipt] Reported read for message ${msgId} in Group ${groupId}`);
    }
  }
}
</script>

<style>
  /* ==========================================
     GLOBAL APP LAYOUT & THEME VARIABLES
     ========================================== */

  /* Global sleek minimalist scrollbar */
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

  /* Reset & base viewport */
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

  .glow-orb {
    display: none !important;
  }

  /* Theme Configurations */
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

  /* Utility helpers */
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

  /* MESSENGER LOGIN SCREEN */
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

  /* WORKBOARD MAIN FRAME */
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
    display: none;
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

  /* 2. MIDDLE LISTING AREA */
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

  /* 3. MODALS BACKDROP */
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
    text-align: left;
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

  .mini-input {
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
  .mini-input:focus {
    border-color: var(--active-blue);
  }

  /* Responsive layouts */
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
  }
</style>
