import type { Directive, DirectiveBinding } from 'vue'

const observerMap = new WeakMap<HTMLImageElement, IntersectionObserver>()

const vLazyImage: Directive<HTMLImageElement, string> = {
  mounted(el: HTMLImageElement, binding: DirectiveBinding<string>) {
    const src = binding.value
    if (!src) return

    // Store the real src and set a placeholder
    el.dataset.lazySrc = src
    el.removeAttribute('src')

    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            const img = entry.target as HTMLImageElement
            const realSrc = img.dataset.lazySrc
            if (realSrc) {
              img.src = realSrc
              img.removeAttribute('data-lazy-src')
            }
            observer.unobserve(img)
            observerMap.delete(img)
          }
        }
      },
      { rootMargin: '200px' },
    )

    observer.observe(el)
    observerMap.set(el, observer)
  },

  updated(el: HTMLImageElement, binding: DirectiveBinding<string>) {
    if (binding.value !== binding.oldValue) {
      el.dataset.lazySrc = binding.value
      if (binding.value) {
        // If element is already visible, load immediately
        const rect = el.getBoundingClientRect()
        const inView = rect.top < window.innerHeight + 200 && rect.bottom > -200
        if (inView) {
          el.src = binding.value
          el.removeAttribute('data-lazy-src')
        }
      }
    }
  },

  unmounted(el: HTMLImageElement) {
    const observer = observerMap.get(el)
    if (observer) {
      observer.disconnect()
      observerMap.delete(el)
    }
  },
}

export default vLazyImage
