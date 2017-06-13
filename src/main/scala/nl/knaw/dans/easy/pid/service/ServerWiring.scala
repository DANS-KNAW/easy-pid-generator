package nl.knaw.dans.easy.pid.service

import nl.knaw.dans.easy.pid.PropertiesComponent
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

trait ServerWiring extends PidServletComponent with ServletMounterComponent with PidServerComponent {
  this: ServerWiring.Dependencies =>

  override val pidServlet: PidServlet = new PidServlet {}
  override val mounter: ServletMounter = new ServletMounter {}
  override val server: PidServer = new PidServer {
    override val serverPort: Int = properties.properties.getInt("pid-generator.daemon.http.port")
  }
}

object ServerWiring {
  type Dependencies = PidServletComponent.Dependencies
    with ServletMounterComponent.Dependencies
    with PidServerComponent.Dependencies
    with PropertiesComponent
    with DebugEnhancedLogging
}
