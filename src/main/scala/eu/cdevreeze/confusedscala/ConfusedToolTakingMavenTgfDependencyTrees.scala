/*
 * Copyright 2021-2021 Chris de Vreeze
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cdevreeze.confusedscala

import java.io.File

import scala.concurrent.duration._
import scala.util.chaining._

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import coursier.Dependency
import coursier.LocalRepositories
import coursier.Module
import coursier.Organization
import coursier.Repositories
import coursier.cache.Cache
import coursier.cache.FileCache
import coursier.complete.Complete
import coursier.core.ModuleName
import coursier.core.Repository
import coursier.maven.MavenRepository
import coursier.util.Task
import eu.cdevreeze.confusedscala.ConfusedToolTakingMavenTgfDependencyTrees.GraphWrapper
import eu.cdevreeze.confusedscala.internal.ConfigWrapper._
import eu.cdevreeze.confusedscala.internal.tgf.Graph
import eu.cdevreeze.confusedscala.internal.tgf.Node
import eu.cdevreeze.confusedscala.internal.tgf.TrivialGraphFormat

/**
 * Tool that reports which of the parsed dependencies do not have any group ID that is also in a public repository.
 * The dependencies are parsed from an input file in TGF format (trivial graph format) that is output from a "mvn dependency:tree"
 * command.
 *
 * In the companion object, Lightbend Config is used to create and configure the tool, w.r.t. public repositories, caching etc.
 * See [[https://github.com/dvreeze/confused-scala/blob/master/src/main/resources/example-application.conf]] (although
 * many of the settings there are applicable to program ConfusedTool but not to ConfusedToolTakingMavenTgfDependencyTrees).
 * System properties like config.file, config.resource (for classpath resources) or config.url can be used to refer to an
 * application configuration file.
 *
 * As a program the tool takes exactly 1 parameter, namely the path to the input file in TGF format.
 *
 * @author Chris de Vreeze
 */
final class ConfusedToolTakingMavenTgfDependencyTrees(
    val cache: Cache[Task],
    val extraPublicRepositories: Seq[Repository],
    val scalaVersionOpt: Option[String]) {

  val publicComplete: Complete[Task] =
    Complete()
      .withRepositories(extraPublicRepositories)
      .addRepositories(Repositories.central) // appending Maven Central again, but not ivy2Local
      .withCache(cache)
      .withScalaVersionOpt(scalaVersionOpt)

  /**
   * This is the overloaded main method containing all functionality of the Confused-Scala tool (taking TGF dependency input).
   */
  def findAllGroupIdsMissingInPublicRepositories(tgfInputFile: File): ConfusedResult = {
    findAllGroupIdsMissingInPublicRepositories(parseMavenGraph(tgfInputFile))
  }

  /**
   * This is the overloaded main method containing all functionality of the Confused-Scala tool (taking TGF dependency input as String).
   */
  def findAllGroupIdsMissingInPublicRepositories(tgfInputString: String): ConfusedResult = {
    findAllGroupIdsMissingInPublicRepositories(parseMavenGraph(tgfInputString))
  }

  /**
   * This is the overloaded main method containing all functionality of the Confused-Scala tool (taking dependency graphs as input).
   */
  def findAllGroupIdsMissingInPublicRepositories(graph: Graph): ConfusedResult = {
    val dependencies: Seq[Dependency] = findAllDependencies(graph)
    findDirectGroupIdsMissingInPublicRepositories(dependencies)
  }

  /**
   * Given the passed dependencies, finds their group IDs that are missing in public repositories.
   * This method calls method findDirectGroupIdsMissingInPublicRepositories on the underlying "ConfusedWithoutResolve" instance.
   */
  def findDirectGroupIdsMissingInPublicRepositories(dependencies: Seq[Dependency]): ConfusedResult = {
    val confused: ConfusedWithoutResolve = new ConfusedWithoutResolve(publicComplete)
    confused.findDirectGroupIdsMissingInPublicRepositories(dependencies)
  }

  def findAllDependencies(graph: Graph): Seq[Dependency] = {
    GraphWrapper(graph).findAllDependencies
  }

  def parseMavenGraph(tgfInputFile: File): Graph = {
    val lines: Seq[String] = scala.io.Source.fromFile(tgfInputFile, scala.io.Codec.UTF8.toString).getLines().toSeq
    TrivialGraphFormat.parse(lines)
  }

  def parseMavenGraph(tgfInputString: String): Graph = {
    val lines: Seq[String] = scala.io.Source.fromString(tgfInputString).getLines().toSeq
    TrivialGraphFormat.parse(lines)
  }
}

object ConfusedToolTakingMavenTgfDependencyTrees {

  final case class GraphWrapper(graph: Graph) {

    def findNodeAsDependency(id: String): Option[Dependency] = graph.findNode(id).map(convertNodeToDependency)

    def getNodeAsDependency(id: String): Dependency =
      findNodeAsDependency(id).getOrElse(sys.error(s"Missing node with ID '$id'"))

    def findAllDependencies: Seq[Dependency] = {
      graph.nodes.map(convertNodeToDependency)
    }

    def findAllRootDependencies: Seq[Dependency] = {
      graph.rootNodeIds.toSeq.sorted.map(getNodeAsDependency)
    }

    def convertNodeToDependency(node: Node): Dependency = {
      MavenDependency.parse(node.label).toDependency
    }
  }

  final case class MavenDependency(
      groupId: String,
      artifactId: String,
      artifactType: String,
      version: String,
      scopeOption: Option[String]) {

    def toDependency: Dependency = Dependency(Module(Organization(groupId), ModuleName(artifactId)), version)
  }

  object MavenDependency {

    def parse(s: String): MavenDependency = {
      val parts: Seq[String] = s.split(':').toSeq
      require(parts.sizeIs == 4 || parts.sizeIs == 5, s"Not a Maven dependency: '$s'")
      val scopeOption: Option[String] = if (parts.sizeIs == 5) Some(parts(4)) else None
      MavenDependency(parts(0), parts(1), parts(2), parts(3), scopeOption)
    }
  }

  def main(args: Array[String]): Unit = {
    require(args.lengthIs == 1, s"Usage: ConfusedToolTakingMavenTgfDependencyTrees <tgf input file path> ...")

    val inputFile: File = new File(args(0)).ensuring(_.isFile, s"Not a normal file: '${args(0)}'")

    val config: Config = ConfigFactory.load()

    val extraTrustedGroupIds: Set[Organization] =
      config.wrap.getOptStringSeq("extra-trusted-group-ids").map(_.map(Organization(_)).toSet).getOrElse(Set.empty)
    val errorOnNonEmptyResult: Boolean = config.wrap.getOptBoolean("error-on-non-empty-result").getOrElse(false)
    val trustOwnGroupIds: Boolean = config.wrap.getOptBoolean("trust-own-group-ids").getOrElse(false)

    val confusedTool: ConfusedToolTakingMavenTgfDependencyTrees = ConfusedToolTakingMavenTgfDependencyTrees.from(config)

    val defaultScalaVersion: String = config.getString("shared.scala-version") // mandatory

    val mavenGraph: Graph = confusedTool.parseMavenGraph(inputFile)

    val rootDeps: Seq[Dependency] = mavenGraph.pipe(GraphWrapper.apply).findAllRootDependencies
    val trustedGroupIds: Set[Organization] =
      extraTrustedGroupIds.union(rootDeps.map(_.module.organization).filter(_ => trustOwnGroupIds).toSet)

    // Do the work
    val confusedResult: ConfusedResult = confusedTool.findAllGroupIdsMissingInPublicRepositories(mavenGraph)

    confusedResult.allGroupIds.foreach { groupId =>
      println(s"Analyzing group ID: ${groupId.value}")
    }

    println()
    confusedResult.missingGroupIds
      .tap(groupIds => if (groupIds.isEmpty) println(s"All analyzed group IDs occur in public repositories"))
      .foreach { groupId =>
        val extraMsg: String = if (trustedGroupIds.contains(groupId)) "(but it is trusted/ignored)" else ""
        println(s"Group ID that does not occur in any of the public repositories: ${groupId.value} $extraMsg".trim)
      }

    val exitWithError: Boolean = errorOnNonEmptyResult && !confusedResult.missingGroupIds.forall(trustedGroupIds)

    if (exitWithError) {
      println("Failing with exit code 1")
      System.exit(1)
    }
  }

  def from(config: Config): ConfusedToolTakingMavenTgfDependencyTrees = {
    val cacheLocationOpt: Option[File] = config.wrap.getOptString("filecache.location").map(new File(_))
    val cacheTtlOpt: Option[Duration] = config.wrap.getOptString("filecache.ttl").map(Duration(_))

    // TODO Credentials etc.

    val cache: Cache[Task] = FileCache[Task]()
      .pipe(acc => cacheLocationOpt.map(v => acc.withLocation(v)).getOrElse(acc))
      .pipe(acc => cacheTtlOpt.map(v => acc.withTtl(v)).getOrElse(acc))

    val extraPublicRepositories: Seq[Repository] =
      config.wrap.getOptStringSeq("extra-public-repositories").map(_.map(parseRepository)).getOrElse(Seq.empty)

    val scalaVersionOpt: Option[String] = config.wrap.getOptString("shared.scala-version")

    new ConfusedToolTakingMavenTgfDependencyTrees(cache, extraPublicRepositories, scalaVersionOpt)
  }

  private def parseRepository(repoString: String): Repository = {
    // TODO Enhance
    repoString match {
      case "ivy2Local" => LocalRepositories.ivy2Local
      case "central"   => Repositories.central
      case s           => MavenRepository(s)
    }
  }
}
