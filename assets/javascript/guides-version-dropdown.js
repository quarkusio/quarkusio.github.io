$(document).ready(function() {
  $('#guides-version-dropdown').on('change', function() {
    if (this.value == 'latest') {
      window.location.href = '/guides/';
    } else {
      window.location.href = '/version/' + this.value + '/guides/';
    }
  });
});
