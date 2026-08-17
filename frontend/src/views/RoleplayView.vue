<script setup>
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api/client.js'

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
  if (id) await openSession(Number(id))
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
  messages.value.push({ id: `tmp-${Date.now()}`, role: 'user', content: text })
  draft.value = ''
  await scrollBottom()
  try {
    await api.sendRoleplayMessage(session.value.id, text)
    messages.value = await api.listRoleplayMessages(session.value.id)
    await scrollBottom()
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
  <div class="demo-page">
    <div class="demo-container">
      <header class="demo-hero">
        <h1>AI 角色对话</h1>
        <p>给 AI 一个完整身份，也给自己一个人设，AI 会以设定回应你。</p>
      </header>

      <p v-if="error" class="demo-banner error">{{ error }}</p>

      <section class="demo-grid-2">
        <article class="demo-card">
          <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 14px">
            <div class="play-avatar" style="width: 40px; height: 40px">{{ aiInitial }}</div>
            <h2 style="margin: 0">AI 身份</h2>
          </div>
          <label class="demo-field"><span>名字</span><input v-model="form.aiName" :disabled="!!session" /></label>
          <label class="demo-field"><span>性别</span><input v-model="form.aiGender" :disabled="!!session" /></label>
          <label class="demo-field"><span>身份</span><input v-model="form.aiTitle" :disabled="!!session" /></label>
          <label class="demo-field"
            ><span>性格</span><textarea v-model="form.aiPersonality" :disabled="!!session"
          /></label>
          <label class="demo-field"
            ><span>关系</span><textarea v-model="form.aiRelation" :disabled="!!session"
          /></label>
        </article>

        <article class="demo-card">
          <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 14px">
            <div class="play-avatar muted" style="width: 40px; height: 40px">{{ playerInitial }}</div>
            <h2 style="margin: 0">玩家身份</h2>
          </div>
          <label class="demo-field"
            ><span>名字</span><input v-model="form.playerName" :disabled="!!session"
          /></label>
          <label class="demo-field"
            ><span>性别</span><input v-model="form.playerGender" :disabled="!!session"
          /></label>
          <label class="demo-field"
            ><span>身份</span><input v-model="form.playerTitle" :disabled="!!session"
          /></label>
          <label class="demo-field"
            ><span>性格</span><textarea v-model="form.playerPersonality" :disabled="!!session"
          /></label>
          <label class="demo-field"
            ><span>关系</span><textarea v-model="form.playerRelation" :disabled="!!session"
          /></label>
        </article>
      </section>

      <div class="demo-card" style="margin-top: 16px">
        <label class="demo-field" style="margin-bottom: 0"
          ><span>开场场景（可选）</span
          ><textarea v-model="form.scene" :disabled="!!session" placeholder="交代时间地点与氛围…"
        /></label>
      </div>

      <div class="demo-actions" style="justify-content: center; margin: 22px 0">
        <button
          v-if="!session"
          class="demo-btn primary"
          type="button"
          :disabled="loading"
          @click="startSession"
        >
          {{ loading ? '创建中…' : '开始以设定身份对话' }}
        </button>
        <template v-else>
          <button class="demo-btn" type="button" @click="resetToNew">新建会话</button>
          <span class="demo-muted">当前：{{ session.title || `${session.aiName} × ${session.playerName}` }}</span>
        </template>
      </div>

      <section v-if="session" class="demo-card" style="padding: 0; overflow: hidden">
        <div ref="chatLog" class="rp-log">
          <div
            v-for="m in messages"
            :key="m.id"
            class="rp-row"
            :class="m.role === 'user' ? 'user' : 'assistant'"
          >
            <div class="play-avatar" :class="{ muted: m.role === 'user' }">
              {{ m.role === 'user' ? playerInitial : aiInitial }}
            </div>
            <div>
              <p v-if="m.role !== 'user'" class="demo-muted" style="margin: 0 0 4px; font-size: 0.75rem">
                {{ session.aiName }}
              </p>
              <div class="rp-bubble">{{ m.content }}</div>
            </div>
          </div>
        </div>
        <div class="rp-composer">
          <input v-model="draft" type="text" :placeholder="placeholder" :disabled="sending" @keydown="onKeydown" />
          <button class="demo-btn primary" type="button" :disabled="sending || !draft.trim()" @click="send">
            {{ sending ? '…' : '发送' }}
          </button>
        </div>
      </section>

      <section class="demo-grid-2" style="margin-top: 20px">
        <details class="demo-card">
          <summary style="cursor: pointer; font-weight: 600; color: #9a4b5c">
            生理记录 · 即将开放
          </summary>
          <p class="demo-muted" style="margin-top: 10px">
            日历式亲密/生理追踪将在后续版本接入。接口预留
            <code>GET /api/roleplay/sessions/{id}/health</code>。
          </p>
        </details>
        <details class="demo-card">
          <summary style="cursor: pointer; font-weight: 600; color: #8a6d4b">
            角色状态 · 即将开放
          </summary>
          <p class="demo-muted" style="margin-top: 10px">
            房子、宠物、亲属、伴侣、好感度等将随对话更新。接口预留
            <code>GET /api/roleplay/sessions/{id}/status</code>。
          </p>
        </details>
      </section>

      <section v-if="sessions.length" style="margin-top: 24px">
        <h2 class="demo-section-title" style="margin-top: 0">最近会话</h2>
        <button
          v-for="s in sessions"
          :key="s.id"
          type="button"
          class="demo-list-btn"
          @click="router.push(`/roleplay/${s.id}`)"
        >
          <strong>{{ s.title || `${s.aiName} × ${s.playerName}` }}</strong>
          <span>#{{ s.id }}</span>
        </button>
      </section>
    </div>
  </div>
</template>

<style scoped>
.rp-log {
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  max-height: 420px;
  overflow: auto;
  background: #fafafa;
}
.rp-row {
  display: flex;
  gap: 10px;
  max-width: 88%;
}
.rp-row.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}
.rp-bubble {
  border-radius: 16px;
  padding: 10px 14px;
  font-size: 0.92rem;
  line-height: 1.55;
  white-space: pre-wrap;
}
.rp-row.assistant .rp-bubble {
  background: #fff;
  border: 1px solid var(--demo-border);
  border-top-left-radius: 4px;
}
.rp-row.user .rp-bubble {
  background: var(--demo-brand-soft);
  border: 1px solid #c5ddd2;
  border-top-right-radius: 4px;
}
.rp-composer {
  display: flex;
  gap: 10px;
  padding: 12px 14px;
  border-top: 1px solid var(--demo-border);
  background: #fff;
}
.rp-composer input {
  flex: 1;
  border: 1px solid var(--demo-border);
  border-radius: 10px;
  padding: 10px 12px;
}
.play-avatar {
  width: 36px;
  height: 36px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  background: var(--demo-brand);
  color: #fff;
  font-size: 0.85rem;
  font-weight: 600;
  flex-shrink: 0;
}
.play-avatar.muted {
  background: var(--demo-secondary);
  color: var(--demo-text);
}
</style>
