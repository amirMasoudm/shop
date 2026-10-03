package org.example.shop1.model.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;

/**
 * اعتبارسنجیِ توکنِ JWTِ ترب (الگوریتم EdDSA / ed25519).
 * <p>
 * عمداً بدونِ کتابخانهٔ جدید (jjwt/BouncyCastle) نوشته شده: از Java 15 به بعد خودِ JCA
 * الگوریتمِ {@code Ed25519} را بومی دارد و ما روی Java 17 هستیم. توکن دستی به سه بخشِ
 * base64url شکسته می‌شود و امضا روی {@code header.payload} بررسی می‌شود.
 * <p>
 * چهار اعتبارسنجیِ الزامیِ مستنداتِ ترب: صحتِ امضا، {@code exp}، {@code nbf} و
 * {@code aud} (که باید با hostnameِ خودِ ما یکی باشد — مهم‌ترین بخشِ امنیتی، چون بدونش
 * توکنی که ترب برای سایتِ دیگری صادر کرده روی سایتِ ما هم می‌پذیرفت).
 */
@Service
public class TorobTokenService {

    private static final Logger log = LoggerFactory.getLogger(TorobTokenService.class);

    /** رواداریِ کوچک برای اختلافِ ساعتِ سرورها (ثانیه). */
    private static final long CLOCK_SKEW_SECONDS = 60;

    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${torob.public-key:}")
    private String publicKeyBase64;

    /** نتیجهٔ اعتبارسنجی — بدونِ افشایِ جزئیات به بیرون، فقط برای لاگِ داخلی. */
    public record Result(boolean valid, String reason) {
        public static Result ok() { return new Result(true, null); }
        public static Result fail(String reason) { return new Result(false, reason); }
    }

    /**
     * @param token   محتوایِ هدرِ {@code X-Torob-Token}
     * @param ourHost hostnameِ خودمان (از هدرِ {@code Host}) که باید با {@code aud} یکی باشد
     */
    public Result verify(String token, String ourHost) {
        if (publicKeyBase64 == null || publicKeyBase64.isBlank()) {
            return Result.fail("کلیدِ عمومیِ ترب تنظیم نشده است (torob.public-key)");
        }
        if (token == null || token.isBlank()) {
            return Result.fail("توکن ارسال نشده");
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return Result.fail("ساختارِ توکن معتبر نیست");
        }

        try {
            // ۱. صحتِ امضا روی «header.payload»
            byte[] signedData = (parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII);
            byte[] signature = Base64.getUrlDecoder().decode(parts[2]);

            PublicKey publicKey = KeyFactory.getInstance("Ed25519")
                    .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyBase64)));

            Signature verifier = Signature.getInstance("Ed25519");
            verifier.initVerify(publicKey);
            verifier.update(signedData);
            if (!verifier.verify(signature)) {
                return Result.fail("امضا معتبر نیست");
            }

            // ۲–۴. claimها
            JsonNode claims = mapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
            long now = Instant.now().getEpochSecond();

            if (claims.hasNonNull("exp") && now > claims.get("exp").asLong() + CLOCK_SKEW_SECONDS) {
                return Result.fail("توکن منقضی شده");
            }
            if (claims.hasNonNull("nbf") && now + CLOCK_SKEW_SECONDS < claims.get("nbf").asLong()) {
                return Result.fail("زمانِ اعتبارِ توکن هنوز نرسیده");
            }

            String aud = claims.hasNonNull("aud") ? claims.get("aud").asText() : null;
            if (aud == null || aud.isBlank()) {
                return Result.fail("aud در توکن نیست");
            }
            // هدرِ Host ممکن است پورت داشته باشد؛ فقط hostname مقایسه می‌شود
            String host = ourHost == null ? "" : ourHost.split(":")[0].trim();
            String audHost = aud.replaceFirst("^https?://", "").split("/")[0].split(":")[0].trim();
            if (host.isEmpty() || !audHost.equalsIgnoreCase(host)) {
                return Result.fail("aud با hostnameِ ما یکی نیست (aud=" + audHost + ", host=" + host + ")");
            }

            return Result.ok();

        } catch (IllegalArgumentException e) {
            return Result.fail("base64ِ توکن خراب است");
        } catch (Exception e) {
            log.warn("خطا در اعتبارسنجیِ توکنِ ترب: {}", e.toString());
            return Result.fail("خطا در اعتبارسنجی");
        }
    }
}
