# گزارش — دادهٔ فنیِ آنتن و رادیو از منبعِ رسمیِ سازنده (۱۴۰۵/۰۷/۱۰؛ دورِ اصلاح ۱۴۰۵/۰۷/۱۱)

تحقیقِ بدونِ تغییرِ دیتابیس برایِ ماشین‌حسابِ «داده‌لینک». هر عدد از صفحهٔ محصول یا دیتاشیتِ خودِ سازنده آمده؛ عددی که منبع نداشت خالی ماند و در `notes` نوشته شد «در منبع نیست». دیتابیس دست نخورد؛ تنها تغییرِ سایت اصلاحِ چند ردیفِ کارتِ آنتن‌هایِ وایمکس‌نیر است (بخشِ ۸).

فایل‌ها: [`rf-specs-1405-07.csv`](rf-specs-1405-07.csv) (یک ردیف برایِ هر محصول) · [`rf-specs-mcs-1405-07.csv`](rf-specs-mcs-1405-07.csv) (جدولِ کاملِ نرخ‌ها).

## دورِ اصلاحِ ۱۱ مهر — آنچه اعمال شد

- **وایمکس‌نیر برندِ خودِ داده‌نماست:** منبعِ ۱۲ ردیف کاتالوگ‌ها و برگه‌هایِ خودِ شرکت است و `source_url` همه `https://dadehnama.com/wimaxnear` شد؛ کاتالوگِ دقیق در `notes`. `irmikro.com` هیچ‌جا نیست (نه ستون، نه notes). ۴ مدل از کاتالوگِ تازه (27D، 28D Revised، 30D، 33D)، ۲ مدل از برگهٔ رسمیِ روی سایت (32D، 34.5D Ultra-HP)، ۶ مدل بی‌برگه (ستون‌هایِ فنی خالی، «از شرکت پرسیده شود»). بهره همه اسمی؛ عددِ آزمونِ تکتا فقط در notes.
- **B5C پذیرفته شد:** دیتاشیتِ خودِ میموسا است و فرقی نمی‌کند کجا میزبانی شده.
- **قاعدهٔ محافظه‌کارانه:** `ENS620EXT` توان ۲۶ (۲۷ در notes)؛ توانِ نرخ‌هایِ AF-5XHD که محدوده بود، کمترین شد («12-15»→12، «19-20»→19، «21-22»→21، «23-24»→23؛ اصلِ محدوده در notes هر ردیف)؛ `C6x` ۲۴ و `LiteBeam` ۲۴ ماند.
- **ستون‌هایِ عددی فقط عدد:** باندِ `2400-2483.5` (ENS620EXT) به `2400-2483`؛ پهنای پرتوِ «±2.5» در دو mANT خالی (متن در notes)؛ `band_mhz` ردیف‌هایِ نرخ که فقط «5 GHz»/«2.4 GHz» بود خالی شد. هر دو فایل با قواعدِ ابزارِ واردکردنِ الف (`RfSpecImportService`) سنجیده شد: ۸۱ ردیفِ بی‌هشدار، ۲ ردیفِ `kind`-خالی (عمدی)، صفر هشدار.
- **بهرهٔ فاز اسمی:** `DN-0002` ۳۰ و `DN-0003` ۳۴؛ بیشینه و کمینه فقط در notes.
- **از پیشنهاد بیرون:** `kind` خالی برای `DN-0191` (بوردِ لخت) و `DN-0218` (Force 4630)؛ ابزارِ واردکردن چنین ردیفی را رد می‌کند و دادهٔ فنی‌شان در notes می‌ماند.
- **RAy3:** همان ۲۴ گیگاهرتز، با notesِ «باند از مشخصاتِ قدیمیِ سایت؛ تأییدِ شرکت لازم (مالک می‌پرسد)».
- **همان‌طور که بود:** خانواده‌هایِ بی‌جملهٔ صریح جدا؛ ردیف‌هایِ MCS0ِ میموسا؛ طبقه‌بندیِ NetBox 5 و Metal 52 ac؛ `DN-0001`؛ PTP 670 فقط ۵٫۸ گیگ؛ اختلاف‌هایِ بخشِ ۵ اصلاح نشد (تسکِ جدا بعد از انتشار).
- **SP-7036N (بندِ ۱-ج):** نساخته شد؛ منتظرِ قیمت و کدِ هلو از مالک.

## باقی‌مانده از مدیر و مالک

- **وایمکس‌نیر، شش مدلِ بی‌برگه** (`DN-0004`، `DN-0006`، `DN-0007`، `DN-0010`، `DN-0012`، `DN-0015`): از شرکت پرسیده شود. مدیر پنج‌تا گفته بود؛ `DN-0012` (32D-HP رادوم‌دار) ششمی است چون برگهٔ 32D مدلِ معمولی است. هر کاتالوگِ تازه‌ای که برسد برای مدلِ خودش مقدم است.
- **`DN-0011` (32D) و `DN-0016` (34D) وایمکس‌نیر:** برگه‌هایشان پهن‌باند 4800–6200 و Ultra-HP 4800–6100 است ولی نامِ محصول «H-band»؛ شرکت همانی‌بودن و باند را تأیید کند.
- **Force 4630 (`DN-0218`):** در سایتِ کمبیوم نه دیتاشیت دارد و نه اسمش در فهرستِ ePMP است؛ خودِ سایتِ ما هم دربارهٔ آنتنش تناقض دارد. کالای واقعی چیست؟
- **RAy3 راکام (`DN-0106`، `DN-0107`):** باند ۲۴ گیگ از مشخصاتِ قدیمیِ سایت است و تأییدِ شرکت می‌خواهد (مالک می‌پرسد). آنتنِ پارابولیک جدا خریده می‌شود (بهره خالی).

## ۱ — دامنه و تعدادها

کاتالوگِ زندهٔ سایت (`/api/v1/products`) ۲۲۹ محصول دارد؛ **۸۳ محصول در دامنه** است (۲۴ رادیوی کانکتوردار، ۳۹ رادیوی آنتن‌داخلی، ۱۸ آنتن، ۲ ردیفِ بدونِ `kind` که از پیشنهاد بیرون است) و ۱۴۶ محصول بیرونِ دامنه.

| دستهٔ سایت | آنتن | رادیو (کانکتوردار) | رادیو (آنتن‌داخلی) | بدونِ kind | جمع |
|---|---|---|---|---|---|
| `access-point` | ۰ | ۲ | ۰ | ۰ | ۲ |
| `access-point-mikrotik` | ۰ | ۰ | ۲ | ۰ | ۲ |
| `antenna-dish` | ۱۶ | ۰ | ۰ | ۰ | ۱۶ |
| `cambium` | ۰ | ۲ | ۱ | ۱ | ۴ |
| `engenius` | ۰ | ۱ | ۰ | ۰ | ۱ |
| `ligowave` | ۰ | ۳ | ۰ | ۰ | ۳ |
| `mikrotik` | ۰ | ۰ | ۲ | ۱ | ۳ |
| `mimosa` | ۰ | ۳ | ۳ | ۰ | ۶ |
| `racom` | ۰ | ۲ | ۰ | ۰ | ۲ |
| `ubnt` | ۰ | ۱ | ۴ | ۰ | ۵ |
| `wireless-outdoor-mikrotik` | ۲ | ۱۰ | ۲۷ | ۰ | ۳۹ |
| **جمع** | **۱۸** | **۲۴** | **۳۹** | **۲** | **۸۳** |

**بیرونِ دامنه:**

| دستهٔ سایت | تعداد | دلیل |
|---|---|---|
| `router-mikrotik` | ۲۹ | روتر |
| `voip` | ۲۲ | ویپ |
| `access-point-mikrotik` | ۱۷ | hAP/cAP/mAP/WAP (۲٫۴ داخلی)/wsAP ac lite (توکار در دیوار) و روترهایِ بی‌سیمِ خانگی؛ فقط wAP ac و wAP ax داخل دامنه ماندند |
| `lte-mikrotik` | ۱۶ | مودم/روترِ LTE |
| `passive-cable` | ۱۵ | تجهیزاتِ پسیو و کابل |
| `switch-mikrotik` | ۱۴ | سوییچ |
| `mikrotik-license` | ۱۳ | لایسنس و اکسسوری |
| `access-point` | ۸ | اکسس‌پوینت‌هایِ داخلیِ اینجینیوس (EWS276/356/357-FIT) و UniFi (U6 LR، U6 Pro، U7 Pro، UAP-AC Pro) و گیت‌وی XG60-FIT |
| `ubnt` | ۶ | اکسس‌پوینت‌هایِ UniFi (U7 LR، U7 Lite، U6 Mesh Pro، U6+، U6 Mesh، AP-AC Mesh Pro): پوششِ Wi‑Fi، نه رادیوی لینک |
| `engenius` | ۵ | سوییچ‌هایِ ECS/EWS و کنترلر FitController100 |
| `mikrotik` | ۱ | سوییچِ CRS317 (سه محصولِ دیگرِ این دسته رادیو بودند و داخل دامنه آمدند) |
| `microwave-radio` | ۰ | دسته خالی است (محصولی ندارد) |

### موردهایِ مرزی (با دلیلِ انتخاب یا رد)

| محصول | تصمیم | دلیل |
|---|---|---|
| SXTsq Lite2 / SXT SA5 (نسخهٔ n) / RB912UAG — `DN-0190` `DN-0189` `DN-0191` | داخل شد (در بازبینیِ نهایی) | در دستهٔ ریشهٔ «میکروتیک» ثبت‌اند، نه دسته‌هایِ رادیو؛ در اسکنِ اولِ دسته‌بندی از قلم افتاده بودند و با مرورِ نامِ هر ۲۲۹ محصول پیدا شدند. RB912UAG بوردِ لخت (IP00، بدونِ آنتن و محفظه) است؛ سه ردیفِ دیگرِ این دسته (CRS317 سوییچ) بیرون ماند |
| mANT 30dBi / mANT30 PA — `DN-0072` `DN-0073` | داخل شد (آنتن) | در دستهٔ `wireless-outdoor-mikrotik` ثبت‌اند ولی دیشِ تنها هستند، نه رادیو |
| HGO-antenna-OUT — `DN-0001` | داخل شد (آنتن) | در `antenna-dish` است ولی «HGO» برندِ جدا نیست؛ آنتنِ همه‌جهتهٔ کوچکِ خودِ میکروتیک (۶٫۷dBi در ۵گیگ) است؛ برایِ لینکِ نقطه‌به‌نقطه نباید پیشنهاد شود |
| wAP ac / wAP ax — `DN-0037` `DN-0038` | داخل شد (رادیوی آنتن‌داخلی) | اکسس‌پوینتِ بیرونیِ ضدآب که در نقشِ CPE/لینک هم به‌کار می‌رود؛ بقیهٔ ۱۷ محصولِ `access-point-mikrotik` داخلی/خانگی‌اند و بیرون ماندند |
| ENH1350EXT / ENS620EXT — `DN-0026` `DN-0027` | داخل شد (رادیوی کانکتوردار) | outdoor با حالتِ Client Bridge؛ در دستهٔ `access-point` (نه `engenius`) ثبت شده‌اند |
| UniFi U6 Mesh / U6 Mesh Pro / AP-AC Mesh Pro — `DN-0220` `DN-0222` `DN-0221` | بیرون ماند | IP67 است ولی اکسس‌پوینتِ پوششِ Wi‑Fi است نه رادیوی لینک؛ اگر مدیر «اکسس‌پوینتِ بیرونی» را داخل می‌شمارد، با wAP هم‌رده است و باید اضافه شود |
| wsAP ac lite — `DN-0046` | بیرون ماند | توکار در دیوار (wall-plate)، داخلی |
| Cube 60Pro ac / nRAY / LHG 60G / کیتِ Wireless Wire Dish — `DN-0085` `DN-0081` `DN-0089` `DN-0105` | داخل شد، با عددِ ناقص | ۶۰گیگ؛ میکروتیک بهره/Tx/حساسیتِ رادیوی ۶۰گیگ را منتشر نکرده (فقط سقفِ EIRP نظارتی؛ در `tx_max_dbm` نیامد چون بهره را هم دربردارد) |
| NetBox 5 — `DN-0093` | داخل شد (`RADIO`) | سایت «آنتن یکپارچه» می‌گوید، سازنده کانکتورِ RP-SMA |
| Metal 52 ac — `DN-0092` | داخل شد (`RADIO_INTEGRATED`، مرزی) | کانکتورِ N خارجی و آنتنِ همراه؛ سازنده بهره را در جدولِ مشخصات داده |
| LDF 5 ac — `DN-0068` | داخل شد | بهرهٔ خودِ رادیو ۹dBi؛ ۲۹dBi فقط با دیشِ همراه |
| LHG 5 — `DN-0229` و `DN-0077` | دو ردیفِ جدا، یک مدلِ سازنده | نسخهٔ تکی و بستهٔ ۳تایی هر دو RBLHG-5nD‌اند |
| SP62-35D، SP62-29D و چهار مدلِ دیگرِ وایمکس‌نیر — `DN-0007` `DN-0015` `DN-0004` `DN-0006` `DN-0010` `DN-0012` | داخل (بی‌برگه) | برگهٔ رسمیِ مدل‌به‌مدل ندارند؛ ستون‌هایِ فنی خالی و «از شرکت پرسیده شود» |
| دستهٔ `microwave-radio` | بدونِ محصول | دسته خالی است |

## ۲ — پوششِ هر ستون (چند از چند)

| ستون | مخرج | پر شده |
|---|---|---|
| `bands_mhz` | همهٔ ردیف‌ها | ۶۲ از ۸۳ |
| `gain_dbi` | آنتن + رادیوی آنتن‌داخلی | ۴۷ از ۵۷ |
| `antenna_type` | آنتن + رادیوی آنتن‌داخلی | ۴۱ از ۵۷ |
| `diameter_cm` | فقط دیش‌ها (`antenna_type=DISH`) | ۱۷ از ۱۸ |
| `beamwidth_deg` | آنتن + رادیوی آنتن‌داخلی | ۱۹ از ۵۷ |
| `polarization` | آنتن + رادیوی آنتن‌داخلی | ۲۴ از ۵۷ |
| `tx_max_dbm` | رادیوها | ۵۸ از ۶۳ |
| `sens_low_dbm` | رادیوها | ۵۵ از ۶۳ |
| `sens_high_dbm` | رادیوها | ۴۹ از ۶۳ |
| `compat_family` | رادیوها | ۶۳ از ۶۳ |
| `source_url` | همهٔ ردیف‌ها | ۸۲ از ۸۳ |
| جدولِ کاملِ نرخ‌ها (فایلِ دوم) | رادیوها | ۵۵ از ۶۳ |

خانهٔ خالی یعنی منبعِ رسمی آن عدد را نداده، یا (در `source_url` خالی) منبعِ رسمی پیدا نشده؛ دلیلِ هر مورد در `notes` همان ردیف است.

## ۳ — خانواده‌هایِ سازگاری و جملهٔ سازنده

دو رادیو فقط وقتی با هم لینک می‌شوند که پروتکلشان یکی باشد. کدِ هر خانواده، محصولاتش، و آنچه سازنده دربارهٔ سازگاری می‌گوید (بازنویسیِ فارسی با پیوند؛ جایی که عینِ عبارت مهم بود، تکه‌ای کوتاه در گیومه). **وضعیت** نشان می‌دهد جملهٔ صریحِ سازنده پیدا شد یا نه؛ مبهم‌ها جدا نگه داشته شدند و در بخشِ ۶ پرسیده‌ام.

| کد | محصولات | آنچه سازنده می‌گوید | پیوند | وضعیت |
|---|---|---|---|---|
| `MIKROTIK_WIFI` | DN-0037، DN-0098، DN-0079، DN-0078، DN-0086، DN-0082، DN-0068، DN-0084، DN-0087، DN-0090، DN-0093، DN-0075، DN-0092، DN-0074، DN-0101، DN-0096، DN-0102، DN-0076، DN-0083، DN-0104، DN-0095، DN-0190، DN-0189، DN-0191، DN-0069، DN-0085، DN-0071، DN-0229، DN-0077، DN-0099، DN-0080، DN-0070 | سازنده می‌گوید پروتکلِ اختصاصیِ Nv2 فقط بینِ دستگاه‌هایِ RouterOS کار می‌کند؛ و در جدولِ صفحهٔ Wireless، درایورِ قدیمی (Nstreme و Nv2) برایِ دستگاه‌هایِ ARM با 802.11ac از طریقِ بستهٔ wireless در دسترس است (MIPS/11n فقط درایورِ قدیمی دارد). | https://help.mikrotik.com/docs/spaces/ROS/pages/122388485/Nv2 ؛ https://help.mikrotik.com/docs/spaces/ROS/pages/1409138/Wireless | تأییدِ نیمه‌صریح (Nv2 بین RouterOSها). جملهٔ جدا برایِ سازگاریِ حالتِ استانداردِ 802.11 بینِ مدل‌ها پیدا نشد (استاندارد عمومی است). |
| `MIKROTIK_WIFI_AX` | DN-0038، DN-0097، DN-0094، DN-0103، DN-0091، DN-0088، DN-0100 | در همان جدولِ صفحهٔ Wireless، برایِ دستگاه‌هایِ جدیدِ 802.11ax مقدارِ «درایورِ قدیمی (Nstreme، Nv2)» «-» یعنی ممکن نیست؛ این دستگاه‌ها بستهٔ wifi-qcom می‌خواهند (صفحهٔ «Missing wireless or wifi interface after update»). | https://help.mikrotik.com/docs/spaces/ROS/pages/1409138/Wireless ؛ https://help.mikrotik.com/docs/spaces/RKB/pages/280657934/Missing+wireless+or+wifi+interface+after+update | جدا نگه داشته شد چون Nv2 روی ax ممکن نیست. لینکِ ax↔n/ac در حالتِ استانداردِ 802.11 جملهٔ صریح ندارد → سؤال. |
| `MIKROTIK_60G_NRAY` | DN-0081 | میکروتیک nRAY را به‌صورتِ جفتِ از پیش پیکربندی‌شده (دو دستگاه) عرضه می‌کند؛ بروشور: لینکِ 2Gb/s aggregate در باندِ ۶۰گیگ. | https://mikrotik.com/product/wireless_wire_nray | جدا نگه داشته شد: جملهٔ صریحِ سازگاریِ بین‌مدلی (nRAY با LHG 60G یا Cube 60Pro) پیدا نشد → سؤال. |
| `MIKROTIK_60G_LHG` | DN-0089، DN-0105 | کیتِ Wireless Wire Dish دو واحدِ یک مدل (RBLHGG-60ad) است که کارخانه جفت و پیکربندی کرده؛ LHG 60G تکی همان سخت‌افزار است. | https://mikrotik.com/product/wireless_wire_dish ؛ https://mikrotik.com/product/lhg_60g | جدا نگه داشته شد: سازگاریِ بین‌مدلی صریح نیست → سؤال. |
| `MIKROTIK_60G_AY` | DN-0085 | Cube 60Pro ac دو رادیو دارد: ۶۰گیگ (802.11ay) و رادیوی پشتیبانِ ۵گیگ 802.11ac؛ برایِ همین کدِ خانوادهٔ ۵گیگش `MIKROTIK_WIFI` هم آمده. | https://mikrotik.com/product/wireless_wire_cube_pro | جدا نگه داشته شد: جملهٔ سازنده دربارهٔ سازگاریِ ay با ad پیدا نشد (فقط در فروشگاه‌ها گفته شده که منبع نیست) → سؤال. |
| `UBNT_AIRMAX_M` | DN-0219 | راهنمایِ airOS 8 می‌گوید airOS 8 با ایستگاه‌هایِ airMAX M که airOS 6 دارند سازگار است (APِ ac در حالتِ «PTMP Mixed» کلاینتِ M را می‌پذیرد). لینکِ PTP بینِ دو airGrid در دیتاشیتِ airGrid آمده. | https://dl.ubnt.com/guides/airOS/airOS_8_UG_V02.pdf (ص۵ و ۱۴) ؛ https://dl.ui.com/datasheets/airgridm/airGrid_HP.pdf (ص۲) | جهتِ «M به APِ ac» تأیید شده؛ جهتِ عکس در منبع نیست. |
| `UBNT_AIRMAX_AC` | DN-0018 | راهنمایِ airOS 8 برایِ همهٔ محصولاتِ airMAX ac است؛ جدولِ «Model Comparison» دیتاشیتِ LiteBeam نشان می‌دهد پروتکلِ airMAX ac فقط برایِ LBE-5AC-23 است، نه LBE-M5-23. | https://dl.ubnt.com/guides/airOS/airOS_8_UG_V02.pdf ؛ https://dl.ubnt.com/datasheets/LiteBeam/LiteBeam_ds.pdf (ص۴) | تأییدِ صریح برایِ airMAX ac. |
| `UBNT_AIRFIBER_5XHD` | DN-0226 | راهنمایِ سریع (QSG): در لینک یکی Master و دیگری Slave است و دیتاشیت (ص۶) لینکِ بک‌هال را بینِ دو AF-5XHD می‌گوید. | https://dl.ui.com/guides/airfiber/airFiber_AF-5XHD_QSG.pdf ؛ https://dl.ui.com/datasheets/airfiber/airFiber_5XHD_DS.pdf | بین دو AF-5XHD تأیید شد؛ سازگاری با سایرِ رادیوهایِ LTU جملهٔ صریح ندارد → سؤال. |
| `UBNT_AIRFIBER_60_LR` | DN-0227 | صفحهٔ رسمیِ فروشگاهِ یوبیکیوتی: LR یا در PtP با یک واحدِ دیگر جفت می‌شود یا در PtMP کلاینتِ یک Wave AP است. | https://store.ui.com/us/en/products/airfiber-60-lr | جملهٔ صریحِ LR↔XG پیدا نشد → جدا نگه داشته شد. |
| `UBNT_AIRFIBER_60_XG` | DN-0228 | دیتاشیت می‌گوید PtP-only با رادیوی پشتیبانِ ۵گیگ است و مدلِ طرفِ مقابل را نام نمی‌برد. | https://dl.ui.com/ds/af60-xg_ds.pdf | جملهٔ صریحِ جفت پیدا نشد → سؤال. |
| `MIMOSA_5G_A5_C5` | DN-0187، DN-0183، DN-0186 | صفحهٔ A5x می‌گوید C5x و C5c با APِ A5x برایِ SRS خوب کار می‌کنند (دربارهٔ A5x، نه A5c)؛ پاورقیِ دیتاشیتِ C5c می‌گوید PTMP بالایِ ۶٫۲GHz به APِ نوعِ A5/A5c نیاز دارد؛ هر سه دیتاشیت پروتکل‌هایِ WiFi Interop و SRS را فهرست می‌کنند. | https://mimosa.co/products/a5x ؛ https://mimosa.co/sites/default/files/2024-06/Mimosa-Radisys-C5c-Datasheet_DS-2024-01.pdf | غیرمستقیم؛ جملهٔ صریحِ A5c↔C5x پیدا نشد → سؤال. |
| `MIMOSA_5G_B5X` | DN-0185 | صفحهٔ B5 می‌گوید GPS Sync به B5 و B5c اجازهٔ استفادهٔ مجددِ یک کانال را می‌دهد (همزمان‌سازی است، نه گواهِ لینک‌شدن). | https://mimosa.co/products/legacy/b5 | جدا از B5C نگه داشته شد؛ لینکِ B5x↔B5c صریح نیست → سؤال. |
| `MIMOSA_5G_B5C` | DN-0184 | همان صفحهٔ B5 (GPS Sync برایِ B5 و B5c). | https://mimosa.co/products/legacy/b5 | جدا نگه داشته شد → سؤال. |
| `MIMOSA_6G` | DN-0188 | دیتاشیتِ C6x (۲۰۲۶): کلاینتی که با APِ نوعِ A6 جفت می‌شود. | https://mimosa.co/sites/default/files/2026-07/Mimosa-Radisys-DS-2026-07-C6x.pdf | لینکِ PTP بینِ دو C6x یا با B6x صریح نیامده → سؤال. |
| `ENGENIUS_WIFI_11N` | DN-0024 | دیتاشیتِ ENS500EXT (ص۱): برایِ اتصالِ نقطه‌به‌چندنقطه به Client Bridgeهایِ ۵گیگِ اینجینیوس (مثلاً ENS500) توصیه شده است. | https://www.engeniustech.com/wp-content/uploads/2016/12/ENS500EXT_Datasheet_O.pdf | جدا از ac نگه داشته شد؛ تأییدِ صریحِ پس‌سازگاری ندارد → سؤال. |
| `ENGENIUS_WIFI_11AC` | DN-0026، DN-0027 | دفترچه‌هایِ ENH1350EXT (ص۳۲) و ENS620EXT: APِ اینجینیوس می‌تواند نقطهٔ مرکزی باشد و Client Bridgeهایِ بیرونیِ اینجینیوس به آن وصل شوند (همان SSID و رمز). | https://www.engeniustech.com/wp-content/uploads/2019/06/ENH1350EXT_Manul_180620.pdf ؛ https://www.engeniustech.com/wp-content/uploads/2017/11/ENS620EXT_UM_V1.2_171002.pdf | جملهٔ صریحِ «همهٔ مدل‌ها با هم» پیدا نشد؛ فقط AP↔Client Bridge در دفترچه‌ها. |
| `CAMBIUM_PTP670` | DN-0215 | راهنمای کاربرِ PTP 670 (ص۱۵۵): این سری از همکاریِ ODUهای واریانتِ فرکانسیِ متفاوت پشتیبانی نمی‌کند؛ دو سرِ لینک باید ODUی از یک واریانت باشند. | https://brandcentral.cambiumnetworks.com/m/53b05011123b13c2/original/PTP-670-Series-User-Guide.pdf | فقط قیدِ «هم‌واریانت» صریح است؛ سازگاری با PTP 650/700 در منبع پیدا نشد. |
| `CAMBIUM_EPMP_6GHZ_4600` | DN-0217، DN-0216 | دیتاشیت‌های Force 4625 و 4600C بندِ «Interoperability with ePMP 4600 Series Access Points» را دارند و دیتاشیتِ APِ 4600 می‌گوید با ماژول‌های مشترکِ Force 4600 همکاری دارد. | https://brandcentral.cambiumnetworks.com/m/e1aa7c6c90f854f/original/Cambium_Networks_data_sheet_ePMP_Force_4625_SM.pdf ؛ https://brandcentral.cambiumnetworks.com/m/2e472b2f7a6c752b/original/Cambium_Networks_data_sheet_ePMP_4600_Series_AP.pdf | AP↔SM در PMP تأیید شد؛ جفت‌شدنِ SM با SM در PTP (مثلاً 4625 با 4600C) صریح نیامده → سؤال. |
| `LIGOWAVE_DLB_IPOLL` | DN-0163 | دیتاشیتِ LigoDLB 5ac می‌گوید سری ac با دستگاه‌های LigoDLB قدیمی در حالتِ iPoll سازگارِ پس‌رو است. | https://www.ligowave.com/public/downloads/datasheets/LigoDLB%20ac/LigoDLB_5ac.pdf ؛ https://www.ligowave.com/products/ligodlb-5ac | تأییدِ صریح. |
| `LIGOWAVE_WJET_V_5GHZ` | DN-0164 | هر دو دیتاشیتِ RapidFire فقط «پروتکلِ بی‌سیم: W-Jet V» را می‌نویسند و جملهٔ سازگاریِ جدا ندارند. | https://www.ligowave.com/public/downloads/datasheets/LigoPTP/RapidFire (دیتاشیتِ ligoPTP 5-N) | جملهٔ صریح پیدا نشد؛ باندِ ۵-N (4900–6100) و ۶-N (5900–6400) هم‌پوشانی دارند → جدا نگه داشته شد → سؤال. |
| `LIGOWAVE_WJET_V_6GHZ` | DN-0165 | همان‌طور که بالا: فقط «W-Jet V» در دیتاشیتِ ligoPTP 6-N. | https://www.ligowave.com/public/downloads/datasheets/LigoPTP/PTP%206-N (دیتاشیتِ ligoPTP 6-N) | جدا نگه داشته شد → سؤال. |
| `RACOM_RAY3_24` | DN-0106، DN-0107 | راهنمای RAy3 (بخشِ ۱٫۱٫۱): همهٔ واحدهای باندِ ۱۷ یا ۲۴ گیگ سخت‌افزارِ یکسان‌اند و تنظیماتِ کارخانه‌ایِ جفت از دو کانالِ متفاوت برای واحدِ L و U استفاده می‌کند تا لینک با پارامترهای پیش‌فرض برقرار شود. | https://www.racom.eu/eng/products/m/ray3/product.html | برای RAy3-24 تأیید شد؛ ولی باندِ ۲۴ گیگ از مشخصاتِ قدیمیِ خودِ سایت برداشته شده (عنوانِ محصول باند نمی‌دهد) → نیازمندِ تأییدِ مالک. |

## ۴ — محصولاتِ بی‌منبع و ردیف‌هایِ ناقص

**بی‌منبع (۱ ردیف):** `source_url` خالی و دلیلش در `notes`.

- `DN-0218` — رادیو کمبیوم نتورک 6GHz FORCE 4630 — آنتن یکپارچه ۳۰dBi

**وایمکس‌نیرِ بی‌برگه (۶ ردیف):** `source_url` = صفحهٔ برندِ سایت، ستون‌هایِ فنی خالی، «از شرکت پرسیده شود».

- `DN-0004` — آنتن دیش وایمکس‌نیر WimaxNear SP62-30 XD—۳۰dBi مختص Mimosa C5X
- `DN-0006` — آنتن دیش وایمکس‌نیر دوال WimaxNear SP62-33D Low band ۳۳dBi
- `DN-0007` — آنتن دیش وایمکس نیر دوال SP62-35D - ۳۵dBi Wimaxnear  
- `DN-0010` — آنتن دیش وایمکس‌نیر ۳۰دی بی دوال  WimaxNear Sp62-30D -Low band
- `DN-0012` — آنتن دیش وایمکس‌نیر هایپرفورمنس  ۳۲dBi دوقطبی WimaxNear SP62-32D-HP رادوم‌دار
- `DN-0015` — آنتن دیش وایمکس نیر 29dbi مدل WimaxNear sp62-29D Hband 

**ردیف‌هایی که منبع دارند ولی عددِ مهمی در منبع نیست:**

- `DN-0024` — اکسس پوینت آتدور اینجینیوس ENS500EXT — 5GHz N300: در منبع نیست → حساسیت
- `DN-0026` — اکسس پوینت EnGenius ENH1350EXT: در منبع نیست → حساسیت
- `DN-0027` — اکسس پوینت EnGenius ENS620EXT: در منبع نیست → حساسیت
- `DN-0081` — رادیو میکروتیک Wireless Wire nRAY: در منبع نیست → توان، حساسیت، بهره
- `DN-0089` — رادیو وایرلس میکروتیک LHG 60G: در منبع نیست → توان، حساسیت، بهره
- `DN-0105` — لینک بی‌سیم میکروتیک LHG 60G — Wireless Wire Dish (مدل RBLHGG-60adkit): در منبع نیست → توان، حساسیت، بهره
- `DN-0228` — رادیو یوبیکیوتی AirFiber 60 XG: در منبع نیست → توان، حساسیت، بهره
- `DN-0227` — رادیو یوبیکیوتی AirFiber 60 LR: در منبع نیست → توان، حساسیت

## ۵ — اختلاف با متنِ فعلیِ سایت (در این دور اصلاح نمی‌شود؛ تسکِ جدا بعد از انتشار)

### ۵-الف — بهره، باند، توان

| محصول | متنِ سایت | عددِ منبع | پیوند |
|---|---|---|---|
| `DN-0002` آنتن دیش فاز Phase-30ISO-WB-HP — ۳۰dBi دووال | بهره: ۳۰ دسی‌بل | بهرهٔ اسمی 30، بیشینه در باند 31.3 (کمینه 29.2 در 4500MHz) → `gain_dbi=31.3` طبقِ قاعدهٔ «بیشینه در باند» | https://fara-moj.ir/wp-content/uploads/2025/10/catalog-faramoj-phase-v-3.5.0.pdf |
| `DN-0068` LDF 5 ac/به همراه آنتن 29 dbi | بهره: ۲۹dBi (با دیشِ همراه) | بهرهٔ خودِ LDF ۹dBi؛ ۲۹dBi فقط ترکیبِ رادیو با دیشِ ژنریکِ همراه است → `gain_dbi=9` | https://mikrotik.com/product/ldf_5_ac |
| `DN-0086` رادیو وایرلس میکروتیک Groove A-52HPn | «کانکتورِ خارجی (۶dBi داخلی)» | آنتنِ همراهِ جداشدنی با بهرهٔ باندی: 6dBi در 2.4GHz و 8dBi در 5GHz → `kind=RADIO`، ستونِ بهره خالی | https://mikrotik.com/product/RBGrooveA-52HPnr2 |
| `DN-0092` رادیو وایرلس میکروتیک Metal 52 ac | آنتن N-male + omni ۸dBi همراه | جدولِ مشخصاتِ سازنده یک عددِ 6dBi برایِ هر دو باند؛ «Included parts»: omni ۶/۸dBi | https://mikrotik.com/product/RBMetalG-52SHPacn |
| `DN-0188` رادیو میموسا C6x | توان: ۲۷dBm | دیتاشیتِ فعلی (DS-2026-04): 24dBm؛ دیتاشیتِ قدیمی‌ترِ DS-2025-03: 27dBm و باندِ 5150–6425 | https://mimosa.co/sites/default/files/2026-07/Mimosa-Radisys-DS-2026-07-C6x.pdf |
| `DN-0219` Air Grid M5 27dbi | باند: ۵٫۱–۵٫۸۵GHz | دیتاشیت: 5725–5850MHz | https://dl.ui.com/datasheets/airgridm/airGrid_HP.pdf |
| `DN-0018` رادیو کلاینت یوبیکیوتی LiteBeam 5AC-23 | باند: ۵٫۱–۵٫۸GHz | دیتاشیت: 5150–5875MHz (آمریکا 5150–5850) | https://dl.ubnt.com/datasheets/LiteBeam/LiteBeam_ds.pdf |
| `DN-0187` رادیو میموسا C5X | باند: ۵٫۱–۵٫۸۵GHz | دیتاشیت: 4900–6400MHz (محدود به مقرراتِ کشور) | https://mimosa.co/sites/default/files/2025-02/Mimosa-Radisys-C5x-DS-2024-01-IP67.pdf |
| `DN-0185` رادیو میموسا B5x | باند: ۵٫۱–۵٫۸۵GHz | دیتاشیت: 4900–6400MHz | https://mimosa.co/sites/default/files/2024-07/Mimosa-Radisys-B5x-Datasheet_DS-2023-12.pdf |
| `DN-0183` رادیو میموسا A5c | باند: ۵٫۱–۵٫۸۵GHz | دیتاشیت: 5150–6400MHz | https://mimosa.co/sites/default/files/2024-07/Mimosa-Radisys-A5c-Datasheet_DS-2023-12.pdf |
| `DN-0184` رادیو میموسا B5C | باند: ۵٫۱–۵٫۸۵GHz | دیتاشیت: 4900–6200MHz | https://www.cladirect.com/wp-content/uploads/2021/01/Mimosa-by-Airspan-B5c-Datasheet_DS-0008-09.pdf |
| `DN-0186` رادیو میموسا C5C | باند: ۵٫۱–۵٫۸۵GHz | دیتاشیت: 4900–6400MHz | https://mimosa.co/sites/default/files/2024-06/Mimosa-Radisys-C5c-Datasheet_DS-2024-01.pdf |
| `DN-0001` HGO-antenna-OUT میکروتیک | بهره و باند در سایت نیست | 3.6dBi در 2.4GHz و 6.7dBi در 5GHz؛ باند 2400–2500 و 5150–5850MHz | https://mikrotik.com/product/hgo_antenna_out |
| `DN-0215` رادیو موتورولا کمبیوم نت ورکز PTP 670 Connectorized | باند: ۴٫۹–۶GHz | ۴۹۰۰ تا ۶۰۵۰MHz | https://brandcentral.cambiumnetworks.com/asset/2eb0a206-615f-4203-a1c0-16e5b5a01efe/Cambium_Networks_data_sheet_PTP_670.pdf |
| `DN-0165` رادیو بی سیم لیگوویو LigoPTP 6-N RapidFire | باند: 6GHz | ۵۹۰۰ تا ۶۴۰۰MHz | https://www.ligowave.com/public/downloads/datasheets/LigoPTP/PTP%206-N,%206-25%20RapidFire.pdf |
| `DN-0164` رادیو 5گیگاهرتز لیگوویو مدل ligoPTP 5-N RapidFire | باند: 5GHz · توان: تا 30dBm | ۴۹۰۰ تا ۶۱۰۰MHz · تا 31dBm (در 65Mbps) | https://www.ligowave.com/public/downloads/datasheets/LigoPTP/RapidFire%205-N,%205-23%20Datasheet.pdf |

جمعِ ۵-الف: **۱۶** مورد.

### ۵-ب — اختلاف‌هایِ دیگر (نوعِ آنتن، کانکتور، تغذیه، ظرفیت، ابعاد، IP)

| محصول | متنِ سایت | عددِ منبع | پیوند |
|---|---|---|---|
| `DN-0093` رادیو وایرلس میکروتیک NetBox 5 | آنتن: یکپارچه | سازنده: کانکتورِ RP-SMA برایِ آنتن → آنتنِ جدا؛ `kind=RADIO` | https://mikrotik.com/product/RB911G-5HPacD-NB |
| `DN-0081` رادیو میکروتیک Wireless Wire nRAY | گذردهی تا ۱٫۵Gbps · مصرف ۱۵W | بروشور: «2 Gb/s aggregate link»؛ دیتاشیت: حداکثر مصرف 6W | https://mikrotik.com/product/wireless_wire_nray |
| `DN-0076` اکسس پوینت میکروتیک مدل QRT 5 ac | سکتورِ مسطح (SECTOR) | سازنده «flat panel antenna» → `antenna_type=PANEL`؛ بهره (24dBi) یکی است | https://mikrotik.com/product/RB911G-5HPacD-QRT |
| `DN-0002` آنتن دیش فاز Phase-30ISO-WB-HP — ۳۰dBi دووال | F/B: تا ۳۰dB | کاتالوگ: ≥30dB (حداقل، نه سقف) | https://fara-moj.ir/wp-content/uploads/2025/10/catalog-faramoj-phase-v-3.5.0.pdf |
| `DN-0003` آنتن دیش فاز Phase-34-WB-NP — ۳۴dBi دوقطبی سوراخ دار | F/B: تا ۴۲dB · نوع: Solid Dish | کاتالوگ: ≥42dB؛ متنِ سازنده «Semi Punched Dish» | https://fara-moj.ir/wp-content/uploads/2025/10/catalog-faramoj-phase-v-3.5.0.pdf |
| `DN-0001` HGO-antenna-OUT میکروتیک | برند HGO · کانکتور N-Female یا SMA · دستهٔ آنتن‌دیش | محصولِ خودِ میکروتیک؛ فقط RP-SMA Male؛ آنتنِ همه‌جهتهٔ کوچک است نه دیش | https://mikrotik.com/product/hgo_antenna_out |
| `DN-0219` Air Grid M5 27dbi | ظرفیت تا 150Mbps · IP55 | دیتاشیت: «100+ Mbps real outdoor throughput»؛ IP55 در دیتاشیت نیست | https://dl.ui.com/datasheets/airgridm/airGrid_HP.pdf |
| `DN-0226` رادیو یوبیکیوتی AirFiber 5XHD | (آنتنِ جدا ذکر نشده) · INVICTUS | دو کانکتورِ RP-SMA؛ آنتنِ جدا لازم است؛ «INVICTUS» در دیتاشیت نیست | https://dl.ui.com/datasheets/airfiber/airFiber_5XHD_DS.pdf |
| `DN-0228` رادیو یوبیکیوتی AirFiber 60 XG | ۲×SFP+ · برد ۵۰۰m/۲km · PoE++ 802.3bt · IP55 | یک RJ45 + یک SFP+ (1/10G)؛ برد 4km؛ Passive PoE 48V؛ IP55 در منبع نیست | https://dl.ui.com/ds/af60-xg_ds.pdf |
| `DN-0227` رادیو یوبیکیوتی AirFiber 60 LR | ظرفیت 1Gbps · برد ۲km · 802.3af/at · IP55 | ظرفیت 1.9Gbps؛ برد 12+km؛ Passive PoE 48V؛ IP55 در منبع نیست | https://dl.ui.com/ds/af60-lr_ds.pdf |
| `DN-0018` رادیو کلاینت یوبیکیوتی LiteBeam 5AC-23 | IP66 · ۴۵۰Mbps | هیچ‌کدام در دیتاشیتِ این مدل (LBE-5AC-23) نیست | https://dl.ubnt.com/datasheets/LiteBeam/LiteBeam_ds.pdf |
| `DN-0188` رادیو میموسا C6x | ابعاد ۱۷۵×۷۰×۶۱mm | منبع 178×113×67mm (ابعادِ سایت مالِ C5x است) | https://mimosa.co/sites/default/files/2026-07/Mimosa-Radisys-DS-2026-07-C6x.pdf |
| `DN-0187` رادیو میموسا C5X | Connectorized با ۲×N-type | کانکتورِ twist-on اختصاصیِ N5-X؛ N-type ندارد | https://mimosa.co/sites/default/files/2025-02/Mimosa-Radisys-C5x-DS-2024-01-IP67.pdf |
| `DN-0185` رادیو میموسا B5x | ظرفیت ۱٫۵Gbps · تغذیه 24V | ۱٫۵Gbps «IP aggregate» است (PHY 1.7Gbps)؛ تغذیه 48V؛ تعدادِ پورتِ اترنت در منبع نیست | https://mimosa.co/sites/default/files/2024-07/Mimosa-Radisys-B5x-Datasheet_DS-2023-12.pdf |
| `DN-0183` رادیو میموسا A5c | تغذیه Passive PoE 24V · سکتور ۶۰–۹۰° | 802.3at یا Passive PoE 48–56V؛ زاویهٔ سکتور در دیتاشیت نیست | https://mimosa.co/sites/default/files/2024-07/Mimosa-Radisys-A5c-Datasheet_DS-2023-12.pdf |
| `DN-0184` رادیو میموسا B5C | ۴×N-type · تغذیه 24V | ۲×N-type؛ تغذیه 48V؛ تعدادِ پورتِ اترنت در منبع نیست | https://www.cladirect.com/wp-content/uploads/2021/01/Mimosa-by-Airspan-B5c-Datasheet_DS-0008-09.pdf |
| `DN-0186` رادیو میموسا C5C | MIMO ۴×۴ · N-type · IP67 · ۲ پورتِ گیگابیت · تأخیر <1ms · ۵۰km | 2x2:2؛ ۲×RP-SMA؛ IP55؛ تعدادِ پورت نیست؛ تأخیر و ۵۰km در دیتاشیت نیست | https://mimosa.co/sites/default/files/2024-06/Mimosa-Radisys-C5c-Datasheet_DS-2024-01.pdf |
| `DN-0026` اکسس پوینت EnGenius ENH1350EXT | 5GHz تا 866Mbps · 24V passive PoE · ۲۲۲×۱۱۲×۵۴mm، ۶۲۰g | 867Mbps؛ تغذیه در اسنادِ خودِ سازنده ناهمسان (24V صفحه، 48V دیتاشیت، 54V جدولِ Station)؛ ابعاد 173.6×111.2×30.3mm و 295g | https://www.engeniustech.com/engenius-products/enturbo-ac1300-wave-2-outdoor-wireless-access-point/ |
| `DN-0027` اکسس پوینت EnGenius ENS620EXT | PoE IN + PoE OUT · دما -30..+70°C | PoE-out در منبع نیست؛ دما -20..+60°C | https://www.engeniustech.com/wp-content/uploads/2017/07/ENS620EXT-DS.pdf |
| `DN-0215` رادیو موتورولا کمبیوم نت ورکز PTP 670 Connectorized | IP55 | IP66/IP67 (دیتاشیت) | https://brandcentral.cambiumnetworks.com/asset/2eb0a206-615f-4203-a1c0-16e5b5a01efe/Cambium_Networks_data_sheet_PTP_670.pdf |
| `DN-0217` رادیو کمبیوم نتورک 6GHz FORCE 4625 | تغذیه 18–56V · مصرف ~14W · وزن ~2kg · دما تا +65°C | ۴۴ تا ۵۹V · 13W · 2.76kg · دما تا 55°C | https://brandcentral.cambiumnetworks.com/m/e1aa7c6c90f854f/original/Cambium_Networks_data_sheet_ePMP_Force_4625_SM.pdf |
| `DN-0216` رادیو وایرلس Cambium Force 4600C | دما -30 تا +55°C | دیتاشیتِ ۲۰۲۶: -40 تا 55°C (نسخهٔ ۲۰۲۳: -30) | https://brandcentral.cambiumnetworks.com/m/346e712e03fd0741/original/Cambium_Networks_data_sheet_ePMP_Force_4600_Series_SM.pdf |
| `DN-0165` رادیو بی سیم لیگوویو LigoPTP 6-N RapidFire | پهنای کانال 5/10/20/40/80 | 5, 10, 14, 15, 20, 30, 40, 60, 80 | https://www.ligowave.com/public/downloads/datasheets/LigoPTP/PTP%206-N,%206-25%20RapidFire.pdf |
| `DN-0164` رادیو 5گیگاهرتز لیگوویو مدل ligoPTP 5-N RapidFire | استاندارد 802.11a/n/ac | پروتکلِ W-Jet V | https://www.ligowave.com/public/downloads/datasheets/LigoPTP/RapidFire%205-N,%205-23%20Datasheet.pdf |
| `DN-0163` LigoDLB 5ac | ابعاد ۱۴۰×۱۴۰×۵۶mm · وزن ~500g · IP67 | ۱۵۰×۱۱۵×۵۵mm · 450g · IP67 در دیتاشیت نیست | https://www.ligowave.com/public/downloads/datasheets/LigoDLB%20ac/LigoDLB_5ac.pdf |
| `DN-0106` لینک مایکروویو Racom RAy3 (1Gbps / آنتن 3ft) | «ODU/آنتن یکپارچه» · PoE 40–60V · ظرفیتِ ۱Gbps | سازنده آنتنِ پارابولیکِ خارجیِ جدا می‌خواهد؛ PoE ۲۰ تا ۶۰V؛ ظرفیت کلیدِ نرم‌افزاری است (پیش‌فرضِ سخت‌افزار 360Mb/s) | https://www.racom.eu/eng/products/m/ray3/tech.html |
| `DN-0107` لینک مایکروویو Racom RAy3 (500Mbps / آنتن 2ft) | «ODU/آنتن یکپارچه» · PoE 40–60V · ظرفیتِ ۵۰۰Mbps · رمزگذاری AES | آنتنِ خارجیِ جدا؛ ۲۰ تا ۶۰V؛ کلیدِ نرم‌افزاری؛ رمزگذاری فقط با کلیدِ ویژه و روی مدل‌های E/X/S | https://www.racom.eu/eng/products/m/ray3/tech.html |
| `DN-0218` رادیو کمبیوم نتورک 6GHz FORCE 4630 — آنتن یکپارچه ۳۰dBi | techSpecs: «آنتن یکپارچه ۳۰dBi» و ۵٫۹۲۵–۷٫۱۲۵GHz | specificationsِ قدیمیِ همین محصول: ۲×RP-SMA برای آنتنِ خارجی، گیرندگی ~۲۲dBi، باند ۴٫۷–۶٫۴GHz، IP66 (تناقضِ درونِ خودِ سایت؛ منبعِ رسمی ندارد) | — |

جمعِ ۵-ب: **۲۸** مورد.

## ۶ — سؤال‌ها

۱. وایمکس‌نیر: شش مدلِ بی‌برگه (`DN-0004` 30XD، `DN-0006` 33D Low band، `DN-0007` 35D، `DN-0010` 30D Low band، `DN-0012` 32D-HP، `DN-0015` 29D H band) برگهٔ رسمی ندارند و ستون‌هایِ فنی‌شان خالی است. کاتالوگِ قدیمیِ ص۹ و ص۱۰ فقط جدولِ خانوادگی است و مدل را نام نمی‌برد؛ به هیچ مدلی نسبت داده نشد.
۲. وایمکس‌نیر: برگهٔ 32D پهن‌باند 4800–6200 است ولی `DN-0011` H-band نامیده شده (کارتِ سایت 4700–6500 و VSWR 1.5 می‌گوید)؛ برگهٔ 34.5D Ultra-HP باندِ 4.8–6.1 دارد ولی `DN-0016` «34D Hband» است. بهره، پرتو و قطر یکی است؛ شرکت همانی‌بودن را تأیید کند. باندِ کارت‌ها دست نخورد.
۳. Force 4630 (`DN-0218`): کالای واقعی چیست؟ نامِ اشتباهِ 4625؟ 4600C با دیشِ بیرونی؟ کالایی غیرِ کمبیوم؟
۴. RAy3: واقعاً RAy3-24 است (۲۴٫۰۰–۲۴٫۲۵ گیگ)؟ بسته کدام آنتن را دارد؟ طبقِ سازنده هر واحد به آنتنِ پارابولیکِ خارجی نیاز دارد. ظرفیتِ ۵۰۰Mbps/۱Gbps هم کلیدِ نرم‌افزاری است؛ `sens_high` طبقِ عنوان با بالاترین ترکیبِ مجازِ جدولِ کلیدها (۲۰۴۸QAM) ثبت شد.
۵. ENH1350EXT (۲۳) و ENS500EXT (۲۶): صفحه‌هایِ منطقه‌ایِ EU/APAC/JP «15dBm» می‌نویسند؛ آن را سقفِ مقرراتیِ منطقه گرفتم و لحاظ نکردم (مثلِ ENS620EXT که مدیر ۲۶ گذاشت، نه ۱۵). اگر ۱۵ می‌خواهید بگویید.
۶. کنبوتونگ (`DN-0017`): سازنده «P9A×2» (دووال، دو N) و «P9A» (تک‌پلاریته) را جدا دارد؛ چون سایت «دووال و ۲×N» می‌گوید ردیفِ ×2 مبنا شد. یک‌بار کالای فیزیکی تأیید شود.
۷. SP-7036N (دیشِ ۷ گیگاهرتز): کاتالوگ رسیده ولی محصول روی سایت نیست؛ ساختِ کارت و ردیفِ `rf-specs` منتظرِ قیمت و کدِ هلو از مالک است.
۸. محصولاتی که سازنده در صفحه‌اش «Discontinued/Legacy/EOL» کرده (موضوعِ تجاری، نه عددی): DynaDish 5 ac، OmniTIK 5 ac، NetBox 5، SXTsq 5 ac، SXTsq 5 High Power، SXT SA5 (نسخهٔ n)، wAP ac، ENS500EXT، ENS620EXT، B5C. جایگزینِ رسمیِ بعضی در `notes` آمده.

## ۷ — روش و سنجشِ دقت

- منبع: صفحهٔ محصول/دیتاشیتِ خودِ سازنده؛ برایِ وایمکس‌نیر (برندِ خودِ شرکت) کاتالوگ‌ها و برگه‌هایِ خودِ شرکت. برایِ عددها، HTML یا PDFِ خام را با `curl` دانلود و متنِ استخراج‌شده را خواندم (نه خلاصهٔ مدلِ جستجو)؛ جدولِ Tx/Rxِ میکروتیک در DOM پنهان است و با پارسِ HTMLِ خام درآمد. کاتالوگ‌هایِ تازهٔ وایمکس‌نیر تصویری‌اند؛ صفحه‌ها را رندر کردم و با چشم خواندم و با جدولِ مدیر سنجیدم.
- دامنه با مرورِ نامِ هر ۲۲۹ محصول بسته شد، نه فقط با دسته‌ها؛ در این مرور سه رادیوی جاافتاده در دستهٔ ریشهٔ «میکروتیک» پیدا و اضافه شد.
- کار بینِ چند عاملِ تحقیق تقسیم شد؛ خروجی‌ها قبل از ادغام اعتبارسنجی شد: `tx_max` با بیشینهٔ جدولِ هر رادیو و `sens_low`/`sens_high` با یک ردیفِ همان جدول می‌خوانند (صفر ناهمخوانی).
- اصلاح‌هایِ ادغام (عددِ حدسی یا غیرِ منبع از ستون بیرون رفت و در `notes` ماند): سقفِ EIRPِ ۶۰گیگ (nRAY، LHG 60G، کیت) از `tx_max_dbm`؛ باندِ عددی و پلاریزاسیونِ LHG 5 ax و LHG XL 5 ax (صفحه فقط «5 GHz» دارد)؛ نوعِ آنتنِ AF60-XG و قطرِ AF60-LR (قطرِ کلِ بدنه)؛ `rate_mbps` ردیف‌هایِ 1x در AF-5XHD؛ `beamwidth` مشکوکِ wAP ac.
- سنجشِ مستقلِ من با خودِ دیتاشیتِ رسمی: AirGrid M5 HP، LiteBeam 5AC-23، Mimosa C5x، AF-5XHD (جدولِ حساسیت و ظرفیت)، Cambium Force 4625، صفحهٔ RAy3-17/24 راکام، و چهار کاتالوگِ تازهٔ وایمکس‌نیر؛ همه با اعدادِ ثبت‌شده یکی بود. بقیه را فقط عاملِ تحقیق و اعتبارسنجیِ خودکار دیده؛ مدیر چند ردیفِ دیگر را هم مستقل بسنجد.
- فایل‌ها بدونِ BOM و با UTF-8 نوشته شده‌اند. کاتالوگِ فاز (fara-moj.ir) فقط تصویر است؛ عددهایِ `DN-0002` و `DN-0003` از روی تصویرِ صفحه‌هایِ ۱۱ و ۱۴ خوانده شد (یکی از عامل‌ها `pymupdf` را با pip نصب کرد). دیتاشیتِ zipِ کنبوتونگ دانلود نشد؛ اعدادِ `DN-0017` از جدولِ مشخصاتِ صفحهٔ رسمیِ kenbotong.com آمد.

## ۸ — کارتِ آنتن‌هایِ وایمکس‌نیر روی سایت

**وضعیت:** اعمال شد (۱۱ مهر، با ورودِ ادمین در مرورگرِ داخلی). `PUT` هر دو با ۲۰۰ برگشت؛ بعد از ذخیره فقط `techSpecs` و `updatedAt` عوض شده بود (اسلاگ، نام، قیمت، موجودی دست‌نخورده) و صفحهٔ `/shop/product/…` هر دو را از سرور باز کردم و مقدارهای تازه در جدولِ مشخصات نشسته بود.

**پشتیبان (پیش از هر تغییر؛ پیش از اعمال، هشِ `techSpecs` هر ۱۲ محصول با پشتیبان برابر بود):** `docs/reports/backup-wimaxnear-cards-1405-07-11.json` — نسخهٔ کاملِ هر ۱۲ محصول (شاملِ `techSpecs`) از APIِ زنده، ۱۱ مهر. ساختنِ کالکشنِ `_backup_…` در مونگو از راهِ APIِ ادمین ممکن نیست؛ اگر کالکشن لازم است، همان روشِ `scripts/fix-wimaxnear-antenna-specs.js` (با `getCollection`) روی سرور اجرا شود. بازگردانی: همین فایل با `PUT /api/v1/products/{id}`.

**قاعده:** فقط ردیفی که با منبعِ همان مدل نمی‌خواند؛ اسلاگ و نام و بقیهٔ فیلدها دست نمی‌خورد (در `PUT` همان اسلاگِ فعلی فرستاده می‌شود و `slugify` رویش بی‌اثر است؛ موجودی و قیمت در بدنه نیست).

| محصول | ردیف | از | به | منبع |
|---|---|---|---|---|
| `DN-0011` SP62-32D | برند | WimaxNear Sp62-32D H-band | WimaxNear | برگهٔ ds-sp62-32d.jpg (لوگو و «D/N: SP62-32D») |
| `DN-0011` SP62-32D | مدل | 32dBi | SP62-32D | همان برگه، «D/N: SP62-32D» |
| `DN-0016` SP62-34D | برند | Wimaxnear SP62-34D Hband | WimaxNear | برگهٔ ds-sp62-345d.jpg |
| `DN-0016` SP62-34D | XPD | ۳۱ دسی‌بل | ۳۳ دسی‌بل | برگهٔ ds-sp62-345d.jpg، «XPD 33 dB» |
| `DN-0016` SP62-34D | نسبت جلو به عقب | ۴۷ دسی‌بل | ۵۸ دسی‌بل | همان برگه، «F/B Ratio 58 dB» |
| `DN-0016` SP62-34D | وزن | ۲۱ کیلوگرم | ۲۵ کیلوگرم | همان برگه، «Weight 25 kg» |

**بی‌تغییر چون با کاتالوگِ تازه می‌خواند:** `DN-0008` (27D)، `DN-0005` (28D)، `DN-0009` (30D)، `DN-0013` (33D) — بهره، باند ۴۷۰۰–۶۵۰۰، پهنای پرتو، VSWR و (جایی که هست) قطر همه یکی بود.

**دست‌نخورده (بی‌برگه):** `DN-0004`، `DN-0006`، `DN-0007`، `DN-0010`، `DN-0012`، `DN-0015`.

**مغایر ولی عمداً عوض نشد (باندِ H-band در برابرِ برگهٔ پهن‌باند):** `DN-0011` باند ۴۷۰۰–۶۵۰۰ (برگه ۴۸۰۰–۶۲۰۰) و VSWR ‏1.5:1 (برگه 1.7:1)؛ `DN-0016` باند ۴٫۷–۶٫۵ (برگه ۴٫۸–۶٫۱). دلیل: برگه‌ها برایِ نسخهٔ پهن‌باند است؛ کاتالوگِ H-band که برسد، همان مقدم است. نامِ محصول هم عوض نشد (پیشنهاد: مدلِ `DN-0016` در نام «34D» است و برگه «34.5D Ultra-HP»).

**متنِ توضیح و پرسش‌هایِ `DN-0016` هنوز عددهایِ قدیمی را دارد** (F/B ۴۷ دسی‌بل، XPD سی‌ویک دسی‌بل، وزن ۲۱ کیلوگرم، و در پرسش‌ها «۱۱۰ سانتی و ۲۱ کیلوگرمی») و با ردیف‌هایِ تازه نمی‌خواند؛ دستور فقط جدولِ مشخصات بود و متن را عوض نکردم. اگر مدیر عددهایِ برگه را پذیرفت، متن هم باید همسان شود؛ اگر نه، سه ردیفِ جدول با پشتیبان برمی‌گردد.

**ردیفِ قطر** روی کارتِ `DN-0005` (≈54 cm) و `DN-0009` (≈66 cm) نیست؛ اضافه نکردم چون دستور «فقط ردیفِ مغایر» بود. اگر بخواهید اضافه می‌شود.
