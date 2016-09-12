/**
 * Copyright (C) 2015-2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.pid.microservice

import org.json4s.Formats
import org.json4s.native.{JsonMethods, Serialization}

import scala.util.Try

// TODO candidate for microservice library
class JsonTransformer(implicit formatters: Formats) {

  def parseJSON[T](json: String)(implicit m: Manifest[T]): Try[T] = Try {
    JsonMethods.parse(json).extract[T]
  }

  def writeJSON[T <: AnyRef](response: T): String = {
    Serialization.write(response)
  }
}

object JsonTransformer {
  def apply(implicit formatters: Formats) = new JsonTransformer
}