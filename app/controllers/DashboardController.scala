package controllers

import javax.inject.Inject

import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import services.EntityCountAPIService
import wrapper.SystemEntityJsonFormat

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by Joseph Sebastian on 11/10/2016.
  */
class DashboardController @Inject()(entityCountAPIService: EntityCountAPIService) extends Controller {

  def getSystemEntityCount() = Action.async {

    entityCountAPIService.getDashboardCount().map{ count =>
      Ok(Json.toJson(count.map(coh => SystemEntityJsonFormat(coh.name, coh.systems.map{ sys => (sys.name, sys.count)} toMap))))
    }
  }
}
