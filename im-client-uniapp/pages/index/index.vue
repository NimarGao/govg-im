<template>
	<view class="container">
		<!-- Login Area -->
		<view v-if="!isLogin" class="login-box">
			<view class="title">IM 即时通讯演示</view>
			<view class="login-form">
                <view class="input-group">
                    <text class="label">用户ID:</text>
                    <input class="input" v-model="userIdInput" placeholder="请输入用户ID (英文/数字)" />
                </view>
                <view class="input-group">
                    <text class="label">用户名:</text>
                    <input class="input" v-model="username" placeholder="请输入昵称" />
                </view>
                <button class="btn" @click="login">登录</button>
            </view>
            <view class="status-text" :class="{ connected: isConnected }">
                {{ isConnected ? '服务器已连接' : '正在连接服务器...' }}
            </view>
		</view>

        <!-- Main App Area -->
		<view v-else class="main-box">
            
            <!-- Session List (Contact List) -->
            <view v-if="view === 'list'" class="session-view">
                <view class="header">
                    <view class="user-profile" @click="copyId">
                        <text class="user-name">{{ username }}</text>
                        <text class="user-id">ID: {{ userId }} (点击复制)</text>
                    </view>
                    <view class="header-actions">
                        <text class="action-btn" @click="showCreateGroup = true">+</text>
                    </view>
                </view>

                <scroll-view scroll-y="true" class="session-list">
                    <view v-if="sessions.length === 0" class="empty-tip">暂无会话</view>
                    <view v-for="(session, index) in sessions" :key="index" 
                          class="session-item" @click="openChat(session)">
                        <view class="avatar">{{ session.name[0] }}</view>
                        <view class="session-info">
                            <view class="session-top">
                                <text class="session-name">{{ session.name }}</text>
                                <text class="session-time">{{ session.time }}</text>
                            </view>
                            <view class="session-bottom">
                                <text class="session-msg">{{ session.lastMsg }}</text>
                                <view v-if="session.unread > 0" class="unread-badge">{{ session.unread }}</view>
                            </view>
                        </view>
                    </view>
                </scroll-view>
                
                <!-- Create/Join Group Modal (Simple overlay) -->
                <view v-if="showCreateGroup" class="modal-mask">
                    <view class="modal-content">
                        <view class="modal-title">群组操作</view>
                        <input class="input-mini" v-model="targetGroupId" placeholder="输入群组 ID 加入" />
                        <button class="btn-mini" @click="joinGroup">加入群组</button>
                        <view class="divider">或者</view>
                        <button class="btn-mini primary" @click="createGroup">创建新群组</button>
                         <button class="btn-mini close" @click="showCreateGroup = false">取消</button>
                    </view>
                </view>
                
                 <!-- Start Chat Modal -->
                <view class="float-btn" @click="showStartChat = true">
                    <text class="float-icon">私聊</text>
                </view>
                
                 <view v-if="showStartChat" class="modal-mask">
                    <view class="modal-content">
                        <view class="modal-title">发起聊天</view>
                        <input class="input-mini" v-model="newChatId" placeholder="输入用户 ID" />
                        <button class="btn-mini primary" @click="startNewChat">开始聊天</button>
                        <button class="btn-mini close" @click="showStartChat = false">取消</button>
                    </view>
                </view>

            </view>

            <!-- Chat Detail View -->
            <view v-else-if="view === 'chat'" class="chat-view">
                <view class="chat-header">
                    <view class="back-btn-wrapper" @click="view = 'list'">
                        <text class="back-arrow">❮</text>
                        <text class="back-text">返回</text>
                        <view v-if="totalUnread > 0" class="header-unread">{{ totalUnread > 99 ? '99+' : totalUnread }}</view>
                    </view>
                    <text class="chat-title">{{ currentSession.name }}</text>
                    <text class="chat-more">...</text>
                </view>

                <scroll-view scroll-y="true" class="msg-list" :scrollTop="scrollTop" :scroll-into-view="scrollIntoView" @scroll="onScroll">
                    <view v-for="(msg, index) in currentMsgs" :key="index" class="msg-item" :class="{ self: msg.isSelf }" :id="'msg-'+index">
                        <view class="msg-time-tip" v-if="shouldShowTime(index)">{{ msg.time }}</view>
                        <view class="msg-bubble-box">
                            <view class="msg-avatar" v-if="!msg.isSelf">{{ msg.sender[0] }}</view>
                            <view class="msg-content">
                                <view class="sender-name" v-if="!msg.isSelf && currentSession.type === 'group'">{{ msg.sender }}</view>
                                <image v-if="msg.msgType === 2" :src="formatImageUrl(msg.content)" class="msg-image" mode="widthFix" @click="previewImage(msg.content)"></image>
                                <text v-else class="msg-text">{{ msg.content }}</text>
                            </view>
                        </view>
                    </view>
                     <view id="scroll-bottom"></view>
                </scroll-view>

                <view class="input-area">
                    <view class="media-btn" @click="chooseImage">📷</view>
                    <input class="input-msg" v-model="msgContent" placeholder="输入消息..." confirm-type="send" @confirm="sendMsg" />
                    <button class="btn-send" @click="sendMsg">发送</button>
                </view>

                <!-- New Message Tip -->
                <view v-if="newMsgCount > 0" class="new-msg-tip" @click="scrollToBottom">
                    <text>⬇️ {{ newMsgCount }}条新消息</text>
                </view>
            </view>
            
		</view>
        
        <!-- Debug Toggle -->
        <view class="debug-toggle" @click="showDebug = !showDebug">调试</view>
        <view v-if="showDebug" class="debug-box">
			<scroll-view scroll-y="true" class="log-list">
				<view v-for="(log, index) in logs" :key="index" class="log-item">{{ log }}</view>
			</scroll-view>
		</view>

	</view>
</template>

<script>
	export default {
		data() {
			return {
                // Connection
				socketTask: null,
				isConnected: false,
                
                // Auth
				isLogin: false,
				username: '',
                userIdInput: '', // Input for login
				userId: '', // Actual logged in ID
                
                // UI State
                view: 'list', // 'list' or 'chat'
                showCreateGroup: false,
                showStartChat: false,
                showDebug: false,
                scrollTop: 0,
                scrollIntoView: '',
                isAtBottom: true, // Track if user is at bottom
                newMsgCount: 0,   // Unread count in current chat
                
                // Data
                sessions: [], // { id, name, type, lastMsg, time, unread }
                messages: {}, // { sessionId: [ { sender, content, time, isSelf } ] }
                currentSession: null,
                
                // Inputs
				msgContent: '',
                targetGroupId: '',
                newChatId: '',
                
                // Logs
                logs: []
			}
		},
        computed: {
            currentMsgs() {
                if (!this.currentSession) return [];
                return this.messages[this.currentSession.id] || [];
            },
            totalUnread() {
                // Sum unread of ALL sessions (excluding current one if we want, but usually back button shows list unread)
                // Actually back button should show TOTAL unread count of the app (excluding current chat because we are reading it)
                return this.sessions.reduce((sum, s) => {
                    // If s is current session, unread should be 0 (handled in openChat), but just in case
                    return sum + (s.unread || 0);
                }, 0);
            }
        },
		onLoad() {
            this.addLog('App Loaded');
            // Load from cache
            const cachedUser = uni.getStorageSync('im_user_info');
            if (cachedUser) {
                this.userIdInput = cachedUser.userId;
                this.username = cachedUser.username;
            } else {
                // Default random
                this.userIdInput = 'user' + Math.floor(Math.random() * 1000);
                this.username = this.userIdInput;
            }
            
            // Load messages & sessions
            try {
                const cachedSessions = uni.getStorageSync('im_sessions');
                const cachedMessages = uni.getStorageSync('im_messages');
                if (cachedSessions) this.sessions = cachedSessions;
                if (cachedMessages) this.messages = cachedMessages;
            } catch (e) {
                console.error('Failed to load cache', e);
            }
            
			this.connectSocket();
		},
        onUnload() {
            if (this.socketTask) {
                this.socketTask.close();
            }
        },
		methods: {
            // --- Utils ---
            onScroll(e) {
                // Check if user is near bottom (allow 50px buffer)
                // Note: scrollHeight is total content height, scrollTop is scrolled distance, height is view height
                // But e.detail doesn't give view height directly easily in all platforms.
                // We can assume if scrollTop increases, user is going down.
                
                // Simplified: If we are not scrolling, we assume we are at bottom ONLY if we just called scrollToBottom
                // But here we need real detection.
                
                // #ifdef H5
                // On H5 we can use DOM element if needed, but let's try e.detail
                // const isBottom = e.detail.scrollHeight - e.detail.scrollTop - e.detail.height < 50;
                // Actually e.detail.height might not be available or correct in scroll-view event
                // Let's use a flag. If new message comes and we didn't scroll, we auto scroll.
                // If user scrolled up, we stop auto scroll.
                // #endif
                
                // Robust way:
                const query = uni.createSelectorQuery().in(this);
                query.select('.msg-list').boundingClientRect(data => {
                    if(!data) return;
                    const viewHeight = data.height;
                    const { scrollHeight, scrollTop } = e.detail;
                    
                    // 调试日志
                    // this.addLog(`Scroll: H=${scrollHeight}, T=${scrollTop}, V=${viewHeight}`);
                    
                    // 只要距离底部超过 50px，就认为不在底部
                    if (scrollHeight - scrollTop - viewHeight < 50) {
                        this.isAtBottom = true;
                        this.newMsgCount = 0; 
                    } else {
                        this.isAtBottom = false;
                    }
                }).exec();
            },

            formatImageUrl(url) {
                // #ifdef H5
                if (location.protocol === 'https:' && url.startsWith('http:')) {
                    const devApi = process.env.VUE_APP_API_URL_DEV || 'http://127.0.0.1:10085';
                    const prodApi = process.env.VUE_APP_API_URL || 'https://im.your-domain.com';
                    
                    // 去掉 http:// 前缀进行比较，防止混淆
                    const devHost = devApi.replace(/^https?:\/\//, '');
                    
                    if (url.includes(devHost)) {
                        return url.replace(devApi, prodApi);
                    }
                }
                // #endif
                return url;
            },

            saveData() {
                uni.setStorage({ key: 'im_sessions', data: this.sessions });
                uni.setStorage({ key: 'im_messages', data: this.messages });
            },
            
            addLog(msg) {
				const time = new Date().toLocaleTimeString();
				this.logs.unshift(`[${time}] ${msg}`);
				if (this.logs.length > 20) this.logs.pop();
			},
            formatTime(date) {
                return date.getHours() + ':' + (date.getMinutes()<10?'0':'') + date.getMinutes();
            },
            shouldShowTime(index) {
                if (index === 0) return true;
                // Simple logic: show time every 5 messages
                return index % 5 === 0; 
            },
            copyId() {
                if (!this.userId) return;
                uni.setClipboardData({
                    data: this.userId,
                    success: () => uni.showToast({ title: 'ID 已复制', icon: 'none' })
                });
            },

            // --- Socket ---
			connectSocket() {
                this.addLog('正在连接...');
                
                // 自动判断协议和域名
                let url;
                // #ifdef H5
                if (location.protocol === 'https:') {
                     // 线上 HTTPS
                     url = process.env.VUE_APP_WS_URL || 'wss://im.your-domain.com/ws';
                } else {
                     // 本地开发 OR 线上 HTTP
                     url = process.env.VUE_APP_WS_URL_DEV || 'ws://127.0.0.1:10087/ws';
                }
                // #endif

                // #ifndef H5
                url = process.env.VUE_APP_WS_URL_DEV || 'ws://127.0.0.1:10087/ws';
                // #endif

				this.socketTask = uni.connectSocket({
					url: url,
					success: () => this.addLog('Socket 初始化成功: ' + url),
                    fail: (err) => this.addLog('Socket 初始化失败: ' + JSON.stringify(err))
				});

				this.socketTask.onOpen((res) => {
					this.addLog('已连接');
					this.isConnected = true;
                    if(this.userId) {
                         // Auto re-login if we have userId (simplified)
                         this.login();
                    }
				});

				this.socketTask.onMessage((res) => {
					this.handleMessage(res.data);
				});

				this.socketTask.onClose(() => {
					this.addLog('已断开');
					this.isConnected = false;
                    this.isLogin = false;
                    setTimeout(() => this.connectSocket(), 3000);
				});
			},
            
			handleMessage(data) {
                // this.addLog('Recv: ' + data);
				const packet = JSON.parse(data);

                // Fix for missing command in old server versions
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
						this.addLog('登录成功: ' + this.userId);
					} else {
						uni.showToast({ title: packet.reason, icon: 'none' });
					}
				}
				else if (packet.command === 4) { // MESSAGE_RESPONSE
                    const sessionId = packet.fromUserId;
                    const sessionName = packet.fromUserName || packet.fromUserId;
                    this.onReceiveMessage(sessionId, sessionName, 'user', packet.message, packet.fromUserId, packet.msgType);
                    this.sendAck(packet.msgId);
				}
                else if (packet.command === 12) { // GROUP_MESSAGE_RESPONSE
                    const sessionId = 'g:' + packet.fromGroupId;
                    const senderName = packet.fromUser; // Usually we need to fetch user info
                    this.onReceiveMessage(sessionId, `群组 ${packet.fromGroupId}`, 'group', packet.message, senderName, packet.msgType);
                }
                else if (packet.command === 8) { // CREATE_GROUP_RESPONSE
                    if(packet.success) {
                         uni.showToast({ title: '群组已创建: ' + packet.groupId });
                         this.startSession('g:' + packet.groupId, '群组 ' + packet.groupId, 'group');
                         this.showCreateGroup = false;
                    }
                }
                else if (packet.command === 10) { // JOIN_GROUP_RESPONSE
                     if(packet.success) {
                         uni.showToast({ title: '已加入群组 ' + packet.groupId });
                         this.startSession('g:' + packet.groupId, '群组 ' + packet.groupId, 'group');
                         this.showCreateGroup = false;
                     }
                }
			},

            // --- Core Logic ---
            onReceiveMessage(sessionId, sessionName, type, content, senderName, msgType = 1) {
                // 1. Add to messages
                if (!this.messages[sessionId]) {
                    this.$set(this.messages, sessionId, []);
                }
                this.messages[sessionId].push({
                    sender: senderName,
                    content: content,
                    msgType: msgType,
                    time: this.formatTime(new Date()),
                    isSelf: false
                });
                
                // 2. Update or Create Session
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
                
                // 3. Update Session Info
                session.lastMsg = msgType === 2 ? '[图片]' : content;
                session.time = this.formatTime(new Date());
                
                // 4. Unread Count
                if (!this.currentSession || this.currentSession.id !== sessionId) {
                    session.unread++;
                } else {
                    // Current chat: check if we should scroll
                    // If user is near bottom OR message is from self (which shouldn't happen here usually, but just in case)
                    // Fix: msg is not defined, use false as default since this is onReceiveMessage
                    if (this.isAtBottom) {
                        setTimeout(() => {
                            this.scrollToBottom();
                        }, 50);
                    } else {
                        // User is scrolling history -> Show tip
                        this.newMsgCount++;
                        // this.addLog(`New msg, count: ${this.newMsgCount}`);
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
                if(!this.newChatId) return;
                this.startSession(this.newChatId, this.newChatId, 'user');
                this.showStartChat = false;
                this.newChatId = '';
            },

            // --- Actions ---
			login() {
				if (!this.isConnected) {
                    uni.showToast({ title: '未连接服务器', icon: 'none' });
                    this.connectSocket();
                    return;
                }
                if (!this.userIdInput) {
                    uni.showToast({ title: '请输入用户ID', icon: 'none' });
                    return;
                }
				this.send({
					command: 1,
                    userId: this.userIdInput,
					username: this.username || this.userIdInput,
					password: 'pwd'
				});
                
                // Save to cache
                uni.setStorageSync('im_user_info', {
                    userId: this.userIdInput,
                    username: this.username || this.userIdInput
                });
			},
            
			sendMsg() {
				if (!this.msgContent) return;
                this.doSend(this.msgContent, 1); // 1: Text
				this.msgContent = '';
			},
            
            chooseImage() {
                uni.chooseImage({
                    count: 1,
                    success: (res) => {
                        const tempFilePaths = res.tempFilePaths;
                        this.addLog('Uploading image...');
                        
                        const devApi = process.env.VUE_APP_API_URL_DEV || 'http://127.0.0.1:10085';
                        const prodApi = process.env.VUE_APP_API_URL || 'https://im.your-domain.com';

                        // Determine API URL
                        let uploadUrl;
                        
                        // #ifdef H5
                        if (location.protocol === 'https:') {
                             uploadUrl = prodApi + '/api/upload'; 
                        } else {
                             uploadUrl = devApi + '/api/upload';
                        }
                        // #endif

                        // #ifndef H5
                        uploadUrl = prodApi + '/api/upload';
                        // #endif

                        uni.uploadFile({
                            url: uploadUrl, 
                            filePath: tempFilePaths[0],
                            name: 'file',
                            success: (uploadRes) => {
                                try {
                                    const data = JSON.parse(uploadRes.data);
                                    if (data.success) {
                                        // Fix URL if it's relative
                                    let fileUrl = data.url;
                                    if (fileUrl.startsWith('/')) {
                                        // #ifdef H5
                                        if (location.protocol === 'https:') {
                                            fileUrl = prodApi + fileUrl;
                                        } else {
                                            fileUrl = devApi + fileUrl;
                                        }
                                        // #endif
                                        
                                        // #ifndef H5
                                        fileUrl = devApi + fileUrl;
                                        // #endif
                                    }
                                        
                                        this.doSend(fileUrl, 2); // 2: Image
                                    } else {
                                        uni.showToast({ title: '上传失败: ' + (data.message || 'Unknown'), icon: 'none' });
                                    }
                                } catch (e) {
                                    this.addLog('Parse Error: ' + uploadRes.data);
                                    uni.showToast({ title: '上传响应解析失败', icon: 'none' });
                                }
                            },
                            fail: (err) => {
                                this.addLog('Upload Error: ' + JSON.stringify(err));
                                uni.showToast({ title: 'Upload Error', icon: 'none' });
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
                
                // Add to local
                if (!this.messages[session.id]) this.$set(this.messages, session.id, []);
				this.messages[session.id].push({
					sender: '我',
					content: content,
                    msgType: msgType,
                    time: this.formatTime(new Date()),
					isSelf: true
				});
                
                // Update Session
                session.lastMsg = msgType === 2 ? '[图片]' : content;
                session.time = this.formatTime(new Date());
                this.saveData();
                
                // Ensure scroll to bottom for self message
                setTimeout(() => {
                    this.scrollToBottom();
                }, 50);
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
                if(!this.targetGroupId) return;
                this.send({ command: 9, groupId: this.targetGroupId });
            },
            
            sendAck(msgId) {
                this.send({ command: 13, msgId: msgId, fromUserId: this.userId });
            },
            
			send(packet) {
				this.socketTask.send({ data: JSON.stringify(packet) });
			},
            
            scrollToBottom() {
                this.newMsgCount = 0; // Clear count
                this.isAtBottom = true; // Assume we are going to bottom
                this.$nextTick(() => {
                    // Reset first to trigger watcher if needed
                    this.scrollIntoView = '';
                    this.$nextTick(() => {
                        this.scrollIntoView = 'scroll-bottom';
                        this.scrollTop = 9999999;
                    });
                });
            }
		}
	}
</script>

<style>
    /* Hide Scrollbar */
    ::-webkit-scrollbar {
        width: 0;
        height: 0;
        color: transparent;
        display: none;
    }
    
    /* Reset & Base */
    page { background-color: #f5f5f5; height: 100%; }
	.container { height: 100vh; display: flex; flex-direction: column; }
    
    /* Login */
	.login-box { margin-top: 30vh; padding: 40px; }
	.title { font-size: 28px; font-weight: bold; text-align: center; margin-bottom: 40px; color: #333; }
    .input-group { margin-bottom: 20px; }
    .label { display: block; margin-bottom: 8px; color: #666; font-size: 14px; }
	.input { background: #fff; padding: 15px; border-radius: 8px; font-size: 16px; }
	.btn { background-color: #007aff; color: #fff; border-radius: 25px; font-size: 16px; margin-top: 30px; }
    .status-text { text-align: center; margin-top: 20px; color: #999; font-size: 14px; }
    .status-text.connected { color: #4cd964; }

    /* Main Area */
    .main-box { flex: 1; display: flex; flex-direction: column; height: 100%; }
    
    /* Session List View */
    .session-view { flex: 1; display: flex; flex-direction: column; }
    .header { background: #fff; padding: 15px 20px; padding-top: 50px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #eee; }
    .user-profile { display: flex; flex-direction: column; }
    .user-name { font-size: 20px; font-weight: bold; }
    .user-id { font-size: 12px; color: #999; margin-top: 4px; }
    .action-btn { font-size: 28px; color: #007aff; line-height: 1; padding: 0 10px; }
    
    .session-list { flex: 1; background: #fff; }
    .session-item { display: flex; padding: 15px; border-bottom: 1px solid #f5f5f5; align-items: center; }
    .session-item:active { background-color: #fafafa; }
    .avatar { width: 44px; height: 44px; background: #eee; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-weight: bold; color: #666; margin-right: 12px; font-size: 18px; }
    .session-info { flex: 1; }
    .session-top { display: flex; justify-content: space-between; margin-bottom: 4px; }
    .session-name { font-size: 16px; font-weight: 500; color: #333; }
    .session-time { font-size: 12px; color: #999; }
    .session-bottom { display: flex; justify-content: space-between; align-items: center; }
    .session-msg { font-size: 14px; color: #999; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 200px; }
    .unread-badge { background: #ff3b30; color: #fff; font-size: 10px; padding: 2px 6px; border-radius: 10px; }
    .empty-tip { text-align: center; color: #ccc; margin-top: 50px; }

    /* Chat Detail View */
    .chat-view { flex: 1; display: flex; flex-direction: column; background: #f2f2f2; position: relative; height: 100vh; }
    
    /* 吸顶 Header */
    .chat-header { 
        position: fixed; top: 0; left: 0; right: 0; z-index: 1000; /* 提高层级 */
        background: #fff; 
        height: 44px; 
        /* 兼容处理：如果没有 status-bar-height，默认给 20px */
        padding-top: 20px; 
        padding-top: var(--status-bar-height); 
        display: flex; align-items: center; padding-left: 10px; padding-right: 15px; 
        border-bottom: 1px solid #eee; 
        box-shadow: 0 1px 2px rgba(0,0,0,0.05); /* 加点阴影更容易看清 */
    }
    .back-btn-wrapper { display: flex; align-items: center; width: 80px; position: relative; }
    .back-arrow { font-size: 18px; color: #333; margin-right: 4px; }
    .back-text { font-size: 16px; color: #333; }
    .header-unread { 
        position: absolute; top: -5px; right: 10px;
        background: #ff3b30; color: #fff; font-size: 10px; 
        padding: 0 6px; height: 16px; line-height: 16px; border-radius: 10px; 
    }
    
    .chat-title { flex: 1; text-align: center; font-size: 17px; font-weight: 600; }
    .chat-more { width: 60px; text-align: right; color: #333; }
    
    /* 消息列表：避让 Header 和 Footer */
    .msg-list { 
        position: absolute; 
        top: calc(45px + var(--status-bar-height)); 
        bottom: calc(60px + constant(safe-area-inset-bottom)); /* 估算高度 */
        bottom: calc(60px + env(safe-area-inset-bottom));
        left: 0; right: 0;
        padding: 15px; box-sizing: border-box; overflow-y: scroll; 
    }
    
    .msg-item { margin-bottom: 20px; width: 100%; }
    .msg-time-tip { text-align: center; color: #ccc; font-size: 12px; margin-bottom: 10px; }
    .msg-bubble-box { display: flex; align-items: flex-start; }
    .msg-avatar { width: 40px; height: 40px; background: #ddd; border-radius: 4px; display: flex; align-items: center; justify-content: center; font-size: 16px; margin-right: 10px; flex-shrink: 0; }
    .msg-content { max-width: 70%; display: flex; flex-direction: column; }
    .sender-name { font-size: 12px; color: #999; margin-bottom: 4px; margin-left: 2px; }
    .msg-text { display: inline-block; background: #fff; padding: 10px 12px; border-radius: 8px; font-size: 16px; line-height: 1.5; color: #333; word-break: break-all; position: relative; }
    .msg-text::before { content: ''; position: absolute; left: -6px; top: 14px; width: 0; height: 0; border-right: 6px solid #fff; border-top: 6px solid transparent; border-bottom: 6px solid transparent; }
    
    .msg-item.self .msg-bubble-box { flex-direction: row-reverse; }
    .msg-item.self .msg-avatar { margin-right: 0; margin-left: 10px; display: block; }
    .msg-item.self .msg-content { align-items: flex-end; }
    .msg-item.self .msg-text { background: #95ec69; }
    .msg-item.self .msg-text::before { left: auto; right: -6px; border-right: none; border-left: 6px solid #95ec69; }

    /* 吸底 Input Area */
    .input-area { 
        position: fixed; bottom: 0; left: 0; right: 0; z-index: 10;
        background: #f7f7f7; padding: 10px; display: flex; align-items: center; border-top: 1px solid #e5e5e5; 
        padding-bottom: calc(10px + constant(safe-area-inset-bottom)); 
        padding-bottom: calc(10px + env(safe-area-inset-bottom)); 
    }
    .input-msg { flex: 1; background: #fff; height: 40px; border-radius: 4px; padding: 0 10px; font-size: 16px; }
    .btn-send { margin-left: 10px; background: #07c160; color: #fff; font-size: 14px; padding: 0 16px; height: 40px; line-height: 40px; border-radius: 4px; border: none; }
    
    .media-btn { font-size: 24px; padding: 0 10px; color: #666; line-height: 40px; }
    .msg-image { max-width: 150px; border-radius: 8px; }

    /* New Message Tip */
    .new-msg-tip {
        position: fixed;
        bottom: 120px; /* 暂时固定高度，避免 calc 兼容性问题 */
        right: 15px;
        background: #fff;
        color: #007aff;
        padding: 8px 16px;
        border-radius: 20px;
        font-size: 13px;
        box-shadow: 0 4px 16px rgba(0,0,0,0.2); /* 加深阴影 */
        z-index: 2000; /* 确保最高层级 */
        display: flex;
        align-items: center;
        border: 1px solid #eee;
        animation: slideUp 0.3s;
    }
    @keyframes slideUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }

    /* Modals & Floats */
    .modal-mask { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 100; }
    .modal-content { background: #fff; width: 70%; padding: 20px; border-radius: 12px; }
    .modal-title { text-align: center; font-weight: bold; font-size: 18px; margin-bottom: 20px; }
    .input-mini { background: #f5f5f5; padding: 10px; border-radius: 6px; margin-bottom: 15px; }
    .btn-mini { margin-bottom: 10px; font-size: 14px; }
    .btn-mini.primary { background: #007aff; color: #fff; }
    .btn-mini.close { background: #eee; color: #666; }
    .divider { text-align: center; color: #ccc; margin: 10px 0; font-size: 12px; }
    
    .float-btn { position: fixed; bottom: 100px; right: 20px; background: #007aff; width: 50px; height: 50px; border-radius: 50%; display: flex; align-items: center; justify-content: center; box-shadow: 0 4px 10px rgba(0,122,255,0.3); }
    .float-icon { color: #fff; font-size: 12px; font-weight: bold; }
    
    /* Debug: 输入框上方 20px */
    .debug-toggle { 
        position: fixed; 
        bottom: calc(80px + constant(safe-area-inset-bottom)); 
        bottom: calc(80px + env(safe-area-inset-bottom));
        left: 20px; 
        font-size: 12px; color: rgba(0,0,0,0.3); z-index: 200; padding: 5px; background: rgba(255,255,255,0.5); border-radius: 4px;
    }
    .debug-box { 
        position: fixed; 
        bottom: calc(110px + constant(safe-area-inset-bottom));
        bottom: calc(110px + env(safe-area-inset-bottom));
        left: 20px; 
        width: 200px; height: 150px; background: rgba(0,0,0,0.8); color: #0f0; padding: 10px; font-size: 10px; border-radius: 8px; z-index: 199; pointer-events: none; 
    }
</style>
