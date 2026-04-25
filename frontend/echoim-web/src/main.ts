import { createApp } from 'vue'
import App from './App.vue'
import { router } from './router'
import { pinia } from './stores/pinia'
import { useAuthStore } from './stores/auth'
import { useChatStore } from './stores/chat'
import { configureHttpClient } from './services/http'
import './styles/tokens.css'
import './styles/theme.css'
import './styles/element-overrides.scss'

const app = createApp(App)

app.use(pinia)
app.use(router)

configureHttpClient({
  getToken: () => useAuthStore(pinia).session?.token ?? null,
  onUnauthorized: () => {
    useAuthStore(pinia).clearSession()
    useChatStore(pinia).resetState()
    if (router.currentRoute.value.name !== 'login') {
      router.replace({ name: 'login' }).catch(() => undefined)
    }
  },
})

app.mount('#app')
