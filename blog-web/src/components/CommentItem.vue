<template>
  <div class="comment-item">
    <div class="comment-avatar">{{ avatarText(comment.nickname) }}</div>
    <div class="comment-body">
      <div class="comment-head">
        <span class="comment-name">{{ comment.nickname || '匿名访客' }}</span>
        <span class="comment-time">{{ formatDateTime(comment.createTime) }}</span>
      </div>
      <p class="comment-content">{{ comment.content }}</p>
      <div class="comment-actions">
        <button type="button" class="link-btn" @click="$emit('reply', comment)">回复</button>
      </div>

      <!-- 二级回复：递归渲染 children -->
      <div v-if="comment.children && comment.children.length" class="comment-children">
        <CommentItem
          v-for="child in comment.children"
          :key="child.id"
          :comment="child"
          @reply="$emit('reply', $event)"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { avatarText, formatDateTime } from '@/utils/format'

defineProps({
  /** CommentVO：id, nickname, content, createTime, children */
  comment: {
    type: Object,
    required: true
  }
})

defineEmits(['reply'])
</script>
