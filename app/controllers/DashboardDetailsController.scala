package controllers

import javax.inject.Inject

import play.api.mvc.{Action, Controller}
import services.{EntityCountOfferAPIService, EntityCountPropertyAPIService, EntityCountPropertyContactAPIService}

/**
  * Created by isugum on 11/10/2016.
  */
class DashboardDetailsController @Inject()(entityCountPropertyContactAPIService: EntityCountPropertyContactAPIService,
                                           entityCountPropertyAPIService : EntityCountPropertyAPIService,
                                           entityCountOfferAPIService: EntityCountOfferAPIService) extends Controller {

  private def getPropertyContractDetails()  = { entityCountPropertyContactAPIService.getPropertyContractDetailsFromCache() }
  private def getPropertyDetails()  = { entityCountPropertyAPIService.getPropertyDetailsFromCache() }
  private def getOfferDetails()  = { entityCountOfferAPIService.getOfferDetailsFromCache() }

  def getEntityDetails (entityName: String) = Action {
    try {
      entityName.toLowerCase() match {
        case "propertycontract" => Ok(getPropertyContractDetails())
        case "property" => Ok(getPropertyDetails())
        case "offer" => Ok(getOfferDetails())
      }
    } catch {
      case e : Exception => e.getStackTrace
        entityCountPropertyAPIService.getPropertyDetails()
        entityCountPropertyContactAPIService.getPropertyContractDetails()
        entityCountOfferAPIService.getOfferDetails()
        entityName.toLowerCase() match {
          case "propertycontract" => Ok(getPropertyContractDetails())
          case "property" => Ok(getPropertyDetails())
          case "offer" => Ok(getOfferDetails())
        }
       // Ok("Updating" + entityName + " json file, pls wait..")
    }

  }

}
