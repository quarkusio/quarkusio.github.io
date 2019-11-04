function scrollDown() {
  $('.scroll-down').click (function() {
    $('html, body').animate({scrollTop: $('.video-band').offset().top }, 'slow');
    return false;
  });
}

if(document.querySelector(".scroll-down")){
  scrollDown();
}
