package nl.knaw.dans.easy.pid.service

import nl.knaw.dans.easy.pid.{ DatabaseAccessComponent, PropertiesComponent }
import nl.knaw.dans.easy.pid.generator.{ DOIGeneratorWiring, URNGeneratorWiring }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

trait ServiceWiring extends PidServletComponent with ServletMounterComponent with PidServerComponent {
  this: PropertiesComponent with DatabaseAccessComponent with DOIGeneratorWiring with URNGeneratorWiring with DebugEnhancedLogging =>

  val pidServlet: PidServlet = new PidServlet {}
  val mounter: ServletMounter = new ServletMounter {}
  val server: PidServer = new PidServer(properties.properties.getInt("pid-generator.daemon.http.port"))
}
