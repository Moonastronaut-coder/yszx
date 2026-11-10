package com.yunshu.zhixue.platform.storage;

import io.minio.MinioClient;
import java.time.Duration;
import okhttp3.OkHttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfiguration {
    @Bean
    MinioClient minioClient(StorageProperties properties) {
        return MinioClient.builder().endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey()).region(properties.region())
                .httpClient(new OkHttpClient.Builder().connectTimeout(Duration.ofSeconds(3))
                        .readTimeout(Duration.ofSeconds(5)).callTimeout(Duration.ofSeconds(8)).build())
                .build();
    }
}
