name := "CatsEffect"

version := "0.1"

scalaVersion := "2.13.7"

lazy val catsEffect = (project in file("."))
  .settings(
    name := "CatsEffect"
  )
  .aggregate(telegramBot, simpleEffects)
lazy val telegramBot = (project in file("TelegramBots"))
  .settings(
    libraryDependencies ++= Seq("org.augustjune" %% "canoe" % "0.5.1",
      "org.scalaj" %% "scalaj-http" % "2.4.2"),
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-parser"
    ).map(_ % "0.14.1")
  )
lazy val simpleEffects = (project in file("SimpleEffects"))
  .settings(
    libraryDependencies += "org.typelevel" %% "cats-effect" % "3.2.9" withSources () withJavadoc (),
    scalacOptions ++= Seq(
      "-feature",
      "-deprecation",
      "-unchecked",
      "-language:postfixOps"
    ),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
  )
