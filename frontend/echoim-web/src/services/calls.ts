import type { ApiCallSessionSummary, CreateCallPayload } from '@/types/api'
import { getJson, postJson } from './http'

export function createCall(payload: CreateCallPayload) {
  return postJson<ApiCallSessionSummary>('/api/calls', payload)
}

export function acceptCall(callId: number) {
  return postJson<ApiCallSessionSummary>(`/api/calls/${callId}/accept`)
}

export function rejectCall(callId: number) {
  return postJson<ApiCallSessionSummary>(`/api/calls/${callId}/reject`)
}

export function cancelCall(callId: number) {
  return postJson<ApiCallSessionSummary>(`/api/calls/${callId}/cancel`)
}

export function endCall(callId: number) {
  return postJson<ApiCallSessionSummary>(`/api/calls/${callId}/end`)
}

export function fetchCall(callId: number) {
  return getJson<ApiCallSessionSummary>(`/api/calls/${callId}`)
}
