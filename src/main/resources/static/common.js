(function () {
  var KEY = 'hp_session';

  /* The login lives in sessionStorage, so every browser tab keeps its own: one tab can be a doctor, another a patient. */
  window.Auth = {
    get: function () { try { return JSON.parse(sessionStorage.getItem(KEY) || 'null'); } catch (e) { return null; } },
    set: function (s) { sessionStorage.setItem(KEY, JSON.stringify(s)); },
    clear: function () { sessionStorage.removeItem(KEY); },
    require: function (role) {
      var s = Auth.get();
      if (!s || s.role !== role) { location.replace('/'); return null; }
      return s;
    },
    logout: function () {
      var s = Auth.get();
      var done = function () { Auth.clear(); location.href = '/'; };
      if (!s) return done();
      fetch('/api/auth/logout', { method: 'POST', headers: { Authorization: 'Bearer ' + s.token } }).then(done, done);
    }
  };

  window.api = function (path, opts) {
    opts = opts || {};
    opts.headers = Object.assign({}, opts.headers || {});
    var s = Auth.get();
    if (s) opts.headers.Authorization = 'Bearer ' + s.token;
    return fetch(path, opts).then(function (r) {
      return r.text().then(function (t) {
        var body = null;
        try { body = t ? JSON.parse(t) : null; } catch (e) { body = { message: t }; }
        if (r.status === 401 && path.indexOf('/api/auth/login') < 0) { Auth.clear(); location.replace('/'); }
        return { status: r.status, ok: r.ok, body: body };
      });
    });
  };

  window.el = function (tag, cls, text) {
    var n = document.createElement(tag);
    if (cls) n.className = cls;
    if (text !== undefined) n.textContent = text;
    return n;
  };

  window.when = function (iso) {
    var d = new Date(iso);
    return d.toLocaleDateString(undefined, { weekday: 'short', day: 'numeric', month: 'short' }) + ', ' +
      d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  window.setupTabs = function (onOpen) {
    var buttons = document.querySelectorAll('.tabs button');
    buttons.forEach(function (b) {
      b.addEventListener('click', function () {
        buttons.forEach(function (x) { x.removeAttribute('aria-current'); });
        b.setAttribute('aria-current', 'page');
        document.querySelectorAll('.view').forEach(function (v) { v.hidden = v.id !== b.dataset.view; });
        if (onOpen) onOpen(b.dataset.view);
      });
    });
  };

  var logout = document.getElementById('logout');
  if (logout) logout.addEventListener('click', Auth.logout);
})();
