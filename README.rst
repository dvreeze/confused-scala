==============
Confused-Scala
==============

Confused-Scala is a tool similar to `confused`_, but limited to the Maven ecosystem. Given one or more root dependencies,
it first resolves all transitive dependencies, and then for each of them checks if the group ID is found in a public Maven
repository (where it can be configured which are the public Maven repositories to look at, besides Maven Central). All
such group IDs that are not found in any public repository will be returned as output.

There is a catch, though. The tool first resolves all dependencies, including the root dependencies, so they must have been
deployed first, which is often not desirable, because deployment is typically preceded by running test code, but what if that
test code uses dependencies that should have been flagged by this tool in the first place? See below for an alternative when
using Maven in the build workflow of a project.

This tool is written in Scala, on top of the well-known `Coursier`_ library. It internally uses Coursier calls like "cs resolve" and
"cs complete", via its high-level API.

Usage
=====

Confused-Scala can be used as a program, or as an API. It can be added as a dependency as follows in an SBT build::

    libraryDependencies += "eu.cdevreeze.confused-scala" %% "confused-scala" % "0.3.0"

It is available only for Scala 2.13.

Not very surprisingly, Confused-Scala can easily be used in an `Ammonite`_ script.

When using Confused-Scala as a program, one or more root dependencies must be passed to the program, in Coursier style
(using colons to separate group ID from artifact ID, and artifact ID from version).

It also needs a `Lightbend Config`_ configuration file. See the `example-application.conf`_ as example. Then point to the
configuration file using system properties such as "config.file", "config.resource" or "config.url".

Typically Confused-Scala would be used in the build flow of a Scala project, after the step that deployed the project's artifacts
to some (private or public) repository, if that is an acceptable approach (which very often is not the case, as mentioned earlier).
One way to invoke the Confused-Scala tool would be to use `Coursier`_ to do it.

For that, see the JAR-based launchers at `Coursier CLI installation`_. These JAR-based launchers are executable JAR files (containing
a shebang) that invoke a Java application (that bootstraps Coursier's functionality), so these JAR-based launchers can be used
as if they were the "cs" command themselves.

Then the Confused-Scala tool can be invoked as follows::

    /path/to/coursier-jar-launcher launch eu.cdevreeze.confused-scala:confused-scala_2.13:0.3.0 \
      -M eu.cdevreeze.confusedscala.ConfusedTool \
      --java-opt -Dconfig.file=/path/to/app.conf -- <dep>

where <dep> (possibly more than one such program argument, space-separated) could be a dependency like::

    eu.cdevreeze.tqa::tqa:0.10.0

It is also possible to install the tool as described in `Coursier install`_, using the `channel`_ of the tool.

In a Maven build, we could easily circumvent the above-mentioned chicken-eff problem. In the build flow, we would first do::

    mvn dependency:tree -DoutputType=tgf -DoutputFile=mydeps-tgf.txt

Then the appropriate Confused-Scala tool can be invoked as follows::

    /path/to/coursier-jar-launcher launch eu.cdevreeze.confused-scala:confused-scala_2.13:0.3.0 \
      -M eu.cdevreeze.confusedscala.ConfusedToolTakingMavenTgfDependencyTrees \
      --java-opt -Dconfig.file=/path/to/app.conf -- <tgf dependencies input file path>

Again, it is also possible to install the tool as described in `Coursier install`_, using the `TGF channel`_ of the tool.

.. _`confused`: https://github.com/visma-prodsec/confused
.. _`Coursier`: https://get-coursier.io/
.. _`Ammonite`: https://ammonite.io/
.. _`Lightbend Config`: https://github.com/lightbend/config
.. _`example-application.conf`: https://github.com/dvreeze/confused-scala/blob/master/src/main/resources/example-application.conf
.. _`Coursier CLI installation`: https://get-coursier.io/docs/cli-installation
.. _`Coursier install`: https://get-coursier.io/docs/cli-install
.. _`channel`: https://github.com/dvreeze/confused-scala/blob/master/apps/resources/confused-scala.json
.. _`TGF channel`: https://github.com/dvreeze/confused-scala/blob/master/apps/resources/confused-scala-maven-tgf.json
