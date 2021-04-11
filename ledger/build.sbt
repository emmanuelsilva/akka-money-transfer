scalaVersion := "2.13.1"
name := "ledger"
organization := "br.com.emmanuel.moneytransfer"
version := "1.0"

enablePlugins(JavaServerAppPackaging, DockerPlugin)

// akka libraries
lazy val akkaVersion = "2.6.9"
libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-stream"      % akkaVersion

// akka http libraries
lazy val akkaHttpVersion = "10.2.1"
libraryDependencies += "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion
libraryDependencies += "ch.qos.logback"    % "logback-classic" % "1.2.3"

// akka cluster libraries
val akkaManagementVersion = "1.0.10"
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-discovery" % akkaVersion,
  "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,
  "com.lightbend.akka.management" %% "akka-management" % akkaManagementVersion,
  "com.lightbend.akka.management" %% "akka-management-cluster-http" % akkaManagementVersion,
  "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % akkaManagementVersion,
  "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % akkaManagementVersion
)

//akka persistence (event sourcing) libraries
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion
)

// testing libraries
libraryDependencies += "org.scalatest"    %% "scalatest" % "3.1.0" % Test
libraryDependencies += "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test
libraryDependencies += "com.typesafe.akka" %% "akka-persistence-testkit" % akkaVersion % Test
libraryDependencies += "com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion % Test

// mainClass in (Compile, run) := Some("br.com.emmanuel.moneytransfer.infrastructure.rest.HttpRestServer")
mainClass in Compile := Some("br.com.emmanuel.moneytransfer.Run")