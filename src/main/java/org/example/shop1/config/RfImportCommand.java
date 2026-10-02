package org.example.shop1.config;

import org.example.shop1.model.service.RfSpecImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * واردکردنِ دادهٔ رادیویی از خطِ فرمان — همان {@link RfSpecImportService}ِ تبِ پنل
 * (پیش‌نمایش، سپس اعمال؛ فقط rf؛ یک رکوردِ خلاصه)، برای جایی که ورودِ پنل در دسترس نیست
 * (لوکال پیامک ندارد). فقط وقتی {@code app.rf.import-products} داده شود فعال است و پس از
 * کار برنامه را می‌بندد؛ در اجرای عادی هیچ اثری ندارد.
 * <pre>
 * java -jar app.jar --spring.main.web-application-type=none \
 *   --app.rf.import-products=/tmp/rf-specs.csv --app.rf.import-rates=/tmp/rf-specs-mcs.csv
 * </pre>
 */
@Component
@ConditionalOnProperty("app.rf.import-products")
public class RfImportCommand implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RfImportCommand.class);

    private final RfSpecImportService importer;
    private final ConfigurableApplicationContext context;

    public RfImportCommand(RfSpecImportService importer, ConfigurableApplicationContext context) {
        this.importer = importer;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String products = context.getEnvironment().getProperty("app.rf.import-products");
        String rates = context.getEnvironment().getProperty("app.rf.import-rates");
        int code = 0;
        try {
            RfSpecImportService.Preview pv = importer.preview(Files.readString(Path.of(products)), Path.of(products).getFileName().toString(),
                    rates == null ? null : Files.readString(Path.of(rates)), rates == null ? null : Path.of(rates).getFileName().toString());
            log.info("RF-IMPORT پیش‌نمایش: {} محصول، {} نرخ، {}", pv.rowsRead(), pv.rateRowsRead(), pv.counts());
            pv.items().stream().filter(i -> !i.errors().isEmpty())
                    .forEach(i -> log.info("RF-IMPORT {} {} {}", i.status(), i.holooCode(), i.errors()));
            RfSpecImportService.ApplyResult r = importer.apply(pv.token());
            log.info("RF-IMPORT اعمال: تازه {} · عوض‌شده {} · بی‌تغییر {} · ردشده {}", r.created(), r.changed(), r.unchanged(), r.skipped());
        } catch (Exception e) {
            log.error("RF-IMPORT ناموفق: {}", e.toString());
            code = 1;
        }
        final int exit = code;
        System.exit(org.springframework.boot.SpringApplication.exit(context, () -> exit));
    }
}
