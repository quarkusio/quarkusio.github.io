$(document).ready(function() {
  $('#guidesindex-version-dropdown').on('change', function() {
    var guideUrl = window.location.href.substring(window.location.href.indexOf("/guides/"));
    if (guideUrl.startsWith("http")) { // /documentation/ URL
      guideUrl = "/guides/";
    }

    if (this.value == 'latest') {
      window.location.href = guideUrl;
    } else {
      window.location.href = '/version/' + this.value + guideUrl;
    }
  });

  $('#guide-version-dropdown').on('change', function() {
    var guideUrl = window.location.href.substring(window.location.href.indexOf("/guides/"));
    if (guideUrl.startsWith("http")) { // /documentation/ URL
      guideUrl = "/guides/";
    }

    if (this.value == 'latest') {
      window.location.href = guideUrl;
    } else {
      window.location.href = '/version/' + this.value + guideUrl;
    }
  });
});
