

lazy val root = project.in(file("."))
  .settings(coreSettings: _*)
  .settings(noPublishSettings: _*)
  .aggregate(octopusJVM, octopusJS)
  .dependsOn(octopusJVM, octopusJS)

lazy val versions = new {
  val shapeless = "2.3.2"
  val scalatest = "3.0.1"
}

lazy val dependencies = Seq(
  libraryDependencies += "com.chuusai" %%% "shapeless" % versions.shapeless,
  libraryDependencies += "org.scalatest" %%% "scalatest" % versions.scalatest % "test"
)

lazy val octopus = crossProject.crossType(CrossType.Pure)
  .settings(
    moduleName := "octopus", name := "octopus",
    description := "Boilerplate-free validation library for Scala"
  )
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(dependencies: _*)

lazy val octopusJVM = octopus.jvm

lazy val octopusJS = octopus.js


lazy val coreSettings = commonSettings ++ publishSettings

lazy val commonSettings = Seq(
  organization := "com.github.krzemin",
  scalaVersion := "2.12.1",
  scalacOptions := commonScalacOptions
)

lazy val commonScalacOptions = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:higherKinds",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-Xfuture"
)

lazy val publishSettings = Seq(
  homepage := Some(url("https://github.com/krzemin/octopus")),
  licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  scmInfo := Some(ScmInfo(url("https://github.com/krzemin/octopus"), "scm:git:git@github.com:krzemin/octopus.git")),
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  pomExtra := (
    <developers>
      <developer>
        <id>krzemin</id>
        <name>Piotr Krzemiński</name>
        <url>http://github.com/krzemin</url>
      </developer>
    </developers>
    )
)

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)
