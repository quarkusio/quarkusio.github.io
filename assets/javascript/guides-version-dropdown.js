$( document ).ready( function () {
    $( '#guidesindex-version-dropdown' ).on( 'change', function () {
        let guideUrl = window.location.href.substring( window.location.href.indexOf( "/guides/" ) );
        if ( guideUrl.startsWith( "http" ) ) { // /documentation/ URL
            guideUrl = "/guides/";
        }

        if ( this.value === 'latest' ) {
            window.location.href = guideUrl;
        }
        else {
            window.location.href = '/version/' + this.value + guideUrl;
        }
    } );

    $( '#guide-version-dropdown' ).on( 'change', function () {
        let guideUrl = window.location.href.substring( window.location.href.indexOf( "/guides/" ) );
        if ( guideUrl.startsWith( "http" ) ) { // /documentation/ URL
            guideUrl = "/guides/";
        }

        if ( this.value === 'latest' ) {
            window.location.href = guideUrl;
        }
        else {
            window.location.href = '/version/' + this.value + guideUrl;
        }
    } );

    // Split-button version selector (menu items are plain links, so this only
    // needs to open/close the dropdown).
    $( '.version-split' ).each( function () {
        const $split = $( this );
        const $toggle = $split.find( '.version-split-toggle' );
        const $menu = $split.find( '.version-split-menu' );

        function closeMenu() {
            $menu.removeClass( 'is-open' );
            $toggle.attr( 'aria-expanded', 'false' );
        }

        $toggle.on( 'click', function (e) {
            e.stopPropagation();
            const isOpen = $menu.toggleClass( 'is-open' ).hasClass( 'is-open' );
            $toggle.attr( 'aria-expanded', isOpen ? 'true' : 'false' );
        } );

        // Close when clicking outside or pressing Escape.
        $( document ).on( 'click', function () {
            closeMenu();
        } );
        $( document ).on( 'keydown', function (e) {
            if ( e.key === 'Escape' ) {
                closeMenu();
            }
        } );
    } );
} );
