// This file is used to build the sbt project with Databricks Connect.
// This also includes the instructions on how to to create the jar uploaded via databricks bundle
scalaVersion := "2.13.16"

name := "my_test_bundle"
organization := "com.examples"
version := "0.1"

libraryDependencies ++= List(
  "org.apache.spark" %% "spark-sql" % "4.0.0" % "provided",
  "org.apache.spark" %% "spark-core" % "4.0.0" % "provided"
)
//"com.databricks" %% "databricks-connect" % "17.0.+" % "provided"
libraryDependencies += "org.slf4j" % "slf4j-simple" % "2.0.16"

assembly / assemblyOption ~= { _.withIncludeScala(false) }
assembly / assemblyExcludedJars := {
  val cp = (assembly / fullClasspath).value
  cp filter { _.data.getName.matches("scala-.*") } // remove Scala libraries
}

assemblyMergeStrategy := {
  case _ => MergeStrategy.preferProject
}

// to run with new jvm options, a fork is required otherwise it uses same options as sbt process
fork := true
javaOptions += "--add-opens=java.base/java.nio=ALL-UNNAMED"

// To ensure logs are written to System.out by default and not System.err
javaOptions += "-Dorg.slf4j.simpleLogger.logFile=System.out"
