package com.example.restaurant.services;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3StorageService {
    private final S3Client _s3Client;
    @Value("${application.storage.s3.bucket-name}")
    private String bucketName;

    @PostConstruct
    public void initBucket() {
        if (bucketName == null || bucketName.isBlank()) {
            log.error("S3 bucket name is not configured! Storage service will not work properly.");
            return;
        }

        try {
            _s3Client.headBucket(HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build()
            );
            log.info("Bucket {} already exists.", bucketName);
        } catch (S3Exception e) {
            if (e instanceof NoSuchBucketException || e.statusCode() == 404) {
                _s3Client.createBucket(CreateBucketRequest.builder()
                        .bucket(bucketName)
                        .build()
                );

                log.info("Created bucket: {}", bucketName);

                String policy = "{" +
                        "\"Version\": \"2012-10-17\"," +
                        "\"Statement\": [{" +
                        "\"Effect\": \"Allow\"," +
                        "\"Principal\": \"*\"," +
                        "\"Action\": \"s3:GetObject\"," +
                        "\"Resource\": \"arn:aws:s3:::" + bucketName + "/*\"" +
                        "}]}";

                _s3Client.putBucketPolicy(PutBucketPolicyRequest.builder()
                        .bucket(bucketName)
                        .policy(policy)
                        .build()
                );

                log.info("Set public read policy for bucket: {}", bucketName);
            } else {
                log.error("Error checking S3 bucket", e);
            }
        }
    }

    public void uploadFromStream(
            InputStream is,
            String fileName,
            String contentType,
            long contentLength
    ) {
        if (is == null) throw new IllegalArgumentException("Input stream cannot be null");
        if (fileName == null || fileName.isBlank()) throw new IllegalArgumentException("File name cannot be empty");
        if (contentLength <= 0) throw new IllegalArgumentException("Content length must be greater than 0");

        String cleanFileName = fileName.trim().replaceAll("\\s+", "_");

        try {
            PutObjectRequest put = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(cleanFileName)
                    .contentType(contentType)
                    .build();

            _s3Client.putObject(put, RequestBody.fromInputStream(is, contentLength));
        } catch (S3Exception e) {
            log.error("Failed to upload file to S3: {}", e.getMessage());
            throw new IllegalStateException("Could not upload file to cloud storage", e);
        }
    }
}
