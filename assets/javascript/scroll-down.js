function scrollDown() {
  $('.scroll-down').click (function() {
    $('html, body').animate({scrollTop: $('.homepage-highlights-band').offset().top }, 'slow');
    return false;
  });
}

if(document.querySelector(".scroll-down")){
  scrollDown();
}
