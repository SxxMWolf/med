FROM nginx:alpine

# Nginx 설정 파일 복사
COPY nginx.conf /etc/nginx/conf.d/default.conf

# SSL 인증서 디렉토리 생성
RUN mkdir -p /etc/nginx/ssl /var/www/certbot

# 포트 노출
EXPOSE 80 443

# Nginx 실행
CMD ["nginx", "-g", "daemon off;"]

