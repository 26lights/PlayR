name := "playr-tutorial"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.7"

scalacOptions += "-language:reflectiveCalls"

libraryDependencies += cache

lazy val playr = RootProject(file("../.."))

lazy val playrTutorial = project in file(".") dependsOn playr enablePlugins PlayScala

