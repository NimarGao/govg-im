<template>
  <div v-if="visible" class="meeting-overlay-backdrop animate-fade-in">
    <!-- Blurry Ambient Background -->
    <div class="ambient-blur-bg"></div>

    <div class="meeting-glass-container animate-scale-up">
      <!-- Meeting Header -->
      <div class="meeting-header">
        <div class="meeting-title">
          <span class="meeting-icon">🎥</span>
          <span class="title-text">多人视频会议舱</span>
        </div>
        <div class="meeting-subtitle">群聊: {{ groupName }} ({{ activeMembers.length }}人在线)</div>
      </div>

      <!-- Center Body - Multi-User Grid -->
      <div class="meeting-body" :class="gridClass">
        <div 
          v-for="member in activeMembers" 
          :key="member.userId" 
          class="member-card"
          :class="{ 'speaking-glow': !member.mute, 'self-card': member.userId === currentUserId }"
        >
          <!-- Simulated Camera Feed / Audio Wave Canvas -->
          <div class="stream-panel">
            <!-- Label tag -->
            <span class="stream-tag">
              {{ member.userId === currentUserId ? '我' : member.username || member.userId }}
              {{ member.mute ? ' (已静音)' : '' }}
            </span>

            <!-- Member Avatar / Graphic -->
            <div class="avatar-container">
              <div class="member-avatar">
                {{ getAvatarChar(member) }}
              </div>
              <div v-if="!member.mute" class="avatar-ripple"></div>
            </div>

            <!-- Individual Voice Wave Canvas -->
            <canvas 
              :ref="el => setCanvasRef(el, member.userId)" 
              class="member-wave-canvas"
            ></canvas>
          </div>
          
          <div class="member-info">
            <span class="mic-status-icon">{{ member.mute ? '🎙️❌' : '🎙️' }}</span>
            <span class="member-name">{{ member.username || member.userId }}</span>
          </div>
        </div>
      </div>

      <!-- Action Controls -->
      <div class="meeting-actions animate-slide-up">
        <div class="action-btn-wrapper mute" @click="toggleMute">
          <button class="round-action-btn bg-translucent" :class="{ active: isMuted }">
            <span class="action-icon">{{ isMuted ? '🎙️❌' : '🎙️' }}</span>
          </button>
          <span class="btn-label">{{ isMuted ? '解除静音' : '静音' }}</span>
        </div>

        <div class="action-btn-wrapper hangup" @click="leaveMeeting">
          <button class="round-action-btn bg-red">
            <span class="action-icon">📞</span>
          </button>
          <span class="btn-label">离开会议</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'TUIMeetingOverlay',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    groupId: {
      type: String,
      required: true
    },
    groupName: {
      type: String,
      default: '群组会议'
    },
    currentUserId: {
      type: String,
      required: true
    },
    membersJson: {
      type: String,
      default: '[]'
    }
  },
  emits: ['toggle-mute', 'leave-meeting'],
  data() {
    return {
      isMuted: false,
      canvasRefs: {},
      animationTimers: {},
      angles: {}
    };
  },
  computed: {
    activeMembers() {
      try {
        const list = JSON.parse(this.membersJson) || [];
        // Map usernames if we have local cache or just fallback to ID
        return list.map(m => {
          return {
            userId: m.userId,
            username: m.userId === this.currentUserId ? '我' : (m.username || m.userId),
            mute: m.mute
          };
        });
      } catch (e) {
        console.error('Failed to parse meeting members json:', e);
        return [];
      }
    },
    gridClass() {
      const len = this.activeMembers.length;
      if (len <= 1) return 'grid-1';
      if (len === 2) return 'grid-2';
      return 'grid-4'; // 3 or 4 members
    }
  },
  watch: {
    membersJson: {
      handler() {
        this.$nextTick(() => {
          this.initAllWaveCanvases();
        });
      },
      immediate: true
    },
    visible(newVal) {
      if (newVal) {
        this.isMuted = false;
        this.$nextTick(() => {
          this.initAllWaveCanvases();
        });
      } else {
        this.clearAllWaveAnimations();
      }
    }
  },
  beforeUnmount() {
    this.clearAllWaveAnimations();
  },
  methods: {
    getAvatarChar(member) {
      const name = member.username || member.userId;
      return name ? name[0].toUpperCase() : 'U';
    },
    setCanvasRef(el, userId) {
      if (el) {
        this.canvasRefs[userId] = el;
      }
    },
    toggleMute() {
      this.isMuted = !this.isMuted;
      this.$emit('toggle-mute', this.isMuted);
    },
    leaveMeeting() {
      this.clearAllWaveAnimations();
      this.$emit('leave-meeting');
    },
    initAllWaveCanvases() {
      this.clearAllWaveAnimations();
      this.activeMembers.forEach(member => {
        const canvas = this.canvasRefs[member.userId];
        if (!canvas) return;
        
        const ctx = canvas.getContext('2d');
        if (!ctx) return;

        canvas.width = canvas.parentElement.clientWidth || 180;
        canvas.height = 60;

        this.angles[member.userId] = 0;

        const draw = () => {
          if (!canvas || !this.visible) return;
          ctx.clearRect(0, 0, canvas.width, canvas.height);

          ctx.beginPath();
          ctx.lineWidth = 2.5;
          
          if (member.mute) {
            // Flat line for muted members
            ctx.strokeStyle = 'rgba(239, 68, 68, 0.4)'; // Red flat line
            ctx.moveTo(0, canvas.height / 2);
            ctx.lineTo(canvas.width, canvas.height / 2);
            ctx.stroke();
          } else {
            // Sine wave for speaking/active members
            ctx.strokeStyle = member.userId === this.currentUserId 
              ? 'rgba(0, 132, 255, 0.85)' // Messenger Blue
              : 'rgba(168, 85, 247, 0.85)'; // Purple for others
            
            const amplitude = 12 + Math.random() * 6; // slightly dynamic
            const frequency = 0.04;
            const angle = this.angles[member.userId] || 0;

            for (let x = 0; x < canvas.width; x++) {
              const y = canvas.height / 2 + Math.sin(x * frequency + angle) * amplitude * Math.sin(x * Math.PI / canvas.width);
              if (x === 0) {
                ctx.moveTo(x, y);
              } else {
                ctx.lineTo(x, y);
              }
            }
            ctx.stroke();
            this.angles[member.userId] = angle + 0.12;
          }

          this.animationTimers[member.userId] = requestAnimationFrame(draw);
        };

        draw();
      });
    },
    clearAllWaveAnimations() {
      Object.keys(this.animationTimers).forEach(userId => {
        if (this.animationTimers[userId]) {
          cancelAnimationFrame(this.animationTimers[userId]);
        }
      });
      this.animationTimers = {};
      this.canvasRefs = {};
    }
  }
};
</script>

<style scoped>
.meeting-overlay-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 99998;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.ambient-blur-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 1;
  background: radial-gradient(circle, rgba(15, 23, 42, 0.85) 0%, rgba(2, 6, 23, 0.95) 100%);
  backdrop-filter: blur(25px);
}

.meeting-glass-container {
  position: relative;
  z-index: 10;
  width: 95%;
  max-width: 720px;
  background: rgba(15, 23, 42, 0.65);
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: 0 40px 80px rgba(0, 0, 0, 0.65), inset 0 1px 0 rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(40px) saturate(1.7);
  border-radius: 24px;
  padding: 30px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 520px;
}

.meeting-header {
  text-align: center;
  margin-bottom: 24px;
  width: 100%;
}

.meeting-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin-bottom: 6px;
}

.meeting-icon {
  font-size: 28px;
  animation: pulseCamera 2s infinite ease-in-out;
}

@keyframes pulseCamera {
  0% { transform: scale(1); filter: drop-shadow(0 0 2px rgba(0, 132, 255, 0.5)); }
  50% { transform: scale(1.1); filter: drop-shadow(0 0 8px rgba(0, 132, 255, 0.8)); }
  100% { transform: scale(1); filter: drop-shadow(0 0 2px rgba(0, 132, 255, 0.5)); }
}

.title-text {
  font-size: 24px;
  font-weight: 800;
  color: #ffffff;
  letter-spacing: -0.5px;
  background: linear-gradient(135deg, #0084ff 0%, #a855f7 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.meeting-subtitle {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
  letter-spacing: 0.3px;
}

/* Multi-user Grid Layouts */
.meeting-body {
  width: 100%;
  flex: 1;
  display: grid;
  gap: 20px;
  margin-bottom: 30px;
  min-height: 300px;
  align-items: center;
}

.grid-1 {
  grid-template-columns: 1fr;
  max-width: 420px;
}

.grid-2 {
  grid-template-columns: 1fr 1fr;
}

.grid-4 {
  grid-template-columns: 1fr 1fr;
  grid-template-rows: 1fr 1fr;
}

.member-card {
  background: rgba(30, 41, 59, 0.45);
  border: 1.5px solid rgba(255, 255, 255, 0.08);
  border-radius: 18px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.3);
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  position: relative;
}

.member-card:hover {
  transform: translateY(-4px);
  border-color: rgba(255, 255, 255, 0.2);
  box-shadow: 0 15px 30px rgba(0, 0, 0, 0.4);
}

.member-card.speaking-glow {
  border-color: rgba(0, 132, 255, 0.7);
  box-shadow: 0 0 25px rgba(0, 132, 255, 0.35);
}

.member-card.self-card {
  background: rgba(30, 41, 59, 0.6);
}

.stream-panel {
  flex: 1;
  min-height: 140px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
  padding: 20px;
}

.stream-tag {
  position: absolute;
  top: 10px;
  left: 10px;
  background: rgba(15, 23, 42, 0.75);
  backdrop-filter: blur(8px);
  color: #ffffff;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 6px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  z-index: 5;
}

.avatar-container {
  position: relative;
  width: 72px;
  height: 72px;
  margin-bottom: 12px;
}

.member-avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: linear-gradient(135deg, #0084ff 0%, #a855f7 100%);
  border: 2.5px solid rgba(255, 255, 255, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 28px;
  font-weight: 800;
  box-shadow: 0 8px 24px rgba(0, 132, 255, 0.3);
  position: relative;
  z-index: 3;
}

.avatar-ripple {
  position: absolute;
  top: -4px;
  left: -4px;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  border: 2px solid rgba(0, 132, 255, 0.6);
  z-index: 1;
  animation: pulseRing 1.8s infinite cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes pulseRing {
  0% { transform: scale(0.95); opacity: 0.9; }
  100% { transform: scale(1.3); opacity: 0; }
}

.member-wave-canvas {
  width: 100%;
  height: 40px;
  opacity: 0.85;
}

.member-info {
  background: rgba(15, 23, 42, 0.8);
  padding: 8px 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.mic-status-icon {
  font-size: 13px;
}

.member-name {
  font-size: 13px;
  font-weight: 600;
  color: #ffffff;
}

/* Meeting Actions */
.meeting-actions {
  display: flex;
  gap: 32px;
  justify-content: center;
  width: 100%;
}

.action-btn-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}

.round-action-btn {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  border: none;
  outline: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.35);
  transition: all 0.25s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.round-action-btn:hover {
  transform: scale(1.08);
  box-shadow: 0 8px 22px rgba(0, 0, 0, 0.45);
}

.round-action-btn:active {
  transform: scale(0.95);
}

.bg-red {
  background: #ff3b30 !important;
}

.bg-translucent {
  background: rgba(255, 255, 255, 0.15) !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
}

.bg-translucent.active {
  background: rgba(255, 255, 255, 0.95) !important;
  color: #0f172a !important;
}

.round-action-btn.active .action-icon {
  filter: invert(1);
}

.action-icon {
  font-size: 24px;
  color: #ffffff;
  display: block;
}

.btn-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.75);
  font-weight: 500;
}

/* Animations */
.animate-fade-in {
  animation: fadeIn 0.3s ease-out forwards;
}

.animate-scale-up {
  animation: scaleUp 0.3s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

.animate-slide-up {
  animation: slideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes scaleUp {
  from { opacity: 0; transform: scale(0.93); }
  to { opacity: 1; transform: scale(1); }
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(15px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
