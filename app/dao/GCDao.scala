package dao

import javax.inject.Inject

import com.google.inject.ImplementedBy
import model.{Property, System, SystemEntity}
import play.api.db.{Database, NamedDatabase}

/**
  * Created by isugum on 10/17/2016.
  */
@ImplementedBy(classOf[GCDaoImpl])
trait GCDao{
  def executeQuery(query: Option[String]): System
  def executeGcQueryForPropertyContractStatus(query: Option[String]) : List[Property]
  def executeGcQueryForPropertyContractCurrency(query: Option[String]) : List[Property]
  def executeGcQueryForPropertyContractModel(query: Option[String]) : List[Property]

}

object GCQueries {
  def getGcCountQuery(collectionName: String) = s"SELECT COUNT(1) FROM $collectionName"
  def getGcCntByStatusQuery(collectionName: String) = s"SELECT status, COUNT(1) AS cnt FROM $collectionName WHERE status IS NOT NULL GROUP BY status"
  def getGcCntByCurrencyQuery(collectionName: String) =  s"SELECT currency, cnt FROM (SELECT currency, COUNT(PROVIDER_ID) AS cnt FROM $collectionName " +
                                                          s"GROUP BY currency ORDER BY cnt DESC) WHERE ROWNUM <= 5 " +
                                                          s"UNION " +
                                                          s"SELECT 'Others', COUNT(currency) AS cnt FROM $collectionName WHERE currency " +
                                                          s"NOT IN (SELECT currency FROM (SELECT currency, COUNT(PROVIDER_ID) AS cnt FROM $collectionName " +
                                                          s"GROUP BY currency ORDER BY cnt DESC) WHERE ROWNUM <= 5)"
  def getGcCntByModelQuery(collectionName: String) = s"SELECT model, COUNT(1) AS cnt FROM $collectionName WHERE model IS NOT NULL GROUP BY model"
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

  override def executeGcQueryForPropertyContractStatus(entity: Option[String]) : List[Property] =  {
    gcDb.withConnection(conn => {
      entity.get match {
        case "cv_property_contract" => val rs = (conn createStatement).executeQuery(getGcCntByStatusQuery(entity.get))
          val countByStatus = new Iterator[Property] {
            def hasNext = rs.next()
            def next() = Property(rs.getString("status") match {
              case "P" => "PENDING"
              case "X" => "CANCELLED"
              case "L" => "LIVE"
              case "S" => "SUSPENDED"
            }, rs.getInt("cnt"))
          }.toStream
          countByStatus.toList
      }

    })
    }

  override def executeGcQueryForPropertyContractCurrency(entity: Option[String]) : List[Property] =  {
    gcDb.withConnection(conn => {
      entity.get match {
        case "cv_property_contract" => val rs = (conn createStatement).executeQuery(getGcCntByCurrencyQuery(entity.get))
            val countByCurrency = new Iterator[Property] {
            def hasNext = rs.next()
            def next() = Property(rs.getString("currency"), rs.getInt("cnt"))
          }.toStream
          countByCurrency.toList
      }
    })
  }

  override def executeGcQueryForPropertyContractModel(entity: Option[String]) : List[Property] =  {
    gcDb.withConnection(conn => {
      entity.get match {
        case "cv_property_contract" => val rs = (conn createStatement).executeQuery(getGcCntByModelQuery(entity.get))
          val countByCurrency = new Iterator[Property] {
            def hasNext = rs.next()
            def next() = Property(rs.getString("model") match {
              case "M" => "MARGIN"
              case "S" => "STATIC"
            }, rs.getInt("cnt"))
          }.toStream
          countByCurrency.toList
      }
    })
  }

}
