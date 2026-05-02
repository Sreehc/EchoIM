import { computed, onUnmounted, ref } from 'vue'

export interface VoiceRecordingResult {
  blob: Blob
  duration: number
  waveform: number[]
}

export interface UseMediaRecorderOptions {
  maxDuration?: number
  waveformBars?: number
  mimeType?: string
}

const DEFAULT_MAX_DURATION = 60
const DEFAULT_WAVEFORM_BARS = 32

function pickSupportedMimeType(preferred?: string): string | undefined {
  const candidates = preferred
    ? [preferred, 'audio/webm;codecs=opus', 'audio/webm', 'audio/ogg;codecs=opus', 'audio/ogg', 'audio/mp4']
    : ['audio/webm;codecs=opus', 'audio/webm', 'audio/ogg;codecs=opus', 'audio/ogg', 'audio/mp4']

  for (const type of candidates) {
    if (typeof MediaRecorder !== 'undefined' && MediaRecorder.isTypeSupported(type)) {
      return type
    }
  }
  return undefined
}

export function useMediaRecorder(options: UseMediaRecorderOptions = {}) {
  const maxDuration = options.maxDuration ?? DEFAULT_MAX_DURATION
  const waveformBars = options.waveformBars ?? DEFAULT_WAVEFORM_BARS

  const recording = ref(false)
  const paused = ref(false)
  const elapsed = ref(0)
  const error = ref<string | null>(null)
  const liveWaveform = ref<number[]>(new Array(waveformBars).fill(0))

  let mediaRecorder: MediaRecorder | null = null
  let mediaStream: MediaStream | null = null
  let audioContext: AudioContext | null = null
  let analyserNode: AnalyserNode | null = null
  let animationFrameId: number | null = null
  let elapsedIntervalId: ReturnType<typeof setInterval> | null = null
  let chunks: BlobPart[] = []

  const supported = computed(() => typeof MediaRecorder !== 'undefined')

  function cleanup() {
    if (animationFrameId != null) {
      cancelAnimationFrame(animationFrameId)
      animationFrameId = null
    }
    if (elapsedIntervalId != null) {
      clearInterval(elapsedIntervalId)
      elapsedIntervalId = null
    }
    if (analyserNode) {
      analyserNode.disconnect()
      analyserNode = null
    }
    if (audioContext) {
      void audioContext.close().catch(() => undefined)
      audioContext = null
    }
    if (mediaStream) {
      mediaStream.getTracks().forEach((track) => track.stop())
      mediaStream = null
    }
    mediaRecorder = null
    chunks = []
  }

  function updateLiveWaveform() {
    if (!analyserNode) return

    const dataArray = new Uint8Array(analyserNode.frequencyBinCount)
    analyserNode.getByteTimeDomainData(dataArray)

    const step = Math.floor(dataArray.length / waveformBars)
    const next: number[] = []
    for (let i = 0; i < waveformBars; i++) {
      let sum = 0
      for (let j = 0; j < step; j++) {
        const value = (dataArray[i * step + j] - 128) / 128
        sum += value * value
      }
      const rms = Math.sqrt(sum / step)
      next.push(Math.min(1, rms * 3))
    }
    liveWaveform.value = next

    if (recording.value && !paused.value) {
      animationFrameId = requestAnimationFrame(updateLiveWaveform)
    }
  }

  async function start(): Promise<void> {
    if (recording.value) return
    error.value = null

    try {
      mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true })
    } catch {
      error.value = '无法访问麦克风，请检查浏览器权限设置'
      return
    }

    const mimeType = pickSupportedMimeType(options.mimeType)
    chunks = []
    elapsed.value = 0
    liveWaveform.value = new Array(waveformBars).fill(0)

    try {
      audioContext = new AudioContext()
      const source = audioContext.createMediaStreamSource(mediaStream)
      analyserNode = audioContext.createAnalyser()
      analyserNode.fftSize = 256
      source.connect(analyserNode)
    } catch {
      // Waveform visualization is best-effort; recording still works without it
      analyserNode = null
    }

    try {
      mediaRecorder = new MediaRecorder(mediaStream, mimeType ? { mimeType } : undefined)
    } catch {
      mediaRecorder = new MediaRecorder(mediaStream)
    }

    mediaRecorder.ondataavailable = (event) => {
      if (event.data.size > 0) {
        chunks.push(event.data)
      }
    }

    mediaRecorder.start(100)
    recording.value = true
    paused.value = false

    elapsedIntervalId = setInterval(() => {
      if (!paused.value) {
        elapsed.value += 1
        if (elapsed.value >= maxDuration) {
          void stop()
        }
      }
    }, 1000)

    if (analyserNode) {
      animationFrameId = requestAnimationFrame(updateLiveWaveform)
    }
  }

  function stop(): Promise<VoiceRecordingResult | null> {
    return new Promise((resolve) => {
      if (!mediaRecorder || mediaRecorder.state === 'inactive') {
        cleanup()
        recording.value = false
        paused.value = false
        resolve(null)
        return
      }

      const duration = elapsed.value

      mediaRecorder.onstop = () => {
        const mimeType = mediaRecorder?.mimeType || 'audio/webm'
        const blob = new Blob(chunks, { type: mimeType })
        const waveform = computeWaveformFromLive()
        cleanup()
        recording.value = false
        paused.value = false
        resolve({ blob, duration, waveform })
      }

      mediaRecorder.stop()
    })
  }

  function cancel() {
    if (mediaRecorder && mediaRecorder.state !== 'inactive') {
      mediaRecorder.onstop = () => cleanup()
      mediaRecorder.stop()
    } else {
      cleanup()
    }
    recording.value = false
    paused.value = false
    elapsed.value = 0
    liveWaveform.value = new Array(waveformBars).fill(0)
  }

  function togglePause() {
    if (!mediaRecorder || mediaRecorder.state === 'inactive') return

    if (paused.value) {
      mediaRecorder.resume()
      paused.value = false
      if (analyserNode) {
        animationFrameId = requestAnimationFrame(updateLiveWaveform)
      }
    } else {
      mediaRecorder.pause()
      paused.value = true
      if (animationFrameId != null) {
        cancelAnimationFrame(animationFrameId)
        animationFrameId = null
      }
    }
  }

  function computeWaveformFromLive(): number[] {
    return liveWaveform.value.map((v) => Math.round(v * 100) / 100)
  }

  onUnmounted(() => {
    cancel()
  })

  return {
    supported,
    recording,
    paused,
    elapsed,
    error,
    liveWaveform,
    start,
    stop,
    cancel,
    togglePause,
  }
}
