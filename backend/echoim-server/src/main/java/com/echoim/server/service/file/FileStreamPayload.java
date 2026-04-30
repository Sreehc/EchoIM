package com.echoim.server.service.file;

import java.io.InputStream;

public record FileStreamPayload(
        String fileName,
        String contentType,
        Long fileSize,
        String disposition,
        InputStream inputStream
) {
}
