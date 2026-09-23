/**
 * تبِ «پشتیبانی مشتریان» — سمتِ کارشناس.
 *
 * دو بخش دارد: صفِ مشترکِ تصاحب‌نشده، و «چت‌های من». پنجرهٔ گفت‌وگو از
 * /js/chat-core.js می‌آید — همان ماژولی که حبابِ مشتری هم استفاده می‌کند، تا
 * ریپلای/آپلود/تایپ/تیکِ خوانده‌شده یک‌جا نگه‌داری شود نه دو جا.
 *
 * مثلِ products-tab.js یک ماژولِ مستقل است تا اگر روزی پنلِ ادمین هم همین تب را
 * خواست، فقط mount شود.
 *
 * وابستگی‌ها (window): ChatCore، و اختیاری Swal برایِ پیامِ خطا.
 */
(function () {
    const API = '/api/v1/chat';

    let host = null;
    let selfId = null;
    let mounted = false;
    let activeConversationId = null;
    // ⚠️ خودِ سند هم نگه داشته می‌شود نه فقط شناسه: دکمه‌هایِ ارجاع/انصراف باید
    // بدانند گفت‌وگو دستِ کیست، و دوباره‌پرسیدنش از سرور بی‌مورد بود.
    let activeConversation = null;

    async function mount(hostEl, opts) {
        opts = opts || {};
        host = hostEl;

        if (!mounted) {
            // شناسهٔ خودِ کارشناس را از خودِ APIِ چت می‌گیریم و نه از میزبان:
            // /api/users/api/current-user عمداً id برنمی‌گرداند و دست‌زدن به آن
            // اندپوینتِ پرمصرف فقط برایِ این تب، ریسکِ بی‌موردی بود.
            try {
                const session = await fetchJson(`${API}/session`);
                selfId = session.userId || null;
            } catch (e) { /* بدونِ selfId هم رندر بر اساسِ senderRole کار می‌کند */ }

            host.innerHTML = `
                <div class="flex flex-col md:flex-row gap-4" style="height:calc(100vh - 220px);min-height:460px;">
                    <aside class="w-full md:w-80 bg-white rounded-xl shadow-sm border flex flex-col overflow-hidden shrink-0">
                        <div class="p-3 border-b bg-gray-50">
                            <div class="font-bold text-sm text-gray-800">درخواست‌های پشتیبانی</div>
                            <div class="text-[11px] text-gray-500">تصاحب‌نشده — هر کارشناسی بازش کند مالِ او می‌شود</div>
                        </div>
                        <div data-queue class="overflow-y-auto" style="max-height:38%;"></div>
                        <div class="p-3 border-t border-b bg-gray-50">
                            <div class="font-bold text-sm text-gray-800">چت‌های من</div>
                        </div>
                        <div data-mine class="overflow-y-auto flex-1"></div>
                    </aside>

                    <section class="flex-1 bg-white rounded-xl shadow-sm border overflow-hidden flex flex-col min-h-0">
                        <div class="p-3 border-b bg-gray-50 flex items-center justify-between gap-2">
                            <div data-header class="text-sm font-bold text-gray-800 truncate">
                                یک گفت‌وگو را انتخاب کنید
                            </div>
                            <div data-actions class="flex items-center gap-2 shrink-0"></div>
                        </div>
                        <div data-thread class="flex-1 min-h-0"></div>
                    </section>
                </div>`;

            ChatCore.mountThread(host.querySelector('[data-thread]'), {isAgent: true, selfId: selfId});
            ChatCore.setComposerEnabled(false, 'برای پاسخ‌دادن، یک گفت‌وگو را باز کنید.');
            ChatCore.connect();
            ChatCore.requestNotificationPermission();

            // صف با رویدادِ زنده تازه می‌شود، نه با polling.
            ChatCore.onEvent(payload => {
                if (!payload) return;
                if (payload.event === 'queue' || payload.event === 'message') refresh();
            });

            mounted = true;
        }
        await refresh();
    }

    async function refresh() {
        try {
            const [queue, mine] = await Promise.all([
                fetchJson(`${API}/conversations/queue`),
                fetchJson(`${API}/conversations/mine`)
            ]);
            renderList(host.querySelector('[data-queue]'), queue, true);
            renderList(host.querySelector('[data-mine]'), mine, false);
        } catch (e) { /* خطای شبکه نباید تب را خالی کند */ }
    }

    function renderList(box, conversations, isQueue) {
        box.innerHTML = '';
        if (!conversations.length) {
            const empty = document.createElement('div');
            empty.className = 'p-4 text-center text-xs text-gray-400';
            empty.textContent = isQueue ? 'درخواستِ تازه‌ای نیست.' : 'هنوز گفت‌وگویی برنداشته‌اید.';
            box.appendChild(empty);
            return;
        }
        conversations.forEach(c => box.appendChild(buildRow(c, isQueue)));
    }

    function buildRow(conversation, isQueue) {
        const row = document.createElement('div');
        row.className = 'p-3 border-b cursor-pointer hover:bg-indigo-50 transition';
        if (conversation.id === activeConversationId) row.classList.add('bg-indigo-50');

        const top = document.createElement('div');
        top.className = 'flex items-center justify-between gap-2';

        const name = document.createElement('div');
        name.className = 'font-bold text-xs text-gray-800 truncate';
        name.textContent = conversation.customerName || 'مشتری';   // ← داده است، پس textContent
        top.appendChild(name);

        if (!isQueue && conversation.unreadForAgent > 0) {
            const badge = document.createElement('span');
            badge.className = 'bg-red-500 text-white text-[10px] font-bold rounded-full px-2 py-0.5 shrink-0';
            badge.textContent = conversation.unreadForAgent;
            top.appendChild(badge);
        }
        row.appendChild(top);

        const preview = document.createElement('div');
        preview.className = 'text-[11px] text-gray-500 truncate mt-1';
        preview.textContent = conversation.lastMessagePreview || '—';
        row.appendChild(preview);

        const meta = document.createElement('div');
        meta.className = 'text-[10px] text-gray-400 mt-1';
        meta.textContent = isQueue
            ? 'در انتظار: ' + waitedFor(conversation.lastMessageAt)
            : formatWhen(conversation.lastMessageAt);
        row.appendChild(meta);

        row.addEventListener('click', () => isQueue ? claim(conversation) : open(conversation));
        return row;
    }

    /**
     * تصاحب. شرطِ مسابقه سمتِ سرور با findAndModify حل شده؛ کارِ اینجا فقط این است
     * که بازندهٔ مسابقه پیامِ روشن ببیند و صفش تازه شود، نه یک خطای مبهم.
     */
    async function claim(conversation) {
        try {
            const claimed = await postJson(`${API}/conversations/${encodeURIComponent(conversation.id)}/claim`);
            await refresh();
            open(claimed);
        } catch (err) {
            const message = (err && err.message) || 'تصاحبِ گفت‌وگو ناموفق بود';
            if (window.Swal) Swal.fire('برداشته شده', message, 'info');
            else alert(message);
            await refresh();
        }
    }

    async function open(conversation) {
        activeConversation = conversation;
        activeConversationId = conversation.id;
        host.querySelector('[data-header]').textContent = conversation.customerName || 'مشتری';
        renderActions();
        ChatCore.setComposerEnabled(true);
        await ChatCore.openConversation(conversation.id);
        await refresh();
    }

    /** بعد از ارجاع یا انصراف، گفت‌وگو دیگر مالِ ما نیست؛ پنجره باید خالی شود. */
    function clearActive() {
        activeConversation = null;
        activeConversationId = null;
        host.querySelector('[data-header]').textContent = 'یک گفت‌وگو را انتخاب کنید';
        renderActions();
        ChatCore.closeConversation();
        ChatCore.setComposerEnabled(false, 'برای پاسخ‌دادن، یک گفت‌وگو را باز کنید.');
    }

    // ==========================================================
    // ارجاع و انصراف
    // ==========================================================

    /**
     * دکمه‌ها فقط وقتی معنی دارند که گفت‌وگو دستِ خودمان باشد. روی ردیفِ صف
     * (هنوز برداشته‌نشده) کاری ندارند و سرور هم ۴۰۹ می‌دهد — پس اصلاً ساخته نمی‌شوند.
     */
    function renderActions() {
        const box = host.querySelector('[data-actions]');
        box.innerHTML = '';
        if (!activeConversation || !activeConversation.assignedAgentId) return;
        if (selfId && activeConversation.assignedAgentId !== selfId) return;
        box.appendChild(actionButton('ارجاع به همکار',
                'bg-indigo-600 text-white hover:bg-indigo-700', transferActive));
        box.appendChild(actionButton('انصراف از برداشت',
                'bg-white text-gray-700 border hover:bg-gray-100', releaseActive));
    }

    function actionButton(label, classes, onClick) {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'text-[11px] font-bold rounded-lg px-3 py-1.5 transition ' + classes;
        button.textContent = label;
        button.addEventListener('click', onClick);
        return button;
    }

    /** شمارِ پیام‌های بی‌پاسخ. نبودنش نباید جلوی ارجاع را بگیرد، فقط پرسش را کم‌دقت‌تر می‌کند. */
    async function unansweredInfo(conversationId) {
        try {
            return await fetchJson(
                `${API}/conversations/${encodeURIComponent(conversationId)}/unanswered`);
        } catch (e) {
            return {total: 0, seen: 0};
        }
    }

    /**
     * پرسشِ مشترکِ پیش از ارجاع و انصراف.
     * <p>
     * خواستهٔ مالک این بود که <b>قبل از هر دو</b> پرسیده شود پیام‌هایِ بی‌پاسخِ مشتری
     * به حالتِ نخوانده برگردند یا نه. هر دو مسیر عمداً از همین یک تابع رد می‌شوند تا
     * امکان نداشته باشد یکی‌شان روزی این پرسش را جا بیندازد.
     */
    async function askHandover(opts) {
        if (!window.Swal) {
            alert('این بخش به پنجرهٔ گفت‌وگوی پنل نیاز دارد؛ صفحه را دوباره بارگذاری کنید.');
            return null;
        }
        const res = await Swal.fire({
            title: opts.title,
            html: (opts.bodyHtml || '') + unreadQuestionHtml(opts.info),
            icon: 'question',
            showCancelButton: true,
            confirmButtonText: opts.confirmText,
            cancelButtonText: 'بی‌خیال',
            reverseButtons: true,
            focusConfirm: false,
            preConfirm: () => {
                const select = document.getElementById('swal-agent');
                if (opts.needsAgent && (!select || !select.value)) {
                    Swal.showValidationMessage('کارشناسِ مقصد را انتخاب کنید');
                    return false;
                }
                const box = document.getElementById('swal-restore');
                return {
                    toAgentId: select ? select.value : null,
                    restoreUnread: !!(box && box.checked)
                };
            }
        });
        return res.isConfirmed ? res.value : null;
    }

    function unreadQuestionHtml(info) {
        const total = (info && info.total) || 0;
        if (!total) {
            return '<div style="margin-top:14px;font-size:12px;color:#6b7280;">'
                 + 'پیامِ بی‌پاسخی از مشتری نمانده است.</div>';
        }
        const seen = (info && info.seen) || 0;
        const seenNote = seen ? ' ' + seen + ' تای آن‌ها را سین کرده‌اید.' : '';
        return '<label style="display:flex;gap:8px;align-items:flex-start;margin-top:14px;padding:10px;'
             + 'text-align:right;background:#fff7ed;border:1px solid #fed7aa;border-radius:10px;cursor:pointer;">'
             + '<input type="checkbox" id="swal-restore" checked style="margin-top:4px;">'
             + '<span style="font-size:12px;line-height:2;color:#7c2d12;"><b>'
             + total + ' پیامِ مشتری بی‌پاسخ مانده.</b>' + seenNote
             + ' به حالتِ «نخوانده» برگردند تا کارشناسِ بعدی ببیندشان؟</span></label>';
    }

    function agentSelectHtml(agents) {
        const options = agents.map(a =>
            '<option value="' + escapeHtml(a.id) + '">' + escapeHtml(a.name) + '</option>').join('');
        return '<div style="text-align:right;">'
             + '<div style="font-size:12px;color:#374151;margin-bottom:6px;">گفت‌وگو به کدام همکار برود؟</div>'
             + '<select id="swal-agent" class="swal2-select" style="width:100%;margin:0;">'
             + '<option value="">— انتخاب کنید —</option>' + options + '</select></div>';
    }

    async function transferActive() {
        const conversation = activeConversation;
        if (!conversation) return;

        let agents = [];
        try {
            agents = await fetchJson(`${API}/agents`);
        } catch (e) {
            return showError('فهرستِ کارشناسان گرفته نشد');
        }
        if (!agents.length) {
            return showInfo('ارجاع ممکن نیست', 'کارشناسِ دیگری برای ارجاع ثبت نشده است.', 'info');
        }

        const choice = await askHandover({
            title: 'ارجاع به همکار',
            bodyHtml: agentSelectHtml(agents),
            confirmText: 'ارجاع بده',
            needsAgent: true,
            info: await unansweredInfo(conversation.id)
        });
        if (!choice) return;

        try {
            const result = await postJson(
                `${API}/conversations/${encodeURIComponent(conversation.id)}/transfer`,
                {toAgentId: choice.toAgentId, restoreUnread: choice.restoreUnread});
            clearActive();
            await refresh();
            showInfo('ارجاع شد',
                'گفت‌وگو به ' + (result.assignedAgentName || 'همکار') + ' سپرده شد.'
                + restoredNote(result.restoredUnread), 'success');
        } catch (err) {
            showError((err && err.message) || 'ارجاع ناموفق بود');
            await refresh();
        }
    }

    async function releaseActive() {
        const conversation = activeConversation;
        if (!conversation) return;

        const choice = await askHandover({
            title: 'انصراف از برداشت',
            bodyHtml: '<div style="text-align:right;font-size:12px;color:#374151;line-height:2;">'
                    + 'گفت‌وگو به صفِ مشترک برمی‌گردد و هر کارشناسی می‌تواند برش دارد.</div>',
            confirmText: 'بله، به صف برگردد',
            needsAgent: false,
            info: await unansweredInfo(conversation.id)
        });
        if (!choice) return;

        try {
            const result = await postJson(
                `${API}/conversations/${encodeURIComponent(conversation.id)}/release`,
                {restoreUnread: choice.restoreUnread});
            clearActive();
            await refresh();
            showInfo('به صف برگشت',
                'گفت‌وگو دوباره در صفِ مشترک است.' + restoredNote(result.restoredUnread), 'success');
        } catch (err) {
            showError((err && err.message) || 'انصراف ناموفق بود');
            await refresh();
        }
    }

    function restoredNote(count) {
        return count > 0 ? ' ' + count + ' پیام به حالتِ نخوانده برگشت.' : '';
    }

    function showInfo(title, text, icon) {
        if (window.Swal) Swal.fire(title, text, icon || 'info');
        else alert(title + '\n' + text);
    }

    function showError(message) {
        if (window.Swal) Swal.fire('خطا', message, 'error');
        else alert(message);
    }

    // دادهٔ سرور است نه ورودیِ مشتری، ولی چون داخلِ innerHTMLِ مودال می‌نشیند
    // همان‌جا هم فرار داده می‌شود — نامِ کارکنان از پنلِ ادمین قابلِ تغییر است.
    function escapeHtml(value) {
        return String(value == null ? '' : value)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    function waitedFor(iso) {
        if (!iso) return '—';
        const minutes = Math.max(0, Math.round((Date.now() - new Date(iso).getTime()) / 60000));
        if (minutes < 1) return 'همین الان';
        if (minutes < 60) return minutes + ' دقیقه';
        return Math.round(minutes / 60) + ' ساعت';
    }

    function formatWhen(iso) {
        if (!iso) return '';
        try { return new Date(iso).toLocaleString('fa-IR', {hour: '2-digit', minute: '2-digit', day: 'numeric', month: 'numeric'}); }
        catch (e) { return ''; }
    }

    async function fetchJson(url) {
        const res = await fetch(url, {credentials: 'include'});
        if (!res.ok) throw new Error(await res.text());
        return res.json();
    }

    async function postJson(url, body) {
        // توکنِ CSRF خودمان فرستاده می‌شود و به رَپرِ fetchِ میزبان تکیه نمی‌کنیم —
        // همان دلیلی که در chat-core.js توضیح داده شده.
        const m = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
        const headers = m ? {'X-XSRF-TOKEN': decodeURIComponent(m[1])} : {};
        const init = {method: 'POST', credentials: 'include', headers: headers};
        if (body !== undefined) {
            headers['Content-Type'] = 'application/json';
            init.body = JSON.stringify(body);
        }
        const res = await fetch(url, init);
        if (!res.ok) throw new Error(await res.text());
        return res.json();
    }

    window.SupportTab = {mount, refresh};
})();
