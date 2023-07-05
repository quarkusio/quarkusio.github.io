let codes = document.querySelectorAll('pre.highlight > code');
let countID = 0;
codes.forEach((code) => {

  code.setAttribute("id", "code" + countID);
  
  let btn = document.createElement('button');
  btn.className = "btn-copy fa fa-clipboard";
  btn.setAttribute("data-clipboard-action", "copy");
  btn.setAttribute("data-clipboard-target", "#code" + countID);
  btn.setAttribute("title", "Copy to clipboard")
  btn.setAttribute("float-right", "true");
  
  let div = document.createElement('div');
  div.appendChild(btn);
  
  code.before(div);

  countID++;
}); 

let clipboard = new ClipboardJS('.btn-copy');
clipboard.on('success', function(e) {
    e.clearSelection();
    e.trigger.className = e.trigger.className.replace("fa-clipboard", "fa-check");
    e.trigger.setAttribute("title", "Copied!")

    setTimeout(function() {
        e.trigger.className = e.trigger.className.replace("fa-check", "fa-clipboard");
        e.trigger.setAttribute("title", "Copy to clipboard")
    }, 2000);
});
