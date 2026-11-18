package com.atlas.explorer.features.storage.config;

import com.atlas.explorer.features.storage.connector.MinioObjectStorage;
import com.atlas.explorer.features.storage.connector.ObjectStorage;
import com.atlas.explorer.features.storage.service.StorageService;
import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "storage.enabled", havingValue = "true")
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfiguration {
    @Bean(destroyMethod = "close")
    MinioClient minioClient(StorageProperties properties) {
        return client(properties.endpoint(), properties.region(), properties.accessKey(), properties.secretKey());
    }

    static MinioClient client(String endpoint, String region, String accessKey, String secretKey) {
        var client = MinioClient.builder().endpoint(endpoint).region(region).credentials(accessKey, secretKey).build();
        client.setTimeout(5_000, 60_000, 60_000);
        return client;
    }

    @Bean ObjectStorage objectStorage(MinioClient client, StorageProperties properties) {
        return new MinioObjectStorage(client, properties.bucket());
    }

    @Bean StorageService storageService(ObjectStorage storage, StorageProperties properties) {
        return new StorageService(storage, properties.bucket());
    }
}
