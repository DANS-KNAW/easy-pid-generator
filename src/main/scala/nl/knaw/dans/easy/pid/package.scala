/**
 * Copyright (C) 2015 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
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
package nl.knaw.dans.easy

import scala.util.{ Failure, Success, Try }

package object pid {

  type Seed = Long
  type Pid = String

  sealed abstract class PidType(val name: String)
  case object DOI extends PidType("doi")
  case object URN extends PidType("urn")

  case class RanOutOfSeeds(pidType: PidType) extends Exception(s"No more ${ pidType.name } seeds available.")

  // TODO copied from easy-bag-store
  implicit class TryExtensions2[T](val t: Try[T]) extends AnyVal {
    // TODO candidate for dans-scala-lib
    def unsafeGetOrThrow: T = {
      t match {
        case Success(value) => value
        case Failure(throwable) => throw throwable
      }
    }
  }
}
