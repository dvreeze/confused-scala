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
 * Parsing support for the Trivial Graph Format (TGF).
 *
 * This support is needed in order to interpret the output of the "mvn dependency:tree" command, when the used output
 * format is "tgf" (-DoutputType=tgf).
 *
 * @author Chris de Vreeze
 */
object TrivialGraphFormat {

  private val sectionSeparator = "#"

  def parse(lines: Seq[String]): Graph = {
    val separatorLineIdx = lines.indexWhere(_.trim == sectionSeparator)
    require(separatorLineIdx >= 0, s"Missing section separator line (#), which is not allowed")
    require(separatorLineIdx > 0, s"Zero nodes, which is not allowed")

    val (nodeLines, remainingLines) = lines.splitAt(separatorLineIdx)
    val edgeLines = remainingLines.ensuring(_.head.trim == sectionSeparator).tail

    val nodes: Seq[Node] = nodeLines.map(Node.parse)
    val edges: Seq[Edge] = edgeLines.map(Edge.parse)

    Graph(nodes, edges)
  }
}
