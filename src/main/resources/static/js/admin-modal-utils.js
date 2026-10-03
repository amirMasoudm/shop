/**
 * سیستمِ مودالِ عمومیِ پنل‌های ادمینی — درگ، ریسایز، بستنِ بک‌دراپ، و چکِ
 * «تغییراتِ ذخیره‌نشده» قبل از بستن. عمداً یک فایلِ مشترک است (نه کدِ تکراری در
 * Admin.html و SalesPanel.html) چون هر دو پنل به همین موتور برای مودال‌هایِ
 * خودشان نیاز دارند (مثلاً productModal که حالا هر دو پنل مشترک استفاده می‌کنند).
 *
 * وابستگی: هیچ (فقط DOM خامِ خودِ صفحه).
 *
 * میزبان باید مجموعه‌ی window.FORM_MODALS را قبل از استفاده تعریف کند (شناسه‌ی
 * مودال‌هایی که «فرم»اند و باید قبل از بستنِ بدونِ ذخیره هشدار بدهند)؛ اگر تعریف
 * نشده باشد، این فایل با یک Set خالی می‌سازدش.
 */
(function () {
    if (!window.FORM_MODALS) window.FORM_MODALS = new Set();
    const FORM_MODALS = window.FORM_MODALS;
    const _modalSnap = {};

    function resetForm(p) {
        document.querySelectorAll(`[id^="${p}-"]`).forEach(e => {
            if (e.tagName === 'INPUT' || e.tagName === 'TEXTAREA' || e.tagName === 'SELECT') e.value = '';
        });
    }

    function snapshotModalForm(id) {
        const modal = document.getElementById(id);
        if (!modal) return;
        const snap = {};
        modal.querySelectorAll('input:not([type=hidden]), textarea, select').forEach(el => {
            const key = el.id || el.name;
            if (key) snap[key] = el.type === 'checkbox' ? String(el.checked) : el.value;
        });
        _modalSnap[id] = snap;
    }

    function isModalDirty(id) {
        const snap = _modalSnap[id];
        if (!snap) return false;
        const modal = document.getElementById(id);
        for (const el of modal.querySelectorAll('input:not([type=hidden]), textarea, select')) {
            const key = el.id || el.name;
            if (!key || snap[key] === undefined) continue;
            const cur = el.type === 'checkbox' ? String(el.checked) : el.value;
            if (cur !== snap[key]) return true;
        }
        return false;
    }

    function closeModal(id) {
        document.getElementById(id).classList.add('hidden');
    }

    function closeModalSafe(id) {
        if (FORM_MODALS.has(id) && isModalDirty(id)) {
            if (!confirm('تغییراتی ذخیره نشده وجود دارد.\nآیا می‌خواهید بدون ذخیره ببندید؟')) return;
        }
        closeModal(id);
    }

    function enableModalBackdrop(id) {
        const modal = document.getElementById(id);
        if (!modal || modal.dataset.backdropBound) return;
        modal.dataset.backdropBound = '1';
        modal.addEventListener('mousedown', (e) => {
            if (e.target !== modal) return;
            FORM_MODALS.has(id) ? closeModalSafe(id) : closeModal(id);
        });
    }

    function enableModalResize(id) {
        const modal = document.getElementById(id);
        if (!modal) return;
        const box = modal.querySelector(':scope > div');
        if (!box || box.dataset.resizeBound) return;
        box.dataset.resizeBound = '1';
        box.style.position = 'relative';
        const rh = document.createElement('div');
        rh.setAttribute('data-resize-handle', '');
        rh.style.cssText = 'position:absolute;bottom:0;right:0;width:18px;height:18px;cursor:se-resize;z-index:5;border-radius:0 0 14px 0;';
        rh.innerHTML = '<svg style="position:absolute;bottom:3px;right:3px" width="12" height="12" viewBox="0 0 12 12" fill="none"><path d="M11 1L1 11M11 6L6 11" stroke="#d1d5db" stroke-width="1.5" stroke-linecap="round"/></svg>';
        box.appendChild(rh);
        let resizing = false, rsx = 0, rsy = 0, rsw = 0, rsh = 0;
        rh.addEventListener('mousedown', (e) => {
            resizing = true;
            rsx = e.clientX; rsy = e.clientY;
            rsw = box.offsetWidth; rsh = box.offsetHeight;
            document.body.style.userSelect = 'none';
            e.preventDefault(); e.stopPropagation();
        });
        window.addEventListener('mousemove', (e) => {
            if (!resizing) return;
            const nw = Math.max(320, Math.min(window.innerWidth - 32, rsw + e.clientX - rsx));
            const nh = Math.max(200, Math.min(window.innerHeight - 32, rsh + e.clientY - rsy));
            box.style.width = nw + 'px';
            box.style.height = nh + 'px';
            box.style.maxWidth = 'none';
            box.style.maxHeight = 'none';
        });
        window.addEventListener('mouseup', () => { resizing = false; document.body.style.userSelect = ''; });
    }

    // امکانِ کشیدن/جابه‌جاییِ مودال روی صفحه با گرفتنِ نوارِ عنوان ([data-drag-handle])
    function enableModalDrag(id) {
        const modal = document.getElementById(id);
        if (!modal) return;
        const box = modal.querySelector(':scope > div');
        if (!box) return;
        // max-height اولیه را یک‌بار ذخیره کن تا reset پاکش نکند
        if (box.dataset.origMaxH === undefined) box.dataset.origMaxH = box.style.maxHeight;
        // هر بار که مودال باز می‌شود به مرکز برگردد و سایزِ قبلی ریست شود
        box.style.transform = '';
        box.dataset.dx = '0';
        box.dataset.dy = '0';
        box.style.width = '';
        box.style.height = '';
        box.style.maxWidth = '';
        box.style.maxHeight = box.dataset.origMaxH;
        const handle = box.querySelector('[data-drag-handle]');
        if (!handle || box.dataset.dragBound === '1') return; // فقط یک‌بار bind شود
        box.dataset.dragBound = '1';
        handle.style.cursor = 'move';
        let sx = 0, sy = 0, dragging = false;
        handle.addEventListener('mousedown', (e) => {
            if (e.target.closest('button')) return; // کلیک روی ✕/دکمه‌ها نباید کشیدن را شروع کند
            dragging = true; sx = e.clientX; sy = e.clientY;
            document.body.style.userSelect = 'none';
            e.preventDefault();
        });
        window.addEventListener('mousemove', (e) => {
            if (!dragging) return;
            const dx = parseFloat(box.dataset.dx || '0') + (e.clientX - sx);
            const dy = parseFloat(box.dataset.dy || '0') + (e.clientY - sy);
            box.style.transform = `translate(${dx}px, ${dy}px)`;
        });
        window.addEventListener('mouseup', (e) => {
            if (!dragging) return;
            dragging = false;
            box.dataset.dx = String(parseFloat(box.dataset.dx || '0') + (e.clientX - sx));
            box.dataset.dy = String(parseFloat(box.dataset.dy || '0') + (e.clientY - sy));
            document.body.style.userSelect = '';
        });
    }

    function openModal(id) {
        document.getElementById(id).classList.remove('hidden');
        enableModalDrag(id);
        enableModalBackdrop(id);
        enableModalResize(id);
        if (FORM_MODALS.has(id)) snapshotModalForm(id);
    }

    Object.assign(window, {
        resetForm, closeModal, closeModalSafe, openModal,
        enableModalDrag, enableModalBackdrop, enableModalResize,
        snapshotModalForm, isModalDirty
    });
})();
