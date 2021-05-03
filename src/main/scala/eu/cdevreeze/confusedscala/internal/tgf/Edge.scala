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
 * Edge in the Trivial Graph Format (TGF).
 *
 * @author Chris de Vreeze
 */
final case class Edge(from: String, to: String, labelOption: Option[String]) {
  require(from.nonEmpty, s"Empty source ID not allowed")
  require(to.nonEmpty, s"Empty target ID not allowed")
}

object Edge {

  private val space = ' '

  def parse(s: String): Edge = parseOption(s).getOrElse(sys.error(s"Could not parse edge '$s'"))

  def parseOption(s: String): Option[Edge] = {
    val idx = s.indexOf(space)

    if (idx < 0) {
      None
    } else {
      val fromId = s.substring(0, idx).trim

      if (fromId.isEmpty) {
        None
      } else {
        val remainder = s.substring(idx + 1).dropWhile(_ == space)

        Remainder.parseRemainderOption(remainder).map(_.toEdge(fromId))
      }
    }
  }

  private[Edge] final case class Remainder(to: String, labelOption: Option[String]) {
    require(to.nonEmpty, s"Empty target ID not allowed")

    def toEdge(from: String): Edge = Edge(from, to, labelOption)
  }

  private[Edge] object Remainder {

    def parseRemainderOption(remainder: String): Option[Remainder] = {
      assert(remainder.takeWhile(_ == space).isEmpty, s"No prefixing spaces allowed")

      val idx = remainder.indexOf(space)

      if (idx < 0) {
        val toId = remainder.trim

        if (toId.isEmpty) None else Some(Remainder(toId, None))
      } else {
        val toId = remainder.substring(0, idx).trim
        val edgeLabel = remainder.substring(idx + 1).dropWhile(_ == space)
        val edgeLabelOption = if (edgeLabel.trim.isEmpty) None else Some(edgeLabel)

        if (toId.isEmpty) None else Some(Remainder(toId, edgeLabelOption))
      }
    }
  }
}
