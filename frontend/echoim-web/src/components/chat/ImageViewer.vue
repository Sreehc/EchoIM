<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { ArrowLeft, ArrowRight, Close, Download, Promotion } from '@element-plus/icons-vue'

export interface ImageViewerImage {
  messageId: number
  imageUrl: string
  fileName?: string | null
}

const props = defineProps<{
  visible: boolean
  images: ImageViewerImage[]
  startIndex: number
}>()

const emit = defineEmits<{
  close: []
  forward: [messageId: number]
}>()

const currentIndex = ref(0)
const scale = ref(1)
const translateX = ref(0)
const translateY = ref(0)
const isDragging = ref(false)
const dragStartX = ref(0)
const dragStartY = ref(0)
const dragStartTranslateX = ref(0)
const dragStartTranslateY = ref(0)
const swipeStartX = ref(0)
const swipeStartY = ref(0)
const swipeDeltaX = ref(0)
const isSwiping = ref(false)
const imageLoaded = ref(false)

const currentImage = computed(() => props.images[currentIndex.value] ?? null)
const hasMultiple = computed(() => props.images.length > 1)
const canPrev = computed(() => hasMultiple.value && currentIndex.value > 0)
const canNext = computed(() => hasMultiple.value && currentIndex.value < props.images.length - 1)
const counterLabel = computed(() => {
  if (props.images.length <= 1) return ''
  return `${currentIndex.value + 1} / ${props.images.length}`
})

watch(() => props.visible, (visible) => {
  if (visible) {
    currentIndex.value = props.startIndex
    resetTransform()
    document.addEventListener('keydown', handleKeydown)
    document.body.style.overflow = 'hidden'
  } else {
    document.removeEventListener('keydown', handleKeydown)
    document.body.style.overflow = ''
  }
})

watch(currentIndex, () => {
  resetTransform()
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleKeydown)
  document.body.style.overflow = ''
})

function resetTransform() {
  scale.value = 1
  translateX.value = 0
  translateY.value = 0
  imageLoaded.value = false
}

function handleKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    event.preventDefault()
    emit('close')
    return
  }
  if (event.key === 'ArrowLeft') {
    event.preventDefault()
    goPrev()
    return
  }
  if (event.key === 'ArrowRight') {
    event.preventDefault()
    goNext()
  }
}

function goPrev() {
  if (!canPrev.value) return
  currentIndex.value--
}

function goNext() {
  if (!canNext.value) return
  currentIndex.value++
}

function handleWheel(event: WheelEvent) {
  event.preventDefault()
  const delta = event.deltaY > 0 ? -0.15 : 0.15
  const nextScale = Math.min(Math.max(scale.value + delta, 0.3), 5)
  scale.value = nextScale
  if (nextScale <= 1) {
    translateX.value = 0
    translateY.value = 0
  }
}

function handlePointerDown(event: PointerEvent) {
  if (event.button !== 0) return
  if (scale.value > 1) {
    isDragging.value = true
    dragStartX.value = event.clientX
    dragStartY.value = event.clientY
    dragStartTranslateX.value = translateX.value
    dragStartTranslateY.value = translateY.value
    ;(event.target as HTMLElement)?.setPointerCapture?.(event.pointerId)
  } else {
    isSwiping.value = true
    swipeStartX.value = event.clientX
    swipeStartY.value = event.clientY
    swipeDeltaX.value = 0
  }
}

function handlePointerMove(event: PointerEvent) {
  if (isDragging.value) {
    translateX.value = dragStartTranslateX.value + (event.clientX - dragStartX.value)
    translateY.value = dragStartTranslateY.value + (event.clientY - dragStartY.value)
    return
  }
  if (isSwiping.value) {
    swipeDeltaX.value = event.clientX - swipeStartX.value
  }
}

function handlePointerUp() {
  if (isDragging.value) {
    isDragging.value = false
    return
  }
  if (isSwiping.value) {
    isSwiping.value = false
    const threshold = 60
    if (swipeDeltaX.value > threshold && canPrev.value) {
      goPrev()
    } else if (swipeDeltaX.value < -threshold && canNext.value) {
      goNext()
    }
    swipeDeltaX.value = 0
  }
}

function handleDoubleClick() {
  if (scale.value > 1) {
    resetTransform()
  } else {
    scale.value = 2
  }
}

function handleImageLoad() {
  imageLoaded.value = true
}

function handleDownload() {
  if (!currentImage.value?.imageUrl) return
  const link = document.createElement('a')
  link.href = currentImage.value.imageUrl
  link.download = currentImage.value.fileName || `image-${currentImage.value.messageId}`
  link.target = '_blank'
  link.rel = 'noopener'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

function handleForward() {
  if (!currentImage.value) return
  emit('forward', currentImage.value.messageId)
}
</script>

<template>
  <teleport to="body">
    <transition name="image-viewer-fade">
      <div v-if="visible && currentImage" class="image-viewer" @wheel.prevent="handleWheel">
        <!-- Backdrop -->
        <div class="image-viewer__backdrop" @click="emit('close')" />

        <!-- Top bar -->
        <header class="image-viewer__header">
          <span class="image-viewer__counter">{{ counterLabel }}</span>
          <div class="image-viewer__actions">
            <button
              class="image-viewer__btn"
              type="button"
              aria-label="下载图片"
              @click="handleDownload"
            >
              <Download />
            </button>
            <button
              class="image-viewer__btn"
              type="button"
              aria-label="转发图片"
              @click="handleForward"
            >
              <Promotion />
            </button>
            <button
              class="image-viewer__btn"
              type="button"
              aria-label="关闭"
              @click="emit('close')"
            >
              <Close />
            </button>
          </div>
        </header>

        <!-- Image container -->
        <div
          class="image-viewer__stage"
          :class="{ 'is-dragging': isDragging, 'is-swiping': isSwiping }"
          @pointerdown="handlePointerDown"
          @pointermove="handlePointerMove"
          @pointerup="handlePointerUp"
          @pointercancel="handlePointerUp"
          @dblclick="handleDoubleClick"
        >
          <img
            :key="currentImage.imageUrl"
            class="image-viewer__image"
            :class="{ 'is-loaded': imageLoaded }"
            :src="currentImage.imageUrl"
            :alt="currentImage.fileName || '图片'"
            :style="{
              transform: `translate(${translateX}px, ${translateY}px) scale(${scale})`,
            }"
            draggable="false"
            @load="handleImageLoad"
          />
        </div>

        <!-- Navigation arrows -->
        <button
          v-if="canPrev"
          class="image-viewer__nav image-viewer__nav--prev"
          type="button"
          aria-label="上一张"
          @click="goPrev"
        >
          <ArrowLeft />
        </button>
        <button
          v-if="canNext"
          class="image-viewer__nav image-viewer__nav--next"
          type="button"
          aria-label="下一张"
          @click="goNext"
        >
          <ArrowRight />
        </button>
      </div>
    </transition>
  </teleport>
</template>

<style scoped>
.image-viewer {
  position: fixed;
  inset: 0;
  z-index: 3000;
  display: flex;
  flex-direction: column;
  user-select: none;
}

.image-viewer__backdrop {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.88);
  backdrop-filter: blur(6px);
}

.image-viewer__header {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: linear-gradient(to bottom, rgba(0, 0, 0, 0.5), transparent);
}

.image-viewer__counter {
  color: rgba(255, 255, 255, 0.8);
  font: 600 var(--text-sm)/1 var(--font-mono);
  letter-spacing: 0.06em;
}

.image-viewer__actions {
  display: flex;
  gap: 6px;
}

.image-viewer__btn {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.85);
  cursor: pointer;
  transition:
    background 0.15s ease,
    border-color 0.15s ease;
}

.image-viewer__btn:hover {
  background: rgba(255, 255, 255, 0.16);
  border-color: rgba(255, 255, 255, 0.3);
}

.image-viewer__btn svg {
  width: 18px;
  height: 18px;
}

.image-viewer__stage {
  position: relative;
  z-index: 1;
  flex: 1;
  display: grid;
  place-items: center;
  overflow: hidden;
  cursor: grab;
  touch-action: none;
}

.image-viewer__stage.is-dragging {
  cursor: grabbing;
}

.image-viewer__stage.is-swiping {
  cursor: grabbing;
}

.image-viewer__image {
  max-width: 92vw;
  max-height: 88vh;
  object-fit: contain;
  opacity: 0;
  transform-origin: center center;
  transition: opacity 0.2s ease;
  will-change: transform;
}

.image-viewer__image.is-loaded {
  opacity: 1;
}

.image-viewer__nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  z-index: 3;
  width: 48px;
  height: 48px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.45);
  color: rgba(255, 255, 255, 0.85);
  cursor: pointer;
  transition:
    background 0.15s ease,
    border-color 0.15s ease,
    transform 0.15s ease;
}

.image-viewer__nav:hover {
  background: rgba(0, 0, 0, 0.65);
  border-color: rgba(255, 255, 255, 0.25);
}

.image-viewer__nav svg {
  width: 22px;
  height: 22px;
}

.image-viewer__nav--prev {
  left: 20px;
}

.image-viewer__nav--prev:hover {
  transform: translateY(-50%) translateX(-2px);
}

.image-viewer__nav--next {
  right: 20px;
}

.image-viewer__nav--next:hover {
  transform: translateY(-50%) translateX(2px);
}

.image-viewer-fade-enter-active,
.image-viewer-fade-leave-active {
  transition: opacity 0.2s ease;
}

.image-viewer-fade-enter-from,
.image-viewer-fade-leave-to {
  opacity: 0;
}

@media (max-width: 767px) {
  .image-viewer__nav {
    display: none;
  }

  .image-viewer__image {
    max-width: 100vw;
    max-height: 92vh;
  }
}
</style>
