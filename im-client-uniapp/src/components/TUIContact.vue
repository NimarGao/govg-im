<template>
  <div class="full-height flex-col">
    <div class="sidebar-header">
      <span class="sidebar-title">Contacts</span>
    </div>
    <div class="search-box">
      <input class="glass-search" v-model="searchQuery" placeholder="搜索好友..." />
    </div>
    <div class="list-scroll-view">
      <div class="contact-section-title">我的好友列表</div>
      <div v-if="filteredFriends.length === 0" class="empty-list-tip">未找到匹配好友</div>
      <div v-for="friend in filteredFriends" :key="friend.id" class="contact-glass-card" @click="$emit('start-chat-with', friend.id)">
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
</template>

<script>
export default {
  name: 'TUIContact',
  props: {
    friendList: {
      type: Array,
      default: () => []
    }
  },
  emits: ['start-chat-with'],
  data() {
    return {
      searchQuery: ''
    };
  },
  computed: {
    filteredFriends() {
      if (!this.searchQuery) return this.friendList;
      const query = this.searchQuery.toLowerCase();
      return this.friendList.filter(friend => 
        friend.name.toLowerCase().includes(query) || 
        friend.id.toLowerCase().includes(query)
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
    background: #bcc0c4;
    border: 2px solid var(--bg-sidebar);
  }
  .online-indicator.active {
    background: var(--active-green);
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
</style>
