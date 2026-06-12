package com.foodmap.common.storage.minio;

import com.foodmap.common.storage.ObjectStorageException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;

/**
 * MinIO/S3 兼容 HTTP 网关实现，通过 AWS Signature V4 发起 path-style PUT Object 请求。
 */
public class MinioHttpObjectStorageGateway implements MinioObjectStorageGateway {
    private static final String ALGORITHM = "AWS4-HMAC-SHA256";
    private static final String SERVICE = "s3";
    private static final String TERMINATOR = "aws4_request";
    private static final String UNSIGNED_PAYLOAD = "UNSIGNED-PAYLOAD";
    private static final DateTimeFormatter AMZ_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter DATE_STAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")
            .withZone(ZoneOffset.UTC);

    private final HttpClient httpClient;
    private final MinioObjectStorageProperties properties;
    private final Clock clock;

    /**
     * 创建 MinIO HTTP 网关。
     *
     * @param httpClient Java HTTP 客户端。
     * @param properties MinIO 对象存储配置。
     */
    public MinioHttpObjectStorageGateway(
            HttpClient httpClient,
            MinioObjectStorageProperties properties
    ) {
        this(httpClient, properties, Clock.systemUTC());
    }

    /**
     * 创建可注入时钟的 MinIO HTTP 网关，主要用于签名单元测试。
     *
     * @param httpClient Java HTTP 客户端。
     * @param properties MinIO 对象存储配置。
     * @param clock 当前时间来源。
     */
    MinioHttpObjectStorageGateway(
            HttpClient httpClient,
            MinioObjectStorageProperties properties,
            Clock clock
    ) {
        this.httpClient = httpClient;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * 上传对象到 MinIO。
     *
     * @param bucketName bucket 名称。
     * @param objectKey 对象 Key。
     * @param contentType 内容类型。
     * @param contentLength 内容长度。
     * @param inputStream 对象内容流。
     * @throws Exception HTTP 调用失败或 MinIO 返回非 2xx。
     */
    @Override
    public void putObject(
            String bucketName,
            String objectKey,
            String contentType,
            long contentLength,
            InputStream inputStream
    ) throws Exception {
        HttpRequest request = buildPutObjectRequest(bucketName, objectKey, contentType, contentLength, inputStream);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ObjectStorageException("MinIO object upload failed, status=" + response.statusCode());
        }
    }

    /**
     * 构建 MinIO PUT Object HTTP 请求。
     *
     * @param bucketName bucket 名称。
     * @param objectKey 对象 Key。
     * @param contentType 内容类型。
     * @param contentLength 内容长度。
     * @param inputStream 对象内容流。
     * @return 已完成 Signature V4 签名的 HTTP 请求。
     */
    private HttpRequest buildPutObjectRequest(
            String bucketName,
            String objectKey,
            String contentType,
            long contentLength,
            InputStream inputStream
    ) throws IOException {
        byte[] body = inputStream.readAllBytes();
        if (body.length != contentLength) {
            throw new ObjectStorageException("MinIO object upload length mismatch");
        }
        URI endpoint = URI.create(trimTrailingSlash(properties.getEndpoint()));
        String canonicalUri = "/" + encodePathSegment(bucketName) + "/" + encodeObjectKey(objectKey);
        URI objectUri = endpoint.resolve(canonicalUri);
        Instant now = clock.instant();
        String amzDate = AMZ_DATE_FORMATTER.format(now);
        String dateStamp = DATE_STAMP_FORMATTER.format(now);
        String host = hostHeader(objectUri);
        String signedHeaders = "content-length;content-type;host;x-amz-content-sha256;x-amz-date";
        String canonicalHeaders = "content-length:" + contentLength + "\n"
                + "content-type:" + contentType + "\n"
                + "host:" + host + "\n"
                + "x-amz-content-sha256:" + UNSIGNED_PAYLOAD + "\n"
                + "x-amz-date:" + amzDate + "\n";
        String canonicalRequest = "PUT\n"
                + canonicalUri + "\n"
                + "\n"
                + canonicalHeaders + "\n"
                + signedHeaders + "\n"
                + UNSIGNED_PAYLOAD;
        String credentialScope = dateStamp + "/" + properties.getRegion() + "/" + SERVICE + "/" + TERMINATOR;
        String stringToSign = ALGORITHM + "\n"
                + amzDate + "\n"
                + credentialScope + "\n"
                + sha256Hex(canonicalRequest);
        String signature = hmacHex(signingKey(dateStamp), stringToSign);
        String authorization = ALGORITHM
                + " Credential=" + properties.getAccessKey() + "/" + credentialScope
                + ", SignedHeaders=" + signedHeaders
                + ", Signature=" + signature;

        return HttpRequest.newBuilder()
                .uri(objectUri)
                .timeout(properties.getRequestTimeout())
                .header("Content-Type", contentType)
                .header("X-Amz-Content-Sha256", UNSIGNED_PAYLOAD)
                .header("X-Amz-Date", amzDate)
                .header("Authorization", authorization)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();
    }

    /**
     * 生成 AWS Signature V4 签名密钥。
     *
     * @param dateStamp 日期戳。
     * @return 签名密钥。
     */
    private byte[] signingKey(String dateStamp) {
        byte[] dateKey = hmacBytes(("AWS4" + properties.getSecretKey()).getBytes(StandardCharsets.UTF_8), dateStamp);
        byte[] regionKey = hmacBytes(dateKey, properties.getRegion());
        byte[] serviceKey = hmacBytes(regionKey, SERVICE);
        return hmacBytes(serviceKey, TERMINATOR);
    }

    /**
     * 计算 HMAC-SHA256 并返回十六进制字符串。
     *
     * @param key 签名密钥。
     * @param value 待签名文本。
     * @return 十六进制签名。
     */
    private String hmacHex(byte[] key, String value) {
        return HexFormat.of().formatHex(hmacBytes(key, value));
    }

    /**
     * 计算 HMAC-SHA256 原始字节。
     *
     * @param key 签名密钥。
     * @param value 待签名文本。
     * @return 签名字节。
     */
    private byte[] hmacBytes(byte[] key, String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new ObjectStorageException("MinIO signature calculation failed", ex);
        }
    }

    /**
     * 计算 SHA-256 十六进制摘要。
     *
     * @param value 待摘要文本。
     * @return 十六进制摘要。
     */
    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new ObjectStorageException("MinIO canonical request digest failed", ex);
        }
    }

    /**
     * 编码对象 Key，保留路径分隔符。
     *
     * @param objectKey 对象 Key。
     * @return URI 路径安全的对象 Key。
     */
    private String encodeObjectKey(String objectKey) {
        String[] segments = objectKey.split("/", -1);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                builder.append('/');
            }
            builder.append(encodePathSegment(segments[i]));
        }
        return builder.toString();
    }

    /**
     * 编码 URI path 段。
     *
     * @param value 原始 path 段。
     * @return 编码后的 path 段。
     */
    private String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("%7E", "~");
    }

    /**
     * 构造 Host 请求头，保留非默认端口。
     *
     * @param uri 对象 URI。
     * @return Host 请求头。
     */
    private String hostHeader(URI uri) {
        int port = uri.getPort();
        boolean defaultPort = port == -1
                || ("http".equalsIgnoreCase(uri.getScheme()) && port == 80)
                || ("https".equalsIgnoreCase(uri.getScheme()) && port == 443);
        if (defaultPort) {
            return uri.getHost().toLowerCase(Locale.ROOT);
        }
        return uri.getHost().toLowerCase(Locale.ROOT) + ":" + port;
    }

    /**
     * 去除结尾斜杠，避免 endpoint 和对象路径拼接出现重复分隔符。
     *
     * @param value 原始 endpoint。
     * @return 不带结尾斜杠的 endpoint。
     */
    private String trimTrailingSlash(String value) {
        String result = value;
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
