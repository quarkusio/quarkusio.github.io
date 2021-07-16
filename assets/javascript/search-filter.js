$(document).ready(function() {

  var matchCount = $('#list .in').length;
  $('.list-count').text(matchCount + ' items');

  $("#search-text").keyup(function () {

    var searchTerm = $("#search-text").val();
    var listItem = $('#list').children('li');
    var searchSplit = searchTerm.replace(/ /g, "'):containsi('")
    
      //extends :contains to be case insensitive
      $.extend($.expr[':'], {
        'containsi': function(elem, i, match, array)
        {
          return (elem.textContent || elem.innerText || '').toLowerCase()
          .indexOf((match[3] || "").toLowerCase()) >= 0;
        }
      });

      $("#list li").not(":containsi('" + searchSplit + "')").each(function(e)   {
        $(this).addClass('out').removeClass('in');
        setTimeout(function() {
          $('.out').addClass('hidden');
          $('#list').addClass('results');
        }, 300);
      });

      $("#list li:containsi('" + searchSplit + "')").each(function(e) {
        $(this).removeClass('hidden out').addClass('in');
        $('#list').removeClass('results');
      });

      var matchCount = $('#list .in').length;
      $('.list-count').text(matchCount + ' items');

    //Shows 'No Results' when no matches are found
    if(matchCount == '0') {
      $('#list').addClass('empty');
    }
    else {
      $('#list').removeClass('empty');
    }
    
  });
});
