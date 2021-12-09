name := "denver"

version := "0.1"

scalaVersion := "2.13.4"

scalacOptions ++= Seq("-deprecation", "-feature")

val akkaVersion = "2.6.17"
val akkaHttpVersion = "10.2.6"
val logbackVersion = "1.2.7"
val mongoVersion = "4.4.0"
val jodaVersion = "2.10.13"

libraryDependencies ++= Seq(
  "org.mongodb.scala" %% "mongo-scala-driver" % mongoVersion,
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "joda-time" % "joda-time" % jodaVersion,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test
)
