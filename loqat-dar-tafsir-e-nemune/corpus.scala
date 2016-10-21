import java.io.{File, PrintWriter}

import com.typesafe.scalalogging.LazyLogging
import org.jqurantree.arabic.ArabicText
import org.jqurantree.orthography.Document
import org.json4s.JsonAST.JArray

import scala.collection.SortedMap
import scala.collection.immutable.IndexedSeq
import scala.io.Source

case class Location(surah: Int,
                    ayah: Int,
                    token: Int,
                    subtoken: Int,
                    token_str: String,
                    token_raw_str: String)

object corpus extends App with LazyLogging {
  val corpus_url = "quranic-corpus-morphology-0.4.txt"
  val lines = Source.fromFile(corpus_url).getLines.toList.drop(57)

  //  logger.info("parsing corpus")

  val pattern = "\\((\\d+):(\\d+):(\\d+):(\\d+)\\).*?LEM:(.*?)\\|".r
  val words = lines.filter(_.contains("ROOT")).flatMap { row =>
    val matches = pattern.findAllMatchIn(row)
    matches.map { row =>
      (Location(row.group(1).toInt,
        row.group(2).toInt,
        row.group(3).toInt,
        row.group(4).toInt,
        Document.getChapter(row.group(1).toInt).getVerse(row.group(2).toInt).getToken(row.group(3).toInt).toString,
        Document.getChapter(row.group(1).toInt).getVerse(row.group(2).toInt).getToken(row.group(3).toInt).removeDiacritics().toString),
        ArabicText.fromBuckwalter(row.group(5)))
    }.toIndexedSeq
  }

  val sorted = words.groupBy { case (_, token) =>
    (token.toString, token.removeDiacritics().toString)
  }.map { case (token, list) =>
    (token, list.map(_._1))
  }.toIndexedSeq.sortBy(_._1.toString)

  val wordMap = SortedMap(sorted: _*)

  val groupedWordMap = wordMap.map { case ((lemma, _), locations) =>
    (lemma, locations.groupBy(_.token_str))
  }

  //  logger.info("creating json file")
  import org.json4s.JsonDSL._

  val jsonAst = {
    groupedWordMap.map { case (lemma, variants) =>
      ("lemma" -> lemma) ~ ("variants" ->
        variants.map { case (word, locs) =>
          ("word" -> word) ~ ("refs" -> locs.map { l =>
            JArray(List(l.ayah, l.token, l.subtoken))
          })
        })
    }
  }

  import org.json4s.jackson.JsonMethods._

  //  logger.info("writing to file")
  val writer = new PrintWriter(new File("lemma.words.json"))
  writer.write(pretty(jsonAst))
  writer.close()
}
