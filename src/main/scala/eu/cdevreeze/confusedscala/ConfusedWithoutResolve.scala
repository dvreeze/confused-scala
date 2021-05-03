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
 * The functionality of reporting which of the passed dependencies do not have any group ID that is also in a public
 * repository. It is safest not to use this class directly. After all, the Complete instance must not use any private
 * repositories.
 *
 * @author Chris de Vreeze
 */
final class ConfusedWithoutResolve(val publicComplete: Complete[Task]) {

  import scala.concurrent.ExecutionContext.Implicits.global

  /**
   * Given the passed dependencies, finds all group IDs of those dependencies that are not found in any of the configured
   * public repositories. It uses the (configured) "Complete" of this instance to do so.
   */
  def findDirectGroupIdsMissingInPublicRepositories(dependencies: Seq[Dependency]): ConfusedResult = {
    val groupIds: Seq[Organization] = dependencies.map(_.module.organization).distinct.sorted

    val confusedResults: Seq[(Organization, Complete.Result)] = groupIds.map { groupId =>
      val completionResult: Complete.Result = publicComplete.withInput(groupId.value + ":").result().unsafeRun()
      groupId -> completionResult
    }

    val missingGroupIds: Seq[Organization] =
      confusedResults
        .filterNot {
          _._2.results.exists {
            case (_, Right(Seq(stringResult, _*))) => true
            case _                                 => false
          }
        }
        .map(_._1)
        .distinct

    ConfusedResult(groupIds, missingGroupIds)
  }
}
