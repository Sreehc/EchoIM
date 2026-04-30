package com.echoim.server.service.file;

import com.echoim.server.entity.ImFileEntity;
import com.echoim.server.im.model.WsMessageItem;
import com.echoim.server.vo.conversation.MessageItemVo;
import com.echoim.server.vo.file.FileDownloadVo;
import com.echoim.server.vo.file.FileInfoVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {

    FileInfoVo upload(Long userId, MultipartFile file, Integer bizType);

    FileInfoVo getFileInfo(Long userId, Long fileId);

    FileDownloadVo getDownloadInfo(Long userId, Long fileId);

    FileStreamPayload getPublicFileStream(Long fileId);

    FileStreamPayload getSignedFileStream(Long fileId, long expiresAt, String disposition, String signature);

    ImFileEntity requireOwnedFile(Long userId, Long fileId, Integer... allowedBizTypes);

    void enrichMessages(Long userId, List<MessageItemVo> messages);

    void enrichWsMessage(Long userId, WsMessageItem item);
}
