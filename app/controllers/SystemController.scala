package controllers

import java.io.{File, PrintWriter}
import javax.inject.Inject

import play.api.mvc.{Action, Controller, Request}
import services.EntityCountAPIService
import play.api.libs.json._
import wrapper.SystemEntityJsonFormat

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by Joseph Sebastian on 11/10/2016.
  */
class SystemController @Inject()(entityCountAPIService: EntityCountAPIService) extends Controller {

  def refreshJsonCache() = Action.async {
    entityCountAPIService.getDashboardCount().map{ count =>
      Ok(Json.toJson(count.map(coh => SystemEntityJsonFormat(coh.name, coh.systems.map{ sys => (sys.name, sys.count)} toMap))))
    }
  }

  def getEntityCount() = Action {
    try {
      val entityListJson = entityCountAPIService.getDashboardCountFromCache()
      Ok(entityListJson)
    } catch {
      case e : Exception => Ok("First time run, hence call the refresh URL to generate the json file")
    }

  }
}
