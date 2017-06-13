package nl.knaw.dans.easy.pid

import scala.sys.process._

trait ServerTestSupportFixture {

  def call(command: String): String = (command !! ProcessLogger(_ => ())).trim

  def callService(path: String = "pids"): String = call(s"curl http://localhost:8060/$path")

  def postUrn: String = call("curl -X POST http://localhost:8060/pids?type=urn")

  def postDoi: String = call("curl -X POST http://localhost:8060/pids?type=doi")

  def successful: String = "Persistent Identifier Generator running"
}
