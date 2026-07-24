$(document).ready(function () {

  const root = $('#list').length > 0 ? '#list' : '#docs';

  var matchCount = $(root + ' .in').length;
  $('.list-count').text(matchCount + ' items');

  const filterItems = (searchTerm, isSearch) => {
    var searchSplit = searchTerm.replace(/ /g, "'):containsi('")

    //extends :contains to be case insensitive
    $.extend($.expr[':'], {
      'containsi': function (elem, i, match, array) {
        return (elem.textContent || elem.innerText || '').toLowerCase()
          .indexOf((match[3] || "").toLowerCase()) >= 0;
      }
    });

    $(root).removeClass('results');
    setTimeout(function () {
      $('.out').addClass('hidden'); // all .out elements
      $(root).addClass('results');
      if ( root === '#docs' ) {
        // also hide enclosing doc list / section if empty
        $('#docs .docslist').filter(function( index ) {
          return $( ".in", this ).length <= 0;
        }).addClass('hidden out');
      }
    }, 300);

    if (root === '#list') {
      $("#list li").not(":containsi('" + searchSplit + "')").each(function (e) {
        $(this).addClass('out').removeClass('in');
      });
      $("#list li:containsi('" + searchSplit + "')").each(function (e) {
        $(this).removeClass('hidden out').addClass('in');
      });
    } else {
      const source = isSearch ? "#docs .docs-card" : "#docs .docs-card .categories";

      $(source).not(":containsi('" + searchSplit + "')").each(function (e) {
        const target = isSearch ? $(this) : $(this).parent();
        target.addClass('out').removeClass('in');
      });

      $(source + ":containsi('" + searchSplit + "')").each(function (e) {
        const target = isSearch ? $(this) : $(this).parent();
        target.removeClass('hidden out').addClass('in');
        target.parent().parent().removeClass('hidden out');
      });
    }

    var matchCount = $(root + ' .in').length;
    $('.list-count').text(matchCount + ' items');

    //Shows 'No Results' when no matches are found
    if (matchCount == '0') {
      $(root).addClass('empty');
    } else {
      $(root).removeClass('empty');
    }
  }

  $("#search-text").keyup(function () {
    filterItems($("#search-text").val(), true);
  });

  // on #docs pages, when a category is clicked, change the contents of the filter box
  // and trigger the keyup event (see handler above)
  $('#guidesindex-category-dropdown').on('change', function() {
    filterItems(this.value, false);
  });
});
