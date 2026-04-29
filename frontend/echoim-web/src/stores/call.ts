import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { ConversationSummary } from '@/types/chat'
import type { ApiCallSessionSummary, WsCallSignalPayload, WsCallSummaryPayload } from '@/types/api'
import { EchoWsClient } from '@/services/ws'
import { acceptCall, cancelCall, createCall, endCall, fetchCall, rejectCall } from '@/services/calls'
import { HttpError } from '@/services/http'
import { useAuthStore } from './auth'
import type { CallPhase, CallSessionSummary } from '@/types/chat'

type SignalEnvelopeType = 'CALL_INVITE' | 'CALL_ACCEPT' | 'CALL_REJECT' | 'CALL_CANCEL' | 'CALL_END' | 'CALL_STATE' | 'CALL_OFFER' | 'CALL_ANSWER' | 'CALL_ICE_CANDIDATE'

export const useCallStore = defineStore('call', () => {
  const authStore = useAuthStore()

  const activeCall = ref<CallSessionSummary | null>(null)
  const phase = ref<CallPhase>('idle')
  const minimized = ref(false)
  const localMuted = ref(false)
  const busy = ref(false)
  const error = ref<string | null>(null)
  const localStream = ref<MediaStream | null>(null)
  const remoteStream = ref<MediaStream | null>(null)

  let wsClient: EchoWsClient | null = null
  let peerConnection: RTCPeerConnection | null = null
  let pendingIceCandidates: RTCIceCandidateInit[] = []

  const isVisible = computed(() => phase.value !== 'idle')
  const isIncoming = computed(() => phase.value === 'incoming')
  const isConnected = computed(() => phase.value === 'connected')

  function attachRealtime(client: EchoWsClient | null) {
    wsClient = client
  }

  async function startOutgoingCall(conversation: ConversationSummary) {
    if (conversation.conversationType !== 1 || conversation.specialType === 'SAVED_MESSAGES') {
      throw new Error('当前会话不支持发起语音通话')
    }
    if (!conversation.peerUserId) {
      throw new Error('当前会话缺少对端用户标识')
    }

    busy.value = true
    error.value = null

    try {
      await ensureLocalAudio()
      const summary = normalizeCall(await createCall({ conversationId: conversation.conversationId, callType: 'audio' }))
      setActiveCall(summary)
      phase.value = 'outgoing'
      minimized.value = false
      await ensurePeerConnection(summary)
    } catch (nextError) {
      clearMediaState()
      error.value = toErrorMessage(nextError, '发起语音通话失败')
      throw nextError
    } finally {
      busy.value = false
    }
  }

  async function acceptIncomingCall() {
    if (!activeCall.value) return

    busy.value = true
    error.value = null

    try {
      await ensureLocalAudio()
      const summary = normalizeCall(await acceptCall(activeCall.value.callId))
      setActiveCall(summary)
      phase.value = 'connecting'
      minimized.value = false
      await ensurePeerConnection(summary)
    } catch (nextError) {
      clearMediaState()
      error.value = toErrorMessage(nextError, '接听语音通话失败')
      throw nextError
    } finally {
      busy.value = false
    }
  }

  async function rejectIncomingCall() {
    if (!activeCall.value) return
    busy.value = true
    try {
      const summary = normalizeCall(await rejectCall(activeCall.value.callId))
      applyTerminalSummary(summary)
    } finally {
      busy.value = false
    }
  }

  async function cancelOutgoingCall() {
    if (!activeCall.value) return
    busy.value = true
    try {
      const summary = normalizeCall(await cancelCall(activeCall.value.callId))
      applyTerminalSummary(summary)
    } finally {
      busy.value = false
    }
  }

  async function endCurrentCall() {
    if (!activeCall.value) return
    busy.value = true
    try {
      if (phase.value === 'incoming') {
        await rejectIncomingCall()
        return
      }
      if (phase.value === 'outgoing') {
        await cancelOutgoingCall()
        return
      }
      const summary = normalizeCall(await endCall(activeCall.value.callId))
      applyTerminalSummary(summary)
    } finally {
      busy.value = false
    }
  }

  function toggleMinimized() {
    minimized.value = !minimized.value
  }

  function toggleMute() {
    const stream = localStream.value
    if (!stream) return
    localMuted.value = !localMuted.value
    stream.getAudioTracks().forEach((track) => {
      track.enabled = !localMuted.value
    })
  }

  async function syncActiveCall() {
    if (!activeCall.value) return
    try {
      const summary = normalizeCall(await fetchCall(activeCall.value.callId))
      setActiveCall(summary)
      syncPhaseFromStatus(summary)
    } catch (nextError) {
      error.value = toErrorMessage(nextError, '通话状态同步失败')
    }
  }

  async function handleWsEvent(type: SignalEnvelopeType, payload: WsCallSummaryPayload | WsCallSignalPayload) {
    if (type === 'CALL_INVITE') {
      setActiveCall(normalizeCall(payload as ApiCallSessionSummary))
      phase.value = 'incoming'
      minimized.value = false
      error.value = null
      return
    }

    if (type === 'CALL_ACCEPT') {
      const summary = normalizeCall(payload as ApiCallSessionSummary)
      if (!matchesActiveCall(summary.callId)) return
      setActiveCall(summary)
      phase.value = 'connecting'
      await ensurePeerConnection(summary)
      await createAndSendOffer()
      return
    }

    if (type === 'CALL_REJECT' || type === 'CALL_CANCEL' || type === 'CALL_END') {
      const summary = normalizeCall(payload as ApiCallSessionSummary)
      if (!matchesActiveCall(summary.callId)) return
      applyTerminalSummary(summary)
      return
    }

    if (type === 'CALL_STATE') {
      const summary = normalizeCall(payload as ApiCallSessionSummary)
      if (!matchesActiveCall(summary.callId) && activeCall.value) return
      setActiveCall(summary)
      syncPhaseFromStatus(summary)
      return
    }

    if (type === 'CALL_OFFER') {
      const signal = payload as WsCallSignalPayload
      if (!matchesActiveCall(signal.callId) || !signal.sdp) return
      if (!activeCall.value) return
      await ensurePeerConnection(activeCall.value)
      if (!peerConnection) return
      await peerConnection.setRemoteDescription({ type: 'offer', sdp: signal.sdp })
      await flushPendingIceCandidates()
      const answer = await peerConnection.createAnswer()
      await peerConnection.setLocalDescription(answer)
      wsClient?.sendCallAnswer({
        callId: signal.callId,
        conversationId: signal.conversationId,
        sdp: answer.sdp ?? null,
      }, buildSignalClientMsgId(signal.callId, 'answer'))
      phase.value = 'connecting'
      return
    }

    if (type === 'CALL_ANSWER') {
      const signal = payload as WsCallSignalPayload
      if (!matchesActiveCall(signal.callId) || !signal.sdp || !peerConnection) return
      await peerConnection.setRemoteDescription({ type: 'answer', sdp: signal.sdp })
      await flushPendingIceCandidates()
      return
    }

    if (type === 'CALL_ICE_CANDIDATE') {
      const signal = payload as WsCallSignalPayload
      if (!matchesActiveCall(signal.callId)) return
      const candidate: RTCIceCandidateInit = {
        candidate: signal.candidate ?? '',
        sdpMid: signal.sdpMid ?? undefined,
        sdpMLineIndex: signal.sdpMLineIndex ?? undefined,
      }
      if (!peerConnection || !peerConnection.remoteDescription) {
        pendingIceCandidates.push(candidate)
        return
      }
      await peerConnection.addIceCandidate(candidate)
    }
  }

  function resetState() {
    clearMediaState()
    activeCall.value = null
    phase.value = 'idle'
    minimized.value = false
    localMuted.value = false
    busy.value = false
    error.value = null
  }

  function setActiveCall(summary: CallSessionSummary) {
    activeCall.value = summary
  }

  function syncPhaseFromStatus(summary: CallSessionSummary) {
    if (summary.status === 'accepted') {
      if (phase.value !== 'connected') {
        phase.value = peerConnection?.connectionState === 'connected' ? 'connected' : 'connecting'
      }
      return
    }
    if (summary.status === 'ringing') {
      phase.value = activeCall.value?.calleeUserId === authStore.currentUser?.userId ? 'incoming' : 'outgoing'
      return
    }
    if (summary.status === 'rejected' || summary.status === 'cancelled' || summary.status === 'ended' || summary.status === 'missed' || summary.status === 'failed') {
      applyTerminalSummary(summary)
    }
  }

  async function ensureLocalAudio() {
    if (localStream.value) {
      localStream.value.getAudioTracks().forEach((track) => {
        track.enabled = true
      })
      localMuted.value = false
      return localStream.value
    }
    const stream = await navigator.mediaDevices.getUserMedia({ audio: true, video: false })
    localMuted.value = false
    localStream.value = stream
    return stream
  }

  async function ensurePeerConnection(summary: CallSessionSummary) {
    if (peerConnection) return peerConnection
    const stream = await ensureLocalAudio()
    const connection = new RTCPeerConnection({
      iceServers: summary.iceServers.map((server) => ({
        urls: [...server.urls],
        username: server.username ?? undefined,
        credential: server.credential ?? undefined,
      })),
    })
    pendingIceCandidates = []
    remoteStream.value = new MediaStream()

    stream.getAudioTracks().forEach((track) => {
      connection.addTrack(track, stream)
    })

    connection.ontrack = (event) => {
      const nextRemoteStream = remoteStream.value ?? new MediaStream()
      event.streams[0]?.getTracks().forEach((track) => {
        nextRemoteStream.addTrack(track)
      })
      remoteStream.value = nextRemoteStream
    }

    connection.onicecandidate = (event) => {
      if (!event.candidate || !activeCall.value) return
      wsClient?.sendCallIceCandidate({
        callId: activeCall.value.callId,
        conversationId: activeCall.value.conversationId,
        candidate: event.candidate.candidate,
        sdpMid: event.candidate.sdpMid,
        sdpMLineIndex: event.candidate.sdpMLineIndex,
      }, buildSignalClientMsgId(activeCall.value.callId, 'ice'))
    }

    connection.onconnectionstatechange = () => {
      const state = connection.connectionState
      if (state === 'connected') {
        phase.value = 'connected'
        error.value = null
        return
      }
      if (state === 'failed') {
        error.value = '语音连接已中断'
        phase.value = 'ended'
        clearPeerConnection()
        return
      }
      if (state === 'disconnected') {
        error.value = '语音连接已断开'
      }
    }

    peerConnection = connection
    return connection
  }

  async function createAndSendOffer() {
    if (!peerConnection || !activeCall.value) return
    const offer = await peerConnection.createOffer()
    await peerConnection.setLocalDescription(offer)
    wsClient?.sendCallOffer({
      callId: activeCall.value.callId,
      conversationId: activeCall.value.conversationId,
      sdp: offer.sdp ?? null,
    }, buildSignalClientMsgId(activeCall.value.callId, 'offer'))
  }

  async function flushPendingIceCandidates() {
    if (!peerConnection || !pendingIceCandidates.length) return
    const candidates = [...pendingIceCandidates]
    pendingIceCandidates = []
    for (const candidate of candidates) {
      await peerConnection.addIceCandidate(candidate)
    }
  }

  function clearPeerConnection() {
    pendingIceCandidates = []
    peerConnection?.close()
    peerConnection = null
    remoteStream.value?.getTracks().forEach((track) => track.stop())
    remoteStream.value = null
  }

  function clearMediaState() {
    clearPeerConnection()
    localStream.value?.getTracks().forEach((track) => track.stop())
    localStream.value = null
    localMuted.value = false
  }

  function applyTerminalSummary(summary: CallSessionSummary) {
    setActiveCall(summary)
    phase.value = 'ended'
    clearMediaState()
    window.setTimeout(() => {
      if (activeCall.value?.callId !== summary.callId) return
      activeCall.value = null
      phase.value = 'idle'
      minimized.value = false
    }, 1800)
  }

  function matchesActiveCall(callId: number) {
    return activeCall.value?.callId === callId
  }

  function normalizeCall(payload: ApiCallSessionSummary): CallSessionSummary {
    return {
      callId: Number(payload.callId),
      conversationId: Number(payload.conversationId),
      callType: payload.callType,
      status: payload.status,
      endReason: payload.endReason ?? null,
      callerUserId: Number(payload.callerUserId),
      calleeUserId: Number(payload.calleeUserId),
      peerUserId: Number(payload.peerUserId),
      peerDisplayName: payload.peerDisplayName || '通话对象',
      peerAvatarUrl: payload.peerAvatarUrl ?? null,
      startedAt: normalizeTime(payload.startedAt),
      answeredAt: payload.answeredAt ? normalizeTime(payload.answeredAt) : null,
      endedAt: payload.endedAt ? normalizeTime(payload.endedAt) : null,
      durationSeconds: Number(payload.durationSeconds ?? 0),
      iceServers: (payload.iceServers ?? []).map((server) => ({
        urls: [...(server.urls ?? [])],
        username: server.username ?? null,
        credential: server.credential ?? null,
      })),
    }
  }

  return {
    activeCall,
    phase,
    minimized,
    localMuted,
    busy,
    error,
    localStream,
    remoteStream,
    isVisible,
    isIncoming,
    isConnected,
    attachRealtime,
    startOutgoingCall,
    acceptIncomingCall,
    rejectIncomingCall,
    cancelOutgoingCall,
    endCurrentCall,
    toggleMinimized,
    toggleMute,
    syncActiveCall,
    handleWsEvent,
    resetState,
  }
})

function normalizeTime(value: string | null | undefined) {
  if (!value) return new Date(0).toISOString()
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? new Date(0).toISOString() : date.toISOString()
}

function toErrorMessage(error: unknown, fallback: string) {
  if (error instanceof HttpError) {
    return error.message || fallback
  }
  if (error instanceof Error) {
    return error.message || fallback
  }
  return fallback
}

function buildSignalClientMsgId(callId: number, type: string) {
  return `call-${callId}-${type}-${Date.now()}`
}
