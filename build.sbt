
val scalaVer = "2.13.5"
val crossScalaVer = Seq(scalaVer)

lazy val commonSettings = Seq(
  name         := "confused-scala",
  description  := "Similar tool as visma-prodsec/confused, for Scala (and Java)",
  organization := "eu.cdevreeze.confused-scala",
  version      := "0.1.0-SNAPSHOT",

  scalaVersion       := scalaVer,
  crossScalaVersions := crossScalaVer,

  ThisBuild / scalacOptions ++= Seq("-Wconf:cat=unused-imports:w,cat=unchecked:w,cat=deprecation:w,cat=feature:w,cat=lint:w"),

  Test / publishArtifact := false,
  publishMavenStyle := true,

  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },

  pomExtra := pomData,
  pomIncludeRepository := { _ => false },

  libraryDependencies += "io.get-coursier" %% "coursier" % "2.0.12",

  libraryDependencies += "com.typesafe" % "config" % "1.4.1"
)

lazy val root = project.in(file("."))
  .settings(commonSettings: _*)

lazy val pomData =
  <url>https://github.com/dvreeze/confused-scala</url>
  <licenses>
    <license>
      <name>Apache License, Version 2.0</name>
      <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
      <distribution>repo</distribution>
      <comments>Confused-Scala is licensed under Apache License, Version 2.0</comments>
    </license>
  </licenses>
  <scm>
    <connection>scm:git:git@github.com:dvreeze/confused-scala.git</connection>
    <url>https://github.com/dvreeze/confused-scala.git</url>
    <developerConnection>scm:git:git@github.com:dvreeze/confused-scala.git</developerConnection>
  </scm>
  <developers>
    <developer>
      <id>dvreeze</id>
      <name>Chris de Vreeze</name>
      <email>chris.de.vreeze@caiway.net</email>
    </developer>
  </developers>
