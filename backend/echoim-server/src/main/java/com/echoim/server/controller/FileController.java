package com.echoim.server.controller;

import com.echoim.server.common.ApiResponse;
import com.echoim.server.common.annotation.RequireLogin;
import com.echoim.server.common.auth.LoginUserContext;
import com.echoim.server.service.file.FileService;
import com.echoim.server.vo.file.FileDownloadVo;
import com.echoim.server.vo.file.FileInfoVo;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @RequireLogin
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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
}
