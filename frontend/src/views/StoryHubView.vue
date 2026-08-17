<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api/client.js'

const router = useRouter()
const loading = ref(false)
const error = ref('')
const outlineEnabled = ref(true)
const storyBases = ref([])
const titleForm = reactive({ title: '', author: '' })
const manualForm = reactive({
  title: '',
  author: '',
  background: '',
  firstNode: '',
})

onMounted(refresh)

async function refresh() {
  error.value = ''
  loading.value = true
  try {
    const settings = await api.settings()
    outlineEnabled.value = !!settings.outlineFromTitleEnabled
    storyBases.value = await api.listStoryBases()
  } catch (e) {
    error.value = e.message + '（请确认后端已启动）'
  } finally {
    loading.value = false
  }
}

async function fromTitle() {
  if (!titleForm.title.trim()) return
  loading.value = true
  error.value = ''
  try {
    const data = await api.fromTitle({
      title: titleForm.title.trim(),
      author: titleForm.author.trim() || undefined,
    })
    await router.push(`/story/${data.id}/edit`)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function openExisting(id, status) {
  if (status === 'CONFIRMED') {
    await router.push(`/story/${id}/play`)
  } else {
    await router.push(`/story/${id}/edit`)
  }
}

async function createManual() {
  if (!manualForm.title.trim() || !manualForm.firstNode.trim()) {
    error.value = '请填写书名，并写至少一个原著节点走向'
    return
  }
  loading.value = true
  error.value = ''
  try {
    const data = await api.createStoryBase({
      title: manualForm.title.trim(),
      author: manualForm.author.trim() || null,
      background: manualForm.background,
      confirm: false,
      nodes: [
        {
          seqNo: 1,
          timeLabel: '开局',
          place: '',
          originalPlot: manualForm.firstNode.trim(),
          status: 'PENDING',
        },
      ],
    })
    await router.push(`/story/${data.id}/edit`)
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
        <h1>穿书工作台</h1>
        <p>选一本小说 → 生成剧情底本 → 以角色身份进书里玩。非官方剧情底本，仅私人娱乐。</p>
      </header>

      <p v-if="error" class="demo-banner error">{{ error }}</p>
      <p v-if="loading" class="demo-banner">加载中…</p>

      <h2 class="demo-section-title">已有剧情底本</h2>
      <div class="demo-card">
        <template v-if="storyBases.length">
          <button
            v-for="b in storyBases"
            :key="b.id"
            type="button"
            class="demo-list-btn"
            @click="openExisting(b.id, b.status)"
          >
            <strong>{{ b.title }}</strong>
            <span>{{ b.status === 'CONFIRMED' ? '已确认' : '草稿' }} · {{ b.author || '未知作者' }}</span>
          </button>
        </template>
        <p v-else class="demo-muted">还没有底本。可手建，或空库启动时使用示例《斗破苍穹》。</p>
      </div>

      <h2 class="demo-section-title">手建剧情底本</h2>
      <div class="demo-card">
        <p class="demo-muted" style="margin-top: 0">写入数据库，之后可继续加节点并确认。</p>
        <form @submit.prevent="createManual">
          <label class="demo-field"><span>书名</span><input v-model="manualForm.title" required /></label>
          <label class="demo-field"><span>作者（可选）</span><input v-model="manualForm.author" /></label>
          <label class="demo-field"><span>背景</span><textarea v-model="manualForm.background" rows="2" /></label>
          <label class="demo-field"
            ><span>第一个原著节点走向</span
            ><textarea v-model="manualForm.firstNode" rows="2" required
          /></label>
          <div class="demo-actions">
            <button class="demo-btn primary" type="submit" :disabled="loading">创建草稿并编辑</button>
          </div>
        </form>
      </div>

      <h2 class="demo-section-title">书名取纲</h2>
      <div class="demo-card">
        <p class="demo-muted" style="margin-top: 0">
          自动取纲：{{ outlineEnabled ? '开启' : '已关闭（可改环境变量 STORY_OUTLINE_FROM_TITLE_ENABLED）' }}
        </p>
        <form class="demo-row" @submit.prevent="fromTitle">
          <input v-model="titleForm.title" placeholder="书名" :disabled="!outlineEnabled" required />
          <input v-model="titleForm.author" placeholder="作者（可选）" :disabled="!outlineEnabled" />
          <button class="demo-btn primary" type="submit" :disabled="!outlineEnabled || loading">
            生成剧情底本
          </button>
        </form>
      </div>
    </div>
  </div>
</template>
