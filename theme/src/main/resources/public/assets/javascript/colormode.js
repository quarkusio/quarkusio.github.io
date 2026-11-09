const DEFAULT_THEME = 'system';

function storedTheme() {
    try {
        return localStorage.getItem('color-theme') || DEFAULT_THEME;
    } catch (e) {
        return DEFAULT_THEME;
    }
}

function setTheme() {
    const theme = storedTheme();
    document.documentElement.dataset.storedTheme = theme;
    let theme_color = "#fafafa";
    if (theme === 'dark' || (theme === 'system' && window.matchMedia('(prefers-color-scheme: dark)').matches)) {
        document.documentElement.classList.add('dark');
        theme_color = "#1e1e1e";
    } else {
        document.documentElement.classList.remove('dark');
    }
    return theme_color;
}

function toggleTheme() {
    const themeOrder = ['light', 'dark', 'system'];
    const newTheme = themeOrder[(themeOrder.indexOf(storedTheme()) + 1) % themeOrder.length];
    try {
        localStorage.setItem('color-theme', newTheme);
    } catch (e) {
        // Storage is blocked; the choice just won't outlive this page.
    }
    document.querySelector('meta[name="theme-color"]').content = setTheme();
    updateButton();
}

function updateButton() {
    const button = document.getElementById('theme-toggle');
    if (!button) {
        return null;
    }
    const theme = storedTheme();
    const themeTitles = {
        'dark': 'Color scheme: dark; next: system preferences',
        'light': 'Color scheme: light; next: dark',
        'system': 'Color scheme: system preferences; next: light'
    };
    button.setAttribute('aria-label', themeTitles[theme]);
    button.setAttribute('title', themeTitles[theme]);
    return button;
}

const meta = document.createElement('meta');
meta.name = "theme-color";
meta.content = setTheme();
document.head.appendChild(meta);

// Follow the OS setting live while the reader has chosen system.
window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
    meta.content = setTheme();
});

document.addEventListener('DOMContentLoaded', function () {
    const button = updateButton();
    if (button) {
        button.addEventListener('click', toggleTheme);
        button.addEventListener('keydown', function (e) {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                toggleTheme();
            }
        });
    }
});
