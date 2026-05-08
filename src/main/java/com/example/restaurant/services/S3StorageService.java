package com.example.restaurant.services;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3StorageService {
  private final S3Client _s3Client;

  @Value("${application.storage.s3.bucket-name}")
  private String bucketName;

  private static final List<String> ALLOWED_MIME_TYPES =
      List.of("image/jpeg", "image/png", "image/webp");

  private static final List<String> ALLOWED_EXTENSIONS = List.of(".jpg", ".jpeg", ".png", ".webp");

  @PostConstruct
  public void initBucket() {
    if (bucketName == null || bucketName.isBlank()) {
      log.error("S3 bucket name is not configured! Storage service will not work properly.");
      return;
    }

    try {
      _s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
      if (log.isInfoEnabled()) {
        log.info("Bucket {} already exists.", bucketName);
      }
    } catch (S3Exception e) {
      if (e instanceof NoSuchBucketException || e.statusCode() == 404) {
        _s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());

        if (log.isInfoEnabled()) {
          log.info("Created bucket: {}", bucketName);
        }

        String policy =
            "{"
                + "\"Version\": \"2012-10-17\","
                + "\"Statement\": [{"
                + "\"Effect\": \"Allow\","
                + "\"Principal\": \"*\","
                + "\"Action\": \"s3:GetObject\","
                + "\"Resource\": \"arn:aws:s3:::"
                + bucketName
                + "/*\""
                + "}]}";

        _s3Client.putBucketPolicy(
            PutBucketPolicyRequest.builder().bucket(bucketName).policy(policy).build());

        log.info("Set public read policy for bucket: {}", bucketName);
      } else {
        if (log.isWarnEnabled()) {
          log.error("Error checking S3 bucket", e);
        }
      }
    }
  }

  public String uploadFromStream(
      InputStream is, String fileName, String contentType, long contentLength) {
    if (is == null) throw new IllegalArgumentException("Input stream cannot be null");
    if (fileName == null || fileName.isBlank())
      throw new IllegalArgumentException("File name cannot be empty");
    if (contentLength <= 0)
      throw new IllegalArgumentException("Content length must be greater than 0");

    if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
      log.warn("Blocked upload attempt with invalid content type: {}", contentType);
      throw new IllegalArgumentException(
          "Invalid file type. Only JPEG, PNG, and WEBP images are allowed.");
    }

    String cleanFileName = fileName.trim().replaceAll("\\s+", "_");

    try {
      PutObjectRequest put =
          PutObjectRequest.builder()
              .bucket(bucketName)
              .key(cleanFileName)
              .contentType(contentType)
              .build();

      _s3Client.putObject(put, RequestBody.fromInputStream(is, contentLength));
      return cleanFileName;
    } catch (S3Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Failed to upload file to S3: {}", e.getMessage());
      }
      throw new IllegalStateException("Could not upload file to cloud storage", e);
    }
  }

  public void deleteFile(String fileName) {
    if (fileName == null || fileName.isBlank()) return;

    String key = extractKeyFromUrl(fileName);

    try {
      _s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(key).build());
      if (log.isInfoEnabled()) {
        log.info("Deleted file from S3: {}", key);
      }
    } catch (S3Exception e) {
      log.error("Failed to delete file {} from S3: {}", key, e.getMessage());
    }
  }

  public String generateUniqFileName(String fileName) {
    if (fileName == null) return UUID.randomUUID() + ".jpg";
    String extension = "";
    int i = fileName.lastIndexOf('.');
    if (i > 0) extension = fileName.substring(i).toLowerCase();

    if (!ALLOWED_EXTENSIONS.contains(extension)) {
      if (log.isWarnEnabled()) {
        log.warn("Blocked upload attempt with invalid extension: {}", extension);
      }
      throw new IllegalArgumentException("Invalid file extension. Allowed: JPG, JPEG, PNG, WEBP.");
    }

    return UUID.randomUUID() + extension;
  }

  private String extractKeyFromUrl(String key) {
    if (key.startsWith("http")) return key.substring(key.lastIndexOf("/") + 1);
    return key;
  }
}
