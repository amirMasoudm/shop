package org.example.shop1.model.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.shop1.model.entity.Product;
import org.example.shop1.model.entity.RfSpec;
import org.example.shop1.model.reposritory.CategoryRepository;
import org.example.shop1.model.reposritory.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** فیکسچرِ ساختگی؛ دیتابیسِ واقعی فقط دادهٔ بازبینی‌شدهٔ چت ب را می‌گیرد. */
class RfSpecImportServiceTest {

    static final String HEAD = "product_id,holoo_code,name,category,kind,bands_mhz,gain_dbi,antenna_type,diameter_cm,beamwidth_deg,polarization,tx_max_dbm,tx_cond,sens_low_dbm,sens_low_cond,sens_high_dbm,sens_high_cond,compat_family,source_url,source_checked,notes\n";
    static final String PRODUCTS = HEAD
            + "p1,DN-1,\"دیش، ۹۰ سانت\",antenna-dish,ANTENNA,4900-6100,30,DISH,90,±2.5,DUAL,,,,,,,,https://example.com/d,1405/07/10,\"یادداشت\nچندخطی\"\n"
            + "p2,DN-2,رادیو,ubnt,RADIO_INTEGRATED,5150-5875;5900-6100,23,PANEL,,,DUAL,27,MCS0,-96,در منبع نیست,-70,MCS9,UBNT_AIRMAX_AC,https://example.com/r,1405/07/10,\n"
            + "p3,DN-3,بی‌نوع,cambium,,,,,,,,,,,,,,,,1405/07/10,در منبع نیست\n"
            + "ghost,DN-9,ناموجود,x,ANTENNA,4900-6100,30,DISH,,,,,,,,,,,,,\n";
    static final String RATES = "product_id,holoo_code,band_mhz,channel_mhz,rate_label,tx_dbm,sens_dbm,rate_mbps,source_url,notes\n"
            + "p2,DN-2,5150-5875,20,MCS0,27,-96,6.5,https://example.com/r,\n"
            + "p2,DN-2,5 GHz,20,MCS9,22,-70,86.7,https://example.com/r,\n";

    Map<String, Product> db = new HashMap<>();
    ProductRepository repo;
    RfSpecImportService svc;
    ActivityLogService log;

    @BeforeEach
    void setUp() {
        repo = mock(ProductRepository.class);
        log = mock(ActivityLogService.class);
        for (String id : List.of("p1", "p2", "p3")) {
            Product p = new Product();
            p.setId(id);
            p.setName("نامِ سایت " + id);
            p.setOnlinePrice(BigDecimal.valueOf(1234));
            p.setDescription("متن");
            db.put(id, p);
        }
        when(repo.findById(anyString())).thenAnswer(inv -> Optional.ofNullable(db.get((String) inv.getArgument(0))));
        when(repo.save(any(Product.class))).thenAnswer(inv -> { Product p = inv.getArgument(0); db.put(p.getId(), p); return p; });
        ObjectMapper om = new ObjectMapper();
        RfSpecService rf = new RfSpecService(repo, mock(CategoryRepository.class), log, om);
        svc = new RfSpecImportService(repo, rf, log, om);
    }

    RfSpecImportService.Item item(RfSpecImportService.Preview pv, String id) {
        return pv.items().stream().filter(i -> id.equals(i.productId())).findFirst().orElseThrow();
    }

    @Test
    void previewClassifiesEveryRowAndWritesNothing() {
        RfSpecImportService.Preview pv = svc.preview(PRODUCTS, "a.csv", RATES, "b.csv");
        assertEquals("new", item(pv, "p1").status());
        assertEquals("new", item(pv, "p2").status());
        assertEquals("empty", item(pv, "p3").status());
        assertEquals("unmatched", item(pv, "ghost").status());
        verify(repo, never()).save(any());
        // «±2.5» عدد نیست → خالی با هشدار؛ حدس زده نمی‌شود
        assertTrue(item(pv, "p1").warnings().stream().anyMatch(w -> w.contains("±2.5")));
        assertTrue(item(pv, "p2").warnings().stream().anyMatch(w -> w.contains("5 GHz")));
        assertFalse(item(pv, "p1").changes().isEmpty());
    }

    @Test
    void applyWritesOnlyRfAndASecondRunChangesNothing() {
        svc.apply(svc.preview(PRODUCTS, "a.csv", RATES, "b.csv").token());
        Product p2 = db.get("p2");
        assertEquals("نامِ سایت p2", p2.getName());
        assertEquals(BigDecimal.valueOf(1234), p2.getOnlinePrice());
        assertEquals("متن", p2.getDescription());
        RfSpec rf = p2.getRf();
        assertEquals("RADIO_INTEGRATED", rf.getKind());
        assertEquals(2, rf.getBands().size());
        assertEquals(27.0, rf.getRadio().getTxMaxDbm());
        assertNull(rf.getRadio().getSensLowCond(), "«در منبع نیست» خالی است");
        assertEquals(2, rf.getRates().size());
        assertNull(rf.getRates().get(1).getMinMhz());
        assertNull(db.get("p1").getRf().getAntenna().getBeamwidthDeg());
        assertNull(db.get("p3").getRf());

        reset(repo);
        when(repo.findById(anyString())).thenAnswer(inv -> Optional.ofNullable(db.get((String) inv.getArgument(0))));
        RfSpecImportService.Preview again = svc.preview(PRODUCTS, "a.csv", RATES, "b.csv");
        assertEquals("same", item(again, "p1").status());
        assertEquals("same", item(again, "p2").status());
        RfSpecImportService.ApplyResult r = svc.apply(again.token());
        assertEquals(0, r.created() + r.changed());
        verify(repo, never()).save(any());
    }

    @Test
    void oneSummaryLogRecordPerRun() {
        svc.apply(svc.preview(PRODUCTS, "a.csv", RATES, "b.csv").token());
        verify(log, times(1)).record(any(), any(), any(), any(), any(), any(), any(), any());
        verify(log, never()).recordProduct(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void csvHandlesQuotesCommasAndNewlines() {
        List<Map<String, String>> rows = RfSpecImportService.Csv.parse("﻿a,b\r\n\"x, y\",\"he said \"\"hi\"\"\nok\"\r\n");
        assertEquals(1, rows.size());
        assertEquals("x, y", rows.get(0).get("a"));
        assertEquals("he said \"hi\"\nok", rows.get(0).get("b"));
    }

    @Test
    void expiredOrUnknownTokenIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> svc.apply("nope"));
        List<String> w = new ArrayList<>();
        assertTrue(RfSpecImportService.bands("abc", "bands_mhz", w).isEmpty());
        assertEquals(1, w.size());
    }
}
