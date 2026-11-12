<template>
  <div>
    <el-row :gutter="14">
      <el-col v-for="card in cards" :key="card.label" :xs="12" :sm="8" :md="4">
        <el-card shadow="never" class="stat-card" :body-style="{ padding: 0, background: card.color }">
          <div class="stat-card" :style="{ background: card.color }">
            <div class="label">{{ card.label }}</div>
            <div class="value">
              {{ card.value }}<span class="unit">{{ card.unit }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="14" style="margin-top: 14px">
      <el-col :md="14">
        <el-card shadow="never" header="培训班报名人数 TOP5">
          <div ref="barRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :md="10">
        <el-card shadow="never" header="课程分类分布">
          <div ref="pieRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" style="margin-top: 14px" header="待办提示">
      <el-alert v-if="data.pendingCount > 0" :title="`有 ${data.pendingCount} 条报名申请等待审批`" type="warning"
        :closable="false" show-icon />
      <el-alert v-else title="暂无待审批的报名申请" type="success" :closable="false" show-icon />
    </el-card>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { dashboard } from '@/api'

const data = ref({})
const barRef = ref()
const pieRef = ref()
let barChart = null
let pieChart = null

const cards = computed(() => [
  { label: '员工总数', value: data.value.userCount ?? 0, unit: '人', color: 'linear-gradient(135deg,#409eff,#2b7cd3)' },
  { label: '课程总数', value: data.value.courseCount ?? 0, unit: '门', color: 'linear-gradient(135deg,#67c23a,#4a9e26)' },
  { label: '培训班数', value: data.value.classCount ?? 0, unit: '个', color: 'linear-gradient(135deg,#e6a23c,#c98b25)' },
  { label: '报名人数', value: data.value.enrollCount ?? 0, unit: '人', color: 'linear-gradient(135deg,#f56c6c,#cf4f4f)' },
  { label: '考试次数', value: data.value.examCount ?? 0, unit: '次', color: 'linear-gradient(135deg,#909399,#6f7275)' },
  { label: '考试通过率', value: data.value.passRate ?? 0, unit: '%', color: 'linear-gradient(135deg,#8e6bd6,#6d4fae)' }
])

const renderCharts = () => {
  const rank = data.value.classRank || []
  barChart = echarts.init(barRef.value)
  barChart.setOption({
    grid: { left: 40, right: 20, top: 20, bottom: 60 },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: rank.map(i => i.name),
      axisLabel: { interval: 0, rotate: rank.length > 3 ? 15 : 0, width: 90, overflow: 'truncate' }
    },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        type: 'bar',
        data: rank.map(i => i.value),
        barWidth: '42%',
        itemStyle: {
          borderRadius: [4, 4, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#66b1ff' },
            { offset: 1, color: '#409eff' }
          ])
        },
        label: { show: true, position: 'top' }
      }
    ]
  })

  pieChart = echarts.init(pieRef.value)
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} 门 ({d}%)' },
    legend: { bottom: 0, icon: 'circle' },
    series: [
      {
        type: 'pie',
        radius: ['42%', '66%'],
        center: ['50%', '45%'],
        data: data.value.categoryDist || [],
        label: { formatter: '{b}\n{c} 门' },
        itemStyle: { borderColor: '#fff', borderWidth: 2 }
      }
    ]
  })
}

const resize = () => {
  barChart?.resize()
  pieChart?.resize()
}

onMounted(async () => {
  data.value = await dashboard()
  renderCharts()
  window.addEventListener('resize', resize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  barChart?.dispose()
  pieChart?.dispose()
})
</script>

<style scoped>
.chart {
  height: 300px;
}
</style>
