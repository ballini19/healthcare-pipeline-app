(function () {
  var existing = Auth.get();
  if (existing) { location.replace(existing.role === 'DOCTOR' ? '/doctor.html' : '/patient.html'); return; }

  function wire(formId, role, target) {
    var form = document.getElementById(formId);
    var note = form.querySelector('.formnote');
    var btn = form.querySelector('button');
    form.addEventListener('submit', function (e) {
      e.preventDefault();
      note.textContent = '';
      btn.disabled = true;
      api('/api/auth/login', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: form.username.value, password: form.password.value, role: role })
      }).then(function (r) {
        if (r.ok) { Auth.set(r.body); location.href = target; return; }
        note.textContent = (r.body && r.body.message) || 'Could not sign in';
        btn.disabled = false;
      }, function () { note.textContent = 'The server is not reachable'; btn.disabled = false; });
    });
  }
  wire('doctorForm', 'DOCTOR', '/doctor.html');
  wire('patientForm', 'PATIENT', '/patient.html');
})();
