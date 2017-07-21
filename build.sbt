import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "info.batey",
      scalaVersion := "2.12.2",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "gc-log-parser",
    libraryDependencies += scalaTest % Test
  )

val akkaVersion = "2.5.3"
val akkaHttpVersion = "10.0.9"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,

  "com.lightbend.akka" %% "akka-stream-alpakka-file" % "0.10",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.6"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.0.9",
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test
)

libraryDependencies +=
  "com.typesafe.akka" %% "akka-persistence" % akkaVersion

libraryDependencies += "com.typesafe.akka" %% "akka-persistence-cassandra" % "0.29"