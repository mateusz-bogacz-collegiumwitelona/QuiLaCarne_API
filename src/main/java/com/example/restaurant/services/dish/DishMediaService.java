package com.example.restaurant.services.dish;

import com.example.restaurant.exceptions.FileProcessingException;
import com.example.restaurant.models.Dishes;
import com.example.restaurant.services.interfaces.IStorageService;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class DishMediaService {
  private final IStorageService _s3Services;

  @Value("${application.storage.s3.public-endpoint}")
  private String s3Endpoint;

  @Value("${application.storage.s3.bucket-name}")
  private String s3BucketName;

  public void updateDishPhoto(Dishes dish, MultipartFile photo) {
    if (photo != null && !photo.isEmpty()) {
      if (dish.getImageUrl() != null) _s3Services.deleteFile(dish.getImageUrl());

      String generatedName = _s3Services.generateUniqFileName(photo.getOriginalFilename());

      try {
        String finalFileName =
            _s3Services.uploadFromStream(
                photo.getInputStream(), generatedName, photo.getContentType(), photo.getSize());
        dish.setImageUrl(finalFileName);
      } catch (IOException e) {
        log.error("Error reading photo input stream", e);
        throw new FileProcessingException("Could not process photo file", e);
      }
    }
  }

  public String getFullImageUrl(String imageFileName) {
    if (imageFileName == null || imageFileName.isBlank()) return "";

    if (imageFileName.toLowerCase().startsWith("http://")
        || imageFileName.toLowerCase().startsWith("https://")) {
      return imageFileName;
    }

    if (s3Endpoint == null || s3Endpoint.isBlank() || s3BucketName == null) {
      log.error("S3 storage is not properly configured.");
      return "";
    }

    return String.format("%s/%s/%s", s3Endpoint.trim(), s3BucketName, imageFileName);
  }
}
