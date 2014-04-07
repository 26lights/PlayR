name := "playr"

version := "0.1.0-SNAPSHOT"

organization := "26lights"

scalaVersion := "2.10.3"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

scalacOptions += "-feature"

scalacOptions += "-deprecation"

scalacOptions += "-unchecked"

fork in Test := true

libraryDependencies ++= Seq (
  "com.typesafe.play"  %% "play"       % "2.2.1",
  // test scope
  "com.typesafe.play"  %% "play-test"  % "2.2.1"  % "test",
  "org.scalatest"      %% "scalatest"  % "2.0"    % "test"
)

site.settings

site.sphinxSupport()
