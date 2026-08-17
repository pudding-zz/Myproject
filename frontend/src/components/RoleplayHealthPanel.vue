<script setup>
import { computed, ref, watch } from 'vue'
import { api } from '../api/client.js'
import {
  DEMO_HEALTH_RECORDS,
  summarizeHealth,
  buildHealthReport,
} from '../data/roleplayHealthDemo.js'

const props = defineProps({
  sessionId: { type: [Number, String], default: null },
  aiName: { type: String, default: '沈清野' },
  playerName: { type: String, default: '林念' },
})

const records = ref([])
const fromApi = ref(false)
const loadError = ref('')
const selected = ref(null)
const reportOpen = ref(false)
const report = ref({ level: '', sub: '', tsukomi: '' })

const summary = computed(() => summarizeHealth(records.value))

const hint = computed(() => {
  if (loadError.value) return `加载失败：${loadError.value}（已回退演示数据）`
  if (fromApi.value) {
    return records.value.length
      ? '会话真实记录'
      : '当前会话暂无生理记录（可用 PUT /health 写入）'
  }
  return '当前为演示数据；进入会话后将拉取真实记录。'
})

async function load() {
  selected.value = null
  loadError.value = ''
  const id = props.sessionId != null && props.sessionId !== '' ? Number(props.sessionId) : null
  if (!id) {
    records.value = DEMO_HEALTH_RECORDS
    fromApi.value = false
    return
  }
  try {
    const data = await api.getRoleplayHealth(id)
    if (data?.available === false) {
      records.value = DEMO_HEALTH_RECORDS
      fromApi.value = false
      return
    }
    records.value = Array.isArray(data?.records) ? data.records : []
    fromApi.value = true
  } catch (e) {
    loadError.value = e.message || '请求失败'
    records.value = DEMO_HEALTH_RECORDS
    fromApi.value = false
  }
}

watch(() => props.sessionId, load, { immediate: true })

function selectDay(d) {
  selected.value = d
}

function openReport() {
  report.value = buildHealthReport(summary.value.totalCount)
  reportOpen.value = true
}
</script>

<template>
  <details class="health-panel">
    <summary>
      <div class="panel-summary-left">
        <span class="panel-title health-rose">生理记录</span>
        <span class="panel-sub">{{ aiName }} · 本月亲密指数追踪</span>
      </div>
      <span class="panel-chevron health-rose">▾</span>
    </summary>
    <div class="panel-body">
      <p class="demo-hint">{{ hint }}</p>
      <div class="health-summary-grid">
        <div class="health-card">
          <p class="num">{{ summary.totalCount }}</p>
          <p class="label">总次数</p>
        </div>
        <div class="health-card">
          <p class="num">{{ summary.totalCal }}</p>
          <p class="label">消耗千卡</p>
        </div>
        <div class="health-card">
          <p class="num">{{ summary.avgHeart }}</p>
          <p class="label">平均心率</p>
        </div>
      </div>

      <div v-if="records.length" class="health-card">
        <div class="health-weekdays">
          <span>日</span><span>一</span><span>二</span><span>三</span><span>四</span><span>五</span><span>六</span>
        </div>
        <div class="health-days">
          <button
            v-for="d in records"
            :key="d.day"
            type="button"
            class="health-day"
            :class="{
              recorded: d.count > 0,
              empty: d.count === 0,
              selected: selected?.day === d.day,
            }"
            @click="selectDay(d)"
          >
            <span class="hd-num">{{ d.day }}</span>
            <span class="hd-state">{{ d.count ? d.count + '次' : '无' }}</span>
          </button>
        </div>
      </div>
      <p v-else class="demo-hint">暂无日历数据</p>

      <div v-if="selected" class="health-card">
        <div style="display: flex; justify-content: space-between; align-items: center">
          <h3 class="health-rose" style="margin: 0; font-size: 1rem">8月{{ selected.day }}日 详情</h3>
          <button
            type="button"
            class="demo-hint"
            style="background: none; border: none; cursor: pointer; color: inherit"
            @click="selected = null"
          >
            收起
          </button>
        </div>
        <div class="health-detail-grid" style="margin-top: 12px">
          <div class="cell"><p class="n">{{ selected.cal }}</p><p class="l">卡路里</p></div>
          <div class="cell"><p class="n">{{ selected.heart }}</p><p class="l">平均心率</p></div>
          <div class="cell"><p class="n">{{ selected.count }}</p><p class="l">次数</p></div>
          <div class="cell"><p class="n">{{ selected.duration }}</p><p class="l">时长(分)</p></div>
        </div>
        <div style="margin-top: 12px; font-size: 0.9rem; line-height: 1.55">
          <p><span style="opacity: 0.7">触发原因：</span>{{ selected.trigger }}</p>
          <p><span style="opacity: 0.7">场景描述：</span>{{ selected.scene }}</p>
          <p class="health-rose"><span style="opacity: 0.7">他的心声：</span>{{ selected.thought }}</p>
        </div>
      </div>

      <div class="health-foot">
        <button type="button" class="health-btn" @click="openReport">生成诊断报告</button>
        <span class="demo-hint">点击日期查看当日详情</span>
      </div>
    </div>
  </details>

  <Teleport to="body">
    <div
      v-if="reportOpen"
      class="health-report-overlay"
      @click.self="reportOpen = false"
    >
      <div class="health-report-card">
        <p style="text-align: center; margin: 0; color: #8e8e93; font-size: 0.9rem">本月诊断结果</p>
        <p class="level">{{ report.level }}</p>
        <p style="text-align: center; margin: 6px 0 0; color: #8e8e93; font-size: 0.85rem">{{ report.sub }}</p>
        <div
          style="margin-top: 16px; padding: 14px; border-radius: 12px; background: #f2f2f7; font-size: 0.9rem; line-height: 1.6"
        >
          {{ report.tsukomi }}
        </div>
        <div style="margin-top: 16px; text-align: center">
          <button type="button" class="demo-btn" @click="reportOpen = false">关闭</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>
