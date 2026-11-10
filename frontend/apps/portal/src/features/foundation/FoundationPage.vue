<script setup lang="ts">
import { onMounted } from 'vue';
import { AppFrame } from '@yunshu/ui';
import { useSessionStore } from '../../app/session';

const session = useSessionStore();
const messages = {
  idle: '等待连接',
  connecting: '正在连接服务',
  ready: '服务连接正常',
  error: '服务暂时无法连接',
};
onMounted(() => session.connect());
</script>

<template>
  <AppFrame title="学员端" subtitle="学习从这里开始">
    <h1>学员端基础工程已就绪</h1>
    <p class="lead">后续将在这里逐步接入已设计的学习与训练功能。</p>
    <section class="foundation-card" aria-label="服务连接状态">
      <div class="status-row" role="status" aria-live="polite">
        <span class="status-dot" :class="session.connection" aria-hidden="true"></span>
        <strong>{{ messages[session.connection] }}</strong>
      </div>
      <p class="muted">学员端已建立独立的请求与会话入口。业务功能将按模块逐步开放。</p>
      <button
        class="primary-button"
        :disabled="session.connection === 'connecting'"
        @click="session.connect"
      >
        重新连接
      </button>
    </section>
  </AppFrame>
</template>
