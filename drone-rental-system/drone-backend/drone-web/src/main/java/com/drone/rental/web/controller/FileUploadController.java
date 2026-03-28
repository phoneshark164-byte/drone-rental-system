package com.drone.rental.web.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传控制器
 */
@RestController
public class FileUploadController {

    @Value("${file.upload.dir:uploads}")
    private String uploadDir;

    @Value("${file.upload.url-prefix:/uploads}")
    private String urlPrefix;

    /**
     * 通用图片上传接口
     */
    @PostMapping("/upload")
    public ApiResult<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ApiResult.fail("请选择要上传的文件");
        }

        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/"))) {
            return ApiResult.fail("只支持上传图片文件");
        }

        try {
            // 生成文件名：日期 + UUID
            String dateStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String newFilename = UUID.randomUUID().toString() + extension;

            // 创建目录
            String fullUploadDir = uploadDir + File.separator + dateStr;
            Path uploadPath = Paths.get(fullUploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 保存文件
            Path filePath = uploadPath.resolve(newFilename);
            Files.copy(file.getInputStream(), filePath);

            // 返回访问URL（前端通过/api代理访问）
            String fileUrl = "/api" + urlPrefix + "/" + dateStr + "/" + newFilename;

            Map<String, String> result = new HashMap<>();
            result.put("url", fileUrl);
            result.put("filename", newFilename);
            result.put("originalName", originalFilename);
            result.put("size", String.valueOf(file.getSize()));

            return ApiResult.success(result);

        } catch (IOException e) {
            return ApiResult.fail("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 管理员图片上传接口
     */
    @PostMapping("/admin/api/upload")
    public ApiResult<Map<String, String>> uploadForAdmin(@RequestParam("file") MultipartFile file) {
        return uploadImage(file);
    }

    /**
     * 运营方图片上传接口
     */
    @PostMapping("/operator/api/upload")
    public ApiResult<Map<String, String>> uploadForOperator(@RequestParam("file") MultipartFile file) {
        return uploadImage(file);
    }

    /**
     * 用户图片上传接口
     */
    @PostMapping("/user/api/upload")
    public ApiResult<Map<String, String>> uploadForUser(@RequestParam("file") MultipartFile file) {
        return uploadImage(file);
    }
}
