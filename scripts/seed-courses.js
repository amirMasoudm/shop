// ─────────────────────────────────────────────────────────────────────────────
// درجِ دوره‌هایِ واقعیِ داده‌نما در کالکشنِ courses
//
// منابع (هر سرفصل عیناً از سندِ شرکت — هیچ سرفصلی از خودم اضافه نشده):
//   الف) «سرفصل ها.pdf» — ۴ صفحه، سندِ رسمیِ سرفصل‌ها
//   ب)  چهار عکسِ سرفصل که مالک فرستاد (نسخه‌ای متفاوت/جدیدتر از همان سند)
//   ج)  دو صفحهٔ کاتالوگِ شرکت: ص۱۹ «دوره جامع شبکه های بیسیم» و ص۱۸ «Mikrotik Academy»
//
// جایی که PDF و عکس اختلاف داشتند، **اجتماعِ هر دو** گرفته شده نه یکی از آن‌ها.
// همهٔ دوره‌ها isActive=false درج می‌شوند چون قیمت/ظرفیت/تاریخ/استاد در هیچ
// منبعی نبود؛ مالک در پنلِ ادمین پر می‌کند و فعال می‌کند.
// ─────────────────────────────────────────────────────────────────────────────

const S = (t, g) => (g ? { group: g, title: t } : { title: t });

// اسلاگ‌هایِ درجِ قبلی که عوض شده‌اند — پاک می‌شوند تا نسخهٔ تکراری نماند
const retiredSlugs = ['کارگاه-تخصصی-شبکه-های-وایرلس'];

// متنِ مشترکِ مزایایِ دوره‌هایِ میکروتیک (کاتالوگ ص۱۸)
const MTC_BENEFITS =
  'تدریس منطبق بر سرفصل‌های دپارتمان آموزش شرکت میکروتیک. ' +
  'برگزاری کلاس‌ها به صورت تئوری و عملی (آزمایشگاه) همراه با حل نمونه‌سؤالات آزمون‌ها و نکات مهم هر بخش. ' +
  'برگزاری آزمون بین‌المللی آنلاین با اعطای مدرک رسمی میکروتیک و یک نسخه لایسنس Level 4 میکروتیک.';

const mtc = (code, en, fa, seoDesc) => ({
  title: 'دورهٔ ' + code + ' — ' + fa,
  slug: 'دوره-' + code.toLowerCase(),
  mode: 'IN_PERSON',
  syllabus: [],   // عمداً خالی: سرفصلِ رسمیِ این مدارک در هیچ‌کدام از منابعِ مالک نبود
  organizerDescription: MTC_BENEFITS,
  seoTitle: 'دورهٔ ' + code + ' میکروتیک (' + en + ') | داده‌نما',
  seoDescription: seoDesc
});

const courses = [

// ═══ ۱) کارگاه تخصصی شبکه‌های وایرلس پیشرفته ══════════════════════════════
// PDF ص۱–۲ (۱۷ ردیف) ∪ عکسِ ۱–۲ (۱۹ ردیف). اختلاف‌ها:
//   • PDF ردیفِ ۱۷ دارد که در عکس نیست: «کارگاه عملی دستگاه Mimosa C5c»
//   • عکس سه ردیف دارد که در PDF نیست: WiFi6E/WiFi7، 802.11ad-ay، لینکِ ۶۰ گیگاهرتز
//   • عنوان: PDF کلمهٔ «پیشرفته» را دارد؛ همان را گرفتیم تا با «دورهٔ جامع» اشتباه نشود
{
  title: 'کلاس و کارگاه تخصصی شبکه‌های وایرلس پیشرفته',
  slug: 'کارگاه-تخصصی-شبکه-های-وایرلس-پیشرفته',
  mode: 'IN_PERSON',
  syllabus: [
    S('تشریح موج، دامنه، فرکانس، باند باریک، باند طیف گسترده، انواع مدولاسیون: BPSK – QPSK – OFDM – QAM'),
    S('روش‌های انتقال اطلاعات DSSS و FHSS'),
    S('روش‌های اتصال شبکه‌ها AD-HOC و Infrastructure'),
    S('معرفی استانداردهای IEEE و توانایی، نقاط ضعف و قوت هر کدام: (802.3af / 802.3at / 802.3bt) و (WiGig – 802.11 a/b/g/ac/ad/ah/af/ax)'),
    S('معرفی dBi، dBm، EIRP و واحدهای اندازه‌گیری و فرمول‌های آن‌ها'),
    S('میزان اتلاف سیگنال در باند ۲ و ۵ گیگاهرتز با احتساب نوع مانع'),
    S('معرفی انواع آنتن‌های موجود و مشخصات فنی آن‌ها (Gain – Beam Width – VSWR – Loss – Polarization – Impedance)'),
    S('معرفی ناحیهٔ Fresnel zone و انواع موانع و نحوهٔ رفع آن‌ها (LOS, nLOS, NLOS)'),
    S('محاسبات بودجهٔ لینک و گین آنتن با توجه به جداول MCS'),
    S('آنالیز لینک‌های رادیویی (PtP شهری – PtP با برد بالای ۵۰ کیلومتر – PtP بر روی دریا – PtMP داخل ساختمان برای محیط‌های دانشگاهی و سالن‌های همایش – PtMP برای کاربران در حال حرکت، لینک‌های بدون دید مستقیم و…)'),
    S('SSID – Beacons – Roaming – Handover – Scanning'),
    S('انواع آنتن و پلاریزاسیون، آزمایشگاه تست آنتن میدان نزدیک – میدان دور و بررسی گزارش‌های آزمایشگاه آنتن'),
    S('کانال‌های فرکانسی 2.4GHz و 5GHz و همپوشانی – انواع Spectrum – اثر آلودگی فرکانسی در شبکه‌ها'),
    S('تکنولوژی‌های MU-MIMO – Wave 2 – Beamforming و روش‌های On Chip / On Antenna / Implicit / Explicit Beamforming'),
    S('استاندارد 802.11AX، تکنولوژی OFDMA، WiFi6، 1024QAM، Subcarrier، Symbol rate، Guard interval، Target Wake Time، BSS Coloring'),
    S('Radiation pattern، Azimuth plane، Elevation Plane، 3dB Beam width، Side lobe، Back lobes، Main lobe و Front to back ratio'),
    S('استانداردهای جدید WiFi6: WiFi6E و WiFi7'),
    S('استاندارد 802.11ad-ay'),
    S('محاسبات لینک‌های ۶۰ گیگاهرتز'),
    S('کارگاه عملی: دستگاه Mimosa C5c', 'کارگاه عملی')
  ],
  seoTitle: 'کلاس و کارگاه تخصصی شبکه‌های وایرلس پیشرفته | داده‌نما',
  seoDescription: 'دورهٔ پیشرفتهٔ شبکه‌های وایرلس داده‌نما: از مدولاسیون و استانداردهای IEEE 802.11 تا محاسبات بودجهٔ لینک، آنالیز لینک‌های PtP و PtMP، WiFi6/6E/7، لینک‌های ۶۰ گیگاهرتز و کارگاه عملی Mimosa C5c.'
},

// ═══ ۲) آنالیز پیشرفتهٔ وایرلس ═══════════════════════════════════════════
// جدولِ دوستونیِ PDF ص۲؛ ترتیب سطر‌به‌سطر راست→چپ (تأییدشده با شماره‌گذاریِ
// جدولِ فارسیِ ص۴ که همان چیدمان را دارد و راستْ فرد است).
// عکس یک خانه را جا انداخته بود: «کارگاه نرم‌افزار Pathloss5» — از PDF اضافه شد.
{
  title: 'دورهٔ آنالیز پیشرفتهٔ وایرلس',
  slug: 'دوره-آنالیز-پیشرفته-وایرلس',
  mode: 'IN_PERSON',
  syllabus: [
    S('محاسبات لینک‌باجت پیشرفتهٔ دستی'),
    S('تخمین ارتفاع مناسب'),
    S('بررسی اثر انحنای زمین در نرم‌افزارهای مختلف آنالیزر پیشرفته'),
    S('Carrier Efficiency and QAM'),
    S('تکنیک دستی تشابه مثلث‌ها برای تشخیص ارتفاع مناسب'),
    S('Phase Shift Key PSK (BPSK, QPSK)'),
    S('ایجاد مانع مصنوعی درگیر در ناحیهٔ فرنل'),
    S('VSWR and Return Loss'),
    S('اثر تداخل فرکانسی در کاهش throughput'),
    S('Cross Polarization'),
    S('بررسی CCQ و SNR'),
    S('Port to Port Isolation'),
    S('اثر بارندگی در لینک‌های وایرلس'),
    S('Radiation Pattern'),
    S('جانمایی APها با نرم‌افزار Ez-WiFi Planner'),
    S('کارگاه نرم‌افزار Link Planner'),
    S('کارگاه نرم‌افزار Pathloss 5'),
    S('محاسبات Pathloss'),
    S('کارگاه نرم‌افزار Ligowave Link Calculator'),
    S('Spectrum')
  ],
  seoTitle: 'دورهٔ آنالیز پیشرفتهٔ لینک‌های وایرلس | داده‌نما',
  seoDescription: 'آنالیز پیشرفتهٔ لینک وایرلس در داده‌نما: لینک‌باجت دستی، اثر انحنای زمین، ناحیهٔ فرنل، تداخل فرکانسی، CCQ و SNR، و کارگاه‌های Pathloss 5، Ez-WiFi Planner، Link Planner و Ligowave Link Calculator.'
},

// ═══ ۳) ارتباطات پرظرفیت مایکروویو ═══════════════════════════════════════
// ⚠️ این دوره در سند **دو فهرست** دارد که مکملِ هم‌اند و هر دو باید بیایند:
//    • PDF ص۴ (بالا): ۱۸ سرفصلِ فارسی — در عکس‌هایی که اول فرستاده شد اصلاً نبود
//    • PDF ص۳ / عکسِ ۳: ۳۲ سرفصلِ انگلیسی
{
  title: 'دورهٔ ارتباطات پرظرفیت مایکروویو',
  slug: 'دوره-ارتباطات-پرظرفیت-مایکروویو',
  mode: 'IN_PERSON',
  syllabus: [
    S('انتخاب باند فرکانسی مایکروویو و تنظیمات کانال RF'),
    S('مدولاسیون ارتباطات مایکروویوی دیجیتال'),
    S('ساختار بسته‌های اطلاعاتی مایکروویو'),
    S('ظرفیت لینک مایکروویو'),
    S('دسته‌بندی تجهیزات مایکروویو و عملکرد داخلی قطعات'),
    S('تنظیم آنتن در فرکانس‌های بالا'),
    S('انواع ایستگاه‌های واسط اکتیو / پسیو'),
    S('ارتباط بین طول مسیر و خط دید واضح در لینک رادیویی'),
    S('فاکتورهای تأثیرگذار بر امواج الکتریکی'),
    S('فاکتورهای مؤثر بر انتشار امواج رادیویی'),
    S('محوشدگی سوسوزن (Scintillation Fading)'),
    S('فناوری Anti-Fading برای مایکروویو دیجیتال'),
    S('شکست اتمسفر (K-Type Fading)'),
    S('محوشدگی چندمسیره (Multipath Fading)'),
    S('ارسال موج و عبور از موانع (Transmission Clearance)'),
    S('تنظیمات محافظتی مایکروویو: SD – FD – HSB – XPIC'),
    S('کارگاه مایکروویو Racom Ray3-24GHz', 'کارگاه عملی'),
    S('کارگاه لینک مایکروویو Ericsson MiniLink', 'کارگاه عملی'),

    S('What is Microwave Communications?'),
    S('Microwave Frequencies'),
    S('Microwave Link Design'),
    S('Loss / Attenuation Calculations'),
    S('Fading and Fade Margin'),
    S('Frequency Planning'),
    S('Microwave Network Application'),
    S('Difficult Areas for Microwave Links'),
    S('Radio Node Hardware'),
    S('Typical Relative Path Lengths with Clear Line of Sight (LOS)'),
    S('Adaptive Coding and Modulation for IP Backhaul (ACM)'),
    S('Free Space & Atmospheric Attenuation'),
    S('Passive Repeater'),
    S('Radio Wave Propagation'),
    S('Microwave Configuration'),
    S('ATPC Operation'),
    S('Hot Standby 1+1 – n+n Configuration'),
    S('Space Diversity'),
    S('Frequency Diversity'),
    S('Rain Attenuation / Calculation'),
    S('Waveguides'),
    S('Microwave Equipment Application'),
    S('Passive Repeater Calculation'),
    S('Microwave Sub Bands'),
    S('Calculating the Microwave Antenna Gain'),
    S('T/R Spacing and Subbands'),
    S('XPIC Configuration'),
    S('OMT and Hybrid Coupler'),
    S('Duplexing (TDD / FDD)'),
    S('Modulation and QAM Table'),
    S('Receive Thresholds and Sensitivity'),
    S('K-type and Duct-type Fading')
  ],
  seoTitle: 'دورهٔ ارتباطات پرظرفیت مایکروویو | داده‌نما',
  seoDescription: 'دورهٔ مایکروویو داده‌نما: انتخاب باند و تنظیم کانال RF، ظرفیت لینک، انواع محوشدگی و Anti-Fading، تنظیمات محافظتی SD/FD/HSB/XPIC و کارگاه عملی روی Racom Ray3 و Ericsson MiniLink.'
},

// ═══ ۴) ارتباطات پرظرفیت مایکروویو پیشرفته ═══════════════════════════════
// PDF ص۴ (پایین) ∪ عکسِ ۴. عکس دو ردیف بیشتر دارد: کارگاه عملیِ اریکسون، و
// «Pathloss 5» در فهرستِ نرم‌افزارهایِ آنالیز که در PDF نیست.
{
  title: 'دورهٔ ارتباطات پرظرفیت مایکروویو پیشرفته',
  slug: 'دوره-ارتباطات-پرظرفیت-مایکروویو-پیشرفته',
  mode: 'IN_PERSON',
  syllabus: [
    S('Advanced Fading'),
    S('Rain Attenuation'),
    S('Advanced Link Budget'),
    S('Advanced Protection'),
    S('Advanced SD+XPIC'),
    S('Advanced FD+SD'),
    S('Checking XPD Value and XPD Calculate'),
    S('XPD Alignment for XPIC Links'),
    S('کارگاه عملی 1+1 / 2+0 روی Ericsson MiniLink', 'کارگاه عملی'),
    S('کارگاه‌های آنالیز لینک مایکروویو با نرم‌افزارهای Pathloss 5، Alcoma Link Calc، Mimosa Link Design، Racom Link Calc و Eband Link Design', 'کارگاه عملی'),
    S('ترکیب محافظت‌های پیشرفته در رادیوهای پرظرفیت هوآوی و اریکسون', 'مباحث ویژه'),
    S('بررسی پیشرفتهٔ لینک در شرایط بارانی', 'مباحث ویژه'),
    S('افزایش ظرفیت چندبرابری همراه با محافظت (ترکیبی از ۸ رادیو و ۱۶ فرکانس در یک لینک رادیویی)', 'مباحث ویژه')
  ],
  seoTitle: 'دورهٔ پیشرفتهٔ ارتباطات پرظرفیت مایکروویو | داده‌نما',
  seoDescription: 'دورهٔ پیشرفتهٔ مایکروویو داده‌نما: Advanced Fading و Link Budget، SD+XPIC و FD+SD، محاسبه و ترازِ XPD، کارگاه عملی Ericsson MiniLink و آنالیز با Pathloss 5، Alcoma، Mimosa، Racom و Eband.'
},

// ═══ ۵) دورهٔ جامع و تخصصی شبکه‌های بی‌سیم (کاتالوگ ص۱۹) ═══════════════════
// دورهٔ پایه — جدا از «کارگاه پیشرفته»ی بالا. سرفصل‌هایِ منحصربه‌فرد دارد
// (نیروگاه خورشیدی/بادی، کانکتور و کابل، مونتاژ رادیو، حالاتِ AP/CPE/Mesh…).
{
  title: 'دورهٔ جامع و تخصصی شبکه‌های بی‌سیم',
  slug: 'دوره-جامع-شبکه-های-بی-سیم',
  mode: 'IN_PERSON',
  syllabus: [
    S('معرفی و تشریح موج، دامنه، فرکانس، باند باریک، باند طیف گسترده، مدولاسیون'),
    S('روش‌های انتقال اطلاعات DSSS و FHSS و OFDM و PSK و QAM و FEC'),
    S('روش‌های اتصال شبکه‌ها AD-HOC و Infrastructure'),
    S('معرفی استانداردهای IEEE، نقاط ضعف و قوت هر کدام'),
    S('تئوری پهنای باند، Carrier Efficiency و MIMO Technology و AC'),
    S('انواع حالات یک رادیو وایرلس (AP – CPE – Mesh – PtP – PtMP – WDS Repeater Bridge) و کاربرد آن‌ها'),
    S('معرفی انواع آنتن‌های موجود و مشخصات مرتبط با آن‌ها (Gain – Beam Width – VSWR – Loss – Cross Polarization – Radiation Pattern – Port to Port Isolation)'),
    S('معرفی انواع کانکتورها و کابل‌های مخابراتی و نحوهٔ خواندن کاتالوگ و میزان Loss آن‌ها'),
    S('معرفی ناحیهٔ Fresnel zone و انواع موانع و نحوهٔ رفع آن‌ها'),
    S('ساخت نیروگاه برقی خورشیدی و بادی برای تأمین برق تجهیزات وایرلس'),
    S('معرفی قطعات اصلی جهت ساخت رادیوهای وایرلس و نحوهٔ مونتاژ آن‌ها'),
    S('آنالیز لینک‌های رادیویی (PtP شهری – PtP با برد بالای ۵۰ کیلومتر – PtP بر روی دریا – PtMP داخل ساختمان برای محیط‌های دانشگاهی و سالن‌های همایش – PtMP برای کاربران در حال حرکت، لینک‌های بدون دید مستقیم و…)')
  ],
  organizerDescription:
    'شرکت ارتباطات شبکه داده‌نما برای اولین بار در ایران به صورت کاملاً حرفه‌ای، علمی و تخصصی در مورد آنتن‌های وایرلس و تجهیزات رادیویی، دورهٔ آموزشی جامع تخصصی شبکه‌های بی‌سیم خود را برگزار می‌نماید. ' +
    'شما پس از برگزاری این دوره قادر خواهید بود انواع شبکه‌های بی‌سیم Point to Point و Point to Multipoint را با کیفیت بالا و حرفه‌ای پیاده‌سازی نمایید. ' +
    'مطالب جمع‌آوری‌شده در این دوره حاصل تجربهٔ متخصصین شرکت داده‌نما در پروژه‌های عملی و مطالعات تخصصی از کتب، سایت‌ها و دوره‌های آموزشی خارجی می‌باشد.',
  faqs: [
    { question: 'آیا اطلاع از تئوری علمی امواج رادیویی و آشنایی با قوانین و فرمول‌های مرتبط با شبکه‌های بی‌سیم، شما را در انجام پروژه‌های جدید یاری نخواهد نمود؟',
      answer: 'این دوره دقیقاً برای همین ساخته شده است: مبانی نظری امواج، قوانین و فرمول‌های حاکم بر لینک رادیویی، و کاربردشان در طراحی پروژه‌های واقعی.' },
    { question: 'آیا به دنبال راه‌حلی برای تخمین انحنای زمین و موانع مجاز طبیعی برای دستیابی به حداکثر پهنای باند مجاز در فواصل طولانی هستید؟',
      answer: 'محاسبهٔ ناحیهٔ فرنل، انحنای زمین و انواع موانع و نحوهٔ رفع آن‌ها از سرفصل‌های همین دوره است.' },
    { question: 'آیا از فیزیک آنتن‌های وایرلس و رادیوها و فرمول‌های مؤثر در یک ارتباط رادیویی اطلاع کافی دارید؟',
      answer: 'مشخصات فنی آنتن — Gain، Beam Width، VSWR، Loss، Cross Polarization، Radiation Pattern و Port to Port Isolation — به‌صورت تفصیلی در دوره تدریس می‌شود.' },
    { question: 'عیوب مکرر و پشتیبانی مداوم و مشکلات شما در عدم پایداری لینک‌های وایرلس، نیاز شما را به یک دورهٔ تخصصی در این زمینه افزون نمی‌نماید؟',
      answer: 'ریشهٔ بیشتر ناپایداری‌های لینک، خطای طراحی و جانمایی است؛ این دوره روی همان نقاط تمرکز دارد.' }
  ],
  seoTitle: 'دورهٔ جامع و تخصصی شبکه‌های بی‌سیم | داده‌نما',
  seoDescription: 'دورهٔ جامع شبکه‌های بی‌سیم داده‌نما: مبانی امواج و مدولاسیون، استانداردهای IEEE، حالات رادیو (AP/CPE/Mesh/PtP/PtMP)، مشخصات آنتن، کانکتور و کابل، ناحیهٔ فرنل، برق خورشیدی و بادی، و آنالیز لینک رادیویی.'
},

// ═══ ۶ تا ۱۱) آکادمی میکروتیک (کاتالوگ ص۱۸) ═══════════════════════════════
// سرفصلِ رسمیِ این شش مدرک در هیچ‌کدام از سندهایِ مالک نبود — عمداً خالی
// گذاشته شد تا از خودم نسازم. باید از سرفصلِ رسمیِ mikrotik.com وارد شود.
mtc('MTCNA', 'MikroTik Certified Network Associate', 'مهندسی عمومی میکروتیک',
    'دورهٔ MTCNA داده‌نما با مدرک رسمی بین‌المللی میکروتیک — کلاس تئوری و عملی (آزمایشگاه) به همراه آزمون آنلاین و لایسنس Level 4.'),
mtc('MTCWE', 'MikroTik Certified Wireless Engineer', 'مهندسی تخصصی وایرلس',
    'دورهٔ MTCWE داده‌نما — مهندسی تخصصی وایرلس میکروتیک با مدرک رسمی بین‌المللی، کلاس عملی در آزمایشگاه و آزمون آنلاین.'),
mtc('MTCTCE', 'MikroTik Certified Traffic Control Engineer', 'مهندسی کنترل پهنای باند',
    'دورهٔ MTCTCE داده‌نما — کنترل ترافیک و پهنای باند در میکروتیک، با مدرک رسمی بین‌المللی و کلاس تئوری و عملی.'),
mtc('MTCUME', 'MikroTik Certified User Management Engineer', 'مهندسی مدیریت کاربران',
    'دورهٔ MTCUME داده‌نما — مدیریت کاربران در میکروتیک با مدرک رسمی بین‌المللی، کلاس عملی و آزمون آنلاین.'),
mtc('MTCRE', 'MikroTik Certified Routing Engineer', 'مهندسی مسیریابی',
    'دورهٔ MTCRE داده‌نما — مهندسی مسیریابی میکروتیک با مدرک رسمی بین‌المللی، کلاس تئوری و عملی در آزمایشگاه.'),
mtc('MTCINE', 'MikroTik Certified Inter-networking Engineer', 'مهندسی پیشرفتهٔ شبکه میکروتیک',
    'دورهٔ MTCINE داده‌نما — بالاترین سطح مدارک میکروتیک، مهندسی پیشرفتهٔ اینترنتورکینگ با مدرک رسمی بین‌المللی.')

];

// ── اجرا ──────────────────────────────────────────────────────────────────
let del = 0;
for (const s of retiredSlugs) del += db.courses.deleteMany({ slug: s }).deletedCount;

let ins = 0, upd = 0;
for (const c of courses) {
  const doc = {
    title: c.title,
    mode: c.mode,
    syllabus: c.syllabus,
    capacity: 0,
    enrolledCount: 0,
    images: [],
    price: Decimal128('0'),
    isActive: false,          // تا وقتی مالک قیمت/ظرفیت/تاریخ را وارد نکرده
    faqs: c.faqs || [],
    slug: c.slug,
    seoTitle: c.seoTitle,
    seoDescription: c.seoDescription,
    _class: 'org.example.shop1.model.entity.Course'
  };
  if (c.organizerDescription) doc.organizerDescription = c.organizerDescription;

  const existing = db.courses.findOne({ slug: c.slug });
  if (existing) {
    // enrolledCount هرگز بازنویسی نشود — تنها OrderService زیادش می‌کند
    doc.enrolledCount = existing.enrolledCount || 0;
    db.courses.replaceOne({ _id: existing._id }, doc);
    upd++;
  } else {
    db.courses.insertOne(doc);
    ins++;
  }
}

print('حذفِ اسلاگِ بازنشسته: ' + del + '   درج‌شده: ' + ins + '   به‌روزشده: ' + upd);
print('─────────────────────────────────────────');
db.courses.find({}, { title: 1, slug: 1, isActive: 1, syllabus: 1 }).forEach(d =>
  print('  • ' + d.title + '  [' + (d.syllabus ? d.syllabus.length : 0) + ' سرفصل]  فعال=' + d.isActive));
