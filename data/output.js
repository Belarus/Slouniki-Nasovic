// \u25AD - падзел цытат ад рэдакцыйных прыкладаў
// \u25CB - тэрміналагічнае спалучэнне
// \u25CA - фразеалагізм


var POSLETTERS="абвгдежзіклмнопрсту";

RegExp.quote = function(str) {
    return (str+'').replace(/[.?*+^$[\]\\(){}|-]/g, "\\$&");
};

//out.tag("<!DOCTYPE html>\n");
//out.tag("<html><head><meta charset=\"UTF-8\"></head><body>");

out.tag("<p>");
out.tag(out_zah(article.zah[0].textContent));
if (article.tlum)
for(var i=0; i<article.tlum.length; i++) {
  if (article.tlum.length > 1) {
    out.text(" "+(i+1)+") ");
  } else {
    out.text(" ");
  }
  out_tlum(article.tlum[i]);
}
out.tag("</p>\n");

//out.tag("</body></html>\n");

function out_zah(zahtext) {
  var zahWords = zahtext.split(/[\s\.,]+/);

  words = new Array();
  for each (var z in zahWords) {
    if (z == z.toUpperCase() && z.toLowerCase() != z.toUpperCase() &&
        z.indexOf('(') < 0 && z.indexOf(')') < 0 &&
        !(z.length == 1 && words.length > 0)) {
      var qz = RegExp.quote(z);
      if (qz.replace(/\\\-/g,'-') != z) out.log(qz);
//out.log("1 word="+z+" zahtext="+zahtext);
      zahtext = zahtext.replace(new RegExp("^"+qz+"([\\s\\.,]+)", 'g'), "<b>"+z+"</b>$1");
//out.log("2 word="+z+" zahtext="+zahtext);
      zahtext = zahtext.replace(new RegExp("([\\s\\.,]+)"+qz+"([\\s\\.,]+)", 'g'), "$1<b>"+z+"</b>$2");
//out.log("3 word="+z+" zahtext="+zahtext);
      zahtext = zahtext.replace(new RegExp("([\\s\\.,]+)"+qz+"$", 'g'), "$1<b>"+z+"</b>");
    }
  }
  return zahtext.replace("</b> <b>", " ").replace("</b> <b>", " ").replace("</b> <b>", " ").replace("</b> <b>", " ");
}

function out_tlum(tlum) {
  if (tlum.gram && !tlum.gram[0].textContent.isEmpty()) {
     out.tag(' ').text(tlum.gram[0].textContent).tag(' ');
  }
  out.tag(' ').text(tlum.desc[0].textContent).tag(' ');
  for each (ex in tlum.ex) {
    if (!ex.textContent.contains('\n')) { // usual text
      out.tag(' <i>').text(ex.textContent).tag('</i> ');
    } else { // poetry
      for each(line in ex.textContent.split("\n")) {
        out.tag(' <span class="poetry"><i>').text(line).tag("</i></span>\n");
      }
    }
  }
}

