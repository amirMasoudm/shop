/**
 * مدیریتِ مقالاتِ بلاگ — مشترکِ پنلِ ادمین و پنلِ فروشِ حضوری.
 *
 * 🔴 <b>چرا ماژولِ مشترک و نه کپیِ دوم:</b> این کد منطقِ سئو دارد (یکتاییِ نامک،
 * پیشنهادِ ریدایرکتِ ۳۰۱ وقتی نامک عوض می‌شود، خوشهٔ محتوایی). دو نسخهٔ جداگانه
 * یعنی روزی یکی از آن‌ها ریدایرکت را نمی‌پرسد و یک نشانیِ ایندکس‌شده بی‌صدا ۴۰۴
 * می‌شود. پس یک کد، دو میزبان.
 *
 * <b>وابستگی‌هایِ میزبان:</b> axios، Swal، escapeHTML، Quill، و موتورِ مودالِ
 * /js/admin-modal-utils.js (openModal/closeModal/closeModalSafe/resetForm/FORM_MODALS).
 *
 * 🔴 <b>حذف:</b> دکمه‌اش فقط با canDelete ساخته می‌شود، ولی مرزِ واقعی سمتِ سرور
 * است (DELETE /api/v1/articles/admin/** فقط ADMIN). این فقط برایِ این است که
 * دکمه‌ای نشان داده نشود که کلیکش ۴۰۳ می‌گیرد.
 */
(function () {
    const API = '/api';

    let articles = [];
    let host = null;
    let quill = null;
    let canDelete = false;
    let modalMounted = false;

    // ==========================================================
    // mount
    // ==========================================================
    function mount(hostEl, opts) {
        host = hostEl;
        canDelete = !!(opts && opts.canDelete);
        if (!host.dataset.articlesMounted) {
            host.innerHTML = LIST_SHELL;
            host.dataset.articlesMounted = '1';
        }
        mountModal();
        return fetchArticles();
    }

    /**
     * مودال بیرونِ host می‌نشیند (روی body).
     * ⚠️ اگر داخلِ host بماند، با hidden-sectionِ تب، خودِ مودال هم پنهان می‌شود و
     * «ذخیره» بی‌اثر به‌نظر می‌رسد.
     */
    function mountModal() {
        if (modalMounted || document.getElementById('articleModal')) {
            modalMounted = true;
            registerModal();
            return;
        }
        const wrap = document.createElement('div');
        wrap.innerHTML = MODAL_SHELL;
        document.body.appendChild(wrap.firstElementChild);
        modalMounted = true;
        registerModal();
    }

    function registerModal() {
        if (!window.FORM_MODALS) window.FORM_MODALS = new Set();
        window.FORM_MODALS.add('articleModal');
    }

    // ==========================================================
    // ویرایشگر
    // ==========================================================
    function initEditor() {
        if (quill) return;
        quill = new Quill('#a-editor', {
            theme: 'snow',
            placeholder: 'متن مقاله را اینجا بنویسید…',
            modules: {
                toolbar: [
                    [{header: [2, 3, false]}],
                    ['bold', 'italic', 'underline'],
                    [{list: 'ordered'}, {list: 'bullet'}],
                    [{direction: 'rtl'}, {align: []}],
                    ['link', 'image', 'code-block'],
                    ['clean']
                ],
                // تغییر سایز عکس با درگ گوشه‌ها + نمایش ابعاد + دکمه‌های چینش
                imageResize: {modules: ['Resize', 'DisplaySize', 'Toolbar']}
            }
        });
        // جهت پیش‌فرض فارسی: راست‌چین
        quill.format('direction', 'rtl');
        quill.format('align', 'right');
        // آپلود تصویر داخل متن از طریق سرور خودمان (نه base64)
        quill.getModule('toolbar').addHandler('image', () => {
            const input = document.createElement('input');
            input.type = 'file';
            input.accept = 'image/*';
            input.onchange = async () => {
                if (!input.files[0]) return;
                const fd = new FormData();
                fd.append('file', input.files[0]);
                try {
                    const res = await axios.post(`${API}/v1/files/upload`, fd,
                        {headers: {'Content-Type': 'multipart/form-data'}});
                    const range = quill.getSelection(true);
                    quill.insertEmbed(range.index, 'image', res.data);
                } catch (e) {
                    Swal.fire('خطا', 'آپلود تصویر ناموفق بود', 'error');
                }
            };
            input.click();
        });
    }

    // ==========================================================
    // داده
    // ==========================================================
    async function fetchArticles() {
        try {
            const res = await axios.get(`${API}/v1/articles/admin/all`);
            articles = res.data || [];
        } catch (e) {
            Swal.fire('خطا', 'دریافت مقالات ناموفق بود', 'error');
            return articles;
        }
        // تبِ بنرها هم همین داده را می‌خواهد بی‌آنکه جدولِ مقاله‌ها mount شده باشد.
        if (host && host.dataset.articlesMounted) {
            refreshHubLists();
            filterArticles();
        }
        return articles;
    }

    function getArticles() {
        return articles;
    }

    // پیشنهادهای خوشه (هم برایِ دیتالیستِ فرمِ کامل، هم برایِ سلکتِ فیلترِ جدول) —
    // بعدِ هر ذخیره هم دوباره صدا زده می‌شود تا هابِ تازه‌تایپ‌شده فوراً در لیست بیاید.
    function refreshHubLists() {
        const hubs = [...new Set(articles.map(a => a.hub).filter(Boolean))].sort();
        const list = document.getElementById('hub-list');
        if (list) list.innerHTML = hubs.map(h => `<option value="${escapeHTML(h)}">`).join('');
        const sel = host.querySelector('#art-hub-filter');
        if (!sel) return;
        const current = sel.value;
        sel.innerHTML = '<option value="">همهٔ هاب‌ها</option><option value="__NONE__">⚠️ بدون هاب</option>'
            + hubs.map(h => `<option value="${escapeHTML(h)}">${escapeHTML(h)}</option>`).join('');
        sel.value = hubs.includes(current) || current === '__NONE__' ? current : '';
    }

    // فیلترِ سمتِ کلاینت (کلِ فهرست از قبل در حافظه است، نیازی به کالِ تازه نیست)
    function filterArticles() {
        const q = (host.querySelector('#art-search').value || '').trim().toLowerCase();
        const hubFilter = host.querySelector('#art-hub-filter').value;
        const statusFilter = host.querySelector('#art-status-filter').value;

        let rows = articles;
        if (q) rows = rows.filter(a => (a.title || '').toLowerCase().includes(q)
            || (a.slug || '').toLowerCase().includes(q));
        if (hubFilter === '__NONE__') rows = rows.filter(a => !a.hub);
        else if (hubFilter) rows = rows.filter(a => a.hub === hubFilter);
        if (statusFilter === 'published') rows = rows.filter(a => a.published);
        else if (statusFilter === 'draft') rows = rows.filter(a => !a.published);

        host.querySelector('#art-filter-count').innerText = `${rows.length} از ${articles.length}`;
        renderArticles(rows);
    }

    function renderArticles(rows) {
        rows = (rows || articles).slice().sort((a, b) => (b.updatedAt || '').localeCompare(a.updatedAt || ''));
        host.querySelector('#articles-tbody').innerHTML = rows.length ? rows.map(a => `
            <tr class="border-t hover:bg-gray-50">
                <td class="p-3 md:p-4 font-medium text-gray-800">${escapeHTML(a.title || '—')}</td>
                <td class="p-3 md:p-4 hidden md:table-cell text-xs text-gray-500" dir="ltr">${escapeHTML(a.slug || '')}</td>
                <td class="p-3 md:p-4">
                    <div class="flex items-center gap-1.5">
                        <input type="text" list="hub-list" value="${escapeHTML(a.hub || '')}" placeholder="بدون هاب"
                               id="hub-input-${a.id}"
                               class="w-28 md:w-36 p-1.5 text-xs border rounded-lg outline-none ${a.hub ? '' : 'border-amber-400 bg-amber-50 placeholder-amber-500'}"
                               onkeydown="if(event.key==='Enter'){event.preventDefault(); ArticlesAdminTab.quickSetHub('${a.id}');}">
                        <button onclick="ArticlesAdminTab.quickSetHub('${a.id}')" title="ذخیرهٔ هاب"
                                class="text-xs bg-indigo-50 text-indigo-600 px-2 py-1.5 rounded hover:bg-indigo-100">✓</button>
                        ${a.hub ? '' : '<span class="text-[9px] md:text-[10px] font-bold text-amber-600 bg-amber-100 px-1.5 py-0.5 rounded whitespace-nowrap">بدون هاب</span>'}
                    </div>
                </td>
                <td class="p-3 md:p-4">
                    <span class="px-2 py-1 rounded text-[10px] md:text-xs font-bold ${a.published ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700'}">
                        ${a.published ? 'منتشرشده' : 'پیش‌نویس'}
                    </span>
                </td>
                <td class="p-3 md:p-4 hidden md:table-cell text-xs text-gray-400">${(a.updatedAt || '').substring(0, 10)}</td>
                <td class="p-3 md:p-4">
                    <div class="flex gap-1">
                        ${a.published ? `<a href="/blog/${encodeURIComponent(a.slug || a.id)}" target="_blank" class="text-xs bg-gray-50 text-gray-600 px-2 py-1 rounded hover:bg-gray-100">👁</a>` : ''}
                        <button onclick='ArticlesAdminTab.edit("${a.id}")' class="text-xs bg-indigo-50 text-indigo-600 px-2 py-1 rounded hover:bg-indigo-100">✏️</button>
                        ${canDelete ? `<button onclick='ArticlesAdminTab.remove("${a.id}")' class="text-xs bg-red-50 text-red-600 px-2 py-1 rounded hover:bg-red-100">✕</button>` : ''}
                    </div>
                </td>
            </tr>
        `).join('') : '<tr><td colspan="6" class="p-8 text-center text-gray-400">هیچ مقاله‌ای با این فیلتر پیدا نشد.</td></tr>';
    }

    // تغییرِ هابِ یک مقاله بدونِ بازکردنِ ویرایشگرِ کامل — عمداً از همان
    // POST /v1/articles/admin رد می‌شود (سرور خودش hubSlug را می‌سازد، دقیقاً همان
    // منطقِ ذخیره‌ی کاملِ مقاله) و کلِ آبجکتِ موجود را دوباره می‌فرستد تا هیچ فیلدی
    // (متن، کاور، سئو) گم نشود — همان باگی که یک‌بار در ذخیره‌ی محصول افتاده بود.
    async function quickSetHub(id) {
        const article = articles.find(x => x.id === id);
        if (!article) return;
        const input = document.getElementById(`hub-input-${id}`);
        const newHub = input.value.trim();
        if (newHub === (article.hub || '')) return; // چیزی عوض نشده

        const payload = Object.assign({}, article, {hub: newHub || null});
        try {
            const res = await axios.post(`${API}/v1/articles/admin`, payload);
            const idx = articles.findIndex(x => x.id === id);
            if (idx !== -1) articles[idx] = res.data;
            refreshHubLists();
            filterArticles();
            Swal.fire({icon: 'success', title: 'هاب ذخیره شد', timer: 1000, showConfirmButton: false});
        } catch (e) {
            Swal.fire('خطا', (e.response && e.response.data) || 'ذخیره‌ی هاب ناموفق بود', 'error');
        }
    }

    // ==========================================================
    // فرم
    // ==========================================================
    function updateSeoPreview() {
        const t = document.getElementById('a-seo-title').value;
        const d = document.getElementById('a-seo-desc').value;
        const tc = document.getElementById('a-seo-title-count');
        tc.innerText = t.length + '/60';
        tc.style.color = t.length > 60 ? '#dc2626' : '#9ca3af';
        const dc = document.getElementById('a-seo-desc-count');
        dc.innerText = d.length + '/160';
        dc.style.color = d.length > 160 ? '#dc2626' : '#9ca3af';
    }

    function openNew() {
        initEditor();
        resetForm('a');
        document.getElementById('a-published').value = 'false';
        document.getElementById('a-cover-preview').classList.add('hidden');
        quill.setContents([]);
        updateSeoPreview();
        document.getElementById('article-modal-title').innerText = 'مقاله جدید';
        openModal('articleModal');
    }

    function edit(id) {
        const a = articles.find(x => x.id === id);
        if (!a) return;
        initEditor();
        document.getElementById('a-id').value = a.id;
        document.getElementById('a-title').value = a.title || '';
        document.getElementById('a-slug').value = a.slug || '';
        document.getElementById('a-excerpt').value = a.excerpt || '';
        document.getElementById('a-hub').value = a.hub || '';
        document.getElementById('a-published').value = String(!!a.published);
        document.getElementById('a-seo-title').value = a.seoTitle || '';
        document.getElementById('a-seo-desc').value = a.seoDescription || '';
        document.getElementById('a-cover').value = a.coverImage || '';
        const prev = document.getElementById('a-cover-preview');
        if (a.coverImage) { prev.src = a.coverImage; prev.classList.remove('hidden'); }
        else prev.classList.add('hidden');
        quill.root.innerHTML = a.contentHtml || '';
        updateSeoPreview();
        document.getElementById('article-modal-title').innerText = 'ویرایش مقاله';
        openModal('articleModal');
    }

    async function uploadCover(input) {
        if (!input.files || !input.files[0]) return;
        const fd = new FormData();
        fd.append('file', input.files[0]);
        try {
            const res = await axios.post(`${API}/v1/files/upload`, fd,
                {headers: {'Content-Type': 'multipart/form-data'}});
            document.getElementById('a-cover').value = res.data;
            const prev = document.getElementById('a-cover-preview');
            prev.src = res.data;
            prev.classList.remove('hidden');
        } catch (e) {
            Swal.fire('خطا', 'آپلود تصویر شاخص ناموفق بود', 'error');
        }
    }

    /**
     * آدرسِ مقاله عوض شد — همان کادرِ محصول، با متنِ یکسان.
     *
     * چرا تأیید و نه خودکار: مقالهٔ منتشرنشده اگر اسلاگش اصلاح شود، ریدایرکت فقط
     * زباله می‌سازد. تصمیم با کسی است که می‌داند صفحه ایندکس شده یا نه.
     */
    async function offerSlugRedirect(oldSlug, newSlug, title) {
        const r = await Swal.fire({
            title: 'آدرسِ مقاله عوض شد',
            html: `<div style="text-align:right; line-height:2; font-size:.9rem;">
                     مقاله: <b>${escapeHTML(title || '')}</b><br>
                     آدرسِ قبلی: <code style="direction:ltr; display:inline-block; max-width:100%; word-break:break-all;">/blog/${escapeHTML(oldSlug)}</code><br>
                     آدرسِ جدید: <code style="direction:ltr; display:inline-block; max-width:100%; word-break:break-all;">/blog/${escapeHTML(newSlug)}</code><br><br>
                     آدرسِ قبلی از الان <b>۴۰۴</b> می‌دهد. ریدایرکتِ ۳۰۱ به آدرسِ جدید ثبت شود؟
                     <span style="display:block; color:#6b7280; font-size:.82rem;">
                       اگر این مقاله قبلاً منتشر/ایندکس شده بود، «بله» را بزنید.
                     </span>
                   </div>`,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: 'بله، ریدایرکت ثبت کن',
            cancelButtonText: 'نه، لازم نیست',
            confirmButtonColor: '#1b4f8a'
        });
        if (!r.isConfirmed) return;

        try {
            // ⚠️ اسلاگ دیکدشده فرستاده می‌شود، نه درصد-کدشده: سرور خودش fromPath را
            // نرمال می‌کند و مقصد را برای پیداکردنِ مقاله خام می‌خواند.
            const res = await axios.post(`${API}/v1/legacy-redirects`, {
                fromPath: `/blog/${oldSlug}`,
                toPath: `/blog/${newSlug}`,
                note: `تغییرِ اسلاگِ «${title || ''}» از پنل`
            });
            const flattened = (res.data && res.data.flattened) || 0;
            Swal.fire({
                icon: 'success',
                title: 'ریدایرکت ثبت شد',
                text: flattened > 0
                    ? `${flattened} ریدایرکتِ قدیمی هم مستقیم به آدرسِ تازه وصل شد.`
                    : '',
                timer: flattened > 0 ? 2600 : 1400,
                showConfirmButton: false
            });
        } catch (e) {
            const msg = (e.response && e.response.data && (e.response.data.message || e.response.data))
                || 'ثبتِ ریدایرکت ناموفق بود';
            // ⚠️ مقاله از قبل ذخیره شده؛ این فقط هشدار است نه شکستِ ذخیره. نبودنِ
            // دسترسی به /api/v1/legacy-redirects (۴۰۳، یعنی هر نقشی جز ادمین) هم
            // دقیقاً همین‌جا و به همین شکلِ بی‌خطر فرود می‌آید — مقاله سالم ذخیره مانده.
            Swal.fire('ریدایرکت ثبت نشد', String(msg) + ' — آدرسِ قبلی ۴۰۴ می‌دهد.', 'error');
        }
    }

    function save() {
        const title = document.getElementById('a-title').value.trim();
        if (!title) { Swal.fire('خطا', 'عنوان مقاله را وارد کنید', 'error'); return; }

        // اسلاگِ پیش از ذخیره. فقط روی ویرایش معنی دارد — مقالهٔ تازه اسلاگِ قبلی ندارد.
        const editingId = document.getElementById('a-id').value || null;
        const oldSlug = editingId ? ((articles.find(x => x.id === editingId) || {}).slug || null) : null;

        const data = {
            id: editingId,
            title: title,
            slug: document.getElementById('a-slug').value.trim() || null,
            excerpt: document.getElementById('a-excerpt').value.trim(),
            hub: document.getElementById('a-hub').value.trim() || null,
            contentHtml: quill.root.innerHTML,
            coverImage: document.getElementById('a-cover').value || null,
            seoTitle: document.getElementById('a-seo-title').value.trim() || null,
            seoDescription: document.getElementById('a-seo-desc').value.trim() || null,
            published: document.getElementById('a-published').value === 'true'
        };

        axios.post(`${API}/v1/articles/admin`, data)
            .then(async res => {
                closeModal('articleModal');
                await fetchArticles();

                // 🔴 اسلاگِ تازه از پاسخِ سرور خوانده می‌شود، نه از فیلدِ فرم:
                // ArticleService.save آن را از generateUniqueSlug رد می‌کند و ممکن
                // است چیزِ دیگری برگرداند (یکتاسازی یا slugify). با خواندن از فرم،
                // ریدایرکت به آدرسی می‌رفت که اصلاً وجود ندارد.
                const newSlug = res && res.data ? res.data.slug : null;

                if (oldSlug && newSlug && oldSlug !== newSlug) {
                    await offerSlugRedirect(oldSlug, newSlug, title);
                } else {
                    Swal.fire({icon: 'success', title: 'ذخیره شد', timer: 1200, showConfirmButton: false});
                }
            })
            .catch(err => Swal.fire('خطا', (err.response && err.response.data) || 'ذخیره مقاله ناموفق بود', 'error'));
    }

    function remove(id) {
        Swal.fire({
            title: 'حذف مقاله؟', text: 'این عمل قابل بازگشت نیست', icon: 'warning',
            showCancelButton: true, confirmButtonText: 'حذف'
        }).then(r => {
            if (!r.isConfirmed) return;
            axios.delete(`${API}/v1/articles/admin/${id}`)
                .then(() => fetchArticles())
                .catch(err => Swal.fire('خطا', (err.response && err.response.data) || 'حذف ناموفق بود', 'error'));
        });
    }

    // ==========================================================
    // مارک‌آپ
    // ==========================================================
    const LIST_SHELL = `
        <div class="flex flex-col md:flex-row justify-between md:items-center mb-6 gap-4">
            <h2 class="text-2xl font-bold text-gray-800">مقالات بلاگ</h2>
            <div class="flex gap-2">
                <a href="/blog" target="_blank"
                   class="w-full md:w-auto bg-gray-100 text-gray-700 px-4 py-2 rounded-lg border hover:bg-gray-200 flex items-center justify-center gap-2 text-sm">
                    👁 مشاهده بلاگ
                </a>
                <button onclick="ArticlesAdminTab.openNew()"
                        class="w-full md:w-auto bg-green-600 text-white px-4 py-2 rounded-lg shadow hover:bg-green-700 flex items-center justify-center gap-2">
                    <span>📝</span> مقاله جدید
                </button>
            </div>
        </div>
        <!-- جست‌وجو/فیلترِ جدول — بدونِ این ابزار، پیداکردنِ یک مقاله بینِ صدها ردیف طاقت‌فرساست. -->
        <div class="bg-white p-3 md:p-4 rounded-xl shadow-sm border mb-4 flex flex-col md:flex-row gap-3">
            <input type="text" id="art-search" placeholder="جست‌وجو در عنوان/اسلاگ…" oninput="ArticlesAdminTab.filter()"
                   class="flex-1 p-2.5 border rounded-lg outline-none text-sm">
            <select id="art-hub-filter" onchange="ArticlesAdminTab.filter()" class="p-2.5 border rounded-lg bg-white outline-none text-sm md:w-56">
                <option value="">همهٔ هاب‌ها</option>
                <option value="__NONE__">⚠️ بدون هاب</option>
            </select>
            <select id="art-status-filter" onchange="ArticlesAdminTab.filter()" class="p-2.5 border rounded-lg bg-white outline-none text-sm md:w-40">
                <option value="">همهٔ وضعیت‌ها</option>
                <option value="published">منتشرشده</option>
                <option value="draft">پیش‌نویس</option>
            </select>
            <div class="text-xs text-gray-400 flex items-center whitespace-nowrap" id="art-filter-count"></div>
        </div>
        <div class="bg-white rounded-xl shadow-sm border overflow-x-auto">
            <table class="w-full text-right text-sm">
                <thead class="bg-gray-50 text-gray-500">
                <tr>
                    <th class="p-3 md:p-4">عنوان</th>
                    <th class="p-3 md:p-4 hidden md:table-cell">اسلاگ</th>
                    <th class="p-3 md:p-4">هاب</th>
                    <th class="p-3 md:p-4">وضعیت</th>
                    <th class="p-3 md:p-4 hidden md:table-cell">به‌روزرسانی</th>
                    <th class="p-3 md:p-4">عملیات</th>
                </tr>
                </thead>
                <tbody id="articles-tbody"></tbody>
            </table>
        </div>`;

    const MODAL_SHELL = `
<div id="articleModal"
     class="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 hidden flex items-center justify-center p-3 md:p-4">
    <div class="bg-white w-full max-w-4xl rounded-2xl shadow-2xl flex flex-col" style="max-height:82vh;">
        <div data-drag-handle class="shrink-0 flex justify-between items-center px-5 py-4 bg-gray-800 text-white rounded-t-2xl select-none">
            <h3 class="text-lg font-bold" id="article-modal-title">مقاله جدید</h3>
            <button onclick="closeModalSafe('articleModal')" class="text-white/70 hover:text-white text-2xl font-bold leading-none px-1 outline-none">✕</button>
        </div>
        <input type="hidden" id="a-id">
        <div class="flex-1 overflow-y-auto px-5 md:px-6 py-4 md:py-5" style="min-height:0;">

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div class="md:col-span-2">
                <label class="block text-sm text-gray-600 mb-1">عنوان مقاله <span class="text-red-500">*</span></label>
                <input type="text" id="a-title" placeholder="مثلاً: آموزش عیب‌یابی رادیو میکروتیک"
                       class="w-full p-2.5 border rounded-lg outline-none" oninput="ArticlesAdminTab.seoPreview()">
            </div>
            <div>
                <label class="block text-sm text-gray-600 mb-1">اسلاگ آدرس (خالی = ساخت خودکار از عنوان)</label>
                <input type="text" id="a-slug" dir="ltr" class="w-full p-2.5 border rounded-lg outline-none font-mono text-sm">
            </div>
            <div>
                <label class="block text-sm text-gray-600 mb-1">وضعیت</label>
                <select id="a-published" class="w-full p-2.5 border rounded-lg bg-white outline-none">
                    <option value="false">پیش‌نویس (منتشر نشود)</option>
                    <option value="true">منتشرشده</option>
                </select>
            </div>
            <div class="md:col-span-2">
                <label class="block text-sm text-gray-600 mb-1">
                    خوشه‌ی محتوایی (Content Hub)
                    <span class="text-[10px] text-gray-400">— مقالات هم‌خوشه خودکار به هم لینک می‌شوند و صفحه‌ی مجموعه می‌سازند</span>
                </label>
                <input type="text" id="a-hub" list="hub-list" placeholder="مثلاً: آموزش میکروتیک"
                       class="w-full p-2.5 border rounded-lg outline-none">
                <datalist id="hub-list"></datalist>
            </div>
            <div class="md:col-span-2">
                <label class="block text-sm text-gray-600 mb-1">خلاصه (برای کارت بلاگ و توضیح متا)</label>
                <textarea id="a-excerpt" rows="2" class="w-full p-2.5 border rounded-lg outline-none resize-none"
                          oninput="ArticlesAdminTab.seoPreview()"></textarea>
            </div>
            <div class="md:col-span-2">
                <label class="block text-sm text-gray-600 mb-1">تصویر شاخص</label>
                <div class="flex items-center gap-3">
                    <input type="file" id="a-cover-file" accept="image/*" class="text-xs" onchange="ArticlesAdminTab.uploadCover(this)">
                    <img id="a-cover-preview" src="" alt="" class="h-14 rounded-lg border hidden">
                    <input type="hidden" id="a-cover">
                </div>
            </div>
        </div>

        <!-- ادیتور محتوا -->
        <div class="mt-4">
            <label class="block text-sm text-gray-600 mb-1">متن مقاله</label>
            <div id="a-editor" style="min-height:260px; direction:rtl;" class="bg-white border rounded-b-lg"></div>
        </div>

        <!-- سئو مقاله -->
        <div class="bg-purple-50 p-3 rounded-xl border border-purple-200 mt-4">
            <label class="block text-xs font-bold text-purple-800 mb-2">تنظیمات سئو مقاله</label>
            <div class="flex items-center justify-between mb-1">
                <label class="text-[11px] text-gray-600">عنوان سئو (خالی = عنوان مقاله + برند)</label>
                <span id="a-seo-title-count" class="text-[10px] text-gray-400">0/60</span>
            </div>
            <input type="text" id="a-seo-title" class="w-full p-2 text-sm border rounded-lg outline-none"
                   oninput="ArticlesAdminTab.seoPreview()">
            <div class="flex items-center justify-between mt-2 mb-1">
                <label class="text-[11px] text-gray-600">توضیح متا (خالی = خلاصه)</label>
                <span id="a-seo-desc-count" class="text-[10px] text-gray-400">0/160</span>
            </div>
            <textarea id="a-seo-desc" rows="2" class="w-full p-2 text-sm border rounded-lg outline-none resize-none"
                      oninput="ArticlesAdminTab.seoPreview()"></textarea>
        </div>

        </div>
        <div class="shrink-0 flex flex-col md:flex-row justify-end gap-3 px-5 py-4 border-t bg-white rounded-b-2xl">
            <button onclick="closeModalSafe('articleModal')"
                    class="w-full md:w-auto px-4 py-2 text-gray-500 border rounded-lg">انصراف
            </button>
            <button onclick="ArticlesAdminTab.save()"
                    class="w-full md:w-auto px-6 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700 font-bold">
                ذخیره مقاله
            </button>
        </div>
    </div>
</div>`;

    window.ArticlesAdminTab = {
        mount, fetch: fetchArticles, getArticles,
        filter: filterArticles, quickSetHub, openNew, edit, remove, save,
        uploadCover, seoPreview: updateSeoPreview
    };
})();
