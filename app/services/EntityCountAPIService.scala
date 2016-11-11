package services

import java.io.{File, PrintWriter}
import javax.inject.Inject

import com.google.inject.ImplementedBy
import dao.{ATGDao, GCDao}
import model.{System, SystemEntity}
import play.api.cache.CacheApi
import play.api.libs.json.{JsValue, Json}
import serviceBroker.CoherenceServiceBroker
import wrapper.{SystemEntityJsonFormat}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

/**
  * Created by Joseph Sebastian on 11/10/2016.
  */
@ImplementedBy(classOf[ EntityCountAPIServiceImpl])
trait EntityCountAPIService {
  def getDashboardCount()
  def getDashboardCountFromCache(): JsValue
}

class  EntityCountAPIServiceImpl @Inject()(coherenceServiceBroker: CoherenceServiceBroker,
                                        aTGDao: ATGDao, gcDao : GCDao,
                                        conf : play.api.Configuration, cache: CacheApi) extends  EntityCountAPIService {

  lazy val entities = conf.getList("coherence.entities").get.unwrapped().toArray.toList

  private def queryCoherence(entity: String): Future[System] = coherenceServiceBroker.executeQuery(entity)
  private def queryAtg(entity: Option[String]): System = aTGDao.executeQuery(entity)
  private def queryGc(entity: Option[String]): System = gcDao.executeQuery(entity)

  override def getDashboardCount() = {
    val listOfSysEntitiy = Future.sequence(entities.map(e =>
      queryCoherence(e.toString).map { s =>
        SystemEntity(e.toString, List(System(s.name, s.count),
          queryAtg(conf.getString("atg." + e.toString.toLowerCase)),
          queryGc(conf.getString("gc." + e.toString.toLowerCase))))
      }))

    // load the result into a count json file for caching purpose
    listOfSysEntitiy.map { count =>
      val json = Json.toJson(count.map(coh => SystemEntityJsonFormat(coh.name, coh.systems.map { sys => (sys.name, sys.count) } toMap)))
      val filedir = new File(conf.getString("json.filedir").get)
          filedir.mkdir()
      val jsonWriter = new PrintWriter(conf.getString("json.filepath").get)
          jsonWriter.write(Json.prettyPrint(json))
          jsonWriter.close()
      cache.remove("entity.counts")
    }
  }

  override def getDashboardCountFromCache(): JsValue = {
    val entityCounts = cache.getOrElse[JsValue]("entity.counts") {
      Json.parse(Source.fromFile(conf.getString("json.filepath").get).mkString)
    }
    entityCounts
  }
}
