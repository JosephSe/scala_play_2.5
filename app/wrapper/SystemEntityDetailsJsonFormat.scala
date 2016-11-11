package wrapper

import model.{SystemProperties}
import play.api.libs.json.Json

/**
  * Created by isugum on 11/8/2016.
  */
case class SystemEntityDetailsJsonFormat(category: String, cohproperties: Map[String, Long], atgproperties: Map[String, Long], gcproperties: Map[String, Long])

object SystemEntityDetailsJsonFormat {
  implicit val systemEntityDetailsJsonFormat = Json.format[SystemEntityDetailsJsonFormat]

  def apply(sysPros: SystemProperties):SystemEntityDetailsJsonFormat = SystemEntityDetailsJsonFormat(sysPros.category,
    sysPros.cohproperties.map { prop =>  (prop.name, prop.count) } toMap,
    sysPros.atgproperties.map { prop =>  (prop.name, prop.count) } toMap,
    sysPros.gcproperties.map { prop =>  (prop.name, prop.count) } toMap)

}