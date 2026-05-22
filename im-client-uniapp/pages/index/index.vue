<template>
	<view class="app-container">
		<!-- Background Decorative Lights -->
		<view class="glow-orb orb-1"></view>
		<view class="glow-orb orb-2"></view>

		<!-- ================= LOGIN SCREEN ================= -->
		<view v-if="!isLogin" class="login-wrapper">
			<view class="login-glass-card">
				<view class="login-header">
					<view class="logo-badge">💬</view>
					<text class="login-title">govg-im 开放服务化体验仓</text>
					<text class="login-subtitle">基于 Spring Boot 3 & Netty 的高性能即时通讯平台</text>
				</view>

				<!-- Tab Switches for Login Modes -->
				<view class="login-tab-header">
					<view class="login-tab" :class="{ active: loginMode === 'token' }" @click="loginMode = 'token'">
						🔑 Token 安全登录
					</view>
					<view class="login-tab" :class="{ active: loginMode === 'legacy' }" @click="loginMode = 'legacy'">
						👤 游客直接登录
					</view>
				</view>

				<!-- Forms -->
				<view class="login-form">
					<!-- Token Login -->
					<view v-if="loginMode === 'token'" class="form-fade">
						<view class="glass-input-group">
							<text class="input-icon">🔑</text>
							<input class="glass-input" v-model="secureTokenInput" placeholder="请粘贴由开放接口申请到的 Token" />
						</view>
						<view class="input-tip-box">
							💡 提示：可以在左上角“调试”或在外部系统通过调用 <code class="code-highlight">/api/external/auth/token</code> 获取安全 Token。
						</view>
					</view>

					<!-- Legacy Login -->
					<view v-else class="form-fade">
						<view class="glass-input-group">
							<text class="input-icon">👤</text>
							<input class="glass-input" v-model="userIdInput" placeholder="请输入用户 ID (英文/数字)" />
						</view>
						<view class="glass-input-group">
							<text class="input-icon">📛</text>
							<input class="glass-input" v-model="username" placeholder="请输入昵称 (不填同 ID)" />
						</view>
					</view>

					<button class="glass-btn primary-glow" @click="login">进入系统</button>
				</view>

				<view class="connection-status-bar" :class="{ active: isConnected }">
					<span class="status-dot"></span>
					{{ isConnected ? '已建立 WebSocket 网络连接' : '正在建立安全通道中...' }}
				</view>
			</view>
		</view>

		<!-- ================= MAIN APP BOARD ================= -->
		<view v-else class="app-glass-board">
			<!-- 1. LEFT NARROW NAVIGATION BAR -->
			<view class="nav-sidebar">
				<view class="user-avatar-wrapper" @click="copyId" title="点击复制用户 ID">
					<view class="user-avatar-gradient">
						<text class="avatar-letter">{{ username ? username[0].toUpperCase() : 'U' }}</text>
					</view>
					<span class="online-indicator active"></span>
				</view>

				<view class="nav-menu">
					<view class="nav-item" :class="{ active: activeTab === 'chats' }" @click="activeTab = 'chats'">
						<text class="nav-icon">💬</text>
						<text class="nav-label">聊天</text>
						<view v-if="totalUnread > 0" class="unread-badge-mini">{{ totalUnread }}</view>
					</view>
					<view class="nav-item" :class="{ active: activeTab === 'contacts' }" @click="activeTab = 'contacts'">
						<text class="nav-icon">👥</text>
						<text class="nav-label">联系人</text>
					</view>
					<view class="nav-item" :class="{ active: activeTab === 'console' }" @click="activeTab = 'console'">
						<text class="nav-icon">🛠️</text>
						<text class="nav-label">测试台</text>
					</view>
					<view class="nav-item" :class="{ active: activeTab === 'settings' }" @click="activeTab = 'settings'">
						<text class="nav-icon">⚙️</text>
						<text class="nav-label">设置</text>
					</view>
				</view>

				<view class="logout-btn" @click="logout" title="退出系统">
					<text class="logout-icon">🚪</text>
				</view>
			</view>

			<!-- 2. MIDDLE LISTING AREA -->
			<view class="middle-sidebar" :class="{ 'mobile-hidden': view === 'chat' }">
				
				<!-- Tab 1: Session Chat list -->
				<view v-if="activeTab === 'chats'" class="full-height flex-col">
					<view class="sidebar-header">
						<text class="sidebar-title">Chats</text>
						<view class="header-actions">
							<view class="circle-action-btn" @click="showStartChat = true" title="发起私聊">💬</view>
							<view class="circle-action-btn" @click="showCreateGroup = true" title="群组操作">+</view>
						</view>
					</view>
					<view class="search-box">
						<input class="glass-search" placeholder="搜索会话..." />
					</view>
					<scroll-view scroll-y="true" class="list-scroll-view">
						<view v-if="sessions.length === 0" class="empty-list-tip">暂无活动会话</view>
						<view v-for="(session, index) in sessions" :key="index" 
							  class="session-glass-card" :class="{ active: currentSession && currentSession.id === session.id }"
							  @click="openChat(session)">
							<view class="avatar-circle">
								{{ session.name[0].toUpperCase() }}
								<span class="online-indicator active"></span>
							</view>
							<view class="session-info">
								<view class="session-header-row">
									<text class="session-name">{{ session.name }}</text>
									<text class="session-time">{{ session.time }}</text>
								</view>
								<view class="session-body-row">
									<text class="session-preview">{{ session.lastMsg }}</text>
									<view v-if="session.unread > 0" class="unread-bubble">{{ session.unread }}</view>
								</view>
							</view>
						</view>
					</scroll-view>
				</view>

				<!-- Tab 2: Contacts list -->
				<view v-if="activeTab === 'contacts'" class="full-height flex-col">
					<view class="sidebar-header">
						<text class="sidebar-title">Contacts</text>
					</view>
					<view class="search-box">
						<input class="glass-search" placeholder="搜索好友..." />
					</view>
					<scroll-view scroll-y="true" class="list-scroll-view">
						<view class="contact-section-title">我的好友列表</view>
						<view v-for="friend in friendList" :key="friend.id" class="contact-glass-card" @click="startChatWith(friend.id)">
							<view class="avatar-circle font-bold">
								{{ friend.name[0].toUpperCase() }}
								<span class="online-indicator" :class="{ active: friend.online }"></span>
							</view>
							<view class="contact-info">
								<text class="contact-name">{{ friend.name }}</text>
								<text class="contact-status" :class="{ online: friend.online }">
									{{ friend.online ? '在线 (' + friend.node + ')' : '离线' }}
								</text>
							</view>
							<view class="quick-chat-action">💬</view>
						</view>
					</scroll-view>
				</view>

				<!-- Tab 3: Developer Platform Console -->
				<view v-if="activeTab === 'console'" class="full-height flex-col">
					<view class="sidebar-header">
						<text class="sidebar-title">Dev Console</text>
					</view>
					<scroll-view scroll-y="true" class="list-scroll-view pad-15">
						
						<!-- Simulator 1: Token Ticket Generator -->
						<view class="console-glass-card">
							<text class="console-card-title">🔑 接口 1: Token 登录票据生成器</text>
							<text class="console-card-desc">模拟外部项目后台，调用本 IM 接口获取授权凭证。</text>
							
							<view class="mini-form">
								<input class="mini-input" v-model="simTokenUserId" placeholder="输入用户 ID" />
								<input class="mini-input" v-model="simTokenUsername" placeholder="输入用户昵称" />
								<button class="mini-btn" @click="simGenerateToken">调用申请 Token</button>
							</view>

							<view v-if="generatedToken" class="console-terminal">
								<text class="terminal-text-cyan">Token 申请成功！</text>
								<text class="terminal-text-white">Token: {{ generatedToken }}</text>
								<button class="terminal-action-btn" @click="copyAndUseToken">复制并使用该 Token 登录</button>
							</view>
						</view>

						<!-- Simulator 2: System Message Pusher -->
						<view class="console-glass-card">
							<text class="console-card-title">🔔 接口 2: 三方系统消息推送模拟</text>
							<text class="console-card-desc">直接调用 REST API，利用 Redis 订阅机制跨实例强推送消息给客户端。</text>
							
							<view class="mini-form">
								<input class="mini-input" v-model="simPushToUserId" placeholder="推送目标用户 ID (默认为我自己)" />
								<input class="mini-input" v-model="simPushSender" placeholder="模拟三方推送系统名称" />
								<textarea class="mini-textarea" v-model="simPushMsg" placeholder="请输入推送卡片内容..."></textarea>
								<button class="mini-btn primary-glow" @click="simTriggerPush">触发外部系统推送</button>
							</view>
						</view>

						<!-- Simulator 3: Online Status Scanner -->
						<view class="console-glass-card">
							<text class="console-card-title">🛰️ 接口 3: 全局在线状态探测仪</text>
							<text class="console-card-desc">实时向 IM 查询任意用户的全局在线实例和路由状态。</text>
							
							<view class="mini-form">
								<input class="mini-input" v-model="simCheckUserId" placeholder="输入待查询用户 ID" />
								<button class="mini-btn" @click="simCheckOnline">查询在线状态</button>
							</view>

							<view v-if="onlineCheckResult" class="console-terminal">
								<pre class="terminal-json">{{ onlineCheckResult }}</pre>
							</view>
						</view>

					</scroll-view>
				</view>

				<!-- Tab 4: System Parameters Settings -->
				<view v-if="activeTab === 'settings'" class="full-height flex-col">
					<view class="sidebar-header">
						<text class="sidebar-title">Settings</text>
					</view>
					<scroll-view scroll-y="true" class="list-scroll-view pad-15">
						<view class="console-glass-card">
							<text class="console-card-title">⚙️ IM 服务物理节点配置</text>
							<text class="console-card-desc">前端连接及调用外部接口的服务器地址参数。</text>
							
							<view class="glass-label-group">
								<text class="glass-mini-label">HTTP REST API 地址</text>
								<input class="mini-input" v-model="serverHttpUrl" />
							</view>
							
							<view class="glass-label-group">
								<text class="glass-mini-label">WebSocket 协议地址</text>
								<input class="mini-input" v-model="serverWsUrl" />
							</view>

							<view class="glass-label-group">
								<text class="glass-mini-label">模拟三方调用的 API Key</text>
								<input class="mini-input" v-model="externalApiKey" placeholder="对应后端 application.yml 密钥" />
							</view>

							<button class="glass-btn primary-glow margin-top-15" @click="saveSettings">保存网络配置</button>
						</view>

						<view class="console-glass-card">
							<text class="console-card-title">📱 关于 govg-im Client</text>
							<view class="about-item">当前用户: <text class="font-bold text-white">{{ userId }}</text></view>
							<view class="about-item">连接状态: <text class="text-green">ONLINE</text></view>
							<view class="about-item">开发框架: Uni-App (H5 / CSS Glassmorphism)</view>
						</view>
					</scroll-view>
				</view>

			</view>

			<!-- 3. RIGHT CORE CHAT AREA -->
			<view class="chat-viewport" :class="{ 'mobile-active': view === 'chat' }">
				<view v-if="currentSession" class="chat-frame">
					<!-- Chat Header -->
					<view class="chat-header-bar">
						<view class="back-action-btn" @click="view = 'list'">
							<text class="back-arrow">❮</text>
							<text class="back-label">返回</text>
							<span v-if="totalUnread > 0" class="header-unread-count">{{ totalUnread }}</span>
						</view>
						
						<view class="header-user-info">
							<text class="header-user-name">{{ currentSession.name }}</text>
							<view class="header-user-status">
								<span class="status-dot"></span>
								Active Now
							</view>
						</view>

						<view class="header-actions">
							<view class="header-icon-btn" @click="showRightPanel = !showRightPanel" title="会话信息">ℹ️</view>
						</view>
					</view>

					<!-- Messages Scroller -->
					<scroll-view scroll-y="true" class="chat-messages-scroller" :scrollTop="scrollTop" :scroll-into-view="scrollIntoView" @scroll="onScroll">
						<view v-for="(msg, index) in currentMsgs" :key="index" class="msg-bubble-wrapper" 
							  :class="{ 'self-msg': msg.isSelf, 'system-push-msg': msg.sender === 'system_service' }" :id="'msg-'+index">
							
							<!-- Time Break -->
							<view class="msg-time-divider" v-if="shouldShowTime(index)">{{ msg.time }}</view>
							
							<!-- Normal Message Bubble Row -->
							<view v-if="msg.sender !== 'system_service'" class="msg-row">
								<view class="msg-avatar-circle" v-if="!msg.isSelf">{{ msg.sender[0].toUpperCase() }}</view>
								<view class="msg-bubble-content">
									<text class="sender-name-label" v-if="!msg.isSelf && currentSession.type === 'group'">{{ msg.sender }}</text>
									
									<!-- Image rendering -->
									<image v-if="msg.msgType === 2" :src="formatImageUrl(msg.content)" class="bubble-image-card" mode="widthFix" @click="previewImage(msg.content)"></image>
									<!-- Text rendering -->
									<text v-else class="bubble-text-card">{{ msg.content }}</text>
								</view>
							</view>

							<!-- Beautiful System Push Message Card -->
							<view v-else class="system-push-card animate-pop">
								<view class="system-card-header">
									<text class="system-card-icon">🔔</text>
									<view class="system-card-meta">
										<text class="system-card-title">{{ msg.senderName || '三方系统通知' }}</text>
										<text class="system-card-time">{{ msg.time }}</text>
									</view>
									<span class="system-badge">外部 API 推送</span>
								</view>
								<view class="system-card-body">
									{{ msg.content }}
								</view>
							</view>

						</view>
						<view id="scroll-bottom"></view>
					</scroll-view>

					<!-- Bottom Input Tray -->
					<view class="bottom-input-tray">
						<view class="tray-action-btn" @click="chooseImage" title="发送图片">📷</view>
						<input class="glass-message-input" v-model="msgContent" placeholder="输入消息..." confirm-type="send" @confirm="sendMsg" />
						<button class="tray-send-btn" @click="sendMsg">发送</button>
					</view>
				</view>

				<!-- Empty Chat View -->
				<view v-else class="chat-empty-fallback">
					<view class="fallback-glow-badge">💬</view>
					<text class="fallback-title">欢迎使用 govg-im Messenger</text>
					<text class="fallback-desc">点击左侧会话发起聊天，或者在“测试台”调用系统 API 模拟即时通讯。</text>
				</view>
			</view>

			<!-- 4. RIGHT CHAT DETAIL SIDEBAR (INFO PANEL) -->
			<view v-if="currentSession && showRightPanel" class="right-info-sidebar">
				<scroll-view scroll-y="true" class="full-height flex-col pad-20">
					<view class="right-profile-header">
						<view class="big-profile-avatar">{{ currentSession.name[0].toUpperCase() }}</view>
						<text class="right-profile-name">{{ currentSession.name }}</text>
						<text class="right-profile-id">ID: {{ currentSession.id }}</text>
					</view>

					<view class="right-divider"></view>

					<view class="right-section">
						<text class="right-section-title">⚙️ 会话操作</text>
						<view class="glass-menu-item" @click="clearHistory">🗑️ 清除聊天记录</view>
						<view class="glass-menu-item" @click="copySessionId">📋 复制会话 ID</view>
					</view>

					<view class="right-section" v-if="currentSession.type === 'group'">
						<text class="right-section-title">👥 群成员 (Mock)</text>
						<view class="member-list-mini">
							<view class="member-mini-card">
								<span class="member-mini-avatar">我</span>
								<text class="member-mini-name">我 (创建者)</text>
							</view>
						</view>
					</view>

					<view class="right-section">
						<text class="right-section-title">🖼️ 共享的图片媒体</text>
						<view class="shared-media-grid">
							<view v-for="(img, idx) in sharedImages" :key="idx" class="shared-img-card" @click="previewImage(img)">
								<image :src="img" class="shared-img-thumb" mode="aspectFill"></image>
							</view>
							<view v-if="sharedImages.length === 0" class="no-media-tip">暂无共享图片</view>
						</view>
					</view>
				</scroll-view>
			</view>
		</view>

		<!-- ================= START CHAT MODAL ================= -->
		<view v-if="showStartChat" class="modal-backdrop">
			<view class="modal-glass-card">
				<text class="modal-title">发起新聊天</text>
				<input class="mini-input" v-model="newChatId" placeholder="请输入目标用户 ID" />
				<view class="modal-actions-row">
					<button class="glass-btn primary-glow" @click="startNewChat">确认发起</button>
					<button class="glass-btn secondary-dark" @click="showStartChat = false">取消</button>
				</view>
			</view>
		</view>

		<!-- ================= GROUP MODAL ================= -->
		<view v-if="showCreateGroup" class="modal-backdrop">
			<view class="modal-glass-card">
				<text class="modal-title">群组加入与创建</text>
				<input class="mini-input" v-model="targetGroupId" placeholder="请输入群组 ID 加入" />
				<button class="glass-btn primary-glow margin-bottom-15" @click="joinGroup">加入该群组</button>
				<view class="modal-divider">或</view>
				<button class="glass-btn cyan-glow margin-bottom-15" @click="createGroup">创建一个新群组</button>
				<button class="glass-btn secondary-dark" @click="showCreateGroup = false">取消</button>
			</view>
		</view>
	</view>
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
				scrollIntoView: '',
				isAtBottom: true,
				newMsgCount: 0,

				// Hardcoded Friends List (Simulating contacts, status checkable)
				friendList: [
					{ id: 'admin', name: '系统管理员', online: false, node: 'N/A' },
					{ id: 'user1', name: '测试用户 1', online: false, node: 'N/A' },
					{ id: 'user2', name: '测试用户 2', online: false, node: 'N/A' },
					{ id: 'user3', name: '测试用户 3', online: false, node: 'N/A' }
				],
				statusCheckTimer: null
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
		onLoad() {
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
		onUnload() {
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
				const query = uni.createSelectorQuery().in(this);
				query.select('.chat-messages-scroller').boundingClientRect(data => {
					if (!data) return;
					const viewHeight = data.height;
					const { scrollHeight, scrollTop } = e.detail;
					if (scrollHeight - scrollTop - viewHeight < 60) {
						this.isAtBottom = true;
						this.newMsgCount = 0;
					} else {
						this.isAtBottom = false;
					}
				}).exec();
			},

			formatImageUrl(url) {
				// Fix direct localhost mappings in H5 env if needed
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

				this.socketTask.onOpen((res) => {
					this.isConnected = true;
					if (this.userId) {
						// Simple reconnection login
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

				// Fallback command mapping
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
						
						// Fill pusher input by default
						this.simPushToUserId = this.userId;
						
						uni.showToast({ title: '成功安全接入 IM 系统', icon: 'success' });
					} else {
						uni.showModal({
							title: '连接拒绝',
							content: packet.reason || '登录凭证无效或过期',
							showCancel: false
						});
					}
				}
				else if (packet.command === 4) { // MESSAGE_RESPONSE (Single Message)
					const sessionId = packet.fromUserId;
					const sessionName = packet.fromUserName || packet.fromUserId;
					this.onReceiveMessage(sessionId, sessionName, 'user', packet.message, packet.fromUserId, packet.msgType, packet.fromUserName);
					this.sendAck(packet.msgId);
				}
				else if (packet.command === 12) { // GROUP_MESSAGE_RESPONSE (Group Message)
					const sessionId = 'g:' + packet.fromGroupId;
					const senderName = packet.fromUser;
					this.onReceiveMessage(sessionId, `群组 ${packet.fromGroupId}`, 'group', packet.message, senderName, packet.msgType);
				}
				else if (packet.command === 8) { // CREATE_GROUP_RESPONSE
					if (packet.success) {
						uni.showToast({ title: '群组创建成功: ' + packet.groupId, icon: 'success' });
						this.startSession('g:' + packet.groupId, '群组 ' + packet.groupId, 'group');
						this.showCreateGroup = false;
					}
				}
				else if (packet.command === 10) { // JOIN_GROUP_RESPONSE
					if (packet.success) {
						uni.showToast({ title: '已加入群组 ' + packet.groupId, icon: 'success' });
						this.startSession('g:' + packet.groupId, '群组 ' + packet.groupId, 'group');
						this.showCreateGroup = false;
					}
				}
			},

			onReceiveMessage(sessionId, sessionName, type, content, senderName, msgType = 1, fromUserName = null) {
				if (!this.messages[sessionId]) {
					this.$set(this.messages, sessionId, []);
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
					this.$set(this.messages, id, []);
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
				this.startSession(this.newChatId, this.newChatId, 'user');
				this.showStartChat = false;
				this.newChatId = '';
			},

			startChatWith(userId) {
				this.startSession(userId, userId, 'user');
				this.activeTab = 'chats';
			},

			// Core Login Action
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
					// Send connection using IM Token
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

				if (!this.messages[session.id]) this.$set(this.messages, session.id, []);
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
					this.scrollIntoView = '';
					this.$nextTick(() => {
						this.scrollIntoView = 'scroll-bottom';
						this.scrollTop = 9999999;
					});
				});
			},

			clearHistory() {
				uni.showModal({
					title: '清除聊天',
					content: '确定要清空与当前用户的聊天记录吗？',
					success: (res) => {
						if (res.confirm) {
							this.$set(this.messages, this.currentSession.id, []);
							const session = this.sessions.find(s => s.id === this.currentSession.id);
							if (session) session.lastMsg = '聊天记录已清空';
							this.saveData();
						}
					}
				});
			},

			// ==========================================
			// 🛠️ DEVELOPER TESTING CONSOLE METHODS 🛠️
			// ==========================================

			// Simulator 1: Request IM Token via HTTP REST
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
						this.logout(); // trigger logout to let user re-login
					}
				});
			},

			// Simulator 2: Push system message via REST
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

			// Simulator 3: Detect global online status
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

			// Start scanning friend status via REST APIs
			startFriendStatusScanner() {
				this.scanFriendsOnlineStatus(); // scan once first
				this.statusCheckTimer = setInterval(() => {
					this.scanFriendsOnlineStatus();
				}, 10000); // scan every 10s
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
			}
		}
	}
</script>

<style>
	/* ==========================================
	   💅 GLOBAL CSS AESTHETICS (Facebook Glassmorphism)
	   ========================================== */

	/* Hide Scrollbar completely */
	::-webkit-scrollbar {
		width: 0;
		height: 0;
		color: transparent;
		display: none;
	}

	page {
		margin: 0;
		padding: 0;
		height: 100%;
		background-color: #0f172a;
		overflow: hidden;
	}

	.app-container {
		position: relative;
		width: 100vw;
		height: 100vh;
		background: radial-gradient(circle at top left, #1e1b4b, #0f172a);
		color: #f1f5f9;
		font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
		overflow: hidden;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	/* Beautiful background glowing balls */
	.glow-orb {
		position: absolute;
		border-radius: 50%;
		filter: blur(120px);
		opacity: 0.15;
		z-index: 1;
		pointer-events: none;
	}
	.orb-1 {
		width: 500px;
		height: 500px;
		background: #6366f1;
		top: -100px;
		left: -100px;
	}
	.orb-2 {
		width: 400px;
		height: 400px;
		background: #a855f7;
		bottom: -50px;
		right: -50px;
	}

	/* Fonts & General classes */
	.font-bold { font-weight: bold; }
	.pad-15 { padding: 15px; }
	.pad-20 { padding: 20px; }
	.full-height { height: 100%; }
	.flex-col { display: flex; flex-direction: column; }
	.text-white { color: #ffffff !important; }
	.text-green { color: #10b981 !important; }
	.margin-bottom-15 { margin-bottom: 15px; }
	.margin-top-15 { margin-top: 15px; }

	/* Animations */
	@keyframes glow {
		0%, 100% { box-shadow: 0 0 10px rgba(16, 185, 129, 0.4); }
		50% { box-shadow: 0 0 20px rgba(16, 185, 129, 0.8); }
	}
	
	@keyframes pop {
		0% { transform: scale(0.9); opacity: 0; }
		100% { transform: scale(1); opacity: 1; }
	}

	.animate-pop {
		animation: pop 0.3s cubic-bezier(0.16, 1, 0.3, 1) forwards;
	}

	/* ==========================================
	   👤 LOGIN PORTAL STYLING
	   ========================================== */
	.login-wrapper {
		z-index: 10;
		width: 90%;
		max-width: 460px;
	}

	.login-glass-card {
		background: rgba(255, 255, 255, 0.03);
		border: 1px solid rgba(255, 255, 255, 0.07);
		border-radius: 24px;
		padding: 35px 30px;
		backdrop-filter: blur(25px);
		box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4);
		display: flex;
		flex-direction: column;
	}

	.login-header {
		text-align: center;
		margin-bottom: 25px;
	}

	.logo-badge {
		font-size: 40px;
		margin-bottom: 15px;
	}

	.login-title {
		display: block;
		font-size: 22px;
		font-weight: 700;
		color: #ffffff;
		margin-bottom: 8px;
		letter-spacing: 0.5px;
	}

	.login-subtitle {
		font-size: 13px;
		color: #94a3b8;
		line-height: 1.4;
	}

	.login-tab-header {
		display: flex;
		background: rgba(255, 255, 255, 0.04);
		border-radius: 12px;
		padding: 4px;
		margin-bottom: 25px;
		border: 1px solid rgba(255, 255, 255, 0.03);
	}

	.login-tab {
		flex: 1;
		text-align: center;
		font-size: 13px;
		padding: 10px;
		color: #94a3b8;
		border-radius: 8px;
		cursor: pointer;
		transition: all 0.2s;
	}

	.login-tab.active {
		background: rgba(255, 255, 255, 0.08);
		color: #ffffff;
		font-weight: 600;
		backdrop-filter: blur(10px);
	}

	.login-form {
		display: flex;
		flex-direction: column;
		margin-bottom: 20px;
	}

	.form-fade {
		animation: pop 0.25s ease-out;
	}

	.glass-input-group {
		display: flex;
		align-items: center;
		background: rgba(255, 255, 255, 0.04);
		border: 1px solid rgba(255, 255, 255, 0.08);
		border-radius: 12px;
		padding: 0 15px;
		height: 50px;
		margin-bottom: 15px;
	}

	.input-icon {
		font-size: 18px;
		margin-right: 12px;
		color: #94a3b8;
	}

	.glass-input {
		flex: 1;
		background: transparent;
		border: none;
		color: #ffffff;
		font-size: 15px;
	}

	.input-tip-box {
		font-size: 11px;
		color: #64748b;
		line-height: 1.4;
		margin-bottom: 15px;
		padding: 0 5px;
	}

	.code-highlight {
		background: rgba(99, 102, 241, 0.15);
		color: #818cf8;
		padding: 2px 5px;
		border-radius: 4px;
		font-family: monospace;
	}

	.glass-btn {
		background: rgba(255, 255, 255, 0.08);
		border: 1px solid rgba(255, 255, 255, 0.12);
		color: #ffffff;
		border-radius: 12px;
		font-size: 15px;
		height: 50px;
		line-height: 50px;
		font-weight: 600;
		cursor: pointer;
		transition: all 0.3s;
		box-sizing: border-box;
	}

	.primary-glow {
		background: linear-gradient(135deg, #0084ff, #a450ff) !important;
		border: none !important;
		box-shadow: 0 4px 15px rgba(0, 132, 255, 0.4);
	}
	.primary-glow:active {
		transform: scale(0.98);
		opacity: 0.9;
	}

	.cyan-glow {
		background: linear-gradient(135deg, #06b6d4, #0891b2) !important;
		border: none !important;
	}

	.secondary-dark {
		background: rgba(255, 255, 255, 0.04) !important;
		border: 1px solid rgba(255,255,255,0.06) !important;
		color: #94a3b8 !important;
	}

	.connection-status-bar {
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 11px;
		color: #ef4444;
	}

	.connection-status-bar.active {
		color: #10b981;
	}

	.status-dot {
		display: inline-block;
		width: 6px;
		height: 6px;
		border-radius: 50%;
		background-color: #ef4444;
		margin-right: 6px;
	}

	.connection-status-bar.active .status-dot {
		background-color: #10b981;
		animation: glow 1.5s infinite;
	}

	/* ==========================================
	   🖥️ MAIN PLATFORM BOARD GRID
	   ========================================== */
	.app-glass-board {
		z-index: 10;
		width: 95vw;
		height: 90vh;
		max-width: 1200px;
		max-height: 800px;
		background: rgba(255, 255, 255, 0.02);
		border: 1px solid rgba(255, 255, 255, 0.05);
		border-radius: 28px;
		backdrop-filter: blur(30px);
		box-shadow: 0 30px 60px rgba(0, 0, 0, 0.5);
		display: flex;
		flex-direction: row;
		overflow: hidden;
	}

	/* ------------------------------------------
	   1. NARROW SIDE NAVIGATION BAR
	   ------------------------------------------ */
	.nav-sidebar {
		width: 75px;
		background: rgba(15, 23, 42, 0.4);
		border-right: 1px solid rgba(255, 255, 255, 0.04);
		display: flex;
		flex-direction: column;
		align-items: center;
		padding: 25px 0;
		justify-content: space-between;
	}

	.user-avatar-wrapper {
		position: relative;
		cursor: pointer;
	}

	.user-avatar-gradient {
		width: 44px;
		height: 44px;
		border-radius: 50%;
		background: linear-gradient(135deg, #0084ff, #a450ff);
		display: flex;
		align-items: center;
		justify-content: center;
		box-shadow: 0 0 10px rgba(164, 80, 255, 0.3);
	}

	.avatar-letter {
		color: #ffffff;
		font-weight: 700;
		font-size: 18px;
	}

	.online-indicator {
		position: absolute;
		bottom: 1px;
		right: 1px;
		width: 10px;
		height: 10px;
		background-color: #64748b;
		border: 2px solid #0f172a;
		border-radius: 50%;
	}

	.online-indicator.active {
		background-color: #10b981;
		animation: glow 1.5s infinite;
	}

	.nav-menu {
		display: flex;
		flex-direction: column;
		width: 100%;
		align-items: center;
	}

	.nav-item {
		position: relative;
		display: flex;
		flex-direction: column;
		align-items: center;
		width: 100%;
		padding: 16px 0;
		cursor: pointer;
		transition: all 0.3s;
	}

	.nav-icon {
		font-size: 22px;
		margin-bottom: 4px;
		opacity: 0.4;
		transition: opacity 0.3s;
	}

	.nav-label {
		font-size: 10px;
		color: #94a3b8;
		opacity: 0.5;
	}

	.nav-item.active .nav-icon {
		opacity: 1;
		text-shadow: 0 0 10px rgba(0, 132, 255, 0.6);
	}
	.nav-item.active .nav-label {
		color: #ffffff;
		opacity: 1;
	}
	.nav-item.active::before {
		content: '';
		position: absolute;
		left: 0;
		top: 15%;
		height: 70%;
		width: 3px;
		background: #0084ff;
		border-radius: 0 4px 4px 0;
		box-shadow: 0 0 8px #0084ff;
	}

	.unread-badge-mini {
		position: absolute;
		top: 10px;
		right: 16px;
		background: #ff3b30;
		color: #fff;
		font-size: 9px;
		font-weight: 700;
		padding: 1px 4px;
		border-radius: 8px;
	}

	.logout-btn {
		cursor: pointer;
		opacity: 0.4;
		transition: opacity 0.3s;
	}
	.logout-btn:hover {
		opacity: 1;
	}
	.logout-icon {
		font-size: 22px;
	}

	/* ------------------------------------------
	   2. MIDDLE FUNCTIONAL SIDEBAR
	   ------------------------------------------ */
	.middle-sidebar {
		width: 310px;
		background: rgba(15, 23, 42, 0.15);
		border-right: 1px solid rgba(255, 255, 255, 0.04);
		display: flex;
		flex-direction: column;
	}

	.sidebar-header {
		padding: 25px 20px 15px 20px;
		display: flex;
		justify-content: space-between;
		align-items: center;
	}

	.sidebar-title {
		font-size: 22px;
		font-weight: 800;
		color: #ffffff;
		letter-spacing: 0.5px;
	}

	.circle-action-btn {
		display: inline-block;
		width: 28px;
		height: 28px;
		line-height: 26px;
		text-align: center;
		border-radius: 50%;
		background: rgba(255, 255, 255, 0.04);
		border: 1px solid rgba(255, 255, 255, 0.05);
		color: #ffffff;
		font-size: 13px;
		margin-left: 8px;
		cursor: pointer;
		transition: background 0.3s;
	}
	.circle-action-btn:hover {
		background: rgba(255, 255, 255, 0.1);
	}

	.search-box {
		padding: 0 15px 15px 15px;
	}

	.glass-search {
		background: rgba(255, 255, 255, 0.03);
		border: 1px solid rgba(255, 255, 255, 0.05);
		border-radius: 12px;
		height: 38px;
		padding: 0 15px;
		color: #ffffff;
		font-size: 13px;
		width: 100%;
		box-sizing: border-box;
	}

	.list-scroll-view {
		flex: 1;
		overflow-y: auto;
	}

	.empty-list-tip {
		text-align: center;
		color: #64748b;
		font-size: 12px;
		margin-top: 40px;
	}

	/* Session Item Glass Card */
	.session-glass-card {
		display: flex;
		padding: 14px 15px;
		margin: 0 10px 8px 10px;
		background: transparent;
		border-radius: 16px;
		align-items: center;
		transition: all 0.25s;
		cursor: pointer;
		border: 1px solid transparent;
	}
	.session-glass-card:hover {
		background: rgba(255, 255, 255, 0.02);
	}
	.session-glass-card.active {
		background: rgba(255, 255, 255, 0.05);
		border: 1px solid rgba(255, 255, 255, 0.04);
	}

	.avatar-circle {
		width: 44px;
		height: 44px;
		background: rgba(255, 255, 255, 0.04);
		border: 1px solid rgba(255, 255, 255, 0.08);
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		font-weight: 700;
		color: #e2e8f0;
		margin-right: 12px;
		font-size: 16px;
		flex-shrink: 0;
		position: relative;
	}

	.session-info {
		flex: 1;
		overflow: hidden;
	}

	.session-header-row {
		display: flex;
		justify-content: space-between;
		margin-bottom: 4px;
	}

	.session-name {
		font-size: 14px;
		font-weight: 600;
		color: #f8fafc;
	}

	.session-time {
		font-size: 10px;
		color: #64748b;
	}

	.session-body-row {
		display: flex;
		justify-content: space-between;
		align-items: center;
	}

	.session-preview {
		font-size: 12px;
		color: #94a3b8;
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
		max-width: 160px;
	}

	.unread-bubble {
		background: #ef4444;
		color: #ffffff;
		font-size: 9px;
		font-weight: 700;
		padding: 2px 6px;
		border-radius: 10px;
		line-height: 1;
	}

	/* Friends / Contacts Styling */
	.contact-section-title {
		padding: 10px 20px;
		font-size: 11px;
		font-weight: 700;
		color: #64748b;
		text-transform: uppercase;
		letter-spacing: 1px;
	}

	.contact-glass-card {
		display: flex;
		padding: 12px 15px;
		margin: 0 10px 6px 10px;
		border-radius: 16px;
		align-items: center;
		cursor: pointer;
		transition: background 0.2s;
	}
	.contact-glass-card:hover {
		background: rgba(255, 255, 255, 0.03);
	}

	.contact-info {
		flex: 1;
	}
	.contact-name {
		display: block;
		font-size: 14px;
		font-weight: 600;
		color: #ffffff;
		margin-bottom: 2px;
	}
	.contact-status {
		font-size: 10px;
		color: #64748b;
	}
	.contact-status.online {
		color: #10b981;
	}
	.quick-chat-action {
		font-size: 14px;
		opacity: 0.3;
		transition: opacity 0.2s;
	}
	.contact-glass-card:hover .quick-chat-action {
		opacity: 0.8;
	}

	/* ------------------------------------------
	   🛠️ DEVELOPER TESTING TERMINAL
	   ------------------------------------------ */
	.console-glass-card {
		background: rgba(255, 255, 255, 0.02);
		border: 1px solid rgba(255, 255, 255, 0.04);
		border-radius: 18px;
		padding: 15px;
		margin-bottom: 15px;
		backdrop-filter: blur(10px);
	}

	.console-card-title {
		display: block;
		font-size: 13px;
		font-weight: 700;
		color: #ffffff;
		margin-bottom: 5px;
	}

	.console-card-desc {
		display: block;
		font-size: 11px;
		color: #64748b;
		line-height: 1.4;
		margin-bottom: 12px;
	}

	.mini-form {
		display: flex;
		flex-direction: column;
	}

	.mini-input {
		background: rgba(255, 255, 255, 0.03);
		border: 1px solid rgba(255, 255, 255, 0.06);
		border-radius: 8px;
		height: 32px;
		padding: 0 10px;
		color: #ffffff;
		font-size: 12px;
		margin-bottom: 8px;
		box-sizing: border-box;
	}

	.mini-textarea {
		background: rgba(255, 255, 255, 0.03);
		border: 1px solid rgba(255, 255, 255, 0.06);
		border-radius: 8px;
		height: 60px;
		padding: 8px 10px;
		color: #ffffff;
		font-size: 12px;
		margin-bottom: 8px;
		box-sizing: border-box;
	}

	.mini-btn {
		background: rgba(255, 255, 255, 0.05);
		border: 1px solid rgba(255, 255, 255, 0.08);
		color: #ffffff;
		font-size: 11px;
		font-weight: 600;
		height: 32px;
		line-height: 30px;
		border-radius: 8px;
		cursor: pointer;
	}

	.console-terminal {
		margin-top: 12px;
		background: rgba(0, 0, 0, 0.3);
		border-radius: 8px;
		padding: 10px;
		border: 1px solid rgba(255, 255, 255, 0.05);
		font-family: monospace;
		font-size: 11px;
		line-height: 1.4;
	}

	.terminal-text-cyan { color: #06b6d4; display: block; font-weight: bold; margin-bottom: 4px;}
	.terminal-text-white { color: #e2e8f0; display: block; word-break: break-all; margin-bottom: 8px; }
	.terminal-json { color: #10b981; margin: 0; white-space: pre-wrap; word-break: break-all; }

	.terminal-action-btn {
		background: #6366f1;
		color: #ffffff;
		border: none;
		border-radius: 4px;
		font-size: 10px;
		height: 24px;
		line-height: 24px;
		cursor: pointer;
	}

	.glass-label-group {
		margin-bottom: 10px;
	}
	.glass-mini-label {
		display: block;
		font-size: 10px;
		color: #64748b;
		margin-bottom: 4px;
	}
	.about-item {
		font-size: 11px;
		color: #94a3b8;
		margin-bottom: 6px;
	}

	/* ------------------------------------------
	   3. RIGHT CHAT WINDOW VIEWPORT
	   ------------------------------------------ */
	.chat-viewport {
		flex: 1;
		background: rgba(15, 23, 42, 0.05);
		display: flex;
		flex-direction: column;
		height: 100%;
	}

	.chat-frame {
		display: flex;
		flex-direction: column;
		height: 100%;
		position: relative;
	}

	.chat-header-bar {
		height: 70px;
		background: rgba(15, 23, 42, 0.2);
		border-bottom: 1px solid rgba(255, 255, 255, 0.04);
		display: flex;
		align-items: center;
		padding: 0 20px;
		justify-content: space-between;
		backdrop-filter: blur(10px);
	}

	.back-action-btn {
		display: none; /* Desktop hidden, mobile shown */
		align-items: center;
		cursor: pointer;
	}

	.header-user-info {
		display: flex;
		flex-direction: column;
	}
	.header-user-name {
		font-size: 15px;
		font-weight: 700;
		color: #ffffff;
	}
	.header-user-status {
		display: flex;
		align-items: center;
		font-size: 10px;
		color: #94a3b8;
		margin-top: 3px;
	}
	.header-user-status .status-dot {
		background-color: #10b981;
		width: 5px;
		height: 5px;
		margin-right: 4px;
	}

	.header-icon-btn {
		font-size: 18px;
		cursor: pointer;
		opacity: 0.6;
		transition: opacity 0.2s;
	}
	.header-icon-btn:hover {
		opacity: 1;
	}

	/* Messaging Scroller Area */
	.chat-messages-scroller {
		flex: 1;
		padding: 20px;
		box-sizing: border-box;
		overflow-y: auto;
		background: radial-gradient(circle at bottom right, rgba(99, 102, 241, 0.03), transparent);
	}

	.msg-bubble-wrapper {
		margin-bottom: 18px;
		width: 100%;
		display: flex;
		flex-direction: column;
		animation: pop 0.3s cubic-bezier(0.16, 1, 0.3, 1) forwards;
	}

	.msg-time-divider {
		text-align: center;
		font-size: 10px;
		color: #475569;
		margin: 12px 0;
	}

	.msg-row {
		display: flex;
		align-items: flex-start;
	}

	.msg-avatar-circle {
		width: 32px;
		height: 32px;
		background: rgba(255,255,255,0.04);
		border: 1px solid rgba(255,255,255,0.06);
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 12px;
		font-weight: 700;
		color: #cbd5e1;
		margin-right: 8px;
		flex-shrink: 0;
	}

	.msg-bubble-content {
		max-width: 65%;
		display: flex;
		flex-direction: column;
	}
	.sender-name-label {
		font-size: 10px;
		color: #64748b;
		margin-bottom: 3px;
		margin-left: 4px;
	}

	.bubble-text-card {
		display: inline-block;
		background: rgba(255, 255, 255, 0.05);
		border: 1px solid rgba(255, 255, 255, 0.05);
		padding: 10px 14px;
		border-radius: 6px 16px 16px 16px;
		font-size: 14px;
		line-height: 1.5;
		color: #e2e8f0;
		word-break: break-all;
	}

	.bubble-image-card {
		max-width: 200px;
		border-radius: 12px;
		border: 1px solid rgba(255, 255, 255, 0.08);
	}

	/* Self bubble alignment */
	.self-msg .msg-row {
		flex-direction: row-reverse;
	}
	.self-msg .msg-bubble-content {
		align-items: flex-end;
	}
	.self-msg .bubble-text-card {
		background: linear-gradient(135deg, #0084ff, #8a2be2);
		border: none;
		color: #ffffff;
		border-radius: 16px 6px 16px 16px;
		box-shadow: 0 4px 10px rgba(0, 132, 255, 0.2);
	}

	/* ==========================================
	   📢 SYSTEM PUSH MESSAGES STYLING (Facebook gold)
	   ========================================== */
	.system-push-msg {
		align-items: center;
		margin: 15px 0;
	}
	.system-push-card {
		width: 100%;
		max-width: 480px;
		background: linear-gradient(135deg, rgba(234, 179, 8, 0.12), rgba(234, 179, 8, 0.02));
		border: 1px solid rgba(234, 179, 8, 0.35);
		border-radius: 16px;
		padding: 14px 18px;
		backdrop-filter: blur(5px);
		box-sizing: border-box;
		box-shadow: 0 4px 15px rgba(234, 179, 8, 0.05);
	}

	.system-card-header {
		display: flex;
		align-items: center;
		margin-bottom: 8px;
		position: relative;
	}
	.system-card-icon {
		font-size: 18px;
		margin-right: 10px;
	}
	.system-card-meta {
		display: flex;
		flex-direction: column;
	}
	.system-card-title {
		font-size: 13px;
		font-weight: 700;
		color: #facc15;
	}
	.system-card-time {
		font-size: 9px;
		color: #854d0e;
		margin-top: 1px;
	}
	.system-badge {
		position: absolute;
		right: 0;
		top: 2px;
		font-size: 9px;
		background: rgba(234, 179, 8, 0.2);
		color: #facc15;
		padding: 2px 6px;
		border-radius: 6px;
		font-weight: bold;
	}

	.system-card-body {
		font-size: 13px;
		color: #e2e8f0;
		line-height: 1.5;
		word-break: break-all;
	}

	/* Bottom Input tray */
	.bottom-input-tray {
		height: 70px;
		background: rgba(15, 23, 42, 0.2);
		border-top: 1px solid rgba(255, 255, 255, 0.04);
		padding: 0 20px;
		display: flex;
		align-items: center;
		backdrop-filter: blur(10px);
	}

	.tray-action-btn {
		font-size: 20px;
		padding: 10px;
		cursor: pointer;
		opacity: 0.6;
		transition: opacity 0.2s;
	}
	.tray-action-btn:hover {
		opacity: 1;
	}

	.glass-message-input {
		flex: 1;
		background: rgba(255, 255, 255, 0.03);
		border: 1px solid rgba(255, 255, 255, 0.05);
		border-radius: 14px;
		height: 40px;
		padding: 0 15px;
		color: #ffffff;
		font-size: 14px;
		box-sizing: border-box;
		margin: 0 10px;
	}

	.tray-send-btn {
		background: #0084ff;
		color: #ffffff;
		font-size: 13px;
		font-weight: 700;
		height: 40px;
		line-height: 40px;
		padding: 0 18px;
		border-radius: 12px;
		border: none;
		cursor: pointer;
		box-shadow: 0 4px 10px rgba(0, 132, 255, 0.3);
	}

	/* Chat Empty Fallback */
	.chat-empty-fallback {
		flex: 1;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		padding: 40px;
		text-align: center;
	}

	.fallback-glow-badge {
		font-size: 60px;
		margin-bottom: 20px;
		text-shadow: 0 0 20px rgba(99, 102, 241, 0.3);
	}

	.fallback-title {
		font-size: 18px;
		font-weight: 700;
		color: #ffffff;
		margin-bottom: 8px;
	}

	.fallback-desc {
		font-size: 13px;
		color: #64748b;
		max-width: 320px;
		line-height: 1.5;
	}

	/* ------------------------------------------
	   4. RIGHT PROFILE INFO PANEL
	   ------------------------------------------ */
	.right-info-sidebar {
		width: 240px;
		background: rgba(15, 23, 42, 0.25);
		border-left: 1px solid rgba(255, 255, 255, 0.04);
		display: flex;
		flex-direction: column;
		animation: pop 0.3s ease;
	}

	.right-profile-header {
		display: flex;
		flex-direction: column;
		align-items: center;
		padding-top: 15px;
	}

	.big-profile-avatar {
		width: 68px;
		height: 68px;
		background: rgba(255, 255, 255, 0.03);
		border: 1.5px solid rgba(255, 255, 255, 0.08);
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 26px;
		font-weight: 800;
		color: #ffffff;
		margin-bottom: 12px;
	}

	.right-profile-name {
		font-size: 16px;
		font-weight: 700;
		color: #ffffff;
	}

	.right-profile-id {
		font-size: 11px;
		color: #64748b;
		margin-top: 4px;
	}

	.right-divider {
		height: 1px;
		background: rgba(255, 255, 255, 0.04);
		margin: 20px 0;
	}

	.right-section {
		margin-bottom: 22px;
	}

	.right-section-title {
		display: block;
		font-size: 11px;
		font-weight: 700;
		color: #475569;
		text-transform: uppercase;
		letter-spacing: 0.5px;
		margin-bottom: 10px;
	}

	.glass-menu-item {
		font-size: 13px;
		color: #cbd5e1;
		padding: 8px 12px;
		margin-bottom: 4px;
		background: rgba(255,255,255,0.01);
		border-radius: 8px;
		cursor: pointer;
	}
	.glass-menu-item:hover {
		background: rgba(255, 255, 255, 0.03);
	}

	.member-list-mini {
		display: flex;
		flex-direction: column;
	}

	.member-mini-card {
		display: flex;
		align-items: center;
		margin-bottom: 8px;
	}

	.member-mini-avatar {
		width: 24px;
		height: 24px;
		border-radius: 50%;
		background: #6366f1;
		color: #fff;
		font-size: 10px;
		text-align: center;
		line-height: 24px;
		margin-right: 8px;
	}

	.member-mini-name {
		font-size: 12px;
		color: #e2e8f0;
	}

	.shared-media-grid {
		display: grid;
		grid-template-columns: repeat(3, 1fr);
		gap: 6px;
	}

	.shared-img-card {
		width: 100%;
		aspect-ratio: 1;
		border-radius: 6px;
		overflow: hidden;
		border: 1px solid rgba(255, 255, 255, 0.05);
		cursor: pointer;
	}
	.shared-img-thumb {
		width: 100%;
		height: 100%;
	}
	.no-media-tip {
		grid-column: span 3;
		text-align: center;
		font-size: 11px;
		color: #475569;
		padding: 10px 0;
	}

	/* ==========================================
	   🪟 WINDOW OVERLAYS & MODALS
	   ========================================== */
	.modal-backdrop {
		position: fixed;
		top: 0;
		left: 0;
		right: 0;
		bottom: 0;
		background: rgba(15, 23, 42, 0.6);
		backdrop-filter: blur(10px);
		display: flex;
		align-items: center;
		justify-content: center;
		z-index: 1000;
	}

	.modal-glass-card {
		width: 80%;
		max-width: 360px;
		background: rgba(30, 27, 75, 0.4);
		border: 1px solid rgba(255, 255, 255, 0.08);
		border-radius: 20px;
		padding: 25px;
		box-shadow: 0 20px 40px rgba(0,0,0,0.5);
		backdrop-filter: blur(20px);
		display: flex;
		flex-direction: column;
		animation: pop 0.25s ease-out;
	}

	.modal-title {
		font-size: 16px;
		font-weight: 700;
		color: #ffffff;
		text-align: center;
		margin-bottom: 20px;
	}

	.modal-actions-row {
		display: flex;
		gap: 10px;
	}

	.modal-divider {
		text-align: center;
		color: #475569;
		font-size: 11px;
		margin-bottom: 15px;
	}

	/* ==========================================
	   📱 MOBILE RESPONSIVENESS (Responsive styles)
	   ========================================== */
	@media (max-width: 768px) {
		.app-glass-board {
			width: 100vw;
			height: 100vh;
			border-radius: 0;
			border: none;
		}

		.nav-sidebar {
			width: 60px;
			padding: 15px 0;
		}
		
		.nav-icon { font-size: 20px; }
		.nav-label { display: none; }
		
		.middle-sidebar {
			flex: 1;
			width: auto;
		}

		.middle-sidebar.mobile-hidden {
			display: none;
		}

		.chat-viewport {
			display: none;
		}

		.chat-viewport.mobile-active {
			display: flex;
			position: absolute;
			top: 0;
			left: 60px; /* offset sidebar */
			right: 0;
			bottom: 0;
			z-index: 50;
		}

		.back-action-btn {
			display: flex;
		}

		.header-unread-count {
			background: #ef4444;
			color: #ffffff;
			font-size: 8px;
			font-weight: bold;
			padding: 1px 4px;
			border-radius: 6px;
			margin-left: 4px;
		}

		.right-info-sidebar {
			display: none !important; /* hide right panel on mobile */
		}
	}
</style>
