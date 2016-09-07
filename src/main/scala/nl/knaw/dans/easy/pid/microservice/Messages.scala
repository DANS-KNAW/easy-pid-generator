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
