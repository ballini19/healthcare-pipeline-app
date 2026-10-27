(function () {
  var $ = function (id) { return document.getElementById(id); };
  var timers = {};

  function api(path, opts) {
    return fetch(path, opts).then(function (r) {
      return r.text().then(function (t) {
        var body = null;
        try { body = t ? JSON.parse(t) : null; } catch (e) { body = { message: t }; }
        return { status: r.status, ok: r.ok, body: body };
      });
    });
  }

  function el(tag, cls, text) {
    var n = document.createElement(tag);
    if (cls) n.className = cls;
    if (text !== undefined) n.textContent = text;
    return n;
  }

  function when(iso) {
    var d = new Date(iso);
    return d.toLocaleDateString(undefined, { weekday: 'short', day: 'numeric', month: 'short' }) + ', ' +
      d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }

  /* views */
  document.querySelectorAll('#tabs button').forEach(function (b) {
    b.addEventListener('click', function () {
      document.querySelectorAll('#tabs button').forEach(function (x) { x.removeAttribute('aria-current'); });
      b.setAttribute('aria-current', 'page');
      document.querySelectorAll('.view').forEach(function (v) { v.hidden = v.id !== b.dataset.view; });
      if (b.dataset.view === 'lab') refreshLab();
      if (b.dataset.view === 'appts') loadAppts();
    });
  });

  /* doctors and slots */
  var doctors = [];
  function fillDoctors() {
    return api('/api/doctors').then(function (r) {
      doctors = r.body || [];
      ['doctor', 'raceDoctor'].forEach(function (id) {
        var s = $(id); s.textContent = '';
        doctors.forEach(function (d) {
          var o = el('option', '', d.name + ' · ' + d.specialty); o.value = d.id; s.appendChild(o);
        });
      });
      return loadSlots();
    });
  }
  function loadSlots() {
    var s = $('slot'); s.textContent = '';
    if (!$('doctor').value) return Promise.resolve();
    return api('/api/slots?doctorId=' + $('doctor').value).then(function (r) {
      (r.body || []).forEach(function (sl) {
        var o = el('option', '', when(sl.startTime)); o.value = sl.id; s.appendChild(o);
      });
      if (!s.options.length) s.appendChild(el('option', '', 'No free slots'));
    });
  }
  $('doctor').addEventListener('change', loadSlots);

  /* booking */
  var bookPoll = null;
  $('bookForm').addEventListener('submit', function (e) {
    e.preventDefault();
    if (!$('slot').value) return;
    var log = $('bookLog'); log.textContent = ''; $('bookEmpty').hidden = true;
    $('resBody').hidden = true; $('tech').hidden = false; $('resTitle').textContent = 'Booking your appointment...';
    clearInterval(bookPoll);
    var t0 = performance.now();
    api('/api/appointments', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        slotId: Number($('slot').value), patientName: $('name').value, patientEmail: $('email').value,
        insuranceId: $('ins').value, reason: $('reason').value
      })
    }).then(function (r) {
      var ms = Math.round(performance.now() - t0);
      var li = el('li');
      if (r.status === 202) {
        li.appendChild(el('span', 'ok', '202 Accepted in ' + ms + ' ms'));
        li.appendChild(el('code', '', 'reference ' + r.body.referenceId));
        log.appendChild(li);
        followStatus(r.body.referenceId);
      } else {
        $('resTitle').textContent = 'Please check the form';
        li.appendChild(el('span', 'bad', 'HTTP ' + r.status));
        li.appendChild(el('code', '', (r.body && (r.body.detail || r.body.message || r.body.title)) || 'Request refused'));
        log.appendChild(li);
      }
    });
  });

  function followStatus(ref) {
    var seen = {};
    function tick() {
      api('/api/appointments/' + ref).then(function (r) {
        if (!r.body || seen[r.body.state + r.body.message]) return;
        seen[r.body.state + r.body.message] = 1;
        var li = el('li');
        li.appendChild(el('span', r.body.state === 'CONFIRMED' ? 'ok' : r.body.state === 'REJECTED' ? 'bad' : '', r.body.state.toLowerCase()));
        li.appendChild(el('code', '', r.body.message));
        $('bookLog').appendChild(li);
        if (r.body.state === 'CONFIRMED' || r.body.state === 'REJECTED') {
          clearInterval(bookPoll); loadSlots();
          var ok = r.body.state === 'CONFIRMED';
          $('resTitle').textContent = ok ? 'Appointment booked' : 'Could not book this slot';
          $('resWhen').textContent = ok ? r.body.message.replace('Appointment confirmed for ', '') : r.body.message;
          $('resRef').textContent = ok ? 'Reference ' + ref : '';
          $('resBody').hidden = false;
          if (ok) loadAppts();
        }
      });
    }
    tick(); bookPoll = setInterval(tick, 400);
  }

  /* race */
  var raceSlot = null;
  $('raceGo').addEventListener('click', function () {
    var btn = $('raceGo'); btn.disabled = true;
    api('/api/slots?doctorId=' + $('raceDoctor').value).then(function (r) {
      var free = r.body || [];
      if (!free.length) { $('raceNote').textContent = 'This doctor has no free slots left.'; btn.disabled = false; return; }
      raceSlot = free[0].id;
      var count = Number($('raceCount').value) || 30;
      var t0 = performance.now();
      $('raceList').textContent = '';
      $('raceNote').textContent = 'Sending ' + count + ' requests for ' + when(free[0].startTime) + '...';
      return api('/api/demo/race?slotId=' + raceSlot + '&count=' + count, { method: 'POST' }).then(function (res) {
        var ms = Math.round(performance.now() - t0);
        $('raceNote').textContent = res.body.sent + ' requests accepted in ' + ms + ' ms. Watch the workers settle them.';
        clearInterval(timers.race);
        timers.race = setInterval(pollRace, 350); pollRace();
      });
    }).then(function () { btn.disabled = false; }, function () { btn.disabled = false; });
  });
  function pollRace() {
    if (!raceSlot) return;
    api('/api/demo/race/status?slotId=' + raceSlot).then(function (r) {
      var c = r.body.counts;
      document.querySelectorAll('#raceTiles b').forEach(function (b) { b.textContent = c[b.dataset.k]; });
      var list = $('raceList'); list.textContent = '';
      r.body.requests.slice(0, 12).forEach(function (q) {
        var li = el('li', q.state === 'CONFIRMED' ? 'good' : q.state === 'REJECTED' ? 'bad' : '');
        li.textContent = q.patientName + ' · ' + q.state.toLowerCase() + ' · ' + q.message;
        list.appendChild(li);
      });
      if (c.QUEUED + c.PROCESSING === 0 && c.CONFIRMED + c.REJECTED > 0) {
        clearInterval(timers.race);
        loadAppts();
        $('raceNote').textContent = c.CONFIRMED + ' booking, ' + c.REJECTED + ' rejected. No double booking.';
        loadSlots();
      }
    });
  }

  /* lab */
  $('labGo').addEventListener('click', function () {
    var f = $('labFile').files[0];
    var opts = { method: 'POST' };
    if (f) { var fd = new FormData(); fd.append('file', f); opts.body = fd; }
    $('labGo').disabled = true;
    api('/api/lab/run', opts).then(function (r) {
      $('labState').textContent = r.status === 202 ? 'Batch running...' : (r.body && r.body.message) || 'Could not start';
      clearInterval(timers.lab); timers.lab = setInterval(refreshLab, 500); refreshLab();
    });
  });
  $('sevFilter').addEventListener('change', refreshLab);

  function refreshLab() {
    api('/api/lab/status').then(function (r) {
      var s = r.body, last = s.last;
      if (last) document.querySelectorAll('#labTiles b').forEach(function (b) { b.textContent = last[b.dataset.k]; });
      $('labGo').disabled = s.running;
      if (s.running) $('labState').textContent = 'Batch running...';
      else if (last) {
        $('labState').textContent = last.status.toLowerCase() + ' in ' + last.millis + ' ms';
        clearInterval(timers.lab);
      }
      var sk = $('skipList'); sk.textContent = '';
      if (!s.skipped.length) sk.appendChild(el('li', '', 'None yet'));
      s.skipped.forEach(function (x) { sk.appendChild(el('li', '', x)); });
    });
    var sev = $('sevFilter').value;
    api('/api/lab/results' + (sev ? '?severity=' + sev : '')).then(function (r) {
      var body = $('labRows'); body.textContent = '';
      (r.body || []).forEach(function (x) {
        var tr = el('tr');
        [x.patientId, x.category + ' ' + x.testName, x.value.toFixed(2) + ' ' + x.unit,
          x.refLow + ' to ' + x.refHigh].forEach(function (t) { tr.appendChild(el('td', '', t)); });
        tr.appendChild(el('td', 'sev-' + x.severity, x.severity.toLowerCase()));
        body.appendChild(tr);
      });
      if (!body.children.length) { var tr = el('tr'); var td = el('td', 'muted', 'No results yet. Run the batch.'); td.colSpan = 5; tr.appendChild(td); body.appendChild(tr); }
    });
    api('/api/lab/alerts').then(function (r) {
      $('alertTotal').textContent = r.body.total ? r.body.total + ' received' : '';
      var list = $('alertList'); list.textContent = '';
      if (!r.body.latest.length) list.appendChild(el('li', '', 'No alerts yet'));
      r.body.latest.slice(0, 8).forEach(function (a) {
        var li = el('li', 'crit');
        li.appendChild(el('b', '', 'CRITICAL '));
        li.appendChild(document.createTextNode(a.patientId + ' · ' + a.testName + ' ' + a.value.toFixed(2) + ' ' + a.unit +
          ' (normal ' + a.refLow + ' to ' + a.refHigh + ')'));
        list.appendChild(li);
      });
    });
  }

  function loadAppts() {
    api('/api/appointments?limit=100').then(function (r) {
      var rows = r.body || [], body = $('apptRows'); body.textContent = '';
      $('apptCount').textContent = rows.length ? rows.length + ' in total.' : '';
      rows.forEach(function (a) {
        var tr = el('tr');
        [a.referenceId, a.patientName, a.slot.doctor.name, when(a.slot.startTime), a.reason || ''].forEach(function (t) {
          tr.appendChild(el('td', '', t));
        });
        body.appendChild(tr);
      });
      if (!rows.length) { var tr = el('tr'); var td = el('td', 'muted', 'Nothing booked yet.'); td.colSpan = 5; tr.appendChild(td); body.appendChild(tr); }
    });
  }

  fillDoctors();
})();
