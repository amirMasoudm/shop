// درجِ چهار دورهٔ واقعیِ داده‌نما — سرفصل‌ها عیناً از تصاویرِ رسمیِ شرکت
// شکلِ سند دقیقاً همانی است که Spring Data می‌نویسد: آیتم‌های تعبیه‌شده بدونِ _class
const S = (t, g) => (g ? { group: g, title: t } : { title: t });

const courses = [

// ── ۱) کلاس و کارگاه تخصصی شبکه‌های وایرلس ─────────────────────────
{
  title: 'کلاس و کارگاه تخصصی شبکه‌های وایرلس',
  slug: 'کارگاه-تخصصی-شبکه-های-وایرلس',
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
    S('محاسبات لینک‌های ۶۰ گیگاهرتز')
  ],
  seoTitle: 'کلاس و کارگاه تخصصی شبکه‌های وایرلس | داده‌نما',
  seoDescription: 'دورهٔ تخصصی شبکه‌های وایرلس داده‌نما: از مدولاسیون و استانداردهای IEEE 802.11 تا محاسبات بودجهٔ لینک، آنالیز لینک‌های PtP و PtMP، WiFi6/6E/7 و لینک‌های ۶۰ گیگاهرتز.'
},

// ── ۲) آنالیز پیشرفتهٔ وایرلس ─────────────────────────────────────
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
    S('محاسبات Pathloss'),
    S('کارگاه نرم‌افزار Ligowave Link Calculator'),
    S('Spectrum')
  ],
  seoTitle: 'دورهٔ آنالیز پیشرفتهٔ لینک‌های وایرلس | داده‌نما',
  seoDescription: 'آنالیز پیشرفتهٔ لینک وایرلس در داده‌نما: لینک‌باجت دستی، اثر انحنای زمین، ناحیهٔ فرنل، تداخل فرکانسی، CCQ و SNR، و کارگاه‌های Ez-WiFi Planner، Link Planner و Ligowave Link Calculator.'
},

// ── ۳) ارتباطات پرظرفیت مایکروویو ─────────────────────────────────
{
  title: 'دورهٔ ارتباطات پرظرفیت مایکروویو',
  slug: 'دوره-ارتباطات-پرظرفیت-مایکروویو',
  mode: 'IN_PERSON',
  syllabus: [
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
  seoDescription: 'دورهٔ مایکروویو داده‌نما در ۳۲ سرفصل: طراحی لینک، محاسبات اتلاف، Fade Margin، فرکانس‌پلنینگ، ACM، Space/Frequency Diversity، XPIC، وِیوگاید و جداول مدولاسیون.'
},

// ── ۴) ارتباطات پرظرفیت مایکروویو پیشرفته ─────────────────────────
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
    S('کارگاه عملی 1+1 / 2+0 روی Ericsson MiniLink'),
    S('کارگاه‌های آنالیز لینک مایکروویو با نرم‌افزارهای Pathloss 5، Alcoma Link Calc، Mimosa Link Design، Racom Link Calc و Eband Link Design'),
    S('ترکیب محافظت‌های پیشرفته در رادیوهای پرظرفیت هوآوی و اریکسون', 'مباحث ویژه'),
    S('بررسی پیشرفتهٔ لینک در شرایط بارانی', 'مباحث ویژه'),
    S('افزایش ظرفیت چندبرابری همراه با محافظت (ترکیبی از ۸ رادیو و ۱۶ فرکانس در یک لینک رادیویی)', 'مباحث ویژه')
  ],
  seoTitle: 'دورهٔ پیشرفتهٔ ارتباطات پرظرفیت مایکروویو | داده‌نما',
  seoDescription: 'دورهٔ پیشرفتهٔ مایکروویو داده‌نما: Advanced Fading و Link Budget، SD+XPIC و FD+SD، محاسبه و ترازِ XPD، کارگاه عملی Ericsson MiniLink و آنالیز با Pathloss 5، Alcoma، Mimosa، Racom و Eband.'
}

];

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
    faqs: [],
    slug: c.slug,
    seoTitle: c.seoTitle,
    seoDescription: c.seoDescription,
    _class: 'org.example.shop1.model.entity.Course'
  };
  const existing = db.courses.findOne({ slug: c.slug });
  if (existing) { db.courses.replaceOne({ _id: existing._id }, doc); upd++; }
  else { db.courses.insertOne(doc); ins++; }
}
print('درج‌شده: ' + ins + '   به‌روزشده: ' + upd);
print('---');
db.courses.find({}, { title: 1, slug: 1, isActive: 1, syllabus: 1 }).forEach(d =>
  print('  • ' + d.title + '  [' + (d.syllabus ? d.syllabus.length : 0) + ' سرفصل]  فعال=' + d.isActive + '  /shop/course/' + d.slug));
