package com.atlas.explorer.features.storage.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.admin.MinioAdminClient;
import io.minio.admin.Status;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

/** Local bootstrap only. Root credentials are never injected into the API. */
@Component
@ConditionalOnProperty(name = "storage.provision", havingValue = "true")
@EnableConfigurationProperties(StorageProperties.class)
public class StorageProvisioner implements ApplicationRunner {
    private final StorageProperties properties;
    private final ConfigurableApplicationContext context;

    public StorageProvisioner(StorageProperties properties, ConfigurableApplicationContext context) {
        this.properties = properties; this.context = context;
    }

    public void run(ApplicationArguments args) {
        var env = context.getEnvironment();
        String rootUser = env.getRequiredProperty("MINIO_ROOT_USER");
        String rootPassword = env.getRequiredProperty("MINIO_ROOT_PASSWORD");
        try (var client = StorageConfiguration.client(properties.endpoint(), properties.region(), rootUser, rootPassword)) {
            if (properties.accessKey().equals(rootUser)) throw new IllegalArgumentException();
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(properties.bucket()).build())) {
                client.makeBucket(MakeBucketArgs.builder().bucket(properties.bucket()).region(properties.region()).build());
            }
            var admin = MinioAdminClient.builder().endpoint(properties.endpoint()).region(properties.region())
                    .credentials(rootUser, rootPassword).build();
            admin.setTimeout(5_000, 30_000, 30_000);
            String policyName = "atlas-" + properties.bucket();
            admin.addCannedPolicy(policyName, """
                {"Version":"2012-10-17","Statement":[
                  {"Effect":"Allow","Action":["s3:GetBucketLocation","s3:ListBucket","s3:ListBucketMultipartUploads"],
                   "Resource":["arn:aws:s3:::%s"]},
                  {"Effect":"Allow","Action":["s3:GetObject","s3:PutObject","s3:DeleteObject","s3:AbortMultipartUpload","s3:ListMultipartUploadParts"],
                   "Resource":["arn:aws:s3:::%s/*"]}
                ]}
                """.formatted(properties.bucket(), properties.bucket()));
            admin.addUser(properties.accessKey(), Status.ENABLED, properties.secretKey(), null, null);
            // Replacement is idempotent, unlike attaching an already attached policy.
            admin.setPolicy(properties.accessKey(), false, policyName);
            System.out.println("Storage bucket and restricted application account ready.");
        } catch (Exception exception) {
            throw new IllegalStateException("Storage provisioning failed; check MinIO readiness and local configuration.");
        }
        context.close();
    }
}
