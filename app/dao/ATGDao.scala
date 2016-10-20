package dao

import javax.inject.Inject

import com.google.inject.ImplementedBy
import model.System
import play.api.db.Database

import scala.concurrent.Future

/**
  * Created by isugum on 10/13/2016.
  */
@ImplementedBy(classOf[ATGDaoImpl])
trait ATGDao {
  def executeQuery(query: Option[String]): System
}

  object ATGQueries {
    def getAtgCountQuery(collectionName: String) = s"select count(1) from $collectionName"
  }

  class ATGDaoImpl @Inject() (conf: play.api.Configuration, db: Database) extends ATGDao{

  import ATGQueries._

  override def executeQuery(entity: Option[String]): System =  entity match {
    case None => System(conf.getString("system.atg").get, 0)
    case _ => executeATGQuery(entity)
  }

    private def executeATGQuery(entity: Option[String]): System = {
        db.withConnection(conn => {
          val rs = (conn createStatement).executeQuery(getAtgCountQuery(entity.get))
          System(conf.getString("system.atg").get, (if (rs.next()) rs.getLong(1) else 0))
        })
      }
  }