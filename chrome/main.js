var gmail;


function refresh(f) {
  if(/in/.test(document.readyState)) {
    setTimeout('refresh(' + f + ')', 10);
  } else {
    f();
  }
}


var main = function(){

  if(typeof Firebase === "undefined") {
    console.log('No firebase');
    // return main();
    return;
  }

  console.log('firebase!');

  var myFirebaseRef = new Firebase("https://rogerchat.firebaseio.com/people/kartikt");
  var display_toolbar = function() {
    var html  = '<div class="groundControl"><style>#base { position: fixed; top: 1em; right: 1em;z-index: 50000;} #corner {width:200px;height:100px;float: left;margin: -40px 30px 1px 1px;}';
        html += '</style><div id="base"><div id="corner"><img src="http://i.imgur.com/LKVIfqG.gif"><audio id="groundControlAudio" autoplay=""><source src="http://50.112.162.251/uploads/audio.mp3" type="audio/mp3">Your browser does not support the audio element.</audio></div></div></div>';
    if($('.groundControl').length == 0) {
      $('body').append(html);
    }

    console.log("added ground control div")

    return;
  }



  console.log(myFirebaseRef);

  myFirebaseRef.on('value', function(x) {
    var d = x.val();
    console.log(d);
    var h = d.has_message;

    console.log(h);
    if(h === true) {
      display_toolbar();
      var aud = document.getElementById("groundControlAudio");
      aud.onended = function() {
        // myFirebaseRef.child("has_message").set(false);
      };
    } else {
      $('.groundControl').remove();
      console.log("removed ground control div")
    }

  })
}



refresh(main);
