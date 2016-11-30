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
  * Created by isugum on 11/28/2016.
  */
@ImplementedBy(classOf[ EntityCountPropertyAPIServiceImpl])
trait EntityCountPropertyAPIService {
  def getPropertyDetails()
  def getPropertyDetailsFromCache(): JsValue
}

class  EntityCountPropertyAPIServiceImpl @Inject()(coherenceServiceBroker: CoherenceServiceBroker,
                                           aTGDao: ATGDao, gcDao : GCDao,
                                           conf : play.api.Configuration, cache: CacheApi) extends  EntityCountPropertyAPIService {
  lazy val propertyTNameGC = conf.getString("gc.property")
  lazy val propertyTNameATG = conf.getString("atg.property")

  //Count by Property Type
  private def queryAtgForPropertyType(entity: Option[String]) : List[Property] = aTGDao.executeAtgQueryForPropertyType(entity)
  private def queryGcForPropertyType(entity: Option[String]) : List[Property] = gcDao.executeGcQueryForPropertyType(entity)
  //Count By Property Category
  private def queryAtgForPropertyCategory(entity: Option[String]) : List[Property] = aTGDao.executeAtgQueryForPropertyCategory(entity)
  private def queryGcForPropertyCategory(entity: Option[String]) : List[Property] = gcDao.executeGcQueryForPropertyCategory(entity)
  //Count By Property Provisioned type
  private def queryAtgForPropertyProvison(entity: Option[String]) : List[Property] = aTGDao.executeAtgQueryForPropertyProvision(entity)
  private def queryGcForPropertyProvision(entity: Option[String]) : List[Property] = gcDao.executeGcQueryForPropertyProvision(entity)

  override def getPropertyDetails() = {
    try {
      val sysProperties: List[SystemProperties] = List(loadPropertyCntByPropType(), loadPropertyCntByPropCategory(), loadPropertyContractCntByPropProv)
      println("::::::::::: " + sysProperties)
      val json = Json.toJson(sysProperties.map(sysProps =>(SystemEntityDetailsJsonFormat(sysProps.category,
          sysProps.cohproperties.map { sys => (sys.name, sys.count) } toMap,
          sysProps.atgproperties.map { sys => (sys.name, sys.count) } toMap,
          sysProps.gcproperties.map { sys => (sys.name, sys.count) } toMap))))
      val filedir = new File(conf.getString("json.filedir").get)
          filedir.mkdir()
      val jsonWriter = new PrintWriter(conf.getString("json.proppath").get)
          jsonWriter.write(Json.prettyPrint(json))
          jsonWriter.close()
      cache.remove("property.details")
      } catch {
        case e: Exception => println("*********" + e.printStackTrace())
      }
  }

  private def loadPropertyCntByPropType(): SystemProperties = {
    val gcProperties: List[Property] = queryGcForPropertyType(propertyTNameGC)
    val atgProperties: List[Property] = queryAtgForPropertyType(propertyTNameATG)
    val sysProps = SystemProperties("countbyproptype", List.empty, atgProperties, gcProperties)
    sysProps
  }

  private def loadPropertyCntByPropCategory(): SystemProperties = {
    val gcProperties: List[Property] = queryGcForPropertyCategory(propertyTNameGC)
    val atgProperties: List[Property] = queryAtgForPropertyCategory(propertyTNameATG)
    val sysProps = SystemProperties("countbypropcategory", List.empty, atgProperties, gcProperties)
    sysProps
  }

  private def loadPropertyContractCntByPropProv(): SystemProperties = {
    val gcProperties: List[Property] = queryGcForPropertyProvision(propertyTNameGC)
    val atgProperties: List[Property] = queryAtgForPropertyProvison(propertyTNameATG)
    val sysProps = SystemProperties("countbypropprov", List.empty, atgProperties, gcProperties)
    sysProps
  }

  override def getPropertyDetailsFromCache(): JsValue = {
    val propDetails = cache.getOrElse[JsValue]("property.details") {
      Json.parse(Source.fromFile(conf.getString("json.proppath").get).mkString)
    }
    propDetails
  }
}
