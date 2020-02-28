name := "playr-demo"

version := "1.0-SNAPSHOT"

scalaVersion := "2.12.10"

scalacOptions += "-language:reflectiveCalls"

lazy val playr = RootProject(file("../.."))

lazy val playrDemo = project in file(".") dependsOn playr enablePlugins (PlayScala) settings (
  libraryDependencies += guice
)
