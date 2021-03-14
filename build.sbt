lazy val akkaHttpVersion = "10.2.4"
lazy val akkaVersion    = "2.6.13"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization    := "com.fedex",
      scalaVersion    := "2.13.4"
    )),
    name := "NT-digital",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"                % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json"     % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion,
      "com.typesafe.akka" %% "akka-stream"              % akkaVersion,
      "ch.qos.logback"    % "logback-classic"           % "1.2.3",

      "com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"                % "3.1.4"         % Test,

      // Cats
      "org.typelevel" %% "cats-core" % "2.1.0",
      "org.typelevel" %% "cats-mtl-core" % "0.7.0",
      "org.typelevel" %% "cats-effect" % "2.0.0",
      "org.typelevel" %% "cats-tagless-macros" % "0.10",
      "org.typelevel" %% "cats-effect-laws" % "2.0.0" % Test,
      "org.typelevel" %% "cats-laws" % "2.1.0",
      "org.typelevel" %% "cats-testkit-scalatest" % "1.0.0-RC1" % Test,
      "dev.profunktor" %% "console4cats" % "0.8.1",
      "dev.profunktor" %% "redis4cats-effects" % "0.9.1",
      "dev.profunktor" %% "redis4cats-log4cats" % "0.9.1",
      "io.chrisdavenport" %% "log4cats-noop" % "1.0.1",
      "org.typelevel" %% "simulacrum" % "1.0.1",

      // Sttp
      "com.softwaremill.sttp.client3" %% "core" % "3.1.7",
      "com.softwaremill.sttp.client3" %% "async-http-client-backend-future" % "3.1.7"
    ),
    scalacOptions ++=Seq(
      "-encoding", "utf8",
      "-Xfatal-warnings",
      "-deprecation",
      "-unchecked",
      "-language:implicitConversions",
      "-language:higherKinds",
      "-language:existentials",
      "-language:postfixOps",
      "-Ymacro-annotations"
    )
  )