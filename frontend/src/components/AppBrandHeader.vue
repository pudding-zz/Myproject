<script setup>
defineProps({
  title: { type: String, default: '选一本小说 → 生成剧情底本 → 以角色身份进书里玩' },
  lead: {
    type: String,
    default:
      '非官方剧情底本，仅私人娱乐。你选谁就是谁；AI 演世界与其他 NPC，推进可能改写大事件。',
  },
})
</script>

<template>
  <header class="top">
    <div class="brand-block">
      <div class="logo-row">
        <span class="mark" aria-hidden="true">
          <svg viewBox="0 0 36 36">
            <circle cx="18" cy="18" r="11" fill="currentColor" opacity="0.16" />
            <path
              d="M20.5 7.2a11 11 0 1 0 0 21.6 9 9 0 1 1 0-21.6z"
              fill="currentColor"
            />
          </svg>
        </span>
        <p class="brand">望月</p>
      </div>
      <h1>{{ title }}</h1>
      <p class="lead">{{ lead }}</p>
    </div>
  </header>
</template>
