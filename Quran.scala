import java.io.{File, PrintWriter}
import java.util.regex.Pattern

import com.rockymadden.stringmetric.similarity.DiceSorensenMetric
import com.typesafe.scalalogging.LazyLogging
import org.json4s._
import org.json4s.jackson.JsonMethods._

import scala.collection.immutable.IndexedSeq
import scala.collection.mutable.ListBuffer
import scala.io.Source

case class Note(sooreh: Option[String], ayat: Option[List[Option[String]]], jeld: Option[String], pages: Option[List[String]])

case class Footnote(index: Option[Int], note: Option[List[Option[Note]]])

case class OriginalEntry(title: Option[String], aye: Option[String], description: Option[String], footnotes: Option[List[Option[Footnote]]])

case class Entry(title: Option[String], phrase: Option[String], description: Option[String], references: Option[ListBuffer[EntryRef]])

case class EntryRef(suraNumber: Int, ayahNumber: Option[Int], start: Option[Int], wordIndex: Option[Int])

case class OriginalEntries(index: Int, entries: Option[List[OriginalEntry]])

case class QuranSurahs(id: Int, name: String, lang: Option[String])

case class OriginalAyat(page: Int, text: String, raw: String, id: String)

case class Ayat(page: Int, text: String, raw: String, surahNumber: Int, ayahNumber: Int)

object QuranStudies extends App with LazyLogging {
  implicit def stringToString(s: String): StringUtils = new StringUtils(s)

  logger.info("getting entries")
  val dict = loghatname.getPreferedEntry

  logger.info("adding new entries")
  val newDict = dict.map { entry =>

    logger.info("processing " + entry.title)
    val matchedAyas = entry.title.map { title =>
      val matchedAyas = quran.getAyats.map { ayah =>
        val index = ayah.raw.indexOf(title.removePunctuation + " ")
        val wordIndex = ayah.raw.removePunctuation.split(" +").indexOf(title.removePunctuation)
        (index, wordIndex, ayah)
      }
      matchedAyas.filter(_._1 != -1).filter(_._2 != -1)
    }
    val newRefs = matchedAyas.map { list =>
      list.map { e =>
        val index = e._1
        val wordIndex = e._2
        val surahNumber = e._3.surahNumber
        val ayahNumber = e._3.ayahNumber
        EntryRef(surahNumber, Option(ayahNumber), Option(index), Option(wordIndex))
      }
    }
    entry.references.foreach { refs =>
      newRefs.foreach { newRefs =>
        refs ++= newRefs
      }
      val distinct = refs.distinct
      refs.clear
      refs ++= distinct
    }
    entry
  }

  import org.json4s.JsonDSL._

  logger.info("creating json database")
  val jsonAst = {
    newDict.map {
      s => ("title" -> s.title) ~ ("phrase" -> s.phrase) ~ ("description" -> s.description) ~ ("references" -> s.references.map(_.map { r =>
        ("surah" -> r.suraNumber) ~ ("ayah" -> r.ayahNumber) ~ ("start" -> r.start) ~ ("word" -> r.wordIndex)
      }))
    }
  }

  val writer = new PrintWriter(new File("words.db.json"))
  logger.info("writing to file")
  writer.write(pretty(jsonAst))
  writer.close()

  val refs = newDict.flatMap(_.references).flatten

  logger.info("number of refs: " + refs.length)
  logger.info("average number of refs per ayah: " + refs.length.toDouble / 6236)
  logger.info("word.db.json created.")
}

object loghatname {
  implicit val formats = DefaultFormats

  implicit def stringToString(s: String): StringUtils = new StringUtils(s)

  val url = "loghat-dar-tafsire-nemune.db.json"
  lazy val content = Source.fromFile(url).getLines.mkString
  lazy val jsonAst = parse(content)
  lazy val entries = jsonAst.extract[List[OriginalEntries]].flatMap(_.entries).flatten

  def getEntries = entries

  def getTitles = loghatname.getEntries.flatMap(_.title)

  def getPreferedEntry = entries.map { originalEntry =>
    val footnotes = originalEntry.footnotes
      .map(_.flatMap(_.flatMap(_.note))).map(_.flatten)


    val maybeMaybeRefses: Option[List[Option[List[EntryRef]]]] = footnotes.map { notes =>
      notes.flatMap {
        _.map {
          note =>
            note.ayat.map {
              _.map {
                aya =>
                  val surahNumber = quran.getSurahNumber(note.sooreh.get)

                  val word = if (originalEntry.title.isDefined) originalEntry.title.get.replaceAll("<p>", "").trim.removePunctuation else ""

                  val ayahNumber: Option[Int] = aya.map(_.toInt)

                  val start: Option[Int] = quran.getAya(surahNumber
                    , ayahNumber.getOrElse(-1)).map {
                    _.raw.removePunctuation.indexOf(word.removePunctuation)
                  }

                  val wordIndex: Option[Int] = quran.getAya(surahNumber, ayahNumber.getOrElse(-1)).map {
                    _.raw.removePunctuation.split(" +").indexOf(word.removePunctuation)
                  }

                  EntryRef(surahNumber, aya.map(_.toInt), start, wordIndex)
              }
            }
        }
      }
    }
    val maybeRefs = maybeMaybeRefses.map(_.flatten).map(_.flatten.to[ListBuffer])
    val a: ListBuffer[EntryRef] = ListBuffer[EntryRef]()



    val mayBeRefs = Option(ListBuffer[EntryRef]())
    val title = originalEntry.title.map(_.replace("<p>", "").trim)
    Entry(title, originalEntry.aye, originalEntry.description, maybeRefs)
  }
}

object quran {
  implicit val formats = DefaultFormats

  lazy val quran_db = "quran.json"
  lazy val quran_url = "http://zolal.s3-website-us-east-1.amazonaws.com/quran/all"
  lazy val content = Source.fromFile(quran_db).getLines.mkString

  val jsonAst = parse(content)
  val ayatsOriginal = jsonAst.extract[List[OriginalAyat]]
  val ayats = ayatsOriginal.map(aya => {
    val surah_ayah = aya.id.split("_")
    Ayat(aya.page, aya.text, aya.raw, surah_ayah(0).toInt, surah_ayah(1).toInt)
  })
  val quranMetadata = "quran-metadata.csv"
  val quranMetas = Source.fromFile(quranMetadata).getLines.filter(!_.startsWith("//")).map(_.split(",")).map {
    l =>
      QuranSurahs(l(0).toInt, l(1), if (l.size == 3) Option(l(2)) else None)
  }.toIndexedSeq.distinct

  def getAyats = ayats

  def getAya(surahNumber: Int, ayahNumber: Int) = ayats.find { e =>
    e.surahNumber == surahNumber && e.ayahNumber == ayahNumber
  }

  def correctSuraName(suraName: String) = {
    val corrigendum = Map("صافا" -> "صافات", "مائد" -> "مائده", "قص" -> "قصص", "فاط" -> "فاطر", "توب" -> "توبه", "اسرا" -> "اسراء", "نب" -> "نبأ", "هو" -> "هود", "واقع" -> "واقعه", "انعا" -> "انعام", "عب" -> "عبس", "احاقف" -> "احقاف", "انفا" -> "انفال", "عنکبو" -> "عنکبوت", "فج" -> "فجر", "فصّل" -> "فصلت", "زلزال" -> "الزلزال")
    corrigendum.get(suraName) match {
      case None => suraName
      case other => other.get
    }
  }

  def getSurahNumber(SurahName: String) = {
    implicit def stringToString(s: String): StringUtils = new StringUtils(s)

    val fixedName: String = correctSuraName(SurahName)
    val res: Option[Int] = quranMetas.find(r => r.name.clearNames == fixedName.clearNames).map(_.id)

    res match {
      case None =>
        val comparedAyaNames: IndexedSeq[(Int, String, Option[Double])] = quranMetas.map { a =>
          (a.id, a.name, DiceSorensenMetric(1).compare(a.name, SurahName))
        }
        val bestSimilarity: Int = comparedAyaNames.toList.sortBy(_._3).last._1
        println("nameee: " + SurahName)
        println("Noneee: " + comparedAyaNames.toList.sortBy(_._3).last._2)
        -1
      case other => other.get
    }
  }
}


class StringUtils(val s: String) {

  def removePunctuation = {
    val p = Pattern.compile("[\\p{P}\\p{Mn}]")
    val m = p.matcher(s)
    m.replaceAll("")
  }


  def clearNames = {
    s.trim.replaceAll("ي", "ی").replaceAll("ك", "ک").replaceFirst("ال", "").replaceAll("ّ", "").replaceAll("ة$", "ه").replaceAll("ی", "ی").replaceAll("ى", "ی")
  }

  def normalize = {
    s.trim.replaceAll("ي", "ی").replaceAll("ك", "ک").replaceAll("ّ", "").replaceAll("ی", "ی").replaceAll("ى", "ی").replaceAll("آ", "ا")
  }

}

object util {

  import java.io.File
  import java.net.URL

  import sys.process._

  def download(url: String, filename: String) = {
    new URL(url) #> new File(filename) !!
  }
}
