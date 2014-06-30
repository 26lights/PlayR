name := "playr"

releaseSettings

organization := "26lights"

scalaVersion := "2.10.4"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

scalacOptions += "-feature"

scalacOptions += "-deprecation"

scalacOptions += "-unchecked"

fork in Test := true

ReleaseKeys.versionBump := sbtrelease.Version.Bump.Minor

libraryDependencies ++= Seq (
  "com.typesafe.play"  %% "play"       % "2.2.3",
  // test scope
  "com.typesafe.play"  %% "play-test"  % "2.2.3"  % "test",
  "org.scalatest"      %% "scalatest"  % "2.2.0"  % "test"
)

site.settings

site.sphinxSupport()
