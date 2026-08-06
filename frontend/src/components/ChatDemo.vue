<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { api } from '../api/client.js'

defineProps({
  themeId: { type: String, required: true },
})

const step = ref('home') // home | edit | play | theater | divergences
const loading = ref(false)
const error = ref('')
const outlineEnabled = ref(true)

const storyBases = ref([])
const story = ref(null)
const characters = ref([])
const selectedCharacterId = ref(null)
const chatId = ref(null)
const messages = ref([])
const draft = ref('')
const sending = ref(false)
const listRef = ref(null)
const divergences = ref([])

const titleForm = reactive({ title: '', author: '' })
const editForm = reactive({
  title: '',
  author: '',
  background: '',
  nodes: [],
})
const createForm = reactive({
  name: '',
  gender: 'male',
  title: '',
  setting: '',
  personality: '',
  playerInsert: false,
})
const showCreate = ref(false)
const theaterSelected = ref([])
const theaterPlayerLine = ref('')
const theaterLines = ref([])

const selectedCharacter = computed(() =>
  characters.value.find((c) => c.id === selectedCharacterId.value),
)

const genderLabel = { male: '男', female: '女', other: '其他' }

onMounted(async () => {
  await refreshHome()
})

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
    openEdit(data)
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function openEdit(data) {
  story.value = data
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
  step.value = 'edit'
}

async function openExisting(id) {
  loading.value = true
  error.value = ''
  try {
    const data = await api.getStoryBase(id)
    if (data.status === 'CONFIRMED') {
      await enterPlay(data)
    } else {
      openEdit(data)
    }
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
    const data = story.value?.id
      ? await api.updateStoryBase(story.value.id, body)
      : await api.createStoryBase(body)
    story.value = data
    if (confirm) {
      await enterPlay(data)
    } else {
      error.value = ''
    }
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function enterPlay(data) {
  story.value = data
  characters.value = await api.listCharacters(data.id)
  selectedCharacterId.value = characters.value[0]?.id ?? null
  chatId.value = null
  messages.value = []
  showCreate.value = false
  theaterSelected.value = characters.value.slice(0, 2).map((c) => c.id)
  step.value = 'play'
  if (selectedCharacterId.value) {
    await ensureChat()
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
  createForm.playerInsert = false
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
  } catch (e) {
    error.value = e.message
    draft.value = text
  } finally {
    sending.value = false
  }
}

async function advance() {
  if (sending.value || !chatId.value) return
  sending.value = true
  error.value = ''
  try {
    messages.value = [
      ...messages.value,
      { role: 'user', content: '（推进剧情）' },
    ]
    const msg = await api.advance(chatId.value)
    messages.value = [...messages.value, msg]
    if (msg.divergence) {
      // refresh story world
      story.value = await api.getStoryBase(story.value.id)
    }
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
    step.value = 'divergences'
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
    theaterPlayerLine.value = ''
    step.value = 'theater'
  } catch (e) {
    error.value = e.message
  } finally {
    sending.value = false
  }
}

function backHome() {
  step.value = 'home'
  refreshHome()
}
</script>

<template>
  <section class="stage" :data-theme="themeId">
    <div class="atmosphere" aria-hidden="true" />

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
        <h1>选一本小说 → 生成剧情底本 → 创建角色进书里玩</h1>
        <p class="lead">
          非官方剧情底本，仅私人娱乐。穿书时 AI 知道原著节点与当前世界，推进可能改写大事件。
        </p>
      </div>
    </header>

    <p v-if="error" class="banner error">{{ error }}</p>
    <p v-if="loading" class="banner">加载中…</p>

    <!-- HOME -->
    <div v-if="step === 'home'" class="panel home">
      <div class="block">
        <h2>已有剧情底本</h2>
        <ul v-if="storyBases.length" class="base-list">
          <li v-for="b in storyBases" :key="b.id">
            <button type="button" @click="openExisting(b.id)">
              <strong>{{ b.title }}</strong>
              <span>{{ b.status === 'CONFIRMED' ? '已确认' : '草稿' }} · {{ b.author || '未知作者' }}</span>
            </button>
          </li>
        </ul>
        <p v-else class="muted">还没有底本。可从书名取纲，或使用启动时的示例《雨巷书店》。</p>
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

    <!-- EDIT -->
    <div v-else-if="step === 'edit'" class="panel">
      <div class="panel-head">
        <div>
          <h2>编辑剧情底本</h2>
          <p>请核对原著节点；不准的地方直接改。确认后才能建角穿书。</p>
        </div>
        <button type="button" class="ghost" @click="backHome">返回</button>
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
        </div>

        <div class="form-actions">
          <button type="button" class="ghost" :disabled="loading" @click="saveBase(false)">仅保存草稿</button>
          <button type="button" :disabled="loading" @click="saveBase(true)">确认底本并开始</button>
        </div>
      </div>
    </div>

    <!-- PLAY -->
    <div v-else-if="step === 'play'" class="workspace">
      <aside class="roster">
        <div class="aside-head">
          <p class="aside-label">{{ story?.title }}</p>
          <button type="button" class="new-btn" @click="openCreate">新建角色</button>
        </div>
        <ul>
          <li v-for="c in characters" :key="c.id">
            <button
              type="button"
              class="char"
              :class="{ active: !showCreate && c.id === selectedCharacterId }"
              @click="selectCharacter(c.id)"
            >
              <span class="avatar">{{ c.name.slice(0, 1) }}</span>
              <span class="meta">
                <span class="name-line">
                  <span class="name">{{ c.name }}</span>
                  <span class="gender">{{ genderLabel[c.gender] || c.gender }}</span>
                </span>
                <span class="setting">{{ c.title || '角色' }}{{ c.playerInsert ? ' · 代入' : '' }}</span>
              </span>
            </button>
          </li>
        </ul>
        <div class="side-actions">
          <button type="button" class="ghost" @click="runTheater">AI 对戏</button>
          <button type="button" class="ghost" @click="openDivergences">偏离记录</button>
          <button type="button" class="ghost" @click="backHome">换书</button>
        </div>
        <div v-if="story?.world" class="world-box">
          <strong>当前世界</strong>
          <p>{{ story.world.currentTime }} · {{ story.world.currentPlace }}</p>
          <p>{{ story.world.summary }}</p>
        </div>
      </aside>

      <div v-if="showCreate" class="chat">
        <div class="chat-head">
          <div>
            <h2>新建角色</h2>
            <p>可建配角或玩家代入角色。</p>
          </div>
        </div>
        <form class="create-form" @submit.prevent="createCharacter">
          <label><span>名字</span><input v-model="createForm.name" required /></label>
          <fieldset>
            <legend>性别</legend>
            <label class="radio"><input v-model="createForm.gender" type="radio" value="male" />男</label>
            <label class="radio"><input v-model="createForm.gender" type="radio" value="female" />女</label>
            <label class="radio"><input v-model="createForm.gender" type="radio" value="other" />其他</label>
          </fieldset>
          <label><span>身份</span><input v-model="createForm.title" /></label>
          <label><span>设定</span><textarea v-model="createForm.setting" rows="2" /></label>
          <label><span>人设</span><textarea v-model="createForm.personality" rows="2" /></label>
          <label class="radio">
            <input v-model="createForm.playerInsert" type="checkbox" />
            这是我的代入角色
          </label>
          <div class="form-actions">
            <button type="button" class="ghost" @click="showCreate = false">取消</button>
            <button type="submit">创建</button>
          </div>
        </form>
      </div>

      <div v-else-if="selectedCharacter" class="chat">
        <div class="chat-head">
          <div>
            <h2>
              {{ selectedCharacter.name }}
              <span class="gender-tag">{{ genderLabel[selectedCharacter.gender] }}</span>
            </h2>
            <p>{{ selectedCharacter.title }} · {{ selectedCharacter.setting }}</p>
          </div>
          <button type="button" class="story-btn" :disabled="sending" @click="advance">推进剧情</button>
        </div>
        <div v-if="selectedCharacter.personality" class="persona">
          <strong>人设</strong>
          <span>{{ selectedCharacter.personality }}</span>
        </div>
        <div ref="listRef" class="messages">
          <div v-for="(m, i) in messages" :key="i" class="row" :class="m.role">
            <div class="bubble">
              {{ m.content }}
              <p v-if="m.divergence" class="div-note">偏离：{{ m.divergence }}</p>
            </div>
          </div>
          <div v-if="sending" class="row assistant"><div class="bubble typing">对方正在回应…</div></div>
        </div>
        <form class="composer" @submit.prevent="send">
          <textarea v-model="draft" rows="2" placeholder="对他说点什么…" @keydown="onKeydown" />
          <button type="submit" :disabled="sending || !draft.trim()">发送</button>
        </form>
      </div>
    </div>

    <!-- THEATER -->
    <div v-else-if="step === 'theater'" class="panel">
      <div class="panel-head">
        <div>
          <h2>AI 对戏</h2>
          <p>多角色轮流发言，像在看戏。可选填一句玩家插话。</p>
        </div>
        <button type="button" class="ghost" @click="step = 'play'">返回穿书</button>
      </div>
      <div class="edit-body">
        <p class="muted">选择 2～6 个角色</p>
        <div class="chips">
          <button
            v-for="c in characters"
            :key="c.id"
            type="button"
            class="chip"
            :class="{ on: theaterSelected.includes(c.id) }"
            @click="toggleTheaterChar(c.id)"
          >
            {{ c.name }}
          </button>
        </div>
        <label>
          <span>玩家插话（可选）</span>
          <input v-model="theaterPlayerLine" placeholder="突然插入一句…" />
        </label>
        <button type="button" :disabled="sending" @click="runTheater">再演一轮</button>
        <div class="theater-log">
          <div v-for="(line, i) in theaterLines" :key="i" class="theater-line">
            <strong>{{ line.characterName }}</strong>
            <p>{{ line.content }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- DIVERGENCES -->
    <div v-else-if="step === 'divergences'" class="panel">
      <div class="panel-head">
        <div>
          <h2>偏离记录</h2>
          <p>原著本应发生的事，被穿书改写成了什么。</p>
        </div>
        <button type="button" class="ghost" @click="step = 'play'">返回</button>
      </div>
      <ul class="div-list">
        <li v-for="d in divergences" :key="d.id">
          <p>{{ d.newText }}</p>
          <span class="muted">{{ d.createdAt }}</span>
        </li>
        <li v-if="!divergences.length" class="muted">还没有偏离记录。试着点「推进剧情」。</li>
      </ul>
    </div>
  </section>
</template>

<style scoped>
.stage {
  position: relative;
  min-height: 100%;
  padding: 28px 20px 36px;
  overflow: hidden;
  color: var(--text);
  font-family: var(--font-body);
  background: var(--bg);
}
.atmosphere {
  position: absolute;
  inset: 0;
  background: var(--atmosphere);
  pointer-events: none;
  z-index: 0;
}
.top,
.panel,
.workspace,
.banner {
  position: relative;
  z-index: 1;
  max-width: 1080px;
  margin: 0 auto;
}
.brand-block {
  margin-bottom: 18px;
  padding-right: 56px;
}
.logo-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.mark {
  width: 36px;
  height: 36px;
  color: var(--brand);
}
.mark svg {
  width: 100%;
  height: 100%;
  display: block;
}
.brand {
  margin: 0;
  font-family: var(--font-brand);
  font-size: clamp(2rem, 5vw, 3rem);
  font-weight: 650;
  letter-spacing: 0.12em;
  color: var(--brand);
}
h1 {
  margin: 0 0 8px;
  font-size: 1.1rem;
  font-weight: 500;
}
.lead,
.muted {
  color: var(--text-muted);
  font-size: 0.92rem;
  line-height: 1.5;
}
.banner {
  margin-bottom: 12px;
  padding: 10px 14px;
  border-radius: 10px;
  background: var(--accent-soft);
  border: 1px solid var(--line);
}
.banner.error {
  color: #8b2e2e;
}
.panel,
.roster,
.chat {
  background: var(--bg-elevated);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
}
.home {
  display: grid;
  gap: 16px;
  padding: 18px;
}
.block h2,
.panel-head h2,
.chat-head h2 {
  margin: 0 0 6px;
  font-size: 1.05rem;
}
.base-list {
  list-style: none;
  padding: 0;
  margin: 10px 0 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.base-list button {
  width: 100%;
  text-align: left;
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 12px;
  border-radius: 12px;
  border: 1px solid var(--line);
  background: var(--bg-panel);
  color: inherit;
  cursor: pointer;
}
.row-form {
  display: grid;
  grid-template-columns: 1.2fr 1fr auto;
  gap: 8px;
  margin-top: 10px;
}
.panel-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 18px;
  border-bottom: 1px solid var(--line);
}
.edit-body,
.create-form {
  padding: 16px 18px 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 0.85rem;
}
label > span {
  color: var(--text-muted);
}
input,
textarea {
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 10px 12px;
  background: color-mix(in srgb, var(--bg) 35%, transparent);
  color: var(--text);
}
.nodes-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.nodes-head h3 {
  margin: 0;
  font-size: 0.95rem;
}
.node-card {
  border: 1px solid var(--line);
  border-radius: 12px;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.node-row {
  display: grid;
  grid-template-columns: 1fr 1fr auto;
  gap: 8px;
}
.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
button {
  border: none;
  border-radius: 10px;
  padding: 8px 14px;
  background: var(--accent);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
}
[data-theme='yehang'] button:not(.ghost):not(.chip):not(.char):not(.new-btn) {
  color: #06201d;
}
.ghost {
  background: transparent;
  border: 1px solid var(--line);
  color: var(--text-muted);
}
.ghost.danger {
  color: #8b2e2e;
}
.workspace {
  display: grid;
  grid-template-columns: 270px minmax(0, 1fr);
  gap: 16px;
  min-height: min(72vh, 680px);
}
.roster {
  padding: 14px 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: var(--bg-panel);
}
.aside-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}
.aside-label {
  margin: 0;
  font-size: 0.8rem;
  color: var(--text-muted);
}
.new-btn {
  border: 1px solid var(--line);
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 0.8rem;
  padding: 4px 10px;
}
.roster ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  overflow: auto;
}
.char {
  width: 100%;
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 10px;
  border: 1px solid transparent;
  border-radius: 12px;
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
}
.char.active {
  background: var(--accent-soft);
  border-color: var(--line);
}
.avatar {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  background: var(--accent-soft);
  color: var(--accent);
  font-weight: 600;
}
.meta {
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.name-line {
  display: flex;
  gap: 6px;
  align-items: center;
}
.name {
  font-weight: 600;
}
.gender,
.gender-tag {
  font-size: 0.68rem;
  padding: 1px 6px;
  border: 1px solid var(--line);
  border-radius: 4px;
  color: var(--text-muted);
}
.setting {
  font-size: 0.78rem;
  color: var(--text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.side-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.world-box {
  margin-top: auto;
  padding: 10px;
  border-radius: 10px;
  background: var(--accent-soft);
  font-size: 0.8rem;
  color: var(--text-muted);
}
.world-box p {
  margin: 4px 0 0;
}
.chat {
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.chat-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 18px;
  border-bottom: 1px solid var(--line);
}
.chat-head p {
  margin: 0;
  color: var(--text-muted);
  font-size: 0.85rem;
}
.story-btn {
  align-self: flex-start;
}
.persona {
  display: flex;
  gap: 8px;
  padding: 10px 18px;
  border-bottom: 1px solid var(--line);
  background: var(--accent-soft);
  font-size: 0.82rem;
  color: var(--text-muted);
}
.messages {
  flex: 1;
  overflow: auto;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.row {
  display: flex;
}
.row.user {
  justify-content: flex-end;
}
.bubble {
  max-width: min(78%, 520px);
  padding: 10px 14px;
  border-radius: 14px;
  line-height: 1.55;
  white-space: pre-wrap;
}
.row.user .bubble {
  background: var(--user-bubble);
  color: var(--user-text);
}
.row.assistant .bubble {
  background: var(--bot-bubble);
  border: 1px solid var(--line);
}
.div-note {
  margin: 8px 0 0;
  font-size: 0.78rem;
  opacity: 0.85;
}
.typing {
  font-style: italic;
  color: var(--text-muted);
}
.composer {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  padding: 14px 16px;
  border-top: 1px solid var(--line);
}
.composer button {
  align-self: end;
  height: 44px;
  min-width: 88px;
}
fieldset {
  border: none;
  margin: 0;
  padding: 0;
  display: flex;
  gap: 14px;
  align-items: center;
}
legend {
  width: 100%;
  margin-bottom: 6px;
  color: var(--text-muted);
  font-size: 0.85rem;
}
.radio {
  flex-direction: row;
  align-items: center;
  gap: 6px;
}
.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.chip {
  background: var(--bg-panel);
  border: 1px solid var(--line);
  color: var(--text);
}
.chip.on {
  background: var(--accent-soft);
  border-color: var(--accent);
  color: var(--accent);
}
.theater-log {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 8px;
}
.theater-line {
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: var(--bg-panel);
}
.theater-line p {
  margin: 6px 0 0;
  line-height: 1.5;
}
.div-list {
  list-style: none;
  margin: 0;
  padding: 16px 18px 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.div-list li {
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 12px;
}
.div-list p {
  margin: 0 0 6px;
}
@media (max-width: 820px) {
  .workspace {
    grid-template-columns: 1fr;
  }
  .row-form {
    grid-template-columns: 1fr;
  }
}
</style>
