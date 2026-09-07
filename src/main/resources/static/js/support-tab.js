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
                        <div data-header class="p-3 border-b bg-gray-50 text-sm font-bold text-gray-800">
                            یک گفت‌وگو را انتخاب کنید
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
        activeConversationId = conversation.id;
        const header = host.querySelector('[data-header]');
        header.textContent = conversation.customerName || 'مشتری';
        ChatCore.setComposerEnabled(true);
        await ChatCore.openConversation(conversation.id);
        await refresh();
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

    async function postJson(url) {
        const res = await fetch(url, {method: 'POST', credentials: 'include'});
        if (!res.ok) throw new Error(await res.text());
        return res.json();
    }

    window.SupportTab = {mount, refresh};
})();
