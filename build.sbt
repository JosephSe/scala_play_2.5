name := """Nova Monitoring"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  cache,
  ws,
  jdbc,
  filters,
  "com.typesafe.slick"        %%     "slick-hikaricp"           %      "3.1.1",
  "com.typesafe.slick"        %%    "slick"            	      %      "3.1.1",
  "org.apache.httpcomponents" % "httpclient"              % "4.5.1",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
  "org.mockito" % "mockito-all" % "1.10.19" % Test
)

fork in run := true