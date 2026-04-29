package com.echoim.server.service.call;

import com.echoim.server.common.auth.LoginUser;
import com.echoim.server.im.model.WsMessage;
import com.echoim.server.vo.call.CallSessionSummaryVo;

public interface CallService {

    CallSessionSummaryVo createCall(Long userId, Long conversationId, String callType);

    CallSessionSummaryVo acceptCall(Long userId, Long callId);

    CallSessionSummaryVo rejectCall(Long userId, Long callId);

    CallSessionSummaryVo cancelCall(Long userId, Long callId);

    CallSessionSummaryVo endCall(Long userId, Long callId);

    CallSessionSummaryVo getCall(Long userId, Long callId);

    void relaySignal(LoginUser loginUser, WsMessage wsMessage);
}
