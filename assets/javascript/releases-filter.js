document.addEventListener('DOMContentLoaded', function() {
  var checkbox = document.getElementById('hide-eol');
  if (!checkbox) return;

  var today = new Date().toISOString().split('T')[0];

  function applyFilter() {
    var rows = document.querySelectorAll('.release-row');
    rows.forEach(function(row) {
      var eol = row.getAttribute('data-eol');
      var supported = row.hasAttribute('data-supported');
      if (eol && eol <= today && !supported) {
        row.classList.toggle('release-hidden', checkbox.checked);
      }
    });
  }

  applyFilter();
  checkbox.addEventListener('change', applyFilter);
});
