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
import coursier._
import coursier.cache.Cache
import coursier.cache.FileCache
import coursier.complete.Complete
import coursier.core.Configuration
import coursier.params.ResolutionParams
import coursier.parse.DependencyParser
import coursier.parse.ModuleParser
import coursier.util.Task
import eu.cdevreeze.confusedscala.internal.ConfigWrapper._

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
    val extraPublicRepositories: Seq[Repository],
    val resolutionParams: ResolutionParams) {

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
    .withResolutionParams(resolutionParams)

  val publicComplete: Complete[Task] =
    Complete()
      .withRepositories(extraPublicRepositories)
      .addRepositories(Repositories.central) // appending Maven Central again, but not ivy2Local
      .withCache(cache)
      .withScalaVersionOpt(resolutionParams.scalaVersionOpt)

  def findAllGroupIdsMissingInPublicRepositories(rootDependencies: Seq[Dependency]): ConfusedResult = {
    val confused: Confused = new Confused(resolve, publicComplete)
    confused.findAllGroupIdsMissingInPublicRepositories(rootDependencies)
  }
}

object ConfusedTool {

  def main(args: Array[String]): Unit = {
    require(args.length >= 1, s"Usage: ConfusedTool <groupId:artifactId:version> ...")

    val config: Config = ConfigFactory.load()

    val extraTrustedGroupIds: Set[Organization] =
      config.wrap.getOptStringSeq("extra-trusted-group-ids").map(_.map(Organization(_)).toSet).getOrElse(Set.empty)
    val errorOnNonEmptyResult: Boolean = config.wrap.getOptBoolean("error-on-non-empty-result").getOrElse(false)
    val trustOwnGroupIds: Boolean = config.wrap.getOptBoolean("trust-own-group-ids").getOrElse(false)

    val confusedTool: ConfusedTool = ConfusedTool.from(config)

    val defaultScalaVersion: String = config.getString("shared.scala-version") // mandatory

    val rootDeps: Seq[Dependency] = args.toSeq.map(parseDependency(_, defaultScalaVersion))
    val trustedGroupIds: Set[Organization] =
      extraTrustedGroupIds.union(rootDeps.map(_.module.organization).filter(_ => trustOwnGroupIds).toSet)

    // Do the work
    val confusedResult: ConfusedResult = confusedTool.findAllGroupIdsMissingInPublicRepositories(rootDeps)

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
      System.exit(1)
    }
  }

  def from(config: Config): ConfusedTool = {
    val cacheLocationOpt: Option[File] = config.wrap.getOptString("filecache.location").map(new File(_))
    val cacheTtlOpt: Option[Duration] = config.wrap.getOptString("filecache.ttl").map(Duration(_))

    // TODO Credentials etc.

    val cache: Cache[Task] = FileCache[Task]()
      .pipe(acc => cacheLocationOpt.map(v => acc.withLocation(v)).getOrElse(acc))
      .pipe(acc => cacheTtlOpt.map(v => acc.withTtl(v)).getOrElse(acc))

    val privateRepositories: Seq[Repository] =
      config.wrap.getOptStringSeq("private-repositories").map(_.map(parseRepository)).getOrElse(Seq.empty)

    val extraPublicRepositories: Seq[Repository] =
      config.wrap.getOptStringSeq("extra-public-repositories").map(_.map(parseRepository)).getOrElse(Seq.empty)

    val resolutionParams: ResolutionParams =
      parseResolutionParams(config.getConfig("resolve"), config.getConfig("shared"))

    new ConfusedTool(cache, privateRepositories, extraPublicRepositories, resolutionParams)
  }

  private def parseRepository(repoString: String): Repository = {
    // TODO Enhance
    repoString match {
      case "ivy2Local" => LocalRepositories.ivy2Local
      case "central"   => Repositories.central
      case s           => MavenRepository(s)
    }
  }

  private def parseResolutionParams(specificConfig: Config, sharedConfig: Config): ResolutionParams = {
    val scalaVersion: String = sharedConfig.getString("scala-version") // mandatory
    val forceScalaVersionOpt: Option[Boolean] = sharedConfig.wrap.getOptBoolean("force-scala-version")

    val defaultConfigurationOpt: Option[Configuration] =
      specificConfig.wrap.getOptString("default-configuration").map(Configuration(_))
    val exclusions: Seq[(Organization, ModuleName)] =
      specificConfig.wrap.getOptStringSeq("exclude").map(_.map(parseModule(_, scalaVersion))).getOrElse(Seq.empty)

    val forcedPropertiesOpt: Option[Map[String, String]] =
      specificConfig.wrap.getOptMap("force-pom-properties").map(_.view.mapValues(_.asInstanceOf[String]).toMap)

    val propertiesOpt: Option[Map[String, String]] =
      specificConfig.wrap.getOptMap("pom-properties").map(_.view.mapValues(_.asInstanceOf[String]).toMap)

    val forceVersions: Seq[Dependency] = specificConfig.wrap
      .getOptStringSeq("force-versions")
      .map(_.map(parseDependency(_, scalaVersion)))
      .getOrElse(Seq.empty)

    val keepOptionalOpt: Option[Boolean] = specificConfig.wrap.getOptBoolean("keep-optional")
    val maxIterationsOpt: Option[Int] = specificConfig.wrap.getOptInt("max-iterations")
    val profilesOpt: Option[Seq[String]] = specificConfig.wrap.getOptStringSeq("profiles")

    // No JDK version, OS info, use system JDK version, use system OS info, typelevel
    // Also no reconciliation, rules. After all, this tool is not about version reconciliation and cleaning up builds.

    ResolutionParams()
      .withScalaVersion(scalaVersion)
      .pipe(acc => forceScalaVersionOpt.map(v => acc.withForceScalaVersion(v)).getOrElse(acc))
      .pipe(acc => defaultConfigurationOpt.map(v => acc.withDefaultConfiguration(v)).getOrElse(acc))
      .withExclusions(exclusions.toSet)
      .pipe(acc => forcedPropertiesOpt.map(v => acc.withForcedProperties(v)).getOrElse(acc))
      .pipe(acc => propertiesOpt.map(v => acc.withProperties(v.toSeq.sorted)).getOrElse(acc))
      .withForceVersion(forceVersions.map(d => d.module -> d.version).toMap)
      .pipe(acc => keepOptionalOpt.map(v => acc.withKeepOptionalDependencies(v)).getOrElse(acc))
      .pipe(acc => maxIterationsOpt.map(v => acc.withMaxIterations(v)).getOrElse(acc))
      .pipe(acc => profilesOpt.map(v => acc.withProfiles(v.toSet)).getOrElse(acc))
  }

  private def parseDependency(s: String, scalaVersion: String): Dependency = {
    DependencyParser.dependency(s, scalaVersion).getOrElse(sys.error(s"Could not parse dependency '$s'"))
  }

  private def parseModule(s: String, scalaVersion: String): (Organization, ModuleName) = {
    ModuleParser
      .module(s, scalaVersion)
      .getOrElse(sys.error(s"Could not parse module '$s'"))
      .pipe(m => (m.organization, m.name))
  }
}
