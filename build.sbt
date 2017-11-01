lazy val doobieVersion = "0.4.4"

lazy val baseSettings = Seq(
  scalaVersion := "2.12.3",
  organization := "com.devim",
  version := "0.1.0-SNAPSHOT",
  resolvers ++= Seq(
    "Sonatype Nexus" at "https://nexus.devim.team/repository/maven-public/",
    "Central Proxy " at "https://nexus.devim.team/repository/maven-central/")
)

lazy val publishSettings = Seq(
  publishArtifact in Test := false,
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://nexus.devim.team/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "repository/maven-snapshots/")
    else
      Some("releases" at nexus + "repository/maven-releases/")
  },
  credentials += Credentials(Path.userHome / ".sbt" / ".credentials")
)

lazy val doobiePostgresProto = (project in file("."))
  .settings(baseSettings)
  .settings(publishSettings)
  .settings(
    name := "doobie-postgres-proto",
    libraryDependencies ++= Seq(
      "com.devim" %% "proto-utils" % "0.1.0-SNAPSHOT",
      //doobie (database)
      "org.tpolecat" %% "doobie-core-cats" % doobieVersion,
      "org.tpolecat" %% "doobie-postgres-cats" % doobieVersion,
      //test
      "com.dimafeng" %% "testcontainers-scala" % "0.7.0" % "test",
      "org.testcontainers" % "postgresql" % "1.4.3%"test"",
      "org.scalatest" %% "scalatest" % "3.0.4" % "test"
    )
  )
