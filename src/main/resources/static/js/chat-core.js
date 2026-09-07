/**
 * هستهٔ مشترکِ چتِ پشتیبانی — بینِ پنلِ کارشناس (SalesPanel.html) و حبابِ مشتری.
 *
 * عمداً یک فایلِ مشترک است، هم‌سو با همان تصمیمی که برایِ products-tab.js گرفته شد:
 * پنجرهٔ گفت‌وگو (فهرستِ پیام، ریپلای، آپلود، در‌حال‌تایپ، تیکِ خوانده‌شده) در هر دو
 * سمت دقیقاً یکی است و اگر کپی می‌شد، هر اصلاحِ بعدی باید دو جا انجام می‌گرفت.
 *
 * دو قاعدهٔ اساسی که در کلِ این فایل رعایت شده:
 *
 *  ۱) 🔴 <b>هیچ متنی از سرور با innerHTML رندر نمی‌شود.</b> متنِ پیام همیشه با
 *     textContent می‌نشیند. پیامِ مشتری در پنلِ داخلیِ کارشناس نمایش داده می‌شود،
 *     پس XSSِ سمتِ مشتری مستقیم به پنل می‌رسد — این جدی‌ترین سطحِ حملهٔ این فیچر
 *     است. سرور هم تگ‌ها را می‌زداید؛ این لایهٔ دوم است، نه تنها لایه.
 *
 *  ۲) <b>وب‌سوکت فقط شتاب‌دهنده است، نه تحویلِ تضمینی.</b> منبعِ حقیقت دیتابیس است:
 *     بعد از هر وصلِ دوباره، جاماندهٔ پیام‌ها با
 *     GET /api/v1/chat/messages?after=<lastId> گرفته می‌شود.
 */
(function () {
    const API = '/api/v1/chat';
    const PAGE_SIZE = 40;

    // ---- وضعیتِ ماژول (خصوصی) ----
    let socket = null;
    let reconnectAttempt = 0;
    let reconnectTimer = null;
    let manuallyClosed = false;

    let host = null;              // المانِ میزبانِ پنجرهٔ گفت‌وگو
    let conversationId = null;
    let isAgent = false;
    let selfId = null;
    let composerEnabled = true;
    let replyTo = null;           // {id, body, senderName}
    let oldestLoadedId = null;
    let newestLoadedId = null;
    let loadingOlder = false;
    let reachedTop = false;
    let typingHideTimer = null;
    let lastTypingSentAt = 0;

    const listeners = [];

    // ==========================================================
    // اتصال
    // ==========================================================

    /**
     * اتصالِ وب‌سوکت با تلاشِ دوبارهٔ نمایی.
     * سقفِ ۳۰ ثانیه دارد تا تبِ بازِ یک کاربرِ آفلاین، سرور را با تلاشِ پی‌درپی نکوبد.
     */
    function connect() {
        manuallyClosed = false;
        if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) return;

        const proto = location.protocol === 'https:' ? 'wss:' : 'ws:';
        try {
            socket = new WebSocket(proto + '//' + location.host + '/ws/chat');
        } catch (e) {
            scheduleReconnect();
            return;
        }

        socket.onopen = () => {
            reconnectAttempt = 0;
            if (conversationId) {
                subscribe(conversationId);
                // جاماندهٔ زمانِ قطعی — به سوکت برای تحویل تکیه نمی‌کنیم
                catchUp();
            }
            emit({event: 'connection', state: 'online'});
        };

        socket.onmessage = (ev) => {
            let payload;
            try { payload = JSON.parse(ev.data); } catch (e) { return; }
            handleEvent(payload);
            emit(payload);
        };

        socket.onclose = () => {
            emit({event: 'connection', state: 'offline'});
            if (!manuallyClosed) scheduleReconnect();
        };

        socket.onerror = () => { try { socket.close(); } catch (e) {} };
    }

    function scheduleReconnect() {
        clearTimeout(reconnectTimer);
        const delay = Math.min(30000, 1000 * Math.pow(2, reconnectAttempt++));
        reconnectTimer = setTimeout(connect, delay);
    }

    function disconnect() {
        manuallyClosed = true;
        clearTimeout(reconnectTimer);
        if (socket) { try { socket.close(); } catch (e) {} }
        socket = null;
    }

    function socketSend(obj) {
        if (socket && socket.readyState === WebSocket.OPEN) {
            socket.send(JSON.stringify(obj));
        }
    }

    function subscribe(id) {
        socketSend({action: 'subscribe', conversationId: id});
    }

    function onEvent(handler) {
        listeners.push(handler);
    }

    function emit(payload) {
        listeners.forEach(fn => { try { fn(payload); } catch (e) {} });
    }

    // ==========================================================
    // رویدادهایِ زنده
    // ==========================================================

    function handleEvent(payload) {
        if (!payload || payload.conversationId !== conversationId) return;

        if (payload.event === 'message') {
            appendMessage(payload.message, true);
            newestLoadedId = payload.message.id;
            maybeNotify(payload.message);
            // پیامِ طرفِ مقابل وقتی پنجره باز و دیده‌شدنی است یعنی خوانده شده
            if (!document.hidden && !isOwn(payload.message)) markRead();
        } else if (payload.event === 'typing') {
            if (payload.senderId !== selfId) showTyping(payload.senderName);
        } else if (payload.event === 'read') {
            const readByOther = isAgent ? payload.by === 'CUSTOMER' : payload.by === 'AGENT';
            if (readByOther) markOwnMessagesRead();
        }
    }

    // ==========================================================
    // پنجرهٔ گفت‌وگو
    // ==========================================================

    /**
     * ساختِ اسکلتِ پنجرهٔ گفت‌وگو داخلِ المانِ میزبان.
     * فقط اسکلت با innerHTML ساخته می‌شود (رشتهٔ ثابتِ خودمان)؛ هیچ دادهٔ کاربری
     * از اینجا رد نمی‌شود.
     */
    function mountThread(hostEl, opts) {
        opts = opts || {};
        host = hostEl;
        isAgent = !!opts.isAgent;
        selfId = opts.selfId || null;

        host.innerHTML = `
            <div class="chat-thread" style="display:flex;flex-direction:column;height:100%;min-height:0;">
                <div class="chat-typing" style="display:none;padding:2px 10px;font-size:11px;color:#6b7280;"></div>
                <div class="chat-messages" style="flex:1;overflow-y:auto;padding:10px;display:flex;flex-direction:column;gap:6px;min-height:0;"></div>
                <div class="chat-reply-bar" style="display:none;padding:6px 10px;background:#eef2ff;border-top:1px solid #c7d2fe;font-size:12px;">
                    <span class="chat-reply-text" style="color:#3730a3;"></span>
                    <button type="button" class="chat-reply-cancel" style="float:left;border:0;background:transparent;cursor:pointer;color:#4338ca;font-weight:bold;">✕</button>
                </div>
                <div class="chat-locked" style="display:none;padding:10px;background:#fef3c7;color:#92400e;font-size:12px;text-align:center;"></div>
                <div class="chat-composer" style="display:flex;gap:6px;padding:8px;border-top:1px solid #e5e7eb;align-items:flex-end;">
                    <button type="button" class="chat-attach" title="پیوستِ فایل"
                            style="border:1px solid #d1d5db;background:#fff;border-radius:8px;padding:6px 9px;cursor:pointer;">📎</button>
                    <input type="file" class="chat-file" style="display:none;">
                    <textarea class="chat-input" rows="1" placeholder="پیام خود را بنویسید…"
                              style="flex:1;resize:none;max-height:110px;border:1px solid #d1d5db;border-radius:8px;padding:8px;font-family:inherit;font-size:13px;outline:none;"></textarea>
                    <button type="button" class="chat-send"
                            style="background:#4338ca;color:#fff;border:0;border-radius:8px;padding:8px 14px;cursor:pointer;font-weight:bold;">ارسال</button>
                </div>
            </div>`;

        el('.chat-send').addEventListener('click', sendCurrent);
        el('.chat-attach').addEventListener('click', () => el('.chat-file').click());
        el('.chat-file').addEventListener('change', onFilePicked);
        el('.chat-reply-cancel').addEventListener('click', () => setReply(null));

        const input = el('.chat-input');
        input.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); sendCurrent(); }
        });
        input.addEventListener('input', () => {
            input.style.height = 'auto';
            input.style.height = Math.min(110, input.scrollHeight) + 'px';
            notifyTyping();
        });

        el('.chat-messages').addEventListener('scroll', function () {
            if (this.scrollTop <= 4) loadOlder();
        });

        document.addEventListener('visibilitychange', () => {
            if (!document.hidden && conversationId) markRead();
        });
    }

    /** بازکردنِ یک گفت‌وگو: تاریخچه + عضویت در اتاقِ زنده. */
    async function openConversation(id) {
        if (!id) throw new Error('شناسهٔ گفت‌وگو داده نشده است');
        conversationId = id;
        oldestLoadedId = null;
        newestLoadedId = null;
        reachedTop = false;
        setReply(null);
        if (host) el('.chat-messages').innerHTML = '';

        const messages = await fetchJson(`${API}/messages?conversationId=${encodeURIComponent(id)}&limit=${PAGE_SIZE}`);
        messages.forEach(m => appendMessage(m, false));
        if (messages.length) {
            oldestLoadedId = messages[0].id;
            newestLoadedId = messages[messages.length - 1].id;
        }
        scrollToBottom();
        subscribe(id);
        markRead();
    }

    /** اسکرول به بالا = صفحهٔ قدیمی‌تر. */
    async function loadOlder() {
        if (loadingOlder || reachedTop || !conversationId || !oldestLoadedId) return;
        loadingOlder = true;
        try {
            const box = el('.chat-messages');
            const previousHeight = box.scrollHeight;
            const older = await fetchJson(
                `${API}/messages?conversationId=${encodeURIComponent(conversationId)}&before=${encodeURIComponent(oldestLoadedId)}&limit=${PAGE_SIZE}`);
            if (!older.length) { reachedTop = true; return; }
            // معکوس، چون هرکدام باید بالایِ قبلی بنشیند
            older.slice().reverse().forEach(m => prependMessage(m));
            oldestLoadedId = older[0].id;
            // موقعیتِ دید حفظ شود، وگرنه با هر صفحه کاربر پرت می‌شود
            box.scrollTop = box.scrollHeight - previousHeight;
        } finally {
            loadingOlder = false;
        }
    }

    /** جبرانِ قطعی: هرچه بعد از آخرین پیامِ دیده‌شده آمده. */
    async function catchUp() {
        if (!conversationId) return;
        const url = newestLoadedId
            ? `${API}/messages?conversationId=${encodeURIComponent(conversationId)}&after=${encodeURIComponent(newestLoadedId)}`
            : `${API}/messages?conversationId=${encodeURIComponent(conversationId)}&limit=${PAGE_SIZE}`;
        const missed = await fetchJson(url);
        missed.forEach(m => {
            if (!document.getElementById('chat-msg-' + m.id)) {
                appendMessage(m, true);
                newestLoadedId = m.id;
            }
        });
    }

    // ==========================================================
    // رندرِ پیام
    // ==========================================================

    function isOwn(message) {
        if (message.senderRole === 'SYSTEM') return false;
        return selfId ? message.senderId === selfId
            : (isAgent ? message.senderRole === 'AGENT' : message.senderRole === 'CUSTOMER');
    }

    function buildMessage(message) {
        const wrap = document.createElement('div');
        wrap.id = 'chat-msg-' + message.id;
        wrap.dataset.messageId = message.id;

        if (message.senderRole === 'SYSTEM') {
            wrap.style.cssText = 'align-self:center;max-width:90%;background:#f3f4f6;color:#4b5563;'
                + 'font-size:11px;padding:5px 10px;border-radius:10px;text-align:center;';
            wrap.textContent = message.body || '';   // ← عمداً textContent
            return wrap;
        }

        const own = isOwn(message);
        wrap.style.cssText = 'max-width:78%;padding:7px 10px;border-radius:12px;font-size:13px;'
            + 'line-height:1.7;white-space:pre-wrap;word-break:break-word;position:relative;'
            + (own ? 'align-self:flex-start;background:#4338ca;color:#fff;'
                   : 'align-self:flex-end;background:#f3f4f6;color:#111827;');

        if (!own && isAgent && message.senderName) {
            const who = document.createElement('div');
            who.style.cssText = 'font-size:10px;opacity:.7;margin-bottom:2px;';
            who.textContent = message.senderName;   // ← نامِ کاربر هم داده است، پس textContent
            wrap.appendChild(who);
        }

        if (message.replyToId) {
            const quoted = document.createElement('div');
            quoted.style.cssText = 'border-right:3px solid currentColor;opacity:.75;padding:2px 6px;'
                + 'margin-bottom:4px;font-size:11px;cursor:pointer;';
            const source = document.getElementById('chat-msg-' + message.replyToId);
            quoted.textContent = source ? shorten(source.dataset.plain || '', 70) : 'پیامِ نقل‌شده';
            quoted.addEventListener('click', () => {
                const target = document.getElementById('chat-msg-' + message.replyToId);
                if (target) target.scrollIntoView({behavior: 'smooth', block: 'center'});
            });
            wrap.appendChild(quoted);
        }

        if (message.attachment) {
            wrap.appendChild(buildAttachment(message));
        }

        if (message.body) {
            const text = document.createElement('div');
            text.textContent = message.body;        // ← عمداً textContent
            wrap.appendChild(text);
        }
        wrap.dataset.plain = message.body || (message.attachment ? message.attachment.name : '');

        const meta = document.createElement('div');
        meta.style.cssText = 'font-size:9px;opacity:.65;margin-top:3px;text-align:left;';
        meta.textContent = formatTime(message.createdAt);
        if (own) {
            const tick = document.createElement('span');
            tick.className = 'chat-tick';
            tick.textContent = message.readAt ? ' ✓✓' : ' ✓';
            meta.appendChild(tick);
        }
        wrap.appendChild(meta);

        // ریپلای: دابل‌کلیک روی پیام
        wrap.addEventListener('dblclick', () => setReply({
            id: message.id,
            body: wrap.dataset.plain,
            senderName: message.senderName
        }));

        return wrap;
    }

    function buildAttachment(message) {
        const a = message.attachment;
        if (message.type === 'IMAGE') {
            const img = document.createElement('img');
            img.src = a.url;                       // آدرسِ APIِ محافظت‌شده، نه مسیرِ عمومی
            img.alt = a.name || '';
            img.style.cssText = 'max-width:100%;border-radius:8px;display:block;margin-bottom:4px;cursor:pointer;';
            img.addEventListener('click', () => window.open(a.url, '_blank', 'noopener'));
            return img;
        }
        const link = document.createElement('a');
        link.href = a.url;
        link.target = '_blank';
        link.rel = 'noopener';
        link.style.cssText = 'display:flex;gap:6px;align-items:center;text-decoration:underline;margin-bottom:4px;';
        link.textContent = '📎 ' + (a.name || 'فایل') + ' (' + formatSize(a.sizeBytes) + ')';
        return link;
    }

    function appendMessage(message, autoScroll) {
        if (!host || document.getElementById('chat-msg-' + message.id)) return;
        const box = el('.chat-messages');
        const wasAtBottom = box.scrollHeight - box.scrollTop - box.clientHeight < 60;
        box.appendChild(buildMessage(message));
        if (autoScroll && wasAtBottom) scrollToBottom();
    }

    function prependMessage(message) {
        if (!host || document.getElementById('chat-msg-' + message.id)) return;
        const box = el('.chat-messages');
        box.insertBefore(buildMessage(message), box.firstChild);
    }

    function markOwnMessagesRead() {
        if (!host) return;
        host.querySelectorAll('.chat-tick').forEach(t => { t.textContent = ' ✓✓'; });
    }

    // ==========================================================
    // ارسال
    // ==========================================================

    async function sendCurrent() {
        if (!composerEnabled || !hasTarget()) return;
        const input = el('.chat-input');
        const text = input.value.trim();
        if (!text) return;
        input.value = '';
        input.style.height = 'auto';

        const payload = {body: text};
        if (conversationId && isAgent) payload.conversationId = conversationId;
        if (replyTo) payload.replyToId = replyTo.id;
        setReply(null);

        try {
            const saved = await postJson(`${API}/messages`, payload);
            appendMessage(saved, true);
            newestLoadedId = saved.id;
            scrollToBottom();
        } catch (err) {
            input.value = text; // پیام گم نشود
            showError(err);
        }
    }

    async function onFilePicked(ev) {
        const file = ev.target.files && ev.target.files[0];
        ev.target.value = '';
        if (!file || !composerEnabled || !hasTarget()) return;

        const form = new FormData();
        form.append('file', file);
        if (conversationId && isAgent) form.append('conversationId', conversationId);
        if (replyTo) form.append('replyToId', replyTo.id);
        setReply(null);

        try {
            // بدونِ Content-Type دستی — مرورگر خودش boundaryِ multipart را می‌گذارد
            const res = await fetch(`${API}/upload`, {
                method: 'POST', body: form, credentials: 'include', headers: withCsrf({})
            });
            if (!res.ok) throw new Error(await res.text());
            const saved = await res.json();
            appendMessage(saved, true);
            newestLoadedId = saved.id;
            scrollToBottom();
        } catch (err) {
            showError(err);
        }
    }

    /**
     * کارشناس بدونِ انتخابِ گفت‌وگو نباید چیزی بفرستد.
     * بدونِ این چک، درخواست به سرور می‌رفت و با «شناسهٔ گفت‌وگو لازم است» برمی‌گشت —
     * پیامی درست ولی گیج‌کننده، چون کاربر فقط دکمهٔ ارسال را زده بود.
     */
    function hasTarget() {
        if (conversationId) return true;
        if (isAgent) showError(new Error('اول یک گفت‌وگو را از فهرست باز کنید.'));
        return false;
    }

    function notifyTyping() {
        // throttle: رویدادِ گذراست، لازم نیست با هر کلید فرستاده شود
        const now = Date.now();
        if (now - lastTypingSentAt < 1500 || !conversationId) return;
        lastTypingSentAt = now;
        socketSend({action: 'typing', conversationId: conversationId});
    }

    function showTyping(name) {
        if (!host) return;
        const box = el('.chat-typing');
        box.textContent = (name ? name + ' ' : '') + 'در حال تایپ…';
        box.style.display = 'block';
        clearTimeout(typingHideTimer);
        typingHideTimer = setTimeout(() => { box.style.display = 'none'; }, 3000);
    }

    function setReply(target) {
        replyTo = target;
        if (!host) return;
        const bar = el('.chat-reply-bar');
        if (!target) { bar.style.display = 'none'; return; }
        el('.chat-reply-text').textContent = 'پاسخ به: ' + shorten(target.body || '', 60);
        bar.style.display = 'block';
        el('.chat-input').focus();
    }

    /** قفلِ خارج از ساعتِ کاری: ورودی غیرفعال، ولی تاریخچه خوانا می‌ماند. */
    function setComposerEnabled(enabled, lockedMessage) {
        composerEnabled = enabled;
        if (!host) return;
        el('.chat-composer').style.display = enabled ? 'flex' : 'none';
        const locked = el('.chat-locked');
        if (enabled) {
            locked.style.display = 'none';
        } else {
            locked.textContent = lockedMessage || '';
            locked.style.display = 'block';
        }
    }

    async function markRead() {
        if (!conversationId) return;
        try {
            await fetch(`${API}/conversations/${encodeURIComponent(conversationId)}/read`,
                {method: 'POST', credentials: 'include', headers: withCsrf({})});
        } catch (e) { /* بی‌اهمیت است؛ دفعهٔ بعد دوباره تلاش می‌شود */ }
    }

    // ==========================================================
    // اعلانِ مرورگر
    // ==========================================================

    function requestNotificationPermission() {
        if ('Notification' in window && Notification.permission === 'default') {
            Notification.requestPermission().catch(() => {});
        }
    }

    function maybeNotify(message) {
        if (!document.hidden || isOwn(message) || message.senderRole === 'SYSTEM') return;
        if (!('Notification' in window) || Notification.permission !== 'granted') return;
        try {
            new Notification(message.senderName || 'پیام تازه', {
                body: shorten(message.body || 'فایل فرستاد', 80)
            });
        } catch (e) { /* بعضی مرورگرها بیرونِ سرویس‌ورکر اجازه نمی‌دهند */ }
    }

    // ==========================================================
    // کمکی
    // ==========================================================

    function el(selector) { return host.querySelector(selector); }

    function scrollToBottom() {
        if (!host) return;
        const box = el('.chat-messages');
        box.scrollTop = box.scrollHeight;
    }

    /**
     * توکنِ CSRF از کوکی.
     * <p>
     * ⚠️ این ماژول عمداً خودش توکن را می‌فرستد و به میزبان تکیه نمی‌کند: پنلِ فروش و
     * صفحهٔ فروشگاه هرکدام رَپرِ fetchِ خودشان را دارند که توکن را تزریق می‌کند، ولی
     * صفحه‌های عمومیِ دیگر (خانه، بلاگ، آموزش…) ندارند. بدونِ این، حبابِ چت روی همان
     * صفحه‌ها موقعِ ارسال ۴۰۳ می‌گرفت — و چون ۴۰۳ی CSRF بدنهٔ عمومی دارد، پیامش هم
     * گیج‌کننده بود.
     */
    function csrfToken() {
        const m = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]+)/);
        return m ? decodeURIComponent(m[1]) : null;
    }

    function withCsrf(headers) {
        const token = csrfToken();
        if (token) headers['X-XSRF-TOKEN'] = token;
        return headers;
    }

    async function fetchJson(url) {
        const res = await fetch(url, {credentials: 'include'});
        if (!res.ok) throw new Error(await res.text());
        return res.json();
    }

    async function postJson(url, body) {
        const res = await fetch(url, {
            method: 'POST',
            credentials: 'include',
            headers: withCsrf({'Content-Type': 'application/json'}),
            body: JSON.stringify(body)
        });
        if (!res.ok) throw new Error(await res.text());
        return res.json();
    }

    function showError(err) {
        const text = (err && err.message ? err.message : '') || 'ارسال ناموفق بود';
        if (window.Swal) Swal.fire('خطا', text, 'error');
        else alert(text);
    }

    function shorten(text, max) {
        text = (text || '').replace(/\s+/g, ' ');
        return text.length > max ? text.slice(0, max) + '…' : text;
    }

    function formatTime(iso) {
        try { return new Date(iso).toLocaleTimeString('fa-IR', {hour: '2-digit', minute: '2-digit'}); }
        catch (e) { return ''; }
    }

    function formatSize(bytes) {
        if (!bytes) return '';
        if (bytes < 1024) return bytes + ' B';
        if (bytes < 1048576) return Math.round(bytes / 1024) + ' KB';
        return (bytes / 1048576).toFixed(1) + ' MB';
    }

    window.ChatCore = {
        connect, disconnect, onEvent,
        mountThread, openConversation, catchUp,
        setComposerEnabled, markRead,
        requestNotificationPermission,
        currentConversationId: () => conversationId
    };
})();
