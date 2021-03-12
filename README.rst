==============
Confused-Scala
==============

Confused-Scala is a tool similar to `confused`_, but limited to the Maven ecosystem. Given one or more root dependencies,
it first resolves all transitive dependencies, and then for each of them checks if the group ID is found in a public Maven
repository (where it can be configured which are the public Maven repositories to look at, besides Maven Central). All
such group IDs that are not found in any public repository will be returned as output.

This tool is written in Scala, on top of the well-known `Coursier`_ library. It uses Coursier calls like "cs resolve" and
"cs complete", via its high-level API.

Usage
=====

Confused-Scala can be used as a program, or as an API. It can be added as a dependency as follows in an SBT build::

    libraryDependencies += "eu.cdevreeze.confused-scala" %% "confused-scala" % "0.1.0"

It is available only for Scala 2.13. Confused-Scala can easily be used in an `Ammonite`_ script.

When using Confused-Scala as a program, one or more root dependencies must be passed to the program, in Coursier style
(using colons to separate group ID from artifact ID, and artifact ID from version).

It also needs a `Lightbend Config`_ configuration file. See the "example-application.conf" as example. Then point to the
configuration file using system properties such as "config.file" or "config.resource".

.. _`confused`: https://github.com/visma-prodsec/confused
.. _`Coursier`: https://get-coursier.io/
.. _`Ammonite`: https://ammonite.io/
.. _`Lightbend Config`: https://github.com/lightbend/config

