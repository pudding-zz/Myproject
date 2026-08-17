<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="demo-page">
    <div class="demo-container">
      <header class="demo-hero">
        <h1>穿书 · 对话</h1>
        <p>选一个原著世界，认领角色身份；或创造一段只属于你的 AI 角色关系。</p>
      </header>

      <section class="demo-grid-2">
        <article class="demo-card">
          <div class="demo-icon">书</div>
          <h2>穿书入口</h2>
          <p class="desc">进入已有世界，扮演原著角色，与 AI 共演剧情。</p>
          <div style="margin-top: 20px">
            <button class="demo-btn primary" type="button" @click="router.push('/story')">
              进入穿书
            </button>
          </div>
        </article>

        <article class="demo-card">
          <div class="demo-icon">言</div>
          <h2>AI角色对话</h2>
          <p class="desc">自定义 AI 身份与玩家身份，开启专属角色关系。</p>
          <div style="margin-top: 20px">
            <button class="demo-btn primary" type="button" @click="router.push('/roleplay')">
              进入对话
            </button>
          </div>
        </article>
      </section>

      <footer class="demo-footer-strip">
        <p>AI 知道你是谁</p>
        <p>身份决定语气</p>
      </footer>
    </div>
  </div>
</template>
