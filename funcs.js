var searchWordsExact = [];
var searchWordsRegexp = [];

function parseSearchString(searchStr, replaceJac) {
    searchWordsExact = [];
    searchWordsRegexp = [];
    var letterCount = 0;
    var w = "";
    for (var i = 0; i < searchStr.length; i++) {
        var c = searchStr.charAt(i).toLowerCase();
        if (c=='*') {
            w += c;
        } else if (c=='-' || LETTERS.indexOf(c)>=0) {
            letterCount++;
            w += c;
        } else if (c == ' ') {
            if (letterCount>=2) {
                if (w.includes('*') || (replaceJac && (w.includes('е') || w.includes('я')))) {
                    w = w.replaceAll('-','\-');
                    w = w.replaceAll('*','.*');
                    w = w.replaceAll('е','[еѣ]');
                    w = w.replaceAll('я','[яѣ]');
                    searchWordsRegexp.push(new RegExp('^'+w+'$'));
                } else {
                    searchWordsExact.push(w);
                }
                w = "";
                letterCount = 0;
            } else {
                $('#error').text('Мусіць быць 2 ці больш літары для пошуку');
                $('#error').show();
                throw "Error";
            }
        } else {
            $('#error').text('Памылка ў радку пошуку: невядомая літара "'+c+'"');
            $('#error').show();
            throw "Error";
        }
    }
    if (letterCount>=2) {
        if (w.includes('*') || (replaceJac && (w.includes('е') || w.includes('я')))) {
            w = w.replaceAll('-','\-');
            w = w.replaceAll('*','.*');
            w = w.replaceAll('е','[еѣ]');
            w = w.replaceAll('я','[яѣ]');
            searchWordsRegexp.push(new RegExp('^'+w+'$'));
        } else {
            searchWordsExact.push(w);
        }
    } else {
        $('#error').text('Мусіць быць 2 ці больш літары для пошуку');
        $('#error').show();
        throw "Error";
    }
}

function wordInArticle(articleWords) {
    for(var i=0; i<searchWordsExact.length; i++) {
        var found = false;
        for(var j=0; j<articleWords.length; j++) {
            if (searchWordsExact[i] == articleWords[j]) {
                found = true;
                break;
            }
        }
        if (!found) {
            return false;
        }
    }
    for(var i=0; i<searchWordsRegexp.length; i++) {
        var found = false;
        for(var j=0; j<articleWords.length; j++) {
            if (searchWordsRegexp[i].test(articleWords[j])) {
                found = true;
                break;
            }
        }
        if (!found) {
            return false;
        }
    }
    return true;
}

function search() {
    $('#error').hide();
    $('#intro').hide();
    $('#listWords').empty();

    var list;
    var replaceJac;
    if ($("#inlineRadio1").is(':checked')) {
        list = WORDS_NEW;
        replaceJac = false;
    } else if ($("#inlineRadio2").is(':checked')) {
        list = WORDS_HEADER;
        replaceJac = true;
    } else if ($("#inlineRadio3").is(':checked')) {
        list = WORDS_TEXT;
        replaceJac = true;
    }

    var words = $('#word').val().toLowerCase();
    parseSearchString(words, replaceJac);

    var found = false;
    for (var i = 0; i < list.length; i++) {
        var article = document.getElementById('aidx'+i);
        if (wordInArticle(list[i])) {
            article.className = '';
            found = true;
        } else {
            article.className = 'hidden';
        }
    }
    if (!found) {
        $('#error').text('Нічога не знойдзена');
        $('#error').show();
    }
}

function charFirst(word) {
    for(var i=0; i<word.length; i++) {
        var c=word.charAt(i).toUpperCase();
        if (c=='U') {
            continue;
        }
        if (c=='Ґ') {
            return 'Г';
        } else if (c.toLowerCase() != c.toUpperCase()) {
            return c;
        }
    }
    return '0';
}
function charLast(word, p) {
    for(var i=p-1; i>=0; i--) {
        var c=word.charAt(i).toUpperCase();
        if (c=='U') {
            continue;
        }
        if (c=='Ґ') {
            return 'Г';
        } else if (c.toLowerCase() != c.toUpperCase()) {
            return c;
        }
    }
    return '0';
}

function firstLetter(list, ch) {
    $('#error').hide();
    $('#intro').hide();
    $('#listWords').empty();
    $('#out div').hide();

    var place = $('#listWords');
    var n = 0;
    var txt = "";
    for (var i = 0; i < list.length; i++) {
        if (charFirst(list[i]) == ch) {
            if (n == 20) {
                place.append("<div class='col-md-3'>"+txt+"<br/><br/></div>\n");
                txt = "";
                n = 0;
            }
            n++;
            var pos = list[i].indexOf('#');
            var word = list[i].substring(0, pos);
            var idx = parseInt(list[i].substring(pos+1));
            txt += '<div><a href="javascript:" onclick="showElementPopover(this,['+idx+'])" data-placement="bottom" data-toggle="popover" data-container="body" data-html="true">'+word+'</a></div>';
        }
    }
    if (n > 0) {
        place.append("<div class='col-md-3'>"+txt+"</div>\n");
    }
}

function lastLetter(list, ch) {
    $('#error').hide();
    $('#intro').hide();
    $('#listWords').empty();
    $('#out div').hide();

    var place = $('#listWords');
    var n = 0;
    var txt = "";
    for (var i = 0; i < list.length; i++) {
        var p = list[i].indexOf('#');
        if (charLast(list[i], p) == ch) {
            if (n == 20) {
                place.append("<div class='col-md-3' style='text-align: right'>"+txt+"<br/><br/></div>\n");
                txt = "";
                n = 0;
            }
            n++;
            var pos = list[i].indexOf('#');
            var word = list[i].substring(0, pos);
            var idx = parseInt(list[i].substring(pos+1));
            txt += '<div><a href="javascript:" onclick="showElementPopover(this,['+idx+'])" data-placement="bottom" data-toggle="popover" data-container="body" data-html="true">'+word+'</a></div>';
        }
    }
    if (n > 0) {
        place.append("<div class='col-md-3' style='text-align: right'>"+txt+"</div>\n");
    }
}

function showLetter(list, ch) {
    $('#error').hide();
    $('#intro').hide();
    $('#listWords').empty();
    $('#out div').hide();

    var place = $('#listWords');
    var n = 0;
    var txt = "";
    for (var i = 0; i < list.length; i++) {
        var pos = list[i].indexOf('#');
        var word = list[i].substring(0, pos);
        var idx = parseInt(list[i].substring(pos+1));
        var article = document.getElementById('aidx'+idx);
        if (charFirst(list[i]) == ch) {
            article.className = '';
        } else {
            article.className = 'hidden';
        }
    }
}

function showElementPopover(elem, idxes) {
	setTimeout(function () {
		$(elem).popover({
			container: 'body',
			html: true,
			sanitize: false,
			placement: 'bottom',
			trigger: 'manual',
			content: function() {
				var r = '';
				for (var i = 0; i < idxes.length; i++) {
                    r += $('#aidx' + idxes[i]).html();
				}
                if (!r) {
                    r="<p style='color:red'>няісная спасылка</p>";
                }
				return r;
			}
		});
		$(elem).popover('show');
    },20);
}
function app(ch) {
    $('#word').val($('#word').val()+ch);
}

function sel() {
    if ($("#inlineRadio1").is(':checked')) {
        $("#inLettersButtons").hide();
    } else if ($("#inlineRadio2").is(':checked') || $("#inlineRadio3").is(':checked')) {
        $("#inLettersButtons").show();
    }
}

$(document).ready(function(){
    $('#searchform').attr('action', 'javascript:search()');
    $('#word').keypress(function(e){
      if(e.keyCode==13) {
        e.preventDefault();
        $('#btnSearch').click();
      }
    });
    $('body').click(function(e) {
    	if (!$(e.target).parents('.popover').length) {
            $('.popover').popover('hide');
        }
    });
    $(".expand").hide();
    $(".expandShow").attr("href","#");
    $(".expandHide").attr("href","#");
    $(".expandShow").click(function() {
        $(this).closest("div").find(".expand").show();
        $(this).hide();
        return false;
      });
    $(".expandHide").click(function() {
        $(this).closest("div").find(".expand").hide();
        $(this).closest("div").find(".expandShow").show();
        return false;
      });
    $('#input').show();
});
