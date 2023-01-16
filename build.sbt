ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.2.1"

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-swing" % "3.0.0",
  "org.json4s" %% "json4s-native" % "4.0.6"
)

lazy val root = (project in file("."))
  .settings(
    name := "PTCGToolScalaEdition",
    idePackagePrefix := Some("ptcgtool")
  )
