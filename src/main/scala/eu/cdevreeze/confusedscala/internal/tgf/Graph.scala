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
 * Graph in the Trivial Graph Format (TGF).
 *
 * @author Chris de Vreeze
 */
final case class Graph(nodes: Seq[Node], edges: Seq[Edge]) {
  require(nodes.nonEmpty, s"Zero nodes not allowed")
  require(nodes.map(_.id).distinct.size == nodes.size, s"Not all node IDs are unique, which is not allowed")
  require(edges.map(_.from).toSet.subsetOf(nodes.map(_.id).toSet), s"Not all edge sources refer to existing nodes")
  require(edges.map(_.to).toSet.subsetOf(nodes.map(_.id).toSet), s"Not all edge targets refer to existing nodes")

  private val nodesById: Map[String, Node] = nodes.map(n => n.id -> n).toMap

  def findNode(id: String): Option[Node] = nodesById.get(id)

  def getNode(id: String): Node = findNode(id).getOrElse(sys.error(s"Missing node with ID '$id'"))

  def isConnected: Boolean = {
    val nodeIds: Set[String] = nodesById.keySet
    val edgeNodeIds: Set[String] = edges.map(_.from).toSet.union(edges.map(_.to).toSet)
    edgeNodeIds == nodeIds
  }

  def rootNodeIds: Set[String] = {
    edges.map(_.from).toSet.diff(edges.map(_.to).toSet)
  }
}
