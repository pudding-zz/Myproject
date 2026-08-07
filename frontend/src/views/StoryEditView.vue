<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api/client.js'
import AppBrandHeader from '../components/AppBrandHeader.vue'

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
  <section class="stage">
    <div class="atmosphere" aria-hidden="true" />
    <AppBrandHeader title="编辑剧情底本" lead="请核对原著节点；不准的地方直接改。确认后才能以角色身份穿书。" />
    <p v-if="error" class="banner error">{{ error }}</p>
    <p v-if="loading" class="banner">加载中…</p>

    <div class="panel">
      <div class="panel-head">
        <div>
          <h2>编辑剧情底本</h2>
          <p class="muted">确认后初始化当前世界，即可建角推进。</p>
        </div>
        <button type="button" class="ghost" @click="router.push('/')">返回</button>
      </div>
      <div class="edit-body">
        <label>
          <span>书名</span>
          <input v-model="editForm.title" />
        </label>
        <label>
          <span>作者</span>
          <input v-model="editForm.author" />
        </label>
        <label>
          <span>背景</span>
          <textarea v-model="editForm.background" rows="3" />
        </label>

        <div class="nodes-head">
          <h3>原著节点</h3>
          <button type="button" class="ghost" @click="addNode">加节点</button>
        </div>
        <div v-for="(n, idx) in editForm.nodes" :key="idx" class="node-card">
          <div class="node-row">
            <input v-model="n.timeLabel" placeholder="时间" />
            <input v-model="n.place" placeholder="地点" />
            <button type="button" class="ghost danger" @click="removeNode(idx)">删</button>
          </div>
          <textarea v-model="n.originalPlot" rows="2" placeholder="原著走向" />
          <span class="status-tag" :class="n.status">{{ n.status || 'PENDING' }}</span>
        </div>

        <div class="form-actions">
          <button type="button" class="ghost" :disabled="loading" @click="saveBase(false)">仅保存草稿</button>
          <button type="button" :disabled="loading" @click="saveBase(true)">确认底本并开始</button>
        </div>
      </div>
    </div>
  </section>
</template>
