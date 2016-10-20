package model

import play.api.libs.json._

import scala.concurrent.Future

/**
  * Created by Joseph Sebastian on 11/10/2016.
  */

case class System(name: String, count: Long)

object System {
  implicit val system = Json.format[System]
}

case class SystemEntity(name: String, systems: List[System])

object SystemEntity {
  implicit val systemEntity = Json.format[SystemEntity]
}

case class CoherenceEntityNew(name: String, systems: Map[String, Long])

object CoherenceEntityNew {
  implicit val coherenceEntity = Json.format[CoherenceEntityNew]

//  def apply(coh: CoherenceEntity):CoherenceEntityNew = CoherenceEntityNew(coh.name, coh.systems.map { sys =>
//      (sys.name, sys.count)
//    } toMap)

}

