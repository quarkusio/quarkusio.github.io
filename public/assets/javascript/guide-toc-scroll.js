(function () {
    // Accordion-style TOC for guides: top-level groups are collapsed by default
    // (via CSS, so there is no expand-then-collapse flash on load) and the group
    // for the section you are currently reading expands as you scroll. Only one
    // group is open at a time.
    const toc = document.querySelector( '.guide-toc-wrapper .toc' );
    if ( !toc ) {
        return;
    }

    // Distance from the top of the viewport at which a heading is considered the
    // "current" one. Kept large enough that at the very top of the page no
    // heading has crossed it yet, so the TOC starts fully collapsed.
    const OFFSET = 120;

    // Each top-level <li> that has a nested list is a collapsible group. Map every
    // heading id it covers (its own link plus all descendant links) back to it.
    const groups = [];
    const groupByHeadingId = {};
    const headings = [];

    const topItems = toc.querySelectorAll( '.roq-toc > ul > li' );
    topItems.forEach( function (li) {
        const hasChildren = !!li.querySelector( 'ul' );
        if ( hasChildren ) {
            li.classList.add( 'toc-group' );
            groups.push( li );
        }
        li.querySelectorAll( 'a[href^="#"]' ).forEach( function (link) {
            const id = decodeURIComponent( link.getAttribute( 'href' ).slice( 1 ) );
            const target = id && document.getElementById( id );
            if ( !target ) {
                return;
            }
            headings.push( {id: id, el: target, link: link} );
            // A non-collapsible top-level item maps to no group (nothing to expand),
            // which correctly closes every group while it is the active section.
            groupByHeadingId[id] = hasChildren ? li : null;
        } );
    } );

    if ( !headings.length ) {
        return;
    }

    // Headings are already in document order in the TOC.
    let openGroup = null;
    let activeLink = null;

    function apply(heading) {
        var group = heading ? groupByHeadingId[heading.id] : null;
        if ( group !== openGroup ) {
            if ( openGroup ) {
                openGroup.classList.remove( 'is-open' );
            }
            if ( group ) {
                group.classList.add( 'is-open' );
            }
            openGroup = group;
        }

        var link = heading ? heading.link : null;
        if ( link !== activeLink ) {
            if ( activeLink ) {
                activeLink.classList.remove( 'is-active' );
            }
            if ( link ) {
                link.classList.add( 'is-active' );
            }
            activeLink = link;
        }
    }

    function currentHeading() {
        let current = null;
        for ( let i = 0; i < headings.length; i++ ) {
            if ( headings[i].el.getBoundingClientRect().top - OFFSET <= 0 ) {
                current = headings[i];
            }
            else {
                break;
            }
        }
        return current;
    }

    let ticking = false;

    function onScroll() {
        if ( ticking ) {
            return;
        }
        ticking = true;
        window.requestAnimationFrame( function () {
            apply( currentHeading() );
            ticking = false;
        } );
    }

    window.addEventListener( 'scroll', onScroll, {passive: true} );
    window.addEventListener( 'resize', onScroll, {passive: true} );
    // Sync on load so a deep link (#anchor) opens the right group immediately.
    apply( currentHeading() );
})();
