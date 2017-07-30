import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "info.batey",
      scalaVersion := "2.12.2",
      version := "0.1.0-SNAPSHOT"
    )),
    name := "gc-log-parser",
    libraryDependencies += scalaTest % Test
  )

val akkaVersion = "2.5.3"
val akkaHttpVersion = "10.0.9"

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.3.1",
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
  "com.lightbend.akka" %% "akka-stream-alpakka-file" % "0.10",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.6",
  "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-cassandra" % "0.29",
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
  "ch.megard" %% "akka-http-cors" % "0.2.1"
)


assemblyMergeStrategy in assembly := {
  case PathList(ps@_*) if ps.last endsWith ".properties"  => MergeStrategy.concat
  case "application.conf"                           => MergeStrategy.concat
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
