function isvisible(obj) {
  return obj.offsetWidth > 0 && obj.offsetHeight > 0;
}
var shuffle = function() {
      elements = document.querySelectorAll('.randomize');
      Array.prototype.forEach.call(elements, function(parentel, i){
        var children = parentel.children;
        children = Array.prototype.slice.call(children, 0);
        children = Array.prototype.sort.call(children, function(a, b) {
            return Math.random() - 0.5;
        });
        for(var i = 0, l = children.length; i < l; i++) parentel.append(children[i]);
      });
};
shuffle();
