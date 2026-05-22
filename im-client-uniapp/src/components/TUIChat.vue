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

        <div class="header-actions" style="display: flex; gap: 8px;">
          <div v-if="currentSession.type === 'user'" class="header-icon-btn" @click="$emit('initiate-call', { type: 'voice' })" title="语音通话">📞</div>
          <div v-if="currentSession.type === 'user'" class="header-icon-btn" @click="$emit('initiate-call', { type: 'video' })" title="视频通话">📹</div>
          <div v-if="currentSession.type === 'group'" class="header-icon-btn instagram-meeting-btn" @click="$emit('initiate-group-call')" title="发起多人视频会议">🎥</div>
          <div class="header-icon-btn" @click="showRightPanel = !showRightPanel" title="会话信息">ℹ️</div>
        </div>
      </div>

      <!-- Instagram-style Active Meeting Banner -->
      <div v-if="isGroupCallActive" class="instagram-meeting-active-banner animate-slide-down" @click="$emit('join-group-call')">
        <div class="meeting-banner-glow-bg"></div>
        <span class="banner-icon animate-pulse-scale">🔊</span>
        <span class="banner-text">
          多人视频会议正在进行中 (已加入 <span class="bold-text">{{ groupCallMembersCount }}</span> 人)
        </span>
        <button class="join-meeting-btn-ins animate-pop">立即加入</button>
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
            <!-- Multi-Select Checkbox Circle -->
            <div v-if="isMultiSelectMode" class="msg-checkbox-wrapper animate-pop" @click.stop="toggleMsgSelection(msg)">
              <div class="msg-checkbox-circle" :class="{ checked: isMsgSelected(msg) }">
                <span v-if="isMsgSelected(msg)" class="msg-check-symbol">✓</span>
              </div>
            </div>
            
            <div class="msg-avatar-circle" v-if="!msg.isSelf && !isMultiSelectMode">{{ msg.sender ? msg.sender[0].toUpperCase() : '?' }}</div>
            <div class="msg-bubble-content" :class="{ 'shake-bubble': msg.status === 'blocked' }">
              <span class="sender-name-label" v-if="!msg.isSelf && currentSession.type === 'group'">{{ msg.sender }}</span>
              
              <!-- Quoted Message Card -->
              <div v-if="msg.quoteMsgId" class="quote-card animate-pop" @click.stop="scrollToMessage(msg.quoteMsgId)">
                <span class="quote-sender">@{{ msg.quoteSender }}</span>
                <span class="quote-text">{{ msg.quoteContent }}</span>
              </div>

              <!-- Image rendering -->
              <img v-if="msg.msgType === 2" :src="formatImageUrl(msg.content)" class="bubble-image-card" @click="previewImage(msg.content)" />
              
              <!-- Custom Card Message rendering (msgType === 3) -->
              <div v-else-if="msg.msgType === 3" class="custom-card-wrapper animate-pop" @contextmenu.prevent="openContextMenu($event, msg)">
                <div v-if="parseCardContent(msg.content).cardType === 'user'" class="user-share-card">
                  <div class="user-card-header">
                    <div class="user-card-avatar">{{ parseCardContent(msg.content).username ? parseCardContent(msg.content).username[0].toUpperCase() : 'U' }}</div>
                    <div class="user-card-meta">
                      <span class="user-card-name">{{ parseCardContent(msg.content).username }}</span>
                      <span class="user-card-id">ID: {{ parseCardContent(msg.content).userId }}</span>
                    </div>
                  </div>
                  <div class="user-card-bio">{{ parseCardContent(msg.content).bio || '这个人很懒，什么都没有留下...' }}</div>
                  <button class="user-card-btn" @click.stop="$emit('show-start-chat'); $emit('start-chat-with', parseCardContent(msg.content).userId)">💬 发起私聊</button>
                </div>
                
                <div v-else-if="parseCardContent(msg.content).cardType === 'product'" class="product-share-card">
                  <img :src="parseCardContent(msg.content).image || 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=300'" class="product-card-img" />
                  <div class="product-card-info">
                    <span class="product-card-title">{{ parseCardContent(msg.content).title }}</span>
                    <span class="product-card-desc">{{ parseCardContent(msg.content).desc }}</span>
                    <div class="product-card-footer">
                      <span class="product-card-price">{{ parseCardContent(msg.content).price }}</span>
                      <button class="product-card-btn" @click.stop="openProductDetails(parseCardContent(msg.content))">立即查看</button>
                    </div>
                  </div>
                </div>
                
                <div v-else class="unknown-card">
                  <span>⚠️ 未知类型卡片消息</span>
                  <pre>{{ msg.content }}</pre>
                </div>
              </div>

              <!-- Voice Message rendering (msgType === 4) -->
              <div v-else-if="msg.msgType === 4" class="voice-bubble-card animate-pop" 
                   :class="{ 'playing': playingMsgId === msg.msgId }" 
                   @click="playVoiceMessage(msg)" @contextmenu.prevent="openContextMenu($event, msg)">
                <span class="voice-play-icon">{{ playingMsgId === msg.msgId ? '⏸️' : '▶️' }}</span>
                <div class="voice-wave-container">
                  <span v-for="(h, idx) in parseVoiceWaveforms(msg.content)" :key="idx" 
                        class="voice-wave-bar" :style="{ height: h + 'px' }"></span>
                </div>
                <span class="voice-duration">{{ parseVoiceDuration(msg.content) }}"</span>
              </div>
              
              <!-- Combined Message rendering (msgType === 5) -->
              <div v-else-if="msg.msgType === 5" class="combined-bubble-card animate-pop" 
                   @click="showCombinedMessages(msg)" @contextmenu.prevent="openContextMenu($event, msg)">
                <div class="combined-card-header">
                  <span class="combined-card-icon">📦</span>
                  <span class="combined-card-title">{{ parseCombinedTitle(msg.content) }}</span>
                </div>
                <div class="combined-card-body">
                  <div v-for="(line, lidx) in parseCombinedSummary(msg.content)" :key="lidx" class="combined-summary-line">
                    {{ line }}
                  </div>
                </div>
                <div class="combined-card-footer">
                  <span>查看全文</span>
                  <span class="arrow-right">➔</span>
                </div>
              </div>

              <!-- Smart Desk Card rendering (msgType === 6) -->
              <div v-else-if="msg.msgType === 6" class="desk-card-wrapper animate-pop">
                <div v-if="parseDeskCard(msg.content).isRatingCard && !msg.ratingSubmitted" class="desk-rating-card" :id="'desk-rating-' + (msg.msgId || '')">
                  <div class="desk-rating-header">
                    <span class="desk-rating-icon">🌟</span>
                    <span class="desk-rating-title">{{ parseDeskCard(msg.content).text || '请为本次服务打分' }}</span>
                  </div>
                  <div class="desk-stars-row" :id="'desk-stars-' + (msg.msgId || '')">
                    <span v-for="s in 5" :key="s" class="desk-star-item"
                          :class="{ active: deskRatingHover[msg.msgId] >= s || deskRatingScore[msg.msgId] >= s }"
                          @mouseenter="setRatingHover(msg.msgId, s)"
                          @mouseleave="setRatingHover(msg.msgId, 0)"
                          @click="setRatingScore(msg.msgId, s)"
                          :id="'desk-star-' + (msg.msgId || '') + '-' + s">⭐</span>
                  </div>
                  <textarea class="desk-feedback-input" :id="'desk-feedback-' + (msg.msgId || '')"
                            v-model="deskFeedback[msg.msgId]" placeholder="请留下您宝贵的意见（可选）..."></textarea>
                  <button class="desk-submit-btn" :id="'desk-submit-btn-' + (msg.msgId || '')"
                          @click="submitDeskRating(msg)" :disabled="!deskRatingScore[msg.msgId]">
                    {{ deskRatingScore[msg.msgId] ? '提交评价 ⭐'.repeat(deskRatingScore[msg.msgId]) : '请先点击星星选分' }}
                  </button>
                </div>
                
                <div v-else-if="msg.ratingSubmitted" class="desk-rating-success animate-pop">
                  <span class="desk-success-icon">❤️</span>
                  <span class="desk-success-text">感谢您的评价，祝您生活愉快！</span>
                </div>

                <div v-else class="desk-faq-card">
                  <div v-if="parseDeskCard(msg.content).text" class="desk-faq-text">
                    {{ parseDeskCard(msg.content).text }}
                  </div>
                  <div v-if="parseDeskCard(msg.content).options && parseDeskCard(msg.content).options.length > 0" class="desk-options-list">
                    <button v-for="(opt, oi) in parseDeskCard(msg.content).options" :key="oi"
                            class="desk-option-btn animate-pop"
                            :id="'desk-opt-' + (msg.msgId || oi) + '-' + oi"
                            :style="{ animationDelay: (oi * 0.07) + 's' }"
                            @click.stop="handleDeskOption(opt)">
                      {{ opt.label }}
                    </button>
                  </div>
                </div>
              </div>
              
              <!-- Text rendering -->
              <span v-else :class="msg.content === '👍' ? 'giant-emoji-msg' : 'bubble-text-card'" 
                    @contextmenu.prevent="openContextMenu($event, msg)">
                {{ msg.content }}
                <span v-if="msg.isEdited || msg.status === 'edited'" style="font-size: 10px; font-style: italic; opacity: 0.6; margin-left: 6px;">(已编辑)</span>
              </span>

              <!-- Floating Reactions Bar (Messenger Style) on hover -->
              <div v-if="msg.status !== 'recalled' && msg.content !== '[撤回消息]'" class="floating-reactions-bar">
                <span v-for="emo in ['👍', '❤️', '😂', '😮', '😢', '🙏']" :key="emo" 
                      class="floating-emoji-item animate-pop" @click.stop="toggleReaction(msg, emo)">
                  {{ emo }}
                </span>
              </div>

              <!-- Message Reactions pills -->
              <div v-if="msg.reactions && Object.keys(msg.reactions).length > 0" class="msg-reactions-wrapper">
                <div v-for="(users, emo) in msg.reactions" :key="emo" 
                     class="reaction-pill" :class="{ 'user-reacted': hasReacted(users) }"
                     @click.stop="toggleReaction(msg, emo)">
                  <span class="reaction-emoji">{{ emo }}</span>
                  <span class="reaction-count">{{ users.length }}</span>
                  <div class="reaction-tooltip animate-pop">
                    <span class="tooltip-title">{{ emo }} 互动好友</span>
                    <div class="tooltip-users">
                      <span v-for="u in users" :key="u.userId" class="tooltip-user-item">{{ u.userName || u.userId }}</span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Group Read Receipt count under self messages in group chat -->
              <div v-if="msg.isSelf && currentSession.type === 'group' && msg.readCount > 0" class="group-read-receipt" @click.stop="showGroupReaders(msg)">
                {{ msg.readCount }}人已读
              </div>

              <!-- Blocked warning banner -->
              <div v-if="msg.status === 'blocked'" class="blocked-alert-strip animate-pop" @click.stop="showBlockedReason(msg)">
                ⚠️ {{ msg.reason && msg.reason.includes('拒收') ? '消息已发出，但被对方拒收了。' : '消息未发送成功：包含敏感词，已被系统拦截。' }} 点击查看详情
              </div>
            </div>

            <!-- Hover actions trigger button -->
            <div v-if="msg.msgId && msg.status !== 'recalled'" class="bubble-actions-trigger" @click.stop="openContextMenu($event, msg)">⋮</div>

            <!-- Status Indicators for self messages -->
            <div v-if="msg.isSelf" class="msg-status-indicator">
              <div v-if="msg.status === 'blocked'" class="status-indicator-blocked animate-pop" @click.stop="showBlockedReason(msg)" title="发送失败">⚠️</div>
              <template v-else-if="currentSession.type !== 'group'">
                <div v-if="msg.status === 'sending'" class="status-indicator-sending"></div>
                <div v-else-if="msg.status === 'sent'" class="status-indicator-sent" title="已送达服务器">✓</div>
                <div v-else-if="msg.status === 'read'" class="status-indicator-read" title="对方已读">✓✓</div>
              </template>
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

      <!-- High-Fidelity Voice Recording Panel Overlay -->
      <div v-if="isRecording" class="voice-recorder-overlay animate-slide-up">
        <div class="recorder-ambient-glow"></div>
        <div class="recorder-header">
          <span class="recorder-title">🎙️ 语音消息模拟录制</span>
          <span class="recorder-timer">{{ recordingDuration }}"</span>
        </div>
        <div class="recorder-visualizer">
          <div v-for="n in 12" :key="n" class="visualizer-bar" 
               :style="{ height: getVisualizerBarHeight(n) + 'px', animationDelay: (n * 0.15) + 's' }"></div>
        </div>
        <div class="recorder-actions">
          <button class="recorder-btn cancel" @click="cancelRecording">放弃 🗑️</button>
          <button class="recorder-btn send" @click="stopAndSendRecording">发送 🚀</button>
        </div>
      </div>

      <!-- Bottom Input Tray (Instagram Direct Capsule Style) -->
      <div v-if="!isMultiSelectMode" class="bottom-input-tray ins-input-tray">
        <div class="tray-circle-btn ins-tray-add-btn" @click="$emit('show-start-chat')" title="新会话">➕</div>
        <div class="ins-input-pill">
          <div class="ins-pill-btn" @click="appendEmoji('😀')" title="Emoji">😀</div>
          <input class="ins-message-input" v-model="localMsgContent" placeholder="发送消息..." @keyup.enter="handleSend" @input="handleTypingInput" />
          <div class="ins-pill-right-actions" :class="{ 'has-content': localMsgContent.trim().length > 0 }">
            <div class="ins-pill-btn" @click="toggleVoiceRecording" title="语音消息" :class="{ 'active-recording': isRecording }">🎙️</div>
            <div class="ins-pill-btn" @click="$emit('choose-image')" title="发送图片">📷</div>
          </div>
        </div>
        <div class="ins-tray-right-action">
          <button v-if="localMsgContent.trim().length > 0" class="ins-send-text-btn animate-pop" @click="handleSend">
            发送
          </button>
          <div v-else class="messenger-thumbs-btn ins-thumbs-btn animate-pop" @click="sendThumbsUp" title="发送点赞">👍</div>
        </div>
      </div>
      
      <!-- Bottom Multi-Select Glassmorphic Action Bar -->
      <div v-else class="bottom-multiselect-bar animate-slide-up">
        <div class="multiselect-info">
          已选择 <span class="highlight-count">{{ selectedMsgIds.length }}</span> 条消息
        </div>
        <div class="multiselect-actions">
          <button class="multiselect-btn cancel" @click="exitMultiSelect">取消 ❌</button>
          <button class="multiselect-btn forward-single" @click="forwardMessages('single')" :disabled="selectedMsgIds.length === 0">逐条转发 🚀</button>
          <button class="multiselect-btn forward-combine" @click="forwardMessages('combine')" :disabled="selectedMsgIds.length === 0">合并转发 📦</button>
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
      <!-- Direct Reactions inside Context Menu -->
      <div class="context-menu-reactions">
        <span v-for="emo in ['👍', '❤️', '😂', '😮', '😢', '🙏']" :key="emo" 
              class="context-emoji-item" @click="toggleReaction(contextMenu.msg, emo); contextMenu.show = false">
          {{ emo }}
        </span>
      </div>
      <div class="context-divider"></div>
      <div class="context-menu-item" @click="startQuoting(contextMenu.msg)">
        💬 回复该消息
      </div>
      <div class="context-menu-item" @click="startMultiSelect(contextMenu.msg)">
        ☑️ 多选消息
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

    <!-- ================= COMBINED MESSAGES DRAWER ================= -->
    <div v-if="combinedDrawer.show" class="drawer-backdrop" @click="combinedDrawer.show = false">
      <div class="drawer-glass-panel animate-slide-left" @click.stop>
        <div class="drawer-header">
          <span class="drawer-title">📦 {{ combinedDrawer.title }}</span>
          <span class="drawer-close-btn" @click="combinedDrawer.show = false">✕</span>
        </div>
        <div class="drawer-messages-list">
          <div v-for="(subMsg, idx) in combinedDrawer.messages" :key="idx" class="drawer-msg-row">
            <div class="drawer-avatar">{{ subMsg.senderName ? subMsg.senderName[0].toUpperCase() : 'U' }}</div>
            <div class="drawer-msg-bubble">
              <div class="drawer-msg-meta">
                <span class="drawer-msg-sender">{{ subMsg.senderName }}</span>
                <span class="drawer-msg-time">{{ subMsg.time }}</span>
              </div>
              <div class="drawer-msg-body">
                <img v-if="subMsg.msgType === 2" :src="formatImageUrl(subMsg.content)" class="drawer-image-card" @click="previewImage(subMsg.content)" />
                <span v-else>{{ subMsg.content }}</span>
              </div>
            </div>
          </div>
          <div v-if="!combinedDrawer.messages || combinedDrawer.messages.length === 0" class="drawer-empty">
            无合并消息内容
          </div>
        </div>
    </div>
    
    <!-- ================= INSTAGRAM-STYLE ALERT DIALOG (BLOCK / SECURE) ================= -->
    <div v-if="blockModal.show" class="ins-alert-overlay" @click="blockModal.show = false">
      <div class="ins-alert-dialog animate-pop" @click.stop>
        <div class="ins-alert-title">{{ blockModal.title }}</div>
        <div class="ins-alert-body">{{ blockModal.content }}</div>
        <div class="ins-alert-divider"></div>
        <div class="ins-alert-btn confirm" @click="blockModal.show = false">确定</div>
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
    },
    groupCallActive: {
      type: Object,
      default: () => ({})
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
    'report-group-read-receipt',
    'send-reaction',
    'start-chat-with',
    'initiate-call',
    'send-desk-action',
    'initiate-group-call',
    'join-group-call'
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
      },
      // Voice message states
      isRecording: false,
      recordingDuration: 0,
      recordingTimer: null,
      visualizerInterval: null,
      recordingWaveforms: Array(12).fill(10),
      playingMsgId: null,
      playTimer: null,

      // Multi-select and forwarding states
      isMultiSelectMode: false,
      selectedMsgIds: [],

      // Combined messages drawer state
      combinedDrawer: {
        show: false,
        title: '聊天记录合辑',
        messages: []
      },
      // Smart Desk Card states
      deskRatingHover: {},
      deskRatingScore: {},
      deskFeedback: {},
      // Instagram block/security dialog state
      blockModal: {
        show: false,
        title: '',
        content: ''
      },
      lastPopMsgId: null
    };
  },
  computed: {
    sharedImages() {
      return this.currentMsgs
        .filter(m => m.msgType === 2 && m.content && m.status !== 'recalled')
        .map(m => this.formatImageUrl(m.content));
    },
    isGroupCallActive() {
      if (!this.currentSession || this.currentSession.type !== 'group') return false;
      const info = this.groupCallActive[this.currentSession.id];
      return !!(info && info.active);
    },
    groupCallMembersCount() {
      if (!this.currentSession || this.currentSession.type !== 'group') return 0;
      const info = this.groupCallActive[this.currentSession.id];
      return info ? (info.membersCount || 0) : 0;
    }
  },
  watch: {
    currentSession(newVal, oldVal) {
      if (newVal) {
        this.localMsgContent = newVal.draft || '';
        this.quotingMessage = null;
        this.editingMessage = null;
        this.showRightPanel = false;
        this.scrollToBottom();
      }
    },
    currentMsgs: {
      deep: true,
      handler(newMsgs) {
        if (this.isAtBottom) {
          setTimeout(() => this.scrollToBottom(), 50);
        }
        
        // 自动探测最后一个自发消息是否是 blocked 状态，如果是，且尚未弹出，则自动弹出
        if (newMsgs && newMsgs.length > 0) {
          const lastMsg = newMsgs[newMsgs.length - 1];
          if (lastMsg.isSelf && lastMsg.status === 'blocked') {
            const msgKey = lastMsg.msgId || lastMsg.createTimeMs;
            if (this.lastPopMsgId !== msgKey) {
              this.lastPopMsgId = msgKey;
              this.showBlockedReason(lastMsg);
            }
          }
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
    showBlockedReason(msg) {
      const isBlacklisted = msg.reason && msg.reason.includes('拒收');
      this.blockModal = {
        show: true,
        title: isBlacklisted ? '发送失败' : '安全违规警示',
        content: msg.reason || '该消息包含不合规的敏感词汇，已被云端拦截发送，无法触达对方。'
      };
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
    },
    toggleReaction(msg, emoji) {
      const session = this.currentSession;
      if (!session) return;
      
      const isGroup = session.type === 'group';
      const targetId = isGroup ? session.id.substring(2) : session.id;
      
      const reactions = msg.reactions || {};
      const userReactions = reactions[emoji] || [];
      const hasReacted = userReactions.some(u => u.userId === this.userId);
      
      const action = hasReacted ? 'remove' : 'add';
      
      this.$emit('send-reaction', {
        msgId: msg.msgId,
        emoji: emoji,
        action: action,
        toUserId: isGroup ? null : targetId,
        toGroupId: isGroup ? targetId : null
      });
    },
    hasReacted(users) {
      if (!users) return false;
      return users.some(u => u.userId === this.userId);
    },
    parseCardContent(content) {
      try {
        return JSON.parse(content);
      } catch (e) {
        return { cardType: 'unknown', text: content };
      }
    },
    openProductDetails(product) {
      uni.showModal({
        title: product.title,
        content: `商品详情：\n价格: ${product.price}\n描述: ${product.desc}\n(模拟外部商品跳转)`,
        showCancel: false
      });
    },
    // Voice Message & Recording Simulation Logic
    toggleVoiceRecording() {
      if (this.isRecording) {
        // Stop recording cleanly
        this.stopAndSendRecording();
      } else {
        // Start simulated recording
        this.isRecording = true;
        this.recordingDuration = 0;
        this.recordingWaveforms = Array(12).fill(10);
        
        // Fast interval to simulate dynamic wavy amplitude jumps (Web Audio style visualizer)
        this.visualizerInterval = setInterval(() => {
          this.recordingWaveforms = Array.from({ length: 12 }, () => Math.floor(6 + Math.random() * 34));
        }, 100);
        
        // Seconds timer
        this.recordingTimer = setInterval(() => {
          this.recordingDuration++;
          if (this.recordingDuration >= 60) {
            this.stopAndSendRecording();
          }
        }, 1000);
      }
    },
    cancelRecording() {
      this.isRecording = false;
      if (this.recordingTimer) {
        clearInterval(this.recordingTimer);
        this.recordingTimer = null;
      }
      if (this.visualizerInterval) {
        clearInterval(this.visualizerInterval);
        this.visualizerInterval = null;
      }
      this.recordingDuration = 0;
      uni.showToast({ title: '已取消录音', icon: 'none' });
    },
    stopAndSendRecording() {
      if (!this.isRecording) return;
      this.isRecording = false;
      
      if (this.recordingTimer) {
        clearInterval(this.recordingTimer);
        this.recordingTimer = null;
      }
      if (this.visualizerInterval) {
        clearInterval(this.visualizerInterval);
        this.visualizerInterval = null;
      }
      
      const duration = this.recordingDuration || 1; // minimum 1s
      
      // Generate a nice random waveforms list for saving (7 bars of heights between 10 and 50)
      const waveforms = Array.from({ length: 7 }, () => Math.floor(12 + Math.random() * 38));
      
      const voicePayload = {
        duration: duration,
        waveforms: waveforms
      };
      
      this.$emit('send-message', {
        content: JSON.stringify(voicePayload),
        msgType: 4 // VOICE_CALL_MESSAGE type
      });
      
      this.recordingDuration = 0;
    },
    parseVoiceWaveforms(content) {
      try {
        const data = JSON.parse(content);
        return data.waveforms || [12, 24, 18, 30, 24, 12, 8];
      } catch (e) {
        return [12, 24, 18, 30, 24, 12, 8];
      }
    },
    parseVoiceDuration(content) {
      try {
        const data = JSON.parse(content);
        return data.duration || 5;
      } catch (e) {
        return 5;
      }
    },
    playVoiceMessage(msg) {
      if (this.playingMsgId === msg.msgId) {
        this.playingMsgId = null;
        if (this.playTimer) {
          clearTimeout(this.playTimer);
          this.playTimer = null;
        }
        return;
      }
      
      this.playingMsgId = msg.msgId;
      const duration = this.parseVoiceDuration(msg.content);
      
      try {
        const AudioContextClass = window.AudioContext || window.webkitAudioContext;
        const ctx = new AudioContextClass();
        
        let elapsed = 0;
        const playTick = () => {
          if (this.playingMsgId !== msg.msgId) {
            try { ctx.close(); } catch(e){}
            return;
          }
          
          const osc = ctx.createOscillator();
          const gain = ctx.createGain();
          
          // Generate a smooth pop sound wave sweep
          const freq = 220 + Math.random() * 260;
          osc.frequency.setValueAtTime(freq, ctx.currentTime);
          osc.type = 'triangle';
          
          gain.gain.setValueAtTime(0.001, ctx.currentTime);
          gain.gain.linearRampToValueAtTime(0.08, ctx.currentTime + 0.05);
          gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + 0.35);
          
          osc.connect(gain);
          gain.connect(ctx.destination);
          osc.start();
          
          setTimeout(() => {
            try { osc.stop(); osc.disconnect(); gain.disconnect(); } catch(e){}
          }, 400);
          
          elapsed += 0.5;
          if (elapsed < duration) {
            this.playTimer = setTimeout(playTick, 500);
          } else {
            this.playingMsgId = null;
            try { ctx.close(); } catch(e){}
          }
        };
        
        playTick();
      } catch (e) {
        console.warn("Web Audio API playing failed", e);
        this.playTimer = setTimeout(() => {
          this.playingMsgId = null;
        }, duration * 1000);
      }
    },
    getVisualizerBarHeight(n) {
      if (this.recordingWaveforms && this.recordingWaveforms.length >= n) {
        return this.recordingWaveforms[n - 1];
      }
      return 10;
    },
    
    // Multi-Select & Forwarding Engine Logic
    startMultiSelect(msg) {
      this.isMultiSelectMode = true;
      this.selectedMsgIds = [msg.msgId];
    },
    exitMultiSelect() {
      this.isMultiSelectMode = false;
      this.selectedMsgIds = [];
    },
    toggleMsgSelection(msg) {
      if (!msg.msgId) return;
      const idx = this.selectedMsgIds.indexOf(msg.msgId);
      if (idx > -1) {
        this.selectedMsgIds.splice(idx, 1);
      } else {
        this.selectedMsgIds.push(msg.msgId);
      }
    },
    isMsgSelected(msg) {
      return this.selectedMsgIds.includes(msg.msgId);
    },
    forwardMessages(type) {
      if (this.selectedMsgIds.length === 0) return;
      
      const selectedMsgs = this.currentMsgs.filter(m => this.selectedMsgIds.includes(m.msgId));
      
      if (type === 'single') {
        // Forward each message separately
        selectedMsgs.forEach(msg => {
          this.$emit('send-message', {
            content: msg.content,
            msgType: msg.msgType || 1
          });
        });
        uni.showToast({ title: `已逐条转发 ${selectedMsgs.length} 条消息`, icon: 'success' });
        this.exitMultiSelect();
      } else if (type === 'combine') {
        // Build微信风格聊天记录合辑 JSON
        const title = this.currentSession.type === 'group'
          ? `群聊【${this.currentSession.name}】的聊天记录`
          : `与【${this.currentSession.name}】的聊天记录`;
          
        const summary = selectedMsgs.slice(0, 3).map(m => {
          const senderName = m.senderName || m.sender || '用户';
          const cleanText = m.msgType === 2 ? '[图片]' : (m.msgType === 4 ? '[语音消息]' : (m.msgType === 3 ? '[分享卡片]' : m.content));
          return `${senderName}: ${cleanText}`;
        });
        
        if (selectedMsgs.length > 3) {
          summary.push(`... 等共 ${selectedMsgs.length} 条消息`);
        }
        
        const messagesList = selectedMsgs.map(m => ({
          senderName: m.senderName || m.sender || '用户',
          content: m.content,
          msgType: m.msgType || 1,
          time: m.time || '10:00'
        }));
        
        const payload = {
          title: title,
          summary: summary,
          messages: messagesList
        };
        
        this.$emit('send-message', {
          content: JSON.stringify(payload),
          msgType: 5 // COMBINED forwarding type
        });
        
        uni.showToast({ title: `已合并转发 ${selectedMsgs.length} 条消息`, icon: 'success' });
        this.exitMultiSelect();
      }
    },
    showCombinedMessages(msg) {
      try {
        const data = JSON.parse(msg.content);
        this.combinedDrawer = {
          show: true,
          title: data.title || '聊天记录合辑',
          messages: data.messages || []
        };
      } catch (e) {
        uni.showToast({ title: '解析合并消息失败', icon: 'none' });
      }
    },
    parseCombinedTitle(content) {
      try {
        return JSON.parse(content).title || '聊天记录合辑';
      } catch (e) {
        return '聊天记录合辑';
      }
    },
    parseCombinedSummary(content) {
      try {
        return JSON.parse(content).summary || [];
      } catch (e) {
        return [];
      }
    },
    parseDeskCard(content) {
      if (!content) return {};
      try {
        return JSON.parse(content);
      } catch (e) {
        return { text: content };
      }
    },
    handleDeskOption(opt) {
      if (!opt || !opt.value) return;
      const action = opt.value;
      if (action === 'menu') {
        // Return to main menu
        this.$emit('send-desk-action', { action: 'get_welcome' });
      } else if (action === 'transfer_agent') {
        this.$emit('send-desk-action', { action: 'transfer_agent' });
      } else {
        this.$emit('send-desk-action', { action: 'ask_robot', robotMsg: action });
      }
    },
    setRatingHover(msgId, s) {
      if (!msgId) return;
      this.deskRatingHover = { ...this.deskRatingHover, [msgId]: s };
    },
    setRatingScore(msgId, s) {
      if (!msgId) return;
      this.deskRatingScore = { ...this.deskRatingScore, [msgId]: s };
    },
    submitDeskRating(msg) {
      const score = this.deskRatingScore[msg.msgId];
      if (!score) return;
      const feedback = this.deskFeedback[msg.msgId] || '';
      this.$emit('send-desk-action', {
        action: 'submit_rate',
        score: score,
        feedback: feedback,
        msgId: msg.msgId
      });
      // Optimistically mark as submitted
      msg.ratingSubmitted = true;
      this.$forceUpdate();
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
    border-radius: 18px 18px 18px 4px; /* Ins style other corners */
    font-size: 14px;
    line-height: 1.45;
    word-break: break-word;
    box-shadow: none;
    border: 1px solid var(--border-color);
    display: inline-block;
    text-align: left;
  }

  .self-msg .bubble-text-card {
    background: var(--ins-bubble-gradient);
    color: #ffffff;
    border: none;
    border-radius: 18px 18px 4px 18px; /* Ins style self corners */
    box-shadow: 0 2px 10px rgba(168, 0, 230, 0.12);
  }

  .bubble-image-card {
    max-width: 240px;
    max-height: 180px;
    border-radius: 18px 18px 18px 4px;
    cursor: pointer;
    box-shadow: 0 1px 4px rgba(0,0,0,0.06);
    transition: transform 0.2s ease;
  }
  .self-msg .bubble-image-card {
    border-radius: 18px 18px 4px 18px;
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

  /* Bottom input tray - Instagram Direct style */
  .ins-input-tray {
    padding: 14px 20px;
    border-top: 1px solid var(--border-color);
    background: var(--bg-chat-header);
    display: flex;
    align-items: center;
    gap: 12px;
    flex-shrink: 0;
    transition: background-color 0.3s ease, border-color 0.3s ease;
  }

  .ins-tray-add-btn {
    width: 32px;
    height: 32px;
    font-size: 16px;
    border-radius: 50%;
    color: var(--text-title);
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: transform 0.2s ease, background-color 0.2s ease;
  }
  .ins-tray-add-btn:hover {
    background-color: var(--btn-hover);
    transform: scale(1.08);
  }

  .ins-input-pill {
    flex: 1;
    display: flex;
    align-items: center;
    background: var(--input-bg);
    border: 1px solid var(--border-color);
    border-radius: 22px;
    padding: 2px 8px 2px 14px;
    gap: 8px;
    box-sizing: border-box;
    transition: border-color 0.2s ease, background-color 0.2s ease;
  }
  .ins-input-pill:focus-within {
    border-color: #a8a8a8;
  }

  .ins-pill-btn {
    width: 32px;
    height: 32px;
    font-size: 18px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    border-radius: 50%;
    user-select: none;
    transition: background-color 0.2s ease, transform 0.1s ease;
  }
  .ins-pill-btn:hover {
    background-color: var(--btn-hover);
    transform: scale(1.08);
  }
  .ins-pill-btn.active-recording {
    background-color: rgba(237, 73, 86, 0.15);
    color: #ed4956;
    animation: pulseGlow 1.2s infinite;
  }

  .ins-message-input {
    flex: 1;
    height: 36px;
    border: none;
    background: transparent;
    color: var(--text-title);
    font-size: 14px;
    outline: none;
    padding: 0;
    box-sizing: border-box;
  }

  .ins-pill-right-actions {
    display: flex;
    align-items: center;
    gap: 4px;
    transition: opacity 0.2s ease, max-width 0.2s ease, transform 0.2s ease;
    max-width: 80px;
    opacity: 1;
  }
  .ins-pill-right-actions.has-content {
    opacity: 0.3; /* Fade slightly when typing to focus on text input */
  }

  .ins-tray-right-action {
    width: 48px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .ins-send-text-btn {
    background: transparent;
    border: none;
    color: #0095f6; /* Ins signature active blue text */
    font-size: 14px;
    font-weight: 700;
    cursor: pointer;
    padding: 6px;
    transition: color 0.15s ease, opacity 0.15s ease;
  }
  .ins-send-text-btn:hover {
    color: #0077c5;
    opacity: 0.9;
  }

  .ins-thumbs-btn {
    font-size: 22px;
    cursor: pointer;
    user-select: none;
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.2s ease;
  }
  .ins-thumbs-btn:hover {
    transform: scale(1.15) rotate(-8deg);
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

  .status-indicator-blocked {
    width: 16px;
    height: 16px;
    background: #ff3b30;
    color: #ffffff;
    font-size: 10px;
    font-weight: bold;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    cursor: pointer;
    user-select: none;
    line-height: 1;
    box-shadow: 0 0 8px rgba(255, 59, 48, 0.6);
    animation: bounceIn 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275) both, pulseWarn 1.5s infinite;
  }

  @keyframes pulseWarn {
    0% { transform: scale(1); box-shadow: 0 0 8px rgba(255, 59, 48, 0.6); }
    50% { transform: scale(1.1); box-shadow: 0 0 16px rgba(255, 59, 48, 0.9); }
    100% { transform: scale(1); box-shadow: 0 0 8px rgba(255, 59, 48, 0.6); }
  }

  .blocked-alert-strip {
    margin-top: 8px;
    padding: 8px 12px;
    border-radius: 8px;
    background: rgba(255, 59, 48, 0.08);
    border: 1px dashed rgba(255, 59, 48, 0.25);
    color: #ff3b30;
    font-size: 12px;
    font-weight: 500;
    display: flex;
    align-items: center;
    gap: 6px;
    cursor: pointer;
    backdrop-filter: blur(10px);
    transition: all 0.2s ease;
    align-self: stretch;
    box-sizing: border-box;
  }
  .blocked-alert-strip:hover {
    background: rgba(255, 59, 48, 0.12);
    border-color: rgba(255, 59, 48, 0.45);
  }

  .shake-bubble {
    animation: shakeBubble 0.35s ease-in-out;
  }

  @keyframes shakeBubble {
    0%, 100% { transform: translateX(0); }
    20%, 60% { transform: translateX(-6px); }
    40%, 80% { transform: translateX(6px); }
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
  /* Reactions pills */
  .msg-reactions-wrapper {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin-top: 4px;
    align-self: flex-start;
  }
  .self-msg .msg-reactions-wrapper {
    align-self: flex-end;
    flex-direction: row-reverse;
  }

  .reaction-pill {
    position: relative;
    background: var(--input-bg);
    border: 1px solid var(--border-color);
    padding: 2px 8px;
    border-radius: 12px;
    font-size: 12px;
    cursor: pointer;
    display: flex;
    align-items: center;
    gap: 4px;
    user-select: none;
    transition: all 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  }
  .reaction-pill:hover {
    transform: scale(1.08);
    background: var(--btn-hover);
  }
  .reaction-pill.user-reacted {
    background: rgba(0, 132, 255, 0.1);
    border-color: var(--active-blue);
  }
  
  .reaction-emoji {
    font-size: 13px;
  }
  .reaction-count {
    font-weight: 700;
    color: var(--text-title);
  }

  /* Tooltip styling */
  .reaction-tooltip {
    position: absolute;
    bottom: calc(100% + 8px);
    left: 50%;
    transform: translateX(-50%) translateY(4px);
    background: rgba(24, 25, 26, 0.95);
    border: 1px solid var(--border-color);
    border-radius: 8px;
    padding: 8px 12px;
    box-shadow: var(--shadow-premium);
    opacity: 0;
    pointer-events: none;
    transition: all 0.15s cubic-bezier(0.16, 1, 0.3, 1);
    z-index: 200;
    width: max-content;
    max-width: 160px;
    text-align: center;
  }
  .reaction-pill:hover .reaction-tooltip {
    opacity: 1;
    transform: translateX(-50%) translateY(0);
    pointer-events: auto;
  }

  .tooltip-title {
    font-size: 11px;
    color: #8a8d91;
    display: block;
    margin-bottom: 4px;
    font-weight: 600;
  }
  .tooltip-users {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }
  .tooltip-user-item {
    font-size: 12px;
    color: #ffffff;
    font-weight: 500;
  }

  /* Floating Reactions Bar */
  .floating-reactions-bar {
    position: absolute;
    top: -38px;
    background: var(--bg-sidebar);
    border: 1px solid var(--border-color);
    backdrop-filter: blur(20px);
    border-radius: 20px;
    padding: 4px 8px;
    display: flex;
    gap: 8px;
    box-shadow: var(--shadow-premium);
    opacity: 0;
    pointer-events: none;
    transform: translateY(8px) scale(0.9);
    transition: all 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275);
    z-index: 100;
  }
  .self-msg .floating-reactions-bar {
    right: 0;
  }
  .msg-row:hover .floating-reactions-bar {
    opacity: 1;
    pointer-events: auto;
    transform: translateY(0) scale(1);
  }

  .floating-emoji-item {
    font-size: 20px;
    cursor: pointer;
    transition: transform 0.15s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  }
  .floating-emoji-item:hover {
    transform: scale(1.3) translateY(-4px);
  }

  /* Context Menu Reactions */
  .context-menu-reactions {
    display: flex;
    gap: 8px;
    padding: 6px 8px;
    justify-content: space-between;
  }
  .context-emoji-item {
    font-size: 22px;
    cursor: pointer;
    transition: transform 0.15s;
  }
  .context-emoji-item:hover {
    transform: scale(1.25);
  }
  .context-divider {
    height: 1px;
    background: var(--border-color);
    margin: 4px 0;
  }

  /* Custom share cards styling */
  .custom-card-wrapper {
    max-width: 280px;
    border-radius: 14px;
    overflow: hidden;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
    border: 1px solid var(--border-color);
    background: var(--bg-board);
    transition: all 0.25s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  }
  .custom-card-wrapper:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
  }

  /* User Share Card */
  .user-share-card {
    padding: 16px;
    display: flex;
    flex-direction: column;
    gap: 12px;
    text-align: left;
    background: linear-gradient(135deg, rgba(0, 132, 255, 0.03) 0%, rgba(168, 85, 247, 0.03) 100%);
  }
  .user-card-header {
    display: flex;
    align-items: center;
    gap: 12px;
  }
  .user-card-avatar {
    width: 44px;
    height: 44px;
    border-radius: 50%;
    background: linear-gradient(135deg, #0084ff 0%, #a855f7 100%);
    color: #ffffff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
    font-weight: 700;
  }
  .user-card-meta {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }
  .user-card-name {
    font-size: 15px;
    font-weight: 700;
    color: var(--text-title);
  }
  .user-card-id {
    font-size: 11px;
    color: var(--text-desc);
  }
  .user-card-bio {
    font-size: 13px;
    color: var(--text-desc);
    line-height: 1.4;
    padding: 8px 10px;
    background: var(--input-bg);
    border-radius: 8px;
    min-height: 36px;
  }
  .user-card-btn {
    border: none;
    background: linear-gradient(135deg, #0084ff, #00c6ff);
    color: #ffffff;
    padding: 8px 16px;
    border-radius: 8px;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
  }
  .user-card-btn:hover {
    filter: brightness(1.08);
  }

  /* Product Share Card */
  .product-share-card {
    display: flex;
    flex-direction: column;
  }
  .product-card-img {
    width: 100%;
    height: 140px;
    object-fit: cover;
  }
  .product-card-info {
    padding: 12px;
    display: flex;
    flex-direction: column;
    gap: 6px;
    text-align: left;
  }
  .product-card-title {
    font-size: 14px;
    font-weight: 700;
    color: var(--text-title);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .product-card-desc {
    font-size: 12px;
    color: var(--text-desc);
    line-height: 1.4;
    height: 32px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
  .product-card-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 6px;
  }
  .product-card-price {
    font-size: 15px;
    font-weight: 800;
    color: #ff9f0a;
  }
  .product-card-btn {
    border: none;
    background: var(--active-blue);
    color: #ffffff;
    padding: 6px 12px;
    border-radius: 6px;
    font-size: 12px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
  }
  .product-card-btn:hover {
    background: #0073e6;
  }

  /* ==========================================
     PHASE 7 VOICE & MULTISELECT PREMIUM CSS
     ========================================== */
  
  /* Voice Bubble Card */
  .voice-bubble-card {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 10px 16px;
    background: rgba(255, 255, 255, 0.08);
    border: 1px solid rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(16px);
    border-radius: 18px;
    cursor: pointer;
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.15);
    transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
    max-width: 240px;
    min-width: 130px;
    user-select: none;
  }
  .voice-bubble-card:hover {
    transform: translateY(-2px);
    background: rgba(255, 255, 255, 0.12);
    border-color: rgba(255, 255, 255, 0.2);
    box-shadow: 0 6px 20px rgba(0, 132, 255, 0.15);
  }
  .voice-bubble-card.playing {
    background: rgba(0, 132, 255, 0.15);
    border-color: rgba(0, 132, 255, 0.3);
    box-shadow: 0 0 15px rgba(0, 132, 255, 0.25);
  }
  .voice-play-icon {
    font-size: 16px;
    transition: transform 0.2s;
  }
  .voice-bubble-card.playing .voice-play-icon {
    transform: scale(1.15);
    animation: pulseIcon 1s infinite alternate;
  }
  .voice-wave-container {
    display: flex;
    align-items: center;
    gap: 3px;
    flex: 1;
    height: 30px;
  }
  .voice-wave-bar {
    width: 3px;
    background: linear-gradient(180deg, #a855f7, #0084ff);
    border-radius: 2px;
    transition: height 0.2s ease, background-color 0.2s ease;
  }
  .voice-bubble-card.playing .voice-wave-bar {
    background: linear-gradient(180deg, #00c6ff, #0072ff);
    animation: voiceBarPlay 0.6s infinite ease-in-out alternate;
  }
  /* Stagger animation delays for play state */
  .voice-bubble-card.playing .voice-wave-bar:nth-child(2n) {
    animation-delay: 0.15s;
  }
  .voice-bubble-card.playing .voice-wave-bar:nth-child(3n) {
    animation-delay: 0.3s;
  }
  .voice-duration {
    font-size: 12px;
    font-weight: 700;
    color: var(--text-desc);
    font-family: monospace;
  }

  /* Voice Recorder Overlay Panel */
  .voice-recorder-overlay {
    position: relative;
    background: rgba(15, 23, 42, 0.75);
    border: 1px solid rgba(255, 255, 255, 0.15);
    backdrop-filter: blur(30px);
    border-radius: 20px;
    padding: 20px 24px;
    margin: 10px 24px;
    display: flex;
    flex-direction: column;
    gap: 16px;
    box-shadow: 0 16px 40px rgba(0, 0, 0, 0.4);
    overflow: hidden;
  }
  .recorder-ambient-glow {
    position: absolute;
    top: -50px;
    left: 50%;
    transform: translateX(-50%);
    width: 140px;
    height: 140px;
    background: radial-gradient(circle, rgba(0, 132, 255, 0.25) 0%, rgba(0, 0, 0, 0) 70%);
    pointer-events: none;
    z-index: 1;
  }
  .recorder-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    z-index: 2;
  }
  .recorder-title {
    font-size: 14px;
    font-weight: 700;
    color: #ffffff;
    letter-spacing: 0.3px;
  }
  .recorder-timer {
    font-family: monospace;
    font-size: 16px;
    font-weight: 800;
    color: #ff3b30;
    background: rgba(255, 59, 48, 0.1);
    border: 1px solid rgba(255, 59, 48, 0.2);
    padding: 2px 10px;
    border-radius: 8px;
    animation: flashTimer 1s infinite alternate;
  }
  .recorder-visualizer {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 5px;
    height: 48px;
    z-index: 2;
  }
  .visualizer-bar {
    width: 4px;
    background: linear-gradient(180deg, #ff007f, #7f00ff);
    border-radius: 3px;
    transition: height 0.1s ease;
  }
  .recorder-actions {
    display: flex;
    gap: 16px;
    justify-content: center;
    z-index: 2;
  }
  .recorder-btn {
    border: none;
    outline: none;
    padding: 10px 24px;
    border-radius: 12px;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
  }
  .recorder-btn.cancel {
    background: rgba(255, 255, 255, 0.08);
    border: 1px solid rgba(255, 255, 255, 0.1);
    color: #ff3b30;
  }
  .recorder-btn.cancel:hover {
    background: rgba(255, 59, 48, 0.12);
    border-color: rgba(255, 59, 48, 0.2);
  }
  .recorder-btn.send {
    background: linear-gradient(135deg, #0084ff, #a855f7);
    color: #ffffff;
    box-shadow: 0 4px 15px rgba(0, 132, 255, 0.3);
  }
  .recorder-btn.send:hover {
    transform: translateY(-1px);
    box-shadow: 0 6px 20px rgba(0, 132, 255, 0.45);
  }

  /* Bottom Multi-Select Glassmorphic Bar */
  .bottom-multiselect-bar {
    position: relative;
    background: rgba(15, 23, 42, 0.8);
    border-top: 1px solid rgba(255, 255, 255, 0.12);
    backdrop-filter: blur(24px);
    padding: 16px 28px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    box-shadow: 0 -12px 30px rgba(0, 0, 0, 0.3);
  }
  .multiselect-info {
    font-size: 14px;
    font-weight: 600;
    color: var(--text-primary);
  }
  .multiselect-info .highlight-count {
    color: var(--active-blue);
    font-size: 18px;
    font-weight: 800;
    margin: 0 4px;
  }
  .multiselect-actions {
    display: flex;
    gap: 12px;
  }
  .multiselect-btn {
    border: none;
    outline: none;
    padding: 8px 18px;
    border-radius: 10px;
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
  }
  .multiselect-btn:disabled {
    opacity: 0.4;
    cursor: not-allowed;
    transform: none !important;
    box-shadow: none !important;
  }
  .multiselect-btn.cancel {
    background: rgba(255, 255, 255, 0.08);
    color: var(--text-primary);
    border: 1px solid rgba(255, 255, 255, 0.1);
  }
  .multiselect-btn.cancel:hover {
    background: rgba(255, 255, 255, 0.15);
  }
  .multiselect-btn.forward-single {
    background: rgba(168, 85, 247, 0.15);
    color: #c084fc;
    border: 1px solid rgba(168, 85, 247, 0.25);
  }
  .multiselect-btn.forward-single:hover:not(:disabled) {
    background: rgba(168, 85, 247, 0.25);
    transform: translateY(-1px);
  }
  .multiselect-btn.forward-combine {
    background: linear-gradient(135deg, #0084ff, #00c6ff);
    color: #ffffff;
    box-shadow: 0 4px 12px rgba(0, 132, 255, 0.25);
  }
  .multiselect-btn.forward-combine:hover:not(:disabled) {
    transform: translateY(-1px);
    box-shadow: 0 6px 18px rgba(0, 132, 255, 0.4);
  }

  /* Multi-Select Checkboxes on bubbles */
  .msg-checkbox-wrapper {
    padding-right: 12px;
    cursor: pointer;
    display: flex;
    align-items: center;
    align-self: center;
  }
  .msg-checkbox-circle {
    width: 20px;
    height: 20px;
    border-radius: 50%;
    border: 2px solid rgba(255, 255, 255, 0.3);
    background: rgba(255, 255, 255, 0.05);
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.22s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  }
  .msg-checkbox-circle:hover {
    border-color: var(--active-blue);
    transform: scale(1.1);
  }
  .msg-checkbox-circle.checked {
    background: linear-gradient(135deg, #0084ff, #a855f7);
    border-color: transparent;
    box-shadow: 0 0 10px rgba(0, 132, 255, 0.4);
  }
  .msg-check-symbol {
    color: #ffffff;
    font-size: 12px;
    font-weight: bold;
  }

  /* Combined Forwarding Message Card */
  .combined-bubble-card {
    background: rgba(255, 255, 255, 0.08);
    border: 1px solid rgba(255, 255, 255, 0.12);
    backdrop-filter: blur(16px);
    border-radius: 16px;
    padding: 14px;
    width: 260px;
    cursor: pointer;
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
    transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
    text-align: left;
  }
  .combined-bubble-card:hover {
    transform: translateY(-2px);
    background: rgba(255, 255, 255, 0.12);
    border-color: rgba(0, 132, 255, 0.25);
    box-shadow: 0 10px 25px rgba(0, 132, 255, 0.15);
  }
  .combined-card-header {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 8px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.06);
    padding-bottom: 6px;
  }
  .combined-card-icon {
    font-size: 16px;
  }
  .combined-card-title {
    font-size: 14px;
    font-weight: 700;
    color: var(--text-title);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .combined-card-body {
    display: flex;
    flex-direction: column;
    gap: 4px;
    margin-bottom: 10px;
  }
  .combined-summary-line {
    font-size: 12px;
    color: var(--text-desc);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  .combined-card-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 11px;
    color: var(--active-blue);
    font-weight: 600;
    border-top: 1px solid rgba(255, 255, 255, 0.06);
    padding-top: 8px;
  }
  .combined-card-footer .arrow-right {
    font-size: 10px;
    transition: transform 0.2s;
  }
  .combined-bubble-card:hover .combined-card-footer .arrow-right {
    transform: translateX(4px);
  }

  /* Sliding Drawer Modal */
  .drawer-backdrop {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 10000;
    background: rgba(10, 10, 15, 0.5);
    backdrop-filter: blur(10px);
    display: flex;
    justify-content: flex-end;
  }
  .drawer-glass-panel {
    width: 450px;
    max-width: 90%;
    height: 100%;
    background: rgba(17, 24, 39, 0.85);
    border-left: 1px solid rgba(255, 255, 255, 0.12);
    backdrop-filter: blur(40px) saturate(1.8);
    box-shadow: -20px 0 50px rgba(0, 0, 0, 0.4);
    display: flex;
    flex-direction: column;
    box-sizing: border-box;
  }
  .drawer-header {
    height: 72px;
    padding: 0 28px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  }
  .drawer-title {
    font-size: 17px;
    font-weight: 800;
    color: #ffffff;
    letter-spacing: -0.3px;
  }
  .drawer-close-btn {
    font-size: 20px;
    color: rgba(255, 255, 255, 0.5);
    cursor: pointer;
    transition: color 0.2s;
    width: 32px;
    height: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
  }
  .drawer-close-btn:hover {
    color: #ffffff;
    background: rgba(255, 255, 255, 0.08);
  }
  .drawer-messages-list {
    flex: 1;
    overflow-y: auto;
    padding: 24px 28px;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }
  .drawer-msg-row {
    display: flex;
    gap: 12px;
    align-items: flex-start;
  }
  .drawer-avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background: linear-gradient(135deg, #a855f7 0%, #0084ff 100%);
    color: #ffffff;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 15px;
    font-weight: 700;
    box-shadow: 0 4px 10px rgba(0, 0, 0, 0.2);
  }
  .drawer-msg-bubble {
    display: flex;
    flex-direction: column;
    gap: 4px;
    flex: 1;
  }
  .drawer-msg-meta {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  .drawer-msg-sender {
    font-size: 13px;
    font-weight: 700;
    color: rgba(255, 255, 255, 0.9);
  }
  .drawer-msg-time {
    font-size: 11px;
    color: rgba(255, 255, 255, 0.4);
  }
  .drawer-msg-body {
    background: rgba(255, 255, 255, 0.06);
    border: 1px solid rgba(255, 255, 255, 0.06);
    padding: 10px 14px;
    border-radius: 0 14px 14px 14px;
    font-size: 13.5px;
    color: rgba(255, 255, 255, 0.9);
    line-height: 1.45;
    word-break: break-all;
    align-self: flex-start;
  }
  .drawer-image-card {
    max-width: 200px;
    border-radius: 8px;
    cursor: pointer;
  }
  .drawer-empty {
    text-align: center;
    color: rgba(255, 255, 255, 0.35);
    font-size: 13px;
    padding: 40px;
  }

  /* Keyframe Animations */
  @keyframes voiceBarPlay {
    0% { height: 6px; }
    100% { height: 26px; }
  }
  @keyframes pulseIcon {
    from { transform: scale(1); }
    to { transform: scale(1.15); }
  }
  @keyframes flashTimer {
    from { opacity: 0.6; }
    to { opacity: 1; }
  }
  .animate-slide-left {
    animation: slideLeft 0.3s cubic-bezier(0.16, 1, 0.3, 1) forwards;
  }
  @keyframes slideLeft {
    from { transform: translateX(100%); }
    to { transform: translateX(0); }
  }

  /* ===================================================
     Smart Desk Card Styles (msgType === 6)
  ==================================================== */
  .desk-card-wrapper {
    max-width: 320px;
    min-width: 240px;
    width: 100%;
  }

  /* FAQ Card */
  .desk-faq-card {
    background: linear-gradient(135deg, rgba(0, 212, 170, 0.08) 0%, rgba(0, 132, 255, 0.08) 100%);
    border: 1px solid rgba(0, 212, 170, 0.25);
    border-radius: 16px;
    padding: 16px;
    display: flex;
    flex-direction: column;
    gap: 12px;
    backdrop-filter: blur(12px);
  }

  .desk-faq-text {
    font-size: 14px;
    color: var(--text-title);
    line-height: 1.6;
    white-space: pre-line;
  }

  .desk-options-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .desk-option-btn {
    background: linear-gradient(135deg, rgba(0, 212, 170, 0.12) 0%, rgba(0, 132, 255, 0.12) 100%);
    border: 1px solid rgba(0, 212, 170, 0.3);
    border-radius: 20px;
    color: var(--text-title);
    font-size: 13px;
    font-weight: 600;
    padding: 10px 16px;
    cursor: pointer;
    text-align: left;
    transition: all 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275);
    position: relative;
    overflow: hidden;
  }
  .desk-option-btn::before {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(135deg, rgba(0, 212, 170, 0.2), rgba(0, 132, 255, 0.2));
    opacity: 0;
    transition: opacity 0.2s;
  }
  .desk-option-btn:hover {
    transform: translateX(4px) scale(1.02);
    border-color: rgba(0, 212, 170, 0.6);
    box-shadow: 0 4px 16px rgba(0, 212, 170, 0.3);
  }
  .desk-option-btn:hover::before {
    opacity: 1;
  }
  .desk-option-btn:active {
    transform: translateX(2px) scale(0.98);
  }

  /* Rating Card */
  .desk-rating-card {
    background: linear-gradient(135deg, rgba(255, 214, 10, 0.06) 0%, rgba(255, 159, 10, 0.06) 100%);
    border: 1px solid rgba(255, 214, 10, 0.3);
    border-radius: 16px;
    padding: 20px;
    display: flex;
    flex-direction: column;
    gap: 16px;
    backdrop-filter: blur(12px);
  }

  .desk-rating-header {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .desk-rating-icon {
    font-size: 24px;
    animation: star-spin 2s linear infinite;
  }
  @keyframes star-spin {
    0%, 100% { transform: rotate(0deg) scale(1); }
    25% { transform: rotate(-10deg) scale(1.1); }
    75% { transform: rotate(10deg) scale(1.1); }
  }

  .desk-rating-title {
    font-size: 14px;
    font-weight: 700;
    color: var(--text-title);
    line-height: 1.4;
    flex: 1;
  }

  .desk-stars-row {
    display: flex;
    gap: 8px;
    justify-content: center;
  }

  .desk-star-item {
    font-size: 30px;
    cursor: pointer;
    filter: grayscale(1) opacity(0.4);
    transition: all 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275);
    user-select: none;
  }
  .desk-star-item:hover,
  .desk-star-item.active {
    filter: grayscale(0) opacity(1);
    transform: scale(1.25) rotate(-5deg);
    text-shadow: 0 0 12px rgba(255, 214, 10, 0.8);
  }
  .desk-star-item.active {
    animation: star-active-pulse 0.3s ease;
  }
  @keyframes star-active-pulse {
    0% { transform: scale(1.5) rotate(-8deg); }
    100% { transform: scale(1.25) rotate(-5deg); }
  }

  .desk-feedback-input {
    background: rgba(255, 255, 255, 0.06);
    border: 1px solid rgba(255, 214, 10, 0.25);
    border-radius: 10px;
    padding: 10px 12px;
    font-size: 13px;
    color: var(--text-title);
    resize: none;
    height: 64px;
    outline: none;
    font-family: inherit;
    transition: border-color 0.2s;
  }
  .desk-feedback-input:focus {
    border-color: rgba(255, 214, 10, 0.6);
  }
  .desk-feedback-input::placeholder {
    color: var(--input-placeholder);
    font-size: 12px;
  }

  .desk-submit-btn {
    background: linear-gradient(135deg, #ffd60a 0%, #ff9f0a 100%);
    color: #1a1a1a;
    border: none;
    border-radius: 20px;
    padding: 12px 16px;
    font-size: 13px;
    font-weight: 700;
    cursor: pointer;
    transition: all 0.2s ease;
    box-shadow: 0 4px 16px rgba(255, 214, 10, 0.4);
  }
  .desk-submit-btn:hover:not(:disabled) {
    filter: brightness(1.1);
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(255, 214, 10, 0.6);
  }
  .desk-submit-btn:disabled {
    background: var(--btn-hover);
    color: var(--text-desc);
    box-shadow: none;
    cursor: default;
  }

  /* Rating Success State */
  .desk-rating-success {
    background: linear-gradient(135deg, rgba(48, 209, 88, 0.1) 0%, rgba(0, 212, 170, 0.1) 100%);
    border: 1px solid rgba(48, 209, 88, 0.3);
    border-radius: 16px;
    padding: 20px;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 10px;
    text-align: center;
  }

  .desk-success-icon {
    font-size: 36px;
    animation: heart-beat 1s ease infinite;
  }
  @keyframes heart-beat {
    0%, 100% { transform: scale(1); }
    50% { transform: scale(1.2); }
  }

  .desk-success-text {
    font-size: 14px;
    font-weight: 600;
    color: #30d158;
  }

  /* Service bot special avatar in chat */
  .msg-avatar-circle.service-bot {
    background: linear-gradient(135deg, #00d4aa 0%, #0084ff 100%) !important;
    box-shadow: 0 0 12px rgba(0, 212, 170, 0.5);
    animation: servicebot-glow 2.5s ease-in-out infinite;
  }

  /* Instagram style active meeting banner styles */
  .instagram-meeting-btn {
    animation: scale-up-pulse 2s infinite ease-in-out;
  }
  @keyframes scale-up-pulse {
    0%, 100% { transform: scale(1); filter: drop-shadow(0 0 2px rgba(220, 39, 67, 0.3)); }
    50% { transform: scale(1.1); filter: drop-shadow(0 0 8px rgba(220, 39, 67, 0.7)); }
  }

  .instagram-meeting-active-banner {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 20px;
    margin: 12px 24px;
    border-radius: 12px;
    background: linear-gradient(45deg, rgba(240, 148, 51, 0.08), rgba(230, 104, 60, 0.08), rgba(220, 39, 67, 0.08), rgba(204, 35, 102, 0.08));
    border: 1px solid rgba(230, 104, 60, 0.18);
    backdrop-filter: blur(16px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
    cursor: pointer;
    overflow: hidden;
    z-index: 100;
  }
  .instagram-meeting-active-banner:hover {
    background: linear-gradient(45deg, rgba(240, 148, 51, 0.12), rgba(230, 104, 60, 0.12), rgba(220, 39, 67, 0.12), rgba(204, 35, 102, 0.12));
    border-color: rgba(230, 104, 60, 0.28);
    box-shadow: 0 12px 30px rgba(0, 0, 0, 0.18);
  }
  .banner-icon {
    font-size: 18px;
    margin-right: 12px;
    color: #e6683c;
  }
  .banner-text {
    flex: 1;
    font-size: 13.5px;
    font-weight: 650;
    color: var(--text-title);
    letter-spacing: -0.2px;
  }
  .banner-text .bold-text {
    color: #e6683c;
    font-weight: 800;
  }
  .join-meeting-btn-ins {
    background: linear-gradient(45deg, #f09433 0%, #e6683c 25%, #dc2743 50%, #cc2366 75%, #bc1888 100%);
    color: #ffffff;
    border: none;
    outline: none;
    padding: 6px 14px;
    font-size: 11px;
    font-weight: 750;
    border-radius: 20px;
    cursor: pointer;
    box-shadow: 0 4px 12px rgba(220, 39, 67, 0.35);
    transition: all 0.2s ease;
  }
  .join-meeting-btn-ins:hover {
    transform: scale(1.06);
    box-shadow: 0 6px 16px rgba(220, 39, 67, 0.45);
  }

  .animate-pulse-scale {
    animation: ins-pulse 1.8s infinite ease-in-out;
  }
  @keyframes ins-pulse {
    0%, 100% { transform: scale(1); }
    50% { transform: scale(1.22); }
  }

  /* Instagram style Alert Overlay & Dialog */
  .ins-alert-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.45);
    backdrop-filter: blur(3px);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 99999;
    animation: fadeInModal 0.2s ease-out forwards;
  }

  .ins-alert-dialog {
    width: 280px;
    background: var(--bg-board);
    border: 1px solid var(--border-color);
    border-radius: 12px;
    padding-top: 24px;
    box-shadow: 0 10px 30px rgba(0,0,0,0.25);
    text-align: center;
    overflow: hidden;
  }

  .ins-alert-title {
    font-size: 16px;
    font-weight: 700;
    color: var(--text-title);
    margin-bottom: 8px;
    padding: 0 16px;
  }

  .ins-alert-body {
    font-size: 13.5px;
    line-height: 1.45;
    color: var(--text-desc);
    margin-bottom: 24px;
    padding: 0 20px;
  }

  .ins-alert-divider {
    height: 1px;
    background-color: var(--border-color);
    width: 100%;
  }

  .ins-alert-btn.confirm {
    width: 100%;
    padding: 14px 0;
    background: transparent;
    border: none;
    outline: none;
    color: #0095f6; /* Instagram elegant blue confirm text */
    font-size: 14px;
    font-weight: 700;
    cursor: pointer;
    transition: background-color 0.2s ease;
  }
  .ins-alert-btn.confirm:active {
    background-color: var(--btn-hover);
  }

  @keyframes fadeInModal {
    from { opacity: 0; }
    to { opacity: 1; }
  }
</style>
