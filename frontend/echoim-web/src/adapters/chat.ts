import type {
  ApiConversationItem,
  ApiGlobalSearchMessageItem,
  ApiMessageItem,
  ApiOfflineSyncConversation,
} from '@/types/api'
import type {
  ChatFile,
  ChatMessage,
  ConversationSummary,
  GlobalSearchMessageItem,
  MessageForwardSource,
  MessageReactionStat,
  MessageReplySource,
  StickerPayload,
  VoicePayload,
} from '@/types/chat'
import { normalizeDisplayText } from '@/utils/text'

function normalizeTime(value: string | null | undefined): string {
  if (!value) return new Date(0).toISOString()
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? new Date(0).toISOString() : date.toISOString()
}

function adaptFile(file: ChatFile | null | undefined): ChatFile | null {
  if (!file) return null

  return {
    fileId: Number(file.fileId),
    fileName: normalizeDisplayText(file.fileName) || '未命名文件',
    fileExt: file.fileExt ?? null,
    contentType: file.contentType ?? null,
    fileSize: file.fileSize ?? null,
    bizType: file.bizType ?? null,
    objectKey: file.objectKey ?? null,
    url: file.url ?? null,
    downloadUrl: file.downloadUrl ?? null,
    expiresIn: file.expiresIn ?? null,
    expireAt: file.expireAt ? normalizeTime(file.expireAt) : null,
  }
}

function adaptForwardSource(source: MessageForwardSource | null | undefined): MessageForwardSource | null {
  if (!source) return null

  return {
    sourceMessageId: Number(source.sourceMessageId),
    sourceConversationId: Number(source.sourceConversationId),
    sourceSenderId: Number(source.sourceSenderId),
    sourceMsgType: source.sourceMsgType,
    sourcePreview: source.sourcePreview == null ? null : normalizeDisplayText(source.sourcePreview),
  }
}

function adaptReplySource(source: MessageReplySource | null | undefined): MessageReplySource | null {
  if (!source) return null

  return {
    sourceMessageId: Number(source.sourceMessageId),
    sourceConversationId: Number(source.sourceConversationId),
    sourceSenderId: Number(source.sourceSenderId),
    sourceMsgType: source.sourceMsgType,
    sourcePreview: source.sourcePreview == null ? null : normalizeDisplayText(source.sourcePreview),
  }
}

function adaptReactionStats(stats: MessageReactionStat[] | null | undefined): MessageReactionStat[] {
  return (stats ?? []).map((stat) => ({
    emoji: stat.emoji,
    count: Number(stat.count ?? 0),
    reacted: Boolean(stat.reacted),
  }))
}

function adaptSticker(sticker: StickerPayload | null | undefined): StickerPayload | null {
  if (!sticker) return null
  return {
    stickerId: sticker.stickerId,
    title: normalizeDisplayText(sticker.title) || '贴纸',
  }
}

function adaptVoice(voice: VoicePayload | null | undefined): VoicePayload | null {
  if (!voice) return null
  return {
    duration: Number(voice.duration ?? 0),
    waveform: Array.isArray(voice.waveform) ? voice.waveform.map(Number) : [],
  }
}

export function adaptConversationSummary(
  item: ApiConversationItem,
): ConversationSummary {
  return {
    conversationId: Number(item.conversationId),
    conversationNo: item.conversationNo ?? '',
    conversationType: item.conversationType,
    conversationName: normalizeDisplayText(item.conversationName) || `会话 ${item.conversationId}`,
    avatarUrl: item.avatarUrl ?? null,
    lastMessagePreview: normalizeDisplayText(item.lastMessagePreview) ?? '',
    lastMessageTime: normalizeTime(item.lastMessageTime),
    unreadCount: Number(item.unreadCount ?? 0),
    isTop: Number(item.isTop ?? 0),
    isMute: Number(item.isMute ?? 0),
    peerUserId: item.peerUserId == null ? null : Number(item.peerUserId),
    groupId: item.groupId == null ? null : Number(item.groupId),
    latestSeq: Number(item.latestSeq ?? 0),
    canSend: item.canSend ?? true,
    groupStatus: item.groupStatus ?? null,
    myRole: item.myRole == null ? null : Number(item.myRole),
    archived: Boolean(item.archived),
    manualUnread: Boolean(item.manualUnread),
    specialType: item.specialType ?? null,
    folderHints: item.folderHints ?? null,
  }
}

export function adaptChatMessage(item: ApiMessageItem): ChatMessage {
  return {
    messageId: Number(item.messageId),
    conversationId: Number(item.conversationId),
    conversationType: item.conversationType,
    seqNo: Number(item.seqNo ?? 0),
    clientMsgId: item.clientMsgId ?? `server-${item.messageId}`,
    fromUserId: Number(item.fromUserId),
    toUserId: item.toUserId == null ? null : Number(item.toUserId),
    groupId: item.groupId == null ? null : Number(item.groupId),
    msgType: item.msgType,
    content: item.content == null ? null : normalizeDisplayText(item.content),
    fileId: item.fileId == null ? null : Number(item.fileId),
    file: adaptFile(item.file),
    sentAt: normalizeTime(item.sentAt),
    sendStatus: Number(item.sendStatus ?? 1),
    recalled: Boolean(item.recalled),
    recalledAt: item.recalledAt ? normalizeTime(item.recalledAt) : null,
    edited: Boolean(item.edited),
    editedAt: item.editedAt ? normalizeTime(item.editedAt) : null,
    delivered: Boolean(item.delivered),
    deliveredAt: item.deliveredAt ? normalizeTime(item.deliveredAt) : null,
    read: Boolean(item.read),
    readAt: item.readAt ? normalizeTime(item.readAt) : null,
    viewCount: Number(item.viewCount ?? 0),
    forwardSource: adaptForwardSource(item.forwardSource),
    replySource: adaptReplySource(item.replySource),
    reactions: adaptReactionStats(item.reactions),
    sticker: adaptSticker(item.sticker),
    voice: adaptVoice(item.voice),
    errorMessage: null,
  }
}

export function messagePreviewFromMessage(message: ChatMessage): string {
  if (message.recalled) {
    return '撤回了一条消息'
  }

  if (message.msgType === 'STICKER') {
    return message.sticker?.title ? `[贴纸] ${message.sticker.title}` : '[贴纸]'
  }

  if (message.msgType === 'IMAGE') {
    return message.file?.fileName ? `[图片] ${message.file.fileName}` : '[图片]'
  }

  if (message.msgType === 'GIF') {
    return message.file?.fileName ? `[GIF] ${message.file.fileName}` : '[GIF]'
  }

  if (message.msgType === 'FILE') {
    return message.file?.fileName ? `[文件] ${message.file.fileName}` : '[文件]'
  }

  if (message.msgType === 'VOICE') {
    return '[语音]'
  }

  if (message.msgType === 'SYSTEM') {
    return message.content ?? '系统消息'
  }

  return message.content?.trim() || '新消息'
}

export function mergeMessages(existing: ChatMessage[], incoming: ChatMessage[]): ChatMessage[] {
  const merged = [...existing]
  const messageIdIndex = new Map<number, number>()
  const clientMsgIdIndex = new Map<string, number>()
  const seqIndex = new Map<number, number>()

  const indexMessage = (message: ChatMessage, index: number) => {
    if (message.messageId > 0) {
      messageIdIndex.set(message.messageId, index)
    }
    if (message.clientMsgId) {
      clientMsgIdIndex.set(message.clientMsgId, index)
    }
    if (message.seqNo > 0) {
      seqIndex.set(message.seqNo, index)
    }
  }

  const findMessageIndex = (message: ChatMessage) => {
    if (message.messageId > 0 && messageIdIndex.has(message.messageId)) {
      return messageIdIndex.get(message.messageId)
    }
    if (message.clientMsgId && clientMsgIdIndex.has(message.clientMsgId)) {
      return clientMsgIdIndex.get(message.clientMsgId)
    }
    if (message.seqNo > 0 && seqIndex.has(message.seqNo)) {
      return seqIndex.get(message.seqNo)
    }
    return undefined
  }

  existing.forEach((message, index) => {
    indexMessage(message, index)
  })

  for (const message of incoming) {
    const matchedIndex = findMessageIndex(message)

    if (matchedIndex == null) {
      merged.push(message)
      indexMessage(message, merged.length - 1)
      continue
    }

    const previous = merged[matchedIndex]
    merged[matchedIndex] = {
      ...previous,
      ...message,
      sendStatus: message.sendStatus,
      delivered: Boolean(previous.delivered || message.delivered),
      read: Boolean(previous.read || message.read),
      viewCount: Math.max(previous.viewCount ?? 0, message.viewCount ?? 0),
      errorMessage: message.errorMessage ?? previous.errorMessage ?? null,
    }
    indexMessage(merged[matchedIndex], matchedIndex)
  }

  return merged.sort((left, right) => {
    if (left.seqNo !== right.seqNo) return left.seqNo - right.seqNo
    return new Date(left.sentAt).getTime() - new Date(right.sentAt).getTime()
  })
}

export function mapOfflineConversation(
  item: ApiOfflineSyncConversation,
) {
  return {
    conversation: adaptConversationSummary(item.conversation),
    messages: item.messages.map(adaptChatMessage),
    fromSeq: Number(item.fromSeq ?? 0),
    toSeq: Number(item.toSeq ?? 0),
    hasMore: Boolean(item.hasMore),
  }
}

export function adaptGlobalSearchMessage(item: ApiGlobalSearchMessageItem): GlobalSearchMessageItem {
  return {
    messageId: Number(item.messageId),
    conversationId: Number(item.conversationId),
    conversationType: item.conversationType,
    conversationName: normalizeDisplayText(item.conversationName) || `会话 ${item.conversationId}`,
    specialType: item.specialType ?? null,
    fromUserId: Number(item.fromUserId),
    senderName: normalizeDisplayText(item.senderName) || `用户 ${item.fromUserId}`,
    msgType: item.msgType,
    preview: normalizeDisplayText(item.preview) || '',
    sentAt: normalizeTime(item.sentAt),
    archived: Boolean(item.archived),
  }
}
