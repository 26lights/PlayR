publishTo := {
  val isSnapshot = version.value.contains("-SNAPSHOT")
  val repo = "http://build.26source.org/nexus/content/repositories/"
  val (name, url) = if (isSnapshot)
    ("snapshots", repo + "snapshots")
  else
    ("releases", repo + "releases")
  Some(name at url)
}

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

