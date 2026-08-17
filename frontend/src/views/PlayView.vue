<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api/client.js'
import '../styles/play.css'

const props = defineProps({
  id: { type: [String, Number], required: true },
})

const router = useRouter()
const panel = ref('play') // play | theater | divergences
const loading = ref(false)
const error = ref('')
const story = ref(null)
const characters = ref([])
const selectedCharacterId = ref(null)
const chatId = ref(null)
const messages = ref([])
const draft = ref('')
const sending = ref(false)
const listRef = ref(null)
const divergences = ref([])
const showCreate = ref(false)
const theaterSelected = ref([])
const theaterPlayerLine = ref('')
const theaterLines = ref([])
const theaterDivergence = ref('')

const createForm = reactive({
  name: '',
  gender: 'male',
  title: '',
  setting: '',
  personality: '',
  playerInsert: true,
})

const selectedCharacter = computed(() =>
  characters.value.find((c) => c.id === selectedCharacterId.value),
)

const genderLabel = { male: '男', female: '女', other: '其他' }
const statusLabel = { PENDING: '待发生', CHANGED: '已改写', SKIPPED: '已跳过' }

onMounted(enterPlay)

async function refreshStory() {
  story.value = await api.getStoryBase(props.id)
}

async function enterPlay() {
  loading.value = true
  error.value = ''
  try {
    await refreshStory()
    if (story.value.status !== 'CONFIRMED') {
      await router.replace(`/story/${props.id}/edit`)
      return
    }
    characters.value = await api.listCharacters(story.value.id)
    selectedCharacterId.value = characters.value[0]?.id ?? null
    chatId.value = null
    messages.value = []
    showCreate.value = false
    theaterSelected.value = characters.value.slice(0, 2).map((c) => c.id)
    panel.value = 'play'
    if (selectedCharacterId.value) {
      await ensureChat()
    }
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function ensureChat() {
  if (!selectedCharacterId.value || !story.value) return
  const created = await api.createChat({
    characterId: selectedCharacterId.value,
    storyBaseId: story.value.id,
  })
  chatId.value = created.id
  messages.value = await api.listMessages(chatId.value)
  await scrollBottom()
}

async function selectCharacter(id) {
  selectedCharacterId.value = id
  showCreate.value = false
  try {
    await ensureChat()
  } catch (e) {
    error.value = e.message
  }
}

function openCreate() {
  showCreate.value = true
  createForm.name = ''
  createForm.gender = 'male'
  createForm.title = ''
  createForm.setting = ''
  createForm.personality = ''
  createForm.playerInsert = true
}

async function createCharacter() {
  if (!createForm.name.trim() || !story.value) return
  loading.value = true
  error.value = ''
  try {
    const c = await api.createCharacter({
      storyBaseId: story.value.id,
      name: createForm.name.trim(),
      gender: createForm.gender,
      title: createForm.title,
      setting: createForm.setting,
      personality: createForm.personality,
      playerInsert: createForm.playerInsert,
    })
    characters.value = await api.listCharacters(story.value.id)
    showCreate.value = false
    await selectCharacter(c.id)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function scrollBottom() {
  await nextTick()
  if (listRef.value) listRef.value.scrollTop = listRef.value.scrollHeight
}

watch(messages, () => scrollBottom(), { deep: true })

async function send() {
  const text = draft.value.trim()
  if (!text || sending.value || !chatId.value) return
  sending.value = true
  error.value = ''
  draft.value = ''
  try {
    const msg = await api.sendMessage(chatId.value, text)
    messages.value = [...messages.value, { role: 'user', content: text }, msg]
    await refreshStory()
  } catch (e) {
    error.value = e.message
    draft.value = text
  } finally {
    sending.value = false
  }
}

async function advance() {
  if (sending.value || !chatId.value) return
  const action = draft.value.trim() || '（推进剧情）'
  sending.value = true
  error.value = ''
  draft.value = ''
  try {
    messages.value = [...messages.value, { role: 'user', content: action }]
    const msg = await api.advance(chatId.value, action)
    messages.value = [...messages.value, msg]
    await refreshStory()
  } catch (e) {
    error.value = e.message
  } finally {
    sending.value = false
  }
}

function onKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    send()
  }
}

async function openDivergences() {
  if (!story.value) return
  loading.value = true
  try {
    divergences.value = await api.listDivergences(story.value.id)
    panel.value = 'divergences'
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function toggleTheaterChar(id) {
  if (theaterSelected.value.includes(id)) {
    theaterSelected.value = theaterSelected.value.filter((x) => x !== id)
  } else if (theaterSelected.value.length < 6) {
    theaterSelected.value = [...theaterSelected.value, id]
  }
}

async function runTheater() {
  if (theaterSelected.value.length < 2 || !story.value) {
    error.value = '对戏至少选择 2 个角色'
    return
  }
  sending.value = true
  error.value = ''
  try {
    const res = await api.theaterRound(story.value.id, {
      characterIds: theaterSelected.value,
      playerLine: theaterPlayerLine.value.trim() || undefined,
      turns: 3,
    })
    theaterLines.value = res.lines || []
    theaterDivergence.value = res.divergence || ''
    theaterPlayerLine.value = ''
    panel.value = 'theater'
    await refreshStory()
  } catch (e) {
    error.value = e.message
  } finally {
    sending.value = false
  }
}
</script>

<template>
  <div class="demo-page">
    <div class="play-shell">
      <div class="play-head">
        <h1>{{ story ? `以角色身份穿书 · ${story.title}` : '穿书工作台' }}</h1>
        <div class="demo-actions">
          <button class="demo-btn" type="button" @click="router.push('/story')">换书</button>
          <button class="demo-btn" type="button" @click="router.push(`/story/${id}/edit`)">编辑底本</button>
        </div>
      </div>
      <p class="demo-muted" style="margin: 0 0 12px">
        选左侧角色后，你就是该角色。输入言行或点推进；AI 只演世界与其他人物。
      </p>
      <p v-if="error" class="demo-banner error">{{ error }}</p>
      <p v-if="loading" class="demo-banner">加载中…</p>

      <div v-if="panel === 'play'" class="play-workspace">
        <aside class="play-aside">
          <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px">
            <p class="play-aside-label" style="margin: 0">角色名册</p>
            <button class="demo-btn" type="button" @click="openCreate">新建</button>
          </div>
          <button
            v-for="c in characters"
            :key="c.id"
            type="button"
            class="play-char"
            :class="{ active: !showCreate && c.id === selectedCharacterId }"
            @click="selectCharacter(c.id)"
          >
            <span class="play-avatar">{{ c.name.slice(0, 1) }}</span>
            <span>
              <span class="name">{{ c.name }}（{{ genderLabel[c.gender] || c.gender }}）</span>
              <span class="sub">{{ c.title || '以 TA 身份进入' }}{{ c.playerInsert ? ' · 原创' : '' }}</span>
            </span>
          </button>

          <div class="play-side-actions">
            <button class="demo-btn" type="button" @click="panel = 'theater'">AI 对戏</button>
            <button class="demo-btn" type="button" @click="openDivergences">偏离记录</button>
          </div>

          <div v-if="story?.world" class="play-world">
            <strong>当前世界</strong>
            <p>{{ story.world.currentTime }} · {{ story.world.currentPlace }}</p>
            <p v-if="story.world.presentCharacters">在场：{{ story.world.presentCharacters }}</p>
            <p>{{ story.world.summary }}</p>
          </div>
          <div v-if="story?.nodes?.length" class="play-world">
            <strong>原著节点</strong>
            <div v-for="n in story.nodes" :key="n.id || n.seqNo" style="margin-top: 8px">
              <span class="demo-muted">{{ statusLabel[n.status] || n.status }}</span>
              <p style="margin: 2px 0">{{ n.seqNo }}. {{ n.timeLabel }} / {{ n.place }}</p>
              <p style="margin: 0">{{ n.changedPlot || n.originalPlot }}</p>
            </div>
          </div>
        </aside>

        <div v-if="showCreate" class="play-main" style="padding: 16px">
          <h2 style="margin-top: 0">新建角色</h2>
          <form @submit.prevent="createCharacter">
            <label class="demo-field"><span>名字</span><input v-model="createForm.name" required /></label>
            <label class="demo-field"
              ><span>性别</span>
              <select v-model="createForm.gender">
                <option value="male">男</option>
                <option value="female">女</option>
                <option value="other">其他</option>
              </select>
            </label>
            <label class="demo-field"><span>身份</span><input v-model="createForm.title" /></label>
            <label class="demo-field"><span>设定</span><textarea v-model="createForm.setting" rows="2" /></label>
            <label class="demo-field"><span>人设</span><textarea v-model="createForm.personality" rows="2" /></label>
            <label class="demo-field" style="flex-direction: row; align-items: center; gap: 8px">
              <input v-model="createForm.playerInsert" type="checkbox" />
              <span>标记为原创插入角色</span>
            </label>
            <div class="demo-actions">
              <button class="demo-btn" type="button" @click="showCreate = false">取消</button>
              <button class="demo-btn primary" type="submit">创建</button>
            </div>
          </form>
        </div>

        <div v-else-if="selectedCharacter" class="play-main">
          <div class="play-chat-head">
            <div>
              <h2>你是 {{ selectedCharacter.name }}（{{ genderLabel[selectedCharacter.gender] }}）</h2>
              <p>{{ selectedCharacter.title }} · {{ selectedCharacter.setting }}</p>
            </div>
            <button class="demo-btn primary" type="button" :disabled="sending" @click="advance">推进剧情</button>
          </div>
          <div ref="listRef" class="play-messages">
            <div
              v-for="(m, i) in messages"
              :key="i"
              class="play-bubble-row"
              :class="m.role === 'user' ? 'user' : 'assistant'"
            >
              <div class="play-bubble">
                {{ m.content }}
                <p v-if="m.divergence" class="play-note">偏离：{{ m.divergence }}</p>
                <p v-if="m.worldSummary" class="play-note">世界：{{ m.worldSummary }}</p>
              </div>
            </div>
            <div v-if="sending" class="play-bubble-row assistant">
              <div class="play-bubble">世界正在回应…</div>
            </div>
          </div>
          <form class="play-composer" @submit.prevent="send">
            <textarea
              v-model="draft"
              rows="2"
              :placeholder="`以「${selectedCharacter.name}」的身份说或做…`"
              @keydown="onKeydown"
            />
            <button class="demo-btn primary" type="submit" :disabled="sending || !draft.trim()">发送</button>
          </form>
        </div>
      </div>

      <div v-else-if="panel === 'theater'" class="demo-card play-panel">
        <div class="play-head">
          <h1 style="font-size: 1.1rem">AI 对戏</h1>
          <button class="demo-btn" type="button" @click="panel = 'play'">返回穿书</button>
        </div>
        <p class="demo-muted">选择 2～6 个角色；插话视为当前所选角色的台词。</p>
        <div class="play-chips">
          <button
            v-for="c in characters"
            :key="c.id"
            type="button"
            class="play-chip"
            :class="{ on: theaterSelected.includes(c.id) }"
            @click="toggleTheaterChar(c.id)"
          >
            {{ c.name }}
          </button>
        </div>
        <label class="demo-field"
          ><span>玩家插话（可选）</span
          ><input v-model="theaterPlayerLine" placeholder="突然插入一句…"
        /></label>
        <button class="demo-btn primary" type="button" :disabled="sending" @click="runTheater">演一轮</button>
        <p v-if="theaterDivergence" class="play-note">偏离：{{ theaterDivergence }}</p>
        <div v-if="theaterLines.length" class="play-world">
          <div v-for="(line, i) in theaterLines" :key="i" style="margin-bottom: 10px">
            <strong>{{ line.characterName }}</strong>
            <p style="margin: 4px 0 0">{{ line.content }}</p>
          </div>
        </div>
      </div>

      <div v-else-if="panel === 'divergences'" class="demo-card play-panel">
        <div class="play-head">
          <h1 style="font-size: 1.1rem">偏离记录</h1>
          <button class="demo-btn" type="button" @click="panel = 'play'">返回</button>
        </div>
        <div v-for="d in divergences" :key="d.id" class="play-world" style="margin-bottom: 8px">
          <p v-if="d.originalText" class="demo-muted">本应：{{ d.originalText }}</p>
          <p>{{ d.newText }}</p>
          <span class="demo-muted">{{ d.createdAt }}</span>
        </div>
        <p v-if="!divergences.length" class="demo-muted">还没有偏离记录。试着点「推进剧情」。</p>
      </div>
    </div>
  </div>
</template>
