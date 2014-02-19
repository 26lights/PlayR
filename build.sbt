name := "playr"

version := "0.1.0"

organization := "26lights"

scalaVersion := "2.10.3"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

scalacOptions += "-feature"

scalacOptions += "-deprecation"

scalacOptions += "-unchecked"

libraryDependencies ++= Seq (
  "com.typesafe.play"  %% "play"  % "2.2.1"
)

