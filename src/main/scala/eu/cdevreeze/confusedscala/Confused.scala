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

import coursier._
import coursier.complete.Complete
import coursier.util.Task

/**
 * The functionality of finding all dependencies of a given dependency, and reporting which ones do not have any group ID
 * that is also in a public repository. It is safest not to use this class directly, but to use it via class ConfusedTool.
 * After all, the Resolve instance must search private repositories first, and the Complete instance must not use any private
 * repositories.
 *
 * @author Chris de Vreeze
 */
final class Confused(val resolve: Resolve[Task], val publicComplete: Complete[Task]) {

  import scala.concurrent.ExecutionContext.Implicits.global

  private val confusedWithoutResolve: ConfusedWithoutResolve = new ConfusedWithoutResolve(publicComplete)

  /**
   * First calls method findAllDependencies, and then calls method findDirectGroupIdsMissingInPublicRepositories on its results.
   * This is the main method containing all functionality of the Confused-Scala tool, but without the configuration.
   */
  def findAllGroupIdsMissingInPublicRepositories(rootDependencies: Seq[Dependency]): ConfusedResult = {
    val deps: Seq[Dependency] = findAllDependencies(rootDependencies)
    findDirectGroupIdsMissingInPublicRepositories(deps)
  }

  /**
   * Transitively finds all dependencies, given the passed root dependencies (that are returned together with the
   * transitive dependencies). It uses the "Resolve" of this instance to do so.
   */
  def findAllDependencies(rootDependencies: Seq[Dependency]): Seq[Dependency] = {
    val resolution: Resolution = resolve.withDependencies(rootDependencies).run()
    resolution.dependencies.toSeq.sortBy(d => (d.module.toString, d.version))
  }

  /**
   * Given the passed dependencies, finds all group IDs of those dependencies that are not found in any of the configured
   * public repositories. It (indirectly) uses the (configured) "Complete" of this instance to do so.
   */
  def findDirectGroupIdsMissingInPublicRepositories(dependencies: Seq[Dependency]): ConfusedResult = {
    confusedWithoutResolve.findDirectGroupIdsMissingInPublicRepositories(dependencies)
  }
}
