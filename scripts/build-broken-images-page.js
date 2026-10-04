// می‌سازد: صفحهٔ کاریِ بازیابیِ عکس‌های ازدست‌رفتهٔ مقالات
// ورودی: خروجیِ JSON گروه‌بندی‌شده بر اساس هاب  ·  خروجی: یک فایل HTML مستقل
const fs = require('fs');
const path = require('path');

const src = process.argv[2];
const out = process.argv[3];
const raw = JSON.parse(fs.readFileSync(src, 'utf8'));

const HUB_NAMES = {
  'تکنیک-های-وایرلس': 'تکنیک‌های وایرلس',
  'وایرلس': 'وایرلس',
  'شبکه': 'شبکه',
  'امنیت-شبکه': 'امنیت شبکه',
  'دوره‌های-آموزشی': 'دوره‌های آموزشی',
  'متفرقه': 'متفرقه',
  'آموزش-میکروتیک': 'آموزش میکروتیک',
  'مقالات': 'مقالات',
  'مقایسهٔ-محصولات': 'مقایسهٔ محصولات',
  'VoIP': 'ویپ',
  'کانفیگِ-میکروتیک': 'کانفیگ میکروتیک',
  '(بدون هاب)': 'بدون دسته'
};

const esc = (s) => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;')
  .replace(/>/g, '&gt;').replace(/"/g, '&quot;');

// نامِ فایل‌های تلگرام تاریخ را داخل خودشان دارند: photo_2018-03-27_15-27-02.jpg
const DATE_RE = /(\d{4})-(\d{2})-(\d{2})/;

const hubs = Object.keys(raw).map((key) => {
  const articles = raw[key].map((a) => {
    const paths = a.u.map((u) => decodeURIComponent(u.replace(/^https?:\/\/dadehnama\.ir/, '')));
    const dates = [...new Set(paths.map((p) => { const m = p.match(DATE_RE); return m ? m[0] : null; }).filter(Boolean))].sort();
    return { title: a.t, slug: a.s, published: a.p !== false, count: a.n, paths, dates };
  }).sort((x, y) => y.count - x.count);
  return { key, name: HUB_NAMES[key] || key, articles };
}).sort((a, b) => b.articles.length - a.articles.length);

const totalArticles = hubs.reduce((s, h) => s + h.articles.length, 0);
const totalImages = hubs.reduce((s, h) => s + h.articles.reduce((t, a) => t + a.count, 0), 0);
const withDates = hubs.reduce((s, h) => s + h.articles.filter((a) => a.dates.length).length, 0);
const datedImages = hubs.reduce((s, h) => s + h.articles.reduce((t, a) =>
  t + a.paths.filter((p) => DATE_RE.test(p)).length, 0), 0);

const yearSet = new Set();
hubs.forEach((h) => h.articles.forEach((a) => a.dates.forEach((d) => yearSet.add(d.slice(0, 4)))));
const years = [...yearSet].sort();

const body = hubs.map((h) => `
  <section class="hub" data-hub="${esc(h.key)}">
    <header class="hub-head">
      <h2>${esc(h.name)}</h2>
      <span class="hub-count">${h.articles.length} مقاله</span>
    </header>
    <div class="rows">
      ${h.articles.map((a) => `
      <article class="row${a.dates.length ? ' has-dates' : ''}"
               data-search="${esc((a.title + ' ' + a.slug + ' ' + a.paths.join(' ')).toLowerCase())}"
               data-years="${esc(a.dates.map((d) => d.slice(0, 4)).join(' '))}">
        <div class="row-main">
          <div class="row-text">
            <h3>${esc(a.title)}${a.published ? '' : ' <span class="draft">منتشرنشده</span>'}</h3>
            <div class="row-meta">
              <a class="open" href="http://185.239.3.236/blog/${encodeURI(a.slug)}" target="_blank" rel="noopener">
                باز کردن مقاله
                <svg viewBox="0 0 24 24" width="13" height="13" fill="none" stroke="currentColor" stroke-width="2.3" stroke-linecap="round"><path d="M13 5l-7 7 7 7"/></svg>
              </a>
              ${a.dates.length
                ? `<span class="dates">${a.dates.map((d) => `<span class="date">${d}</span>`).join('')}</span>`
                : '<span class="nodate">بدون تاریخ در نام فایل</span>'}
            </div>
          </div>
          <button class="count" aria-expanded="false">
            <b>${a.count}</b>
            <span>عکس</span>
          </button>
        </div>
        <ul class="files" hidden>
          ${a.paths.map((p) => `<li>${esc(p)}</li>`).join('')}
        </ul>
      </article>`).join('')}
    </div>
  </section>`).join('');

const html = `<title>بازیابی عکس‌های مقالات</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Vazirmatn:wght@400;500;700;900&family=IBM+Plex+Mono:wght@400;500&display=swap">
<style>
  :root {
    --ground: #eef1f6;
    --surface: #ffffff;
    --surface-2: #f6f8fb;
    --line: #dde4ee;
    --line-soft: #e8edf4;
    --ink: #16202e;
    --text: #16202e;
    --text-mid: #4e5f78;
    --text-dim: #8493a8;
    --blue: #1b4f8a;
    --blue-lt: #2b6cb8;
    --amber: #b8650a;
    --amber-bg: #fdf3e6;
    --amber-line: #f0d9b8;
    --shadow: 0 1px 2px rgba(22,32,46,.05), 0 8px 24px rgba(22,32,46,.06);
  }
  @media (prefers-color-scheme: dark) {
    :root:not([data-theme="light"]) {
      --ground: #0d131c;
      --surface: #151d29;
      --surface-2: #1b2432;
      --line: #26313f;
      --line-soft: #202a37;
      --ink: #e8edf4;
      --text: #e2e9f2;
      --text-mid: #9db0c8;
      --text-dim: #6c7f96;
      --blue: #6ba3e0;
      --blue-lt: #8bbaeb;
      --amber: #e9a44e;
      --amber-bg: #2a2013;
      --amber-line: #453321;
      --shadow: 0 1px 2px rgba(0,0,0,.3), 0 8px 24px rgba(0,0,0,.35);
    }
  }
  :root[data-theme="dark"] {
    --ground: #0d131c;
    --surface: #151d29;
    --surface-2: #1b2432;
    --line: #26313f;
    --line-soft: #202a37;
    --ink: #e8edf4;
    --text: #e2e9f2;
    --text-mid: #9db0c8;
    --text-dim: #6c7f96;
    --blue: #6ba3e0;
    --blue-lt: #8bbaeb;
    --amber: #e9a44e;
    --amber-bg: #2a2013;
    --amber-line: #453321;
    --shadow: 0 1px 2px rgba(0,0,0,.3), 0 8px 24px rgba(0,0,0,.35);
  }

  * { box-sizing: border-box; }
  body {
    margin: 0;
    background: var(--ground);
    color: var(--text);
    font-family: 'Vazirmatn', system-ui, sans-serif;
    line-height: 1.65;
    direction: rtl;
  }
  .wrap { max-width: 1080px; margin: 0 auto; padding: 34px 22px 70px; }

  .masthead { margin-bottom: 22px; }
  .eyebrow {
    font-size: .74rem; font-weight: 700; letter-spacing: .09em;
    color: var(--text-dim); text-transform: uppercase; margin-bottom: 9px;
  }
  h1 {
    font-size: clamp(1.6rem, 3.6vw, 2.15rem); font-weight: 900;
    margin: 0 0 10px; letter-spacing: -.5px; text-wrap: balance; color: var(--ink);
  }
  .lede { font-size: 1rem; color: var(--text-mid); margin: 0; max-width: 62ch; text-wrap: pretty; }

  .lead-box {
    margin: 20px 0 24px; background: var(--amber-bg);
    border: 1px solid var(--amber-line); border-radius: 12px; padding: 16px 19px;
  }
  .lead-box h2 {
    font-size: .95rem; font-weight: 900; margin: 0 0 6px; color: var(--amber);
    display: flex; align-items: center; gap: 8px;
  }
  .lead-box p { margin: 0; font-size: .91rem; color: var(--text-mid); text-wrap: pretty; }
  .lead-box code {
    font-family: 'IBM Plex Mono', ui-monospace, monospace; font-size: .85em;
    background: var(--surface); border: 1px solid var(--amber-line);
    border-radius: 5px; padding: 1px 6px; direction: ltr; display: inline-block;
  }

  .stats { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; margin-bottom: 24px; }
  .stat {
    background: var(--surface); border: 1px solid var(--line);
    border-radius: 11px; padding: 14px 16px;
  }
  .stat b {
    display: block; font-size: 1.7rem; font-weight: 900; line-height: 1.15;
    color: var(--ink); font-variant-numeric: tabular-nums;
  }
  .stat span { font-size: .78rem; color: var(--text-dim); }
  .stat.accent b { color: var(--amber); }

  .toolbar {
    position: sticky; top: 0; z-index: 5; background: var(--ground);
    padding: 12px 0 14px; margin-bottom: 6px; border-bottom: 1px solid var(--line);
    display: flex; flex-wrap: wrap; gap: 10px; align-items: center;
  }
  .search {
    flex: 1 1 260px; display: flex; align-items: center; gap: 9px;
    background: var(--surface); border: 1px solid var(--line);
    border-radius: 10px; padding: 0 13px;
  }
  .search svg { color: var(--text-dim); flex-shrink: 0; }
  .search input {
    flex-grow: 1; border: 0; background: transparent; outline: none;
    font-family: inherit; font-size: .93rem; color: var(--text); padding: 11px 0;
  }
  .search input::placeholder { color: var(--text-dim); }
  .chips { display: flex; gap: 6px; flex-wrap: wrap; }
  .chip {
    font-family: inherit; font-size: .82rem; font-weight: 500; cursor: pointer;
    background: var(--surface); color: var(--text-mid);
    border: 1px solid var(--line); border-radius: 100px; padding: 8px 14px;
    transition: all .15s ease;
  }
  .chip:hover { border-color: var(--blue); color: var(--blue); }
  .chip[aria-pressed="true"] {
    background: var(--blue); border-color: var(--blue); color: #fff; font-weight: 700;
  }
  :root[data-theme="dark"] .chip[aria-pressed="true"],
  :root:not([data-theme="light"]) .chip[aria-pressed="true"] { color: #0d131c; }
  .chip:focus-visible, .count:focus-visible, .open:focus-visible, input:focus-visible {
    outline: 2px solid var(--blue); outline-offset: 2px;
  }

  .hub { margin-top: 30px; }
  .hub-head {
    display: flex; align-items: baseline; gap: 11px;
    padding-bottom: 9px; margin-bottom: 12px; border-bottom: 2px solid var(--line);
  }
  .hub-head h2 { font-size: 1.12rem; font-weight: 900; margin: 0; color: var(--ink); }
  .hub-count { font-size: .8rem; color: var(--text-dim); font-variant-numeric: tabular-nums; }

  .rows { display: flex; flex-direction: column; gap: 8px; }
  .row {
    background: var(--surface); border: 1px solid var(--line);
    border-radius: 11px; overflow: hidden; box-shadow: var(--shadow);
  }
  .row-main { display: flex; align-items: center; gap: 14px; padding: 13px 16px; }
  .row-text { flex-grow: 1; min-width: 0; }
  .row h3 {
    font-size: .95rem; font-weight: 700; margin: 0 0 4px;
    color: var(--text); line-height: 1.5;
  }
  .draft {
    font-size: .68rem; font-weight: 700; color: var(--amber);
    background: var(--amber-bg); border: 1px solid var(--amber-line);
    border-radius: 5px; padding: 1px 6px; vertical-align: middle;
  }
  .row-meta { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
  .open {
    display: inline-flex; align-items: center; gap: 4px; font-size: .82rem;
    font-weight: 700; color: var(--blue); text-decoration: none;
  }
  .open:hover { color: var(--blue-lt); text-decoration: underline; }
  .dates { display: flex; gap: 5px; flex-wrap: wrap; }
  .date {
    font-family: 'IBM Plex Mono', ui-monospace, monospace; font-size: .73rem;
    background: var(--surface-2); border: 1px solid var(--line-soft);
    color: var(--text-mid); border-radius: 5px; padding: 1px 7px; direction: ltr;
  }
  .nodate { font-size: .78rem; color: var(--text-dim); }

  .count {
    flex-shrink: 0; display: flex; flex-direction: column; align-items: center;
    justify-content: center; min-width: 62px; padding: 7px 10px; cursor: pointer;
    font-family: inherit; background: var(--surface-2);
    border: 1px solid var(--line); border-radius: 9px; color: var(--text-mid);
    transition: all .15s ease;
  }
  .count:hover { border-color: var(--blue); color: var(--blue); }
  .count b { font-size: 1.15rem; font-weight: 900; line-height: 1.1; font-variant-numeric: tabular-nums; }
  .count span { font-size: .68rem; }
  .count[aria-expanded="true"] { background: var(--blue); border-color: var(--blue); color: #fff; }
  :root[data-theme="dark"] .count[aria-expanded="true"],
  :root:not([data-theme="light"]) .count[aria-expanded="true"] { color: #0d131c; }

  .files {
    margin: 0; padding: 12px 16px 14px; list-style: none;
    border-top: 1px solid var(--line-soft); background: var(--surface-2);
    display: flex; flex-direction: column; gap: 4px;
    max-height: 320px; overflow-y: auto;
  }
  .files li {
    font-family: 'IBM Plex Mono', ui-monospace, monospace; font-size: .77rem;
    color: var(--text-mid); direction: ltr; text-align: left;
    word-break: break-all; line-height: 1.7;
  }

  .empty { display: none; text-align: center; padding: 60px 20px; color: var(--text-dim); }
  .empty.on { display: block; }
  .hub[hidden], .row[hidden] { display: none; }

  @media (max-width: 720px) {
    .stats { grid-template-columns: repeat(2, minmax(0, 1fr)); }
    .toolbar { position: static; }
    .row-main { align-items: flex-start; }
  }
  @media (prefers-reduced-motion: reduce) { * { transition: none !important; } }
</style>

<div class="wrap">
  <header class="masthead">
    <div class="eyebrow">داده نما · بازیابی محتوا</div>
    <h1>عکس‌های گم‌شدهٔ مقالات</h1>
    <p class="lede">
      ${totalArticles} مقاله از ۲۸۱ مقالهٔ واردشده، عکس‌های داخل متنشان بارگذاری نمی‌شود.
      عکس‌ها روی دامنهٔ <code style="font-family:'IBM Plex Mono',monospace;font-size:.85em;direction:ltr">dadehnama.ir</code>
      میزبانی می‌شدند و ایمپورتر آن دامنه را «بیگانه» تشخیص داد و ردشان کرد. آن آدرس‌ها حالا ۴۰۴ می‌دهند
      و در آرشیو اینترنت هم نیستند. کاورهای مقالات سالم‌اند — هر ۲۷۵ فایل موجود است.
    </p>
  </header>

  <div class="lead-box">
    <h2>
      <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M12 16v-5"/><path d="M12 8h.01"/><circle cx="12" cy="12" r="9"/></svg>
      سرنخ بازیابی
    </h2>
    <p>
      نام بیشتر فایل‌ها الگوی خروجی تلگرام است — <code>photo_2018-03-27_15-27-02.jpg</code> —
      یعنی این عکس‌ها از همان کانال شرکت آمده‌اند و <b>تاریخ انتشارشان داخل نام فایل است</b>.
      ${datedImages} فایل از ${totalImages} فایل تاریخ‌دارند؛ برای پیدا کردنشان در کانال، همان تاریخ را جست‌وجو کنید.
    </p>
  </div>

  <div class="stats">
    <div class="stat"><b>${totalArticles}</b><span>مقالهٔ آسیب‌دیده</span></div>
    <div class="stat accent"><b>${totalImages}</b><span>عکس گم‌شده</span></div>
    <div class="stat"><b>${withDates}</b><span>مقاله با تاریخ در نام فایل</span></div>
    <div class="stat"><b>${hubs.length}</b><span>موضوع</span></div>
  </div>

  <div class="toolbar">
    <label class="search">
      <svg viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="2.1" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><path d="M20 20l-3.5-3.5"/></svg>
      <input type="search" id="q" placeholder="جست‌وجو در عنوان مقاله یا نام فایل…" autocomplete="off">
    </label>
    <div class="chips">
      <button class="chip" id="datedOnly" aria-pressed="false">فقط تاریخ‌دارها</button>
      ${years.map((y) => `<button class="chip year" data-year="${y}" aria-pressed="false">${y}</button>`).join('')}
    </div>
  </div>

  <div id="list">${body}</div>
  <div class="empty" id="empty">چیزی با این فیلترها پیدا نشد.</div>
</div>

<script>
  document.querySelectorAll('.count').forEach(function (btn) {
    btn.addEventListener('click', function () {
      var open = btn.getAttribute('aria-expanded') === 'true';
      btn.setAttribute('aria-expanded', String(!open));
      btn.closest('.row').querySelector('.files').hidden = open;
    });
  });

  var q = document.getElementById('q');
  var datedOnly = document.getElementById('datedOnly');
  var yearBtns = Array.prototype.slice.call(document.querySelectorAll('.chip.year'));
  var rows = Array.prototype.slice.call(document.querySelectorAll('.row'));
  var empty = document.getElementById('empty');

  function activeYears() {
    return yearBtns.filter(function (b) { return b.getAttribute('aria-pressed') === 'true'; })
      .map(function (b) { return b.dataset.year; });
  }

  function apply() {
    var term = q.value.trim().toLowerCase();
    var onlyDated = datedOnly.getAttribute('aria-pressed') === 'true';
    var yrs = activeYears();
    var shown = 0;

    rows.forEach(function (row) {
      var ok = true;
      if (term && row.dataset.search.indexOf(term) === -1) ok = false;
      if (ok && onlyDated && !row.classList.contains('has-dates')) ok = false;
      if (ok && yrs.length) {
        var have = (row.dataset.years || '').split(' ');
        ok = yrs.some(function (y) { return have.indexOf(y) !== -1; });
      }
      row.hidden = !ok;
      if (ok) shown++;
    });

    document.querySelectorAll('.hub').forEach(function (hub) {
      var any = Array.prototype.some.call(hub.querySelectorAll('.row'), function (r) { return !r.hidden; });
      hub.hidden = !any;
    });
    empty.classList.toggle('on', shown === 0);
  }

  q.addEventListener('input', apply);
  datedOnly.addEventListener('click', function () {
    datedOnly.setAttribute('aria-pressed', String(datedOnly.getAttribute('aria-pressed') !== 'true'));
    apply();
  });
  yearBtns.forEach(function (b) {
    b.addEventListener('click', function () {
      b.setAttribute('aria-pressed', String(b.getAttribute('aria-pressed') !== 'true'));
      apply();
    });
  });
</script>`;

fs.mkdirSync(path.dirname(out), { recursive: true });
fs.writeFileSync(out, html);
console.log('نوشته شد: ' + out + '  |  ' + totalArticles + ' مقاله، ' + totalImages + ' عکس، ' + withDates + ' با تاریخ');
