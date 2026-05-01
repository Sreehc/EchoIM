import { computed, onMounted, onUnmounted, ref, watch, type Ref } from 'vue'

export interface VirtualScrollOptions {
  /** Height of each item in pixels (used for fixed-height mode) */
  itemHeight: number
  /** Number of items to render outside the visible area */
  overscan?: number
  /** Scroll container element */
  containerRef: Ref<HTMLElement | null>
}

export function useVirtualScroll<T>(
  items: Ref<T[]>,
  options: VirtualScrollOptions,
) {
  const { itemHeight, overscan = 5, containerRef } = options

  const scrollTop = ref(0)
  const containerHeight = ref(0)

  const totalHeight = computed(() => items.value.length * itemHeight)

  const visibleRange = computed(() => {
    const start = Math.max(0, Math.floor(scrollTop.value / itemHeight) - overscan)
    const visibleCount = Math.ceil(containerHeight.value / itemHeight)
    const end = Math.min(items.value.length, start + visibleCount + overscan * 2)
    return { start, end }
  })

  const visibleItems = computed(() => {
    const { start, end } = visibleRange.value
    return items.value.slice(start, end).map((item, i) => ({
      item,
      index: start + i,
      style: {
        position: 'absolute' as const,
        top: `${(start + i) * itemHeight}px`,
        left: 0,
        right: 0,
        height: `${itemHeight}px`,
      },
    }))
  })

  const offsetY = computed(() => visibleRange.value.start * itemHeight)

  function handleScroll(e: Event) {
    const target = e.target as HTMLElement
    scrollTop.value = target.scrollTop
  }

  function measureContainer() {
    if (containerRef.value) {
      containerHeight.value = containerRef.value.clientHeight
    }
  }

  let resizeObserver: ResizeObserver | null = null

  onMounted(() => {
    measureContainer()
    if (containerRef.value) {
      resizeObserver = new ResizeObserver(() => measureContainer())
      resizeObserver.observe(containerRef.value)
    }
  })

  onUnmounted(() => {
    resizeObserver?.disconnect()
  })

  watch(containerRef, (el, oldEl) => {
    if (oldEl) resizeObserver?.unobserve(oldEl)
    if (el) {
      resizeObserver?.observe(el)
      measureContainer()
    }
  })

  return {
    visibleItems,
    totalHeight,
    handleScroll,
    scrollToIndex: (index: number) => {
      if (containerRef.value) {
        containerRef.value.scrollTop = index * itemHeight
      }
    },
  }
}
