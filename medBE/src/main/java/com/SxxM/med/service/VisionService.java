package com.SxxM.med.service;

import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class VisionService {
    
    private final WebClient.Builder webClientBuilder;
    
    @Value("${google.vision.credentials.path:}")
    private String credentialsPath;
    
    private ImageAnnotatorClient getClient() throws IOException {
        if (credentialsPath != null && !credentialsPath.isEmpty()) {
            System.setProperty("GOOGLE_APPLICATION_CREDENTIALS", credentialsPath);
        }
        return ImageAnnotatorClient.create();
    }
    
    public String extractTextFromImage(String imageData, boolean isBase64) {
        try (ImageAnnotatorClient vision = getClient()) {
            ByteString imageBytes;
            
            if (isBase64) {
                // Base64 디코딩
                byte[] decodedBytes = Base64.getDecoder().decode(imageData);
                imageBytes = ByteString.copyFrom(decodedBytes);
            } else {
                // URL 또는 파일 경로에서 이미지 읽기
                byte[] imageBytesArray;
                if (imageData.startsWith("http://") || imageData.startsWith("https://")) {
                    // HTTP URL인 경우
                    WebClient webClient = webClientBuilder.build();
                    imageBytesArray = webClient
                            .get()
                            .uri(URI.create(imageData))
                            .retrieve()
                            .bodyToMono(byte[].class)
                            .block();
                    if (imageBytesArray == null) {
                        throw new RuntimeException("이미지를 다운로드할 수 없습니다: " + imageData);
                    }
                } else {
                    // 로컬 파일 경로인 경우
                    imageBytesArray = Files.readAllBytes(Paths.get(imageData));
                }
                imageBytes = ByteString.copyFrom(imageBytesArray);
            }
            
            Image img = Image.newBuilder().setContent(imageBytes).build();
            Feature feat = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
            AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                    .addFeatures(feat)
                    .setImage(img)
                    .build();
            
            BatchAnnotateImagesResponse response = vision.batchAnnotateImages(List.of(request));
            List<AnnotateImageResponse> responses = response.getResponsesList();
            
            if (responses.isEmpty() || !responses.get(0).hasFullTextAnnotation()) {
                log.warn("이미지에서 텍스트를 찾을 수 없습니다");
                return "";
            }
            
            return responses.get(0).getFullTextAnnotation().getText();
        } catch (Exception e) {
            log.error("Vision API 호출 중 오류 발생", e);
            throw new RuntimeException("이미지 OCR 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
}

