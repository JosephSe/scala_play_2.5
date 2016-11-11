package controllers

import javax.inject.Inject

import play.api.mvc.{Action, Controller, Request}
import services.{EntityCountAPIService, EntityCountPropertyContactAPIService}

/**
  * Created by Joseph Sebastian on 11/10/2016.
  */
class SystemController @Inject()(entityCountAPIService: EntityCountAPIService,
                                 entityCountPropertyContactAPIService: EntityCountPropertyContactAPIService) extends Controller {

  def refreshJsonCache() = Action {
    try {
      entityCountAPIService.getDashboardCount()
      entityCountPropertyContactAPIService.getPropertyContractDetails()
    }catch {
      case e : Exception => Ok("Exception occurred while refreshing the cache, please retry.." + e.getMessage)
    }
    Ok("Refresh Done Successfully!")
    }
  }
