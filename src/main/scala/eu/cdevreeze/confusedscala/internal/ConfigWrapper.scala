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

package eu.cdevreeze.confusedscala.internal

import com.typesafe.config.Config
import scala.jdk.CollectionConverters._

import com.typesafe.config.ConfigValue

/**
 * Small wrapper around Lightbend Config, extending it with some more Scala-friendly methods. Most open source
 * Scala wrappers offer far more functionality than needed here, or have dependencies that are not desirable here,
 * or change the rather low-level feel of Lightbend Config.
 *
 * @author Chris de Vreeze
 */
final case class ConfigWrapper(underlyingConfig: Config) {

  def getOptValue(path: String): Option[ConfigValue] = {
    if (underlyingConfig.hasPath(path)) Some(underlyingConfig.getValue(path)) else None
  }

  def getOptMap(path: String): Option[Map[String, Any]] = {
    getOptValue(path).map(_.unwrapped.asInstanceOf[java.util.Map[String, Any]].asScala.toMap)
  }

  // Getters for specific types

  def getOptAny(path: String): Option[Any] = {
    if (underlyingConfig.hasPath(path)) Some(underlyingConfig.getAnyRef(path)) else None
  }

  def getAnySeq(path: String): Seq[Any] = underlyingConfig.getAnyRefList(path).asScala.toSeq

  def getOptAnySeq(path: String): Option[Seq[Any]] = {
    if (underlyingConfig.hasPath(path)) Some(getAnySeq(path)) else None
  }

  def getOptBoolean(path: String): Option[Boolean] = {
    if (underlyingConfig.hasPath(path)) Some(underlyingConfig.getBoolean(path).booleanValue) else None
  }

  def getBooleanSeq(path: String): Seq[Boolean] =
    underlyingConfig.getBooleanList(path).asScala.toSeq.map(_.booleanValue)

  def getOptBooleanSeq(path: String): Option[Seq[Boolean]] = {
    if (underlyingConfig.hasPath(path)) Some(getBooleanSeq(path)) else None
  }

  def getOptConfig(path: String): Option[Config] = {
    if (underlyingConfig.hasPath(path)) Some(underlyingConfig.getConfig(path)) else None
  }

  def getConfigSeq(path: String): Seq[Config] = underlyingConfig.getConfigList(path).asScala.toSeq

  def getOptConfigSeq(path: String): Option[Seq[Config]] = {
    if (underlyingConfig.hasPath(path)) Some(getConfigSeq(path)) else None
  }

  def getOptInt(path: String): Option[Int] = {
    if (underlyingConfig.hasPath(path)) Some(underlyingConfig.getInt(path).intValue) else None
  }

  def getIntSeq(path: String): Seq[Int] = underlyingConfig.getIntList(path).asScala.toSeq.map(_.intValue)

  def getOptIntSeq(path: String): Option[Seq[Int]] = {
    if (underlyingConfig.hasPath(path)) Some(getIntSeq(path)) else None
  }

  def getOptLong(path: String): Option[Long] = {
    if (underlyingConfig.hasPath(path)) Some(underlyingConfig.getLong(path).longValue) else None
  }

  def getLongSeq(path: String): Seq[Long] = underlyingConfig.getLongList(path).asScala.toSeq.map(_.longValue)

  def getOptLongSeq(path: String): Option[Seq[Long]] = {
    if (underlyingConfig.hasPath(path)) Some(getLongSeq(path)) else None
  }

  def getOptDouble(path: String): Option[Double] = {
    if (underlyingConfig.hasPath(path)) Some(underlyingConfig.getDouble(path).doubleValue) else None
  }

  def getDoubleSeq(path: String): Seq[Double] = underlyingConfig.getDoubleList(path).asScala.toSeq.map(_.doubleValue)

  def getOptDoubleSeq(path: String): Option[Seq[Double]] = {
    if (underlyingConfig.hasPath(path)) Some(getDoubleSeq(path)) else None
  }

  def getOptNumber(path: String): Option[Number] = {
    if (underlyingConfig.hasPath(path)) Some(underlyingConfig.getNumber(path)) else None
  }

  def getNumberSeq(path: String): Seq[Number] = underlyingConfig.getNumberList(path).asScala.toSeq

  def getOptNumberSeq(path: String): Option[Seq[Number]] = {
    if (underlyingConfig.hasPath(path)) Some(getNumberSeq(path)) else None
  }

  def getOptString(path: String): Option[String] = {
    if (underlyingConfig.hasPath(path)) Some(underlyingConfig.getString(path)) else None
  }

  def getStringSeq(path: String): Seq[String] = underlyingConfig.getStringList(path).asScala.toSeq

  def getOptStringSeq(path: String): Option[Seq[String]] = {
    if (underlyingConfig.hasPath(path)) Some(getStringSeq(path)) else None
  }
}

object ConfigWrapper {

  implicit class ToConfigWrapper(val config: Config) extends AnyVal {

    implicit def wrap: ConfigWrapper = ConfigWrapper(config)
  }
}
