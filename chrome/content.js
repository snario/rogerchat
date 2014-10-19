
if (typeof jQuery === "undefined")  {
  var j = document.createElement('script');
  j.src = chrome.extension.getURL('jquery-1.10.2.min.js');
  (document.head || document.documentElement).appendChild(j);
}

var g = document.createElement('script');
g.src = "https://cdn.firebase.com/js/client/1.1.2/firebase.js";
(document.head || document.documentElement).appendChild(g);

var s = document.createElement('script');
s.src = chrome.extension.getURL('main.js');
(document.head || document.documentElement).appendChild(s);
