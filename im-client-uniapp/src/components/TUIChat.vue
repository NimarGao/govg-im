<template>
  <div class="chat-viewport" :class="{ 'mobile-active': view === 'chat' }">
    <div v-if="currentSession" class="chat-frame">
      <!-- Chat Header -->
      <div class="chat-header-bar">
        <div class="back-action-btn" @click="$emit('back-to-list')">
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
        <div v-for="(msg, index) in currentMsgs" :key="msg.msgId || index" class="msg-bubble-wrapper" 
            :class="{ 'self-msg': msg.isSelf, 'system-push-msg': msg.sender === 'system_service' }" :id="msg.msgId ? 'msg-' + msg.msgId : 'msg-' + index">
          
          <!-- Time Divider -->
          <div class="msg-time-divider" v-if="shouldShowTime(index, msg)">{{ msg.time }}</div>
          
          <!-- 1. System Notification or Recalled Message -->
          <div v-if="msg.sender === 'system_notification' || msg.status === 'recalled' || msg.content === '[撤回消息]'" class="system-notification-row animate-pop">
            <span class="system-notification-text">
              {{ msg.sender === 'system_notification' ? msg.content : (msg.isSelf ? '您撤回了一条消息' : (msg.senderName || msg.sender) + ' 撤回了一条消息') }}
            </span>
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
            <div class="msg-avatar-circle" v-if="!msg.isSelf">{{ msg.sender ? msg.sender[0].toUpperCase() : '?' }}</div>
            <div class="msg-bubble-content">
              <span class="sender-name-label" v-if="!msg.isSelf && currentSession.type === 'group'">{{ msg.sender }}</span>
              
              <!-- Quoted Message Card -->
              <div v-if="msg.quoteMsgId" class="quote-card animate-pop" @click.stop="scrollToMessage(msg.quoteMsgId)">
                <span class="quote-sender">@{{ msg.quoteSender }}</span>
                <span class="quote-text">{{ msg.quoteContent }}</span>
              </div>

              <!-- Image rendering -->
              <img v-if="msg.msgType === 2" :src="formatImageUrl(msg.content)" class="bubble-image-card" @click="previewImage(msg.content)" />
              
              <!-- Text rendering -->
              <span v-else :class="msg.content === '👍' ? 'giant-emoji-msg' : 'bubble-text-card'" 
                    @contextmenu.prevent="openContextMenu($event, msg)">
                {{ msg.content }}
                <span v-if="msg.isEdited || msg.status === 'edited'" style="font-size: 10px; font-style: italic; opacity: 0.6; margin-left: 6px;">(已编辑)</span>
              </span>

              <!-- Group Read Receipt count under self messages in group chat -->
              <div v-if="msg.isSelf && currentSession.type === 'group' && msg.readCount > 0" class="group-read-receipt" @click.stop="showGroupReaders(msg)">
                {{ msg.readCount }}人已读
              </div>
            </div>

            <!-- Hover actions trigger button -->
            <div v-if="msg.msgId && msg.status !== 'recalled'" class="bubble-actions-trigger" @click.stop="openContextMenu($event, msg)">⋮</div>

            <!-- Status Indicators for self messages -->
            <div v-if="msg.isSelf && currentSession.type !== 'group'" class="msg-status-indicator">
              <div v-if="msg.status === 'sending'" class="status-indicator-sending"></div>
              <div v-else-if="msg.status === 'sent'" class="status-indicator-sent" title="已送达服务器">✓</div>
              <div v-else-if="msg.status === 'read'" class="status-indicator-read" title="对方已读">✓✓</div>
            </div>
          </div>

        </div>

        <!-- Typing Indicator (Facebook Messenger Style) -->
        <div v-if="peerTyping" class="msg-bubble-wrapper animate-pop">
          <div class="msg-row">
            <div class="msg-avatar-circle">{{ currentSession ? currentSession.name[0].toUpperCase() : '?' }}</div>
            <div class="msg-bubble-content">
              <div class="typing-indicator-bubble">
                <span class="typing-dot"></span>
                <span class="typing-dot"></span>
                <span class="typing-dot"></span>
              </div>
            </div>
          </div>
        </div>

        <div id="scroll-bottom"></div>
      </div>

      <!-- Quoting / Editing Action Bar (Sliding capsule) -->
      <div v-if="quotingMessage || editingMessage" class="input-action-bar">
        <div class="action-bar-content">
          <span class="action-bar-title">
            {{ quotingMessage ? '回复 @' + (quotingMessage.senderName || quotingMessage.sender) : '正在编辑消息' }}
          </span>
          <span class="action-bar-text">
            {{ quotingMessage ? quotingMessage.content : editingMessage.content }}
          </span>
        </div>
        <span class="action-bar-close" @click="cancelInputMode">✕</span>
      </div>

      <!-- Bottom Input Tray (Facebook Messenger High-Fidelity Style) -->
      <div class="bottom-input-tray">
        <div class="tray-left-actions">
          <div class="tray-circle-btn" @click="$emit('show-start-chat')" title="新会话">➕</div>
          <div class="tray-circle-btn" @click="$emit('choose-image')" title="发送图片">📷</div>
          <div class="tray-circle-btn" @click="appendEmoji('😀')" title="Emoji">😀</div>
        </div>
        <div class="messenger-input-wrapper">
          <input class="messenger-message-input" v-model="localMsgContent" placeholder="Aa" @keyup.enter="handleSend" @input="handleTypingInput" />
        </div>
        <div class="tray-right-actions">
          <button v-if="localMsgContent.trim()" class="messenger-send-btn" @click="handleSend">
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

    <!-- ================= RIGHT CHAT DETAIL SIDEBAR (INFO PANEL) ================= -->
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
          <div class="glass-menu-item" @click="$emit('clear-history', currentSession)">🗑️ 清除聊天记录</div>
          <div class="glass-menu-item" @click="copySessionId">📋 复制会话 ID</div>
        </div>

        <div class="right-section" v-if="currentSession.type === 'group'">
          <span class="right-section-title">👥 真实群成员 ({{ currentGroupMembers.length }}人)</span>
          <div class="member-list-mini">
            <div v-for="member in currentGroupMembers" :key="member.userId" class="member-mini-card">
              <div class="member-mini-avatar-wrapper" style="position: relative; display: flex; align-items: center; justify-content: center;">
                <span class="member-mini-avatar">{{ member.username ? member.username[0].toUpperCase() : 'U' }}</span>
                <span class="online-indicator-dot-mini" :style="{ background: member.online ? 'var(--active-green)' : '#bcc0c4' }" 
                      style="position: absolute; bottom: -2px; right: -2px; width: 8px; height: 8px; border-radius: 50%; border: 1.5px solid var(--bg-sidebar);"></span>
              </div>
              <span class="member-mini-name">{{ member.username }}</span>
              <span class="member-mini-tag" v-if="member.userId === userId" style="font-size: 10px; background: var(--btn-hover); color: var(--text-desc); padding: 2px 6px; border-radius: 4px; margin-left: auto;">我</span>
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

    <!-- ================= GROUP READERS MODAL ================= -->
    <div v-if="showReadersModal" class="modal-backdrop">
      <div class="modal-glass-card">
        <span class="modal-title" style="margin-bottom: 10px;">消息已读状态</span>
        <div style="font-size: 13px; color: var(--text-desc); margin-bottom: 15px; max-width: 100%; word-break: break-all; text-align: center;">
          "{{ readersModalMsg ? readersModalMsg.content : '' }}"
        </div>
        
        <div style="max-height: 250px; overflow-y: auto; width: 100%; display: flex; flex-direction: column; gap: 8px; margin-bottom: 15px;">
          <div v-for="user in getReaderDetails(readersModalMsg)" :key="user.userId" 
               style="display: flex; align-items: center; gap: 10px; padding: 8px 12px; background: var(--btn-hover); border-radius: 8px;">
            <span class="member-mini-avatar" style="margin: 0; width: 28px; height: 28px; font-size: 12px;">
              {{ user.username ? user.username[0].toUpperCase() : 'U' }}
            </span>
            <span style="font-size: 14px; font-weight: 500; color: var(--text-primary);">{{ user.username }}</span>
            <span style="font-size: 12px; color: var(--active-green); margin-left: auto; font-weight: 600;">✓ 已读</span>
          </div>
          <div v-if="!readersModalMsg || !readersModalMsg.readUserIds || readersModalMsg.readUserIds.length === 0" 
               style="text-align: center; color: var(--text-desc); font-size: 13px; padding: 20px;">
            暂无已读记录
          </div>
        </div>
        
        <button class="glass-btn secondary-dark" @click="showReadersModal = false">关闭</button>
      </div>
    </div>

    <!-- ================= CUSTOM CONTEXT MENU ================= -->
    <div v-if="contextMenu.show && contextMenu.msg" class="custom-context-menu" 
         :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }">
      <div class="context-menu-item" @click="startQuoting(contextMenu.msg)">
        💬 回复该消息
      </div>
      <div v-if="contextMenu.msg.isSelf && contextMenu.msg.msgType === 1 && (Date.now() - (contextMenu.msg.createTimeMs || 0) < 120000)" 
           class="context-menu-item" @click="startEditing(contextMenu.msg)">
        ✏️ 编辑消息
      </div>
      <div v-if="contextMenu.msg.isSelf && (Date.now() - (contextMenu.msg.createTimeMs || 0) < 120000)" 
           class="context-menu-item danger" @click="$emit('send-recall-request', contextMenu.msg)">
        🗑️ 撤回该消息
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'TUIChat',
  props: {
    currentSession: {
      type: Object,
      default: null
    },
    currentMsgs: {
      type: Array,
      default: () => []
    },
    peerTyping: {
      type: Boolean,
      default: false
    },
    currentGroupMembers: {
      type: Array,
      default: () => []
    },
    userId: {
      type: String,
      default: ''
    },
    username: {
      type: String,
      default: ''
    },
    serverHttpUrl: {
      type: String,
      default: 'http://127.0.0.1:10085'
    },
    view: {
      type: String,
      default: 'chat'
    },
    totalUnread: {
      type: Number,
      default: 0
    }
  },
  emits: [
    'back-to-list',
    'show-start-chat',
    'choose-image',
    'send-thumbs-up',
    'send-message',
    'send-edit-request',
    'send-recall-request',
    'clear-history',
    'handle-input',
    'report-read-receipt',
    'report-group-read-receipt'
  ],
  data() {
    return {
      localMsgContent: '',
      quotingMessage: null,
      editingMessage: null,
      showRightPanel: false,
      showReadersModal: false,
      readersModalMsg: null,
      isAtBottom: true,
      contextMenu: {
        show: false,
        x: 0,
        y: 0,
        msg: null
      }
    };
  },
  computed: {
    sharedImages() {
      return this.currentMsgs
        .filter(m => m.msgType === 2 && m.content && m.status !== 'recalled')
        .map(m => this.formatImageUrl(m.content));
    }
  },
  watch: {
    currentSession(newVal, oldVal) {
      if (newVal) {
        this.localMsgContent = '';
        this.quotingMessage = null;
        this.editingMessage = null;
        this.showRightPanel = false;
        this.scrollToBottom();
      }
    },
    currentMsgs: {
      deep: true,
      handler() {
        if (this.isAtBottom) {
          setTimeout(() => this.scrollToBottom(), 50);
        }
      }
    }
  },
  methods: {
    onScroll(e) {
      const scrollEl = e.target;
      const viewHeight = scrollEl.clientHeight;
      const scrollHeight = scrollEl.scrollHeight;
      const scrollTop = scrollEl.scrollTop;
      this.isAtBottom = (scrollHeight - scrollTop - viewHeight < 60);
    },
    formatImageUrl(url) {
      if (!url) return '';
      if (url.startsWith('/')) {
        return this.serverHttpUrl + url;
      }
      return url;
    },
    shouldShowTime(index, msg) {
      if (index === 0) return true;
      return index % 5 === 0;
    },
    previewImage(url) {
      uni.previewImage({
        urls: [this.formatImageUrl(url)]
      });
    },
    copySessionId() {
      if (!this.currentSession) return;
      uni.setClipboardData({
        data: this.currentSession.id,
        success: () => uni.showToast({ title: '已复制会话 ID', icon: 'none' })
      });
    },
    appendEmoji(emoji) {
      this.localMsgContent += emoji;
    },
    handleSend() {
      if (!this.localMsgContent) return;
      if (this.editingMessage) {
        this.$emit('send-edit-request', {
          msg: this.editingMessage,
          newContent: this.localMsgContent
        });
      } else {
        const quoteInfo = this.quotingMessage ? {
          msgId: this.quotingMessage.msgId,
          sender: this.quotingMessage.senderName || this.quotingMessage.sender,
          content: this.quotingMessage.content
        } : null;
        this.$emit('send-message', {
          content: this.localMsgContent,
          quote: quoteInfo
        });
      }
      this.localMsgContent = '';
      this.quotingMessage = null;
      this.editingMessage = null;
    },
    sendThumbsUp() {
      this.$emit('send-thumbs-up');
    },
    handleTypingInput() {
      this.$emit('handle-input');
    },
    cancelInputMode() {
      this.localMsgContent = '';
      this.quotingMessage = null;
      this.editingMessage = null;
    },
    openContextMenu(e, msg) {
      if (!msg || msg.sender === 'system_notification' || msg.sender === 'system_service') return;
      if (msg.status === 'recalled' || msg.content === '[撤回消息]') return;
      e.preventDefault();
      
      this.contextMenu = {
        show: true,
        x: e.clientX || 200,
        y: e.clientY || 200,
        msg: msg
      };
      
      const closeMenu = () => {
        this.contextMenu.show = false;
        document.removeEventListener('click', closeMenu);
      };
      setTimeout(() => {
        document.addEventListener('click', closeMenu);
      }, 10);
    },
    startQuoting(msg) {
      this.quotingMessage = msg;
      this.editingMessage = null;
    },
    startEditing(msg) {
      this.editingMessage = msg;
      this.quotingMessage = null;
      this.localMsgContent = msg.content;
    },
    scrollToMessage(quoteMsgId) {
      if (!quoteMsgId) return;
      const element = document.getElementById('msg-' + quoteMsgId);
      if (element) {
        element.scrollIntoView({ behavior: 'smooth', block: 'center' });
        element.classList.add('highlight-pulse');
        setTimeout(() => {
          element.classList.remove('highlight-pulse');
        }, 1500);
      } else {
        uni.showToast({ title: '无法定位到原消息', icon: 'none' });
      }
    },
    getReaderDetails(msg) {
      if (!msg || !msg.readUserIds) return [];
      return msg.readUserIds.map(uId => {
        const member = this.currentGroupMembers.find(m => m.userId === uId);
        return {
          userId: uId,
          username: member ? member.username : uId
        };
      });
    },
    showGroupReaders(msg) {
      this.readersModalMsg = msg;
      this.showReadersModal = true;
    },
    scrollToBottom() {
      this.$nextTick(() => {
        const scroller = document.querySelector('.chat-messages-scroller');
        if (scroller) {
          scroller.scrollTop = scroller.scrollHeight;
        }
      });
    }
  }
};
</script>

<style scoped>
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
    border-radius: 50%;
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

  /* Scroller */
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
    text-align: left;
  }

  /* Chat bubble styling */
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
    text-align: left;
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

  /* Push Message Card */
  .system-push-card {
    background: var(--input-bg);
    border: 1px solid var(--border-color);
    border-radius: 12px;
    padding: 14px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
    margin: 10px 0;
    max-width: 100%;
    width: 320px;
    text-align: left;
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

  /* Bottom input tray */
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

  /* Right profile panel */
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
    text-align: left;
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

  /* Modals */
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
    animation: contextFadeIn 0.25s ease-out;
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

  .secondary-dark {
    background: var(--btn-hover) !important;
    color: var(--text-title) !important;
    box-shadow: none !important;
  }
  .secondary-dark:hover {
    background: var(--btn-active) !important;
  }

  /* Context Menu */
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
  .context-menu-item.danger {
    color: #ff3b30;
  }
  .context-menu-item.danger:hover {
    background-color: rgba(255, 59, 48, 0.1);
  }

  /* Quote Preview Bar */
  .input-action-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 10px 20px;
    background: var(--bg-chat-header);
    border-top: 1px solid var(--border-color);
    border-bottom: 1px solid var(--border-color);
    animation: slideUpAction 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  }
  @keyframes slideUpAction {
    from { transform: translateY(100%); opacity: 0; }
    to { transform: translateY(0); opacity: 1; }
  }

  .action-bar-content {
    display: flex;
    flex-direction: column;
    gap: 2px;
    flex: 1;
    text-align: left;
  }
  .action-bar-title {
    font-size: 12px;
    font-weight: 600;
    color: var(--active-blue);
  }
  .action-bar-text {
    font-size: 13px;
    color: var(--text-desc);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 80%;
  }
  .action-bar-close {
    font-size: 18px;
    color: var(--text-desc);
    cursor: pointer;
    padding: 4px;
    transition: color 0.2s;
  }
  .action-bar-close:hover {
    color: var(--text-primary);
  }

  /* Quoted Bubble inside Message Bubble */
  .quote-card {
    display: flex;
    flex-direction: column;
    gap: 4px;
    padding: 6px 12px;
    margin-bottom: 6px;
    background: rgba(0, 0, 0, 0.05);
    border-left: 3px solid var(--active-blue);
    border-radius: 6px;
    font-size: 12px;
    color: var(--text-desc);
    cursor: pointer;
    transition: background-color 0.2s;
    text-align: left;
  }
  .quote-sender {
    font-weight: 600;
    color: var(--text-primary);
  }
  .quote-text {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  
  .self-msg .quote-card {
    border-left-color: #ffffff;
    color: rgba(255, 255, 255, 0.8);
  }
  .self-msg .quote-card .quote-sender {
    color: #ffffff;
  }

  /* Group read receipts styling */
  .group-read-receipt {
    font-size: 11px;
    color: var(--text-desc);
    margin-top: 4px;
    cursor: pointer;
    transition: color 0.2s;
    user-select: none;
    text-align: right;
  }
  .group-read-receipt:hover {
    color: var(--active-blue);
    text-decoration: underline;
  }

  /* Highlight Message Pulse */
  .highlight-pulse {
    animation: highlightPulseKey 1.5s ease-in-out;
  }
  @keyframes highlightPulseKey {
    0% { transform: scale(1); background-color: transparent; }
    25% { transform: scale(1.03); background-color: rgba(0, 132, 255, 0.2); }
    50% { transform: scale(1.03); background-color: rgba(0, 132, 255, 0.2); }
    100% { transform: scale(1); background-color: transparent; }
  }

  .bubble-actions-trigger {
    opacity: 0;
    font-size: 16px;
    color: var(--text-desc);
    cursor: pointer;
    padding: 0 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: opacity 0.2s, color 0.2s;
    user-select: none;
    order: 3;
  }
  .msg-row:hover .bubble-actions-trigger {
    opacity: 1;
  }
  .bubble-actions-trigger:hover {
    color: var(--text-primary);
  }
  .self-msg .bubble-actions-trigger {
    order: -1;
  }

  /* Typing Indicator Bubble */
  .typing-indicator-bubble {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    background: var(--btn-hover);
    padding: 10px 16px;
    border-radius: 18px;
    width: fit-content;
    height: 36px;
    box-sizing: border-box;
    border-bottom-left-radius: 4px;
    box-shadow: 0 1px 2px rgba(0,0,0,0.05);
  }

  .typing-dot {
    width: 6px;
    height: 6px;
    background: var(--text-desc);
    border-radius: 50%;
    animation: typingDot 1.4s infinite ease-in-out both;
  }

  .typing-dot:nth-child(1) {
    animation-delay: 0s;
  }

  .typing-dot:nth-child(2) {
    animation-delay: 0.2s;
  }

  .typing-dot:nth-child(3) {
    animation-delay: 0.4s;
  }

  @keyframes typingDot {
    0%, 80%, 100% {
      transform: scale(0.6);
      opacity: 0.4;
    }
    40% {
      transform: scale(1.2) translateY(-4px);
      opacity: 1;
    }
  }

  /* Responsive layouts */
  @media (max-width: 768px) {
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

  /* Status indicators */
  .msg-status-indicator {
    align-self: flex-end;
    margin-bottom: 2px;
    margin-left: 6px;
    flex-shrink: 0;
  }

  @keyframes rotateStatus {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
  }

  .status-indicator-sending {
    width: 12px;
    height: 12px;
    border: 1.5px solid var(--active-blue);
    border-top-color: transparent;
    border-radius: 50%;
    animation: rotateStatus 0.8s linear infinite;
  }

  .status-indicator-sent {
    width: 14px;
    height: 14px;
    background: var(--border-color);
    color: var(--bg-board);
    font-size: 9px;
    font-weight: bold;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    user-select: none;
    line-height: 1;
    transition: all 0.2s ease;
  }

  .status-indicator-read {
    width: 14px;
    height: 14px;
    background: var(--active-blue);
    color: #ffffff;
    font-size: 8px;
    font-weight: bold;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    user-select: none;
    line-height: 1;
    animation: bounceIn 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275) both;
  }

  @keyframes bounceIn {
    0% { transform: scale(0.3); opacity: 0; }
    50% { transform: scale(1.1); }
    100% { transform: scale(1.0); opacity: 1; }
  }

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
</style>
