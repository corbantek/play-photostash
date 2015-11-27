name := "play-photostash"

version := "1.0"

lazy val `play-photostash` = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.6"

//libraryDependencies ++= Seq(javaJdbc, javaEbean, cache, javaWs)
libraryDependencies ++= Seq(
	"com.arangodb" % "arangodb-java-driver" % "2.5.4",
	"org.imgscalr" % "imgscalr-lib" % "4.2",
	"com.drewnoakes" % "metadata-extractor" % "2.8.1"
)

// Compile the project before generating Eclipse files, so that generated .scala or .class files for views and routes are present
EclipseKeys.preTasks := Seq(compile in Compile)
EclipseKeys.projectFlavor := EclipseProjectFlavor.Java           // Java project. Don't expect Scala IDE
EclipseKeys.createSrc := EclipseCreateSrc.ValueSet(EclipseCreateSrc.ManagedClasses, EclipseCreateSrc.ManagedResources)  // Use .class files instead of generated .scala files for views and routes 