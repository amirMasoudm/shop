# -*- coding: utf-8 -*-
"""
گزارشِ وضعیتِ سینکِ شیتِ شرکت با کاتالوگِ اپ.

اجرا:  python scripts/sync-report.py

همیشه از دادهٔ زنده می‌خوانَد (API لوکال + دو فایلِ اکسل)، نه از گزارش‌های قبلی.
اگر عددی نتوانست خوانده شود، صریح می‌گوید — هیچ عددی حدس زده نمی‌شود.
"""
import json
import sys
import urllib.request

import openpyxl

API = "http://localhost/api/v1/products?size=400"
SHEET = r"C:\Users\m\Downloads\mikrotik (3).xlsx"
SHEET_TAB = "قیمت میکروتیک "
DECISION = (r"C:\Users\m\Downloads\Telegram Desktop"
            r"\mikrotik-not-in-app-2026-08-16 اکسل کالا قیمت.xlsx")
FACTOR = 1.08


def fa(n):
    return str(n).translate(str.maketrans("0123456789", "۰۱۲۳۴۵۶۷۸۹"))


def load_api():
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    with opener.open(API, timeout=40) as r:
        return json.load(r)["content"]


def main():
    try:
        items = load_api()
    except Exception as e:
        print(f"⛔ اپِ لوکال جواب نداد: {e}")
        print("   اگر خطای اتصال است، احتمالاً پورت‌فورواردِ داکر خوابیده:")
        print("   docker compose restart nginx")
        sys.exit(1)

    ws = openpyxl.load_workbook(SHEET, data_only=True)[SHEET_TAB]
    rows = {}
    for r in range(2, ws.max_row + 1):
        name = ws.cell(r, 5).value
        if not name:
            continue
        d = ws.cell(r, 4).value
        try:
            d = float(d)
        except (TypeError, ValueError):
            d = None
        rows[r] = d

    src = openpyxl.load_workbook(DECISION, data_only=True).worksheets[0]
    dec = {}
    for r in range(17, 152):
        if not src.cell(r, 2).value:
            continue
        key = str(src.cell(r, 6).value).strip() if src.cell(r, 6).value else "(خالی)"
        dec[key] = dec.get(key, 0) + 1

    not_in_app = sum(dec.values())
    in_app = len(rows) - not_in_app
    decided = len(rows) - dec.get("(خالی)", 0)

    sheet_prices = {round(v * 1000 * FACTOR) for v in rows.values() if v is not None}
    zero, fresh, stale = [], [], []
    for x in items:
        try:
            p = float(x.get("onlinePrice") or 0)
        except (TypeError, ValueError):
            p = 0
        if p == 0:
            zero.append(x)
        elif round(p) in sheet_prices:
            fresh.append(x)
        else:
            stale.append(x)
    fake_stock = sum(1 for x in items if x.get("stock") == 999)

    print("=" * 58)
    print("گزارشِ سینک — شیتِ شرکت در برابرِ کاتالوگِ اپ")
    print("=" * 58)
    print(f"\n▎شیتِ مرجع: {fa(len(rows))} ردیفِ نام‌دار")
    print(f"   از اول در اپ بودند ............ {fa(in_app)}")
    print(f"   در اپ نبودند (به شرکت رفت) .... {fa(not_in_app)}")

    print(f"\n▎جوابِ شرکت به آن {fa(not_in_app)} ردیف")
    for k, v in sorted(dec.items(), key=lambda t: -t[1]):
        print(f"   {k:<14} ............ {fa(v)}")

    pct = round(decided * 100 / len(rows))
    print(f"\n   ✅ تعیین‌تکلیف‌شده: {fa(decided)} از {fa(len(rows))}  ({fa(pct)}٪)")
    print(f"   ⏸ منتظرِ شرکت:      {fa(dec.get('(خالی)', 0))}")

    print(f"\n▎کاتالوگِ اپ: {fa(len(items))} محصول")
    print(f"   ✅ قیمتِ منطبق با شیت .......... {fa(len(fresh))}")
    print(f"   🔴 قیمتِ کهنه (نامنطبق) ........ {fa(len(stale))}")
    print(f"   ⚪ قیمتِ صفر (نمایش داده نمی‌شود) {fa(len(zero))}")
    print(f"   🔴 موجودیِ ساختگیِ ۹۹۹ .......... {fa(fake_stock)}")

    covered = round(len(fresh) * 100 / len(items))
    print(f"\n▎خلاصه")
    print(f"   {fa(pct)}٪ ردیف‌های شیت تعیین‌تکلیف شده‌اند،")
    print(f"   ولی فقط {fa(covered)}٪ محصولاتِ اپ قیمتِ واقعی دارند")
    print(f"   — چون {fa(len(items) - len(rows))} محصولِ اپ اصلاً در شیت ردیف ندارند.")
    print("=" * 58)


if __name__ == "__main__":
    main()
