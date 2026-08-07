<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api/client.js'
import AppBrandHeader from '../components/AppBrandHeader.vue'

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
  <section class="stage">
    <div class="atmosphere" aria-hidden="true" />
    <AppBrandHeader
      :title="story ? `以角色身份穿书 · ${story.title}` : '穿书工作台'"
      lead="选左侧角色后，你就是该角色。输入言行或点推进；AI 只演世界与其他人物。"
    />
    <p v-if="error" class="banner error">{{ error }}</p>
    <p v-if="loading" class="banner">加载中…</p>

    <div v-if="panel === 'play'" class="workspace">
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
                <span class="setting">以 TA 身份进入{{ c.playerInsert ? ' · 原创' : '' }}</span>
              </span>
            </button>
          </li>
        </ul>
        <div class="side-actions">
          <button type="button" class="ghost" @click="panel = 'theater'">AI 对戏</button>
          <button type="button" class="ghost" @click="openDivergences">偏离记录</button>
          <button type="button" class="ghost" @click="router.push(`/story/${id}/edit`)">编辑底本</button>
          <button type="button" class="ghost" @click="router.push('/')">换书</button>
        </div>
        <div v-if="story?.world" class="world-box">
          <strong>当前世界</strong>
          <p>{{ story.world.currentTime }} · {{ story.world.currentPlace }}</p>
          <p v-if="story.world.presentCharacters">在场：{{ story.world.presentCharacters }}</p>
          <p>{{ story.world.summary }}</p>
        </div>
        <div v-if="story?.nodes?.length" class="nodes-box">
          <strong>原著节点</strong>
          <ul>
            <li v-for="n in story.nodes" :key="n.id || n.seqNo">
              <span class="status-tag" :class="n.status">{{ statusLabel[n.status] || n.status }}</span>
              <p>{{ n.seqNo }}. {{ n.timeLabel }} / {{ n.place }}</p>
              <p>{{ n.changedPlot || n.originalPlot }}</p>
            </li>
          </ul>
        </div>
      </aside>

      <div v-if="showCreate" class="chat">
        <div class="chat-head">
          <div>
            <h2>新建角色</h2>
            <p>创建后可选中，以该身份参与剧情。</p>
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
            标记为原创插入角色
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
              你是 {{ selectedCharacter.name }}
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
              <p v-if="m.worldSummary" class="div-note">世界：{{ m.worldSummary }}</p>
            </div>
          </div>
          <div v-if="sending" class="row assistant">
            <div class="bubble typing">世界正在回应…</div>
          </div>
        </div>
        <form class="composer" @submit.prevent="send">
          <textarea
            v-model="draft"
            rows="2"
            :placeholder="`以「${selectedCharacter.name}」的身份说或做…`"
            @keydown="onKeydown"
          />
          <button type="submit" :disabled="sending || !draft.trim()">发送</button>
        </form>
      </div>
    </div>

    <div v-else-if="panel === 'theater'" class="panel">
      <div class="panel-head">
        <div>
          <h2>AI 对戏</h2>
          <p class="muted">多角色轮流发言；结束后会结算偏离与当前世界。插话视为你当前视角角色的台词。</p>
        </div>
        <button type="button" class="ghost" @click="panel = 'play'">返回穿书</button>
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
          <span>玩家插话（可选，以当前所选角色视角）</span>
          <input v-model="theaterPlayerLine" placeholder="突然插入一句…" />
        </label>
        <button type="button" :disabled="sending" @click="runTheater">演一轮</button>
        <p v-if="theaterDivergence" class="div-note">偏离：{{ theaterDivergence }}</p>
        <div v-if="story?.world" class="world-box">
          <strong>当前世界</strong>
          <p>{{ story.world.currentTime }} · {{ story.world.currentPlace }}</p>
          <p>{{ story.world.summary }}</p>
        </div>
        <div class="theater-log">
          <div v-for="(line, i) in theaterLines" :key="i" class="theater-line">
            <strong>{{ line.characterName }}</strong>
            <p>{{ line.content }}</p>
          </div>
        </div>
      </div>
    </div>

    <div v-else-if="panel === 'divergences'" class="panel">
      <div class="panel-head">
        <div>
          <h2>偏离记录</h2>
          <p class="muted">原著本应发生的事，被穿书改写成了什么。</p>
        </div>
        <button type="button" class="ghost" @click="panel = 'play'">返回</button>
      </div>
      <ul class="div-list">
        <li v-for="d in divergences" :key="d.id">
          <p v-if="d.originalText" class="muted">本应：{{ d.originalText }}</p>
          <p>{{ d.newText }}</p>
          <span class="muted">{{ d.createdAt }}</span>
        </li>
        <li v-if="!divergences.length" class="muted">还没有偏离记录。试着点「推进剧情」。</li>
      </ul>
    </div>
  </section>
</template>
