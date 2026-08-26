$( document ).ready( function () {
    function navigateToVersion( select ) {
        let guideUrl = window.location.href.substring( window.location.href.indexOf( "/guides/" ) );
        if ( guideUrl.startsWith( "http" ) ) { // /documentation/ URL
            guideUrl = "/guides/";
        }
        // Versioned paths contain a dot (e.g. /version/3.33/), which the clean-URL
        // handler treats as a file extension, so the trailing slash is mandatory.
        if ( !guideUrl.endsWith( "/" ) ) {
            guideUrl += "/";
        }

        window.location.href = select.value === 'latest'
            ? guideUrl
            : '/version/' + select.value + guideUrl;
    }

    $( '#guidesindex-version-dropdown, #guide-version-dropdown' ).on( 'change', function () {
        navigateToVersion( this );
    } );

    // Split-button version selector (menu items are plain links, so this only
    // needs to open/close the dropdown).
    function closeAllMenus() {
        $( '.version-split-menu' ).removeClass( 'is-open' );
        $( '.version-split-toggle' ).attr( 'aria-expanded', 'false' );
    }

    $( '.version-split' ).each( function () {
        const $split = $( this );
        const $toggle = $split.find( '.version-split-toggle' );
        const $menu = $split.find( '.version-split-menu' );

        $toggle.on( 'click', function (e) {
            e.stopPropagation();
            const willOpen = !$menu.hasClass( 'is-open' );
            closeAllMenus();
            $menu.toggleClass( 'is-open', willOpen );
            $toggle.attr( 'aria-expanded', willOpen ? 'true' : 'false' );
        } );
    } );

    // Close any open menu when clicking outside or pressing Escape (registered once).
    $( document ).on( 'click', closeAllMenus );
    $( document ).on( 'keydown', function (e) {
        if ( e.key === 'Escape' ) {
            closeAllMenus();
        }
    } );
} );
