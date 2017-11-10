lazy val root = project.in(file("."))
  .settings(coreSettings: _*)
  .settings(noPublishSettings: _*)
  .aggregate(octopusJVM, octopusJS, octopusCatsJVM, octopusCatsJS, octopusScalazJVM, octopusScalazJS)
  .dependsOn(octopusJVM, octopusJS, octopusCatsJVM, octopusCatsJS, octopusScalazJVM, octopusScalazJS)

lazy val versions = new {
  val scala = "2.12.4"
  val shapeless = "2.3.2"
  val scalatest = "3.0.4"
  val cats = "0.9.0"
  val scalaz = "7.2.16"
}

lazy val dependencies = Seq(
  libraryDependencies += "com.chuusai" %%% "shapeless" % versions.shapeless,
  libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,
  libraryDependencies += "org.scalatest" %%% "scalatest" % versions.scalatest % "test"
)

lazy val octopus = crossProject.crossType(CrossType.Pure)
  .settings(
    moduleName := "octopus",
    name := "octopus",
    description := "Boilerplate-free validation library for Scala"
  )
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(dependencies: _*)

lazy val octopusJVM = octopus.jvm
lazy val octopusJS = octopus.js

lazy val octopusCats = crossProject.crossType(CrossType.Pure)
  .settings(
    moduleName := "octopus-cats", name := "octopus-cats",
    description := "Cats integration for Octopus validation library"
  )
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(dependencies: _*)
  .settings(
    libraryDependencies += "org.typelevel" %%% "cats-core" % versions.cats % "test,provided"
  )
  .dependsOn(octopus % "compile->compile;test->test")


lazy val octopusCatsJVM = octopusCats.jvm
lazy val octopusCatsJS = octopusCats.js

lazy val octopusScalaz = crossProject.crossType(CrossType.Pure)
  .settings(
    moduleName := "octopus-scalaz", name := "octopus-scalaz",
    description := "Scalaz integration for Octopus validation library"
  )
  .settings(commonSettings: _*)
  .settings(publishSettings: _*)
  .settings(dependencies: _*)
  .settings(
    libraryDependencies += "org.scalaz" %%% "scalaz-core" % versions.scalaz % "test,provided"
  )
  .dependsOn(octopus % "compile->compile;test->test")


lazy val octopusScalazJVM = octopusScalaz.jvm
lazy val octopusScalazJS = octopusScalaz.js


lazy val coreSettings = commonSettings ++ publishSettings

lazy val commonSettings = Seq(
  scalaVersion := versions.scala,
  scalacOptions := commonScalacOptions
) ++ lintUnused

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

lazy val lintUnused = Seq(
  scalacOptions ++= {
    if (scalaVersion.value == "2.12.1") Seq() else Seq("-Xlint:-unused")
  }
)

lazy val publishSettings = Seq(
  organization := "com.github.krzemin",
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
