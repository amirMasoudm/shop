package org.example.shop1.controller;

import java.util.Map;

/** دسترسیِ تست به اعتبارسنجیِ پارامترهای {@code rf/suggest} (package-private). */
public final class AppRfControllerAccess {
    private AppRfControllerAccess() {}

    public static void parse(Map<String, String> q) {
        AppRfController.num(q, "fMhz", 2000, 80000);
        AppRfController.num(q, "gainA", 0, 80);
        AppRfController.num(q, "gainB", 0, 80);
        AppRfController.num(q, "marginDb", -400, 400);
        AppRfController.num(q, "txDbm", -30, 50);
        AppRfController.num(q, "sensDbm", -130, -20);
        AppRfController.weather(q);
    }
}
