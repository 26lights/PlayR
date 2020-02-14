publishTo in ThisBuild := Some(
  Resolver.file("file", sys.env.get("DIST_PATH").map(file).getOrElse(target.value / "dist"))
)
publishMavenStyle in ThisBuild := true

// Compile / packageDoc / publishArtifact := false
// Compile / packageSrc / publishArtifact := false
