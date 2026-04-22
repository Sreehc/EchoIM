package com.echoim.server.controller;

import com.echoim.server.common.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> upload(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(Map.of(
                "fileId", 50001L,
                "fileName", file.getOriginalFilename() == null ? "unknown" : file.getOriginalFilename(),
                "contentType", file.getContentType() == null ? "application/octet-stream" : file.getContentType(),
                "fileSize", file.getSize(),
                "url", "/upload/demo/" + (file.getOriginalFilename() == null ? "unknown" : file.getOriginalFilename())
        ));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> info(@PathVariable Long id) {
        return ApiResponse.success(Map.of(
                "fileId", id,
                "fileName", "welcome.png",
                "contentType", "image/png",
                "fileSize", 102400L,
                "url", "/upload/demo/welcome.png"
        ));
    }

    @GetMapping("/{id}/download")
    public ApiResponse<Map<String, Object>> download(@PathVariable Long id) {
        return ApiResponse.success(Map.of(
                "fileId", id,
                "downloadUrl", "/upload/demo/welcome.png"
        ));
    }
}
