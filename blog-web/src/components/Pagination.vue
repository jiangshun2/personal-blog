<template>
  <nav v-if="totalPages > 1" class="pagination" aria-label="分页导航">
    <button type="button" class="page-btn" :disabled="current <= 1" @click="go(current - 1)">
      上一页
    </button>
    <button
      v-for="(page, index) in pages"
      :key="index"
      type="button"
      class="page-btn page-number"
      :class="{ active: page === current, disabled: page === '...' }"
      :disabled="page === '...'"
      @click="page !== '...' && go(page)"
    >
      {{ page }}
    </button>
    <button
      type="button"
      class="page-btn"
      :disabled="current >= totalPages"
      @click="go(current + 1)"
    >
      下一页
    </button>
  </nav>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  /** 当前页码（从 1 开始） */
  current: { type: Number, default: 1 },
  /** 每页条数 */
  pageSize: { type: Number, default: 10 },
  /** 总条数 */
  total: { type: Number, default: 0 }
})

const emit = defineEmits(['change'])

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / (props.pageSize || 10))))

/** 页码窗口：总页数较少时全部展示，较多时用省略号（首尾页始终可见） */
const pages = computed(() => {
  const total = totalPages.value
  const current = props.current
  const result = []

  if (total <= 7) {
    for (let i = 1; i <= total; i += 1) result.push(i)
    return result
  }

  result.push(1)
  const start = Math.max(2, current - 1)
  const end = Math.min(total - 1, current + 1)
  if (start > 2) result.push('...')
  for (let i = start; i <= end; i += 1) result.push(i)
  if (end < total - 1) result.push('...')
  result.push(total)
  return result
})

function go(page) {
  const target = Number(page)
  if (!target || isNaN(target)) return
  if (target < 1 || target > totalPages.value) return
  if (target === props.current) return
  emit('change', target)
}
</script>
