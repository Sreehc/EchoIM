package com.echoim.server.service;

import com.aliyun.oss.OSS;
import com.echoim.server.common.audit.AuditLogService;
import com.echoim.server.common.exception.BizException;
import com.echoim.server.config.FileProperties;
import com.echoim.server.mapper.ImFileMapper;
import com.echoim.server.service.impl.FileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockMultipartFile;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FileServiceImplTest {

    private ImFileMapper imFileMapper;
    private FileProperties fileProperties;
    private ObjectProvider<OSS> ossProvider;
    private OSS oss;
    private FileServiceImpl fileService;

    @BeforeEach
    void setUp() throws MalformedURLException {
        imFileMapper = mock(ImFileMapper.class);
        fileProperties = new FileProperties();
        fileProperties.getOss().setBucketName("bucket");
        fileProperties.getOss().setEndpoint("https://oss-cn-beijing.aliyuncs.com");
        fileProperties.getOss().setAccessKeyId("key");
        fileProperties.getOss().setAccessKeySecret("secret");
        ossProvider = mock(ObjectProvider.class);
        oss = mock(OSS.class);
        when(ossProvider.getIfAvailable()).thenReturn(oss);
        when(oss.generatePresignedUrl(anyString(), anyString(), any(Date.class)))
                .thenReturn(new URL("https://example.com/file"));
        fileService = new FileServiceImpl(imFileMapper, fileProperties, ossProvider, mock(AuditLogService.class));
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
