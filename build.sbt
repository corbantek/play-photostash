name := "play-photostash"

version := "1.0"

lazy val `play-photostash` = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(javaJdbc, javaEbean, cache, javaWs)

unmanagedResourceDirectories in Test <+= baseDirectory(_ / "target/web/public/test")