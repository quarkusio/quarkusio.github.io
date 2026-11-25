$(document).ready(function() {
  "use strict";
  var myNav = {
    init: function() {
      this.cacheDOM();
      this.browserWidth();
      this.bindEvents();
    },
    cacheDOM: function() {
      this.navToggle = $(".nav-toggle");
      this.chkBox = $("#checkbox");
      this.navMenu = $("#menu");
    },
    browserWidth: function() {
      $(window).resize(this.bindEvents.bind(this));
    },
    bindEvents: function() {
      var width = window.innerWidth;

      if (width < 1024) {
        this.navToggle.click(this.animate.bind(this));
        this.navMenu.hide();
        this.chkBox[0].checked = false;
      } else {
        this.resetNav();
      }
    },
    animate: function(e) {
      var checkbox = this.chkBox[0];
      !checkbox.checked ?
        this.navMenu.slideDown() :
        this.navMenu.slideUp();
    },
    resetNav: function() {
      // Drop the inline display the slide animation left behind and let the stylesheet lay the menu out
      // again: .show() sets display: block, which overrides the desktop rule and leaves the narrow-viewport
      // menu, stacked, on a wide screen after a resize.
      this.navMenu.css('display', '');
      this.chkBox[0].checked = false;
    }
  };
  myNav.init();
});
