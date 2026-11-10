import { defineStore } from 'pinia';
import { ref } from 'vue';
import { api } from './http';

export const useSessionStore = defineStore('admin-session', () => {
  const connection = ref<'idle' | 'connecting' | 'ready' | 'error'>('idle');
  async function connect() {
    connection.value = 'connecting';
    try {
      await api.refreshCsrf();
      connection.value = 'ready';
    } catch {
      connection.value = 'error';
    }
  }
  // CSRF 握手只表示连接就绪，不表示用户已登录。
  return { connection, connect };
});
