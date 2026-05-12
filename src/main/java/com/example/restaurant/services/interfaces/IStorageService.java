package com.example.restaurant.services.interfaces;

import java.io.InputStream;

public interface IStorageService {
  String uploadFromStream(InputStream is, String fileName, String contentType, long contentLength);

  void deleteFile(String fileName);

  String generateUniqFileName(String fileName);
}
