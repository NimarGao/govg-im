<template>
  <div v-if="callState" class="call-overlay-backdrop animate-fade-in">
    <!-- Blurry Ambient Background -->
    <div class="ambient-blur-bg" :style="{ backgroundImage: 'radial-gradient(circle, rgba(0, 132, 255, 0.2) 0%, rgba(126, 34, 206, 0.2) 100%)' }"></div>

    <div class="call-glass-container animate-scale-up">
      <!-- Calling Header / Session Info -->
      <div class="call-header">
        <div class="caller-profile">
          <div class="caller-avatar-glow">
            <div class="caller-avatar-circle">
              {{ peerName ? peerName[0].toUpperCase() : 'U' }}
            </div>
            <!-- Ripple Pulse rings -->
            <div v-if="callState !== 'connected'" class="ripple-ring ring-1"></div>
            <div v-if="callState !== 'connected'" class="ripple-ring ring-2"></div>
          </div>
          <span class="peer-name">{{ peerName }}</span>
          <span class="call-status-text">{{ statusText }}</span>
        </div>
      </div>

      <!-- Center Body - Video Stream Mock or 3D Telemetry Sine Waves -->
      <div class="call-body">
        <div v-if="callType === 'video' && callState === 'connected'" class="video-mock-container animate-fade-in">
          <div class="camera-stream local-stream">
            <span class="camera-tag">我</span>
            <div class="stream-pulse"></div>
          </div>
          <div class="camera-stream remote-stream">
            <span class="camera-tag">对方 ({{ peerName }})</span>
            <div class="remote-avatar">{{ peerName ? peerName[0].toUpperCase() : '?' }}</div>
            <!-- Canvas for mock camera motion detection/telemetry -->
            <canvas ref="videoCanvas" class="video-canvas"></canvas>
          </div>
        </div>
        <div v-else class="audio-wave-container">
          <canvas ref="waveCanvas" class="audio-wave-canvas"></canvas>
          <div class="duration-counter" v-if="callState === 'connected'">{{ formattedDuration }}</div>
        </div>
      </div>

      <!-- Call Action Controls -->
      <div class="call-actions">
        <!-- 1. Ringing Screen (Recipient) -->
        <div v-if="callState === 'ringing'" class="action-buttons-group animate-slide-up">
          <div class="action-btn-wrapper reject" @click="handleAction('reject')">
            <button class="round-call-btn bg-red animate-pulse-red">
              <span class="call-icon">📞</span>
            </button>
            <span class="btn-label">拒绝</span>
          </div>
          <div class="action-btn-wrapper accept" @click="handleAction('accept')">
            <button class="round-call-btn bg-green animate-pulse-green">
              <span class="call-icon icon-rotate-135">📞</span>
            </button>
            <span class="btn-label">接听</span>
          </div>
        </div>

        <!-- 2. Dialing Screen (Caller) -->
        <div v-else-if="callState === 'dialing'" class="action-buttons-group animate-slide-up">
          <div class="action-btn-wrapper cancel" @click="handleAction('cancel')">
            <button class="round-call-btn bg-red">
              <span class="call-icon">📞</span>
            </button>
            <span class="btn-label">取消拨打</span>
          </div>
        </div>

        <!-- 3. Connected Screen (Talking) -->
        <div v-else-if="callState === 'connected'" class="action-buttons-group animate-slide-up">
          <div class="action-btn-wrapper mute" @click="isMuted = !isMuted">
            <button class="round-call-btn bg-translucent" :class="{ active: isMuted }">
              <span class="call-icon">{{ isMuted ? '🎙️❌' : '🎙️' }}</span>
            </button>
            <span class="btn-label">{{ isMuted ? '被静音' : '静音' }}</span>
          </div>
          
          <div class="action-btn-wrapper hangup" @click="handleAction('hangup')">
            <button class="round-call-btn bg-red">
              <span class="call-icon">📞</span>
            </button>
            <span class="btn-label">挂断</span>
          </div>

          <div class="action-btn-wrapper speaker" @click="isSpeaker = !isSpeaker">
            <button class="round-call-btn bg-translucent" :class="{ active: isSpeaker }">
              <span class="call-icon">{{ isSpeaker ? '🔊' : '🔈' }}</span>
            </button>
            <span class="btn-label">扬声器</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'TUICallOverlay',
  props: {
    callState: {
      type: String, // 'dialing' | 'ringing' | 'connected' | null
      default: null
    },
    peerName: {
      type: String,
      default: '未知用户'
    },
    callType: {
      type: String, // 'voice' | 'video'
      default: 'voice'
    }
  },
  emits: ['action'],
  data() {
    return {
      durationSeconds: 0,
      timer: null,
      waveTimer: null,
      isMuted: false,
      isSpeaker: true,
      audioCtx: null,
      soundSource: null,
      oscillator: null,
      gainNode: null
    };
  },
  computed: {
    statusText() {
      if (this.callState === 'dialing') return '正在呼叫对方...';
      if (this.callState === 'ringing') return `邀请您进行${this.callType === 'video' ? '视频' : '语音'}通话`;
      if (this.callState === 'connected') return '通话已接通';
      return '';
    },
    formattedDuration() {
      const mins = Math.floor(this.durationSeconds / 60);
      const secs = this.durationSeconds % 60;
      return `${mins < 10 ? '0' : ''}${mins}:${secs < 10 ? '0' : ''}${secs}`;
    }
  },
  watch: {
    callState(newState) {
      if (newState === 'connected') {
        this.stopRingingSound();
        this.startCallDurationTimer();
        this.$nextTick(() => {
          this.initWaveCanvas();
          if (this.callType === 'video') {
            this.initVideoCanvas();
          }
        });
      } else if (newState === 'dialing') {
        this.resetTimer();
        this.playRingingSound('dialing');
        this.$nextTick(() => {
          this.initWaveCanvas();
        });
      } else if (newState === 'ringing') {
        this.resetTimer();
        this.playRingingSound('ringing');
        this.$nextTick(() => {
          this.initWaveCanvas();
        });
      } else {
        this.stopRingingSound();
        this.resetTimer();
        this.stopWaveAnimation();
      }
    }
  },
  mounted() {
    if (this.callState) {
      if (this.callState === 'connected') {
        this.startCallDurationTimer();
        this.initWaveCanvas();
        if (this.callType === 'video') this.initVideoCanvas();
      } else {
        this.playRingingSound(this.callState);
        this.initWaveCanvas();
      }
    }
  },
  beforeUnmount() {
    this.stopRingingSound();
    this.resetTimer();
    this.stopWaveAnimation();
  },
  methods: {
    startCallDurationTimer() {
      this.durationSeconds = 0;
      if (this.timer) clearInterval(this.timer);
      this.timer = setInterval(() => {
        this.durationSeconds++;
      }, 1000);
    },
    resetTimer() {
      if (this.timer) {
        clearInterval(this.timer);
        this.timer = null;
      }
      this.durationSeconds = 0;
    },
    handleAction(action) {
      this.stopRingingSound();
      this.$emit('action', action);
    },
    // Synthetic sound effects using Web Audio API (avoiding loading static mp3 resources!)
    playRingingSound(mode) {
      try {
        this.stopRingingSound();
        // Initialize AudioContext
        const AudioContextClass = window.AudioContext || window.webkitAudioContext;
        this.audioCtx = new AudioContextClass();
        
        if (mode === 'dialing') {
          // Play standard telephone dialing progress tone (450Hz tone, repeated 1s on, 4s off)
          this.playBeepPattern(450, 1.0, 4.0);
        } else if (mode === 'ringing') {
          // Play sweet double-ring tone (480Hz + 440Hz combined, repeat 1.2s on, 2.5s off)
          this.playBeepPattern([440, 480], 1.2, 2.5);
        }
      } catch (e) {
        console.warn('Web Audio synthesis failed or not allowed by browser security policies:', e);
      }
    },
    playBeepPattern(freqs, activeDuration, pauseDuration) {
      if (!this.audioCtx) return;
      
      const playTone = () => {
        if (!this.audioCtx || this.audioCtx.state === 'closed') return;
        
        this.gainNode = this.audioCtx.createGain();
        this.gainNode.gain.setValueAtTime(0.001, this.audioCtx.currentTime);
        this.gainNode.gain.linearRampToValueAtTime(0.2, this.audioCtx.currentTime + 0.05);
        this.gainNode.gain.setValueAtTime(0.2, this.audioCtx.currentTime + activeDuration - 0.05);
        this.gainNode.gain.linearRampToValueAtTime(0.001, this.audioCtx.currentTime + activeDuration);
        
        const oscs = [];
        const freqArray = Array.isArray(freqs) ? freqs : [freqs];
        freqArray.forEach(f => {
          const osc = this.audioCtx.createOscillator();
          osc.type = 'sine';
          osc.frequency.setValueAtTime(f, this.audioCtx.currentTime);
          osc.connect(this.gainNode);
          osc.start();
          oscs.push(osc);
        });

        this.gainNode.connect(this.audioCtx.destination);

        setTimeout(() => {
          oscs.forEach(o => { try { o.stop(); } catch(e){} });
          try { this.gainNode.disconnect(); } catch(e){}
        }, activeDuration * 1000);
      };

      // Trigger immediately and then interval
      playTone();
      const intervalMs = (activeDuration + pauseDuration) * 1000;
      this.soundSource = setInterval(playTone, intervalMs);
    },
    stopRingingSound() {
      if (this.soundSource) {
        clearInterval(this.soundSource);
        this.soundSource = null;
      }
      if (this.audioCtx) {
        try {
          this.audioCtx.close();
        } catch (e) {}
        this.audioCtx = null;
      }
    },
    // HTML5 Canvas animated sine wave drawing
    initWaveCanvas() {
      this.stopWaveAnimation();
      const canvas = this.$refs.waveCanvas;
      if (!canvas) return;
      const ctx = canvas.getContext('2d');
      if (!ctx) return;

      canvas.width = canvas.parentElement.clientWidth || 360;
      canvas.height = 120;

      let angle = 0;
      const draw = () => {
        if (!canvas) return;
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        
        const count = 3;
        for (let i = 0; i < count; i++) {
          ctx.beginPath();
          ctx.lineWidth = i === 0 ? 3 : 1.5;
          ctx.strokeStyle = i === 0 ? 'rgba(0, 132, 255, 0.8)' : `rgba(168, 85, 247, ${0.4 - i * 0.1})`;
          
          const amplitude = this.callState === 'connected' ? (35 - i * 8) : (15 - i * 4);
          const frequency = (0.015 + i * 0.005);
          
          for (let x = 0; x < canvas.width; x++) {
            const y = canvas.height / 2 + Math.sin(x * frequency + angle + i) * amplitude * Math.sin(x * Math.PI / canvas.width);
            if (x === 0) {
              ctx.moveTo(x, y);
            } else {
              ctx.lineTo(x, y);
            }
          }
          ctx.stroke();
        }

        angle += this.callState === 'connected' ? 0.08 : 0.03;
        this.waveTimer = requestAnimationFrame(draw);
      };
      draw();
    },
    stopWaveAnimation() {
      if (this.waveTimer) {
        cancelAnimationFrame(this.waveTimer);
        this.waveTimer = null;
      }
    },
    // Simulation of active video feed in high-fidelity (cool rotating vector matrix)
    initVideoCanvas() {
      const canvas = this.$refs.videoCanvas;
      if (!canvas) return;
      const ctx = canvas.getContext('2d');
      if (!ctx) return;

      canvas.width = 300;
      canvas.height = 200;

      let phase = 0;
      const drawVideo = () => {
        if (!canvas || this.callState !== 'connected' || this.callType !== 'video') return;
        ctx.clearRect(0, 0, canvas.width, canvas.height);

        // Blurry background effect
        ctx.fillStyle = 'rgba(15, 23, 42, 0.9)';
        ctx.fillRect(0, 0, canvas.width, canvas.height);

        // Draw HUD grid
        ctx.strokeStyle = 'rgba(0, 132, 255, 0.15)';
        ctx.lineWidth = 1;
        for (let x = 20; x < canvas.width; x += 20) {
          ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, canvas.height); ctx.stroke();
        }
        for (let y = 20; y < canvas.height; y += 20) {
          ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(canvas.width, y); ctx.stroke();
        }

        // Draw a simulated 3D rotating planet/avatar telemetry
        const cx = canvas.width / 2;
        const cy = canvas.height / 2;
        const radius = 50 + Math.sin(phase) * 5;

        // Draw outer orbital rings
        ctx.strokeStyle = 'rgba(168, 85, 247, 0.4)';
        ctx.lineWidth = 1.5;
        ctx.beginPath();
        ctx.ellipse(cx, cy, radius * 1.5, radius * 0.6, phase * 0.5, 0, Math.PI * 2);
        ctx.stroke();

        ctx.strokeStyle = 'rgba(0, 132, 255, 0.6)';
        ctx.beginPath();
        ctx.arc(cx, cy, radius, 0, Math.PI * 2);
        ctx.stroke();

        // Draw scanline
        const scanY = (Math.sin(phase * 2) + 1) * (canvas.height / 2);
        ctx.strokeStyle = 'rgba(38, 194, 129, 0.5)';
        ctx.lineWidth = 2;
        ctx.shadowColor = 'rgba(38, 194, 129, 0.6)';
        ctx.shadowBlur = 10;
        ctx.beginPath();
        ctx.moveTo(10, scanY);
        ctx.lineTo(canvas.width - 10, scanY);
        ctx.stroke();
        ctx.shadowBlur = 0; // reset

        // Draw HUD framing corners
        ctx.strokeStyle = 'rgba(0, 132, 255, 0.8)';
        ctx.lineWidth = 2.5;
        // top left
        ctx.beginPath(); ctx.moveTo(10, 25); ctx.lineTo(10, 10); ctx.lineTo(25, 10); ctx.stroke();
        // top right
        ctx.beginPath(); ctx.moveTo(canvas.width - 25, 10); ctx.lineTo(canvas.width - 10, 10); ctx.lineTo(canvas.width - 10, 25); ctx.stroke();
        // bottom left
        ctx.beginPath(); ctx.moveTo(10, canvas.height - 25); ctx.lineTo(10, canvas.height - 10); ctx.lineTo(25, canvas.height - 10); ctx.stroke();
        // bottom right
        ctx.beginPath(); ctx.moveTo(canvas.width - 25, canvas.height - 10); ctx.lineTo(canvas.width - 10, canvas.height - 10); ctx.lineTo(canvas.width - 10, canvas.height - 25); ctx.stroke();

        phase += 0.02;
        requestAnimationFrame(drawVideo);
      };
      drawVideo();
    }
  }
};
</script>

<style scoped>
/* ==========================================
   TUICALL OVERLAY MODERN Sleek CSS
   ========================================== */
.call-overlay-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 99999;
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
  background-size: cover;
  background-position: center;
  filter: blur(50px) brightness(0.4);
  transform: scale(1.15);
}

.call-glass-container {
  position: relative;
  z-index: 10;
  width: 90%;
  max-width: 450px;
  background: rgba(15, 23, 42, 0.7);
  border: 1px solid rgba(255, 255, 255, 0.15);
  box-shadow: 0 32px 64px rgba(0, 0, 0, 0.5), inset 0 1px 0 rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(40px) saturate(1.8);
  border-radius: 28px;
  padding: 40px 30px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 520px;
}

.call-header {
  margin-top: 10px;
  text-align: center;
  width: 100%;
}

.caller-profile {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.caller-avatar-glow {
  position: relative;
  margin-bottom: 8px;
}

.caller-avatar-circle {
  width: 88px;
  height: 88px;
  border-radius: 50%;
  background: linear-gradient(135deg, #0084ff 0%, #a855f7 100%);
  border: 3px solid rgba(255, 255, 255, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 38px;
  font-weight: 800;
  box-shadow: 0 8px 32px rgba(0, 132, 255, 0.35);
  position: relative;
  z-index: 5;
}

.ripple-ring {
  position: absolute;
  top: -6px;
  left: -6px;
  right: -6px;
  bottom: -6px;
  border-radius: 50%;
  border: 1.5px solid rgba(0, 132, 255, 0.5);
  z-index: 2;
  animation: rippleGlow 2s cubic-bezier(0.16, 1, 0.3, 1) infinite;
}

.ring-2 {
  animation-delay: 0.6s;
  border-color: rgba(168, 85, 247, 0.4);
}

@keyframes rippleGlow {
  0% { transform: scale(1); opacity: 1; }
  100% { transform: scale(1.5); opacity: 0; }
}

.peer-name {
  font-size: 22px;
  font-weight: 800;
  color: #ffffff;
  letter-spacing: -0.5px;
  text-shadow: 0 2px 4px rgba(0,0,0,0.3);
}

.call-status-text {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.7);
  letter-spacing: 0.3px;
}

.call-body {
  flex: 1;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 30px 0;
  min-height: 140px;
}

.audio-wave-container {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 15px;
}

.audio-wave-canvas {
  width: 100%;
  height: 100px;
}

.duration-counter {
  font-family: monospace;
  font-size: 18px;
  font-weight: 700;
  color: rgba(38, 194, 129, 0.95);
  background: rgba(38, 194, 129, 0.12);
  border: 1px solid rgba(38, 194, 129, 0.2);
  padding: 4px 14px;
  border-radius: 12px;
  letter-spacing: 0.5px;
}

.video-mock-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
  align-items: center;
  position: relative;
}

.camera-stream {
  border-radius: 16px;
  overflow: hidden;
  position: relative;
  box-shadow: 0 8px 24px rgba(0,0,0,0.3);
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: #0f172a;
}

.local-stream {
  width: 110px;
  height: 150px;
  position: absolute;
  right: 12px;
  top: -10px;
  z-index: 10;
  border: 2px solid rgba(0, 132, 255, 0.8);
  box-shadow: 0 12px 32px rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1e1b4b 0%, #111827 100%);
}

.local-stream .stream-pulse {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba(0, 132, 255, 0.5);
  animation: videoPulse 1.8s infinite ease-in-out;
}

@keyframes videoPulse {
  0% { transform: scale(0.6); opacity: 0.8; }
  100% { transform: scale(1.6); opacity: 0; }
}

.remote-stream {
  width: 100%;
  max-width: 320px;
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.remote-avatar {
  position: absolute;
  z-index: 2;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #a855f7 0%, #db2777 100%);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: bold;
  border: 2px solid rgba(255,255,255,0.5);
}

.video-canvas {
  width: 100%;
  height: 100%;
  display: block;
}

.camera-tag {
  position: absolute;
  top: 8px;
  left: 8px;
  background: rgba(15, 23, 42, 0.7);
  backdrop-filter: blur(8px);
  color: #ffffff;
  font-size: 10px;
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 4px;
  border: 1px solid rgba(255,255,255,0.08);
  z-index: 15;
}

.call-actions {
  width: 100%;
  display: flex;
  justify-content: center;
  margin-bottom: 10px;
}

.action-buttons-group {
  display: flex;
  gap: 32px;
  align-items: center;
  justify-content: center;
  width: 100%;
}

.action-btn-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.round-call-btn {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  border: none;
  outline: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.35);
  transition: all 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

.round-call-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.45);
}

.round-call-btn:active {
  transform: scale(0.95);
}

.bg-red {
  background: #ff3b30 !important;
}

.bg-green {
  background: #34c759 !important;
}

.bg-translucent {
  background: rgba(255, 255, 255, 0.15) !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
}

.bg-translucent.active {
  background: rgba(255, 255, 255, 0.9) !important;
  color: #0f172a !important;
}

.round-call-btn.active .call-icon {
  filter: invert(1);
}

.call-icon {
  font-size: 26px;
  color: #ffffff;
  display: block;
  user-select: none;
}

.icon-rotate-135 {
  transform: rotate(135deg);
}

.btn-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 500;
}

/* Vibrations & Pulsations animations */
@keyframes pulseRed {
  0% { box-shadow: 0 0 0 0 rgba(255, 59, 48, 0.6); }
  70% { box-shadow: 0 0 0 15px rgba(255, 59, 48, 0); }
  100% { box-shadow: 0 0 0 0 rgba(255, 59, 48, 0); }
}

@keyframes pulseGreen {
  0% { box-shadow: 0 0 0 0 rgba(52, 199, 89, 0.6); }
  70% { box-shadow: 0 0 0 15px rgba(52, 199, 89, 0); }
  100% { box-shadow: 0 0 0 0 rgba(52, 199, 89, 0); }
}

.animate-pulse-red {
  animation: pulseRed 1.6s infinite;
}

.animate-pulse-green {
  animation: pulseGreen 1.6s infinite;
}

/* Utility Animations */
.animate-fade-in {
  animation: fadeIn 0.35s ease-out forwards;
}

.animate-scale-up {
  animation: scaleUp 0.35s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

.animate-slide-up {
  animation: slideUp 0.35s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes scaleUp {
  from { opacity: 0; transform: scale(0.9); }
  to { opacity: 1; transform: scale(1); }
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
