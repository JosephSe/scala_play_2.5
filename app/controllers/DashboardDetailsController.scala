package controllers

import javax.inject.Inject

import play.api.mvc.{Action, Controller}
import services.{EntityCountPropertyContactAPIService}

/**
  * Created by isugum on 11/10/2016.
  */
class DashboardDetailsController @Inject()(entityCountPropertyContactAPIService: EntityCountPropertyContactAPIService) extends Controller {

  private def getPropertyContractDetails()  = { entityCountPropertyContactAPIService.getPropertyContractDetailsFromCache() }

  def getEntityDetails (entityName: String) = Action {
    try {
      entityName.toLowerCase() match {
        case "propertycontract" => Ok(getPropertyContractDetails())
      }
    } catch {
      case e : Exception => e.getStackTrace; Ok("First time run, hence call the refresh URL to generate the " + entityName + " json file")
    }

  }

}
