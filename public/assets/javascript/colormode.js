function setTheme() {
  const storedTheme = localStorage.getItem('color-theme') || 'light';
  document.documentElement.dataset.storedTheme = storedTheme;
  let theme_color = "#fafafa";
  if (storedTheme === 'dark' || (storedTheme === 'system' && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
    document.documentElement.classList.add('dark');
    theme_color = "#1e1e1e";
  } else {
    document.documentElement.classList.remove('dark');
  }
  return theme_color;
}
function toggleTheme() {
  const themeOrder = ['light', 'dark', 'system'];
  const storedTheme = localStorage.getItem('color-theme') || 'system';
  const newTheme = themeOrder[(themeOrder.indexOf(storedTheme) + 1) % themeOrder.length];
  localStorage.setItem('color-theme', newTheme);
  document.querySelector('meta[name="theme-color"]').content = setTheme();
  updateButton();
}
function updateButton() {
  const button = document.getElementById('theme-toggle');
  const storedTheme = localStorage.getItem('color-theme') || 'system';
  const themeTitles = {
    'dark': 'Color scheme: dark; next: system preferences',
    'light': 'Color scheme: light; next: dark',
    'system': 'Color scheme: system preferences; next: light'
  };
  button.setAttribute('aria-label', storedTheme);
  button.setAttribute('title', themeTitles[storedTheme]);
  return button;
}
const meta = document.createElement('meta');
meta.name = "theme-color";
meta.content = setTheme();
document.head.appendChild(meta);
window.onload = function() {
  const button = updateButton();
  button.addEventListener('click', toggleTheme);
};