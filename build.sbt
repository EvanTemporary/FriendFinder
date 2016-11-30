import Keys._

name := "FriendFinder"

version := "1.0"

lazy val `friendFiner` = (project in file(".")).enablePlugins(PlayScala)

// Default port is 3000
PlayKeys.devSettings := Seq("play.server.http.port" -> "3000")

scalaVersion := "2.11.7"

resolvers ++= Seq(
  "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= Seq(
  jdbc, cache , ws,
  "org.json4s" %% "json4s-native" % "3.5.0"
)
