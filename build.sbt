// *****************************************************************************
// Library dependencies
// *****************************************************************************

lazy val library =
  new {

    object Version {
      val zio = "1.0.0-RC18-2"
      val zioConfig = "1.0.0-RC16-2"
    }

    val zio = "dev.zio" %% "zio" % Version.zio
    val zioTest = "dev.zio" %% "zio-test" % Version.zio
    val zioTestSbt = "dev.zio" %% "zio-test-sbt" % Version.zio

    val zioConfig = "dev.zio" %% "zio-config" % Version.zioConfig
    val zioConfigTypesafe = "dev.zio" %% "zio-config-typesafe" % Version.zioConfig
    val zioConfigMagnolia = "dev.zio" %% "zio-config-magnolia" % Version.zioConfig
  }

// *****************************************************************************
// Projects
// *****************************************************************************

lazy val root =
  project
    .in(file("."))
    .settings(settings)
    .settings(
      libraryDependencies ++= Seq(
        library.zio,
        library.zioConfig,
        library.zioConfigTypesafe,
        library.zioConfigMagnolia,
        library.zioTest % Test,
        library.zioTestSbt % Test
      ),
      testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
    )

// *****************************************************************************
// Settings
// *****************************************************************************

lazy val settings =
  commonSettings ++
    commandAliases

lazy val commonSettings =
  Seq(
    name := "zio-properties",
    scalaVersion := "2.13.1",
    organization := "com.example"
  )

lazy val commandAliases =
  addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt") ++
    addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")


// *****************************************************************************
// Scalac
// *****************************************************************************

lazy val stdOptions = Seq(
  "-encoding",
  "UTF-8",
  "-explaintypes",
  "-Yrangepos",
  "-feature",
  "-language:higherKinds",
  "-language:existentials",
  "-Xlint:_,-type-parameter-shadow",
  "-Xsource:2.13",
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard",
  "-unchecked",
  "-deprecation",
  "-Xfatal-warnings"
)

lazy val stdOpts213 = Seq(
  "-Wunused:imports",
  "-Wvalue-discard",
  "-Wunused:patvars",
  "-Wunused:privates",
  "-Wunused:params",
  "-Wvalue-discard"
)

scalacOptions := stdOptions ++ stdOpts213

useGpg := true


ThisBuild / organization := "com.adrianfilip.zio-properties"
ThisBuild / organizationName := "adrianfilip"
ThisBuild / organizationHomepage := Some(url("http://adrianfilip.com/"))

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/adrianfilip/zio-properties"),
    "scm:git@github.com:adrianfilip/zio-properties.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id    = "adrianfilip",
    name  = "Adrian Filip",
    email = "realadrianfilip@gmail.com",
    url   = url("https://adrianfilip.com")
  )
)

ThisBuild / description := "Config ZIO apps from multiple property sources having a standard property resolution order."
ThisBuild / licenses := List("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))
ThisBuild / homepage := Some(url("https://github.com/adrianfilip/zio-properties"))

// Remove all additional repository other than Maven Central from POM
ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
ThisBuild / publishMavenStyle := true