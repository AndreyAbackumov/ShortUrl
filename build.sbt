val scala3Version = "3.9.0"
val Http4sVersion = "0.23.30"
val CirceVersion = "0.14.14"
val MunitVersion = "1.1.1"
val LogbackVersion = "1.5.18"
val MunitCatsEffectVersion = "2.1.0"
val skunkVersion = "1.0.0"

lazy val root = project
  .in(file("."))
  .settings(
    name := "MyFirstWebProject",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies += "org.tpolecat" %% "skunk-core" % skunkVersion,

    libraryDependencies ++= Seq(
      "org.http4s"      %% "http4s-ember-server" % Http4sVersion,
      "org.http4s"      %% "http4s-ember-client" % Http4sVersion,
      "org.http4s"      %% "http4s-circe"        % Http4sVersion,
      "org.http4s"      %% "http4s-dsl"          % Http4sVersion,
      "org.scalameta"   %% "munit"               % MunitVersion           % Test,
      "org.typelevel"   %% "munit-cats-effect"   % MunitCatsEffectVersion % Test,
      "ch.qos.logback"  %  "logback-classic"     % LogbackVersion         % Runtime,
      "io.circe"        %% "circe-generic"       % CirceVersion,
      "org.http4s" %% "http4s-ember-server" % "0.23.36",
      "org.scalatest" %% "scalatest" % "3.2.19" % Test,
      "com.dimafeng" %% "testcontainers-scala-scalatest" % "0.41.4" % Test,
      "com.dimafeng" %% "testcontainers-scala-postgresql" % "0.41.4" % Test,
      "org.http4s" %% "http4s-circe" % "0.23.27" % Test,
      "org.http4s" %% "http4s-dsl"   % "0.23.27" % Test,
      "io.circe" %% "circe-parser" % "0.14.10" % Test,
      "org.tpolecat" %% "natchez-core" % "0.3.5"
    ),
  )