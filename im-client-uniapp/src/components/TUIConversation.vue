<template>
  <div class="full-height flex-col" style="position: relative;">
    <div class="sidebar-header">
      <span class="sidebar-title">Chats</span>
      <div class="header-actions">
        <div class="circle-action-btn" @click="$emit('show-start-chat')" title="发起私聊">💬</div>
        <div class="circle-action-btn" @click="$emit('show-create-group')" title="群组操作">+</div>
      </div>
    </div>
    <div class="search-box">
      <input class="glass-search" v-model="searchQuery" placeholder="搜索会话..." />
    </div>

    <!-- Instagram Stories Horizontal scroll rail -->
    <div class="ins-stories-rail">
      <div class="active-friend-item" v-for="friend in friendList" :key="friend.id" @click="$emit('start-chat-with', friend.id)">
        <div class="ins-story-avatar-ring" :class="{ 'has-story': friend.online }">
          <div class="ins-story-avatar-spacer">
            <div class="ins-story-avatar-inner font-bold">
              {{ friend.name[0].toUpperCase() }}
            </div>
          </div>
          <span v-if="friend.online" class="online-indicator-dot" :class="friend.statusType || 'online'"></span>
        </div>
        <span class="active-friend-name">{{ friend.name.split(' ')[0] }}</span>
      </div>
    </div>

    <div class="list-scroll-view">
      <!-- Always-pinned 智能客服 entry -->
      <div id="session-card-service-bot"
          class="session-glass-card service-bot-card"
          :class="{ active: currentSession && currentSession.id === 'service_bot' }"
          @click="$emit('open-chat', { id: 'service_bot', name: '智能客服', type: 'user' })">
        <div class="ins-story-avatar-ring has-story service-bot-ring">
          <div class="ins-story-avatar-spacer">
            <div class="ins-story-avatar-inner font-bold service-bot-avatar">
              🤖
            </div>
          </div>
        </div>
        <div class="session-info">
          <div class="session-header-row">
            <span class="session-name">
              智能客服
              <span class="service-bot-badge">AI</span>
            </span>
            <span class="session-time">24/7</span>
          </div>
          <div class="session-body-row">
            <span class="session-preview">点我开始咨询 👋</span>
          </div>
        </div>
      </div>

      <div v-if="filteredSessions.filter(s => s.id !== 'service_bot').length === 0 && sessions.length === 0" class="empty-list-tip">暂无其他活动会话</div>
      <div v-for="(session, index) in filteredSessions.filter(s => s.id !== 'service_bot')" :key="index" 
          class="session-glass-card" :class="{ active: currentSession && currentSession.id === session.id, pinned: session.isPinned }"
          @click="$emit('open-chat', session)"
          @contextmenu.prevent="openSessionContextMenu($event, session)">
        <!-- Stories-style Avatar Ring for Sessions -->
        <div class="ins-story-avatar-ring session-avatar-ring" :class="{ 'has-story': isOnline(session) }">
          <div class="ins-story-avatar-spacer">
            <div class="ins-story-avatar-inner font-bold">
              {{ session.name[0].toUpperCase() }}
            </div>
          </div>
          <span class="online-indicator" v-if="isOnline(session)" :class="getStatusType(session) || 'online'"></span>
        </div>
        <div class="session-info">
          <div class="session-header-row">
            <span class="session-name">
              {{ session.name }}
              <span v-if="isOnline(session) && getStatusText(session)" class="session-signature-tag animate-pop" :title="getStatusText(session)">
                {{ getStatusText(session) }}
              </span>
              <span v-if="session.isPinned" style="font-size: 11px; margin-left: 4px;" title="置顶会话">📌</span>
            </span>
            <span class="session-time">{{ session.time }}</span>
          </div>
          <div class="session-body-row">
            <span class="session-preview" v-if="session.draft">
              <span style="color: #ff9f0a; font-weight: 600;">[草稿] </span>
              <span style="color: var(--text-title);">{{ session.draft }}</span>
            </span>
            <span class="session-preview" v-else>{{ session.lastMsg }}</span>
            <!-- Instagram classic unread blue dot -->
            <div v-if="session.unread > 0" class="ins-unread-blue-dot" title="未读消息"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- ================= SESSION CONTEXT MENU ================= -->
    <div v-if="contextMenu.show && contextMenu.session" class="custom-context-menu" 
         :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }">
      <div class="context-menu-item" @click="togglePin(contextMenu.session)">
        {{ contextMenu.session.isPinned ? '📌 取消置顶' : '📌 置顶该会话' }}
      </div>
    </div>

    <!-- Floating Action Button (Messenger Style) -->
    <div class="messenger-fab-container" :class="{ active: showFabMenu }" @mouseleave="$emit('update:showFabMenu', false)">
      <div class="fab-submenu">
        <div class="fab-submenu-item" @click="$emit('show-create-group'); $emit('update:showFabMenu', false)" title="群聊操作">
          <span class="fab-submenu-label">创建或加入群聊</span>
          <div class="fab-submenu-icon">👥</div>
        </div>
        <div class="fab-submenu-item" @click="$emit('show-start-chat'); $emit('update:showFabMenu', false)" title="发起私聊">
          <span class="fab-submenu-label">发起新私聊</span>
          <div class="fab-submenu-icon">💬</div>
        </div>
      </div>
      <div class="messenger-main-fab" @click="$emit('update:showFabMenu', !showFabMenu)" :title="showFabMenu ? '关闭菜单' : '新建聊天/群组'">
        <span class="main-fab-icon">+</span>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'TUIConversation',
  props: {
    sessions: {
      type: Array,
      default: () => []
    },
    currentSession: {
      type: Object,
      default: null
    },
    friendList: {
      type: Array,
      default: () => []
    },
    showFabMenu: {
      type: Boolean,
      default: false
    }
  },
  emits: [
    'open-chat',
    'show-create-group',
    'show-start-chat',
    'start-chat-with',
    'update:showFabMenu',
    'toggle-pin'
  ],
  data() {
    return {
      searchQuery: '',
      contextMenu: {
        show: false,
        x: 0,
        y: 0,
        session: null
      }
    };
  },
  computed: {
    filteredSessions() {
      let result = this.sessions;
      if (this.searchQuery) {
        const query = this.searchQuery.toLowerCase();
        result = this.sessions.filter(session => 
          session.name.toLowerCase().includes(query) || 
          (session.lastMsg && session.lastMsg.toLowerCase().includes(query))
        );
      }
      return [...result].sort((a, b) => {
        const aPinned = a.isPinned ? 1 : 0;
        const bPinned = b.isPinned ? 1 : 0;
        if (aPinned !== bPinned) return bPinned - aPinned;
        return 0;
      });
    }
  },
  methods: {
    openSessionContextMenu(e, session) {
      if (!session) return;
      e.preventDefault();
      
      this.contextMenu = {
        show: true,
        x: e.clientX || 200,
        y: e.clientY || 200,
        session: session
      };
      
      const closeMenu = () => {
        this.contextMenu.show = false;
        document.removeEventListener('click', closeMenu);
      };
      setTimeout(() => {
        document.addEventListener('click', closeMenu);
      }, 10);
    },
    togglePin(session) {
      this.$emit('toggle-pin', session);
      this.contextMenu.show = false;
    },
    getMatchingFriend(session) {
      if (session.type !== 'user') return null;
      return this.friendList.find(f => f.id === session.id);
    },
    isOnline(session) {
      if (session.type !== 'user') return true; // Groups are always online in this UI
      const f = this.getMatchingFriend(session);
      return f ? f.online : false;
    },
    getStatusType(session) {
      if (session.type !== 'user') return 'online';
      const f = this.getMatchingFriend(session);
      return f ? (f.statusType || 'online') : 'offline';
    },
    getStatusText(session) {
      if (session.type !== 'user') return '';
      const f = this.getMatchingFriend(session);
      return f ? f.statusText : '';
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

  /* Instagram Stories Horizontal scroll rail */
  .ins-stories-rail {
    display: flex;
    gap: 16px;
    padding: 10px 20px 16px 20px;
    overflow-x: auto;
    border-bottom: 1px solid var(--border-color);
    margin-bottom: 8px;
    flex-shrink: 0;
  }
  .ins-stories-rail::-webkit-scrollbar {
    display: none;
  }

  .active-friend-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
    cursor: pointer;
    width: 60px;
    flex-shrink: 0;
  }

  /* 3-layer Instagram Stories avatar ring */
  .ins-story-avatar-ring {
    position: relative;
    width: 52px;
    height: 52px;
    border-radius: 50%;
    background: transparent;
    display: flex;
    align-items: center;
    justify-content: center;
    box-sizing: border-box;
    transition: transform 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
    padding: 2.5px;
  }
  .ins-story-avatar-ring.has-story {
    background: var(--ins-gradient);
  }
  .ins-story-avatar-spacer {
    width: 100%;
    height: 100%;
    border-radius: 50%;
    background: var(--bg-board); /* Spacer layer */
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 2px;
    box-sizing: border-box;
  }
  .ins-story-avatar-inner {
    width: 100%;
    height: 100%;
    border-radius: 50%;
    background: linear-gradient(135deg, #3897f0 0%, #a800e6 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    color: #ffffff;
    font-size: 13px;
    font-weight: 800;
    box-sizing: border-box;
    box-shadow: inset 0 1px 2px rgba(0,0,0,0.15);
  }
  .active-friend-item:hover .ins-story-avatar-ring {
    transform: scale(1.06);
  }
  .ins-story-avatar-ring.has-story:hover {
    animation: rotateStoryRing 4s linear infinite;
  }
  @keyframes rotateStoryRing {
    0% { filter: hue-rotate(0deg); }
    100% { filter: hue-rotate(360deg); }
  }

  .online-indicator-dot {
    position: absolute;
    bottom: 1px;
    right: 1px;
    width: 11px;
    height: 11px;
    border-radius: 50%;
    background: var(--active-green);
    border: 2px solid var(--bg-board);
    transition: all 0.3s ease;
  }
  .online-indicator-dot.busy { background: #ff9f0a; }
  .online-indicator-dot.away { background: #ffd60a; }
  .online-indicator-dot.offline { display: none; }

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

  /* Session Cards */
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
    transition: all 0.3s ease;
    border: 2px solid transparent;
  }

  /* Service Bot Avatar special styling */
  .service-bot-card {
    background: linear-gradient(135deg, rgba(0, 212, 170, 0.06) 0%, rgba(0, 132, 255, 0.06) 100%) !important;
    border: 1px solid rgba(0, 212, 170, 0.2) !important;
    margin: 4px 8px !important;
  }
  .service-bot-card:hover {
    background: linear-gradient(135deg, rgba(0, 212, 170, 0.12) 0%, rgba(0, 132, 255, 0.12) 100%) !important;
  }
  .service-bot-card.active {
    background: linear-gradient(135deg, rgba(0, 212, 170, 0.16) 0%, rgba(0, 132, 255, 0.16) 100%) !important;
    border-color: rgba(0, 212, 170, 0.4) !important;
  }

  .service-bot-avatar {
    background: linear-gradient(135deg, #00d4aa 0%, #0084ff 100%) !important;
    font-size: 22px;
    box-shadow: 0 0 16px rgba(0, 212, 170, 0.5), 0 0 32px rgba(0, 132, 255, 0.3);
    border-color: #00d4aa !important;
    animation: servicebot-glow 2.5s ease-in-out infinite;
  }
  @keyframes servicebot-glow {
    0%, 100% { box-shadow: 0 0 16px rgba(0, 212, 170, 0.5), 0 0 32px rgba(0, 132, 255, 0.3); }
    50% { box-shadow: 0 0 24px rgba(0, 212, 170, 0.8), 0 0 48px rgba(0, 132, 255, 0.5); }
  }

  .service-bot-glow-ring {
    position: absolute;
    inset: -4px;
    border-radius: 50%;
    border: 2px solid rgba(0, 212, 170, 0.4);
    animation: ring-pulse 2.5s ease-in-out infinite;
  }
  @keyframes ring-pulse {
    0%, 100% { opacity: 0.4; transform: scale(1); }
    50% { opacity: 0.9; transform: scale(1.06); }
  }

  .service-bot-badge {
    font-size: 9px;
    background: linear-gradient(135deg, #00d4aa, #0084ff);
    color: #fff;
    font-weight: 800;
    border-radius: 4px;
    padding: 1px 4px;
    letter-spacing: 0.5px;
  }

  .avatar-circle.online {
    box-shadow: 0 0 8px rgba(48, 209, 88, 0.5);
    border-color: #30d158;
  }
  .avatar-circle.busy {
    box-shadow: 0 0 8px rgba(255, 159, 10, 0.5);
    border-color: #ff9f0a;
  }
  .avatar-circle.away {
    box-shadow: 0 0 8px rgba(255, 214, 10, 0.5);
    border-color: #ffd60a;
  }
  .avatar-circle.offline {
    box-shadow: none;
    border-color: transparent;
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
    transition: all 0.3s ease;
  }

  .online-indicator.online {
    background: #30d158;
  }
  .online-indicator.busy {
    background: #ff9f0a;
  }
  .online-indicator.away {
    background: #ffd60a;
  }
  .online-indicator.offline {
    background: #bcc0c4;
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
    min-width: 0;
  }

  .session-name {
    font-size: 15px;
    font-weight: 600;
    color: var(--text-title);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    display: flex;
    align-items: center;
    gap: 6px;
    flex: 1;
    min-width: 0;
  }

  .session-signature-tag {
    font-size: 10px;
    color: #ffd60a;
    font-style: italic;
    background: rgba(255, 214, 10, 0.12);
    border: 1px solid rgba(255, 214, 10, 0.25);
    border-radius: 4px;
    padding: 1px 5px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 80px;
  }

  .session-time {
    font-size: 12px;
    color: var(--text-desc);
    margin-left: 8px;
    flex-shrink: 0;
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

  .ins-unread-blue-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: #0095f6;
    box-shadow: 0 0 6px #0095f6;
    margin-right: 8px;
    flex-shrink: 0;
    align-self: center;
  }

  /* Floating Action Button (Messenger Style) */
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

  /* Pinned and Context Menu Styles */
  .session-glass-card.pinned {
    background: rgba(0, 132, 255, 0.04);
    border-left: 3px solid var(--active-blue);
    border-top-left-radius: 0;
    border-bottom-left-radius: 0;
  }
  
  .custom-context-menu {
    position: fixed;
    z-index: 10000;
    min-width: 150px;
    background: var(--bg-sidebar);
    border: 1px solid var(--border-color);
    border-radius: 12px;
    box-shadow: var(--shadow-premium);
    backdrop-filter: blur(20px);
    padding: 6px;
    display: flex;
    flex-direction: column;
    gap: 2px;
    transform-origin: top left;
    animation: contextFadeIn 0.15s cubic-bezier(0.16, 1, 0.3, 1);
  }

  @keyframes contextFadeIn {
    from { opacity: 0; transform: scale(0.95); }
    to { opacity: 1; transform: scale(1); }
  }

  .context-menu-item {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 8px 12px;
    font-size: 14px;
    font-weight: 500;
    color: var(--text-primary);
    border-radius: 8px;
    cursor: pointer;
    transition: background-color 0.2s;
  }
  .context-menu-item:hover {
    background-color: var(--btn-hover);
  }

  .animate-pop {
    animation: pop 0.25s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  }
  @keyframes pop {
    0% { transform: scale(0.8); opacity: 0; }
    100% { transform: scale(1); opacity: 1; }
  }
</style>
