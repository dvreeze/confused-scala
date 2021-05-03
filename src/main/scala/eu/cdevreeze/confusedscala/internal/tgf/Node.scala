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

package eu.cdevreeze.confusedscala.internal.tgf

/**
 * Node in the Trivial Graph Format (TGF).
 *
 * @author Chris de Vreeze
 */
final case class Node(id: String, label: String) {
  require(id.nonEmpty, s"Empty node ID not allowed")
}

object Node {

  private val space = ' '

  def parse(s: String): Node = parseOption(s).getOrElse(sys.error(s"Could not parse node '$s'"))

  def parseOption(s: String): Option[Node] = {
    val idx = s.indexOf(space)

    if (idx < 0) {
      None
    } else {
      val nodeId = s.substring(0, idx).trim
      val nodeLabel = s.substring(idx + 1).dropWhile(_ == space)

      if (nodeId.isEmpty || nodeLabel.trim.isEmpty) {
        None
      } else {
        Some(Node(nodeId, nodeLabel))
      }
    }
  }
}
