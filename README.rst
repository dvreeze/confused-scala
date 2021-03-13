==============
Confused-Scala
==============

Confused-Scala is a tool similar to `confused`_, but limited to the Maven ecosystem. Given one or more root dependencies,
it first resolves all transitive dependencies, and then for each of them checks if the group ID is found in a public Maven
repository (where it can be configured which are the public Maven repositories to look at, besides Maven Central). All
such group IDs that are not found in any public repository will be returned as output.

This tool is written in Scala, on top of the well-known `Coursier`_ library. It internally uses Coursier calls like "cs resolve" and
"cs complete", via its high-level API.

Usage
=====

Confused-Scala can be used as a program, or as an API. It can be added as a dependency as follows in an SBT build::

    libraryDependencies += "eu.cdevreeze.confused-scala" %% "confused-scala" % "0.2.0"

It is available only for Scala 2.13.

Not very surprisingly, Confused-Scala can easily be used in an `Ammonite`_ script.

When using Confused-Scala as a program, one or more root dependencies must be passed to the program, in Coursier style
(using colons to separate group ID from artifact ID, and artifact ID from version).

It also needs a `Lightbend Config`_ configuration file. See the `example-application.conf`_ as example. Then point to the
configuration file using system properties such as "config.file", "config.resource" or "config.url".

Typically Confused-Scala would be used in the build flow of a Scala project, after the step that deployed the project's artifacts
to some (private or public) repository. One way to invoke the Confused-Scala tool would be to use `Coursier`_ to do it.
To prevent installation of Coursier on the build server, a JAR-based launcher could be used, provided that Java 8+ is available.

See the JAR-based launchers at `Coursier CLI installation`_. These JAR-based launchers are executable JAR files (containing
a shebang) that invoke a Java application (that bootstraps Coursier's functionality), so these JAR-based launchers can be used
as if they were the "cs" command themselves.

Then the Confused-Scala tool can be invoked as follows::

    /path/to/coursier-jar-launcher eu.cdevreeze.confused-scala:confused-scala_2.13:0.2.0 \
      --java-opt -Dconfig.file=/path/to/app.conf -- <dep>

where <dep> (possibly more than one such program argument, space-separated) could be a dependency like::

    eu.cdevreeze.tqa::tqa:0.10.0

It is also possible to install the tool as described in `Coursier install`_, using the `channel`_ of the tool.

.. _`confused`: https://github.com/visma-prodsec/confused
.. _`Coursier`: https://get-coursier.io/
.. _`Ammonite`: https://ammonite.io/
.. _`Lightbend Config`: https://github.com/lightbend/config
.. _`example-application.conf`: https://github.com/dvreeze/confused-scala/blob/master/src/main/resources/example-application.conf
.. _`Coursier CLI installation`: https://get-coursier.io/docs/cli-installation
.. _`Coursier install`: https://get-coursier.io/docs/cli-install
.. _`channel`: https://github.com/dvreeze/confused-scala/blob/master/apps/resources/confused-scala.json
