package com.echoim.server.vo.conversation;

import com.echoim.server.vo.message.MentionVo;
import com.echoim.server.vo.message.MessageForwardSourceVo;
import com.echoim.server.vo.message.MessageReactionStatVo;
import com.echoim.server.vo.message.MessageReplySourceVo;
import com.echoim.server.vo.message.StickerPayloadVo;
import com.echoim.server.vo.message.VoicePayloadVo;
import java.time.LocalDateTime;

import com.echoim.server.vo.file.FileInfoVo;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class MessageItemVo {

    private Long messageId;
    private Long conversationId;
    private Integer conversationType;
    private Long seqNo;
    private String clientMsgId;
    private Long fromUserId;
    private Long toUserId;
    private Long groupId;
    private String msgType;
    private String content;
    private Long fileId;
    private FileInfoVo file;
    private Integer sendStatus;
    private LocalDateTime sentAt;
    private Boolean recalled;
    private LocalDateTime recalledAt;
    private Boolean edited;
    private LocalDateTime editedAt;
    private Boolean delivered;
    private LocalDateTime deliveredAt;
    private Boolean read;
    private LocalDateTime readAt;
    private Integer viewCount;
    private MessageForwardSourceVo forwardSource;
    private MessageReplySourceVo replySource;
    private java.util.List<MessageReactionStatVo> reactions;
    private StickerPayloadVo sticker;
    private VoicePayloadVo voice;
    private Boolean pinned;
    private Long pinnedByUserId;
    private LocalDateTime pinnedAt;
    private java.util.List<MentionVo> mentions;
    private Integer selfDestructSeconds;
    @JsonIgnore
    private String extraJsonRaw;

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public Integer getConversationType() {
        return conversationType;
    }

    public void setConversationType(Integer conversationType) {
        this.conversationType = conversationType;
    }

    public Long getSeqNo() {
        return seqNo;
    }

    public void setSeqNo(Long seqNo) {
        this.seqNo = seqNo;
    }

    public String getClientMsgId() {
        return clientMsgId;
    }

    public void setClientMsgId(String clientMsgId) {
        this.clientMsgId = clientMsgId;
    }

    public Long getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(Long fromUserId) {
        this.fromUserId = fromUserId;
    }

    public Long getToUserId() {
        return toUserId;
    }

    public void setToUserId(Long toUserId) {
        this.toUserId = toUserId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public FileInfoVo getFile() {
        return file;
    }

    public void setFile(FileInfoVo file) {
        this.file = file;
    }

    public Integer getSendStatus() {
        return sendStatus;
    }

    public void setSendStatus(Integer sendStatus) {
        this.sendStatus = sendStatus;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public Boolean getRecalled() {
        return recalled;
    }

    public void setRecalled(Boolean recalled) {
        this.recalled = recalled;
    }

    public LocalDateTime getRecalledAt() {
        return recalledAt;
    }

    public void setRecalledAt(LocalDateTime recalledAt) {
        this.recalledAt = recalledAt;
    }

    public Boolean getEdited() {
        return edited;
    }

    public void setEdited(Boolean edited) {
        this.edited = edited;
    }

    public LocalDateTime getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(LocalDateTime editedAt) {
        this.editedAt = editedAt;
    }

    public Boolean getDelivered() {
        return delivered;
    }

    public void setDelivered(Boolean delivered) {
        this.delivered = delivered;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public Boolean getRead() {
        return read;
    }

    public void setRead(Boolean read) {
        this.read = read;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public MessageForwardSourceVo getForwardSource() {
        return forwardSource;
    }

    public void setForwardSource(MessageForwardSourceVo forwardSource) {
        this.forwardSource = forwardSource;
    }

    public MessageReplySourceVo getReplySource() {
        return replySource;
    }

    public void setReplySource(MessageReplySourceVo replySource) {
        this.replySource = replySource;
    }

    public java.util.List<MessageReactionStatVo> getReactions() {
        return reactions;
    }

    public void setReactions(java.util.List<MessageReactionStatVo> reactions) {
        this.reactions = reactions;
    }

    public StickerPayloadVo getSticker() {
        return sticker;
    }

    public void setSticker(StickerPayloadVo sticker) {
        this.sticker = sticker;
    }

    public VoicePayloadVo getVoice() {
        return voice;
    }

    public void setVoice(VoicePayloadVo voice) {
        this.voice = voice;
    }

    public Boolean getPinned() {
        return pinned;
    }

    public void setPinned(Boolean pinned) {
        this.pinned = pinned;
    }

    public Long getPinnedByUserId() {
        return pinnedByUserId;
    }

    public void setPinnedByUserId(Long pinnedByUserId) {
        this.pinnedByUserId = pinnedByUserId;
    }

    public LocalDateTime getPinnedAt() {
        return pinnedAt;
    }

    public void setPinnedAt(LocalDateTime pinnedAt) {
        this.pinnedAt = pinnedAt;
    }

    public java.util.List<MentionVo> getMentions() {
        return mentions;
    }

    public void setMentions(java.util.List<MentionVo> mentions) {
        this.mentions = mentions;
    }

    public Integer getSelfDestructSeconds() {
        return selfDestructSeconds;
    }

    public void setSelfDestructSeconds(Integer selfDestructSeconds) {
        this.selfDestructSeconds = selfDestructSeconds;
    }

    public String getExtraJsonRaw() {
        return extraJsonRaw;
    }

    public void setExtraJsonRaw(String extraJsonRaw) {
        this.extraJsonRaw = extraJsonRaw;
    }
}
