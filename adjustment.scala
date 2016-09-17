import scala.io.Source

object adjustment extends App {

  val a: List[(Int, Int, Int)] = quran
    .getAyats
    .map(a => (a.surahNumber, a.ayahNumber, a.raw.split(" ").length))


  val uthmani = "/home/milad/Downloads/quran-uthmani.txt"

  val b: List[(Int, Int, Int)] = Source.fromFile(uthmani)
    .getLines()
    .map(_.split("\\|"))
    .filter(_.length == 3)
    .map(a => (a(0).toInt, a(1).toInt, a(2).split(" ").length)).toList


  val noteq = (a zip b).count(a => a._1 != a._2)
  val all = a.length

  println(noteq, all, noteq.toDouble / all.toDouble)


  val res = Source.fromFile(uthmani)
    .getLines()
    .map(_.split("\\|"))
    .filter(_.length == 3)
    .map(a => (a(0).toInt, a(1).toInt, a(2).split( """((?=وَ)|(?<=وَ))""").flatMap(_.split(" ")).toList.length)).toList


  val noteq2 = (a zip res).count(a => a._1 != a._2)
  val all2 = a.length

  println(noteq2, all2, noteq2.toDouble / all2.toDouble)

  //  res.foreach(println)


}

case class Address(surah: Int,
                    ayah: Int,
                    token: Int,
                    subtoken: Int)

object vaCollection extends App {
  val corpus_url = "quranic-corpus-morphology-0.4.txt"
  val lines = Source.fromFile(corpus_url).getLines.toList.drop(57)

  val pattern = "\\((\\d+):(\\d+):(\\d+):(\\d+)\\).*".r

  val c = lines
    .filter(_.contains("wa\tREM\tPREFIX|w:REM+"))
    .map { row =>
      val res = row.substring(1, 8).split(":")
      Address(res(0).toInt, res(1).toInt, res(2).toInt, res(3).toInt)
    }
  println("count: " + c)
}
