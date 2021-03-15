name := "denver"

version := "0.1"

scalaVersion := "2.13.4"

scalacOptions ++= Seq("-deprecation", "-feature")

val akkaVersion = "2.6.13"
val akkaHttpVersion = "10.2.4"
val logbackVersion = "1.2.3"
val mongoVersion = "4.2.2"
val jodaVersion = "2.10.10"
val jpathVersion = "2.5.0"

libraryDependencies ++= Seq(
  "org.mongodb.scala" %% "mongo-scala-driver" % mongoVersion,
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "joda-time" % "joda-time" % jodaVersion,
  "com.jayway.jsonpath" % "json-path" % jpathVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test
)
