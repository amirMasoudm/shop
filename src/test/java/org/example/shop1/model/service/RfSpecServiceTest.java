package org.example.shop1.model.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.shop1.controller.AppRfControllerAccess;
import org.example.shop1.exeption.AppApiException;
import org.example.shop1.model.entity.Product;
import org.example.shop1.model.entity.RfSpec;
import org.example.shop1.model.reposritory.CategoryRepository;
import org.example.shop1.model.reposritory.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** قاعدهٔ {@code rf/suggest} طبقِ قراردادِ نسخهٔ ۲ — فیکسچرِ ساختگی، نه دادهٔ دیتابیس. */
class RfSpecServiceTest {

    private static final double T = 10;

    static Product antenna(String id, double gain, int lo, int hi, int stock, Integer price) {
        Product p = base(id, stock, price);
        RfSpec rf = new RfSpec();
        rf.setKind("ANTENNA");
        rf.setBands(List.of(new RfSpec.Band(lo, hi)));
        RfSpec.Antenna a = new RfSpec.Antenna();
        a.setGainDbi(gain);
        rf.setAntenna(a);
        p.setRf(rf);
        return p;
    }

    static Product radio(String id, String kind, double tx, double sensLow, Double integratedGain, int stock) {
        Product p = base(id, stock, 1000);
        RfSpec rf = new RfSpec();
        rf.setKind(kind);
        rf.setBands(List.of(new RfSpec.Band(4900, 6100)));
        RfSpec.Radio r = new RfSpec.Radio();
        r.setTxMaxDbm(tx);
        r.setSensLowDbm(sensLow);
        rf.setRadio(r);
        if (integratedGain != null) {
            RfSpec.Antenna a = new RfSpec.Antenna();
            a.setGainDbi(integratedGain);
            rf.setAntenna(a);
        }
        p.setRf(rf);
        return p;
    }

    static Product base(String id, int stock, Integer price) {
        Product p = new Product();
        p.setId(id);
        p.setName(id);
        p.setStock(stock);
        if (price != null) p.setOnlinePrice(BigDecimal.valueOf(price));
        return p;
    }

    static RfSpecService.SuggestQuery q(double f, double margin, boolean weather) {
        return new RfSpecService.SuggestQuery(f, 32, 32, margin, 20, -75, weather);
    }

    static List<String> ids(List<Product> ps) {
        return ps.stream().map(Product::getId).toList();
    }

    @Test
    void bandEdgesAreInclusive() {
        List<Product> c = List.of(antenna("a", 32, 5000, 6000, 1, 100));
        assertEquals(List.of("a"), ids(RfSpecService.suggest(c, q(5000, 25, false), T).antennasA()));
        assertEquals(List.of("a"), ids(RfSpecService.suggest(c, q(6000, 25, false), T).antennasA()));
        assertTrue(RfSpecService.suggest(c, q(4999, 25, false), T).antennasA().isEmpty());
        assertTrue(RfSpecService.suggest(c, q(6001, 25, false), T).antennasA().isEmpty());
    }

    @Test
    void minGainSplitsTheDeficitAndIgnoresSurplus() {
        // حاشیه ۲۵ > ۱۰: کسری صفر
        RfSpecService.Suggestion s = RfSpecService.suggest(List.of(), q(5500, 25, false), T);
        assertEquals(32, s.minGainA());
        assertEquals(32, s.minGainB());
        // حاشیه ۴ < ۱۰: کسریِ ۶ نصف می‌شود → +۳ برای هر سر
        s = RfSpecService.suggest(List.of(), q(5500, 4, false), T);
        assertEquals(35, s.minGainA());
        // حاشیهٔ منفی
        s = RfSpecService.suggest(List.of(), q(5500, -6, false), T);
        assertEquals(40, s.minGainA());
    }

    @Test
    void antennaToleranceIsHalfDbAndSmallestSufficientGainComesFirst() {
        List<Product> c = List.of(antenna("g30", 30, 4900, 6100, 1, 100),
                antenna("g34_5", 34.5, 4900, 6100, 1, 100), antenna("g36", 36, 4900, 6100, 1, 50),
                antenna("g34_4", 34.4, 4900, 6100, 1, 100));
        // minGain = 32 + (10-4)/2 = 35 → مجاز از ۳۴٫۵
        assertEquals(List.of("g34_5", "g36"), ids(RfSpecService.suggest(c, q(5500, 4, false), T).antennasA()));
    }

    @Test
    void integratedRadioWithTooLittleGainIsDropped() {
        List<Product> c = List.of(radio("ok", "RADIO_INTEGRATED", 27, -90, 32.0, 1),
                radio("weak", "RADIO_INTEGRATED", 27, -90, 20.0, 1),
                radio("unknownGain", "RADIO_INTEGRATED", 27, -90, null, 1),
                radio("conn", "RADIO", 27, -90, null, 1),
                radio("lowTx", "RADIO", 15, -90, null, 1),
                radio("deaf", "RADIO", 27, -70, null, 1));
        assertEquals(List.of("conn", "ok"), ids(RfSpecService.suggest(c, q(5500, 25, false), T).radios()).stream().sorted().toList());
    }

    @Test
    void discontinuedIsNeverOffered() {
        Product stopped = antenna("stopped", 32, 4900, 6100, 1, 100);
        stopped.setDiscontinued(true);
        Product live = antenna("live", 32, 4900, 6100, 1, 100);
        ProductRepository repo = mock(ProductRepository.class);
        when(repo.findAll()).thenReturn(List.of(stopped, live, base("noRf", 1, 1)));
        RfSpecService svc = new RfSpecService(repo, mock(CategoryRepository.class), mock(ActivityLogService.class), new ObjectMapper());
        assertEquals(List.of("live"), ids(svc.catalog()));
    }

    @Test
    void outOfStockComesAfterInStock() {
        List<Product> c = List.of(antenna("out", 32, 4900, 6100, 0, 10), antenna("in", 33, 4900, 6100, 2, 900));
        assertEquals(List.of("in", "out"), ids(RfSpecService.suggest(c, q(5500, 25, false), T).antennasA()));
    }

    @Test
    void band60NeedsWeather() {
        List<Product> c = List.of(antenna("v", 38, 57000, 66000, 1, 100));
        RfSpecService.Suggestion dry = RfSpecService.suggest(c, q(60000, 25, false), T);
        assertTrue(dry.antennasA().isEmpty());
        assertEquals(RfSpecService.NOTICE_60, dry.notice());
        RfSpecService.Suggestion wet = RfSpecService.suggest(c, q(60000, 25, true), T);
        assertEquals(List.of("v"), ids(wet.antennasA()));
        assertNull(wet.notice());
    }

    @Test
    void emptyCatalogGivesEmptyListsNotAnError() {
        RfSpecService.Suggestion s = RfSpecService.suggest(List.of(), q(5500, 25, false), T);
        assertTrue(s.antennasA().isEmpty() && s.antennasB().isEmpty() && s.radios().isEmpty());
        assertNull(s.notice());
    }

    @Test
    void everyInvalidInputIs400WithItsField() {
        Map<String, String> ok = new HashMap<>(Map.of("fMhz", "5500", "gainA", "32", "gainB", "32",
                "marginDb", "25.6", "txDbm", "20", "sensDbm", "-75", "weather", "0"));
        for (String[] bad : new String[][]{{"fMhz", "1999"}, {"fMhz", "x"}, {"gainA", "-1"}, {"gainB", ""},
                {"marginDb", "NaN"}, {"txDbm", "99"}, {"sensDbm", "-10"}, {"weather", "2"}}) {
            Map<String, String> m = new HashMap<>(ok);
            m.put(bad[0], bad[1]);
            AppApiException e = assertThrows(AppApiException.class, () -> AppRfControllerAccess.parse(m), bad[0]);
            assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
            assertEquals(bad[0], e.getBody().get("field"));
        }
        assertDoesNotThrow(() -> AppRfControllerAccess.parse(ok));
    }

    @Test
    void validationCatchesBandOrderKindsAndRanges() {
        RfSpec rf = new RfSpec();
        rf.setKind("ANTENNA");
        rf.setBands(List.of(new RfSpec.Band(6000, 5000)));
        RfSpec.Radio r = new RfSpec.Radio();
        r.setTxMaxDbm(99.0);
        rf.setRadio(r);
        List<String> e = RfSpecService.validate(rf);
        assertTrue(e.stream().anyMatch(s -> s.contains("ابتدا")));
        assertTrue(e.stream().anyMatch(s -> s.contains("آنتن بخشِ رادیو ندارد")));
        assertTrue(e.stream().anyMatch(s -> s.contains("توانِ ارسال")));
        rf.setKind("FOO");
        assertTrue(RfSpecService.validate(rf).stream().anyMatch(s -> s.contains("نوع")));
    }
}
