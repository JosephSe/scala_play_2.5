package services


import java.io.{File, PrintWriter}
import javax.inject.Inject

import com.google.inject.ImplementedBy
import dao.{ATGDao, GCDao}
import model.{Property, SystemProperties}
import play.api.cache.CacheApi
import play.api.libs.json.{JsValue, Json}
import serviceBroker.CoherenceServiceBroker
import wrapper.{SystemEntityDetailsJsonFormat}

import scala.io.Source

/**
  * Created by isugum on 11/10/2016.
  */
@ImplementedBy(classOf[ EntityCountPropertyContactAPIServiceImpl])
trait EntityCountPropertyContactAPIService {
  def getPropertyContractDetails()
  def getPropertyContractDetailsFromCache(): JsValue
}

class  EntityCountPropertyContactAPIServiceImpl @Inject()(coherenceServiceBroker: CoherenceServiceBroker,
                                           aTGDao: ATGDao, gcDao : GCDao,
                                           conf : play.api.Configuration, cache: CacheApi) extends  EntityCountPropertyContactAPIService {

  lazy val entities = conf.getList("coherence.entities").get.unwrapped().toArray.toList

  //Count by Status
  private def queryCoherenceForPropertyContractStatus(entity: String) : List[Property] = coherenceServiceBroker.executeQueryForPropertyContractStatus(entity)
  private def queryAtgForPropertyContractStatus(entity: Option[String]) : List[Property] = aTGDao.executeAtgQueryForPropertyContractStatus(entity)
  private def queryGcForPropertyContractStatus(entity: Option[String]) : List[Property] = gcDao.executeGcQueryForPropertyContractStatus(entity)
  //Count By Currency
  private def queryCoherenceForPropertyContractCurrency(entity: String) : List[Property] = coherenceServiceBroker.executeQueryForPropertyContractCurrency(entity)
  private def queryAtgForPropertyContractCurrency(entity: Option[String]) : List[Property] = aTGDao.executeAtgQueryForPropertyContractCurrency(entity)
  private def queryGcForPropertyContractCurrency(entity: Option[String]) : List[Property] = gcDao.executeGcQueryForPropertyContractCurrency(entity)
  //Count By Model
  private def queryCoherenceForPropertyContractModel(entity: String) : List[Property] = coherenceServiceBroker.executeQueryForPropertyContractModel(entity)
  private def queryAtgForPropertyContractModel(entity: Option[String]) : List[Property] = aTGDao.executeAtgQueryForPropertyContractModel(entity)
  private def queryGcForPropertyContractModel(entity: Option[String]) : List[Property] = gcDao.executeGcQueryForPropertyContractModel(entity)

  override def getPropertyContractDetails() = {
    try {
      val sysProperties: List[SystemProperties] = List(loadPropertyContractCntByStatusDetails(), loadPropertyContractCntByCurrencyDetails(), loadPropertyContractCntByModelDetails())
      println("::::::::::: " + sysProperties)
      val json = Json.toJson(sysProperties.map(sysProps =>(SystemEntityDetailsJsonFormat(sysProps.category,
          sysProps.cohproperties.map { sys => (sys.name, sys.count) } toMap,
          sysProps.atgproperties.map { sys => (sys.name, sys.count) } toMap,
          sysProps.gcproperties.map { sys => (sys.name, sys.count) } toMap))))
      val filedir = new File(conf.getString("json.filedir").get)
          filedir.mkdir()
      val jsonWriter = new PrintWriter(conf.getString("json.propcontpath").get)
          jsonWriter.write(Json.prettyPrint(json))
          jsonWriter.close()
      cache.remove("property.contract.details")
      } catch {
        case e: Exception => println("*********" + e.getMessage)
      }
  }

  private def loadPropertyContractCntByStatusDetails(): SystemProperties = {
    val gcProperties: List[Property] = queryGcForPropertyContractStatus(Option("cv_property_contract"))
    val atgProperties: List[Property] = queryAtgForPropertyContractStatus(Option("fit_contract"))
    val cohProperties: List[Property] = queryCoherenceForPropertyContractStatus("PropertyContract")
    val sysProps = SystemProperties("countbystatus", cohProperties, atgProperties, gcProperties)
    sysProps
  }

  private def loadPropertyContractCntByCurrencyDetails(): SystemProperties = {
    val gcProperties: List[Property] = queryGcForPropertyContractCurrency(Option("cv_property_contract"))
    val atgProperties: List[Property] = queryAtgForPropertyContractCurrency(Option("fit_contract"))
    val cohProperties: List[Property] = queryCoherenceForPropertyContractCurrency("PropertyContract")
    val sysProps = SystemProperties("countbycurrency", cohProperties, atgProperties, gcProperties)
    sysProps
  }

  private def loadPropertyContractCntByModelDetails(): SystemProperties = {
    val gcProperties: List[Property] = queryGcForPropertyContractModel(Option("cv_property_contract"))
    val atgProperties: List[Property] = queryAtgForPropertyContractModel(Option("fit_contract"))
    val cohProperties: List[Property] = queryCoherenceForPropertyContractModel("PropertyContract")
    val sysProps = SystemProperties("countbymodel", cohProperties, atgProperties, gcProperties)
    sysProps
  }

  override def getPropertyContractDetailsFromCache(): JsValue = {
    val propContractDetails = cache.getOrElse[JsValue]("property.contract.details") {
      Json.parse(Source.fromFile(conf.getString("json.propcontpath").get).mkString)
    }
    propContractDetails
  }
}
