package com.echoim.server.controller;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.common.annotation.RequireLogin;
import com.echoim.server.common.auth.LoginUserContext;
import com.echoim.server.common.ratelimit.RateLimit;
import com.echoim.server.service.file.FileService;
import com.echoim.server.service.file.FileStreamPayload;
import com.echoim.server.vo.file.FileDownloadVo;
import com.echoim.server.vo.file.FileInfoVo;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @RequireLogin
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RateLimit(keyType = RateLimit.KeyType.USER, name = "file-upload", permits = 20, windowSeconds = 60, message = "上传过于频繁")
    public ApiResponse<FileInfoVo> upload(@RequestPart("file") MultipartFile file,
                                          @RequestParam(value = "bizType", required = false) Integer bizType) {
        return ApiResponse.success(fileService.upload(LoginUserContext.requireUserId(), file, bizType));
    }

    @RequireLogin
    @GetMapping("/{id}")
    public ApiResponse<FileInfoVo> info(@PathVariable Long id) {
        return ApiResponse.success(fileService.getFileInfo(LoginUserContext.requireUserId(), id));
    }

    @RequireLogin
    @GetMapping("/{id}/download")
    public ApiResponse<FileDownloadVo> download(@PathVariable Long id) {
        return ApiResponse.success(fileService.getDownloadInfo(LoginUserContext.requireUserId(), id));
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<InputStreamResource> publicFile(@PathVariable Long id) {
        return toResponse(fileService.getPublicFileStream(id));
    }

    @GetMapping("/access/{id}")
    public ResponseEntity<InputStreamResource> signedAccess(@PathVariable Long id,
                                                            @RequestParam long expires,
                                                            @RequestParam(required = false) String disposition,
                                                            @RequestParam("sig") String signature) {
        return toResponse(fileService.getSignedFileStream(id, expires, disposition, signature));
    }

    private ResponseEntity<InputStreamResource> toResponse(FileStreamPayload payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.builder(payload.disposition())
                .filename(payload.fileName(), StandardCharsets.UTF_8)
                .build());
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType(payload.contentType()))
                .contentLength(payload.fileSize() == null ? -1 : payload.fileSize())
                .body(new InputStreamResource(payload.inputStream()));
    }
}
