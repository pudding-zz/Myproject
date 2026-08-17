<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api/client.js'
import '../styles/roleplay.css'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const sending = ref(false)
const error = ref('')
const session = ref(null)
const messages = ref([])
const sessions = ref([])
const draft = ref('')
const chatLog = ref(null)

const form = reactive({
  aiName: '沈清野',
  aiGender: '男',
  aiTitle: '冷感外科医生',
  aiPersonality: '克制、毒舌、护短；不擅长直白表达感情',
  aiRelation: '你们在同一家医院相遇，他对你总多看一眼',
  playerName: '林念',
  playerGender: '女',
  playerTitle: '实习规培生',
  playerPersonality: '认真、偶尔莽撞、对他好奇',
  playerRelation: '今晚值班又被他撞见',
  scene: '医院值班室门口，夜班灯管嗡嗡作响。',
})

const aiInitial = computed(() => (session.value?.aiName || form.aiName || 'AI').slice(0, 1))
const playerInitial = computed(() => (session.value?.playerName || form.playerName || '我').slice(0, 1))
const placeholder = computed(
  () => `以「${session.value?.playerName || form.playerName}」的身份回复…`,
)

onMounted(async () => {
  await refreshSessionList()
  const id = route.params.id
  if (id) {
    await openSession(Number(id))
  }
})

watch(
  () => route.params.id,
  async (id) => {
    if (id) await openSession(Number(id))
  },
)

async function refreshSessionList() {
  try {
    sessions.value = await api.listRoleplaySessions()
  } catch {
    sessions.value = []
  }
}

async function startSession() {
  if (!form.aiName.trim() || !form.playerName.trim()) {
    error.value = '请至少填写 AI 名字与玩家名字'
    return
  }
  error.value = ''
  loading.value = true
  try {
    const data = await api.createRoleplaySession({
      aiName: form.aiName.trim(),
      aiGender: form.aiGender.trim() || null,
      aiTitle: form.aiTitle.trim() || null,
      aiPersonality: form.aiPersonality.trim() || null,
      aiRelation: form.aiRelation.trim() || null,
      playerName: form.playerName.trim(),
      playerGender: form.playerGender.trim() || null,
      playerTitle: form.playerTitle.trim() || null,
      playerPersonality: form.playerPersonality.trim() || null,
      playerRelation: form.playerRelation.trim() || null,
      scene: form.scene.trim() || null,
      openingLine: true,
    })
    session.value = data
    messages.value = await api.listRoleplayMessages(data.id)
    await refreshSessionList()
    await router.replace(`/roleplay/${data.id}`)
    await scrollBottom()
  } catch (e) {
    error.value = e.message || '创建会话失败'
  } finally {
    loading.value = false
  }
}

async function openSession(id) {
  if (!id) return
  error.value = ''
  loading.value = true
  try {
    session.value = await api.getRoleplaySession(id)
    messages.value = await api.listRoleplayMessages(id)
    fillFormFromSession(session.value)
    await scrollBottom()
  } catch (e) {
    error.value = e.message || '加载会话失败'
  } finally {
    loading.value = false
  }
}

function fillFormFromSession(s) {
  if (!s) return
  form.aiName = s.aiName || ''
  form.aiGender = s.aiGender || ''
  form.aiTitle = s.aiTitle || ''
  form.aiPersonality = s.aiPersonality || ''
  form.aiRelation = s.aiRelation || ''
  form.playerName = s.playerName || ''
  form.playerGender = s.playerGender || ''
  form.playerTitle = s.playerTitle || ''
  form.playerPersonality = s.playerPersonality || ''
  form.playerRelation = s.playerRelation || ''
  form.scene = s.scene || ''
}

function resetToNew() {
  session.value = null
  messages.value = []
  draft.value = ''
  error.value = ''
  router.replace('/roleplay')
}

async function send() {
  const text = draft.value.trim()
  if (!text || !session.value || sending.value) return
  sending.value = true
  error.value = ''
  messages.value.push({
    id: `tmp-${Date.now()}`,
    role: 'user',
    content: text,
  })
  draft.value = ''
  await scrollBottom()
  try {
    const reply = await api.sendRoleplayMessage(session.value.id, text)
    messages.value = await api.listRoleplayMessages(session.value.id)
    if (reply) await scrollBottom()
  } catch (e) {
    error.value = e.message || '发送失败'
    messages.value = await api.listRoleplayMessages(session.value.id).catch(() => messages.value)
  } finally {
    sending.value = false
  }
}

async function scrollBottom() {
  await nextTick()
  if (chatLog.value) chatLog.value.scrollTop = chatLog.value.scrollHeight
}

function onKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    send()
  }
}
</script>

<template>
  <div class="rp-page">
    <nav class="rp-nav">
      <router-link class="rp-brand" to="/">望月</router-link>
      <div>
        <router-link to="/">首页</router-link>
        <router-link to="/">穿书</router-link>
        <router-link class="active" to="/roleplay">AI角色对话</router-link>
      </div>
    </nav>

    <div class="rp-main">
      <header class="rp-hero">
        <h1>AI 角色对话</h1>
        <p>给 AI 一个完整身份，也给自己一个人设，AI 会以设定回应你。</p>
      </header>

      <div v-if="error" class="rp-error">{{ error }}</div>

      <div class="rp-grid">
        <article class="rp-card">
          <div class="rp-card-head">
            <div class="rp-avatar ai">{{ aiInitial }}</div>
            <h2>AI 身份</h2>
          </div>
          <div class="rp-field"><label>名字</label><input v-model="form.aiName" :disabled="!!session" /></div>
          <div class="rp-field"><label>性别</label><input v-model="form.aiGender" :disabled="!!session" /></div>
          <div class="rp-field"><label>身份</label><input v-model="form.aiTitle" :disabled="!!session" /></div>
          <div class="rp-field"><label>性格</label><textarea v-model="form.aiPersonality" :disabled="!!session" /></div>
          <div class="rp-field"><label>关系</label><textarea v-model="form.aiRelation" :disabled="!!session" /></div>
        </article>

        <article class="rp-card">
          <div class="rp-card-head">
            <div class="rp-avatar player">{{ playerInitial }}</div>
            <h2>玩家身份</h2>
          </div>
          <div class="rp-field"><label>名字</label><input v-model="form.playerName" :disabled="!!session" /></div>
          <div class="rp-field"><label>性别</label><input v-model="form.playerGender" :disabled="!!session" /></div>
          <div class="rp-field"><label>身份</label><input v-model="form.playerTitle" :disabled="!!session" /></div>
          <div class="rp-field"><label>性格</label><textarea v-model="form.playerPersonality" :disabled="!!session" /></div>
          <div class="rp-field"><label>关系</label><textarea v-model="form.playerRelation" :disabled="!!session" /></div>
        </article>
      </div>

      <div class="rp-scene rp-card" style="margin-top: 16px">
        <label>开场场景（可选）</label>
        <textarea v-model="form.scene" :disabled="!!session" placeholder="交代时间地点与氛围…" />
      </div>

      <div class="rp-actions">
        <button
          v-if="!session"
          class="rp-btn primary"
          type="button"
          :disabled="loading"
          @click="startSession"
        >
          {{ loading ? '创建中…' : '开始以设定身份对话' }}
        </button>
        <template v-else>
          <button class="rp-btn" type="button" @click="resetToNew">新建会话</button>
          <span class="rp-session-item meta" style="border: none; background: transparent; cursor: default">
            当前：{{ session.title || `${session.aiName} × ${session.playerName}` }}
          </span>
        </template>
      </div>

      <section v-if="session" class="rp-chat">
        <div ref="chatLog" class="rp-chat-log">
          <div
            v-for="m in messages"
            :key="m.id"
            class="rp-msg"
            :class="m.role === 'user' ? 'user' : 'assistant'"
          >
            <div
              class="rp-avatar"
              :class="m.role === 'user' ? 'player' : 'ai'"
              style="width: 36px; height: 36px; font-size: 0.85rem"
            >
              {{ m.role === 'user' ? playerInitial : aiInitial }}
            </div>
            <div>
              <div v-if="m.role !== 'user'" class="rp-msg-name">{{ session.aiName }}</div>
              <div class="rp-bubble">{{ m.content }}</div>
            </div>
          </div>
          <div v-if="!messages.length && !loading" class="rp-msg-name" style="align-self: center">
            暂无消息
          </div>
        </div>
        <div class="rp-composer">
          <input
            v-model="draft"
            type="text"
            :placeholder="placeholder"
            :disabled="sending"
            @keydown="onKeydown"
          />
          <button class="rp-btn primary" type="button" :disabled="sending || !draft.trim()" @click="send">
            {{ sending ? '…' : '发送' }}
          </button>
        </div>
      </section>

      <div class="rp-ext">
        <details>
          <summary class="rose">生理记录 <span style="font-weight: 400; font-size: 0.8rem; opacity: 0.75">即将开放</span></summary>
          <div class="body">
            日历式亲密/生理追踪、诊断报告等将在后续版本接入。接口已预留
            <code>GET /api/roleplay/sessions/{id}/health</code>。
          </div>
        </details>
        <details>
          <summary class="champ">角色状态 <span style="font-weight: 400; font-size: 0.8rem; opacity: 0.75">即将开放</span></summary>
          <div class="body">
            房子、宠物、亲属、伴侣、好感度等状态快照将随对话更新。接口已预留
            <code>GET /api/roleplay/sessions/{id}/status</code>。
          </div>
        </details>
      </div>

      <section v-if="sessions.length" class="rp-sessions">
        <h3>最近会话</h3>
        <button
          v-for="s in sessions"
          :key="s.id"
          type="button"
          class="rp-session-item"
          @click="router.push(`/roleplay/${s.id}`)"
        >
          <span>{{ s.title || `${s.aiName} × ${s.playerName}` }}</span>
          <span class="meta">#{{ s.id }}</span>
        </button>
      </section>
    </div>
  </div>
</template>
