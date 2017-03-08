publishTo := {
  val isSnapshot = version.value.contains("-SNAPSHOT")
  val repo = "http://build.26lights.net/nexus/content/repositories/public-"
  val (name, url) = if (isSnapshot)
    ("snapshots", repo + "snapshots")
  else
    ("releases", repo + "releases")
  Some(name at url)
}

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

