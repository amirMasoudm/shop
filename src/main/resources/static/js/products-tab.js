/**
 * تبِ محصولات — ماژولِ مشترکِ UI بینِ پنلِ ادمین و پنلِ فروشِ حضوری (SalesPanel).
 *
 * عمداً یک فایلِ واحد است، هم‌سو با همان تصمیمی که برایِ pricing-workspace.js
 * گرفته شد: اگر فرمِ محصول (عکس‌ها، مشخصاتِ فنی، FAQ، سئو/اسلاگ) در دو صفحه کپی
 * می‌شد، هر تغییرِ بعدی باید دو جا انجام می‌گرفت و یکی‌شان عقب می‌ماند.
 *
 * مارک‌آپِ تب/مودال در /fragments/products-tab-fragment.html است (نه رشته‌ی JS)،
 * چون فرمِ محصول به‌قدرِ کافی بزرگ است که نگه‌داشتنش به‌صورتِ HTMLِ خام
 * (نه Stringِ داخلِ جاوااسکریپت) خواناتر/قابل‌ویرایش‌تر است.
 *
 * وابستگی‌هایی که میزبان باید فراهم کند (window): axios، Swal، escapeHTML،
 * toggleLoader، API (رشته‌ی پایه‌ی مسیرِ API)، Sortable (کتابخانه — /js/Sortable.min.js)،
 * و موتورِ مودالِ عمومی از admin-modal-utils.js (openModal/closeModal/closeModalSafe/
 * resetForm/FORM_MODALS).
 *
 * مرزِ واقعیِ دسترسی سمتِ سرور است (/api/v1/products/admin/**، POST/PUT/DELETEِ
 * /api/v1/products/**)؛ نمایش/عدم‌نمایشِ دکمه‌ی تب در میزبان فقط برایِ UI است.
 */
(function () {
    const AI_API = "/api/v1/ai/generate-description";
    const FALLBACK_IMG = "data:image/svg+xml;charset=UTF-8,%3Csvg%20width%3D%2240%22%20height%3D%2240%22%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20viewBox%3D%220%200%2040%2040%22%20preserveAspectRatio%3D%22none%22%3E%3Cdefs%3E%3Cstyle%20type%3D%22text%2Fcss%22%3E%23holder_1%20text%20%7B%20fill%3A%23999%3Bfont-weight%3Anormal%3Bfont-family%3AHelvetica%2C%20monospace%3Bfont-size%3A10pt%20%7D%20%3C%2Fstyle%3E%3C%2Fdefs%3E%3Cg%20id%3D%22holder_1%22%3E%3Crect%20width%3D%2240%22%20height%3D%2240%22%20fill%3D%22%23eee%22%3E%3C%2Frect%3E%3Cg%3E%3Ctext%20x%3D%2210%22%20y%3D%2224%22%3EIMG%3C%2Ftext%3E%3C%2Fg%3E%3C%2Fg%3E%3C%2Fsvg%3E";
    const FRAGMENT_URL = "/fragments/products-tab-fragment.html";

    // ---- استیتِ ماژول (مستقل از state سراسریِ میزبان) ----
    let products = [];
    let flatCategories = [];
    let currentImages = [];
    let currentImageAlts = [];
    let currentSpecs = {};
    let mounted = false;
    /** پیش‌فرض true چون پنلِ ادمین همیشه ADMIN است؛ پنلِ فروش صریح ست می‌کند. */
    let canEdit = true;
    let onRefreshedCb = null;

    function getApi() { return window.API || '/api'; }

    // ==========================================
    // mount — تزریقِ مارک‌آپ + راه‌اندازیِ رویدادها (یک‌بار)
    // ==========================================
    async function mount(tabEl, modalEl, opts) {
        opts = opts || {};
        // فقط ADMIN ویرایش دارد؛ بقیهٔ نقش‌ها همین تب را فقط می‌بینند. مرزِ واقعی
        // سمتِ سرور است (POST/PUT/DELETEِ /v1/products فقط ADMIN)؛ این فقط برایِ
        // این است که دکمه‌ای نشان داده نشود که کلیکش حتماً ۴۰۳ می‌گیرد.
        if (opts.canEdit !== undefined) canEdit = !!opts.canEdit;
        if (!mounted) {
            const html = await fetch(FRAGMENT_URL).then(r => r.text());
            const doc = new DOMParser().parseFromString(html, 'text/html');
            tabEl.innerHTML = doc.getElementById('products-tab-shell-tpl').innerHTML;
            modalEl.innerHTML = doc.getElementById('products-tab-modal-tpl').innerHTML;

            if (window.FORM_MODALS) window.FORM_MODALS.add('productModal');
            bindEvents();
            mounted = true;
        }
        const addBtn = tabEl.querySelector('[data-add-product]');
        if (addBtn) addBtn.style.display = canEdit ? '' : 'none';
        if (typeof opts.onRefreshed === 'function') onRefreshedCb = opts.onRefreshed;
        await refresh();
    }

    function bindEvents() {
        // جابجاییِ عکس‌ها با درگ
        const imgBox = document.getElementById('image-preview-container');
        if (imgBox && window.Sortable) {
            new Sortable(imgBox, {
                animation: 150,
                // روی اینپوتِ alt درگ شروع نشود (وگرنه تایپ/انتخابِ متن مختل می‌شود)
                filter: '.img-alt-input, button',
                preventOnFilter: false,
                onEnd: () => {
                    syncImagesFromDom();
                    renderImages();
                }
            });

            const hi = () => imgBox.classList.add('border-indigo-500', 'bg-indigo-50');
            const lo = () => imgBox.classList.remove('border-indigo-500', 'bg-indigo-50');
            ['dragenter', 'dragover'].forEach(ev =>
                imgBox.addEventListener(ev, e => { e.preventDefault(); e.stopPropagation(); hi(); }));
            ['dragleave', 'drop'].forEach(ev =>
                imgBox.addEventListener(ev, e => { e.preventDefault(); e.stopPropagation(); lo(); }));
            imgBox.addEventListener('drop', e => {
                const files = Array.from(e.dataTransfer?.files || []);
                if (files.length) uploadProductImages(files);
            });
        }

        const pCat = document.getElementById('p-category');
        if (pCat) {
            pCat.addEventListener('change', function (e) {
                currentSpecs = {};
                renderSpecInputs(e.target.value);
            });
        }
    }

    // ==========================================
    // واکشی + رندرِ جدول
    // ==========================================
    async function refresh() {
        const [prodsRes, catsRes] = await Promise.all([
            axios.get(`${getApi()}/v1/products/admin`),
            axios.get(`${getApi()}/categories/tree?type=ONLINE`)
        ]);
        products = prodsRes.data || [];
        flatCategories = flatten(catsRes.data || []);
        renderProducts();
        populateCategorySelects();
        if (onRefreshedCb) onRefreshedCb(products);
    }

    function flatten(list, res) {
        res = res || [];
        (list || []).forEach(c => {
            res.push(c);
            if (c.children) flatten(c.children, res);
        });
        return res;
    }

    function getCatName(id) {
        const c = flatCategories.find(x => x.id === id);
        return c ? c.name : '-';
    }

    function collectSubtreeIds(id) {
        const set = new Set([id]);
        let changed = true;
        while (changed) {
            changed = false;
            flatCategories.forEach(c => {
                if (c.parentId && set.has(c.parentId) && !set.has(c.id)) {
                    set.add(c.id);
                    changed = true;
                }
            });
        }
        return set;
    }

    function populateProductFilterCategory() {
        const sel = document.getElementById('filter-category');
        if (!sel) return;
        const current = sel.value;
        sel.innerHTML = '<option value="">همه دسته‌ها</option>' +
            flatCategories.map(c => `<option value="${c.id}">${'— '.repeat(c.level || 0)}${c.name}</option>`).join('');
        sel.value = current;
    }

    // دسته‌بندیِ سایت/انبار در فرمِ محصول (نه c-parentِ تبِ دسته‌بندی — آن فقط در Admin.html است)
    function populateCategorySelects() {
        const pCat = document.getElementById('p-category');
        if (pCat) {
            pCat.innerHTML = '<option value="">انتخاب...</option>' +
                flatCategories.map(c => `<option value="${c.id}">${c.name}</option>`).join('');
        }
        axios.get(`${getApi()}/categories/tree?type=WAREHOUSE`).then(res => {
            const whFlat = flatten(res.data || []);
            const whSel = document.getElementById('p-warehouse-cat');
            if (whSel) whSel.innerHTML = '<option value="">انتخاب...</option>' + whFlat.map(c => `<option value="${c.id}">${c.name}</option>`).join('');
        }).catch(() => {});
    }

    function renderProductRows(list) {
        const tbody = document.getElementById('products-table-body');
        if (!tbody) return;
        tbody.innerHTML = list.map(p => `
            <tr class="hover:bg-gray-50 border-b transition group">
                <td class="p-3 md:p-4"><img src="${p.images?.[0] || FALLBACK_IMG}" onerror="this.src='${FALLBACK_IMG}'" class="w-8 h-8 md:w-10 md:h-10 rounded object-cover border bg-white"></td>
                <td class="p-3 md:p-4">
                    <div class="font-bold text-gray-800 truncate w-24 md:w-auto">${p.name}</div>
                    <div class="text-[9px] md:text-[10px] text-gray-400 font-mono hidden md:block">ID: ${p.id.substring(0, 6)}... ${p.weight ? '| وزن: ' + p.weight + 'g' : ''}</div>
                </td>
                <td class="p-3 md:p-4 text-[10px] md:text-sm text-gray-600 hidden md:table-cell">${getCatName(p.categoryId)}</td>
                <td class="p-3 md:p-4 text-green-700 font-bold">${(p.onlinePrice || p.price || 0).toLocaleString()}</td>
                <td class="p-3 md:p-4 text-indigo-700 font-bold hidden md:table-cell">${(p.price || 0).toLocaleString()}</td>
                <td class="p-3 md:p-4 text-center"><span class="px-2 py-0.5 md:py-1 rounded text-[10px] md:text-xs font-bold ${p.stock > 0 ? 'bg-blue-100 text-blue-700' : 'bg-red-100 text-red-700'}">${p.stock}</span></td>
                <td class="p-3 md:p-4 text-center flex flex-col md:flex-row items-center justify-center gap-1 md:gap-2">
                    ${canEdit ? `
                    <button onclick='editProduct("${p.id}")' class="text-indigo-600 hover:bg-indigo-50 p-1 md:p-2 rounded transition">✏️</button>
                    <button onclick="deleteProductWithRedirect('${p.id}')" class="text-red-500 hover:bg-red-50 p-1 md:p-2 rounded transition">🗑️</button>`
                    : '<span class="text-[10px] text-gray-400">فقط مشاهده</span>'}
                </td>
            </tr>
        `).join('');

        const fc = document.getElementById('filter-count');
        if (fc) fc.innerText = `${list.length} از ${products.length} محصول`;
    }

    function applyProductFilters() {
        const q = (document.getElementById('prod-search').value || '').toLowerCase().trim();
        const catId = document.getElementById('filter-category').value;
        const minP = parseFloat(document.getElementById('filter-min-price').value);
        const maxP = parseFloat(document.getElementById('filter-max-price').value);
        const stock = document.getElementById('filter-stock').value;

        const subtree = catId ? collectSubtreeIds(catId) : null;

        const list = products.filter(p => {
            if (q && !((p.name || '').toLowerCase().includes(q) || (p.id || '').toLowerCase().includes(q))) return false;
            if (subtree && !subtree.has(p.categoryId)) return false;

            const price = p.onlinePrice || p.price || 0;
            if (!isNaN(minP) && price < minP) return false;
            if (!isNaN(maxP) && price > maxP) return false;

            const s = p.stock || 0;
            if (stock === 'in' && !(s > 0)) return false;
            if (stock === 'out' && !(s <= 0)) return false;
            if (stock === 'low' && !(s > 0 && s <= 5)) return false;

            return true;
        });

        renderProductRows(list);
    }

    function renderProducts() {
        populateProductFilterCategory();
        applyProductFilters();
    }

    function resetProductFilters() {
        document.getElementById('prod-search').value = '';
        document.getElementById('filter-category').value = '';
        document.getElementById('filter-min-price').value = '';
        document.getElementById('filter-max-price').value = '';
        document.getElementById('filter-stock').value = '';
        applyProductFilters();
    }

    // ==========================================
    // مودالِ افزودن/ویرایش
    // ==========================================
    function togglePackInput() {
        const unit = document.getElementById('p-unit').value;
        const wrap = document.getElementById('pack-qty-wrapper');
        if (unit === 'بسته') wrap.classList.remove('hidden'); else wrap.classList.add('hidden');
    }

    function openProductModal() {
        resetForm('p');
        currentImages = [];
        currentImageAlts = [];
        currentSpecs = {};
        document.getElementById('p-weight').value = '';
        document.getElementById('p-length').value = '';
        document.getElementById('p-width').value = '';
        document.getElementById('p-height').value = '';

        renderImages();
        populateCategorySelects();
        document.getElementById('p-specs-container').innerHTML = '<p class="text-xs text-gray-400 col-span-full">لطفاً ابتدا دسته‌بندی سایت را انتخاب کنید.</p>';
        renderTechSpecRows([]);
        renderFaqRows([]);
        if (window.RfSpecEditor) RfSpecEditor.render(document.getElementById('rf-spec-editor'), null);
        renderRelProducts([], null);
        setSlugHint(false);
        // پیش‌فرضِ محصولِ تازه «نمایش در ترب» است — همان چیزی که برایِ محصولاتِ قدیمی
        // هم صادق است (نبودنِ فیلد یعنی فعال).
        document.getElementById('p-torob-enabled').checked = true;
        document.getElementById('p-discontinued').checked = false;
        document.getElementById('p-discontinued-notice').checked = false;
        document.getElementById('p-replacement-search').value = '';
        renderReplacementProducts(null, null);
        toggleDiscontinuedFields();
        document.getElementById('modal-title-action').textContent = 'افزودن محصول';
        document.getElementById('modal-title-id').textContent = 'ID: NEW';
        showHolooForNewProduct();
        openModal('productModal');
    }

    function editProduct(id) {
        const p = products.find(x => x.id === id);
        if (!p) return;
        document.getElementById('p-id').value = p.id;
        document.getElementById('p-name').value = p.name;
        document.getElementById('p-base-price').value = p.basePrice || '';
        document.getElementById('p-price').value = p.price || '';
        document.getElementById('p-online-price').value = p.onlinePrice || p.price || '';
        document.getElementById('p-stock').value = p.stock;
        document.getElementById('p-discount').value = p.discountPercent || '';
        document.getElementById('p-unit').value = p.unit || 'عدد';
        document.getElementById('p-pack-qty').value = p.packQuantity || '';
        document.getElementById('p-warehouse-desc').value = p.warehouseDescription || '';
        document.getElementById('p-desc').value = p.description || '';
        document.getElementById('p-slug').value = p.slug || '';
        // 🔴 نه !!p.torobEnabled — محصولی که این فیلد را ندارد باید تیک‌خورده بیاید،
        // وگرنه اولین ذخیره از پنل خاموشش می‌کند.
        document.getElementById('p-torob-enabled').checked = (p.torobEnabled !== false);
        document.getElementById('p-discontinued').checked = (p.discontinued === true);
        document.getElementById('p-discontinued-notice').checked = (p.discontinuedNoticeVisible === true);
        document.getElementById('p-replacement-search').value = '';
        renderReplacementProducts(p.replacementProductId || null, p.id);
        toggleDiscontinuedFields();
        showHolooForExistingProduct(p.holooCode, p.id);
        document.getElementById('p-seo-title').value = p.seoTitle || '';
        document.getElementById('p-seo-desc').value = p.seoDescription || '';
        document.getElementById('p-weight').value = p.weight || '';
        document.getElementById('p-length').value = p.length || '';
        document.getElementById('p-width').value = p.width || '';
        document.getElementById('p-height').value = p.height || '';

        togglePackInput();
        populateCategorySelects();

        document.getElementById('p-category').value = p.categoryId;
        document.getElementById('p-warehouse-cat').value = p.warehouseCategoryId;

        currentSpecs = p.specifications || {};
        renderSpecInputs(p.categoryId);

        renderTechSpecRows(p.techSpecs || []);
        renderFaqRows(p.faqs || []);
        renderRelProducts(p.relatedProductIds || [], p.id);
        if (window.RfSpecEditor) RfSpecEditor.render(document.getElementById('rf-spec-editor'), p.rf || null);

        currentImages = p.images || [];
        currentImageAlts = (p.images || []).map((_, i) => (p.imageAlts && p.imageAlts[i]) ? p.imageAlts[i] : '');
        renderImages();

        setSlugHint(true);
        document.getElementById('modal-title-action').textContent = 'ویرایش محصول';
        document.getElementById('modal-title-id').textContent = 'ID: ' + p.id;
        openModal('productModal');
    }

    function setSlugHint(isEdit) {
        const el = document.getElementById('p-slug-hint');
        if (!el) return;
        if (isEdit) {
            el.textContent = '⚠️ خالی‌کردنِ این فیلد آدرسِ فعلیِ محصول را از روی نام بازمی‌سازد و لینکِ قبلی می‌شکند. اگر عوضش کردید، هنگامِ ذخیره پیشنهادِ ریدایرکت داده می‌شود.';
            el.className = 'text-[9px] text-amber-600 mt-1';
        } else {
            el.textContent = 'اگر خالی بگذارید، سیستم به صورت خودکار از روی نام محصول می‌سازد.';
            el.className = 'text-[9px] text-gray-400 mt-1';
        }
    }

    function saveProduct() {
        const id = document.getElementById('p-id').value;
        syncImagesFromDom();
        const specs = {...(currentSpecs || {})};
        document.querySelectorAll('.spec-input').forEach(input => {
            const key = input.dataset.key;
            const val = input.value.trim();
            if (val !== '') specs[key] = val;
            else delete specs[key];
        });

        const data = {
            id: id || null,
            name: document.getElementById('p-name').value,
            basePrice: Number(document.getElementById('p-base-price').value),
            price: Number(document.getElementById('p-price').value),
            onlinePrice: Number(document.getElementById('p-online-price').value),
            stock: Number(document.getElementById('p-stock').value),
            discountPercent: Number(document.getElementById('p-discount').value),
            categoryId: document.getElementById('p-category').value,
            warehouseCategoryId: document.getElementById('p-warehouse-cat').value,
            unit: document.getElementById('p-unit').value,
            packQuantity: Number(document.getElementById('p-pack-qty').value),
            weight: Number(document.getElementById('p-weight').value) || 0,
            length: Number(document.getElementById('p-length').value) || 0,
            width: Number(document.getElementById('p-width').value) || 0,
            height: Number(document.getElementById('p-height').value) || 0,
            slug: document.getElementById('p-slug').value,
            seoTitle: document.getElementById('p-seo-title').value,
            seoDescription: document.getElementById('p-seo-desc').value,
            warehouseDescription: document.getElementById('p-warehouse-desc').value,
            description: document.getElementById('p-desc').value,
            images: currentImages,
            imageAlts: currentImageAlts,
            specifications: specs,
            techSpecs: collectTechSpecs(),
            faqs: collectFaqs(),
            relatedProductIds: collectRelatedIds(),
            torobEnabled: document.getElementById('p-torob-enabled').checked,
            discontinued: document.getElementById('p-discontinued').checked,
            // وقتی توقف خاموش است جایگزین هم پاک می‌شود — ماندنش فقط رکوردِ مرده می‌ساخت
            replacementProductId: document.getElementById('p-discontinued').checked ? collectReplacementId() : '',
            discontinuedNoticeVisible: document.getElementById('p-discontinued-notice').checked
        };

        // کدِ کالا فقط هنگامِ ساخت فرستاده می‌شود. در ویرایش عمداً فرستاده نمی‌شود —
        // سرور هم نادیده‌اش می‌گیرد، ولی نفرستادنش نیت را روشن نگه می‌دارد.
        if (!id) {
            const picked = document.getElementById('p-holoo-select');
            data.holooCode = picked ? picked.value : '';
        }

        const method = id ? 'put' : 'post';
        const url = id ? `${getApi()}/v1/products/${id}` : `${getApi()}/v1/products`;

        const before = id ? products.find(x => x.id === id) : null;
        const oldResolver = before ? productResolverOf(before) : null;

        axios[method](url, data).then(async (res) => {
            const saved = res && res.data;
            // مشخصاتِ رادیویی جدا ذخیره می‌شود (فقط اگر عوض شده). خطایش محصول را برنمی‌گرداند؛
            // مودال باز می‌ماند تا ادمین اصلاح کند.
            if (window.RfSpecEditor && saved && saved.id) {
                try { await RfSpecEditor.save(saved.id); }
                catch (e) {
                    const m = (e.response && e.response.data && e.response.data.message) || e.message || 'ذخیرهٔ مشخصاتِ رادیویی ناموفق بود';
                    Swal.fire('محصول ذخیره شد، مشخصاتِ رادیویی نه', m, 'warning');
                    return;
                }
            }
            closeModal('productModal');
            if (oldResolver && saved) {
                const newResolver = productResolverOf(saved);
                if (newResolver && newResolver !== oldResolver) {
                    await offerSlugChangeRedirect(oldResolver, newResolver, saved.name);
                }
            }
            await refresh();
        }).catch(err => {
            Swal.fire('خطا', (err.response && err.response.data && (err.response.data.message || err.response.data)) || 'ذخیره‌ی محصول ناموفق بود', 'error');
        });
    }

    async function offerSlugChangeRedirect(oldResolver, newResolver, name) {
        const r = await Swal.fire({
            title: 'آدرسِ محصول عوض شد',
            html: `<div style="text-align:right; line-height:2; font-size:.9rem;">
                     محصول: <b>${escapeHTML(name || '')}</b><br>
                     آدرسِ قبلی: <code style="direction:ltr; display:inline-block;">/shop/product/${escapeHTML(oldResolver)}</code><br>
                     آدرسِ جدید: <code style="direction:ltr; display:inline-block;">/shop/product/${escapeHTML(newResolver)}</code><br><br>
                     آدرسِ قبلی از الان <b>۴۰۴</b> می‌دهد. ریدایرکتِ ۳۰۱ به آدرسِ جدید ثبت شود؟
                     <span style="display:block; color:#6b7280; font-size:.82rem;">
                       اگر این محصول قبلاً منتشر/ایندکس شده بود، «بله» را بزنید.
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
            const res = await axios.post(`${getApi()}/v1/product-redirects`, {
                fromSlug: oldResolver,
                toResolver: newResolver,
                note: `تغییرِ اسلاگِ «${name || ''}» از پنل`
            });
            if (res.data && res.data.targetResolves === false) {
                Swal.fire('ثبت شد، ولی...', 'مقصد الان به محصولِ زنده‌ای نمی‌رسد؛ آدرسِ قبلی همچنان ۴۰۴ می‌دهد.', 'warning');
            } else {
                Swal.fire({title: 'ریدایرکت ثبت شد', icon: 'success', timer: 1400, showConfirmButton: false});
            }
        } catch (e) {
            const msg = (e.response && e.response.data && (e.response.data.message || e.response.data)) || 'ثبتِ ریدایرکت ناموفق بود';
            // ⚠️ محصول از قبل ذخیره شده؛ این فقط هشدار است نه شکستِ ذخیره. اگر نقشِ
            // کاربر به /api/v1/product-redirects دسترسی نداشته باشد (۴۰۳) هم دقیقاً
            // همینجا و به همین شکلِ بی‌خطر فرود می‌آید — محصول سالم ذخیره مانده.
            Swal.fire('ریدایرکت ثبت نشد', escapeHTML(String(msg)) + ' — آدرسِ قبلی ۴۰۴ می‌دهد.', 'error');
        }
    }

    // ==========================================
    // جدولِ مشخصاتِ فنی + FAQ + محصولاتِ مکمل
    // ==========================================
    function techSpecRowHtml(r) {
        r = r || {};
        return `<div class="tech-spec-row grid grid-cols-12 gap-2">
            <input type="text" placeholder="گروه (اختیاری)" value="${(r.group || '').replace(/"/g,'&quot;')}"
                   class="ts-group col-span-3 p-2 text-xs border rounded-lg outline-none bg-blue-50/50">
            <input type="text" placeholder="عنوان (مثلاً: فرکانس)" value="${(r.key || '').replace(/"/g,'&quot;')}"
                   class="ts-key col-span-4 p-2 text-xs border rounded-lg outline-none">
            <input type="text" placeholder="مقدار (مثلاً: 5GHz)" value="${(r.value || '').replace(/"/g,'&quot;')}"
                   class="ts-value col-span-4 p-2 text-xs border rounded-lg outline-none">
            <button type="button" onclick="this.parentElement.remove()"
                    class="col-span-1 text-red-500 hover:bg-red-50 rounded-lg text-sm">✕</button>
        </div>`;
    }

    function renderTechSpecRows(rows) {
        document.getElementById('tech-specs-rows').innerHTML = (rows && rows.length)
            ? rows.map(techSpecRowHtml).join('') : '';
    }

    function addTechSpecRow() {
        document.getElementById('tech-specs-rows').insertAdjacentHTML('beforeend', techSpecRowHtml());
    }

    function collectTechSpecs() {
        return Array.from(document.querySelectorAll('#tech-specs-rows .tech-spec-row')).map(row => ({
            group: row.querySelector('.ts-group').value.trim() || null,
            key: row.querySelector('.ts-key').value.trim(),
            value: row.querySelector('.ts-value').value.trim()
        })).filter(r => r.key && r.value);
    }

    // faqRowHtml/renderFaqRows/addFaqRow عمداً سراسری می‌مانند (پایینِ فایل، در
    // Object.assign) — تبِ دوره‌هایِ Admin.html هم همین مارک‌آپِ FAQ را دوباره استفاده می‌کند.
    function faqRowHtml(f) {
        f = f || {};
        return `<div class="faq-row border rounded-lg p-2 bg-amber-50/40">
            <div class="flex gap-2 items-start">
                <div class="flex-1 space-y-1">
                    <input type="text" placeholder="سوال..." value="${(f.question || '').replace(/"/g,'&quot;')}"
                           class="fq-q w-full p-2 text-xs border rounded-lg outline-none font-bold">
                    <textarea placeholder="جواب..." rows="2"
                              class="fq-a w-full p-2 text-xs border rounded-lg outline-none resize-none">${(f.answer || '')}</textarea>
                </div>
                <button type="button" onclick="this.closest('.faq-row').remove()"
                        class="text-red-500 hover:bg-red-50 rounded-lg px-2 py-1 text-sm">✕</button>
            </div>
        </div>`;
    }

    function renderFaqRows(faqs) {
        document.getElementById('faq-rows').innerHTML = (faqs && faqs.length)
            ? faqs.map(faqRowHtml).join('') : '';
    }

    function addFaqRow() {
        document.getElementById('faq-rows').insertAdjacentHTML('beforeend', faqRowHtml());
    }

    function collectFaqs() {
        return Array.from(document.querySelectorAll('#faq-rows .faq-row')).map(row => ({
            question: row.querySelector('.fq-q').value.trim(),
            answer: row.querySelector('.fq-a').value.trim()
        })).filter(f => f.question && f.answer);
    }

    function productPickRow(p, inputHtml) {
        return `
            <label class="rel-prod-item flex items-center gap-2 p-1.5 hover:bg-gray-50 rounded cursor-pointer border-b border-gray-100 last:border-0"
                   data-name="${(p.name || '').toLowerCase()}">
                ${inputHtml}
                <img src="${p.images?.[0] || FALLBACK_IMG}" class="w-6 h-6 rounded object-cover bg-gray-200" onerror="this.src='${FALLBACK_IMG}'">
                <span class="text-xs text-gray-700">${p.name}</span>
            </label>`;
    }

    function renderRelProducts(selectedIds, excludeId) {
        const list = products.filter(p => p.id !== excludeId);
        document.getElementById('rel-prod-list').innerHTML = list.map(p => productPickRow(p,
            `<input type="checkbox" value="${p.id}" class="rel-prod-chk rounded h-4 w-4"
                    ${(selectedIds || []).includes(p.id) ? 'checked' : ''}>`)).join('');
    }

    function filterRelProducts() {
        const q = document.getElementById('rel-prod-search').value.toLowerCase();
        // ⚠️ محدود به همین لیست: productPickRow کلاسِ rel-prod-item را در لیستِ جایگزین
        // هم می‌گذارد، و جست‌وجوی سراسری آن‌جا را هم پنهان می‌کرد.
        document.querySelectorAll('#rel-prod-list .rel-prod-item').forEach(item => {
            item.style.display = item.dataset.name.includes(q) ? 'flex' : 'none';
        });
    }

    // ── توقفِ تولید ─────────────────────────────────────────────────────────
    // جایگزین فقط یکی است، پس رادیو و نه تیک؛ و گزینهٔ «بدونِ جایگزین» عمداً اول
    // می‌آید، چون حالتِ پیش‌فرض و امن است (صفحه می‌ماند، ۳۰۱ی نمی‌خورد).
    function renderReplacementProducts(selectedId, excludeId) {
        const none = `
            <label class="rel-prod-item flex items-center gap-2 p-1.5 hover:bg-gray-50 rounded cursor-pointer border-b border-gray-100"
                   data-name="">
                <input type="radio" name="p-replacement" value="" class="h-4 w-4" ${selectedId ? '' : 'checked'}>
                <span class="text-xs text-gray-500">بدونِ جایگزین — صفحه با همین آدرس بماند</span>
            </label>`;
        // جایگزینِ متوقف‌شده هم انتخاب‌شدنی است (زنجیره دنبال می‌شود)، ولی علامت می‌خورد
        const rows = products.filter(p => p.id !== excludeId).map(p => productPickRow(p,
            `<input type="radio" name="p-replacement" value="${p.id}" class="h-4 w-4"
                    ${p.id === selectedId ? 'checked' : ''}>`
            + (p.discontinued ? '<span class="text-[9px] text-rose-700 font-bold">متوقف</span>' : '')));
        document.getElementById('p-replacement-list').innerHTML = none + rows.join('');
    }

    function filterReplacementProducts() {
        const q = document.getElementById('p-replacement-search').value.toLowerCase();
        document.querySelectorAll('#p-replacement-list .rel-prod-item').forEach(item => {
            // گزینهٔ «بدونِ جایگزین» همیشه پیدا بماند
            const always = item.dataset.name === '';
            item.style.display = (always || item.dataset.name.includes(q)) ? 'flex' : 'none';
        });
    }

    function collectReplacementId() {
        const r = document.querySelector('#p-replacement-list input[name="p-replacement"]:checked');
        // رشتهٔ خالی یعنی «بدونِ جایگزین» — سرور آن را پاک می‌کند؛ null یعنی «دست نزن»
        return r ? r.value : '';
    }

    function toggleDiscontinuedFields() {
        const on = document.getElementById('p-discontinued').checked;
        document.getElementById('p-discontinued-fields').classList.toggle('hidden', !on);
    }

    function collectRelatedIds() {
        return Array.from(document.querySelectorAll('.rel-prod-chk:checked')).map(c => c.value).slice(0, 10);
    }

    function renderSpecInputs(catId) {
        const container = document.getElementById('p-specs-container');
        container.innerHTML = '';

        if (!catId) {
            container.innerHTML = '<p class="text-xs text-gray-400 col-span-full">لطفاً ابتدا دسته‌بندی سایت را انتخاب کنید.</p>';
            return;
        }

        const category = flatCategories.find(c => c.id === catId);

        if (!category || !category.filterKeys || category.filterKeys.length === 0) {
            container.innerHTML = '<p class="text-xs text-gray-400 col-span-full">این دسته ویژگی خاصی برای فیلتر ندارد.</p>';
            return;
        }

        category.filterKeys.forEach(key => {
            const val = currentSpecs[key] || '';
            const div = document.createElement('div');
            div.innerHTML = `
                <label class="block text-xs font-bold text-gray-700 mb-1">${key}</label>
                <input type="text" class="spec-input w-full p-2 border rounded-lg focus:ring-2 focus:ring-purple-400 outline-none text-sm"
                       data-key="${key}" value="${val}" placeholder="مقدار را وارد کنید...">
            `;
            container.appendChild(div);
        });
    }

    // ==========================================
    // عکس‌ها
    // ==========================================
    async function handleImageSelect(e) {
        await uploadProductImages(Array.from(e.target.files));
        e.target.value = '';
    }

    async function uploadProductImages(files) {
        files = files.filter(f => f && f.type && f.type.startsWith('image/'));
        if (!files.length) return;
        syncImagesFromDom();
        toggleLoader(true);
        try {
            for (let f of files) {
                const formData = new FormData();
                formData.append("file", f);
                const res = await axios.post(`${getApi()}/v1/files/upload`, formData, {
                    headers: { 'Content-Type': 'multipart/form-data' }
                });
                currentImages.push(res.data);
                currentImageAlts.push('');
            }
            renderImages();
        } catch (error) {
            console.error("خطا در آپلود عکس:", error);
            Swal.fire('خطا', 'آپلود تصویر با مشکل مواجه شد. حجم فایل را بررسی کنید.', 'error');
        } finally {
            toggleLoader(false);
        }
    }

    function addImageByUrl(url) {
        if (url) {
            currentImages.push(url);
            renderImages();
            document.getElementById('p-image-url-box').value = '';
        }
    }

    function renderImages() {
        const n = currentImages.length;
        document.getElementById('image-preview-container').innerHTML = currentImages.map((src, i) => `
            <div class="img-item relative w-full rounded shadow group bg-white p-1 ${i === 0 ? 'ring-2 ring-green-500' : ''}">
                <img src="${src}" data-src="${src}" onerror="this.src='${FALLBACK_IMG}'"
                     class="w-full h-16 md:h-24 object-cover cursor-move rounded" title="برای جابه‌جایی بکشید">
                ${i === 0
                    ? '<span class="img-badge-main absolute bottom-8 md:bottom-10 right-1 bg-green-600 text-white text-[8px] md:text-[9px] px-1.5 py-0.5 rounded-full font-bold shadow">تصویر اصلی</span>'
                    : `<button onclick="makeMainImage(${i})" title="این تصویر، تصویرِ اصلیِ کارت شود"
                               class="img-btn-main absolute bottom-8 md:bottom-10 right-1 text-[8px] md:text-[9px] px-1.5 py-0.5 rounded-full font-bold shadow">اصلی کن</button>`}
                <button onclick="removeImage(${i})" title="حذف تصویر"
                        class="img-btn-del absolute top-1 left-1 rounded-full w-5 h-5 md:w-6 md:h-6 flex items-center justify-center text-[11px] md:text-sm font-bold">✕</button>
                <span class="absolute top-1 right-1 bg-gray-800/75 text-white rounded px-1 text-[8px] md:text-[9px] font-bold">${i + 1}/${n}</span>
                <input type="text" class="img-alt-input w-full mt-1 p-1 text-[10px] border rounded outline-none" placeholder="متن جایگزین (alt)"
                       value="${(currentImageAlts[i] || '').replace(/"/g, '&quot;')}">
            </div>
        `).join('');
    }

    function makeMainImage(i) {
        syncImagesFromDom();
        if (i <= 0 || i >= currentImages.length) return;
        currentImages.unshift(currentImages.splice(i, 1)[0]);
        currentImageAlts.unshift(currentImageAlts.splice(i, 1)[0]);
        renderImages();
    }

    function syncImagesFromDom() {
        const items = document.querySelectorAll('#image-preview-container .img-item');
        const imgs = [], alts = [];
        items.forEach(it => {
            const im = it.querySelector('img');
            imgs.push(im.getAttribute('data-src') || im.getAttribute('src'));
            alts.push((it.querySelector('.img-alt-input')?.value || '').trim());
        });
        currentImages = imgs;
        currentImageAlts = alts;
    }

    function removeImage(i) {
        syncImagesFromDom();
        currentImages.splice(i, 1);
        currentImageAlts.splice(i, 1);
        renderImages();
    }

    function generateDescriptionWithAI() {
        const name = document.getElementById('p-name').value;
        if (!name) return Swal.fire('هشدار', 'ابتدا نام محصول را وارد کنید', 'warning');
        const btn = document.getElementById('btn-ai');
        const original = btn.innerHTML;
        btn.innerHTML = '⏳ در حال ارتباط...';
        btn.disabled = true;
        axios.post(AI_API, {
            name: name,
            attributes: `دسته‌بندی: ${getCatName(document.getElementById('p-category').value)}`,
            additionalInfo: ''
        })
            .then(res => {
                const text = res.data.description || res.data;
                const txtArea = document.getElementById('p-desc');
                txtArea.value = '';
                let i = 0;
                const type = () => {
                    if (i < text.length) {
                        txtArea.value += text.charAt(i);
                        i++;
                        setTimeout(type, 10);
                    }
                };
                type();
            })
            .catch(err => {
                console.error(err);
                Swal.fire('خطا', 'مشکل در ارتباط با سرور هوش مصنوعی', 'error');
            })
            .finally(() => {
                btn.innerHTML = original;
                btn.disabled = false;
            });
    }

    // ==========================================
    // حذفِ محصول با تعیینِ تکلیفِ آدرسِ قدیمی (۳۰۱ به‌جایِ ۴۰۴)
    // ==========================================
    function productResolverOf(p) {
        return (p && p.slug && p.slug.trim()) ? p.slug.trim() : (p ? p.id : '');
    }

    async function deleteProductWithRedirect(id) {
        const product = products.find(p => p.id === id);
        if (!product) { Swal.fire('خطا', 'محصول در لیستِ پنل پیدا نشد. صفحه را تازه کنید.', 'error'); return; }

        const name = escapeHTML(product.name || '');
        const fromSlug = productResolverOf(product);

        const choice = await Swal.fire({
            title: 'حذفِ محصول',
            html: `<div style="text-align:right; line-height:2; font-size:.9rem;">
                     محصول: <b>${name}</b><br>
                     آدرسِ فعلی: <code style="direction:ltr; display:inline-block;">/shop/product/${escapeHTML(fromSlug)}</code><br><br>
                     آیا آدرسِ این محصول به محصولِ دیگری ریدایرکت شود؟<br>
                     <span style="color:#6b7280; font-size:.82rem;">
                       اگر این محصول تکراری/ادغام‌شونده است «بله» را بزنید؛
                       اگر هرگز منتشر و ایندکس نشده، «رد کن».
                     </span>
                   </div>`,
            icon: 'warning',
            showDenyButton: true,
            showCancelButton: true,
            confirmButtonText: 'ریدایرکت به محصولِ دیگر',
            denyButtonText: 'رد کن، مستقیم حذف کن',
            cancelButtonText: 'انصراف',
            confirmButtonColor: '#1b4f8a',
            denyButtonColor: '#d33'
        });

        if (choice.isDismissed) return;

        if (choice.isDenied) {
            const sure = await Swal.fire({
                title: 'بدونِ ریدایرکت حذف شود؟',
                html: `<div style="text-align:right; line-height:2; font-size:.9rem;">
                         این کار آدرسِ محصول را <b>۴۰۴</b> می‌کند.<br>
                         مطمئنید این محصول هرگز منتشر/ایندکس نشده؟
                       </div>`,
                icon: 'warning', showCancelButton: true,
                confirmButtonText: 'بله، مستقیم حذف کن', cancelButtonText: 'انصراف',
                confirmButtonColor: '#d33'
            });
            if (!sure.isConfirmed) return;
            await performProductDelete(id);
            return;
        }

        const target = await pickRedirectTarget(id);
        if (!target) return;

        const toResolver = productResolverOf(target);
        let data;
        try {
            const res = await axios.post(`${getApi()}/v1/product-redirects`, {
                fromSlug: fromSlug,
                toResolver: toResolver,
                note: `حذفِ «${product.name}» از پنل`
            });
            data = res.data || {};
        } catch (e) {
            // ⚠️ اگر نقشِ کاربر به /api/v1/product-redirects دسترسی نداشته باشد
            // (۴۰۳)، همین‌جا با پیامِ روشن متوقف می‌شود — محصول حذف نمی‌شود.
            const msg = (e.response && e.response.data && (e.response.data.message || e.response.data)) || 'ثبتِ ریدایرکت ناموفق بود';
            Swal.fire('ریدایرکت ثبت نشد', escapeHTML(String(msg)), 'error');
            return;
        }

        if (data.targetResolves === false) {
            const cont = await Swal.fire({
                title: 'هشدار: مقصد حل نمی‌شود',
                html: `<div style="text-align:right; line-height:2; font-size:.9rem;">
                         ریدایرکت ثبت شد، ولی مقصد الان به هیچ محصولِ زنده‌ای نمی‌رسد —
                         یعنی آدرسِ قدیمی همچنان <b>۴۰۴</b> می‌دهد.<br>
                         بازهم محصول حذف شود؟
                       </div>`,
                icon: 'warning', showCancelButton: true,
                confirmButtonText: 'بله، حذف کن', cancelButtonText: 'انصراف — فعلاً حذف نکن',
                confirmButtonColor: '#d33'
            });
            if (!cont.isConfirmed) {
                Swal.fire('حذف نشد', 'رکوردِ ریدایرکت ثبت شده و باقی می‌ماند. محصول دست‌نخورده است.', 'info');
                return;
            }
        }

        await performProductDelete(id);
    }

    async function pickRedirectTarget(excludeId) {
        const list = products.filter(p => p.id !== excludeId);
        if (!list.length) {
            await Swal.fire('محصولی نیست', 'محصولِ دیگری برایِ ریدایرکت وجود ندارد.', 'info');
            return null;
        }

        const rows = list.map(p => productPickRow(p,
            `<input type="radio" name="redirect-target" value="${p.id}" class="redirect-target-radio h-4 w-4">`)).join('');

        const res = await Swal.fire({
            title: 'مقصدِ ریدایرکت',
            html: `<input id="redirect-target-search" placeholder="جست‌وجویِ نامِ محصول..."
                          style="width:100%; padding:8px 10px; border:1px solid #d1d5db; border-radius:8px; margin-bottom:10px; font-size:.85rem;">
                   <div id="redirect-target-list" style="max-height:320px; overflow-y:auto; text-align:right;">${rows}</div>`,
            width: 560,
            showCancelButton: true,
            confirmButtonText: 'تأیید مقصد',
            cancelButtonText: 'انصراف',
            confirmButtonColor: '#1b4f8a',
            didOpen: () => {
                const box = document.getElementById('redirect-target-search');
                box.addEventListener('input', () => {
                    const q = box.value.toLowerCase();
                    document.querySelectorAll('#redirect-target-list .rel-prod-item').forEach(item => {
                        item.style.display = item.dataset.name.includes(q) ? 'flex' : 'none';
                    });
                });
            },
            preConfirm: () => {
                const picked = document.querySelector('.redirect-target-radio:checked');
                if (!picked) { Swal.showValidationMessage('یک محصولِ مقصد انتخاب کنید'); return false; }
                return picked.value;
            }
        });

        if (!res.isConfirmed) return null;
        return products.find(p => p.id === res.value) || null;
    }

    async function performProductDelete(id) {
        try {
            await axios.delete(`${getApi()}/v1/products/${id}`);
            await refresh();
            Swal.fire({title: 'حذف شد', icon: 'success', timer: 1400, showConfirmButton: false});
        } catch (e) {
            Swal.fire('خطا', 'حذفِ محصول ناموفق بود.', 'error');
        }
    }

    // getProducts: برایِ مصرف‌کننده‌هایِ دیگرِ Admin.html که به فهرستِ محصولات نیاز
    // دارند ولی خودشان بخشی از تبِ محصولات نیستند (مثلاً انتخاب‌گرِ محصولِ سکشنِ
    // لندینگ، لینکِ محصولِ بنر، یا نمایشِ نامِ محصولِ یک نظر) — به‌جایِ فچِ دوباره،
    // همان دیتایِ همین ماژول را می‌خوانند.
    // ==========================================
    // کدِ کالا (هلو)
    // ==========================================

    /**
     * ویرایش: کد فقط نشان داده می‌شود.
     * 🔴 عمداً ورودی نیست — کدی که شرکت در هلو روی کالا نشانده نباید از اینجا عوض
     * شود، وگرنه اولین همگام‌سازی آن کالا را پیدا نمی‌کند. سرور هم همین را اعمال
     * می‌کند؛ این فقط نیمهٔ کاربریِ همان قاعده است.
     */
    function showHolooForExistingProduct(code, productId) {
        const box = document.getElementById('p-holoo-existing');
        const issueBtn = document.getElementById('p-holoo-issue');
        if (!box) return;

        // محصولِ بی‌کد: همان جعبهٔ حالتِ ساخت بازاستفاده می‌شود (نه کپیِ دوم از آن)،
        // به‌علاوهٔ دکمهٔ صدور. بدونِ این، کالای بی‌کد هیچ راهی برای گرفتنِ کد نداشت
        // و در هیچ همگام‌سازیِ موجودی‌ای پیدا نمی‌شد.
        if (!code) {
            showHolooForNewProduct();
            if (issueBtn) {
                issueBtn.classList.remove('hidden');
                issueBtn.dataset.productId = productId || '';
                issueBtn.disabled = false;
                issueBtn.textContent = 'صدورِ کدِ هلو';
            }
            return;
        }

        const fresh = document.getElementById('p-holoo-new');
        if (fresh) fresh.classList.add('hidden');
        if (issueBtn) issueBtn.classList.add('hidden');
        box.classList.remove('hidden');
        document.getElementById('p-holoo-code-text').textContent = code;
    }

    /** ساخت: یا کدِ تازه، یا یکی از رزروهایِ مصرف‌نشده. */
    async function showHolooForNewProduct() {
        const box = document.getElementById('p-holoo-existing');
        const fresh = document.getElementById('p-holoo-new');
        const issueBtn = document.getElementById('p-holoo-issue');
        if (!box || !fresh) return;
        box.classList.add('hidden');
        fresh.classList.remove('hidden');
        // پیش‌فرض پنهان است؛ شاخهٔ «محصولِ بی‌کد» خودش نشانش می‌دهد
        if (issueBtn) issueBtn.classList.add('hidden');

        const select = document.getElementById('p-holoo-select');
        select.innerHTML = '';
        const first = document.createElement('option');
        first.value = '';
        first.textContent = 'کدِ تازه صادر شود';
        select.appendChild(first);

        try {
            const res = await axios.get(`${getApi()}/v1/holoo/codes/reserved`);
            (res.data || []).forEach(c => {
                const o = document.createElement('option');
                o.value = c.code;
                // textContent نه innerHTML: یادداشت را کاربر نوشته
                o.textContent = c.code + ' — ' + (c.note || 'بدونِ یادداشت');
                select.appendChild(o);
            });
        } catch (e) {
            // نبودنِ فهرستِ رزرو نباید جلویِ ساختِ محصول را بگیرد؛ کدِ تازه صادر می‌شود
        }
    }

    function copyHolooCode() {
        const text = document.getElementById('p-holoo-code-text').textContent;
        if (!text) return;
        navigator.clipboard.writeText(text.trim()).catch(() => {});
    }

    /**
     * صدورِ کد برای محصولِ باز در فرم.
     * ⚠️ بعد از موفقیت، هم جعبه به حالتِ نمایش می‌رود و هم نسخهٔ درون‌حافظه‌ای محصول
     * به‌روز می‌شود — وگرنه بازکردنِ دوبارهٔ همان فرم باز «کد ندارد» نشان می‌داد.
     */
    async function issueHolooCode() {
        const btn = document.getElementById('p-holoo-issue');
        if (!btn) return;
        const productId = btn.dataset.productId;
        if (!productId) return;

        const select = document.getElementById('p-holoo-select');
        const picked = select ? select.value : '';

        btn.disabled = true;
        btn.textContent = 'در حال صدور…';
        try {
            const res = await axios.post(`${getApi()}/v1/holoo/codes/assign`,
                    {productId, code: picked || null});
            const code = res.data && res.data.holooCode;
            const local = products.find(x => x.id === productId);
            if (local) local.holooCode = code;
            showHolooForExistingProduct(code, productId);
        } catch (err) {
            btn.disabled = false;
            btn.textContent = 'صدورِ کدِ هلو';
            // ⚠️ عمداً serverError صدا زده نمی‌شود: آن تابع فقط در Admin.html تعریف شده
            // و همین فایل در پنلِ فروش هم بار می‌شود. همان الگویِ saveProduct.
            Swal.fire('خطا', (err.response && err.response.data
                    && (err.response.data.message || err.response.data)) || 'صدورِ کد ناموفق بود', 'error');
        }
    }

    document.addEventListener('click', e => {
        if (!e.target) return;
        if (e.target.id === 'p-holoo-copy') copyHolooCode();
        if (e.target.id === 'p-holoo-issue') issueHolooCode();
    });

    window.ProductsTab = { mount, refresh, getProducts: () => products };

    // onclickهایِ داخلِ مارک‌آپ سراسری‌اند، پس این‌ها روی window قرار می‌گیرند.
    // faqRowHtml/renderFaqRows/addFaqRow عمداً هم اینجایند: تبِ دوره‌هایِ Admin.html
    // (renderCourseFaqRows) همین‌ها را دوباره استفاده می‌کند.
    Object.assign(window, {
        togglePackInput, applyProductFilters, resetProductFilters,
        openProductModal, editProduct, saveProduct,
        addTechSpecRow, addFaqRow, faqRowHtml, renderFaqRows, collectFaqs,
        filterRelProducts, filterReplacementProducts, toggleDiscontinuedFields,
        handleImageSelect, addImageByUrl,
        makeMainImage, removeImage, generateDescriptionWithAI,
        deleteProductWithRedirect
    });
})();
