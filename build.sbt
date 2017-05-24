name := "akka-http-validation"

version := "0.1"

scalaVersion := "2.12.2"

libraryDependencies ++= {
  val akkaHttpVersion = "10.0.6"
  Seq(
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,

    "org.scalatest"     %% "scalatest" % "3.0.1" % "test",
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test"
  )
}