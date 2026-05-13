package com.echoim.server.service;

import com.echoim.server.service.message.impl.MessageViewServiceImpl;
import com.echoim.server.vo.conversation.MessageItemVo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class MessageViewServiceImplTest {

    @Test
    void enrichMessagesShouldExposeReplySourceFromExtraJson() {
        MessageViewServiceImpl service = new MessageViewServiceImpl(
                mock(com.echoim.server.mapper.ImMessageReceiptMapper.class),
                mock(com.echoim.server.mapper.ImMessageReactionMapper.class),
                mock(com.echoim.server.mapper.ImMessageMapper.class),
                mock(com.echoim.server.mapper.ImGroupMemberMapper.class),
                new ObjectMapper()
        );
        MessageItemVo message = new MessageItemVo();
        message.setMessageId(1L);
        message.setConversationType(1);
        message.setFromUserId(10002L);
        message.setSendStatus(1);
        message.setMsgType("TEXT");
        message.setContent("reply body");
        message.setExtraJsonRaw("""
                {
                  "replySource": {
                    "sourceMessageId": 88,
                    "sourceConversationId": 30001,
                    "sourceSenderId": 10001,
                    "sourceMsgType": "TEXT",
                    "sourcePreview": "original body"
                  }
                }
                """);

        service.enrichMessages(10001L, List.of(message));

        assertNotNull(message.getReplySource());
        assertEquals(88L, message.getReplySource().getSourceMessageId());
        assertEquals("original body", message.getReplySource().getSourcePreview());
    }
}
