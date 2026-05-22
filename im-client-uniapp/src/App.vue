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
          <!-- Instagram Web Cursive Logo -->
          <div class="sidebar-ins-logo">
            <span class="ins-logo-text">govg-im</span>
            <span class="ins-logo-icon">📸</span>
          </div>

          <div class="nav-menu">
            <div class="nav-item" :class="{ active: activeTab === 'chats' }" @click="activeTab = 'chats'">
              <span class="nav-icon">{{ activeTab === 'chats' ? '💬' : '💭' }}</span>
              <span class="nav-label">聊天</span>
              <div v-if="totalUnread > 0" class="unread-badge-mini">{{ totalUnread }}</div>
            </div>
            <div class="nav-item" :class="{ active: activeTab === 'contacts' }" @click="activeTab = 'contacts'">
              <span class="nav-icon">{{ activeTab === 'contacts' ? '👥' : '👤' }}</span>
              <span class="nav-label">联系人</span>
              <div v-if="friendRequests.length > 0" class="unread-badge-mini">{{ friendRequests.length }}</div>
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

          <!-- Profile Avatar with Stories-style ring at the very bottom -->
          <div class="user-avatar-wrapper ins-my-profile" @click="copyId" title="点击复制用户 ID (Click to copy)">
            <div class="user-avatar-gradient">
              <span class="avatar-letter">{{ username ? username[0].toUpperCase() : 'U' }}</span>
            </div>
            <span class="online-indicator active"></span>
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
          @toggle-pin="handleTogglePin"
        />

        <!-- Tab 2: Contacts list -->
        <t-u-i-contact
          v-if="activeTab === 'contacts'"
          :friend-list="friendList"
          :friend-requests="friendRequests"
          :blacklist-list="blacklistList"
          @start-chat-with="startChatWith"
          @send-friend-request="handleSendFriendRequest"
          @friend-request-approval="handleFriendRequestApproval"
          @relation-action="handleRelationAction"
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
          :my-status-type="myStatusType"
          :my-status-text="myStatusText"
          :audit-policy="auditPolicy"
          :sensitive-words="sensitiveWords"
          @sim-generate-token="simGenerateToken"
          @copy-and-use-token="copyAndUseToken"
          @sim-trigger-push="simTriggerPush"
          @sim-check-online="simCheckOnline"
          @set-theme="setTheme"
          @save-settings="saveSettings"
          @update-status="handleUpdateStatus"
          @sync-audit="handleSyncAudit"
          @agent-login="handleAgentLogin"
          @agent-send-reply="handleAgentSendReply"
          @agent-end-session="handleAgentEndSession"
          ref="devConsole"
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
        :group-call-active="groupCallActive"
        @back-to-list="view = 'list'"
        @show-start-chat="showStartChat = true"
        @choose-image="chooseImage"
        @send-thumbs-up="sendThumbsUp"
        @send-message="handleSendMessageEvent"
        @send-edit-request="handleSendEditRequestEvent"
        @send-recall-request="sendRecallRequest"
        @clear-history="clearHistory"
        @handle-input="handleInput"
        @send-reaction="handleSendReactionEvent"
        @initiate-call="handleInitiateCall"
        @send-desk-action="handleSendDeskAction"
        @initiate-group-call="handleInitiateGroupCall"
        @join-group-call="handleJoinGroupCall"
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

    <!-- ================= TUICALL OVERLAY ================= -->
    <t-u-i-call-overlay
      v-if="currentCall"
      :call-state="currentCall.state"
      :peer-name="currentCall.peerName"
      :call-type="currentCall.callType"
      @action="handleCallAction"
    />

    <!-- ================= TUIMEETING OVERLAY ================= -->
    <t-u-i-meeting-overlay
      :visible="meetingOverlayVisible"
      :group-id="meetingGroupId"
      :group-name="meetingGroupName"
      :current-user-id="userId"
      :members-json="meetingMembersJson"
      @toggle-mute="handleMeetingToggleMute"
      @leave-meeting="handleMeetingLeave"
    />
  </div>
</template>

<script>
import TUIConversation from './components/TUIConversation.vue';
import TUIContact from './components/TUIContact.vue';
import TUIDevConsole from './components/TUIDevConsole.vue';
import TUIChat from './components/TUIChat.vue';
import TUICallOverlay from './components/TUICallOverlay.vue';
import TUIMeetingOverlay from './components/TUIMeetingOverlay.vue';

export default {
  components: {
    TUIConversation,
    TUIContact,
    TUIDevConsole,
    TUIChat,
    TUICallOverlay,
    TUIMeetingOverlay
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
      friendList: [],
      statusCheckTimer: null,
      currentTheme: 'light',
      peerTyping: false,
      typingTimer: null,
      lastTypingTime: 0,
      
      // Temporary quote reference for doSend
      quotingMessage: null,
      
      // Voice/Video call signaling states
      currentCall: null,

      // User Status States
      myStatusType: 'online',
      myStatusText: '',

      // Cloud Auditing States
      auditPolicy: 'BLOCK',
      sensitiveWords: ['fuck', 'shit', 'crack', 'hack', 'bomb', 'trump', 'buy bitcoin', '挂科', '垃圾'],

      // Relationship States (TUIContact)
      friendRequests: [],
      blacklistList: [],

      // Group Call States (TUIRoomKit)
      groupCallActive: {}, // { 'g:groupId': { active: true/false, membersCount: 1, members: [...] } }
      meetingOverlayVisible: false,
      meetingGroupId: '',
      meetingGroupName: '',
      meetingMembers: []
    };
  },
  computed: {
    currentMsgs() {
      if (!this.currentSession) return [];
      return this.messages[this.currentSession.id] || [];
    },
    totalUnread() {
      return this.sessions.reduce((sum, s) => sum + (s.unread || 0), 0);
    },
    meetingMembersJson() {
      return JSON.stringify(this.meetingMembers);
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
          
          // Request browser notification permission for TIMPush
          if (typeof Notification !== 'undefined' && Notification.permission === 'default') {
            setTimeout(() => Notification.requestPermission(), 2000);
          }

          // Fetch relationships on successful login
          this.fetchRelationships();
          
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
              if (packet.status === 5001) {
                found.status = 'blocked';
                found.reason = packet.message || '消息包含安全违规词汇，已被云端拦截发送。';
              } else if (packet.status === 5002) {
                found.status = 'blocked';
                found.reason = packet.message || '消息已发出，但被对方拒收了。';
              } else {
                found.status = 'sent';
              }
              this.saveData();
              this.$forceUpdate();
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
      else if (packet.command === 31) { // MESSAGE_REACTION_RESPONSE
        const msgId = packet.msgId;
        const fromUserId = packet.fromUserId;
        const fromUserName = packet.fromUserName;
        const emoji = packet.emoji;
        const action = packet.action;
        const toGroupId = packet.toGroupId;
        const toUserId = packet.toUserId;
        
        const sessionId = toGroupId ? 'g:' + toGroupId : (fromUserId === this.userId ? toUserId : fromUserId);
        const list = this.messages[sessionId];
        if (list && msgId) {
          const found = list.find(m => m.msgId === msgId);
          if (found) {
            if (!found.reactions) {
              found.reactions = {};
            }
            if (action === 'add') {
              for (let emo in found.reactions) {
                found.reactions[emo] = found.reactions[emo].filter(u => u.userId !== fromUserId);
              }
              if (!found.reactions[emoji]) {
                found.reactions[emoji] = [];
              }
              if (!found.reactions[emoji].some(u => u.userId === fromUserId)) {
                found.reactions[emoji].push({ userId: fromUserId, userName: fromUserName });
              }
            } else if (action === 'remove') {
              if (found.reactions[emoji]) {
                found.reactions[emoji] = found.reactions[emoji].filter(u => u.userId !== fromUserId);
              }
            }
            for (let emo in found.reactions) {
              if (found.reactions[emo].length === 0) {
                delete found.reactions[emo];
              }
            }
            this.saveData();
            this.$forceUpdate();
          }
        }
      }
      else if (packet.command === 49) { // MULTI_VOICE_CALL_RESPONSE
        const groupId = packet.groupId;
        const action = packet.action;
        const fromUserId = packet.userId;
        let members = [];
        try {
          members = typeof packet.members === 'string' ? JSON.parse(packet.members) : (packet.members || []);
        } catch (e) {
          console.error('[Meeting WS] Failed to parse members JSON:', e);
        }

        console.log(`[Meeting Signal] Received action ${action} from user ${fromUserId} in group ${groupId}. Members:`, members);

        const sId = 'g:' + groupId;
        
        // Update groupCallActive state in a reactive, compatible manner
        this.groupCallActive[sId] = {
          active: members.length > 0,
          membersCount: members.length,
          members: members
        };
        this.groupCallActive = Object.assign({}, this.groupCallActive);

        // If this is about the current user, manage the TUIMeetingOverlay visibility and data
        const isCurrentUserInMeeting = members.some(m => m.userId === this.userId);
        if (isCurrentUserInMeeting) {
          this.meetingGroupId = groupId;
          this.meetingGroupName = `群组 ${groupId}`;
          this.meetingMembers = members;
          this.meetingOverlayVisible = true;
        } else {
          // If the current user just left or group call is empty
          if (fromUserId === this.userId && action === 'leave') {
            this.meetingOverlayVisible = false;
          }
          if (this.meetingGroupId === groupId && !members.some(m => m.userId === this.userId)) {
            this.meetingOverlayVisible = false;
          }
        }

        // Update locally the active meeting members in overlay if visible
        if (this.meetingOverlayVisible && this.meetingGroupId === groupId) {
          this.meetingMembers = members;
        }

        // Inject grey system notification message into the chat history
        let notificationText = '';
        if (action === 'start') {
          notificationText = `🔊 ${fromUserId} 发起了群多人视频会议`;
        } else if (action === 'join') {
          notificationText = `👋 ${fromUserId} 加入了视频会议`;
        } else if (action === 'leave') {
          notificationText = `🚪 ${fromUserId} 离开了视频会议`;
        }

        if (notificationText) {
          if (!this.messages[sId]) {
            this.messages[sId] = [];
          }
          this.messages[sId].push({
            sender: 'system_notification',
            senderName: '系统通知',
            content: notificationText,
            msgType: 1,
            time: this.formatTime(new Date()),
            isSelf: false
          });
          this.saveData();
          this.$forceUpdate();
          
          if (this.currentSession && this.currentSession.id === sId) {
            setTimeout(() => this.scrollToBottom(), 50);
          }
        }
      }
      else if (packet.command === 51 || packet.command === 53) { // FRIEND_ADD_RESPONSE / RELATION_ACTION_RESPONSE
        console.log('[Relationships Signal] Relationship updated, refetching relationships...');
        this.fetchRelationships();
      }
      else if (packet.command === 40) { // VOICE_CALL_REQUEST
        const callId = packet.callId;
        const fromUserId = packet.fromUserId;
        const callType = packet.callType;
        
        console.log(`[Call Signaling] Voice call request from ${fromUserId}. Call ID: ${callId}. Type: ${callType}`);
        
        if (this.currentCall) {
          console.log(`[Call Signaling] Already busy. Replying busy back to ${fromUserId}`);
          this.send({
            command: 41, // VOICE_CALL_RESPONSE
            callId: callId,
            toUserId: fromUserId,
            fromUserId: this.userId,
            action: 'busy'
          });
          return;
        }
        
        this.currentCall = {
          callId: callId,
          peerId: fromUserId,
          peerName: fromUserId,
          callType: callType,
          state: 'ringing'
        };
        
        const friend = this.friendList.find(f => f.id === fromUserId);
        if (friend) {
          this.currentCall.peerName = friend.name;
        }
      }
      else if (packet.command === 41) { // VOICE_CALL_RESPONSE
        const callId = packet.callId;
        const fromUserId = packet.fromUserId;
        const action = packet.action;
        
        console.log(`[Call Signaling] Received voice call response from ${fromUserId}. Action: ${action}`);
        
        if (this.currentCall && this.currentCall.callId === callId) {
          if (action === 'accept') {
            this.currentCall.state = 'connected';
          } else if (action === 'reject') {
            uni.showModal({
              title: '通话已拒绝',
              content: '对方已拒绝您的通话请求。',
              showCancel: false
            });
            this.currentCall = null;
          } else if (action === 'busy') {
            uni.showModal({
              title: '对方正忙',
              content: '对方当前正处于通话中。',
              showCancel: false
            });
            this.currentCall = null;
          } else if (action === 'cancel') {
            uni.showToast({ title: '通话已取消', icon: 'none' });
            this.currentCall = null;
          } else if (action === 'hangup') {
            uni.showToast({ title: '通话已挂断', icon: 'none' });
            this.currentCall = null;
          }
        }
      }
      else if (packet.command === 43) { // USER_STATUS_RESPONSE
        const uId = packet.userId;
        const sType = packet.statusType || 'online';
        const sText = packet.statusText || '';
        const isOnline = packet.online;
        
        console.log(`[Status Sync] Received status from ${uId}: ${sType} - "${sText}" (Online: ${isOnline})`);
        
        // Find friend in friendList
        const friend = this.friendList.find(f => f.id === uId);
        if (friend) {
          friend.online = isOnline;
          friend.statusType = sType;
          friend.statusText = sText;
        }
        
        // If it's myself, update my own status state
        if (uId === this.userId) {
          this.myStatusType = sType;
          this.myStatusText = sText;
        }
        this.$forceUpdate();
      }
      else if (packet.command === 45) { // AUDIT_CONFIG_RESPONSE
        if (packet.success !== false) {
          this.auditPolicy = packet.policy || 'BLOCK';
          this.sensitiveWords = packet.sensitiveWords || [];
          console.log(`[Audit Sync] Received sync: policy=${this.auditPolicy}, words=${this.sensitiveWords}`);
          uni.showToast({ title: '安全审核策略同步成功', icon: 'success' });
        }
      }
      else if (packet.command === 47) { // CUSTOM_DESK_RESPONSE
        const fromUserId = packet.fromUserId || 'service_bot';
        const deskType = packet.deskType; // 'faq_card', 'text', 'rating_card', 'agent_transfer'
        const content = packet.content;
        const msgId = packet.msgId || ('desk_' + Date.now());
        
        console.log(`[Desk] Received desk response: type=${deskType}, from=${fromUserId}`);
        
        // Ensure the service_bot session exists
        let session = this.sessions.find(s => s.id === 'service_bot');
        if (!session) {
          session = {
            id: 'service_bot',
            name: '智能客服',
            type: 'user',
            lastMsg: '客服回复',
            time: this.formatTime(new Date()),
            unread: 0
          };
          this.sessions.unshift(session);
        }
        
        if (!this.messages['service_bot']) this.messages['service_bot'] = [];
        
        const deskMsg = {
          msgId: msgId,
          sender: 'service_bot',
          senderName: packet.agentName || '智能客服',
          content: content,
          msgType: 6,
          time: this.formatTime(new Date()),
          createTimeMs: Date.now(),
          isSelf: false,
          ratingSubmitted: false
        };
        
        this.messages['service_bot'].push(deskMsg);
        session.lastMsg = deskType === 'rating_card' ? '⭐ 请为本次服务打分' :
                          deskType === 'agent_transfer' ? '🧑‍💼 已为您转接人工客服' : '🤖 收到新消息';
        session.time = this.formatTime(new Date());
        
        if (!this.currentSession || this.currentSession.id !== 'service_bot') {
          session.unread++;
          
          // Show browser notification if permitted
          if (typeof Notification !== 'undefined' && Notification.permission === 'granted') {
            new Notification('💬 智能客服回复', {
              body: deskType === 'rating_card' ? '服务结束，请为本次体验打分' : '您有新的客服消息',
              icon: 'https://img.icons8.com/color/48/chat.png'
            });
          }
        } else {
          setTimeout(() => this.scrollToBottom && this.scrollToBottom(), 50);
        }
        
        // Handle agent transfer notification for DevConsole
        if (deskType === 'agent_transfer' && packet.agentId && this.$refs.devConsole) {
          const agentConsole = this.$refs.devConsole;
          if (agentConsole.currentAgentId === packet.agentId) {
            agentConsole.addIncomingCustomer({ userId: this.userId, userName: this.username });
          }
        }
        
        this.saveData();
        this.$forceUpdate();
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
      if (this.currentSession && this.$refs.tuiChat) {
        this.currentSession.draft = this.$refs.tuiChat.localMsgContent || '';
      }

      this.currentSession = session;
      session.unread = 0;
      this.view = 'chat';
      this.scrollToBottom();
      
      if (this.typingTimer) {
        clearTimeout(this.typingTimer);
        this.typingTimer = null;
      }
      this.peerTyping = false;

      // Handle service_bot session specially
      if (session.id === 'service_bot') {
        // If no messages yet, send initial welcome request
        if (!this.messages['service_bot'] || this.messages['service_bot'].length === 0) {
          this.messages['service_bot'] = [];
          this.send({
            command: 46, // CUSTOM_DESK_REQUEST
            fromUserId: this.userId,
            action: 'get_welcome'
          });
        }
        this.$nextTick(() => this.scrollToBottom());
        return;
      }

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

      this.$nextTick(() => {
        session.draft = '';
        this.saveData();
      });
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
                quoteContent: m.quoteContent,
                reactions: m.reactions || {}
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
      
      // If talking to service_bot, route through DeskRequestPacket
      if (this.currentSession && this.currentSession.id === 'service_bot') {
        this.handleSendDeskAction({ action: 'ask_robot', robotMsg: content });
        // Add local self echo message
        if (!this.messages['service_bot']) this.messages['service_bot'] = [];
        this.messages['service_bot'].push({
          msgId: 'local_' + Date.now(),
          sender: this.userId,
          senderName: this.username,
          content: content,
          msgType: 1,
          time: this.formatTime(new Date()),
          createTimeMs: Date.now(),
          isSelf: true
        });
        this.$forceUpdate();
        return;
      }
      
      this.doSend(content, 1);
    },

    handleSendEditRequestEvent({ msg, newContent }) {
      this.sendEditRequest(msg, newContent);
    },

    handleSendDeskAction({ action, robotMsg, score, feedback, msgId }) {
      if (!this.userId) return;
      console.log(`[Desk] Sending desk action: ${action}`);
      this.send({
        command: 46, // CUSTOM_DESK_REQUEST
        fromUserId: this.userId,
        action: action,
        message: robotMsg || '',
        score: score || 0,
        feedback: feedback || '',
        refMsgId: msgId || ''
      });
    },

    handleAgentLogin({ agentId, agentName }) {
      console.log(`[Agent] Simulating agent login: ${agentId} (${agentName})`);
      // In a real system this would call a REST API to register the agent as available
      // For demo, we just log the action
    },

    handleAgentSendReply({ agentId, toUserId, message }) {
      // Send normal message but from agent ID to customer user ID
      // In simulation, the agent types in the DevConsole and their message is sent via WebSocket
      this.send({
        command: 3, // MESSAGE_REQUEST
        toUserId: toUserId,
        fromUserId: agentId,
        message: message,
        msgType: 1
      });
    },

    handleAgentEndSession({ agentId, customerId }) {
      // Signal end of session via desk request
      this.send({
        command: 46, // CUSTOM_DESK_REQUEST
        fromUserId: customerId,
        action: 'end_session',
        agentId: agentId
      });
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

    simTriggerPush({ toUserId, senderName, message, msgType = 1 }) {
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
          msgType: msgType
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
              friend.statusType = res.data.statusType || 'online';
              friend.statusText = res.data.statusText || '';
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

    handleUpdateStatus({ statusType, statusText }) {
      this.send({
        command: 42, // USER_STATUS_REQUEST
        userId: this.userId,
        statusType,
        statusText
      });
      console.log(`[Status] Sent status update: ${statusType} - ${statusText}`);
    },

    handleSyncAudit({ policy, sensitiveWords }) {
      this.send({
        command: 44, // AUDIT_CONFIG_REQUEST
        policy,
        sensitiveWords
      });
      console.log(`[Audit] Sent sync audit request. Policy: ${policy}`);
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
    },

    handleTogglePin(session) {
      session.isPinned = !session.isPinned;
      this.saveData();
    },

    handleSendReactionEvent(reactionData) {
      const packet = {
        command: 30,
        msgId: reactionData.msgId,
        emoji: reactionData.emoji,
        action: reactionData.action,
        toUserId: reactionData.toUserId,
        toGroupId: reactionData.toGroupId
      };
      this.send(packet);
    },

    handleInitiateCall({ type }) {
      if (!this.isConnected || !this.isLogin) {
        uni.showToast({ title: '未连接或未登录，无法呼叫', icon: 'none' });
        return;
      }
      if (!this.currentSession || this.currentSession.type !== 'user') return;
      
      const peerId = this.currentSession.id;
      const callId = 'call-' + Date.now();
      
      this.currentCall = {
        callId: callId,
        peerId: peerId,
        peerName: this.currentSession.name,
        callType: type,
        state: 'dialing'
      };

      this.send({
        command: 40, // VOICE_CALL_REQUEST
        callId: callId,
        fromUserId: this.userId,
        toUserId: peerId,
        callType: type
      });
      console.log(`[Call Signaling] Initiating ${type} call to ${peerId}. Call ID: ${callId}`);
    },

    handleCallAction(action) {
      if (!this.currentCall) return;
      const { callId, peerId } = this.currentCall;
      
      console.log(`[Call Signaling] Action selected: ${action} to ${peerId} for Call ID: ${callId}`);

      if (action === 'cancel') {
        this.send({
          command: 41, // VOICE_CALL_RESPONSE
          callId: callId,
          toUserId: peerId,
          fromUserId: this.userId,
          action: 'cancel'
        });
        this.currentCall = null;
      } else if (action === 'reject') {
        this.send({
          command: 41, // VOICE_CALL_RESPONSE
          callId: callId,
          toUserId: peerId,
          fromUserId: this.userId,
          action: 'reject'
        });
        this.currentCall = null;
      } else if (action === 'accept') {
        this.send({
          command: 41, // VOICE_CALL_RESPONSE
          callId: callId,
          toUserId: peerId,
          fromUserId: this.userId,
          action: 'accept'
        });
        this.currentCall.state = 'connected';
      } else if (action === 'hangup') {
        this.send({
          command: 41, // VOICE_CALL_RESPONSE
          callId: callId,
          toUserId: peerId,
          fromUserId: this.userId,
          action: 'hangup'
        });
        this.currentCall = null;
      }
    },

    // ================= RELATIONSHIPS METHODS (TUIContact) =================
    fetchRelationships() {
      if (!this.userId) return;
      console.log(`[Relationships] Fetching relationships for user: ${this.userId}`);
      
      // 1. Fetch friends list
      uni.request({
        url: `${this.serverHttpUrl}/api/external/friends?userId=${this.userId}`,
        method: 'GET',
        header: { 'Authorization': 'Bearer ' + this.externalApiKey },
        success: (res) => {
          if (res.statusCode === 200 && res.data.success) {
            this.friendList = res.data.friends.map(f => ({
              id: f.id,
              name: f.name || f.id,
              online: f.online,
              statusType: f.statusType || 'online',
              statusText: f.statusText || '',
              node: f.node || 'N/A'
            }));
            console.log('[Relationships] Friends list loaded:', this.friendList);
          }
        },
        fail: (err) => console.error('[Relationships] Failed to fetch friends list', err)
      });

      // 2. Fetch friend requests
      uni.request({
        url: `${this.serverHttpUrl}/api/external/friend_requests?userId=${this.userId}`,
        method: 'GET',
        header: { 'Authorization': 'Bearer ' + this.externalApiKey },
        success: (res) => {
          if (res.statusCode === 200 && res.data.success) {
            this.friendRequests = res.data.requests || [];
            console.log('[Relationships] Friend requests loaded:', this.friendRequests);
          }
        },
        fail: (err) => console.error('[Relationships] Failed to fetch friend requests', err)
      });

      // 3. Fetch blacklist
      uni.request({
        url: `${this.serverHttpUrl}/api/external/blacklist?userId=${this.userId}`,
        method: 'GET',
        header: { 'Authorization': 'Bearer ' + this.externalApiKey },
        success: (res) => {
          if (res.statusCode === 200 && res.data.success) {
            this.blacklistList = res.data.blacklist || [];
            console.log('[Relationships] Blacklist loaded:', this.blacklistList);
          }
        },
        fail: (err) => console.error('[Relationships] Failed to fetch blacklist', err)
      });
    },

    handleSendFriendRequest({ targetUserId, remark }) {
      if (!this.userId) return;
      this.send({
        command: 50, // FRIEND_ADD_REQUEST
        userId: this.userId,
        targetUserId: targetUserId,
        remark: remark,
        status: 'pending'
      });
      console.log(`[Relationships] Sent friend request to ${targetUserId}`);
    },

    handleFriendRequestApproval({ targetUserId, status }) {
      if (!this.userId) return;
      this.send({
        command: 50, // FRIEND_ADD_RESPONSE
        userId: targetUserId, // The original request sender is the target of response
        targetUserId: this.userId,
        status: status
      });
      console.log(`[Relationships] Sent friend request approval to ${targetUserId}: ${status}`);
      // Optimistically remove from list
      this.friendRequests = this.friendRequests.filter(req => req.userId !== targetUserId);
      setTimeout(() => this.fetchRelationships(), 1000);
    },

    handleRelationAction({ targetUserId, action }) {
      if (!this.userId) return;
      this.send({
        command: 52, // RELATION_ACTION_REQUEST
        action: action, // 'add_blacklist' | 'remove_blacklist'
        userId: this.userId,
        targetUserId: targetUserId
      });
      console.log(`[Relationships] Sent relation action: ${action} for ${targetUserId}`);
      setTimeout(() => this.fetchRelationships(), 800);
    },

    // ================= MEETINGS METHODS (TUIRoomKit) =================
    handleInitiateGroupCall() {
      if (!this.currentSession || this.currentSession.type !== 'group') return;
      const groupId = this.currentSession.id.substring(2);
      console.log(`[Meeting] Initiating group call in group ${groupId}`);
      this.send({
        command: 48, // MULTI_VOICE_CALL_REQUEST
        groupId: groupId,
        action: 'start',
        userId: this.userId,
        mute: false
      });
    },

    handleJoinGroupCall() {
      if (!this.currentSession || this.currentSession.type !== 'group') return;
      const groupId = this.currentSession.id.substring(2);
      console.log(`[Meeting] Joining group call in group ${groupId}`);
      this.send({
        command: 48, // MULTI_VOICE_CALL_REQUEST
        groupId: groupId,
        action: 'join',
        userId: this.userId,
        mute: false
      });
    },

    handleMeetingToggleMute(isMuted) {
      if (!this.meetingGroupId) return;
      console.log(`[Meeting] Toggle mute to ${isMuted} in meeting ${this.meetingGroupId}`);
      this.send({
        command: 48, // MULTI_VOICE_CALL_REQUEST
        groupId: this.meetingGroupId,
        action: 'toggle_mute',
        userId: this.userId,
        mute: isMuted
      });
    },

    handleMeetingLeave() {
      if (!this.meetingGroupId) return;
      console.log(`[Meeting] Leaving meeting ${this.meetingGroupId}`);
      this.send({
        command: 48, // MULTI_VOICE_CALL_REQUEST
        groupId: this.meetingGroupId,
        action: 'leave',
        userId: this.userId
      });
      this.meetingOverlayVisible = false;
      this.meetingMembers = [];
    }
  }
}
</script>

<style>
  @import url('https://fonts.googleapis.com/css2?family=Grand+Hotel&family=Plus+Jakarta+Sans:wght@300;400;500;600;700;800&display=swap');

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
    font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
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
    --bg-app: #fafafa;
    --bg-board: #ffffff;
    --bg-sidebar: #ffffff;
    --bg-middle: #ffffff;
    --bg-chat-viewport: #ffffff;
    --bg-chat-header: #ffffff;
    --border-color: #dbdbdb;
    --text-title: #262626;
    --text-desc: #737373;
    --text-primary: #262626;
    --input-bg: #efefef;
    --input-placeholder: #737373;
    --msg-self-bg: linear-gradient(135deg, #3897f0 0%, #a800e6 60%, #cc2366 100%);
    --msg-self-text: #ffffff;
    --msg-other-bg: #efefef;
    --msg-other-text: #262626;
    --msg-time: #8e8e8e;
    --scroll-thumb: #cccccc;
    --active-blue: #0095f6;
    --active-green: #78de45;
    --btn-hover: #fafafa;
    --btn-active: #f2f2f2;
    --accent-cyan: #0095f6;
    --terminal-bg: #f5f5f5;
    --terminal-text: #262626;
    --shadow-premium: none;
    --shadow-modal: 0 4px 12px rgba(0, 0, 0, 0.15);
    
    /* Instagram specific values */
    --ins-gradient: linear-gradient(45deg, #f09433 0%, #e6683c 25%, #dc2743 50%, #cc2366 75%, #bc1888 100%);
    --ins-bubble-gradient: linear-gradient(135deg, #3897f0 0%, #a800e6 60%, #cc2366 100%);
  }

  .app-container.theme-dark {
    --bg-app: #000000;
    --bg-board: #000000;
    --bg-sidebar: #000000;
    --bg-middle: #000000;
    --bg-chat-viewport: #000000;
    --bg-chat-header: #000000;
    --border-color: #262626;
    --text-title: #f5f5f5;
    --text-desc: #a8a8a8;
    --text-primary: #f5f5f5;
    --input-bg: #121212;
    --input-placeholder: #737373;
    --msg-self-bg: linear-gradient(135deg, #3897f0 0%, #a800e6 60%, #cc2366 100%);
    --msg-self-text: #ffffff;
    --msg-other-bg: #262626;
    --msg-other-text: #f5f5f5;
    --msg-time: #737373;
    --scroll-thumb: #262626;
    --active-blue: #0095f6;
    --active-green: #78de45;
    --btn-hover: #121212;
    --btn-active: #262626;
    --accent-cyan: #0095f6;
    --terminal-bg: #121212;
    --terminal-text: #f5f5f5;
    --shadow-premium: none;
    --shadow-modal: 0 4px 12px rgba(0, 0, 0, 0.5);
    
    /* Instagram specific values */
    --ins-gradient: linear-gradient(45deg, #f09433 0%, #e6683c 25%, #dc2743 50%, #cc2366 75%, #bc1888 100%);
    --ins-bubble-gradient: linear-gradient(135deg, #3897f0 0%, #a800e6 60%, #cc2366 100%);
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
    border-radius: 8px;
    padding: 30px;
    box-shadow: var(--shadow-modal);
    display: flex;
    flex-direction: column;
    transition: background-color 0.3s ease, border-color 0.3s ease;
  }

  .login-header {
    text-align: center;
    margin-bottom: 24px;
  }

  .logo-badge {
    font-size: 40px;
    margin-bottom: 8px;
    display: inline-block;
    filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.15));
  }

  .login-title {
    display: block;
    font-family: 'Grand Hotel', 'Plus Jakarta Sans', cursive;
    font-size: 48px;
    font-weight: 500;
    background: var(--ins-gradient);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    margin-bottom: 6px;
    letter-spacing: 0.5px;
  }

  .login-subtitle {
    display: block;
    font-size: 13px;
    color: var(--text-desc);
    line-height: 1.4;
  }

  .login-tab-header {
    display: flex;
    background: var(--btn-active);
    border-radius: 6px;
    padding: 2px;
    margin-bottom: 20px;
    border: 1px solid var(--border-color);
  }

  .login-tab {
    flex: 1;
    text-align: center;
    font-size: 13px;
    padding: 8px;
    color: var(--text-desc);
    border-radius: 4px;
    cursor: pointer;
    transition: all 0.2s ease;
    font-weight: 500;
  }

  .login-tab.active {
    background: var(--bg-board);
    color: var(--text-primary);
    font-weight: 600;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  }

  .login-form {
    display: flex;
    flex-direction: column;
    margin-bottom: 16px;
  }

  .form-fade {
    animation: slideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  }

  .glass-input-group {
    display: flex;
    align-items: center;
    background: var(--btn-hover);
    border: 1px solid var(--border-color);
    border-radius: 4px;
    padding: 0 12px;
    height: 40px;
    margin-bottom: 12px;
    transition: all 0.2s ease;
  }
  .glass-input-group:focus-within {
    border-color: var(--text-desc);
    background: var(--bg-board);
  }

  .input-icon {
    font-size: 15px;
    margin-right: 8px;
    color: var(--text-desc);
  }

  .glass-input {
    flex: 1;
    background: transparent;
    border: none;
    outline: none;
    color: var(--text-primary);
    font-size: 14px;
    width: 100%;
  }
  .glass-input::placeholder {
    color: var(--input-placeholder);
  }

  .input-tip-box {
    font-size: 11px;
    color: var(--text-desc);
    line-height: 1.4;
    margin-bottom: 16px;
    padding: 0 2px;
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
    background: var(--ins-gradient);
    color: #ffffff;
    border: none;
    border-radius: 4px;
    font-size: 14px;
    height: 40px;
    line-height: 40px;
    font-weight: 700;
    cursor: pointer;
    text-align: center;
    transition: all 0.2s ease;
    width: 100%;
    box-shadow: none;
  }
  .glass-btn:hover {
    filter: brightness(1.06);
    transform: translateY(-0.5px);
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

  .sidebar-ins-logo {
    padding: 10px 0;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 100%;
  }
  .ins-logo-text {
    display: none;
    font-family: 'Grand Hotel', cursive;
    font-size: 26px;
    background: var(--ins-gradient);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }
  .ins-logo-icon {
    font-size: 26px;
    filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.1));
    transition: transform 0.3s ease;
  }
  .sidebar-ins-logo:hover .ins-logo-icon {
    transform: rotate(-10deg) scale(1.1);
  }

  .user-avatar-wrapper {
    position: relative;
    cursor: pointer;
  }

  /* Three-layer Instagram Stories avatar ring */
  .ins-my-profile {
    width: 48px;
    height: 48px;
    border-radius: 50%;
    background: var(--ins-gradient);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 2.5px;
    box-sizing: border-box;
    transition: transform 0.25s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  }
  .ins-my-profile:hover {
    transform: scale(1.08);
  }
  .ins-my-profile .user-avatar-gradient {
    width: 100%;
    height: 100%;
    border-radius: 50%;
    background: var(--bg-sidebar); /* Outer spacing ring of the background */
    border: none;
    display: flex;
    align-items: center;
    justify-content: center;
    box-sizing: border-box;
    padding: 2px;
  }
  .ins-my-profile .avatar-letter {
    width: 100%;
    height: 100%;
    background: linear-gradient(135deg, #3897f0 0%, #a800e6 100%);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #ffffff;
    font-weight: 800;
    font-size: 13px;
    box-shadow: inset 0 1px 2px rgba(0,0,0,0.1);
  }

  .online-indicator {
    position: absolute;
    bottom: 0px;
    right: 0px;
    width: 12px;
    height: 12px;
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
    border-radius: 8px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    position: relative;
    transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
    color: var(--text-desc);
  }
  .nav-item:hover {
    background: var(--btn-hover);
    color: var(--text-primary);
    transform: scale(1.06);
  }
  .nav-item.active {
    background: transparent;
    color: var(--text-primary);
  }
  .nav-item.active .nav-icon {
    transform: scale(1.1);
    filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.08));
  }

  .nav-icon {
    font-size: 24px;
    transition: transform 0.2s ease;
  }
  .nav-label {
    display: none;
  }

  .unread-badge-mini {
    position: absolute;
    top: 2px;
    right: 2px;
    background: var(--active-blue); /* Ins Direct unread count style */
    color: #ffffff;
    font-size: 10px;
    font-weight: 800;
    min-width: 15px;
    height: 15px;
    line-height: 15px;
    text-align: center;
    border-radius: 50%;
    padding: 0;
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
