<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api/client.js'
import AppBrandHeader from '../components/AppBrandHeader.vue'

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

onMounted(refreshHome)

async function refreshHome() {
  error.value = ''
  loading.value = true
  try {
    const settings = await api.settings()
    outlineEnabled.value = !!settings.outlineFromTitleEnabled
    storyBases.value = await api.listStoryBases()
  } catch (e) {
    error.value = e.message + '（请确认后端已启动，且 Vite 代理 /api）'
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
  <section class="stage">
    <div class="atmosphere" aria-hidden="true" />
    <AppBrandHeader />
    <p v-if="error" class="banner error">{{ error }}</p>
    <p v-if="loading" class="banner">加载中…</p>

    <div class="panel home">
      <div class="block">
        <h2>玩法入口</h2>
        <p class="muted">穿书仍用下方底本；角色扮演可单独设定 AI 与玩家身份后开聊。</p>
        <div class="form-actions">
          <button type="button" @click="router.push('/roleplay')">进入 AI 角色对话</button>
        </div>
      </div>

      <div class="block">
        <h2>已有剧情底本</h2>
        <ul v-if="storyBases.length" class="base-list">
          <li v-for="b in storyBases" :key="b.id">
            <button type="button" @click="openExisting(b.id, b.status)">
              <strong>{{ b.title }}</strong>
              <span>{{ b.status === 'CONFIRMED' ? '已确认' : '草稿' }} · {{ b.author || '未知作者' }}</span>
            </button>
          </li>
        </ul>
        <p v-else class="muted">还没有底本。可手建，或空库启动时使用示例《斗破苍穹》。</p>
      </div>

      <div class="block">
        <h2>手建剧情底本</h2>
        <p class="muted">写入数据库，之后可继续加节点并确认。</p>
        <form class="edit-body" style="padding: 0" @submit.prevent="createManual">
          <label><span>书名</span><input v-model="manualForm.title" required /></label>
          <label><span>作者（可选）</span><input v-model="manualForm.author" /></label>
          <label><span>背景</span><textarea v-model="manualForm.background" rows="2" /></label>
          <label><span>第一个原著节点走向</span><textarea v-model="manualForm.firstNode" rows="2" required /></label>
          <div class="form-actions">
            <button type="submit" :disabled="loading">创建草稿并编辑</button>
          </div>
        </form>
      </div>

      <div class="block">
        <h2>书名取纲</h2>
        <p class="muted">
          自动取纲：{{ outlineEnabled ? '开启' : '已关闭（可改环境变量 STORY_OUTLINE_FROM_TITLE_ENABLED）' }}
        </p>
        <form class="row-form" @submit.prevent="fromTitle">
          <input v-model="titleForm.title" placeholder="书名" :disabled="!outlineEnabled" required />
          <input v-model="titleForm.author" placeholder="作者（可选）" :disabled="!outlineEnabled" />
          <button type="submit" :disabled="!outlineEnabled || loading">生成剧情底本</button>
        </form>
      </div>
    </div>
  </section>
</template>
