/**
 * 前端 API 封装。后端 context-path=/api，Vite 已代理 /api -> :8080
 */
async function request(path, options = {}) {
  const res = await fetch(`/api${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {}),
    },
    ...options,
  })
  const json = await res.json().catch(() => ({}))
  if (!res.ok || (json.code !== undefined && json.code !== 0)) {
    const message = json.message || res.statusText || '请求失败'
    throw new Error(message)
  }
  return json.data
}

export const api = {
  settings: () => request('/story-bases/settings'),
  listStoryBases: () => request('/story-bases'),
  getStoryBase: (id) => request(`/story-bases/${id}`),
  fromTitle: (body) =>
    request('/story-bases/from-title', { method: 'POST', body: JSON.stringify(body) }),
  createStoryBase: (body) =>
    request('/story-bases', { method: 'POST', body: JSON.stringify(body) }),
  updateStoryBase: (id, body) =>
    request(`/story-bases/${id}`, { method: 'PUT', body: JSON.stringify(body) }),
  listDivergences: (id) => request(`/story-bases/${id}/divergences`),
  theaterRound: (id, body) =>
    request(`/story-bases/${id}/theater/round`, {
      method: 'POST',
      body: JSON.stringify(body),
    }),

  listCharacters: (storyBaseId) =>
    request(`/characters?storyBaseId=${encodeURIComponent(storyBaseId)}`),
  createCharacter: (body) =>
    request('/characters', { method: 'POST', body: JSON.stringify(body) }),
  updateCharacter: (id, body) =>
    request(`/characters/${id}`, { method: 'PUT', body: JSON.stringify(body) }),

  createChat: (body) =>
    request('/chats', { method: 'POST', body: JSON.stringify(body) }),
  listMessages: (chatId) => request(`/chats/${chatId}/messages`),
  sendMessage: (chatId, content) =>
    request(`/chats/${chatId}/messages`, {
      method: 'POST',
      body: JSON.stringify({ content }),
    }),
  advance: (chatId, content) =>
    request(`/chats/${chatId}/advance`, {
      method: 'POST',
      body: JSON.stringify({ content: content || '（推进剧情）' }),
    }),

  listRoleplaySessions: () => request('/roleplay/sessions'),
  createRoleplaySession: (body) =>
    request('/roleplay/sessions', { method: 'POST', body: JSON.stringify(body) }),
  getRoleplaySession: (id) => request(`/roleplay/sessions/${id}`),
  listRoleplayMessages: (id) => request(`/roleplay/sessions/${id}/messages`),
  sendRoleplayMessage: (id, content) =>
    request(`/roleplay/sessions/${id}/messages`, {
      method: 'POST',
      body: JSON.stringify({ content }),
    }),
  getRoleplayStatus: (id) => request(`/roleplay/sessions/${id}/status`),
  putRoleplayStatus: (id, body) =>
    request(`/roleplay/sessions/${id}/status`, {
      method: 'PUT',
      body: JSON.stringify(body),
    }),
  getRoleplayHealth: (id) => request(`/roleplay/sessions/${id}/health`),
  putRoleplayHealth: (id, body) =>
    request(`/roleplay/sessions/${id}/health`, {
      method: 'PUT',
      body: JSON.stringify(body),
    }),
}
