package nl.knaw.dans.easy.pid.service

import nl.knaw.dans.easy.pid.PropertiesComponent
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

trait ServiceWiring extends PidServletComponent with ServletMounterComponent with PidServerComponent {
  this: ServiceWiring.Dependencies =>

  val pidServlet: PidServlet = new PidServlet {}
  val mounter: ServletMounter = new ServletMounter {}
  val server: PidServer = new PidServer(properties.properties.getInt("pid-generator.daemon.http.port"))
}

object ServiceWiring {
  type Dependencies = PidServletComponent.Dependencies
    with ServletMounterComponent.Dependencies
    with PidServerComponent.Dependencies
    with PropertiesComponent
    with DebugEnhancedLogging
}
