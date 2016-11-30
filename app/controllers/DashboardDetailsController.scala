package controllers

import javax.inject.Inject

import play.api.mvc.{Action, Controller}
import services.{EntityCountPropertyContactAPIService}

/**
  * Created by isugum on 11/10/2016.
  */
class DashboardDetailsController @Inject()(entityCountPropertyContactAPIService: EntityCountPropertyContactAPIService,
                                           entityCountPropertyAPIService : EntityCountPropertyAPIService ) extends Controller {

  private def getPropertyContractDetails()  = { entityCountPropertyContactAPIService.getPropertyContractDetailsFromCache() }
  private def getPropertyDetails()  = { entityCountPropertyAPIService.getPropertyDetailsFromCache() }

  def getEntityDetails (entityName: String) = Action {
    try {
      entityName.toLowerCase() match {
        case "propertycontract" => Ok(getPropertyContractDetails())
        case "property" => Ok(getPropertyDetails())
      }
    } catch {
      case e : Exception => e.getStackTrace
        entityCountPropertyAPIService.getPropertyDetails()
        entityCountPropertyContactAPIService.getPropertyContractDetails()
        entityName.toLowerCase() match {
          case "propertycontract" => Ok(getPropertyContractDetails())
          case "property" => Ok(getPropertyDetails())
        }
       // Ok("Updating" + entityName + " json file, pls wait..")
    }

  }

}
