package dao

import javax.inject.Inject
import com.google.inject.ImplementedBy
import model.System
import play.api.db.{Database, NamedDatabase}

/**
  * Created by isugum on 10/17/2016.
  */
@ImplementedBy(classOf[GCDaoImpl])
trait GCDao{
  def executeQuery(query: Option[String]): System
}

object GCQueries {
  def getGcCountQuery(collectionName: String) = s"select count(1) from $collectionName"
}

class GCDaoImpl @Inject()(@NamedDatabase("gc") gcDb: Database, @NamedDatabase("rm") rmDb: Database, conf : play.api.Configuration)extends GCDao{

  import GCQueries._

  override def executeQuery(entity: Option[String]): System =  entity.get match {
    case mark if (mark.startsWith("mark")) => executeRmQuery(entity)
    case _ => executeGcQuery(entity)
  }

  private def executeGcQuery(entity: Option[String]): System = {
      gcDb.withConnection(conn => {
        val rs = (conn createStatement).executeQuery(getGcCountQuery(entity.get))
        System(conf.getString("system.gc").get, (if (rs.next()) rs.getLong(1) else 0))
      })
  }

  private def executeRmQuery(entity: Option[String]): System = {
      rmDb.withConnection(conn => {
        val rs = (conn createStatement).executeQuery(getGcCountQuery(entity.get))
        System(conf.getString("system.gc").get, (if (rs.next()) rs.getLong(1) else 0))
      })
    }
}
