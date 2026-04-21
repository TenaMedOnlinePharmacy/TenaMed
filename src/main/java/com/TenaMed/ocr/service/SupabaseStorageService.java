package com.TenaMed.ocr.service;

import org.springframework.web.multipart.MultipartFile;

public interface SupabaseStorageService {

    String uploadAndGetSignedUrl(MultipartFile file);

    String uploadAndGetSignedUrl(MultipartFile file, String folder);

    String uploadAndGetPublicUrl(MultipartFile file, String folder);
}
