package org.example.shop1.controller;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppRegistrationAdminControllerTest {

    @Test
    void csvCellQuotesAndDisarmsFormulas() {
        assertEquals("\"علی, رضایی\"", AppRegistrationAdminController.cell("علی, رضایی"));
        assertEquals("\"a\"\"b\"", AppRegistrationAdminController.cell("a\"b"));
        assertEquals("\"'=HYPERLINK(1)\"", AppRegistrationAdminController.cell("=HYPERLINK(1)"));
        assertEquals("\"'+98\"", AppRegistrationAdminController.cell("+98"));
        assertEquals("", AppRegistrationAdminController.cell(null));
    }

    @Test
    void dateIsJalaliInTehranTime() {
        // ۱ اکتبر ۲۰۲۶، ۲۰:۰۰ UTC = همان روز ۲۳:۳۰ تهران = ۹ مهر ۱۴۰۵
        assertEquals("1405/07/09 23:30", AppRegistrationAdminController.jalali(Instant.parse("2026-10-01T20:00:00Z")));
        // نیم ساعت بعد در UTC، در تهران روز عوض شده
        assertEquals("1405/07/10 00:00", AppRegistrationAdminController.jalali(Instant.parse("2026-10-01T20:30:00Z")));
    }
}
