<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const active = computed(() => {
  const p = route.path
  if (p.startsWith('/roleplay')) return 'roleplay'
  if (p.startsWith('/story')) return 'story'
  return 'home'
})
</script>

<template>
  <div class="demo-shell">
    <nav class="demo-nav">
      <div class="demo-nav-inner">
        <router-link to="/" class="demo-nav-brand">穿书与AI角色对话</router-link>
        <ul class="demo-nav-links">
          <li>
            <router-link to="/" :class="{ active: active === 'home' }">首页</router-link>
          </li>
          <li>
            <router-link to="/story" :class="{ active: active === 'story' }">穿书</router-link>
          </li>
          <li>
            <router-link to="/roleplay" :class="{ active: active === 'roleplay' }">AI角色对话</router-link>
          </li>
        </ul>
      </div>
    </nav>
    <slot />
  </div>
</template>

<style scoped>
.demo-nav {
  position: fixed;
  top: 0;
  inset-inline: 0;
  z-index: 50;
  height: var(--demo-nav-h);
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--demo-border);
}

.demo-nav-inner {
  max-width: 1024px;
  margin: 0 auto;
  height: 100%;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.demo-nav-brand {
  font-size: 1.05rem;
  font-weight: 600;
  text-decoration: none;
  letter-spacing: -0.01em;
  color: var(--demo-text);
  white-space: nowrap;
}

.demo-nav-links {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  align-items: center;
  gap: 20px;
}

.demo-nav-links a {
  text-decoration: none;
  font-size: 0.9rem;
  color: var(--demo-muted);
  padding-bottom: 2px;
  border-bottom: 2px solid transparent;
}

.demo-nav-links a:hover,
.demo-nav-links a.active {
  color: var(--demo-text);
}

.demo-nav-links a.active {
  border-bottom-color: var(--demo-primary);
}

@media (max-width: 640px) {
  .demo-nav-brand {
    font-size: 0.92rem;
  }
  .demo-nav-links {
    gap: 12px;
  }
  .demo-nav-links a {
    font-size: 0.82rem;
  }
}
</style>
