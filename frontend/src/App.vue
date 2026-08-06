<script setup>
import { ref, watch } from 'vue'
import ChatDemo from './components/ChatDemo.vue'
import MoonThemeToggle from './components/MoonThemeToggle.vue'
import { themes } from './data/themes.js'

const themeId = ref(themes[0].id)

watch(
  themeId,
  (id) => {
    document.documentElement.setAttribute('data-theme', id)
  },
  { immediate: true },
)

function cycleTheme() {
  const idx = themes.findIndex((t) => t.id === themeId.value)
  themeId.value = themes[(idx + 1) % themes.length].id
}
</script>

<template>
  <div class="page" :data-theme="themeId">
    <MoonThemeToggle :theme-id="themeId" @cycle="cycleTheme" />
    <ChatDemo :theme-id="themeId" />
  </div>
</template>

<style scoped>
.page {
  min-height: 100%;
  background: var(--bg);
}
</style>
