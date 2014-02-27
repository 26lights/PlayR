name := "playr-tutorial"

version := "1.0-SNAPSHOT"

play.Project.playScalaSettings

scalacOptions += "-language:reflectiveCalls"

libraryDependencies += cache

lazy val playr = RootProject(file("../.."))

lazy val playrTutorial = project in file(".") dependsOn playr

