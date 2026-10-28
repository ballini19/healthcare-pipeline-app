(function () {
  var me = Auth.require('PATIENT');
  if (!me) return;
  var $ = function (id) { return document.getElementById(id); };

  $('whoName').textContent = me.displayName;
  $('name').value = me.displayName;
  $('email').value = me.email || '';

  setupTabs(function (view) { if (view === 'mine') loadMine(); });

  /* doctors and free slots */
  function fillDoctors() {
    return api('/api/doctors').then(function (r) {
      var s = $('doctor'); s.textContent = '';
      (r.body || []).forEach(function (d) {
        var o = el('option', '', d.name + ' · ' + d.specialty); o.value = d.id; s.appendChild(o);
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

  /* booking: the server answers 202 at once, then we follow the request until it settles */
  var poll = null;
  $('bookForm').addEventListener('submit', function (e) {
    e.preventDefault();
    if (!$('slot').value) return;
    var log = $('bookLog'); log.textContent = '';
    $('bookEmpty').hidden = true; $('resBody').hidden = true; $('tech').hidden = false;
    $('resTitle').textContent = 'Booking your appointment...';
    $('result').classList.remove('sage');
    $('bookBtn').disabled = true;
    clearInterval(poll);
    var t0 = performance.now();
    api('/api/appointments', {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        slotId: Number($('slot').value), patientName: $('name').value, patientEmail: $('email').value,
        insuranceId: $('ins').value, reason: $('reason').value
      })
    }).then(function (r) {
      var li = el('li');
      if (r.status === 202) {
        li.appendChild(el('span', 'ok', '202 Accepted in ' + Math.round(performance.now() - t0) + ' ms'));
        li.appendChild(el('code', '', 'reference ' + r.body.referenceId));
        log.appendChild(li);
        follow(r.body.referenceId);
      } else {
        $('resTitle').textContent = 'Please check the form';
        li.appendChild(el('span', 'bad', 'HTTP ' + r.status));
        li.appendChild(el('code', '', (r.body && (r.body.detail || r.body.message || r.body.title)) || 'Request refused'));
        log.appendChild(li);
        $('bookBtn').disabled = false;
      }
    }, function () { $('bookBtn').disabled = false; });
  });

  function follow(ref) {
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
          clearInterval(poll); $('bookBtn').disabled = false; loadSlots();
          var ok = r.body.state === 'CONFIRMED';
          $('resTitle').textContent = ok ? 'Appointment booked' : 'Could not book this slot';
          $('resWhen').textContent = ok ? r.body.message.replace('Appointment confirmed for ', '') : r.body.message;
          $('resRef').textContent = ok ? 'Reference ' + ref : '';
          $('resBody').hidden = false;
          $('result').classList.toggle('sage', ok);
          if (ok) loadMine();
        }
      });
    }
    tick(); poll = setInterval(tick, 400);
  }

  /* my appointments */
  function loadMine() {
    api('/api/my/appointments').then(function (r) {
      var body = $('mineRows'); body.textContent = '';
      (r.body || []).forEach(function (a) {
        var tr = el('tr');
        [a.referenceId, a.doctorName + ' · ' + a.specialty, when(a.startTime), a.reason || ''].forEach(function (t) { tr.appendChild(el('td', '', t)); });
        body.appendChild(tr);
      });
      if (!body.children.length) {
        var tr = el('tr'); var td = el('td', 'empty', 'You have no appointments yet.'); td.colSpan = 4; tr.appendChild(td); body.appendChild(tr);
      }
    });
  }

  fillDoctors();
  loadMine();
})();
