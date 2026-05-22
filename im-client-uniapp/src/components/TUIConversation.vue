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

    <!-- Horizontal Active Friends Scroll Rail (Facebook Messenger style) -->
    <div class="active-friends-rail">
      <div class="active-friend-item" v-for="friend in friendList" :key="friend.id" @click="$emit('start-chat-with', friend.id)">
        <div class="active-friend-avatar">
          <span class="friend-letter">{{ friend.name[0].toUpperCase() }}</span>
          <span class="online-indicator-dot" :class="{ active: friend.online }"></span>
        </div>
        <span class="active-friend-name">{{ friend.name.split(' ')[0] }}</span>
      </div>
    </div>

    <div class="list-scroll-view">
      <div v-if="filteredSessions.length === 0" class="empty-list-tip">暂无活动会话</div>
      <div v-for="(session, index) in filteredSessions" :key="index" 
          class="session-glass-card" :class="{ active: currentSession && currentSession.id === session.id }"
          @click="$emit('open-chat', session)">
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
    'update:showFabMenu'
  ],
  data() {
    return {
      searchQuery: ''
    };
  },
  computed: {
    filteredSessions() {
      if (!this.searchQuery) return this.sessions;
      const query = this.searchQuery.toLowerCase();
      return this.sessions.filter(session => 
        session.name.toLowerCase().includes(query) || 
        (session.lastMsg && session.lastMsg.toLowerCase().includes(query))
      );
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

  /* Active Friends Rail */
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
</style>
