var fs = require('fs');

function Ref() {
  this.sooreh = null;
  this.ayat = new Array();
  this.jeld = null;
  this.pages = new Array();
}


fs.readFile('/home/milad/workspace/source/darkoob/src/loghat/loghat-dar-tafsire-nemune.db.json', 'utf8', function (err, data) {
  if (err) throw err;
  var obj = JSON.parse(data);

  var fn = new Array();
  var entries = new Array();
  obj.forEach(function (e) {
    e.entries.forEach(function (entry) {
      fn.push(entry.footnotes[0]);
      if (entry.footnotes.length > 1) {
        //console.log(entry.footnotes)
      }
    })
  });


  function parseFootnote(footnote) {
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

  fn.forEach(function (e) {

    if (e != null && e.hasOwnProperty('note')) {
      //console.log(e.note);
      var entries = e.note.split(";");
      entries = entries.map(function (ent) {
        ent = ent.replace(/<br>/g, '');
        return ent.trim();
      });

      entries = entries.map(function (e) {
        var fnote = parseFootnote(e);
        return fnote;
      });


      console.log(entries);
      //console.log("\n")

    } else {
      console.log(e);
    }
  });


  //var footnotes = entries.map(function(e) {
  //  return e.footnotes;
  //});

  //console.log(footnotes)


});

