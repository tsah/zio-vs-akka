val ZioVersion    = "1.0.0-RC17"
val Specs2Version = "4.7.0"
val akkaVersion   = "2.6.1"

resolvers += Resolver.sonatypeRepo("releases")
resolvers += Resolver.sonatypeRepo("snapshots")

lazy val root = (project in file("."))
  .settings(
    organization := "tsah",
    name := "akka-zio",
    version := "0.0.1",
    scalaVersion := "2.13.1",
    maxErrors := 3,
    libraryDependencies ++= Seq(
      "dev.zio"    %% "zio"         % ZioVersion,
      "org.specs2" %% "specs2-core" % Specs2Version % "test",
      "com.typesafe.akka" %% "akka-actor" % "2.6.1"
    )
  )

// Refine scalac params from tpolecat
scalacOptions --= Seq(
  "-Xfatal-warnings"
)

//addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("chk", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")
