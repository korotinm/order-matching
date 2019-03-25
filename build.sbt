import sbt.Keys._

lazy val slf4jVersion = "1.7.25"
scalaVersion in ThisBuild := "2.12.8"

resolvers += "Akka Snapshots" at "https://repo.akka.io/snapshots/"

lazy val commonDependencies = Seq(
  "org.scala-lang.modules" %% "scala-java8-compat" % "0.8.0",
  "net.codingwell" %% "scala-guice" % "4.2.2",
  "com.beachape" %% "enumeratum" % "1.5.12",
  "com.typesafe.akka" %% "akka-actor-typed" % "2.5.21",
  "org.scalatest" %% "scalatest" % "3.0.1" % Test,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % "2.5.21" % Test
)

lazy val commonSettings = Seq(
  organization := "noname",
  version := "0.1-SNAPSHOT",
  testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-W", "30", "30"),
  javaOptions in (Test, run) += "-Xmx128G -Xms128G",
  scalacOptions ++= List(
    "-encoding",
    "UTF-8",
    "-target:jvm-1.8",
    "-unchecked",
    "-Ywarn-unused",
    "-deprecation",
    "-Ypartial-unification"
  ),
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
  resolvers += "Typesafe" at "http://repo.typesafe.com/typesafe/releases/",
  libraryDependencies ++= commonDependencies
)

lazy val exchange = (project in file("exchange"))
  .settings(commonSettings)
  .settings(
    parallelExecution in Test := false
  )

val ordermatching = (project in file("."))
  .settings(commonSettings)
  .aggregate(exchange)
