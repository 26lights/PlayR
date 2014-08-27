name := "playr-demo"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.2"

scalacOptions += "-language:reflectiveCalls"

lazy val playr = RootProject(file("../.."))

lazy val playrDemo = project in file(".") dependsOn playr enablePlugins(PlayScala)

