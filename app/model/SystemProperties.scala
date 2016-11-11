package model

import play.api.libs.json.Json

/**
  * Created by isugum on 11/9/2016.
  */
case class Property(name: String, count: Long)

object Property {
  implicit val property = Json.format[Property]
}

case class SystemProperties(category: String, cohproperties: List[Property], atgproperties: List[Property], gcproperties: List[Property])

object SystemProperties {
  implicit val systemProperties = Json.format[SystemProperties]
}