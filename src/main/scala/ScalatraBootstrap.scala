import org.scalatra.LifeCycle

import javax.servlet.ServletContext
import nl.knaw.dans.easy.pid.PidService

class ScalatraBootstrap extends LifeCycle {

  override def init(context: ServletContext) {

    context mount (new PidService, "/pids")
  }
}