package com.echoim.server.service;

import com.echoim.server.common.audit.AuditLogService;
import com.echoim.server.common.exception.BizException;
import com.echoim.server.config.FileProperties;
import com.echoim.server.mapper.ImFileMapper;
import com.echoim.server.service.impl.FileServiceImpl;
import com.echoim.server.service.file.storage.LocalFileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class FileServiceImplTest {

    private ImFileMapper imFileMapper;
    private FileProperties fileProperties;
    private LocalFileStorageService localFileStorageService;
    private FileServiceImpl fileService;

    @BeforeEach
    void setUp() {
        imFileMapper = mock(ImFileMapper.class);
        fileProperties = new FileProperties();
        localFileStorageService = mock(LocalFileStorageService.class);
        fileService = new FileServiceImpl(
                imFileMapper,
                fileProperties,
                mock(AuditLogService.class),
                localFileStorageService,
                List.of()
        );
    }

    @Test
    void uploadShouldRejectBadImageContent() {
        MockMultipartFile file = new MockMultipartFile("file", "bad.png", "image/png", "not-an-image".getBytes());
        assertThrows(BizException.class, () -> fileService.upload(10001L, file, 2));
    }

    @Test
    void uploadShouldRejectDisallowedExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "shell.sh", "text/plain", "echo hi".getBytes());
        assertThrows(BizException.class, () -> fileService.upload(10001L, file, 4));
    }
}
