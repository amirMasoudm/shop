package org.example.shop1.model.service.payment;

/** هر شکستِ ارتباطی/پروتکلی با درگاهِ ملت (نه شکستِ خودِ تراکنش — آن با ResCode گزارش می‌شود). */
public class MellatGatewayException extends RuntimeException {
    public MellatGatewayException(String message) {
        super(message);
    }

    public MellatGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
