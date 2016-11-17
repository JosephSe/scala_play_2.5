

name := """NovaMonitoring"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  cache,
  ws,
  jdbc,
  filters,
//  "com.typesafe.slick"        %%     "slick-hikaricp"           %      "3.1.1",
  "com.typesafe.slick"        %%    "slick"            	      %      "3.1.1",
  "org.apache.httpcomponents" % "httpclient"              % "4.5.1",
  "org.apache.httpcomponents" % "httpcore" % "4.4.2",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "org.mockito" % "mockito-all" % "1.10.19" % Test
)

fork in run := true

assemblyMergeStrategy in assembly := {
//    case PathList("com.zaxxer", "HikariCP", xs@_*) => MergeStrategy.first
  //  case PathList(ps @ _*) if ps.last endsWith ".html" => MergeStrategy.first
  //  case "unwanted.txt" => MergeStrategy.discard
  case x if x.endsWith("io.netty.versions.properties") => MergeStrategy.first
  case x if x.endsWith("LogConfigurationException.class") => MergeStrategy.first
  case x if x.endsWith("Log.class") => MergeStrategy.first
  case x if x.endsWith("LogFactory.class") => MergeStrategy.first
  case x if x.endsWith("SimpleLog$1.class") => MergeStrategy.first
  case "application.conf" => MergeStrategy.concat
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
