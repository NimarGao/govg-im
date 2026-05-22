// Uni-App browser polyfill for govg-im H5体验仓
// 允许 Uni-App 前端源码在纯 H5 (Vite/Vue3) 容器中完美运行，无缝对接 Netty 后端

const uni = {
  // 1. 本地存储 (LocalStorage)
  getStorageSync(key) {
    const val = localStorage.getItem(key);
    if (!val) return '';
    try {
      return JSON.parse(val);
    } catch (e) {
      return val;
    }
  },
  
  setStorageSync(key, data) {
    const str = typeof data === 'object' ? JSON.stringify(data) : data;
    localStorage.setItem(key, str);
  },

  setStorage({ key, data, success }) {
    this.setStorageSync(key, data);
    if (success) success();
  },

  getStorage({ key, success }) {
    const data = this.getStorageSync(key);
    if (success) success({ data });
  },

  // 2. 界面交互 (Toast, Loading, Modal)
  showToast({ title, icon = 'none', duration = 2000 }) {
    console.log(`[Toast] ${title}`);
    
    // 动态创建极具毛玻璃质感的 Toast 提示
    let toast = document.getElementById('custom-glass-toast');
    if (!toast) {
      toast = document.createElement('div');
      toast.id = 'custom-glass-toast';
      toast.style.cssText = `
        position: fixed;
        top: 20px;
        left: 50%;
        transform: translateX(-50%) translateY(-20px);
        background: rgba(15, 23, 42, 0.7);
        border: 1px solid rgba(255, 255, 255, 0.1);
        backdrop-filter: blur(15px);
        color: #ffffff;
        padding: 12px 24px;
        border-radius: 12px;
        font-size: 14px;
        z-index: 10000;
        box-shadow: 0 10px 25px rgba(0,0,0,0.3);
        opacity: 0;
        transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
        display: flex;
        align-items: center;
        gap: 8px;
        pointer-events: none;
      `;
      document.body.appendChild(toast);
    }

    const iconMap = {
      success: '✅',
      loading: '⏳',
      exception: '❌',
      none: '💡'
    };

    toast.innerHTML = `<span>${iconMap[icon] || '💡'}</span> <span>${title}</span>`;
    
    // 触发动画展示
    setTimeout(() => {
      toast.style.opacity = '1';
      toast.style.transform = 'translateX(-50%) translateY(0)';
    }, 10);

    // 自动销毁定时器
    if (toast.timer) clearTimeout(toast.timer);
    toast.timer = setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateX(-50%) translateY(-20px)';
    }, duration);
  },

  showLoading({ title = '加载中...' }) {
    console.log(`[Loading] ${title}`);
    let loader = document.getElementById('custom-glass-loader');
    if (!loader) {
      loader = document.createElement('div');
      loader.id = 'custom-glass-loader';
      loader.style.cssText = `
        position: fixed;
        top: 0; left: 0; right: 0; bottom: 0;
        background: rgba(15, 23, 42, 0.6);
        backdrop-filter: blur(10px);
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        z-index: 9999;
        color: white;
        font-size: 15px;
        gap: 15px;
        opacity: 0;
        transition: opacity 0.25s ease;
      `;
      loader.innerHTML = `
        <div class="spinner-ring" style="
          width: 40px; height: 40px;
          border: 3px solid rgba(255,255,255,0.1);
          border-top-color: #0084ff;
          border-radius: 50%;
          animation: spin 0.8s linear infinite;
        "></div>
        <style>
          @keyframes spin { to { transform: rotate(360deg); } }
        </style>
        <span class="loader-text">${title}</span>
      `;
      document.body.appendChild(loader);
      setTimeout(() => loader.style.opacity = '1', 5);
    } else {
      loader.querySelector('.loader-text').innerText = title;
    }
  },

  hideLoading() {
    const loader = document.getElementById('custom-glass-loader');
    if (loader) {
      loader.style.opacity = '0';
      setTimeout(() => loader.remove(), 250);
    }
  },

  showModal({ title, content, showCancel = true, success }) {
    console.log(`[Modal] ${title}: ${content}`);
    const confirmed = confirm(`${title}\n\n${content}`);
    if (success) {
      success({
        confirm: confirmed,
        cancel: !confirmed
      });
    }
  },

  // 3. 网络请求 (HTTP API Fetch Wrapper)
  request({ url, method = 'GET', header = {}, data = null, success, fail }) {
    const options = {
      method: method.toUpperCase(),
      headers: {
        'Content-Type': 'application/json',
        ...header
      }
    };

    if (data && options.method !== 'GET' && options.method !== 'HEAD') {
      options.body = typeof data === 'string' ? data : JSON.stringify(data);
    }

    fetch(url, options)
      .then(async (res) => {
        let resData;
        const text = await res.text();
        try {
          resData = JSON.parse(text);
        } catch (e) {
          resData = text;
        }

        if (success) {
          success({
            statusCode: res.status,
            data: resData,
            header: Object.fromEntries(res.headers.entries())
          });
        }
      })
      .catch((err) => {
        console.error('Request failed:', err);
        if (fail) fail(err);
      });
  },

  // 4. 双工通信 (WebSocket Wrapper)
  connectSocket({ url, success, fail }) {
    console.log(`[Socket] Connecting to ${url}`);
    let ws;
    try {
      ws = new WebSocket(url);
      if (success) success();
    } catch (e) {
      if (fail) fail(e);
      return null;
    }

    const socketTask = {
      ws: ws,
      onOpen(cb) {
        ws.onopen = (event) => cb(event);
      },
      onMessage(cb) {
        ws.onmessage = (event) => cb({ data: event.data });
      },
      onClose(cb) {
        ws.onclose = (event) => cb(event);
      },
      onError(cb) {
        ws.onerror = (event) => cb(event);
      },
      send({ data, success, fail }) {
        if (ws.readyState === WebSocket.OPEN) {
          ws.send(data);
          if (success) success();
        } else {
          console.warn('[Socket] Attempted to send message while WebSocket is not open.');
          if (fail) fail(new Error('WebSocket is not open'));
        }
      },
      close() {
        ws.close();
      }
    };

    return socketTask;
  },

  // 5. 资源选择与图片上传 (DOM File Input & Multipart Post)
  chooseImage({ count = 1, success, fail }) {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = 'image/*';
    if (count > 1) input.multiple = true;
    
    input.onchange = (e) => {
      const files = Array.from(e.target.files);
      if (files.length === 0) return;

      const tempFilePaths = files.map(file => URL.createObjectURL(file));
      // 将真实 File 关联挂载到 window 对象上供 uploadFile 获取
      window._pendingFiles = window._pendingFiles || {};
      tempFilePaths.forEach((path, idx) => {
        window._pendingFiles[path] = files[idx];
      });

      if (success) {
        success({ tempFilePaths });
      }
    };

    input.click();
  },

  uploadFile({ url, filePath, name, success, fail }) {
    console.log(`[Upload] FilePath: ${filePath} to ${url}`);
    
    // 获取 chooseImage 中暂存的真实 File 对象
    const file = window._pendingFiles ? window._pendingFiles[filePath] : null;
    if (!file) {
      // 降级尝试通过 Blob URL fetch 转换
      fetch(filePath)
        .then(res => res.blob())
        .then(blob => {
          const mockFile = new File([blob], "upload_image.png", { type: blob.type });
          this.doActualUpload(url, mockFile, name, success, fail);
        })
        .catch(err => {
          console.error('[Upload] Failed to resolve local resource blob', err);
          if (fail) fail(err);
        });
      return;
    }

    this.doActualUpload(url, file, name, success, fail);
  },

  doActualUpload(url, file, name, success, fail) {
    const formData = new FormData();
    formData.append(name, file);

    fetch(url, {
      method: 'POST',
      body: formData
    })
      .then(async (res) => {
        const text = await res.text();
        if (success) {
          success({
            statusCode: res.status,
            data: text
          });
        }
      })
      .catch(err => {
        console.error('[Upload] Upload fetch failed', err);
        if (fail) fail(err);
      });
  },

  // 6. 全屏图片预览
  previewImage({ urls }) {
    if (!urls || urls.length === 0) return;
    const url = urls[0];

    const previewOverlay = document.createElement('div');
    previewOverlay.style.cssText = `
      position: fixed;
      top: 0; left: 0; right: 0; bottom: 0;
      background: rgba(0, 0, 0, 0.95);
      z-index: 100000;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: zoom-out;
      opacity: 0;
      transition: opacity 0.25s ease;
    `;
    previewOverlay.innerHTML = `
      <img src="${url}" style="max-width: 90%; max-height: 90%; border-radius: 8px; box-shadow: 0 10px 40px rgba(0,0,0,0.5);" />
    `;
    
    previewOverlay.onclick = () => {
      previewOverlay.style.opacity = '0';
      setTimeout(() => previewOverlay.remove(), 250);
    };

    document.body.appendChild(previewOverlay);
    setTimeout(() => previewOverlay.style.opacity = '1', 5);
  },

  // 7. 剪切板读写
  setClipboardData({ data, success }) {
    navigator.clipboard.writeText(data)
      .then(() => {
        uni.showToast({ title: '内容已成功复制到剪切板' });
        if (success) success();
      })
      .catch(err => {
        console.error('Failed to copy to clipboard', err);
        uni.showToast({ title: '复制失败，请手动选择复制', icon: 'exception' });
      });
  },

  // 8. 元素选择与盒模型尺寸计算 mock
  createSelectorQuery() {
    return {
      in() { return this; },
      select(selector) {
        this.selector = selector;
        return this;
      },
      boundingClientRect(callback) {
        // 在 Vue 的 tick 之后执行，保证 DOM 已经渲染
        setTimeout(() => {
          const el = document.querySelector(this.selector);
          if (el) {
            callback({
              height: el.clientHeight,
              width: el.clientWidth,
              top: el.offsetTop,
              left: el.offsetLeft
            });
          } else {
            callback(null);
          }
        }, 1);
        return this;
      },
      exec() {}
    };
  }
};

export default uni;
