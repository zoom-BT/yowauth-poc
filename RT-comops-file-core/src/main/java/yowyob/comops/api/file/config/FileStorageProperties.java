package yowyob.comops.api.file.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "iwm.file.storage")
public class FileStorageProperties {
    private String backend = "local";
    private String rootPath = "./data/files";
    private long maxFileSizeBytes = 10 * 1024 * 1024;
    private List<String> allowedContentTypes = new ArrayList<>();
    /**
     * Quand true, chaque fichier uploadé part en quarantaine (PENDING) et n'est téléchargeable
     * qu'après le verdict ACCEPTED du service d'analyse externe. Quand false (défaut), aucun
     * service d'analyse n'est requis : les fichiers sont immédiatement ACCEPTED.
     */
    private boolean analysisEnabled = false;
    private final S3 s3 = new S3();

    public String getBackend() {
        return backend;
    }

    public void setBackend(String backend) {
        this.backend = backend == null || backend.isBlank() ? "local" : backend.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public List<String> getAllowedContentTypes() {
        return allowedContentTypes;
    }

    public void setAllowedContentTypes(List<String> allowedContentTypes) {
        this.allowedContentTypes = allowedContentTypes == null ? new ArrayList<>() : new ArrayList<>(allowedContentTypes);
    }

    public boolean isAnalysisEnabled() {
        return analysisEnabled;
    }

    public void setAnalysisEnabled(boolean analysisEnabled) {
        this.analysisEnabled = analysisEnabled;
    }

    public S3 getS3() {
        return s3;
    }

    public static class S3 {
        private String endpoint;
        private String region = "us-east-1";
        private String bucket;
        private String accessKey;
        private String secretKey;
        private String prefix = "";
        private boolean pathStyleAccess = true;

        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public String getBucket() { return bucket; }
        public void setBucket(String bucket) { this.bucket = bucket; }
        public String getAccessKey() { return accessKey; }
        public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
        public String getPrefix() { return prefix; }
        public void setPrefix(String prefix) { this.prefix = prefix == null ? "" : prefix.trim(); }
        public boolean isPathStyleAccess() { return pathStyleAccess; }
        public void setPathStyleAccess(boolean pathStyleAccess) { this.pathStyleAccess = pathStyleAccess; }
    }
}
