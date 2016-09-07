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

import java.util.UUID

/*
  TODO: add version to RequestMessage (header)
  TODO: add version and sender to ResponseMessage (header)
 */

// a PID can either be an URN or a DOI
sealed abstract class PidType(val name: String)
case object URN extends PidType("urn")
case object DOI extends PidType("doi")

// request
case class RequestHead(requestID: UUID, responseDS: ResponseDatastructure)
case class RequestBody(pidType: PidType)
case class RequestMessage(head: RequestHead, body: RequestBody)

// response result can either be a success or a failure
sealed abstract class ResponseResult
case class ResponseSuccessResult(result: String) extends ResponseResult
case class ResponseFailureResult(error: String) extends ResponseResult

// response
case class ResponseHead(requestID: UUID)
case class ResponseBody(pidType: PidType, result: ResponseResult)
case class ResponseMessage(head: ResponseHead, body: ResponseBody)
