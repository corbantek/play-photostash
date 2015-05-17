name := "play-photostash"

version := "1.0"

lazy val `play-photostash` = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.1"

//libraryDependencies ++= Seq(javaJdbc, javaEbean, cache, javaWs)
libraryDependencies ++= Seq(
	"com.arangodb" % "arangodb-java-driver" % "2.5.4"
)