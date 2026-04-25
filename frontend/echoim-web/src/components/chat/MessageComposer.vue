<script setup lang="ts">
import { ref } from 'vue'
import { ChatDotRound, FolderAdd, Promotion } from '@element-plus/icons-vue'

const emit = defineEmits<{
  send: [content: string]
}>()

const draft = ref('')

function submit() {
  const content = draft.value.trim()
  if (!content) return

  emit('send', content)
  draft.value = ''
}

function onKeydown(event: Event | KeyboardEvent) {
  if (!(event instanceof KeyboardEvent)) return

  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    submit()
  }
}
</script>

<template>
  <footer class="composer">
    <div class="composer__bar">
      <button class="composer__icon" type="button" aria-label="附件">
        <FolderAdd />
      </button>
      <button class="composer__icon" type="button" aria-label="表情">
        <ChatDotRound />
      </button>
      <el-input
        v-model="draft"
        :autosize="{ minRows: 1, maxRows: 4 }"
        maxlength="500"
        placeholder="输入消息，Enter 发送，Shift + Enter 换行"
        resize="none"
        type="textarea"
        aria-label="消息输入框"
        @keydown="onKeydown"
      />
      <el-button
        type="primary"
        circle
        class="composer__send"
        :disabled="!draft.trim()"
        aria-label="发送消息"
        data-testid="send-message"
        @click="submit"
      >
        <Promotion />
      </el-button>
    </div>
  </footer>
</template>

<style scoped>
.composer {
  padding: 10px 12px 12px;
  border-top: 1px solid var(--color-line);
  background: var(--color-bg-surface);
}

.composer__bar {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px;
  border: 1px solid var(--color-line);
  border-radius: 18px;
  background: var(--color-bg-elevated);
}

.composer__icon {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: var(--color-text-2);
}

.composer__icon:hover,
.composer__icon:focus-visible {
  background: var(--color-hover);
  color: var(--color-text-1);
}

.composer__bar :deep(.el-textarea) {
  flex: 1;
}

.composer__bar :deep(.el-textarea__inner) {
  min-height: 40px !important;
  padding: 10px 10px 9px;
  border: 0;
  background: transparent;
  line-height: 1.4;
  font-size: 0.94rem;
  resize: none;
}

.composer__send {
  width: 38px;
  min-width: 38px;
  height: 38px;
  padding: 0;
  border-radius: 14px;
}

@media (max-width: 767px) {
  .composer {
    padding-inline: 10px;
  }
}
</style>
