package com.SxxM.med.ocr.service;

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
        try {
            log.info("Vision API 호출 시작: isBase64={}, imageData 길이={}", isBase64, imageData != null ? imageData.length() : 0);
            
            ImageAnnotatorClient vision = getClient();
            
            ByteString imageBytes;
            
            if (isBase64) {
                // Base64 디코딩
                log.info("Base64 이미지 디코딩 시작");
                byte[] decodedBytes = Base64.getDecoder().decode(imageData);
                imageBytes = ByteString.copyFrom(decodedBytes);
                log.info("Base64 이미지 디코딩 완료: 바이트 크기={}", decodedBytes.length);
            } else {
                // URL 또는 파일 경로에서 이미지 읽기
                byte[] imageBytesArray;
                if (imageData.startsWith("http://") || imageData.startsWith("https://")) {
                    // HTTP URL인 경우
                    log.info("HTTP URL에서 이미지 다운로드: {}", imageData);
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
                    log.info("로컬 파일에서 이미지 읽기: {}", imageData);
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
            
            log.info("Google Vision API 호출 시작");
            BatchAnnotateImagesResponse response = vision.batchAnnotateImages(List.of(request));
            List<AnnotateImageResponse> responses = response.getResponsesList();
            log.info("Google Vision API 호출 완료: 응답 개수={}", responses.size());
            
            if (responses.isEmpty() || !responses.get(0).hasFullTextAnnotation()) {
                log.warn("이미지에서 텍스트를 찾을 수 없습니다");
                return "";
            }
            
            String extractedText = responses.get(0).getFullTextAnnotation().getText();
            log.info("OCR 텍스트 추출 완료: 텍스트 길이={}", extractedText != null ? extractedText.length() : 0);
            
            vision.close();
            return extractedText;
        } catch (com.google.api.gax.rpc.ResourceExhaustedException e) {
            log.error("Google Vision API 할당량 초과 또는 리소스 부족", e);
            throw new RuntimeException("Vision API 할당량이 초과되었거나 리소스가 부족합니다: " + e.getMessage(), e);
        } catch (com.google.api.gax.rpc.PermissionDeniedException e) {
            log.error("Google Vision API 권한 없음: credentialsPath={}", credentialsPath, e);
            throw new RuntimeException("Vision API 권한이 없습니다. GOOGLE_APPLICATION_CREDENTIALS 환경변수를 확인하세요: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Vision API 호출 중 오류 발생", e);
            throw new RuntimeException("이미지 OCR 처리 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }
}

