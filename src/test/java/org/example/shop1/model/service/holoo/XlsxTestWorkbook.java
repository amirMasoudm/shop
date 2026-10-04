package org.example.shop1.model.service.holoo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * ساختِ یک {@code .xlsx}ِ کمینه در حافظه برایِ تست.
 * <p>
 * عمداً همان شکلی نوشته می‌شود که فایلِ واقعیِ در دست داشت: <b>بدونِ</b>
 * {@code sharedStrings.xml} و با {@code inlineStr}. اگر تست فایلی می‌ساخت که
 * همه‌چیز را در جدولِ رشته‌های مشترک می‌گذارد، دقیقاً همان مسیری آزموده می‌شد که
 * فایلِ واقعی از آن رد نمی‌شود.
 */
final class XlsxTestWorkbook {

    private XlsxTestWorkbook() {
    }

    /** یک برگه: نام و ردیف‌ها. {@code null} در یک خانه یعنی خانهٔ واقعاً خالی. */
    record SheetSpec(String name, List<List<Object>> rows) {}

    static byte[] build(List<SheetSpec> sheets) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            write(zip, "[Content_Types].xml", contentTypes(sheets.size()));
            write(zip, "_rels/.rels", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                    </Relationships>""");
            write(zip, "xl/workbook.xml", workbook(sheets));
            write(zip, "xl/_rels/workbook.xml.rels", workbookRels(sheets.size()));
            for (int i = 0; i < sheets.size(); i++) {
                write(zip, "xl/worksheets/sheet" + (i + 1) + ".xml", sheet(sheets.get(i)));
            }
        }
        return out.toByteArray();
    }

    private static String contentTypes(int sheetCount) {
        StringBuilder sb = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""");
        for (int i = 1; i <= sheetCount; i++) {
            sb.append("<Override PartName=\"/xl/worksheets/sheet").append(i)
                    .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
        }
        return sb.append("</Types>").toString();
    }

    private static String workbook(List<SheetSpec> sheets) {
        StringBuilder sb = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets>""");
        for (int i = 0; i < sheets.size(); i++) {
            sb.append("<sheet name=\"").append(escape(sheets.get(i).name()))
                    .append("\" sheetId=\"").append(i + 1)
                    .append("\" r:id=\"rId").append(i + 1).append("\"/>");
        }
        return sb.append("</sheets></workbook>").toString();
    }

    private static String workbookRels(int sheetCount) {
        StringBuilder sb = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""");
        for (int i = 1; i <= sheetCount; i++) {
            sb.append("<Relationship Id=\"rId").append(i)
                    .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet")
                    .append(i).append(".xml\"/>");
        }
        return sb.append("</Relationships>").toString();
    }

    private static String sheet(SheetSpec spec) {
        StringBuilder sb = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""");
        int rowNumber = 0;
        for (List<Object> row : spec.rows()) {
            rowNumber++;
            sb.append("<row r=\"").append(rowNumber).append("\">");
            for (int c = 0; c < row.size(); c++) {
                Object v = row.get(c);
                if (v == null) continue;   // ⚠️ خانهٔ خالی اصلاً نوشته نمی‌شود — مثلِ فایلِ واقعی
                String ref = columnName(c) + rowNumber;
                if (v instanceof Number) {
                    sb.append("<c r=\"").append(ref).append("\"><v>").append(v).append("</v></c>");
                } else {
                    sb.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                            .append(escape(String.valueOf(v))).append("</t></is></c>");
                }
            }
            sb.append("</row>");
        }
        return sb.append("</sheetData></worksheet>").toString();
    }

    private static String columnName(int index) {
        StringBuilder sb = new StringBuilder();
        int n = index;
        do {
            sb.insert(0, (char) ('A' + n % 26));
            n = n / 26 - 1;
        } while (n >= 0);
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static void write(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
