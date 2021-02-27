scalaVersion := "2.13.1"
name := "akka-money-transfer"
organization := "br.com.emmanuel.moneytransfer"
version := "1.0"

lazy val akkaVersion = "2.6.6"
lazy val akkaHttpVersion = "10.2.1"

// akka libraries
libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion
libraryDependencies += "com.typesafe.akka" %% "akka-stream"      % akkaVersion

// akka http libraries
libraryDependencies += "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion
libraryDependencies += "ch.qos.logback"    % "logback-classic" % "1.2.3"

// testing libraries
libraryDependencies += "org.scalatest"    %% "scalatest" % "3.0.8" % Test
libraryDependencies += "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test
libraryDependencies += "com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion % Test

mainClass in (Compile, run) := Some("br.com.emmanuel.moneytransfer.infrastructure.rest.HttpRestServer")