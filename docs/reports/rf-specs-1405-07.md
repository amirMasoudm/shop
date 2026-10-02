# گزارش — دادهٔ فنیِ آنتن و رادیو از منبعِ رسمیِ سازنده (۱۴۰۵/۰۷/۱۰)

تحقیقِ بدونِ تغییرِ دیتابیس برایِ ماشین‌حسابِ «داده‌لینک». هر عدد از صفحهٔ محصول یا دیتاشیتِ خودِ سازنده آمده؛ عددی که منبع نداشت خالی ماند و در `notes` نوشته شد «در منبع نیست». هیچ چیزی در سایت یا دیتابیس اصلاح نشد.

فایل‌ها: [`rf-specs-1405-07.csv`](rf-specs-1405-07.csv) (یک ردیف برایِ هر محصول) · [`rf-specs-mcs-1405-07.csv`](rf-specs-mcs-1405-07.csv) (جدولِ کاملِ نرخ‌ها).

## خلاصهٔ تصمیم‌های لازم از مدیر

- **آنتن‌های وایمکس‌نیر (۱۲ از ۱۸ آنتن):** سازنده سایتِ رسمیِ جدا ندارد؛ `wimaxnear.com` به فروشگاهِ irmikro.com می‌رود. اعدادِ آن صفحه‌ها طبقِ قاعدهٔ «فروشگاه منبع نیست» در ستون‌های اصلی نیامد (فقط در `notes` به‌عنوانِ مرجعِ نامعتبر). تصمیم: بپذیریم یا بی‌منبع بماند؟
- **Force 4630 (`DN-0218`):** در سایتِ کمبیوم نه دیتاشیت دارد و نه اسمش در فهرستِ ePMP است (فقط 4616 و 4600C و 4625)؛ خودِ سایتِ ما هم دربارهٔ آنتنش با خودش تناقض دارد. کالای واقعی چیست؟ تا روشن نشود عددی وارد نمی‌شود.
- **RAy3 راکام (`DN-0106`، `DN-0107`):** عنوانِ محصول باند نمی‌دهد؛ ۲۴ گیگ از مشخصاتِ قدیمیِ خودِ سایت برداشته شد و تأیید می‌خواهد. آنتنِ پارابولیک جدا خریده می‌شود (بهره خالی).
- **۶۰گیگ میکروتیک (Cube 60Pro، nRAY، LHG 60G):** بهره، توان و حساسیتِ رادیوی ۶۰گیگ را سازنده منتشر نکرده؛ خالی ماند. این‌ها نباید در «پیشنهادِ محصول» داده‌لینک با عددِ ساختگی بیایند.
- **خانواده‌های سازگاریِ مبهم** (بخشِ ۳ و سؤال‌های ۱۱ تا ۱۵) و **منبعِ B5C** (دیتاشیتِ ساختِ میموسا ولی میزبان: cladirect.com) نیاز به تصمیمِ مدیر دارند.

## ۱ — دامنه و تعدادها

کاتالوگِ زندهٔ سایت (`/api/v1/products`) ۲۲۹ محصول دارد؛ **۸۳ محصول در دامنه** است (۲۵ رادیوی کانکتوردار، ۳۹ رادیوی آنتن‌داخلی، ۱۸ آنتن) و ۱۴۶ محصول بیرونِ دامنه.

| دستهٔ سایت | آنتن | رادیو (کانکتوردار) | رادیو (آنتن‌داخلی) | جمع |
|---|---|---|---|---|
| `access-point` | ۰ | ۲ | ۰ | ۲ |
| `access-point-mikrotik` | ۰ | ۰ | ۲ | ۲ |
| `antenna-dish` | ۱۶ | ۰ | ۰ | ۱۶ |
| `cambium` | ۰ | ۲ | ۱ | ۳ |
| `engenius` | ۰ | ۱ | ۰ | ۱ |
| `ligowave` | ۰ | ۳ | ۰ | ۳ |
| `mikrotik` | ۰ | ۱ | ۲ | ۳ |
| `mimosa` | ۰ | ۳ | ۳ | ۶ |
| `racom` | ۰ | ۲ | ۰ | ۲ |
| `ubnt` | ۰ | ۱ | ۴ | ۵ |
| `wireless-outdoor-mikrotik` | ۲ | ۱۰ | ۲۷ | ۳۹ |
| **جمع** | **۱۸** | **۲۵** | **۳۹** | **۸۳** |

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
| SP62-35D و SP62-29D وایمکس‌نیر — `DN-0007` `DN-0015` | داخل (بی‌منبع) | در فهرستِ ۱۲ محصولیِ برند نیستند |
| دستهٔ `microwave-radio` | بدونِ محصول | دسته خالی است |

## ۲ — پوششِ هر ستون (چند از چند)

| ستون | مخرج | پر شده |
|---|---|---|
| `bands_mhz` | همهٔ ردیف‌ها | ۵۶ از ۸۳ |
| `gain_dbi` | آنتن + رادیوی آنتن‌داخلی | ۴۱ از ۵۷ |
| `antenna_type` | آنتن + رادیوی آنتن‌داخلی | ۳۵ از ۵۷ |
| `diameter_cm` | فقط دیش‌ها (`antenna_type=DISH`) | ۱۱ از ۱۲ |
| `beamwidth_deg` | آنتن + رادیوی آنتن‌داخلی | ۱۵ از ۵۷ |
| `polarization` | آنتن + رادیوی آنتن‌داخلی | ۱۸ از ۵۷ |
| `tx_max_dbm` | رادیوها | ۵۹ از ۶۴ |
| `sens_low_dbm` | رادیوها | ۵۶ از ۶۴ |
| `sens_high_dbm` | رادیوها | ۵۰ از ۶۴ |
| `compat_family` | رادیوها | ۶۴ از ۶۴ |
| `source_url` | همهٔ ردیف‌ها | ۷۰ از ۸۳ |
| جدولِ کاملِ نرخ‌ها (فایلِ دوم) | رادیوها | ۵۶ از ۶۴ |

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

**بی‌منبع (۱۳ ردیف):** `source_url` خالی و دلیلش در `notes`.

- `DN-0004` — آنتن دیش وایمکس‌نیر WimaxNear SP62-30 XD—۳۰dBi مختص Mimosa C5X
- `DN-0005` — آنتن دیش وایمکس‌نیر WimaxNear SP62-28D — ۲۸dBi dual Hband  
- `DN-0006` — آنتن دیش وایمکس‌نیر دوال WimaxNear SP62-33D Low band ۳۳dBi
- `DN-0007` — آنتن دیش وایمکس نیر دوال SP62-35D - ۳۵dBi Wimaxnear  
- `DN-0008` — آنتن دیش وایمکس‌نیر WimaxNear SP62-27D dual Hband ۲۷Dbi
- `DN-0009` — آنتن دیش وایمکس‌نیر WimaxNear SP62-30D Hband Dual pol 30Dbi
- `DN-0010` — آنتن دیش وایمکس‌نیر ۳۰دی بی دوال  WimaxNear Sp62-30D -Low band
- `DN-0011` — آنتن دیش ۳۲دی بی دوال  WimaxNear Sp62-32D H-band
- `DN-0012` — آنتن دیش وایمکس‌نیر هایپرفورمنس  ۳۲dBi دوقطبی WimaxNear SP62-32D-HP رادوم‌دار
- `DN-0013` — آنتن دیش وایمکس‌نیر ۳۳dBi دوال  WimaxNear SP62-33D Hband
- `DN-0015` — آنتن دیش وایمکس نیر 29dbi مدل WimaxNear sp62-29D Hband 
- `DN-0016` — آنتن دیش وایمکس نیر  WimaxNear SP62-34D Hband dual34dbi
- `DN-0218` — رادیو کمبیوم نتورک 6GHz FORCE 4630 — آنتن یکپارچه ۳۰dBi

**ردیف‌هایی که منبع دارند ولی عددِ مهمی در منبع نیست:**

- `DN-0024` — اکسس پوینت آتدور اینجینیوس ENS500EXT — 5GHz N300: در منبع نیست → حساسیت
- `DN-0026` — اکسس پوینت EnGenius ENH1350EXT: در منبع نیست → حساسیت
- `DN-0027` — اکسس پوینت EnGenius ENS620EXT: در منبع نیست → حساسیت
- `DN-0081` — رادیو میکروتیک Wireless Wire nRAY: در منبع نیست → توان، حساسیت، بهره
- `DN-0089` — رادیو وایرلس میکروتیک LHG 60G: در منبع نیست → توان، حساسیت، بهره
- `DN-0105` — لینک بی‌سیم میکروتیک LHG 60G — Wireless Wire Dish (مدل RBLHGG-60adkit): در منبع نیست → توان، حساسیت، بهره
- `DN-0228` — رادیو یوبیکیوتی AirFiber 60 XG: در منبع نیست → توان، حساسیت، بهره
- `DN-0227` — رادیو یوبیکیوتی AirFiber 60 LR: در منبع نیست → توان، حساسیت

## ۵ — اختلاف با متنِ فعلیِ سایت (چیزی در سایت اصلاح نشد)

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

### ۵-ج — آنتن‌هایِ وایمکس‌نیر: فقط مرجعِ نامعتبر

منبعِ این ۱۰ ردیف فروشگاهی است (بخش ۴)، پس اعدادش **در ستون‌هایِ اصلی نیامد** و اختلاف‌هایِ زیر «سایتِ ما در برابرِ صفحهٔ فروشگاهِ irmikro.com» است، نه در برابرِ سازنده؛ فقط برایِ اطلاعِ مدیر.

| محصول | متنِ سایتِ ما | صفحهٔ irmikro.com |
|---|---|---|
| `DN-0005` آنتن دیش وایمکس‌نیر WimaxNear SP62-28D — ۲۸dBi dual Hband   | باندِ بالا ۶۵۰۰MHz · پهنای پرتو ۷° | ۶۴۰۰MHz · ۹° |
| `DN-0008` آنتن دیش وایمکس‌نیر WimaxNear SP62-27D dual Hband ۲۷Dbi | باندِ بالا ۶۵۰۰ · بهره ۲۵٫۲ تا ۲۷٫۷ (در سراسرِ باند) | ۶۴۰۰ · بهرهٔ ثابتِ ۲۷ |
| `DN-0009` آنتن دیش وایمکس‌نیر WimaxNear SP62-30D Hband Dual pol 30Dbi | باندِ بالا ۶۵۰۰ · پهنای پرتو ۶° | ۶۴۰۰ · ۴٫۱° |
| `DN-0010` آنتن دیش وایمکس‌نیر ۳۰دی بی دوال  WimaxNear Sp62-30D -Low band | بهره ۳۰–۳۱ · پهنای پرتو ۶° | بهرهٔ ثابتِ ۳۰ · ۴٫۱° · باندِ جدول «4800-6100GHz» (غلطِ تایپیِ واحد) |
| `DN-0011` آنتن دیش ۳۲دی بی دوال  WimaxNear Sp62-32D H-band | باندِ بالا ۶۵۰۰ · بهره ۳۰٫۲ تا ۳۲٫۲ · قطر ۸۰cm، وزن ۶٫۵kg | ۶۴۰۰ · بهرهٔ ثابتِ ۳۲ · قطر ۸۲cm، وزن ۸٫۵kg |
| `DN-0013` آنتن دیش وایمکس‌نیر ۳۳dBi دوال  WimaxNear SP62-33D Hband | باندِ بالا ۶۵۰۰ · پهنای پرتو ۴° | ۶۴۰۰ · ۴٫۱° |
| `DN-0016` آنتن دیش وایمکس نیر  WimaxNear SP62-34D Hband dual34dbi | باندِ بالا ۶۵۰۰ · پهنای پرتو ۳٫۰° · قطر ۱۱۰cm · وزن ۲۱kg | ۶۴۰۰ · ۴٫۱° · ۹۲cm · ۱۰٫۵kg؛ XPD و جلو به عقب و ایزولاسیونِ سایت در صفحه نیست |
| `DN-0012` آنتن دیش وایمکس‌نیر هایپرفورمنس  ۳۲dBi دوقطبی WimaxNear SP62-32D-HP رادوم‌دار | بهره ۳۰٫۲ تا ۳۲٫۲؛ رادوم، ۸۰cm، ۶٫۵kg | متنِ صفحه ۳۲dBi ولی جدولِ همان صفحه کپیِ SP62-30D (۳۰dBi) است؛ رادوم/قطر/وزن در صفحه نیست |
| `DN-0007` آنتن دیش وایمکس نیر دوال SP62-35D - ۳۵dBi Wimaxnear   | SP62-35D؛ «N-Female» در مشخصات و «SMA» در توضیحات | مدلِ SP62-35D در فهرستِ برند نیست (فقط SP62-35D-HP) |
| `DN-0015` آنتن دیش وایمکس نیر 29dbi مدل WimaxNear sp62-29D Hband  | SP62-29D؛ بهره ۲۹٫۶؛ باند ۴٫۷–۶٫۵GHz | SP62-29D در فهرستِ ۱۲ محصولیِ برند نیست |

## ۶ — سؤال‌ها

۱. وایمکس‌نیر: irmikro.com را «سایتِ سازنده» می‌شماریم؟ (اگر نه، ۱۰ ردیفِ فعلی بی‌منبع‌اند؛ اعدادشان در `notes` آماده است.) همچنین `DN-0012` (SP62-30D-HP) در صفحه با خودش تناقض دارد (متن ۳۲dBi، جدول ۳۰dBi) و `DN-0007` (SP62-35D) و `DN-0015` (SP62-29D) در فهرستِ برند نیستند؛ آیا مدلِ دیگری با همین نام است؟
۲. B5C (`DN-0184`): تنها دیتاشیتِ متنیِ سازنده (DS-0008-09، ساختِ Mimosa by Airspan) روی سایتِ یک توزیع‌کننده (cladirect.com) میزبانی شده؛ محصولِ Legacy است و صفحه‌اش در mimosa.co به صفحهٔ دیگری می‌رود. قبول می‌کنید یا بی‌منبع شود؟
۳. Force 4630 (`DN-0218`): کالای واقعی چیست؟ نامِ اشتباهِ 4625؟ 4600C با دیشِ بیرونی؟ کالایی غیرِ کمبیوم؟
۴. RAy3: واقعاً RAy3-24 است (۲۴٫۰۰–۲۴٫۲۵ گیگ)؟ بسته کدام آنتن را دارد؟ طبقِ سازنده هر واحد به آنتنِ پارابولیکِ خارجی نیاز دارد. ظرفیتِ ۵۰۰Mbps/۱Gbps هم کلیدِ نرم‌افزاری است؛ `sens_high` طبقِ عنوان با بالاترین ترکیبِ مجازِ جدولِ کلیدها (۲۰۴۸QAM) ثبت شد.
۵. فاز (`DN-0002`، `DN-0003`): `gain_dbi` را «بیشینه در باند» (۳۱٫۳ و ۳۵٫۳) گذاشتم، طبقِ قاعدهٔ تسک؛ سازنده بهرهٔ اسمی (۳۰ و ۳۴) و کمینه (۲۹٫۲ و ۳۱٫۸ در 4500MHz) را هم داده و هر سه در `notes` است. بیشینه برای فرکانس‌های پایینِ باند ۱ تا ۳ دسی‌بل خوش‌بینانه است؛ ماشین‌حساب کدام را بخواند؟
۶. HGO-antenna-OUT (`DN-0001`): دوباند است (3.6dBi در 2.4GHz، 6.7dBi در 5GHz) و `gain_dbi` یک عدد است؛ ۶٫۷ (۵ گیگ) گذاشتم. اگر چتِ الف بهره را به‌ازای باند نگه می‌دارد، این ردیف ستونِ دوم می‌خواهد. ضمناً این آنتنِ همه‌جهتهٔ کوچک برای لینکِ نقطه‌به‌نقطه نباید پیشنهاد شود و در دستهٔ `antenna-dish` جایش نیست.
۷. ENS620EXT (`DN-0027`): توان در اسنادِ خودِ اینجینیوس ناهمسان است (۲۷ در دو دیتاشیتِ PDF و جدولِ مقایسه؛ ۲۶ در دفترچهٔ قدیمی و صفحهٔ جهانی؛ ۱۵ در جدولِ مشخصاتِ صفحهٔ آسیا-اقیانوسیه). ۲۷ ثبت شد؛ اگر محافظه‌کارانه‌تر می‌خواهید ۲۶. صفحه‌های منطقه‌ایِ ENH1350EXT و ENS500EXT هم ۱۵dBm (سقفِ مقرراتی) می‌نویسند.
۸. نسخهٔ کالا: C6x (`DN-0188`) دیتاشیتِ فعلی ۲۴dBm و قدیمی‌تر ۲۷dBm؛ LiteBeam (`DN-0018`) طبقِ مدلِ LBE-5AC-23 توان ۲۴ ثبت شد ولی نسلِ Gen2 توان ۲۵ دارد؛ wAP ac (`DN-0037`) دو ویرایشِ سخت‌افزاری دارد (جدولِ Tx/Rxِ ویرایشِ جدید RBwAPG-5HacD2HnD آمد). کالای موجودِ انبار کدام نسخه است؟
۹. RB912UAG (`DN-0191`): نامِ سایت پسوندِ مدل ندارد؛ نسخهٔ 5HPnD (۵گیگ) طبقِ مشخصاتِ ۵گیگِ سایت انتخاب شد (2HPnD ۲٫۴گیگ است). تأیید؟ ضمناً این بوردِ لخت (IP00) بدونِ آنتن و محفظه است و نباید مثلِ رادیوی آماده پیشنهاد شود.
۱۰. طبقه‌بندی: NetBox 5 (`DN-0093`) را `RADIO` گذاشتم (سازنده RP-SMA می‌گوید، سایت «یکپارچه»)؛ Metal 52 ac (`DN-0092`) را `RADIO_INTEGRATED` (کانکتورِ N خارجی ولی آنتنِ همراه و بهره در جدولِ مشخصات)؛ LiteBeam و آنتن‌های بومیِ میموسا `antenna_type` خالی دارند چون سازنده نوعش را نام نمی‌برد (سایت برای LiteBeam «پارابولیک» می‌گوید). تأیید؟
۱۱. خانوادهٔ میکروتیک: `MIKROTIK_WIFI` (11n/11ac، درایورِ wireless با Nv2) و `MIKROTIK_WIFI_AX` را جدا کردم چون سازنده Nv2 را روی ax ممکن نمی‌داند. آیا ax↔n/ac در حالتِ استانداردِ 802.11 باید پیشنهاد شود؟ جملهٔ صریحِ سازنده پیدا نشد.
۱۲. ۶۰گیگِ میکروتیک: nRAY، LHG 60G (و کیت) و Cube 60Pro (802.11ay) سه خانوادهٔ جدا شدند؛ جملهٔ سازنده دربارهٔ سازگاریِ بین‌مدلی یا ad↔ay پیدا نشد (فقط در فروشگاه‌ها).
۱۳. یوبیکیوتی: AF-5XHD با سایرِ رادیوهای LTU، و AF60-LR با AF60-XG جملهٔ صریح ندارند؛ جدا ماندند. airMAX M→APِ ac فقط در یک جهت تأیید شده.
۱۴. میموسا: A5c↔C5x/C5c، B5x↔B5c، و لینکِ PTP در C6x جملهٔ صریح ندارند؛ جدا ماندند.
۱۵. اینجینیوس و لیگوویو: ENS500EXT (11n) با ENH1350EXT/ENS620EXT (11ac) جدا ماند؛ ligoPTP 5-N و 6-N هم (هر دو W-Jet V ولی بدونِ جملهٔ سازگاری) جدا ماند. یکی شوند؟
۱۶. جدولِ نرخ‌ها: میموسا فقط حساسیتِ MCS0 را در ۳ تا ۴ پهنای کانال می‌دهد (۱۹ ردیف، `tx_dbm` و `sens_high` خالی)؛ طبقِ «فقط وقتی سازنده جدول داده» آیا حذف شوند؟ ردیف‌های `1x` در AF-5XHD `rate_mbps` ندارند چون دیتاشیت صریحاً به ردیفِ ظرفیت وصلشان نکرده. `tx_dbm` در نرخ‌های بالای AF-5XHD عیناً محدوده است («12-15»).
۱۷. PTP 670: فقط جدولِ باندِ ۵٫۸ گیگ و حالتِ IP آمد (همان دیتاشیت). جدولِ باندهای ۴٫۹/۵٫۱/۵٫۲/۵٫۴/۵٫۹ و حالتِ TDM (از User Guide) هم اضافه شود؟ اختلافشان با ۵٫۸ حدودِ ۰٫۲ تا ۱dB است.
۱۸. کنبوتونگ (`DN-0017`): سازنده «P9A×2» (دووال، دو N) و «P9A» (تک‌پلاریته) را جدا دارد؛ چون سایت «دووال و ۲×N» می‌گوید ردیفِ ×2 مبنا شد. بهتر است یک‌بار کالای فیزیکی تأیید شود.
۱۹. محصولاتی که سازنده در صفحه‌اش «Discontinued/Legacy/EOL» کرده (موضوعِ تجاری، نه عددی): DynaDish 5 ac، OmniTIK 5 ac، NetBox 5، SXTsq 5 ac، SXTsq 5 High Power، wAP ac، ENS500EXT، ENS620EXT، B5C. جایگزینِ رسمیِ بعضی‌ها در `notes` آمده.

## ۷ — روش و سنجشِ دقت

- منبع فقط صفحهٔ محصول/دیتاشیتِ خودِ سازنده بود. برای عددها، HTML یا PDFِ خام را با `curl` دانلود و متنِ استخراج‌شده را خواندم (نه خلاصهٔ مدلِ جستجو)؛ جدولِ Tx/Rxِ میکروتیک در DOM پنهان است و با پارسِ HTMLِ خام درآمد.
- دامنه با مرورِ نامِ هر ۲۲۹ محصول بسته شد، نه فقط با دسته‌ها؛ در این مرور سه رادیوی جاافتاده در دستهٔ ریشهٔ «میکروتیک» پیدا و اضافه شد.
- کار بینِ چند عاملِ تحقیق تقسیم شد (میکروتیک ×۳، یوبیکیوتی و میموسا، کمبیوم و لیگوویو و راکام، اینجینیوس و wAP، آنتن‌ها ×۲)؛ خروجی‌ها قبل از ادغام اعتبارسنجی شد: `tx_max` با بیشینهٔ جدولِ هر رادیو و `sens_low`/`sens_high` با یک ردیفِ همان جدول می‌خوانند (صفر ناهمخوانی).
- اصلاح‌های ادغام (عددِ حدسی یا غیرِ منبع از ستون بیرون رفت و در `notes` ماند): سقفِ EIRPِ ۶۰گیگ (nRAY، LHG 60G، کیت) از `tx_max_dbm`؛ باندِ عددی و پلاریزاسیونِ LHG 5 ax و LHG XL 5 ax (صفحه فقط «5 GHz» دارد)؛ نوعِ آنتنِ AF60-XG و قطرِ AF60-LR (قطرِ کلِ بدنه)؛ `rate_mbps` ردیف‌های 1x در AF-5XHD؛ `beamwidth` مشکوکِ wAP ac؛ بهرهٔ `DN-0012`؛ و کلِ اعدادِ آنتن‌های وایمکس‌نیر.
- سنجشِ مستقلِ من با خودِ دیتاشیتِ رسمی: AirGrid M5 HP، LiteBeam 5AC-23، Mimosa C5x، AF-5XHD (جدولِ حساسیت و ظرفیت)، Cambium Force 4625، و صفحهٔ RAy3-17/24 راکام؛ همه با اعدادِ ثبت‌شده یکی بود. بقیه را فقط عاملِ تحقیق و اعتبارسنجیِ خودکار دیده؛ مدیر چند ردیفِ دیگر را هم مستقل بسنجد.
- فایل‌ها بدونِ BOM و با UTF-8 نوشته شده‌اند. برای خواندنِ دیتاشیتِ تصویریِ فاز (فقط تصویر، بدونِ متن) یکی از عامل‌ها `pymupdf` را با pip نصب کرد؛ هیچ تغییرِ دیگری در دستگاه یا مخزن نبود.
- کاتالوگِ فاز (fara-moj.ir، سایتِ سازنده) فقط تصویر است و متنِ قابلِ جست‌وجو ندارد؛ عددهایِ `DN-0002` و `DN-0003` از روی تصویرِ صفحه‌هایِ ۱۱ و ۱۴ خوانده شد و مدیر باید خودِ صفحه را باز کند. دیتاشیتِ zipِ کنبوتونگ از این دستگاه دانلود نشد؛ اعدادِ `DN-0017` از جدولِ مشخصاتِ صفحهٔ رسمیِ kenbotong.com آمد.
