<script setup>
import { computed } from 'vue'
import { buildStatusDemo } from '../data/roleplayStatusDemo.js'

const props = defineProps({
  aiName: { type: String, default: '沈清野' },
  playerName: { type: String, default: '林念' },
})

const snap = computed(() => buildStatusDemo(props.aiName, props.playerName))
</script>

<template>
  <details class="status-panel" open>
    <summary>
      <div class="panel-summary-left">
        <span class="panel-title status-champagne">角色状态</span>
        <span class="panel-sub">本轮状态快照 · 随对话更新</span>
      </div>
      <span class="panel-chevron status-champagne">▾</span>
    </summary>
    <div class="panel-body">
      <p class="demo-hint">当前为演示快照；后端落库后将随会话真实更新。</p>

      <div class="status-grid">
        <div v-for="b in snap.blocks" :key="b.title" class="status-card">
          <p class="status-section-title">{{ b.title }}</p>
          <p>{{ b.content }}</p>
          <p class="status-os">{{ b.os }}</p>
        </div>
      </div>

      <div class="status-card">
        <p class="status-section-title">爱爱</p>
        <ul class="status-list">
          <li v-for="(item, i) in snap.intimacy" :key="i">{{ item }}</li>
        </ul>
        <p class="status-os">最近五次记录，按时间倒序。</p>
      </div>

      <div class="status-grid">
        <div v-for="b in snap.life" :key="b.title" class="status-card">
          <p class="status-section-title">{{ b.title }}</p>
          <p v-for="(line, i) in b.lines" :key="i">{{ line }}</p>
          <p class="status-os">{{ b.os }}</p>
        </div>
      </div>

      <div class="status-card">
        <p class="status-section-title">好感度</p>
        <div class="status-favor-row">
          <div v-for="f in snap.favorability" :key="f.name" class="status-card" style="padding: 10px">
            <p style="font-weight: 600">{{ f.name }}</p>
            <p>
              当前 {{ f.value }}
              <span class="status-champagne">{{ f.delta >= 0 ? '+' : '' }}{{ f.delta }}</span>
            </p>
          </div>
        </div>
        <p class="status-os">{{ snap.favorOs }}</p>
      </div>

      <div class="status-card">
        <p class="status-section-title">论坛</p>
        <div v-for="(post, i) in snap.forum" :key="i" class="status-post">
          <p class="status-champagne" style="font-weight: 600; margin-bottom: 6px">{{ post.title }}</p>
          <p style="margin-bottom: 8px">{{ post.content }}</p>
          <p v-for="(r, j) in post.replies" :key="j">
            <span class="status-forum-id">{{ r.author }}</span>：{{ r.text }}
          </p>
        </div>
      </div>

      <div class="status-card">
        <p class="status-section-title">小剧场</p>
        <p style="white-space: pre-wrap">{{ snap.theater.content }}</p>
        <p class="status-os">{{ snap.theater.os }}</p>
      </div>

      <div class="status-grid">
        <div v-for="block in snap.misc" :key="block.title" class="status-card">
          <p class="status-section-title">{{ block.title }}</p>
          <div v-for="(item, i) in block.items" :key="i" style="margin-bottom: 10px">
            <p>{{ item.text }}</p>
            <p class="status-os">{{ item.os }}</p>
          </div>
        </div>
      </div>

      <div class="status-card">
        <p class="status-section-title">接入</p>
        <p v-for="(line, i) in snap.access.lines" :key="i" style="margin-bottom: 8px">{{ line }}</p>
        <p class="status-os">{{ snap.access.os }}</p>
      </div>
    </div>
  </details>
</template>
