<script setup>
import { computed, ref, watch } from 'vue'
import { api } from '../api/client.js'
import { buildStatusDemo } from '../data/roleplayStatusDemo.js'

const props = defineProps({
  sessionId: { type: [Number, String], default: null },
  aiName: { type: String, default: '沈清野' },
  playerName: { type: String, default: '林念' },
})

const EMPTY_SNAP = {
  blocks: [],
  intimacy: [],
  life: [],
  favorability: [],
  favorOs: '',
  forum: [],
  theater: { content: '', os: '' },
  misc: [],
  access: { lines: [], os: '' },
}

const snap = ref({ ...EMPTY_SNAP, theater: { content: '', os: '' }, access: { lines: [], os: '' } })
const fromApi = ref(false)
const loadError = ref('')

const hint = computed(() => {
  if (loadError.value) return `加载失败：${loadError.value}（已回退演示快照）`
  if (fromApi.value) return '会话真实状态快照'
  return '当前为演示快照；进入会话后将拉取真实状态。'
})

function normalizeSnap(data) {
  if (!data || typeof data !== 'object') return { ...EMPTY_SNAP }
  return {
    blocks: Array.isArray(data.blocks) ? data.blocks : [],
    intimacy: Array.isArray(data.intimacy) ? data.intimacy : [],
    life: Array.isArray(data.life) ? data.life : [],
    favorability: Array.isArray(data.favorability) ? data.favorability : [],
    favorOs: data.favorOs || '',
    forum: Array.isArray(data.forum) ? data.forum : [],
    theater: {
      content: data.theater?.content || '',
      os: data.theater?.os || '',
    },
    misc: Array.isArray(data.misc) ? data.misc : [],
    access: {
      lines: Array.isArray(data.access?.lines) ? data.access.lines : [],
      os: data.access?.os || '',
    },
  }
}

async function load() {
  loadError.value = ''
  const id = props.sessionId != null && props.sessionId !== '' ? Number(props.sessionId) : null
  if (!id) {
    snap.value = buildStatusDemo(props.aiName, props.playerName)
    fromApi.value = false
    return
  }
  try {
    const data = await api.getRoleplayStatus(id)
    if (data?.available === false) {
      snap.value = buildStatusDemo(props.aiName, props.playerName)
      fromApi.value = false
      return
    }
    snap.value = normalizeSnap(data)
    fromApi.value = true
  } catch (e) {
    loadError.value = e.message || '请求失败'
    snap.value = buildStatusDemo(props.aiName, props.playerName)
    fromApi.value = false
  }
}

watch(
  () => [props.sessionId, props.aiName, props.playerName],
  load,
  { immediate: true },
)
</script>

<template>
  <details class="status-panel">
    <summary>
      <div class="panel-summary-left">
        <span class="panel-title status-champagne">角色状态</span>
        <span class="panel-sub">本轮状态快照 · 随对话更新</span>
      </div>
      <span class="panel-chevron status-champagne">▾</span>
    </summary>
    <div class="panel-body">
      <p class="demo-hint">{{ hint }}</p>

      <div v-if="snap.blocks.length" class="status-grid">
        <details v-for="b in snap.blocks" :key="b.title" class="status-card status-sub">
          <summary class="status-section-title">{{ b.title }}</summary>
          <div class="status-sub-body">
            <p>{{ b.content }}</p>
            <p v-if="b.os" class="status-os">{{ b.os }}</p>
          </div>
        </details>
      </div>

      <details v-if="snap.intimacy.length" class="status-card status-sub">
        <summary class="status-section-title">爱爱</summary>
        <div class="status-sub-body">
          <ul class="status-list">
            <li v-for="(item, i) in snap.intimacy" :key="i">{{ item }}</li>
          </ul>
          <p class="status-os">最近五次记录，按时间倒序。</p>
        </div>
      </details>

      <div v-if="snap.life.length" class="status-grid">
        <details v-for="b in snap.life" :key="b.title" class="status-card status-sub">
          <summary class="status-section-title">{{ b.title }}</summary>
          <div class="status-sub-body">
            <p v-for="(line, i) in b.lines || []" :key="i">{{ line }}</p>
            <p v-if="b.os" class="status-os">{{ b.os }}</p>
          </div>
        </details>
      </div>

      <details v-if="snap.favorability.length || snap.favorOs" class="status-card status-sub">
        <summary class="status-section-title">好感度</summary>
        <div class="status-sub-body">
          <div class="status-favor-row">
            <div v-for="f in snap.favorability" :key="f.name" class="status-card" style="padding: 10px">
              <p style="font-weight: 600">{{ f.name }}</p>
              <p>
                当前 {{ f.value }}
                <span class="status-champagne">{{ f.delta >= 0 ? '+' : '' }}{{ f.delta }}</span>
              </p>
            </div>
          </div>
          <p v-if="snap.favorOs" class="status-os">{{ snap.favorOs }}</p>
        </div>
      </details>

      <details v-if="snap.forum.length" class="status-card status-sub">
        <summary class="status-section-title">论坛</summary>
        <div class="status-sub-body">
          <div v-for="(post, i) in snap.forum" :key="i" class="status-post">
            <p class="status-champagne" style="font-weight: 600; margin-bottom: 6px">{{ post.title }}</p>
            <p style="margin-bottom: 8px">{{ post.content }}</p>
            <p v-for="(r, j) in post.replies || []" :key="j">
              <span class="status-forum-id">{{ r.author }}</span>：{{ r.text }}
            </p>
          </div>
        </div>
      </details>

      <details
        v-if="snap.theater.content || snap.theater.os"
        class="status-card status-sub"
      >
        <summary class="status-section-title">小剧场</summary>
        <div class="status-sub-body">
          <p style="white-space: pre-wrap">{{ snap.theater.content }}</p>
          <p v-if="snap.theater.os" class="status-os">{{ snap.theater.os }}</p>
        </div>
      </details>

      <div v-if="snap.misc.length" class="status-grid">
        <details v-for="block in snap.misc" :key="block.title" class="status-card status-sub">
          <summary class="status-section-title">{{ block.title }}</summary>
          <div class="status-sub-body">
            <div v-for="(item, i) in block.items || []" :key="i" style="margin-bottom: 10px">
              <p>{{ item.text }}</p>
              <p v-if="item.os" class="status-os">{{ item.os }}</p>
            </div>
          </div>
        </details>
      </div>

      <details
        v-if="(snap.access.lines && snap.access.lines.length) || snap.access.os"
        class="status-card status-sub"
      >
        <summary class="status-section-title">接入</summary>
        <div class="status-sub-body">
          <p v-for="(line, i) in snap.access.lines" :key="i" style="margin-bottom: 8px">{{ line }}</p>
          <p v-if="snap.access.os" class="status-os">{{ snap.access.os }}</p>
        </div>
      </details>

      <p
        v-if="fromApi && !snap.blocks.length && !snap.life.length && !snap.intimacy.length"
        class="demo-hint"
      >
        状态为空骨架，可用 PUT /status 写入内容。
      </p>
    </div>
  </details>
</template>
