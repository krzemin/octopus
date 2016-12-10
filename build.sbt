name := "octopus"

version := "1.0"

scalaVersion := "2.12.1"

libraryDependencies ++= Seq(
  "com.chuusai" %% "shapeless" % "2.3.2",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test"
)

scalacOptions ++= Seq(
  "-language:higherKinds",
  "-feature"
)
