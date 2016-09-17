var fs = require('fs');
var _ = require('lodash');
var assert = require('assert');
var cheerio = require('cheerio');

function removeEOL(doc) {
  return doc.replace(/\r?\n|\r/g, '');
}

function addSeparator(doc) {
  var regex = /(.*?):<br>/g;
  doc = doc.replace(regex, function (match) {
    if (match.length <= 40) { // if colon (:) means title
      return ["</entry>", "<entry>", match].join('');
    } else {
      return match;
    }
  });

  doc = doc.replace(/<\/p>\r\n<hr>/gm, function (match) {
    return ["</entry>", match].join('');
  });
  doc = doc.replace(/\(\d+\) ?. /gm, function (match) {
    return ["</footnote>", "<footnote>", match].join('');
  });
  doc = doc.replace(/<\/h5>/gm, function (match) {
    return ["</footnote>", match].join('');
  });
  return doc;
}

function Ref() {
  this.sooreh = null;
  this.ayat = [];
  this.jeld = null;
  this.pages = [];
}

function parseFootnote2(footnote) {
  var re = /([\u0600-\u06FF\uFB8A\u067E\u0686\u06AF]+|\w+)،?( | )? ?(aye|آیه|raye|رآیه)( | )? ? ?(\d+)( | )? ?(r|ر)?\((j|ج)( | )? ?(\d+)،( | )?( | )?(s|ص|safhe|صفحه)?( | )?( | )?(\d+)\).?/g;
  var res = re.exec(footnote);
  var note = new Ref();

  if (res !== null) {
    note.sooreh = res[1].slice(0, res[1].length - 1);
    note.ayat.push(res[5]);
    note.jeld = res[10];
    note.pages.push(res[16]);
  }
  if (res === null) {
    var re1 = /([\u0600-\u06FF\uFB8A\u067E\u0686\u06AF]+|\w+)،?( | )?( | )?(ayat|آیات|aye|آیه) ?(\d+)( ?(،|v|و) ? ?(\d+))?( ?(،|v|و) ?(\d+))?( | )?( | )?\((j|ج)( | )?( | )?(\d+)،( | )?( | )?(safahat|صفحات|s|ص)( | )?( | )?(\d+)(،( | )?( | )?(\d+)(، ?(\d+))?(، ? ?(\d+))?)?\).?/g;
    var res1 = re1.exec(footnote);
    if (res1 !== null) {
      note.sooreh = res1[1].slice(0, res1[1].length - 1);
      note.ayat.push(res1[5]);
      note.ayat.push(res1[8]);
      note.jeld = res1[17];
      note.pages.push(res1[23]);
      note.pages.push(res1[27]);
    }

    if (res1 === null) {
      var re2 = /([\u0600-\u06FF\uFB8A\u067E\u0686\u06AF]+|\w+)، ?(aye|آیه) ? ?(\d+)/g;
      var res2 = re2.exec(footnote);
      if (res2 !== null) {
        note.sooreh = res2[1].slice(0, res2[1].length - 1);
        note.ayat.push(res2[3]);
      }
    }
  }

  if (res2 === null) {
    //console.log(footnote);
    return null;
  } else {
    return note;
  }
}


function parseFootnote(doc) {
  $ = cheerio.load(doc, {
    decodeEntities: false
  });
  var list = [];
  $('footnote').each(function () {
    var footnote = $(this).html();

    var eolRegex = /\r\n/gi;
    footnote = footnote.replace(eolRegex, '');

    var regex = /\((\d+)\) ?. ?(.*)/i;

    var note = footnote.match(regex);


    var entries = note[2].split(";");
    entries = entries.map(function (ent) {
      ent = ent.replace(/<br>/g, '');
      return ent.trim();
    });

    entries = entries.map(function (e) {
      return parseFootnote2(e);
    });


    list.push({
      index: note[1],
      note: entries
    });
  });
  return list;
}

function parseDoc(doc, footnotes) {
  $ = cheerio.load(doc, {
    decodeEntities: false
  });
  var list = [];
  $('entry').each(function () {
    var entry = $(this).html();
    var title = entry.split(/\r\n/)[0].replace(':<br>', '').trim();
    var regex = /\(<span class="aye">(.*)<\/span>\)<br>/i;
    var data = regex.exec(entry);

    var offset = null;
    var description = entry.replace(regex, function (match, a, off) {
      offset = off;
      return '';
    });
    if (offset) {
      description = description.substring(offset);
    } else {
      //TODO: check else block
    }

    var footnotesIdx = [];
    var rgx = /\((\d+)\)/g;
    var match;
    while ((match = rgx.exec(description)) != null) {
      footnotesIdx.push(match[1]);
    }

    var notes = _.map(footnotesIdx, function (id) {
      return _.find(footnotes, function (footnote) {
        return footnote.index == id;
      })
    });

    list.push({
      title: title,
      aye: _.get(data, '[1]'),
      description: removeEOL(description),
      footnotes: notes
    });
  });
  return list;
}


function parsePage(doc, i) {
  doc = addSeparator(doc);

  var footnotes = parseFootnote(doc);
  var entries = parseDoc(doc, footnotes);

  //TODO: check which entries are lost
  if (entries.length !== footnotes.length) {
    console.log(i, entries.length, footnotes.length);
  }

  return {
    index: i,
    entries: entries
  };
}


fs.readFile('loghat-dar-tafsire-nemune.json', 'utf8', function (err, data) {
  if (err) throw err;
  var obj = JSON.parse(data);

  var db = [];
  for (var i = 0; i < obj.length; i++) {
    var doc = obj[i].content;
    db.push(parsePage(doc, i));
  }
  fs.writeFileSync('loghat-dar-tafsire-nemune.db.json', JSON.stringify(db), 'utf8');
});





