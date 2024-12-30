addSbtPlugin("ch.epfl.scala"                     % "sbt-bloop"                     % "2.0.6")
addSbtPlugin("ch.epfl.scala"                     % "sbt-scalafix"                  % "0.13.0")
addSbtPlugin("com.eed3si9n"                      % "sbt-buildinfo"                 % "0.13.1")
addSbtPlugin("com.github.sbt"                    % "sbt-unidoc"                    % "0.5.0")
addSbtPlugin("com.github.sbt"                    % "sbt-ci-release"                % "1.9.2")
addSbtPlugin("com.github.cb372"                  % "sbt-explicit-dependencies"     % "0.3.1")
addSbtPlugin("com.thoughtworks.sbt-api-mappings" % "sbt-api-mappings"              % "3.0.2")
addSbtPlugin("com.typesafe"                      % "sbt-mima-plugin"               % "1.1.4")
addSbtPlugin("de.heikoseeberger"                 % "sbt-header"                    % "5.10.0")
addSbtPlugin("org.portable-scala"                % "sbt-scala-native-crossproject" % "1.3.2")
addSbtPlugin("org.portable-scala"                % "sbt-scalajs-crossproject"      % "1.3.2")
addSbtPlugin("org.scala-js"                      % "sbt-scalajs"                   % "1.17.0")
addSbtPlugin("org.scala-native"                  % "sbt-scala-native"              % "0.5.6")
addSbtPlugin("org.scalameta"                     % "sbt-mdoc"                      % "2.6.2")
addSbtPlugin("org.scalameta"                     % "sbt-scalafmt"                  % "2.5.2")
addSbtPlugin("pl.project13.scala"                % "sbt-jcstress"                  % "0.2.0")
addSbtPlugin("pl.project13.scala"                % "sbt-jmh"                       % "0.4.7")
addSbtPlugin("dev.zio"                           % "zio-sbt-website"               % "0.4.0-alpha.29")
addDependencyTreePlugin

libraryDependencies += "org.snakeyaml" % "snakeyaml-engine" % "2.7"

resolvers ++= Resolver.sonatypeOssRepos("public")
