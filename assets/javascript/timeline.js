// Progressive enhancement for .tl timelines (see partials/charts/timeline.html).
// Everything is pre-rendered; this only adds the today line, tooltips, table sync and the view toggle.
(function() {
  var DAY = 24 * 60 * 60 * 1000;
  var tooltip = null;

  function parse(d) {
    return Date.parse(d + 'T00:00:00Z');
  }

  function todayUtc() {
    var n = new Date();
    return Date.UTC(n.getFullYear(), n.getMonth(), n.getDate());
  }

  function placeToday(tl) {
    var line = tl.querySelector('.tl-today');
    var tag = tl.querySelector('.tl-today-tag');
    var start = parse(tl.getAttribute('data-range-start'));
    var end = parse(tl.getAttribute('data-range-end'));
    var today = todayUtc();
    var show = today >= start && today <= end;
    if (!line) {
      return;
    }
    line.style.display = show ? '' : 'none';
    if (tag) {
      tag.style.display = show ? '' : 'none';
    }
    if (!show) {
      return;
    }
    var pct = ((today - start) / (end - start) * 100).toFixed(3) + '%';
    line.setAttribute('x1', pct);
    line.setAttribute('x2', pct);
    if (tag) {
      tag.setAttribute('x', pct);
      tag.querySelector('text').textContent = 'Today · ' + new Date(today).toLocaleDateString('en-GB',
        { day: '2-digit', month: 'short', year: 'numeric', timeZone: 'UTC' });
    }
  }

  function ensureTooltip() {
    if (!tooltip) {
      tooltip = document.createElement('div');
      tooltip.className = 'tl-tooltip';
      tooltip.setAttribute('role', 'tooltip');
      tooltip.hidden = true;
      document.body.appendChild(tooltip);
    }
    return tooltip;
  }

  function showTooltip(seg) {
    var tip = ensureTooltip();
    var tl = seg.closest('.tl');
    tip.textContent = '';
    var title = document.createElement('strong');
    title.textContent = seg.getAttribute('data-tooltip-title') || '';
    tip.appendChild(title);
    (seg.getAttribute('data-tooltip') || '').split('|').forEach(function(line) {
      if (line) {
        var div = document.createElement('div');
        div.textContent = line;
        tip.appendChild(div);
      }
    });
    tip.hidden = false;
    var box = seg.getBoundingClientRect();
    var tlBox = tl.getBoundingClientRect();
    var left = box.left + box.width / 2 + window.scrollX;
    var top = box.bottom + window.scrollY + 6;
    // flip to the left when close to the right edge of the chart
    if (tlBox.right - (box.left + box.width / 2) < 240) {
      left -= tip.offsetWidth;
    }
    var maxLeft = window.scrollX + document.documentElement.clientWidth - tip.offsetWidth - 4;
    left = Math.max(window.scrollX + 4, Math.min(left, maxLeft));
    tip.style.left = left + 'px';
    tip.style.top = top + 'px';
  }

  function hideTooltip() {
    if (tooltip) {
      tooltip.hidden = true;
    }
  }

  function highlight(version, on) {
    if (!version) {
      return;
    }
    var sel = '[data-version="' + version + '"]';
    document.querySelectorAll(sel + ', [data-row="' + version + '"]').forEach(function(el) {
      el.classList.toggle('is-highlighted', on);
    });
  }

  function versionOf(el) {
    var holder = el.closest('[data-version]');
    return holder ? holder.getAttribute('data-version') : null;
  }

  // The native <title> tooltip is the no-JS fallback; with JS the styled tooltip replaces it,
  // so move its text to aria-label to keep it available to screen readers.
  function suppressNativeTooltips() {
    document.querySelectorAll('.tl .tl-seg > title').forEach(function(title) {
      title.parentNode.setAttribute('aria-label', title.textContent);
      title.parentNode.removeChild(title);
    });
  }

  function initTooltipsAndSync() {
    document.querySelectorAll('.tl [data-tooltip]').forEach(function(seg) {
      seg.addEventListener('mouseenter', function() { showTooltip(seg); highlight(versionOf(seg), true); });
      seg.addEventListener('focus', function() { showTooltip(seg); highlight(versionOf(seg), true); });
      seg.addEventListener('mouseleave', function() { hideTooltip(); highlight(versionOf(seg), false); });
      seg.addEventListener('blur', function() { hideTooltip(); highlight(versionOf(seg), false); });
    });
    document.addEventListener('keydown', function(e) {
      if (e.key === 'Escape') {
        hideTooltip();
      }
    });
    document.querySelectorAll('.tl-labels li').forEach(function(li) {
      var v = li.getAttribute('data-version');
      li.addEventListener('mouseenter', function() { highlight(v, true); });
      li.addEventListener('mouseleave', function() { highlight(v, false); });
    });
    // table rows highlight the chart
    document.querySelectorAll('tr.release-row[data-version]').forEach(function(tr) {
      var v = tr.getAttribute('data-version');
      tr.addEventListener('mouseenter', function() { highlight(v, true); });
      tr.addEventListener('mouseleave', function() { highlight(v, false); });
    });
  }

  function initToggle() {
    var buttons = document.querySelectorAll('[data-timeline-toggle]');
    buttons.forEach(function(btn) {
      btn.addEventListener('click', function() {
        var view = btn.getAttribute('data-timeline-toggle');
        document.querySelectorAll('.tl[data-view]').forEach(function(tl) {
          tl.hidden = tl.getAttribute('data-view') !== view;
        });
        buttons.forEach(function(b) {
          b.setAttribute('aria-pressed', String(b === btn));
        });
        hideTooltip();
      });
    });
  }

  document.addEventListener('DOMContentLoaded', function() {
    document.querySelectorAll('.tl').forEach(placeToday);
    suppressNativeTooltips();
    initTooltipsAndSync();
    initToggle();
  });
})();
