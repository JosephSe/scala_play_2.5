package controllers

import javax.inject.Inject

import play.api.mvc.{Action, Controller}
import services.EntityCountAPIService

/**
  * Created by isugum on 11/10/2016.
  */
class DashboardController @Inject()(entityCountAPIService: EntityCountAPIService) extends Controller {

  def getEntityCount() = Action {
    try {
      val entityListJson = entityCountAPIService.getDashboardCountFromCache()
      Ok(entityListJson)
    } catch {
      case e : Exception =>
         e.printStackTrace()
        entityCountAPIService.getDashboardCount()
        Ok(entityCountAPIService.getDashboardCountFromCache())
        //Ok("No data found to load, Please call the refresh URL to generate the json file")
    }
  }
}
