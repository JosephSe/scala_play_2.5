package wrapper

import model.SystemEntity
import play.api.libs.json.Json

/**
  * Created by isugum on 10/18/2016.
  */
case class SystemEntityJsonFormat(name: String, systems: Map[String, Long])

object SystemEntityJsonFormat {
  implicit val systemEntityJsonFormat = Json.format[SystemEntityJsonFormat]

    def apply(sysEnt: SystemEntity):SystemEntityJsonFormat = SystemEntityJsonFormat(sysEnt.name, sysEnt.systems.map { sys =>
        (sys.name, sys.count)
      } toMap)

}





