name := "loghat"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies += "io.spray" %% "spray-json" % "1.3.2"
libraryDependencies ++= Seq(
  "org.json4s" %% "json4s-core" % "3.3.0",
  "org.json4s" %% "json4s-jackson" % "3.3.0",
  "org.apache.commons" % "commons-lang3" % "3.4",
  "com.rockymadden.stringmetric" %% "stringmetric-core" % "0.27.4",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "org.slf4j" % "slf4j-api" % "1.7.19",
  "com.typesafe.scala-logging" %% "scala-logging-slf4j" % "2.1.2",
  "org.slf4j" % "log4j-over-slf4j" % "1.7.1",
  "ch.qos.logback" % "logback-classic" % "1.0.3"
)

scalaSource in Compile := baseDirectory.value / "."

