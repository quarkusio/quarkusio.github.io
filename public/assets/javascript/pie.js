// Drag the handle round a ring made by partials/charts/pie.html. The handle snaps to the stops listed in
// data-stops ("angle:value,..."). Tell the page about it:
//   'pie-change' on the .pie element, detail.value: the user moved the handle to a stop
// and hear from it:
//   'pie-set' on the .pie element, detail.value: move the handle to where that value belongs, silently
(function() {
  function init(pie) {
    var svg = pie.querySelector('svg');
    var handle = pie.querySelector('.pie-handle');
    var center = pie.getAttribute('data-center');
    var stops = pie.getAttribute('data-stops').split(',').map(function(s) {
      var p = s.split(':');
      return { angle: parseFloat(p[0]), value: parseInt(p[1], 10) };
    });
    if (!handle || !stops.length || stops[0].value === undefined) {
      return;
    }
    var current = -1;
    var dragging = false;

    // the last stop that still includes this value, as the page renders it
    function indexFor(value) {
      var found = 0;
      stops.forEach(function(s, i) {
        if (s.value >= value) {
          found = i;
        }
      });
      return found;
    }

    function move(index, emit) {
      if (index === current) {
        return;
      }
      current = index;
      handle.setAttribute('transform', 'rotate(' + stops[index].angle + ' ' + center + ' ' + center + ')');
      handle.setAttribute('data-value', stops[index].value);
      if (emit) {
        pie.dispatchEvent(new CustomEvent('pie-change', { detail: { value: stops[index].value } }));
      }
    }

    // the stop nearest the pointer, going the short way round
    function nearest(event) {
      var box = svg.getBoundingClientRect();
      var dx = event.clientX - (box.left + box.width / 2);
      var dy = event.clientY - (box.top + box.height / 2);
      var angle = Math.atan2(dx, -dy) * 180 / Math.PI;
      angle = angle < 0 ? angle + 360 : angle;
      var best = 0;
      var bestDistance = 361;
      stops.forEach(function(s, i) {
        var d = Math.abs(angle - s.angle);
        d = Math.min(d, 360 - d);
        if (d < bestDistance) {
          best = i;
          bestDistance = d;
        }
      });
      return best;
    }

    svg.addEventListener('pointerdown', function(event) {
      dragging = true;
      svg.setPointerCapture(event.pointerId);
      move(nearest(event), true);
      event.preventDefault();
    });
    svg.addEventListener('pointermove', function(event) {
      if (dragging) {
        move(nearest(event), true);
      }
    });
    ['pointerup', 'pointercancel'].forEach(function(name) {
      svg.addEventListener(name, function() { dragging = false; });
    });
    pie.addEventListener('pie-set', function(event) {
      move(indexFor(event.detail.value), false);
    });

    current = indexFor(parseInt(handle.getAttribute('data-value'), 10));
    handle.removeAttribute('display'); // nothing can be dragged without this script
  }

  document.addEventListener('DOMContentLoaded', function() {
    document.querySelectorAll('[data-pie]').forEach(init);
  });
})();
