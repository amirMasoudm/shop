package org.example.shop1.model.service.holoo;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * خواندنِ فایلِ {@code .xlsx} بدونِ هیچ کتابخانهٔ بیرونی.
 * <p>
 * <b>چرا دست‌ساز و نه Apache POI:</b> تنها کاری که لازم داریم خواندنِ چند ستونِ
 * متنی و عددی است. POI حدودِ ۱۵ مگابایت وابستگی به یک جارِ ۵۳ مگابایتی اضافه
 * می‌کند برایِ قابلیت‌هایی (قالب‌بندی، نمودار، ماکرو) که هیچ‌کدام را نمی‌خواهیم.
 * خودِ {@code xlsx} یک زیپ از XML است و JDK هر دو را دارد.
 * <p>
 * 🔴 <b>امنیت:</b> فایل از بیرون می‌آید، پس پارسر با {@code FEATURE_SECURE_PROCESSING}
 * و DTDِ ممنوع ساخته می‌شود — وگرنه یک شیتِ دستکاری‌شده می‌توانست با
 * XXE فایل‌های سرور را بخواند.
 * <p>
 * ⚠️ نکته‌هایی که در فایل‌های واقعی به آن خوردیم:
 * <ul>
 *   <li>بعضی فایل‌ها اصلاً {@code sharedStrings.xml} ندارند و همهٔ متن را
 *       {@code inlineStr} می‌نویسند.</li>
 *   <li>خانه‌های خالی در XML اصلاً نوشته نمی‌شوند، پس شمارهٔ ستون باید از خودِ
 *       ارجاعِ خانه ({@code C7}) درآید، نه از ترتیبِ عناصر.</li>
 *   <li>{@code xml:space="preserve"} واقعی است: نامی مثلِ {@code "server "} فاصلهٔ
 *       پایانی دارد و همان فاصله دلیلِ شکستِ تطبیقِ نامی بود.</li>
 * </ul>
 */
public final class XlsxReader {

    private static final Pattern CELL_REF = Pattern.compile("^([A-Z]+)(\\d+)$");

    private XlsxReader() {
    }

    /** یک برگه: نامش و ردیف‌هایش. */
    public record Sheet(String name, List<List<String>> rows) {}

    /**
     * همهٔ برگه‌های فایل، به ترتیبِ خودشان.
     * <p>
     * عمداً همهٔ برگه‌ها خوانده می‌شوند نه فقط اولی: فایلِ خروجی ممکن است برگهٔ
     * راهنما داشته باشد یا دو انبار را در دو برگه بدهد. تشخیصِ اینکه کدام برگه
     * داده دارد کارِ لایهٔ بالاتر است، نه پارسر.
     */
    public static List<Sheet> read(byte[] bytes) throws IOException {
        Map<String, byte[]> parts = unzip(bytes);

        List<String> shared = readSharedStrings(parts.get("xl/sharedStrings.xml"));
        Map<String, String> sheetNames = readSheetNames(parts);

        List<String> sheetPaths = parts.keySet().stream()
                .filter(n -> n.startsWith("xl/worksheets/") && n.endsWith(".xml"))
                .sorted(Comparator.comparingInt(XlsxReader::sheetOrdinal))
                .toList();

        List<Sheet> out = new ArrayList<>();
        for (String path : sheetPaths) {
            String base = path.substring(path.lastIndexOf('/') + 1);
            out.add(new Sheet(sheetNames.getOrDefault(base, base), readSheet(parts.get(path), shared)));
        }
        return out;
    }

    // ==========================================================

    private static Map<String, byte[]> unzip(byte[] bytes) throws IOException {
        Map<String, byte[]> parts = new LinkedHashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;
                parts.put(entry.getName(), zis.readAllBytes());
            }
        }
        if (parts.isEmpty()) {
            throw new IOException("فایل خوانده نشد — یک فایلِ xlsx معتبر نیست");
        }
        return parts;
    }

    /** شمارهٔ داخلِ {@code sheet12.xml} تا مرتب‌سازی الفبایی sheet10 را قبلِ sheet2 نگذارد. */
    private static int sheetOrdinal(String path) {
        Matcher m = Pattern.compile("sheet(\\d+)\\.xml$").matcher(path);
        return m.find() ? Integer.parseInt(m.group(1)) : Integer.MAX_VALUE;
    }

    /** نگاشتِ «sheet1.xml → نامِ نمایشیِ برگه»، از روی workbook.xml و relsش. */
    private static Map<String, String> readSheetNames(Map<String, byte[]> parts) {
        Map<String, String> byBaseName = new LinkedHashMap<>();
        byte[] workbook = parts.get("xl/workbook.xml");
        byte[] rels = parts.get("xl/_rels/workbook.xml.rels");
        if (workbook == null || rels == null) return byBaseName;

        try {
            Map<String, String> relTargets = new LinkedHashMap<>();
            NodeList relationships = parse(rels).getElementsByTagName("Relationship");
            for (int i = 0; i < relationships.getLength(); i++) {
                Element r = (Element) relationships.item(i);
                String target = r.getAttribute("Target");
                relTargets.put(r.getAttribute("Id"), target.substring(target.lastIndexOf('/') + 1));
            }

            NodeList sheets = parse(workbook).getElementsByTagName("sheet");
            for (int i = 0; i < sheets.getLength(); i++) {
                Element s = (Element) sheets.item(i);
                String rid = s.getAttribute("r:id");
                String base = relTargets.get(rid);
                if (base != null) byBaseName.put(base, s.getAttribute("name"));
            }
        } catch (Exception ignored) {
            // نامِ برگه تزئینی است؛ نبودش نباید خواندنِ داده را بخواباند
        }
        return byBaseName;
    }

    private static List<String> readSharedStrings(byte[] xml) {
        List<String> out = new ArrayList<>();
        if (xml == null) return out;   // فایل‌هایی که همه‌چیز را inline می‌نویسند
        try {
            NodeList items = parse(xml).getElementsByTagName("si");
            for (int i = 0; i < items.getLength(); i++) {
                out.add(concatText((Element) items.item(i)));
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private static List<List<String>> readSheet(byte[] xml, List<String> shared) {
        List<List<String>> rows = new ArrayList<>();
        if (xml == null) return rows;
        Document doc;
        try {
            doc = parse(xml);
        } catch (Exception e) {
            return rows;
        }

        NodeList rowNodes = doc.getElementsByTagName("row");
        for (int i = 0; i < rowNodes.getLength(); i++) {
            Element row = (Element) rowNodes.item(i);
            List<String> cells = new ArrayList<>();
            NodeList cellNodes = row.getElementsByTagName("c");
            for (int j = 0; j < cellNodes.getLength(); j++) {
                Element c = (Element) cellNodes.item(j);
                int col = columnOf(c.getAttribute("r"), cells.size());
                while (cells.size() < col) cells.add(null);   // خانه‌های خالی در XML نیستند
                cells.add(valueOf(c, shared));
            }
            rows.add(cells);
        }
        return rows;
    }

    /** {@code "C7"} → ۲ (صفرپایه). اگر ارجاع نداشت، جای بعدی. */
    private static int columnOf(String ref, int fallback) {
        if (ref == null) return fallback;
        Matcher m = CELL_REF.matcher(ref);
        if (!m.matches()) return fallback;
        String letters = m.group(1);
        int col = 0;
        for (int i = 0; i < letters.length(); i++) {
            col = col * 26 + (letters.charAt(i) - 'A' + 1);
        }
        return col - 1;
    }

    private static String valueOf(Element c, List<String> shared) {
        String type = c.getAttribute("t");

        if ("inlineStr".equals(type)) {
            NodeList is = c.getElementsByTagName("is");
            return is.getLength() == 0 ? null : blankToNull(concatText((Element) is.item(0)));
        }

        String raw = firstChildText(c, "v");
        if (raw == null) return null;

        if ("s".equals(type)) {
            try {
                int idx = Integer.parseInt(raw.trim());
                return idx >= 0 && idx < shared.size() ? blankToNull(shared.get(idx)) : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if ("b".equals(type)) {
            return "1".equals(raw.trim()) ? "TRUE" : "FALSE";
        }
        return blankToNull(raw);
    }

    /** همهٔ {@code <t>}های زیرِ یک عنصر، پشتِ‌سرِ هم — متنِ غنی چند تکه است. */
    private static String concatText(Element parent) {
        StringBuilder sb = new StringBuilder();
        NodeList texts = parent.getElementsByTagName("t");
        for (int i = 0; i < texts.getLength(); i++) {
            String v = texts.item(i).getTextContent();
            if (v != null) sb.append(v);
        }
        return sb.toString();
    }

    private static String firstChildText(Element parent, String tag) {
        NodeList list = parent.getElementsByTagName(tag);
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            if (n.getParentNode() == parent) return n.getTextContent();
        }
        return null;
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        // ⚠️ trim فقط برایِ تشخیصِ «خالی» است؛ مقدار با فاصله‌های خودش برمی‌گردد،
        // چون فاصلهٔ پایانیِ نام‌ها یک واقعیتِ همین فایل‌هاست و پنهان‌کردنش
        // اشکال‌زدایی را سخت می‌کند.
        return s.trim().isEmpty() ? null : s;
    }

    private static Document parse(byte[] xml) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        f.setFeature("http://xml.org/sax/features/external-general-entities", false);
        f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        f.setNamespaceAware(false);
        DocumentBuilder b = f.newDocumentBuilder();
        try (InputStream in = new ByteArrayInputStream(xml)) {
            return b.parse(in);
        }
    }
}
