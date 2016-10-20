package services

import javax.inject.Inject

import com.google.inject.ImplementedBy
import dao.{ATGDao, GCDao}
import model.{SystemEntity, System}
import serviceBroker.CoherenceServiceBroker

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by Joseph Sebastian on 11/10/2016.
  */
@ImplementedBy(classOf[ EntityCountAPIServiceImpl])
trait EntityCountAPIService {
  def getDashboardCount(): Future[List[SystemEntity]]
}

class  EntityCountAPIServiceImpl @Inject()(coherenceServiceBroker: CoherenceServiceBroker,
                                        aTGDao: ATGDao, gcDao : GCDao,
                                        conf : play.api.Configuration) extends  EntityCountAPIService {

  lazy val entities = conf.getList("coherence.entities").get.unwrapped().toArray.toList

  private def queryCoherence(entity: String): Future[System] = coherenceServiceBroker.executeQuery(entity)
  private def queryAtg(entity: Option[String]): System = aTGDao.executeQuery(entity)
  private def queryGc(entity: Option[String]): System = gcDao.executeQuery(entity)

  override def getDashboardCount(): Future[List[SystemEntity]] = {
     Future.sequence(entities.map(e =>
      queryCoherence(e.toString).map { s =>
        SystemEntity(e.toString, List(System(s.name, s.count),
          queryAtg(conf.getString("atg."+e.toString.toLowerCase)),
            queryGc(conf.getString("gc."+e.toString.toLowerCase))))
      }))

     }
}
