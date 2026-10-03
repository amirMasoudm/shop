package org.example.shop1.model.service.holoo;

import org.example.shop1.model.service.HolooCodeService;

import java.util.List;

/**
 * تشخیصِ اینکه در یک برگه، هر ستون چه‌کاره است.
 *
 * <h3>🔴 چرا سرستونِ کد هاردکد نشده</h3>
 * هنوز معلوم نیست شرکت کد را در کدام فیلدِ نرم‌افزارِ حسابداری می‌گذارد، پس نامِ
 * سرستونش هم معلوم نیست. ستونِ کد <b>از روی الگویِ خودِ مقادیر</b> پیدا می‌شود:
 * ستونی که بیشترین خانه‌هایش شکلِ {@code DN-0001} دارند. اگر شرکت فردا کد را در
 * ستونی به نامِ «کد فنی» بگذارد، این کد بدونِ تغییر کار می‌کند.
 *
 * <h3>🔴 چرا انبار از داده خوانده می‌شود نه از نامِ فایل</h3>
 * نامِ فایل دستِ کاربر است و هر بار می‌تواند فرق کند یا جابه‌جا دانلود شود. ستونِ
 * «گروه اصلي» در خودِ داده می‌گوید ردیف مالِ کدام انبار است، و همان منبعِ حقیقت است.
 */
record SheetLayout(int headerRow, int codeColumn, int nameColumn,
                   int warehouseColumn, int quantityColumn, int indexColumn) {

    /** حداقلِ ردیفِ کددار تا یک ستون «ستونِ کد» شناخته شود. */
    private static final int MIN_CODE_HITS = 1;

    private static final List<String> NAME_HEADERS = List.of("نام", "شرح", "کالا");
    private static final List<String> WAREHOUSE_HEADERS = List.of("گروه اصلي", "گروه اصلی", "انبار");
    private static final List<String> QUANTITY_HEADERS = List.of("موجودي", "موجودی", "تعداد", "مقدار");
    private static final List<String> INDEX_HEADERS = List.of("رديف", "ردیف", "شماره");

    /**
     * ساختارِ برگه، یا {@code null} اگر اصلاً ستونِ کد ندارد (برگهٔ راهنما و امثالش).
     */
    static SheetLayout detect(List<List<String>> rows) {
        if (rows == null || rows.isEmpty()) return null;

        int width = rows.stream().mapToInt(List::size).max().orElse(0);
        if (width == 0) return null;

        int headerRow = findHeaderRow(rows, width);
        int firstData = headerRow + 1;

        int code = columnWithMostCodes(rows, firstData, width);
        if (code < 0) return null;

        int name = headerColumn(rows, headerRow, width, NAME_HEADERS);
        int warehouse = headerColumn(rows, headerRow, width, WAREHOUSE_HEADERS);
        int quantity = headerColumn(rows, headerRow, width, QUANTITY_HEADERS);
        int index = headerColumn(rows, headerRow, width, INDEX_HEADERS);

        if (warehouse < 0) warehouse = columnWhoseValuesContain(rows, firstData, width, "انبار");
        if (name < 0) name = longestTextColumn(rows, firstData, width, code, warehouse);
        if (quantity < 0) quantity = lastNumericColumn(rows, firstData, width, index);

        return new SheetLayout(headerRow, code, name, warehouse, quantity, index);
    }

    int firstDataRow() {
        return headerRow + 1;
    }

    String value(List<String> cells, int column) {
        if (column < 0 || cells == null || column >= cells.size()) return null;
        return cells.get(column);
    }

    boolean isBlank(List<String> cells) {
        return cells == null || cells.stream().allMatch(c -> c == null || c.trim().isEmpty());
    }

    /**
     * 🔴 ردیفِ جمعِ کل.
     * <p>
     * در فایل‌های واقعی آخرین ردیف {@code [null, null, null, null, 180101]} است —
     * عددِ جمع، بدونِ نام و بدونِ شمارهٔ ردیف. بدونِ این گارد یک «محصول» با موجودیِ
     * صدوهشتادهزاری وارد می‌شد.
     */
    boolean isSummaryRow(List<String> cells) {
        boolean noName = nameColumn >= 0 && blank(value(cells, nameColumn));
        boolean noIndex = indexColumn >= 0 && blank(value(cells, indexColumn));
        boolean hasQuantity = quantityColumn >= 0 && !blank(value(cells, quantityColumn));
        return (noName || noIndex) && hasQuantity;
    }

    // ==========================================================

    private static boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** اولین ردیفی که دستِ‌کم دو سرستونِ شناخته‌شده دارد؛ اگر نبود، یعنی سرستون ندارد. */
    private static int findHeaderRow(List<List<String>> rows, int width) {
        int limit = Math.min(rows.size(), 10);
        for (int r = 0; r < limit; r++) {
            int hits = 0;
            for (int c = 0; c < width; c++) {
                String v = cell(rows, r, c);
                if (v == null) continue;
                if (matchesAny(v, NAME_HEADERS) || matchesAny(v, WAREHOUSE_HEADERS)
                        || matchesAny(v, QUANTITY_HEADERS) || matchesAny(v, INDEX_HEADERS)) {
                    hits++;
                }
            }
            if (hits >= 2) return r;
        }
        return -1;
    }

    private static int headerColumn(List<List<String>> rows, int headerRow, int width, List<String> keywords) {
        if (headerRow < 0) return -1;
        for (int c = 0; c < width; c++) {
            String v = cell(rows, headerRow, c);
            if (v != null && matchesAny(v, keywords)) return c;
        }
        return -1;
    }

    private static boolean matchesAny(String value, List<String> keywords) {
        String v = value.trim();
        for (String k : keywords) {
            if (v.contains(k)) return true;
        }
        return false;
    }

    private static int columnWithMostCodes(List<List<String>> rows, int firstData, int width) {
        int best = -1, bestHits = 0;
        for (int c = 0; c < width; c++) {
            int hits = 0;
            for (int r = Math.max(firstData, 0); r < rows.size(); r++) {
                String v = cell(rows, r, c);
                if (v != null && HolooCodeService.looksLikeCode(v)) hits++;
            }
            if (hits > bestHits) {
                bestHits = hits;
                best = c;
            }
        }
        return bestHits >= MIN_CODE_HITS ? best : -1;
    }

    private static int columnWhoseValuesContain(List<List<String>> rows, int firstData, int width, String needle) {
        int best = -1, bestHits = 0;
        for (int c = 0; c < width; c++) {
            int hits = 0;
            for (int r = Math.max(firstData, 0); r < rows.size(); r++) {
                String v = cell(rows, r, c);
                if (v != null && v.contains(needle)) hits++;
            }
            if (hits > bestHits) {
                bestHits = hits;
                best = c;
            }
        }
        return best;
    }

    /** ستونی که بیشترین متنِ غیرعددی دارد — نامزدِ «نام کالا» وقتی سرستون نداریم. */
    private static int longestTextColumn(List<List<String>> rows, int firstData, int width, int... exclude) {
        int best = -1;
        long bestLength = 0;
        outer:
        for (int c = 0; c < width; c++) {
            for (int x : exclude) if (x == c) continue outer;
            long total = 0;
            for (int r = Math.max(firstData, 0); r < rows.size(); r++) {
                String v = cell(rows, r, c);
                if (v == null || HolooStockImportService.toInt(v) != null) continue;
                total += v.trim().length();
            }
            if (total > bestLength) {
                bestLength = total;
                best = c;
            }
        }
        return best;
    }

    /** راست‌ترین ستونِ عددی (به‌جز ستونِ ردیف) — نامزدِ «موجودی» وقتی سرستون نداریم. */
    private static int lastNumericColumn(List<List<String>> rows, int firstData, int width, int indexColumn) {
        for (int c = width - 1; c >= 0; c--) {
            if (c == indexColumn) continue;
            int numeric = 0;
            for (int r = Math.max(firstData, 0); r < rows.size(); r++) {
                String v = cell(rows, r, c);
                if (v != null && HolooStockImportService.toInt(v) != null) numeric++;
            }
            if (numeric > 0) return c;
        }
        return -1;
    }

    private static String cell(List<List<String>> rows, int row, int col) {
        if (row < 0 || row >= rows.size()) return null;
        List<String> cells = rows.get(row);
        if (cells == null || col < 0 || col >= cells.size()) return null;
        String v = cells.get(col);
        return (v == null || v.trim().isEmpty()) ? null : v;
    }
}
