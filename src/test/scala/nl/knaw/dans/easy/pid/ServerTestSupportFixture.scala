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
package nl.knaw.dans.easy.pid

import java.net.{ HttpURLConnection, URL }

import org.apache.commons.io.IOUtils
import resource._

trait ServerTestSupportFixture {

  def callService(path: String = "pids", method: String = "GET"): (Int, String) = {
    new URL(s"http://localhost:8060/$path").openConnection() match {
      case conn: HttpURLConnection =>
        managed {
          conn.setConnectTimeout(1000)
          conn.setReadTimeout(1000)
          conn.setRequestMethod(method)
          conn
        }
          .map(_.getResponseCode)
          .acquireAndGet {
            case code if code >= 200 && code < 300 => (code, IOUtils.toString(conn.getInputStream))
            case code => (code, IOUtils.toString(conn.getErrorStream))
          }
      case _ => throw new Exception
    }
  }

  def postUrn: (Int, String) = callService("pids?type=urn", "POST")

  def postDoi: (Int, String) = callService("pids?type=doi", "POST")

  def successful: (Int, String) = (200, "Persistent Identifier Generator running")
}
