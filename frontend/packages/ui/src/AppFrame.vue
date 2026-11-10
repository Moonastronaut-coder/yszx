<script setup lang="ts">
defineProps<{ title: string; subtitle: string }>();
</script>

<template>
  <div class="app-frame">
    <header class="app-header">
      <a class="brand" href="/" aria-label="云枢智学首页"
        ><span class="brand-symbol">云</span>云枢智学</a
      >
      <span class="app-label">{{ title }}</span>
    </header>
    <main class="app-main">
      <p class="eyebrow">{{ subtitle }}</p>
      <slot />
    </main>
    <footer class="app-footer">云枢智学 · 让学习更有方向</footer>
  </div>
</template>
