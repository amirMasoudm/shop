package org.example.shop1.model.service.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ارتباطِ SOAPِ خام با درگاهِ بهپرداختِ ملت — بدونِ dependencyِ WSDL-codegen (طبقِ پرامپت:
 * ابزارهایِ سنگینِ SOAP با مخزنِ سفارشیِ پروژه ({@code mvnhub.ir}) ممکن است در دسترس نباشند؛
 * envelope دستی ساخته و با {@link WebClient}ِ موجود فرستاده می‌شود). پاسخِ ملت هم معمولاً یک
 * رشتهٔ سادهٔ کاما-جدا داخلِ تگِ {@code <return>} است، نه XMLِ پیچیده.
 * <p>
 * این کلاس فقط پروتکل را می‌داند؛ هیچ منطقِ سفارش/دیتابیسی اینجا نیست (آن در
 * {@code OrderService} است) — تا بشود مستقل تست/جایگزین کرد.
 */
@Service
public class MellatGatewayService {

    private static final Logger log = LoggerFactory.getLogger(MellatGatewayService.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(25);
    private static final Pattern RETURN_PATTERN =
            Pattern.compile("<(?:\\w+:)?return>(.*?)</(?:\\w+:)?return>", Pattern.DOTALL);

    // عمداً String نه long: در dev مقدارش می‌تواند خالی باشد (${MELLAT_TERMINAL_ID:})
    // و تبدیلِ خودکارِ Springِ رشته‌ی خالی به long موقعِ بالاآمدنِ اپ کرش می‌کند.
    private final String terminalId;
    private final String username;
    private final String password;
    private final WebClient client;

    public MellatGatewayService(
            @Value("${mellat.terminal-id:}") String terminalId,
            @Value("${mellat.username:}") String username,
            @Value("${mellat.password:}") String password,
            // قابلِ‌کانفیگ تا بشود در تست به یک استابِ محلی اشاره‌اش داد؛ پیش‌فرض آدرسِ رسمیِ ملت است.
            @Value("${mellat.gateway-url:https://bpm.shaparak.ir/pgwchannel/services/pgw}") String gatewayUrl) {
        this.terminalId = terminalId;
        this.username = username;
        this.password = password;
        this.client = WebClient.builder().baseUrl(gatewayUrl).build();
    }

    /** آیا کلیدهایِ ملت در env تنظیم شده‌اند؟ برایِ رفتارِ روشن به‌جایِ خطایِ گنگ در dev. */
    public boolean isConfigured() {
        return terminalId != null && !terminalId.isBlank()
                && username != null && !username.isBlank()
                && password != null && !password.isBlank();
    }

    /** نتیجهٔ خامِ bpPayRequest. {@code refId} فقط وقتی resCode="0" معنا دارد. */
    public record PayResult(String resCode, String refId) {
        public boolean success() {
            return "0".equals(resCode);
        }
    }

    public PayResult pay(long orderId, long amountRial, String callBackUrl) {
        if (!isConfigured()) {
            throw new MellatGatewayException("کلیدهایِ درگاهِ ملت تنظیم نشده‌اند (MELLAT_TERMINAL_ID/USERNAME/PASSWORD)");
        }
        String inner = ("""
                <terminalId>%s</terminalId>
                <userName>%s</userName>
                <userPassword>%s</userPassword>
                <orderId>%d</orderId>
                <amount>%d</amount>
                <localDate>%s</localDate>
                <localTime>%s</localTime>
                <additionalData></additionalData>
                <callBackUrl>%s</callBackUrl>
                <payerId>0</payerId>
                """).formatted(esc(terminalId), esc(username), esc(password), orderId, amountRial,
                JalaliDateUtil.localDate(), JalaliDateUtil.localTime(), esc(callBackUrl));

        String ret = extractReturn(send("bpPayRequest", inner));
        String[] parts = ret.split(",", 2);
        String resCode = parts[0].trim();
        String refId = parts.length > 1 ? parts[1].trim() : null;
        log.info("bpPayRequest برایِ orderId={} → ResCode={}", orderId, resCode);
        return new PayResult(resCode, refId);
    }

    /** فقط ResCode را برمی‌گرداند (رشته، مثلِ "0" یا "41"). */
    public String verify(long orderId, long saleOrderId, long saleReferenceId) {
        String resCode = call6("bpVerifyRequest", orderId, saleOrderId, saleReferenceId);
        log.info("bpVerifyRequest برایِ orderId={} → ResCode={}", orderId, resCode);
        return resCode;
    }

    public String settle(long orderId, long saleOrderId, long saleReferenceId) {
        String resCode = call6("bpSettleRequest", orderId, saleOrderId, saleReferenceId);
        log.info("bpSettleRequest برایِ orderId={} → ResCode={}", orderId, resCode);
        return resCode;
    }

    public String reversal(long orderId, long saleOrderId, long saleReferenceId) {
        String resCode = call6("bpReversalRequest", orderId, saleOrderId, saleReferenceId);
        log.warn("bpReversalRequest برایِ orderId={} → ResCode={}", orderId, resCode);
        return resCode;
    }

    private String call6(String operation, long orderId, long saleOrderId, long saleReferenceId) {
        if (!isConfigured()) {
            throw new MellatGatewayException("کلیدهایِ درگاهِ ملت تنظیم نشده‌اند (MELLAT_TERMINAL_ID/USERNAME/PASSWORD)");
        }
        String inner = ("""
                <terminalId>%s</terminalId>
                <userName>%s</userName>
                <userPassword>%s</userPassword>
                <orderId>%d</orderId>
                <saleOrderId>%d</saleOrderId>
                <saleReferenceId>%d</saleReferenceId>
                """).formatted(esc(terminalId), esc(username), esc(password), orderId, saleOrderId, saleReferenceId);
        return extractReturn(send(operation, inner)).trim();
    }

    private String send(String operation, String innerFields) {
        String envelope = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:ns=\"http://interfaces.core.sw.bps.com/\">"
                + "<soapenv:Header/><soapenv:Body><ns:" + operation + ">"
                + innerFields
                + "</ns:" + operation + "></soapenv:Body></soapenv:Envelope>";

        try {
            String response = client.post()
                    .contentType(MediaType.valueOf("text/xml;charset=UTF-8"))
                    .header("SOAPAction", "")
                    .bodyValue(envelope)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(TIMEOUT)
                    .block();
            if (response == null) {
                throw new MellatGatewayException("پاسخِ خالی از درگاهِ ملت برایِ " + operation);
            }
            return response;
        } catch (MellatGatewayException e) {
            throw e;
        } catch (Exception e) {
            log.error("خطایِ ارتباطی با درگاهِ ملت ({}): {}", operation, e.toString());
            throw new MellatGatewayException("ارتباط با درگاهِ پرداخت برقرار نشد (" + operation + ")", e);
        }
    }

    private String extractReturn(String xml) {
        Matcher m = RETURN_PATTERN.matcher(xml);
        if (!m.find()) {
            throw new MellatGatewayException("فرمتِ پاسخِ درگاه نامعتبر است: " + xml);
        }
        return m.group(1);
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
