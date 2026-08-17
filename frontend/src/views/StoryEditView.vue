<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api/client.js'

const props = defineProps({
  id: { type: [String, Number], required: true },
})

const router = useRouter()
const loading = ref(false)
const error = ref('')
const storyId = ref(null)
const editForm = reactive({
  title: '',
  author: '',
  background: '',
  nodes: [],
})

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const data = await api.getStoryBase(props.id)
    storyId.value = data.id
    editForm.title = data.title || ''
    editForm.author = data.author || ''
    editForm.background = data.background || ''
    editForm.nodes = (data.nodes || []).map((n, i) => ({
      seqNo: n.seqNo ?? i + 1,
      timeLabel: n.timeLabel || '',
      place: n.place || '',
      originalPlot: n.originalPlot || '',
      status: n.status || 'PENDING',
    }))
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function addNode() {
  editForm.nodes.push({
    seqNo: editForm.nodes.length + 1,
    timeLabel: '',
    place: '',
    originalPlot: '',
    status: 'PENDING',
  })
}

function removeNode(index) {
  editForm.nodes.splice(index, 1)
}

async function saveBase(confirm) {
  if (!editForm.title.trim() || editForm.nodes.length === 0) {
    error.value = '请填写书名，并至少保留一个原著节点'
    return
  }
  loading.value = true
  error.value = ''
  const body = {
    title: editForm.title.trim(),
    author: editForm.author.trim() || null,
    background: editForm.background,
    confirm,
    nodes: editForm.nodes.map((n, i) => ({
      seqNo: i + 1,
      timeLabel: n.timeLabel,
      place: n.place,
      originalPlot: n.originalPlot,
      status: n.status || 'PENDING',
    })),
  }
  try {
    const data = await api.updateStoryBase(storyId.value, body)
    if (confirm) {
      await router.push(`/story/${data.id}/play`)
    }
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="demo-page">
    <div class="demo-container">
      <header class="demo-hero">
        <h1>编辑剧情底本</h1>
        <p>请核对原著节点；不准的地方直接改。确认后才能以角色身份穿书。</p>
      </header>

      <p v-if="error" class="demo-banner error">{{ error }}</p>
      <p v-if="loading" class="demo-banner">加载中…</p>

      <div class="demo-card">
        <div class="demo-actions" style="margin-bottom: 16px">
          <button class="demo-btn" type="button" @click="router.push('/story')">返回穿书</button>
        </div>
        <label class="demo-field"><span>书名</span><input v-model="editForm.title" /></label>
        <label class="demo-field"><span>作者</span><input v-model="editForm.author" /></label>
        <label class="demo-field"><span>背景</span><textarea v-model="editForm.background" rows="3" /></label>

        <div style="display: flex; justify-content: space-between; align-items: center; margin: 16px 0 8px">
          <h2 style="margin: 0; font-size: 1rem">原著节点</h2>
          <button class="demo-btn" type="button" @click="addNode">加节点</button>
        </div>

        <div
          v-for="(n, idx) in editForm.nodes"
          :key="idx"
          class="demo-card"
          style="margin-bottom: 10px; padding: 14px; background: var(--demo-secondary)"
        >
          <div class="demo-row" style="margin-bottom: 8px">
            <input v-model="n.timeLabel" placeholder="时间" />
            <input v-model="n.place" placeholder="地点" />
            <button class="demo-btn" type="button" @click="removeNode(idx)">删</button>
          </div>
          <textarea
            v-model="n.originalPlot"
            rows="2"
            placeholder="原著走向"
            style="width: 100%; border: 1px solid var(--demo-border); border-radius: 10px; padding: 10px"
          />
          <p class="demo-muted" style="margin: 6px 0 0">状态：{{ n.status || 'PENDING' }}</p>
        </div>

        <div class="demo-actions">
          <button class="demo-btn" type="button" :disabled="loading" @click="saveBase(false)">仅保存草稿</button>
          <button class="demo-btn primary" type="button" :disabled="loading" @click="saveBase(true)">
            确认底本并开始
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
