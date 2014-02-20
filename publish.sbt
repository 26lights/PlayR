publishTo := {
  val isSnapshot = version.value.contains("-SNAPSHOT")
  val repo = "http://build.26source.org/nexus/content/repositories/"
  val (name, url) = if (isSnapshot)
    ("snapshots", repo + "snapshots")
  else
    ("releases", repo + "releases")
  Some(Resolver.url(name, new URL(url))(Resolver.ivyStylePatterns))
}

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

