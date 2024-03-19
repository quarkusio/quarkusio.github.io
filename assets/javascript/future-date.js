function getCompareDate() {
  var d = new Date(),
      month = '' + (d.getMonth() + 1),
      day = '' + d.getDate(),
      year = d.getFullYear();
  if (month.length < 2) month = '0' + month;
  if (day.length < 2) day = '0' + day;
  return [year, month, day].join('');
}
var elements = document.querySelectorAll('[future-date]');
Array.prototype.forEach.call(elements, function(el, i){
  if(el.getAttribute('future-date').split('-').join('') < getCompareDate()) el.remove();
});