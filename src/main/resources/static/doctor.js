(function () {
  var me = Auth.require('DOCTOR');
  if (!me) return;
  var $ = function (id) { return document.getElementById(id); };
  var timers = {};
  var fresh = {};

  $('whoName').textContent = me.displayName;
  setupTabs(function (view) { if (view === 'lab') refreshLab(); });

  /* appointments */
  function loadOverview() {
    return api('/api/doctor/overview').then(function (r) {
      if (!r.ok) return;
      var d = r.body;
      $('docTitle').textContent = d.doctor.name;
      $('docSub').textContent = d.doctor.specialty + '. New bookings appear here the moment they are confirmed.';
      $('tBooked').textContent = d.booked;
      $('tOpen').textContent = d.open;
      var upcoming = d.appointments.filter(function (a) { return new Date(a.startTime) > new Date(); })[0];
      $('tNext').textContent = upcoming ? when(upcoming.startTime) : 'None';
      $('tNext').style.fontSize = upcoming ? '24px' : '';
      var body = $('apptRows'); body.textContent = '';
      d.appointments.forEach(function (a) {
        var tr = el('tr', fresh[a.referenceId] ? 'fresh' : '');
        var first = el('td', '', when(a.startTime));
        if (fresh[a.referenceId]) first.appendChild(el('span', 'tag', 'New'));
        tr.appendChild(first);
        tr.appendChild(el('td', '', a.patientName));
        tr.appendChild(el('td', '', a.reason || ''));
        tr.appendChild(el('td', '', a.referenceId));
        body.appendChild(tr);
      });
      if (!d.appointments.length) {
        var tr = el('tr'); var td = el('td', 'empty', 'No appointments yet. They will show up here as patients book.'); td.colSpan = 4; tr.appendChild(td); body.appendChild(tr);
      }
      fresh = {};
    });
  }

  function live(on) {
    $('live').classList.toggle('on', on);
    $('liveText').textContent = on ? 'Live' : 'Reconnecting';
  }
  function toast(title, text) {
    var t = $('toast'); t.textContent = '';
    t.appendChild(el('b', '', title)); t.appendChild(document.createTextNode(text));
    t.hidden = false;
    clearTimeout(timers.toast);
    timers.toast = setTimeout(function () { t.hidden = true; }, 6000);
  }

  /* the live stream: the server pushes an event as soon as a booking for this doctor is confirmed */
  function connect() {
    if (!window.EventSource) { live(false); return; }
    var es = new EventSource('/api/doctor/stream?token=' + encodeURIComponent(me.token));
    es.onopen = function () { live(true); };
    es.onerror = function () { live(false); };
    es.addEventListener('appointment', function (e) {
      var a = JSON.parse(e.data);
      fresh[a.referenceId] = true;
      toast('New appointment', a.patientName + ' booked ' + when(a.startTime));
      loadOverview();
    });
  }

  /* concurrency test */
  var raceSlot = null;
  function fillDoctors() {
    return api('/api/doctors').then(function (r) {
      var s = $('raceDoctor'); s.textContent = '';
      (r.body || []).forEach(function (d) {
        var o = el('option', '', d.name + ' · ' + d.specialty); o.value = d.id; s.appendChild(o);
      });
      if (me.doctorId) s.value = me.doctorId;
    });
  }
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
        $('raceNote').textContent = res.body.sent + ' requests accepted in ' + Math.round(performance.now() - t0) + ' ms. Watch the workers settle them.';
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
        $('raceNote').textContent = c.CONFIRMED + ' booking, ' + c.REJECTED + ' rejected. No double booking.';
        loadOverview();
      }
    });
  }

  /* lab pipeline */
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
      else if (last) { $('labState').textContent = last.status.toLowerCase() + ' in ' + last.millis + ' ms'; clearInterval(timers.lab); }
      var sk = $('skipList'); sk.textContent = '';
      if (!s.skipped.length) sk.appendChild(el('li', '', 'None yet'));
      s.skipped.forEach(function (x) { sk.appendChild(el('li', '', x)); });
    });
    var sev = $('sevFilter').value;
    api('/api/lab/results' + (sev ? '?severity=' + sev : '')).then(function (r) {
      var body = $('labRows'); body.textContent = '';
      (r.body || []).forEach(function (x) {
        var tr = el('tr');
        [x.patientId, x.category + ' ' + x.testName, x.value.toFixed(2) + ' ' + x.unit, x.refLow + ' to ' + x.refHigh]
          .forEach(function (t) { tr.appendChild(el('td', '', t)); });
        tr.appendChild(el('td', 'sev-' + x.severity, x.severity.toLowerCase()));
        body.appendChild(tr);
      });
      if (!body.children.length) {
        var tr = el('tr'); var td = el('td', 'empty', 'No results yet. Run the batch.'); td.colSpan = 5; tr.appendChild(td); body.appendChild(tr);
      }
    });
    api('/api/lab/alerts').then(function (r) {
      $('alertTotal').textContent = r.body.total ? r.body.total + ' received' : '';
      var list = $('alertList'); list.textContent = '';
      if (!r.body.latest.length) list.appendChild(el('li', '', 'No alerts yet'));
      r.body.latest.slice(0, 8).forEach(function (a) {
        list.appendChild(el('li', 'crit', 'Critical: ' + a.patientId + ' · ' + a.testName + ' ' + a.value.toFixed(2) + ' ' + a.unit +
          ' (normal ' + a.refLow + ' to ' + a.refHigh + ')'));
      });
    });
  }

  loadOverview();
  connect();
  fillDoctors();
})();
