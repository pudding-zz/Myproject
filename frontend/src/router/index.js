import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import StoryEditView from '../views/StoryEditView.vue'
import PlayView from '../views/PlayView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/story/:id/edit', name: 'story-edit', component: StoryEditView, props: true },
    { path: '/story/:id/play', name: 'story-play', component: PlayView, props: true },
  ],
})

export default router
