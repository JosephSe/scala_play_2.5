package controllers

import javax.inject.Inject

import model.CoherenceEntityNew
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import services.CoherenceAPIService

/**
  * Created by Joseph Sebastian on 11/10/2016.
  */
class DashboardController @Inject()(coherenceAPIService: CoherenceAPIService) extends Controller {

  def getAllCount() = Action {
    val data = coherenceAPIService.getDashboardCount()
    Ok(Json.toJson(data))
  }
  def getAllCountNew() = Action {
    val data = coherenceAPIService.getDashboardCount().map(CoherenceEntityNew(_))
    Ok(Json.toJson(data))
  }
}
