<script setup>
import { computed } from 'vue'
import { themes } from '../data/themes.js'

const props = defineProps({
  themeId: { type: String, required: true },
})

const emit = defineEmits(['cycle'])

const current = computed(() => themes.find((t) => t.id === props.themeId) || themes[0])

const title = computed(
  () => `切换风格 · 当前「${current.value.name}」${current.value.phaseLabel}`,
)
</script>

<template>
  <button
    type="button"
    class="moon-btn"
    :title="title"
    :aria-label="title"
    @click="emit('cycle')"
  >
    <svg class="moon" viewBox="0 0 32 32" aria-hidden="true">
      <!-- crescent -->
      <g v-if="current.phase === 'crescent'" class="phase">
        <circle cx="16" cy="16" r="10" fill="currentColor" opacity="0.18" />
        <path
          d="M18.2 6.4a10 10 0 1 0 0 19.2 8.2 8.2 0 1 1 0-19.2z"
          fill="currentColor"
        />
      </g>
      <!-- full -->
      <g v-else-if="current.phase === 'full'" class="phase">
        <circle cx="16" cy="16" r="10" fill="currentColor" />
        <circle cx="12.5" cy="13" r="1.4" fill="var(--bg)" opacity="0.22" />
        <circle cx="18.5" cy="18" r="2" fill="var(--bg)" opacity="0.16" />
        <circle cx="14" cy="19.5" r="1.1" fill="var(--bg)" opacity="0.18" />
      </g>
      <!-- last quarter -->
      <g v-else class="phase">
        <circle cx="16" cy="16" r="10" fill="currentColor" opacity="0.18" />
        <path d="M16 6a10 10 0 0 1 0 20V6z" fill="currentColor" />
      </g>
    </svg>
  </button>
</template>

<style scoped>
.moon-btn {
  position: fixed;
  top: 16px;
  right: 16px;
  z-index: 40;
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border: 1px solid var(--line);
  border-radius: 50%;
  background: var(--bg-elevated);
  color: var(--brand);
  box-shadow: var(--shadow);
  cursor: pointer;
  backdrop-filter: blur(10px);
  transition: transform 0.25s ease, border-color 0.25s ease, color 0.25s ease;
}

.moon-btn:hover {
  transform: rotate(-12deg) scale(1.04);
  border-color: color-mix(in srgb, var(--accent) 45%, var(--line));
  color: var(--accent);
}

.moon {
  width: 22px;
  height: 22px;
}

.phase {
  transform-origin: 16px 16px;
  animation: phase-in 0.35s ease both;
}

@keyframes phase-in {
  from {
    opacity: 0;
    transform: rotate(-25deg) scale(0.85);
  }
  to {
    opacity: 1;
    transform: rotate(0) scale(1);
  }
}
</style>
