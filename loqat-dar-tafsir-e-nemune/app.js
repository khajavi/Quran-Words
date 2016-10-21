var fs = require('fs');
var request = require('request');
var cheerio = require('cheerio');
var Q = require('q');
var async = require("async");
var Entities = require('html-entities').AllHtmlEntities;


function loadHTML(url) {
  var deffered = Q.defer();
  request(url, function (err, res, html) {
    if (!err) {
      deffered.resolve(html);
    } else {
      deffered.reject(err);
    }
  });
  return deffered.promise;
}


function loadMainContent(html) {
  var $ = cheerio.load(html);
  var html = $('#pnlContent').html();
  var entities = new Entities();
  var decoded = entities.decode(html);
  return decoded;
}


loadHTML('http://makarem.ir/main.aspx?reader=1&lid=0&mid=29925&catid=6509&pid=61913').then(function (html) {
  var $ = cheerio.load(html);
  var links = $('#pnlMenu ul').find('a');

  var asyncTasks = [];
  var list = new Array(links.length);
  console.log('links.length', links.length);

  links.each(function (i, elem) {
    var url = 'http://makarem.ir' + this.attribs.href;
    console.log(i, url);
    var title = $(this).text();
    asyncTasks.push(function (callback) {
      loadHTML(url).then(function (html) {
        var content = loadMainContent(html);
        console.log('index', i);
        console.log('title: ', title);
        console.log(url);
        console.log('content: ', content.slice(0, 150));
        console.log('-------------------');
        list[i] = {
          title: title,
          url: url,
          content: content
        };
        callback(null, {
          title: title,
          url: url,
          id:i
        });
      });
    });
  });


  async.parallel(asyncTasks, function (err, results) {
    var outputFilename = 'loghat-dar-tafsire-nemune.json';
    fs.writeFile(outputFilename, JSON.stringify(list, null, 4), function (err) {
      if (err) {
        console.log(err);
      } else {
        console.log("JSON saved to " + outputFilename);
      }
    });
    console.log(results);
  });
});

