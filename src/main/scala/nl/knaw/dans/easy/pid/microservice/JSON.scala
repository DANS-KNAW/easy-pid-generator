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

import org.json4s.JsonAST.{JField, JObject, JString}
import org.json4s.ext.UUIDSerializer
import org.json4s.native.{JsonMethods, Serialization}
import org.json4s.{CustomSerializer, DefaultFormats}

case object PidTypeSerializer extends CustomSerializer[PidType](format => ( {
  case JString("urn") => URN
  case JString("doi") => DOI
}, {
  case pid: PidType => JString(pid.name)
})
)

case object ResponseResultSerializer extends CustomSerializer[ResponseResult](format => ( {
  case JObject(List(JField("result", JString(result)))) => ResponseSuccessResult(result)
  case JObject(List(JField("error", JString(error)))) => ResponseFailureResult(error)
}, {
  case ResponseSuccessResult(result) => JObject(JField("result", JString(result)))
  case ResponseFailureResult(error) => JObject(JField("error", JString(error)))
})
)

object JSON {

  // used in parsing and writing
  implicit val serializers = DefaultFormats + UUIDSerializer + PidTypeSerializer + ResponseResultSerializer

  def parseRequest(json: String): RequestMessage = {
    JsonMethods.parse(json).extract[RequestMessage]
  }

  def writeResponse(response: ResponseMessage): String = {
    Serialization.write(response)
  }
}
