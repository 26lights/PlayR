name := "playr"

organization := "26lights"

scalaVersion := "2.11.12"
// scalaVersion := "2.12.10"

resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

scalacOptions += "-feature"

scalacOptions += "-deprecation"

scalacOptions += "-unchecked"

fork in Test := true

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play" % "2.6.25",
  // test scope
  "com.typesafe.play" %% "play-test" % "2.6.25" % "test",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % "test"
)

enablePlugins(SphinxPlugin, SiteScaladocPlugin)

siteSubdirName in SiteScaladoc := "api"
