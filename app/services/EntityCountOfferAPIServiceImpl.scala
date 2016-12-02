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
  * Created by isugum on 12/01/2016.
  */
@ImplementedBy(classOf[ EntityCountOfferAPIServiceImpl])
trait EntityCountOfferAPIService {
  def getOfferDetails()
  def getOfferDetailsFromCache(): JsValue
}

class  EntityCountOfferAPIServiceImpl @Inject()(coherenceServiceBroker: CoherenceServiceBroker,
                                                aTGDao: ATGDao, gcDao : GCDao,
                                                conf : play.api.Configuration, cache: CacheApi) extends  EntityCountOfferAPIService {
  lazy val OfferCNameCoherence = "Offer"
  lazy val OfferTNameGC = conf.getString("gc.offer")
  lazy val OfferTNameATG = conf.getString("atg.offer")
  //Count by Offer Code
  private def queryCoherenceForOfferCode(entity: String) : List[Property] = coherenceServiceBroker.executeQueryForOfferCode(entity)
  private def queryAtgForOfferCode(entity: Option[String]) : List[Property] = aTGDao.executeAtgQueryForOfferCode(entity)
  private def queryGcForOfferCode(entity: Option[String]) : List[Property] = gcDao.executeGcQueryForOfferCode(entity)
  //Count By Offer Status
  private def queryCoherenceForOfferStatus(entity: String) : List[Property] = coherenceServiceBroker.executeQueryForOfferStatus(entity)
  private def queryAtgForOfferStatus(entity: Option[String]) : List[Property] = aTGDao.executeAtgQueryForOfferStatus(entity)
  private def queryGcForOfferStatus(entity: Option[String]) : List[Property] = gcDao.executeGcQueryForOfferStatus(entity)
  //Count By Stay Type
  private def queryCoherenceForOfferStayType(entity: String) : List[Property] = coherenceServiceBroker.executeQueryForOfferStayType(entity)
  private def queryAtgForOfferStayType(entity: Option[String]) : List[Property] = aTGDao.executeAtgQueryForOfferStayType(entity)
  private def queryGcForOfferStayType(entity: Option[String]) : List[Property] = gcDao.executeGcQueryForOfferStayType(entity)

  override def getOfferDetails() = {
    try {
      val sysProperties: List[SystemProperties] = List(loadOfferCntByOfferCode(), loadOfferCntByOfferStatus(), loadOfferCntByOfferStayType())
      println(" \n Offer Detaisl ::::::::::: " + sysProperties)
      val json = Json.toJson(sysProperties.map(sysProps =>(SystemEntityDetailsJsonFormat(sysProps.category,
        sysProps.cohproperties.map { sys => (sys.name, sys.count) } toMap,
        sysProps.atgproperties.map { sys => (sys.name, sys.count) } toMap,
        sysProps.gcproperties.map { sys => (sys.name, sys.count) } toMap))))
      val filedir = new File(conf.getString("json.filedir").get)
      filedir.mkdir()
      val jsonWriter = new PrintWriter(conf.getString("json.offerpath").get)
      jsonWriter.write(Json.prettyPrint(json))
      jsonWriter.close()
      cache.remove("offer.details")
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }

  private def loadOfferCntByOfferCode(): SystemProperties = {
    val gcProperties: List[Property] = queryGcForOfferCode(OfferTNameGC)
    val atgProperties: List[Property] = queryAtgForOfferCode(OfferTNameATG)
    val cohProperties: List[Property] = queryCoherenceForOfferCode(OfferCNameCoherence)
    val sysProps = SystemProperties("countbyoffercode", cohProperties, atgProperties, gcProperties)
    sysProps
  }

  private def loadOfferCntByOfferStatus(): SystemProperties = {
    val gcProperties: List[Property] = queryGcForOfferStatus(OfferTNameGC)
    val atgProperties: List[Property] = queryAtgForOfferStatus(OfferTNameATG)
    val cohProperties: List[Property] = queryCoherenceForOfferStatus(OfferCNameCoherence)
    val sysProps = SystemProperties("countbyofferstatus",  cohProperties, atgProperties, gcProperties)
    sysProps
  }

  private def loadOfferCntByOfferStayType(): SystemProperties = {
    val gcProperties: List[Property] = queryGcForOfferStayType(OfferTNameGC)
    val atgProperties: List[Property] = queryAtgForOfferStayType(OfferTNameATG)
    val cohProperties: List[Property] = queryCoherenceForOfferStayType(OfferCNameCoherence)
    val sysProps = SystemProperties("countbystaytype",  cohProperties, atgProperties, gcProperties)
    sysProps
  }

  override def getOfferDetailsFromCache(): JsValue = {
    val offerDetails = cache.getOrElse[JsValue]("offer.details") {
      Json.parse(Source.fromFile(conf.getString("json.offerpath").get).mkString)
    }
    offerDetails
  }
}
