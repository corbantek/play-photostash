name := "play-photostash"

version := "1.0"

lazy val `play-photostash` = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.1"

//libraryDependencies ++= Seq(javaJdbc, javaEbean, cache, javaWs)
libraryDependencies ++= Seq(
	"com.arangodb" % "arangodb-java-driver" % "2.5.4",
	"org.imgscalr" % "imgscalr-lib" % "4.2",
	"com.drewnoakes" % "metadata-extractor" % "2.8.1"
)