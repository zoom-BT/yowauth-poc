package yowyob.comops.api.file.adapter.out.storage;

import java.io.IOException;
import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import yowyob.comops.api.file.application.port.out.FileBinaryStorage;
import yowyob.comops.api.file.config.FileStorageProperties;

@Component
@Profile({"r2dbc", "test-memory"})
@ConditionalOnProperty(prefix = "iwm.file.storage", name = "backend", havingValue = "s3")
public class S3FileBinaryStorage implements FileBinaryStorage {

    private final S3Client client;
    private final String bucket;
    private final String prefix;

    public S3FileBinaryStorage(FileStorageProperties properties) {
        FileStorageProperties.S3 s3 = properties.getS3();
        if (s3.getBucket() == null || s3.getBucket().isBlank()) {
            throw new IllegalArgumentException("iwm.file.storage.s3.bucket is required when file storage backend=s3");
        }
        this.bucket = s3.getBucket().trim();
        this.prefix = sanitizePrefix(s3.getPrefix());
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(s3.getRegion() == null || s3.getRegion().isBlank() ? "us-east-1" : s3.getRegion()))
                .forcePathStyle(s3.isPathStyleAccess());
        if (s3.getEndpoint() != null && !s3.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(s3.getEndpoint().trim()));
        }
        if (s3.getAccessKey() != null && !s3.getAccessKey().isBlank()
                && s3.getSecretKey() != null && !s3.getSecretKey().isBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(s3.getAccessKey(), s3.getSecretKey())));
        }
        this.client = builder.build();
    }

    @Override
    public Mono<String> write(String relativePath, Flux<DataBuffer> content) {
        String cleanPath = normalizeRelativePath(relativePath);
        String key = objectKey(cleanPath);
        return DataBufferUtils.join(content)
                .map(buffer -> {
                    byte[] bytes = new byte[buffer.readableByteCount()];
                    buffer.read(bytes);
                    DataBufferUtils.release(buffer);
                    return bytes;
                })
                .flatMap(bytes -> Mono.fromCallable(() -> {
                    client.putObject(PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build(), RequestBody.fromBytes(bytes));
                    return cleanPath;
                }).subscribeOn(Schedulers.boundedElastic()));
    }

    @Override
    public Mono<Resource> read(String relativePath) {
        String key = objectKey(relativePath);
        return Mono.fromCallable(() -> {
                    ResponseBytes<GetObjectResponse> object = client.getObjectAsBytes(GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build());
                    return (Resource) new ByteArrayResource(object.asByteArray()) {
                        @Override
                        public String getFilename() {
                            return key;
                        }
                    };
                })
                .onErrorMap(error -> error instanceof IOException ? error : new IOException("Stored S3 object not found: " + key, error))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private String objectKey(String relativePath) {
        String clean = normalizeRelativePath(relativePath);
        return prefix.isBlank() ? clean : prefix + "/" + clean;
    }

    private String normalizeRelativePath(String relativePath) {
        String clean = relativePath == null ? "" : relativePath.replace('\\', '/');
        while (clean.startsWith("/")) {
            clean = clean.substring(1);
        }
        if (clean.contains("../") || clean.equals("..")) {
            throw new IllegalArgumentException("File storage path escapes configured S3 prefix.");
        }
        return clean;
    }

    private String sanitizePrefix(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String clean = value.trim().replace('\\', '/');
        while (clean.startsWith("/")) {
            clean = clean.substring(1);
        }
        while (clean.endsWith("/")) {
            clean = clean.substring(0, clean.length() - 1);
        }
        return clean;
    }
}
