ivyScala := ivyScala.value.map(_.copy(overrideScalaVersion = true))
resolvers += Resolver.bintrayRepo("twittercsl", "sbt-plugins")

// formatting
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.8.2")

// testing
addSbtPlugin("org.scoverage"  % "sbt-scoverage" % "1.5.0")

addSbtPlugin("org.scoverage"  % "sbt-coveralls" % "1.1.0")

// doc generation
addSbtPlugin("com.eed3si9n"   % "sbt-unidoc"    % "0.4.1")

// packaging
addSbtPlugin("com.eed3si9n"      % "sbt-assembly"  % "0.14.1")

addSbtPlugin("se.marcuslonnberg" % "sbt-docker"    % "1.4.1")

// scrooge
addSbtPlugin("com.twitter" %% "scrooge-sbt-plugin" % "17.11.0")

// microbenchmarking for tests.
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.2.27")

// pgp signing for publishing to sonatype
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")

// our grpc building extends the wrapped sbt-protobuf
addSbtPlugin("com.github.gseitz" % "sbt-protobuf" % "0.6.3")
