<template>
  <div class="full-height flex-col instagram-contact-view">
    <!-- Header -->
    <div class="sidebar-header">
      <span class="sidebar-title">探索与联系人</span>
      <div v-if="friendRequests.length > 0" class="requests-badge-pill" @click="showRequestsDrawer = true">
        ⚡ {{ friendRequests.length }} 条申请
      </div>
    </div>

    <!-- Search Box -->
    <div class="search-box">
      <div class="search-input-wrapper">
        <span class="search-icon">🔍</span>
        <input class="instagram-search" v-model="searchQuery" placeholder="搜索已关注好友..." />
      </div>
    </div>

    <!-- Add Friend Panel (Minimalist Row) -->
    <div class="add-friend-section">
      <span class="section-micro-title">添加新好友</span>
      <div class="add-friend-input-row">
        <input class="instagram-mini-input" v-model="newFriendId" placeholder="输入用户 ID..." @keyup.enter="sendFriendRequest" />
        <input class="instagram-mini-input remark" v-model="newFriendRemark" placeholder="附加信息..." @keyup.enter="sendFriendRequest" />
        <button class="instagram-btn-gradient" @click="sendFriendRequest" :disabled="!newFriendId">
          发送申请
        </button>
      </div>
    </div>

    <!-- Main List Scroll Area -->
    <div class="list-scroll-view">
      
      <!-- Friend Requests Section (If any and not opened in drawer, we also show it here) -->
      <div v-if="friendRequests.length > 0" class="requests-quick-entry animate-pop" @click="showRequestsDrawer = true">
        <div class="stacked-avatars">
          <div v-for="(req, index) in friendRequests.slice(0, 3)" :key="index" class="stacked-avatar">
            {{ req.username ? req.username[0].toUpperCase() : 'U' }}
          </div>
        </div>
        <div class="requests-entry-text">
          <span class="bold-text">收到好友申请</span>
          <span class="desc-text">{{ friendRequests.length }} 个人想要添加你为好友</span>
        </div>
        <span class="chevron-right">❯</span>
      </div>

      <!-- Friends List -->
      <div class="contact-section-header">
        <span>全部联系人 ({{ filteredFriends.length }})</span>
      </div>
      
      <div v-if="filteredFriends.length === 0" class="empty-list-tip">
        <span class="empty-icon">👥</span>
        <span>未找到匹配的好友</span>
      </div>

      <div v-for="friend in filteredFriends" :key="friend.id" class="contact-instagram-card" @click="$emit('start-chat-with', friend.id)">
        <!-- Avatar with Instagram Gradient Border for Online, else Simple Gray -->
        <div class="avatar-wrapper" :class="{ 'has-story': friend.online }">
          <div class="avatar-circle font-bold">
            {{ friend.name[0].toUpperCase() }}
          </div>
          <span class="online-dot" :class="[friend.online ? (friend.statusType || 'online') : 'offline']"></span>
        </div>

        <div class="contact-info">
          <div class="contact-name-row">
            <span class="contact-name">{{ friend.name }}</span>
            <span v-if="friend.online && friend.statusText" class="status-badge-ins animate-pop">
              {{ friend.statusText }}
            </span>
          </div>
          <span class="contact-status" :class="{ online: friend.online }">
            {{ friend.online ? translateStatusType(friend.statusType) + ' • ' + friend.node : '离线' }}
          </span>
        </div>

        <div class="action-buttons-row">
          <button class="action-mini-btn start-chat" @click.stop="$emit('start-chat-with', friend.id)" title="发消息">
            💬
          </button>
          <button class="action-mini-btn block-user" @click.stop="confirmBlock(friend)" title="拉黑">
            🚫
          </button>
        </div>
      </div>

      <!-- Blacklist Accordion -->
      <div class="blacklist-collapsible">
        <div class="blacklist-header" @click="showBlacklist = !showBlacklist">
          <span class="title">🚫 已拉黑名单 ({{ blacklistList.length }})</span>
          <span class="arrow" :class="{ rotated: showBlacklist }">❯</span>
        </div>
        
        <div v-if="showBlacklist" class="blacklist-content animate-slide-down">
          <div v-if="blacklistList.length === 0" class="empty-blacklist-tip">
            暂无拉黑用户
          </div>
          <div v-for="blacked in blacklistList" :key="blacked.id" class="blacklist-item">
            <div class="blacklist-avatar">
              {{ blacked.name ? blacked.name[0].toUpperCase() : (blacked.id ? blacked.id[0].toUpperCase() : '?') }}
            </div>
            <div class="blacklist-info">
              <span class="name">{{ blacked.name || blacked.id }}</span>
              <span class="id">ID: {{ blacked.id }}</span>
            </div>
            <button class="unblock-btn" @click.stop="unblockUser(blacked.id)">
              移出黑名单
            </button>
          </div>
        </div>
      </div>

    </div>

    <!-- ================= INSTAGRAM-STYLE REQUESTS DRAWER ================= -->
    <div v-if="showRequestsDrawer" class="requests-drawer-backdrop animate-fade-in" @click.self="showRequestsDrawer = false">
      <div class="requests-drawer animate-slide-left">
        <div class="drawer-header">
          <button class="close-drawer-btn" @click="showRequestsDrawer = false">❮</button>
          <span class="drawer-title">好友申请列表</span>
          <span class="requests-count-pill">{{ friendRequests.length }}</span>
        </div>

        <div class="drawer-scroll-content">
          <div v-if="friendRequests.length === 0" class="drawer-empty-state">
            <span class="star-icon">✨</span>
            <span>已经处理完所有的申请</span>
          </div>

          <div v-for="req in friendRequests" :key="req.userId" class="request-instagram-card animate-pop">
            <div class="req-avatar">
              {{ req.username ? req.username[0].toUpperCase() : 'U' }}
            </div>
            <div class="req-details">
              <div class="req-user-row">
                <span class="req-name">{{ req.username || req.userId }}</span>
                <span class="req-id">ID: {{ req.userId }}</span>
              </div>
              <div v-if="req.remark" class="req-remark-bubble">
                “{{ req.remark }}”
              </div>
            </div>
            
            <div class="req-actions">
              <button class="req-btn accept animate-pop" @click="approveRequest(req, 'accepted')">
                同意
              </button>
              <button class="req-btn reject animate-pop" @click="approveRequest(req, 'rejected')">
                拒绝
              </button>
            </div>
          </div>
        </div>
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
    },
    friendRequests: {
      type: Array,
      default: () => []
    },
    blacklistList: {
      type: Array,
      default: () => []
    }
  },
  emits: ['start-chat-with', 'send-friend-request', 'friend-request-approval', 'relation-action'],
  data() {
    return {
      searchQuery: '',
      newFriendId: '',
      newFriendRemark: '',
      showBlacklist: false,
      showRequestsDrawer: false
    };
  },
  computed: {
    filteredFriends() {
      if (!this.searchQuery) return this.friendList;
      const query = this.searchQuery.toLowerCase();
      return this.friendList.filter(friend => 
        (friend.name && friend.name.toLowerCase().includes(query)) || 
        (friend.id && friend.id.toLowerCase().includes(query))
      );
    }
  },
  methods: {
    translateStatusType(type) {
      if (type === 'online') return '在线';
      if (type === 'busy') return '忙碌';
      if (type === 'away') return '离开';
      return '在线';
    },
    sendFriendRequest() {
      if (!this.newFriendId.trim()) return;
      this.$emit('send-friend-request', {
        targetUserId: this.newFriendId.trim(),
        remark: this.newFriendRemark.trim()
      });
      this.newFriendId = '';
      this.newFriendRemark = '';
      uni.showToast({ title: '好友申请已发出', icon: 'success' });
    },
    approveRequest(req, status) {
      this.$emit('friend-request-approval', {
        targetUserId: req.userId,
        status: status
      });
      uni.showToast({ 
        title: status === 'accepted' ? '已同意申请' : '已拒绝申请', 
        icon: 'none' 
      });
    },
    confirmBlock(friend) {
      uni.showModal({
        title: '拉黑好友',
        content: `确定拉黑并断开与 ${friend.name} 的联系吗？`,
        confirmColor: '#e6683c',
        success: (res) => {
          if (res.confirm) {
            this.$emit('relation-action', {
              targetUserId: friend.id,
              action: 'add_blacklist'
            });
            uni.showToast({ title: '已加入黑名单', icon: 'none' });
          }
        }
      });
    },
    unblockUser(userId) {
      this.$emit('relation-action', {
        targetUserId: userId,
        action: 'remove_blacklist'
      });
      uni.showToast({ title: '已移出黑名单', icon: 'none' });
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
  
  /* Instagram Contact Aesthetics */
  .instagram-contact-view {
    background: var(--bg-sidebar);
    color: var(--text-title);
    border-right: 1px solid var(--border-color, rgba(255,255,255,0.06));
    box-sizing: border-box;
  }

  .sidebar-header {
    padding: 24px 20px 12px 20px;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  .sidebar-title {
    font-size: 22px;
    font-weight: 800;
    letter-spacing: -0.8px;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
  }

  .requests-badge-pill {
    background: linear-gradient(45deg, #f09433 0%, #e6683c 25%, #dc2743 50%, #cc2366 75%, #bc1888 100%);
    color: #ffffff;
    font-size: 11px;
    font-weight: 700;
    padding: 4px 10px;
    border-radius: 14px;
    cursor: pointer;
    box-shadow: 0 4px 10px rgba(220, 39, 67, 0.35);
    transition: transform 0.2s ease;
  }
  .requests-badge-pill:hover {
    transform: scale(1.05);
  }

  /* Search bar (Instagram minimalist pill) */
  .search-box {
    padding: 4px 20px 12px 20px;
  }
  .search-input-wrapper {
    position: relative;
    display: flex;
    align-items: center;
  }
  .search-icon {
    position: absolute;
    left: 12px;
    font-size: 13px;
    opacity: 0.6;
  }
  .instagram-search {
    width: 100%;
    height: 36px;
    border-radius: 10px;
    background: var(--input-bg);
    border: 1px solid rgba(255, 255, 255, 0.05);
    outline: none;
    padding: 0 12px 0 34px;
    color: var(--text-title);
    font-size: 14px;
    box-sizing: border-box;
    transition: all 0.2s ease;
  }
  .instagram-search:focus {
    background: rgba(255,255,255,0.08);
    border-color: rgba(230, 104, 60, 0.5);
  }
  .instagram-search::placeholder {
    color: var(--input-placeholder);
    opacity: 0.8;
  }

  /* Add Friend minimalistic section */
  .add-friend-section {
    padding: 10px 20px 16px 20px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
    display: flex;
    flex-direction: column;
    gap: 8px;
  }
  .section-micro-title {
    font-size: 11px;
    font-weight: 700;
    color: var(--text-desc);
    text-transform: uppercase;
    letter-spacing: 0.8px;
  }
  .add-friend-input-row {
    display: flex;
    gap: 8px;
    align-items: center;
  }
  .instagram-mini-input {
    flex: 1;
    height: 32px;
    border-radius: 6px;
    background: var(--bg-chat-header, rgba(255,255,255,0.03));
    border: 1px solid rgba(255, 255, 255, 0.08);
    color: var(--text-title);
    font-size: 13px;
    padding: 0 10px;
    outline: none;
    box-sizing: border-box;
  }
  .instagram-mini-input.remark {
    flex: 1.2;
  }
  .instagram-mini-input:focus {
    border-color: rgba(230, 104, 60, 0.4);
  }
  .instagram-btn-gradient {
    background: linear-gradient(45deg, #f09433, #dc2743, #cc2366);
    color: #ffffff;
    border: none;
    outline: none;
    padding: 0 14px;
    height: 32px;
    font-size: 12px;
    font-weight: 700;
    border-radius: 6px;
    cursor: pointer;
    white-space: nowrap;
    transition: all 0.2s ease;
  }
  .instagram-btn-gradient:hover:not(:disabled) {
    opacity: 0.9;
    transform: translateY(-1px);
  }
  .instagram-btn-gradient:disabled {
    background: rgba(255,255,255,0.08) !important;
    color: rgba(255,255,255,0.3) !important;
    cursor: not-allowed;
  }

  .list-scroll-view {
    flex: 1;
    overflow-y: auto;
    padding-bottom: 20px;
  }

  /* Pending Requests entry */
  .requests-quick-entry {
    display: flex;
    align-items: center;
    padding: 12px 16px;
    margin: 12px 12px;
    background: linear-gradient(135deg, rgba(230, 104, 60, 0.08) 0%, rgba(204, 35, 102, 0.08) 100%);
    border: 1px solid rgba(230, 104, 60, 0.15);
    border-radius: 12px;
    cursor: pointer;
    transition: all 0.2s ease;
  }
  .requests-quick-entry:hover {
    transform: translateY(-2px);
    background: linear-gradient(135deg, rgba(230, 104, 60, 0.12) 0%, rgba(204, 35, 102, 0.12) 100%);
  }
  .stacked-avatars {
    display: flex;
    margin-right: 12px;
  }
  .stacked-avatar {
    width: 28px;
    height: 28px;
    border-radius: 50%;
    background: linear-gradient(135deg, #dc2743, #bc1888);
    color: #ffffff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 11px;
    font-weight: 850;
    border: 2px solid var(--bg-sidebar);
    margin-left: -8px;
  }
  .stacked-avatar:first-child {
    margin-left: 0;
  }
  .requests-entry-text {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 2px;
  }
  .requests-entry-text .bold-text {
    font-size: 13px;
    font-weight: 750;
    color: #e6683c;
  }
  .requests-entry-text .desc-text {
    font-size: 11px;
    color: var(--text-desc);
  }
  .chevron-right {
    font-size: 12px;
    opacity: 0.6;
    color: #e6683c;
  }

  .contact-section-header {
    font-size: 12px;
    font-weight: 700;
    color: var(--text-desc);
    padding: 16px 20px 8px 20px;
    letter-spacing: 0.5px;
  }

  .empty-list-tip {
    text-align: center;
    color: var(--text-desc);
    font-size: 13px;
    padding: 50px 20px;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
  }
  .empty-icon {
    font-size: 32px;
    opacity: 0.5;
  }

  /* Contact Instagram Style Card */
  .contact-instagram-card {
    display: flex;
    align-items: center;
    padding: 10px 16px;
    margin: 2px 10px;
    border-radius: 10px;
    cursor: pointer;
    transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
    gap: 12px;
  }
  .contact-instagram-card:hover {
    background: var(--btn-hover, rgba(255,255,255,0.04));
    transform: translateX(4px);
  }

  /* Stories-style colourful gradient ring for online, simple circle for offline */
  .avatar-wrapper {
    position: relative;
    padding: 2.5px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: transform 0.25s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  }
  .avatar-wrapper.has-story {
    background: linear-gradient(45deg, #f09433 0%, #e6683c 25%, #dc2743 50%, #cc2366 75%, #bc1888 100%);
  }
  .avatar-wrapper:hover {
    transform: scale(1.06);
  }
  .avatar-wrapper.has-story:hover {
    animation: rotateStoryRing 4s linear infinite;
  }
  @keyframes rotateStoryRing {
    0% { filter: hue-rotate(0deg); }
    100% { filter: hue-rotate(360deg); }
  }

  .avatar-circle {
    width: 44px;
    height: 44px;
    border-radius: 50%;
    background: linear-gradient(135deg, #262626 0%, #3e3e3e 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 16px;
    font-weight: 800;
    color: #ffffff;
    border: 2px solid var(--bg-sidebar);
    box-sizing: border-box;
  }

  .online-dot {
    position: absolute;
    bottom: 2px;
    right: 2px;
    width: 11px;
    height: 11px;
    border-radius: 50%;
    background: #737373;
    border: 2px solid var(--bg-sidebar);
  }
  .online-dot.online {
    background: #1ed760; /* Instagram Green */
  }
  .online-dot.busy {
    background: #ff5a5f;
  }
  .online-dot.away {
    background: #febd59;
  }

  .contact-info {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 1px;
  }
  .contact-name-row {
    display: flex;
    align-items: center;
    gap: 6px;
    min-width: 0;
  }
  .contact-name {
    font-size: 14px;
    font-weight: 650;
    color: var(--text-title);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .status-badge-ins {
    font-size: 10px;
    color: #ff9500;
    background: rgba(255, 149, 0, 0.08);
    border: 1px solid rgba(255, 149, 0, 0.2);
    border-radius: 4px;
    padding: 0 4px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 80px;
  }
  .contact-status {
    font-size: 11px;
    color: var(--text-desc);
  }
  .contact-status.online {
    color: #1ed760;
  }

  /* Micro Mini action button triggers */
  .action-buttons-row {
    display: flex;
    gap: 6px;
    opacity: 0;
    transition: opacity 0.2s ease;
  }
  .contact-instagram-card:hover .action-buttons-row {
    opacity: 1;
  }
  .action-mini-btn {
    width: 28px;
    height: 28px;
    border-radius: 50%;
    border: 1px solid rgba(255,255,255,0.08);
    background: var(--bg-sidebar, rgba(0,0,0,0.2));
    color: #ffffff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 11px;
    cursor: pointer;
    transition: all 0.2s ease;
  }
  .action-mini-btn:hover {
    transform: scale(1.1);
    background: var(--btn-hover, rgba(255,255,255,0.1));
  }
  .action-mini-btn.block-user:hover {
    border-color: rgba(220, 39, 67, 0.4);
    background: rgba(220, 39, 67, 0.1);
  }

  /* Accordion Blacklist Manager */
  .blacklist-collapsible {
    margin: 16px 12px;
    border: 1px solid rgba(255, 255, 255, 0.06);
    border-radius: 10px;
    background: rgba(255, 255, 255, 0.01);
    overflow: hidden;
  }
  .blacklist-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 14px;
    cursor: pointer;
    user-select: none;
    transition: background 0.2s ease;
  }
  .blacklist-header:hover {
    background: rgba(255, 255, 255, 0.03);
  }
  .blacklist-header .title {
    font-size: 12px;
    font-weight: 700;
    color: var(--text-desc);
  }
  .blacklist-header .arrow {
    font-size: 11px;
    opacity: 0.5;
    transition: transform 0.2s ease;
  }
  .blacklist-header .arrow.rotated {
    transform: rotate(90deg);
  }
  .blacklist-content {
    border-top: 1px solid rgba(255, 255, 255, 0.06);
    padding: 8px;
    background: rgba(0,0,0,0.1);
  }
  .empty-blacklist-tip {
    text-align: center;
    color: var(--text-desc);
    font-size: 12px;
    padding: 16px;
  }
  .blacklist-item {
    display: flex;
    align-items: center;
    padding: 8px 10px;
    gap: 10px;
    border-radius: 6px;
    transition: background 0.2s ease;
  }
  .blacklist-item:hover {
    background: rgba(255,255,255,0.02);
  }
  .blacklist-avatar {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    background: #333333;
    color: #ffffff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 13px;
    font-weight: 700;
  }
  .blacklist-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-width: 0;
  }
  .blacklist-info .name {
    font-size: 13px;
    font-weight: 600;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .blacklist-info .id {
    font-size: 10px;
    color: var(--text-desc);
  }
  .unblock-btn {
    border: 1px solid rgba(255,255,255,0.15);
    background: transparent;
    color: var(--text-title);
    font-size: 11px;
    font-weight: 600;
    padding: 4px 10px;
    border-radius: 6px;
    cursor: pointer;
    transition: all 0.2s ease;
  }
  .unblock-btn:hover {
    background: var(--text-title);
    color: var(--bg-sidebar);
    border-color: var(--text-title);
  }

  /* ================= INSTAGRAM DRAWER ================= */
  .requests-drawer-backdrop {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.7);
    z-index: 99999;
    display: flex;
    justify-content: flex-end;
  }
  .requests-drawer {
    width: 100%;
    max-width: 380px;
    height: 100%;
    background: #121212; /* Clean Instagram dark */
    color: #ffffff;
    box-shadow: -10px 0 30px rgba(0, 0, 0, 0.5);
    display: flex;
    flex-direction: column;
    box-sizing: border-box;
    border-left: 1px solid rgba(255,255,255,0.08);
  }
  
  .drawer-header {
    padding: 20px;
    display: flex;
    align-items: center;
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);
    gap: 12px;
  }
  .close-drawer-btn {
    background: transparent;
    border: none;
    color: #ffffff;
    font-size: 16px;
    cursor: pointer;
    padding: 4px 8px;
    outline: none;
  }
  .close-drawer-btn:hover {
    opacity: 0.8;
  }
  .drawer-title {
    font-size: 16px;
    font-weight: 800;
    flex: 1;
  }
  .requests-count-pill {
    background: #e6683c;
    color: #ffffff;
    font-size: 10px;
    font-weight: 800;
    padding: 2px 8px;
    border-radius: 10px;
  }

  .drawer-scroll-content {
    flex: 1;
    overflow-y: auto;
    padding: 16px;
  }

  .drawer-empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 60%;
    color: var(--text-desc);
    font-size: 13px;
    gap: 12px;
  }
  .star-icon {
    font-size: 40px;
    animation: starPulse 2.5s infinite ease-in-out;
  }
  @keyframes starPulse {
    0% { transform: scale(1); filter: drop-shadow(0 0 2px rgba(255, 149, 0, 0.3)); }
    50% { transform: scale(1.15); filter: drop-shadow(0 0 10px rgba(255, 149, 0, 0.7)); }
    100% { transform: scale(1); filter: drop-shadow(0 0 2px rgba(255, 149, 0, 0.3)); }
  }

  /* Individual request card in Ins drawer */
  .request-instagram-card {
    background: rgba(255, 255, 255, 0.02);
    border: 1px solid rgba(255, 255, 255, 0.05);
    border-radius: 12px;
    padding: 14px;
    margin-bottom: 12px;
    display: flex;
    flex-direction: column;
    gap: 10px;
    box-sizing: border-box;
    transition: all 0.2s ease;
  }
  .request-instagram-card:hover {
    border-color: rgba(255, 255, 255, 0.1);
    background: rgba(255, 255, 255, 0.04);
  }
  .req-avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background: linear-gradient(135deg, #f09433, #dc2743);
    color: #ffffff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    font-weight: 800;
  }
  .req-details {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }
  .req-user-row {
    display: flex;
    align-items: baseline;
    gap: 6px;
  }
  .req-name {
    font-size: 13px;
    font-weight: 700;
  }
  .req-id {
    font-size: 9px;
    color: var(--text-desc);
  }
  .req-remark-bubble {
    font-size: 11px;
    font-style: italic;
    background: rgba(255,255,255,0.05);
    border-radius: 6px;
    padding: 6px 10px;
    color: rgba(255, 255, 255, 0.85);
    word-break: break-all;
  }
  .req-actions {
    display: flex;
    gap: 8px;
  }
  .req-btn {
    flex: 1;
    height: 30px;
    font-size: 12px;
    font-weight: 700;
    border-radius: 6px;
    border: none;
    outline: none;
    cursor: pointer;
    transition: all 0.15s ease;
  }
  .req-btn.accept {
    background: linear-gradient(45deg, #e6683c, #dc2743);
    color: #ffffff;
  }
  .req-btn.accept:hover {
    opacity: 0.95;
    transform: translateY(-1px);
  }
  .req-btn.reject {
    background: rgba(255,255,255,0.08);
    color: #ffffff;
    border: 1px solid rgba(255,255,255,0.1);
  }
  .req-btn.reject:hover {
    background: rgba(255,255,255,0.12);
  }

  /* Animations */
  .animate-pop {
    animation: pop 0.25s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  }
  .animate-slide-down {
    animation: slideDown 0.25s cubic-bezier(0.16, 1, 0.3, 1) forwards;
  }
  .animate-slide-left {
    animation: slideLeft 0.3s cubic-bezier(0.16, 1, 0.3, 1) forwards;
  }
  .animate-fade-in {
    animation: fadeIn 0.25s ease-out forwards;
  }

  @keyframes pop {
    0% { transform: scale(0.8); opacity: 0; }
    100% { transform: scale(1); opacity: 1; }
  }
  @keyframes slideDown {
    from { opacity: 0; transform: translateY(-8px); }
    to { opacity: 1; transform: translateY(0); }
  }
  @keyframes slideLeft {
    from { transform: translateX(100%); }
    to { transform: translateX(0); }
  }
  @keyframes fadeIn {
    from { opacity: 0; }
    to { opacity: 1; }
  }
</style>
