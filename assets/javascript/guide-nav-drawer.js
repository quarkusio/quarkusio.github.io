(function() {
  var toggle = document.getElementById('guide-nav-toggle');
  var wrapper = document.getElementById('guide-nav-wrapper');
  var backdrop = document.getElementById('guide-nav-backdrop');
  var closeBtn = document.getElementById('guide-nav-close');

  if (!toggle || !wrapper) return;

  function openNav() {
    wrapper.classList.add('open');
    backdrop.classList.add('open');
    toggle.style.opacity = '0';
    toggle.style.pointerEvents = 'none';
  }

  function closeNav() {
    wrapper.classList.remove('open');
    backdrop.classList.remove('open');
    toggle.style.opacity = '';
    toggle.style.pointerEvents = '';
    if (document.activeElement) document.activeElement.blur();
  }

  toggle.addEventListener('click', openNav);
  closeBtn.addEventListener('click', closeNav);
  backdrop.addEventListener('click', closeNav);
  document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape' && wrapper.classList.contains('open')) closeNav();
  });
})();
