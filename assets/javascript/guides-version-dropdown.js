$(document).ready(function() {
  $('#guides-version-dropdown').on('change', function() {
    var guideUrl = window.location.href.substring(window.location.href.indexOf("/guides/"));

    if (this.value == 'latest') {
      window.location.href = guideUrl;
    } else {
      window.location.href = '/version/' + this.value + guideUrl;
    }
  });
});
