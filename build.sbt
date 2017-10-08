lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "info.batey",
      scalaVersion := "2.12.3",
      version := "0.1.0-SNAPSHOT"
    )),
    organization := "info.batey",
    name := "gc-log-parser",
    wartremoverErrors ++= Warts.all
  )
  .aggregate(core, app)

val akkaVersion = "2.5.6"
val akkaHttpVersion = "10.0.10"

lazy val core = (project in file("core")).
  settings(
    name := "gc-stream",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.lightbend.akka" %% "akka-stream-alpakka-file" % "0.10",
      "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.6",

      "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
      "org.scalatest" %% "scalatest" % "3.0.1" % Test
    )
  )

lazy val app = (project in file("app"))
  .dependsOn(core)
  .settings(
    name := "app",
    libraryDependencies ++= Seq(
      "com.typesafe" % "config" % "1.3.1",
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "ch.megard" %% "akka-http-cors" % "0.2.1",

      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
      "org.scalatest" %% "scalatest" % "3.0.1" % Test
    )
  )

assemblyMergeStrategy in assembly := {
  case PathList(ps@_*) if ps.last endsWith ".properties" => MergeStrategy.concat
  case "app/src/main/resources/application.conf" => MergeStrategy.concat
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
