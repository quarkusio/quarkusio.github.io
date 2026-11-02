document.addEventListener('DOMContentLoaded', function() {
  var hideEolCheckbox = document.getElementById('hide-eol');
  var hidePastMicrosCheckbox = document.getElementById('hide-past-micros');

  var today = new Date().toISOString().split('T')[0];

  function applyFilters() {
    var hideEol = hideEolCheckbox ? hideEolCheckbox.checked : false;
    var hidePastMicros = hidePastMicrosCheckbox ? hidePastMicrosCheckbox.checked : false;

    var releaseRows = document.querySelectorAll('.cves-main-table .release-row');
    releaseRows.forEach(function(row) {
      var eol = row.getAttribute('data-eol');
      var supported = row.hasAttribute('data-supported');
      var isEol = eol && eol <= today && !supported;
      var isPastMicro = row.hasAttribute('data-past-micro');

      var shouldHide = (hideEol && isEol) || (hidePastMicros && isPastMicro);
      row.classList.toggle('release-hidden', shouldHide);

      var detailsId = row.getAttribute('data-details-id');
      if (detailsId) {
        var detailsRow = document.getElementById(detailsId);
        if (detailsRow) {
          if (shouldHide) {
            detailsRow.classList.add('release-hidden');
            detailsRow.style.display = 'none';
            var btn = row.querySelector('.cve-twistie-btn');
            if (btn) btn.setAttribute('aria-expanded', 'false');
          } else {
            detailsRow.classList.remove('release-hidden');
            // If details was not expanded, keep it display none
            var btn = row.querySelector('.cve-twistie-btn');
            if (btn && btn.getAttribute('aria-expanded') === 'true') {
              detailsRow.style.display = 'table-row';
            } else {
              detailsRow.style.display = 'none';
            }
          }
        }
      }
    });
  }

  // Setup twistie click handlers
  var twistieButtons = document.querySelectorAll('.cve-twistie-btn');
  twistieButtons.forEach(function(btn) {
    btn.addEventListener('click', function(e) {
      e.preventDefault();
      e.stopPropagation();

      var detailsId = btn.getAttribute('aria-controls');
      if (!detailsId) return;

      var detailsRow = document.getElementById(detailsId);
      if (!detailsRow) return;

      var isExpanded = btn.getAttribute('aria-expanded') === 'true';
      if (isExpanded) {
        btn.setAttribute('aria-expanded', 'false');
        detailsRow.style.display = 'none';
      } else {
        btn.setAttribute('aria-expanded', 'true');
        detailsRow.style.display = 'table-row';
      }
    });
  });

  // Table sorting logic for .cves-table
  var SEVERITY_RANKS = {
    'CRITICAL': 4,
    'HIGH': 3,
    'MEDIUM': 2,
    'MODERATE': 2,
    'LOW': 1
  };

  function getSeverityRank(sev) {
    return SEVERITY_RANKS[(sev || '').trim().toUpperCase()] || 0;
  }

  function compareVersions(a, b) {
    if (!a && !b) return 0;
    if (!a) return 1;
    if (!b) return -1;
    var pa = a.split(/[.-]/);
    var pb = b.split(/[.-]/);
    var len = Math.max(pa.length, pb.length);
    for (var i = 0; i < len; i++) {
      var segA = pa[i] || '';
      var segB = pb[i] || '';
      var numA = parseInt(segA, 10);
      var numB = parseInt(segB, 10);
      if (!isNaN(numA) && !isNaN(numB)) {
        if (numA !== numB) return numA - numB;
      } else {
        if (segA !== segB) return segA.localeCompare(segB);
      }
    }
    return 0;
  }

  function sortCveTable(table, sortKey, ascending) {
    var tbody = table.querySelector('tbody');
    if (!tbody) return;

    var rows = Array.from(tbody.querySelectorAll('tr'));
    if (rows.length <= 1) return;

    rows.sort(function(rowA, rowB) {
      if (sortKey === 'severity') {
        var rankA = getSeverityRank(rowA.getAttribute('data-severity'));
        var rankB = getSeverityRank(rowB.getAttribute('data-severity'));
        if (rankA !== rankB) {
          return ascending ? rankA - rankB : rankB - rankA;
        }
        // Secondary sort within severity: published date descending (most recent first)
        var pubA = rowA.getAttribute('data-published') || '';
        var pubB = rowB.getAttribute('data-published') || '';
        if (pubA !== pubB) {
          return pubB.localeCompare(pubA);
        }
        var idA = rowA.getAttribute('data-advisory') || '';
        var idB = rowB.getAttribute('data-advisory') || '';
        return idA.localeCompare(idB);
      } else if (sortKey === 'published') {
        var pubA = rowA.getAttribute('data-published') || '';
        var pubB = rowB.getAttribute('data-published') || '';
        var cmp = pubA.localeCompare(pubB);
        return ascending ? cmp : -cmp;
      } else if (sortKey === 'fixed') {
        var fixA = rowA.getAttribute('data-fixed') || '';
        var fixB = rowB.getAttribute('data-fixed') || '';
        var cmp = compareVersions(fixA, fixB);
        return ascending ? cmp : -cmp;
      } else if (sortKey === 'affects') {
        var affA = (rowA.getAttribute('data-affects') || '').toLowerCase();
        var affB = (rowB.getAttribute('data-affects') || '').toLowerCase();
        var cmp = affA.localeCompare(affB);
        return ascending ? cmp : -cmp;
      } else { // advisory
        var idA = (rowA.getAttribute('data-advisory') || '').toLowerCase();
        var idB = (rowB.getAttribute('data-advisory') || '').toLowerCase();
        var cmp = idA.localeCompare(idB);
        return ascending ? cmp : -cmp;
      }
    });

    rows.forEach(function(r) { tbody.appendChild(r); });
  }

  document.addEventListener('click', function(e) {
    var th = e.target.closest('.cves-table th.sortable');
    if (!th) return;

    var table = th.closest('.cves-table');
    if (!table) return;

    var sortKey = th.getAttribute('data-sort');
    if (!sortKey) return;

    var isCurrentActive = th.classList.contains('sort-active');
    var isAscending = th.classList.contains('sort-asc');

    var newAscending;
    if (isCurrentActive) {
      newAscending = !isAscending;
    } else {
      // Default directions:
      // severity and published default to descending (highest severity or newest date first)
      newAscending = (sortKey !== 'severity' && sortKey !== 'published');
    }

    // Reset sibling headers in this table
    var allTh = table.querySelectorAll('th.sortable');
    allTh.forEach(function(otherTh) {
      otherTh.classList.remove('sort-active', 'sort-asc', 'sort-desc');
      var icon = otherTh.querySelector('i');
      if (icon) {
        icon.className = 'fas fa-sort';
      }
    });

    th.classList.add('sort-active');
    th.classList.add(newAscending ? 'sort-asc' : 'sort-desc');
    var icon = th.querySelector('i');
    if (icon) {
      icon.className = newAscending ? 'fas fa-sort-up' : 'fas fa-sort-down';
    }

    sortCveTable(table, sortKey, newAscending);
  });

  if (hideEolCheckbox) {
    hideEolCheckbox.addEventListener('change', applyFilters);
  }
  if (hidePastMicrosCheckbox) {
    hidePastMicrosCheckbox.addEventListener('change', applyFilters);
  }

  // Initial filter application
  applyFilters();
});
