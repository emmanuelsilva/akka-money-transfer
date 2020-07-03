scalaVersion := "2.13.1"
name := "hello-world"
organization := "br.com.emmanuel.moneytransfer"
version := "1.0"

val akkaVersion = "2.6.6"

// akka libraries
libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion

// testing libraries
libraryDependencies += "org.scalatest"    %% "scalatest" % "3.0.8" % Test
libraryDependencies += "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test