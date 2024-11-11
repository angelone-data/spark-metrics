lazy val scala212 = "2.12.12"
lazy val supportedScalaVersions = List(scala212)
lazy val ioPrometheusVersion = "0.15.0"

lazy val root = (project in file("."))
  .settings(
    name := "de-spark-metrics",
    organization := "com.angelone",
    organizationHomepage := Some(url("https://angelone.in/")),
    homepage := Some(url("https://github.com/angelone-data/de-spark-metrics")),
    developers := List(
      Developer("dvajaria", "Darsh Vajaria", "darsh.vajaria@angelbroking.com", url("https://github.com/dvajaria")),
      Developer("stoader", "Sebastian Toader", "st0ad3r@gmail.com", url("https://github.com/stoader")),
      Developer("sancyx", "Sandor Magyari", "sancyx@gmail.com", url("https://github.com/sancyx")),
      Developer("baluchicken", "Balint Molnar", "balintmolnar91@gmail.com", url("https://github.com/baluchicken"))
    ),
    scmInfo := Some(ScmInfo(url("https://github.com/angelone-data/de-spark-metrics"), "git@github.com:angelone-data/de-spark-metrics.git")),
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
    scalaVersion := scala212,
    crossScalaVersions := supportedScalaVersions,
    version      := "3.2-1.0.0",
    libraryDependencies ++= Seq(
      "io.prometheus" % "simpleclient" % ioPrometheusVersion,
      "io.prometheus" % "simpleclient_dropwizard" % ioPrometheusVersion,
      "io.prometheus" % "simpleclient_common" % ioPrometheusVersion,
      "io.prometheus" % "simpleclient_pushgateway" % ioPrometheusVersion,
      "io.dropwizard.metrics" % "metrics-core" % "4.2.9" % Provided,
      "io.prometheus.jmx" % "collector" % "0.17.0",
      "org.apache.spark" %% "spark-core" % "3.2.1" % Provided,
      "com.novocode" % "junit-interface" % "0.11" % Test,
      // Spark shaded jetty is not resolved in scala 2.11
      // Described in https://issues.apache.org/jira/browse/SPARK-18162?focusedCommentId=15818123#comment-15818123
      // "org.eclipse.jetty" % "jetty-servlet"  % "9.4.18.v20190429" % Test
    ),
    testOptions in Test := Seq(Tests.Argument(TestFrameworks.JUnit, "-a"))
  )


publishMavenStyle := true
useGpg := true

// Add sonatype repository settings
publishTo := Some(
  if (isSnapshot.value)
    Opts.resolver.sonatypeSnapshots
  else
    Opts.resolver.sonatypeStaging
)

