name := "playr-demo"

version := "1.0-SNAPSHOT"

play.Project.playScalaSettings

scalacOptions += "-language:reflectiveCalls"

lazy val playr = RootProject(file("../.."))

lazy val playrDemo = project in file(".") dependsOn playr

