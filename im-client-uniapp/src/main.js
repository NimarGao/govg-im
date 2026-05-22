import { createApp } from 'vue'
import App from './App.vue'
import uni from './uni-polyfill'

// 注入全局变量 uni，使得原始的 index.vue 无缝使用
window.uni = uni

const app = createApp(App)
app.config.globalProperties.uni = uni
app.mount('#app')
