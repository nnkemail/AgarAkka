name := """AgarAkka"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"
//scalaVersion := "2.11.7"
ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }

libraryDependencies ++= Seq(
  ws,
  specs2 % Test,
  "com.typesafe.akka" %% "akka-agent"   % "2.4.0",
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.0",
  "net.codingwell" %% "scala-guice" % "4.0.0",
   "net.ceedubs" %% "ficus" % "1.1.2",
  "com.typesafe.play" %% "play-slick" % "1.0.1",
  "com.typesafe.play" %% "play-slick-evolutions" % "1.0.1",
  "com.mohiva" % "play-silhouette_2.11" % "3.0.4",
  "com.mohiva" % "play-silhouette-testkit_2.11" % "3.0.4",
  "com.h2database" % "h2" % "1.4.188",
  "com.restfb" % "restfb" % "1.18.0",
  cache,
  evolutions
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
resolvers += "Atlassian Releases" at "https://maven.atlassian.com/public/"
resolvers += "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

EclipseKeys.preTasks := Seq(compile in Compile)


fork in run := false