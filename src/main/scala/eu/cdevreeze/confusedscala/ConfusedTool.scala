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
import scala.jdk.CollectionConverters._
import scala.util.chaining._

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import coursier._
import coursier.cache.Cache
import coursier.cache.FileCache
import coursier.complete.Complete
import coursier.parse.DependencyParser
import coursier.util.Task

/**
 * Tool that finds all dependencies of a given dependency, and that reports which ones do not have any group ID
 * that is also in a public repository.
 *
 * Lightbend Config is used to configure the tool, w.r.t. private and public repositories, caching etc.
 * See the example config file in the resources directory. System properties like config.file, config.resource (for classpath
 * resources) or config.url can be used to refer to an application configuration file.
 *
 * As a program the tool takes at least 2 parameters. The first one is the default Scala version. The remaining ones are
 * root dependencies in the form groupId:artifactId:version or groupId::artifactName:version.
 *
 * The program should not use both the local Ivy2 repository and the local Maven repository (as private repositories),
 * but this is not checked. Preferably the local Maven repository is not used.
 *
 * @author Chris de Vreeze
 */
final class ConfusedTool(
    val cache: Cache[Task],
    val privateRepositories: Seq[Repository],
    val extraPublicRepositories: Seq[Repository]) {

  require(
    privateRepositories.intersect(extraPublicRepositories).isEmpty,
    s"Repositories cannot be both private and public")
  require(
    privateRepositories.intersect(Seq(Repositories.central)).isEmpty,
    s"Repositories cannot be both private and public (Maven Central)")

  // Safe order of repositories, because the private repositories are tried first.

  val resolve: Resolve[Task] = Resolve()
    .withRepositories(privateRepositories)
    .addRepositories(extraPublicRepositories: _*)
    .addRepositories(Repositories.central) // appending Maven Central again, but not ivy2Local
    .withCache(cache)

  val publicComplete: Complete[Task] =
    Complete()
      .withRepositories(extraPublicRepositories)
      .addRepositories(Repositories.central) // appending Maven Central again, but not ivy2Local
      .withCache(cache)

  def findAllMissingGroupIds(rootDependencies: Seq[Dependency]): MissingGroupIdsResult = {
    val confused: Confused = new Confused(resolve, publicComplete)
    confused.findAllMissingGroupIds(rootDependencies)
  }
}

object ConfusedTool {

  def main(args: Array[String]): Unit = {
    require(args.length >= 2, s"Usage: ConfusedTool <default Scala version> <groupId:artifactId:version> ...")

    val defaultScalaVersion = args(0)
    val rootDeps: Seq[Dependency] = args.toSeq
      .drop(1)
      .map(s => DependencyParser.dependency(s, defaultScalaVersion))
      .collect { case Right(dep) => dep }

    val config: Config = ConfigFactory.load()
    val confusedTool: ConfusedTool = ConfusedTool.from(config)

    val missingGroupIdsResult: MissingGroupIdsResult = confusedTool.findAllMissingGroupIds(rootDeps)

    missingGroupIdsResult.allGroupIds.foreach { groupId =>
      println(s"Analyzing group ID: $groupId")
    }

    println()
    missingGroupIdsResult.missingGroupIds
      .tap(groupIds => if (groupIds.isEmpty) println(s"No missing publicly available group IDs"))
      .foreach { groupId =>
        println(s"Missing publicly available group ID: $groupId")
      }
  }

  def from(config: Config): ConfusedTool = {
    val cacheLocation: File = new File(config.getString("filecache.location"))
    val ttlInHours: Int = config.getInt("filecache.ttlInHours")

    val cache: Cache[Task] = FileCache().withLocation(cacheLocation).withTtl(ttlInHours.hours)

    val privateRepositories: Seq[Repository] =
      config.getStringList("privateRepositories").asScala.toSeq.map(parseRepository)

    val extraPublicRepositories: Seq[Repository] =
      config.getStringList("extraPublicRepositories").asScala.toSeq.map(parseRepository)

    new ConfusedTool(cache, privateRepositories, extraPublicRepositories)
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
