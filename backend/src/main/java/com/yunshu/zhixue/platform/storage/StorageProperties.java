package com.yunshu.zhixue.platform.storage;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("yunshu.storage")
public record StorageProperties(@NotBlank String endpoint, @NotBlank String accessKey,
        @NotBlank String secretKey, @NotBlank String bucket, @NotBlank String region) {
    @Override
    public String toString() { return "StorageProperties[credentials=REDACTED]"; }
}
