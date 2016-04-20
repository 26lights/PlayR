name := "playr"

organization := "26lights"

scalaVersion := "2.11.8"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

scalacOptions += "-feature"

scalacOptions += "-deprecation"

scalacOptions += "-unchecked"

fork in Test := true

libraryDependencies ++= Seq (
  "com.typesafe.play"      %% "play"               % "2.5.0",
  // test scope
  "com.typesafe.play"      %% "play-test"          % "2.5.0" % "test",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % "test"
)

site.settings

site.sphinxSupport()

site.includeScaladoc("api")
