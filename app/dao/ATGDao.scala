package dao

import javax.inject.Inject

import com.google.inject.ImplementedBy
import model.{Property, System}
import play.api.db.Database

import scala.concurrent.Future

/**
  * Created by isugum on 10/13/2016.
  */
@ImplementedBy(classOf[ATGDaoImpl])
trait ATGDao {
  def executeQuery(query: Option[String]): System

  def executeAtgQueryForPropertyContractStatus(query: Option[String]): List[Property]

  def executeAtgQueryForPropertyContractCurrency(query: Option[String]): List[Property]

  def executeAtgQueryForPropertyContractModel(query: Option[String]): List[Property]
}

object ATGQueries {
  def getAtgCountQuery(collectionName: String) = s"select count(1) from $collectionName"

  def getAtgDetailsQuery(collectionName: String) = s"SELECT status, COUNT(1) AS cnt FROM $collectionName WHERE status IS NOT NULL GROUP BY status"

  def getAtgCntByCurrencyQuery(collectionName: String) = s"SELECT currency, cnt FROM (SELECT currency, COUNT(PROVIDER_ID) AS cnt FROM $collectionName " +
    s"GROUP BY currency ORDER BY cnt DESC) WHERE ROWNUM <= 5 " +
    s"UNION " +
    s"SELECT 'Others', COUNT(currency) AS cnt FROM $collectionName WHERE currency " +
    s"NOT IN (SELECT currency FROM (SELECT currency, COUNT(PROVIDER_ID) AS cnt FROM $collectionName " +
    s"GROUP BY currency ORDER BY cnt DESC) WHERE ROWNUM <= 5)"

  def getAtgCntByModelQuery(collectionName: String) = s"SELECT model, COUNT(1) AS cnt FROM $collectionName WHERE model IS NOT NULL GROUP BY model"
}

class ATGDaoImpl @Inject()(conf: play.api.Configuration, db: Database) extends ATGDao {

  import ATGQueries._

  override def executeQuery(entity: Option[String]): System = entity match {
    case None => System(conf.getString("system.atg").get, 0)
    case _ => executeATGQuery(entity)
  }

  private def executeATGQuery(entity: Option[String]): System = {
    db.withConnection(conn => {
      val rs = (conn createStatement).executeQuery(getAtgCountQuery(entity.get))
      System(conf.getString("system.atg").get, (if (rs.next()) rs.getLong(1) else 0))
    })
  }

  override def executeAtgQueryForPropertyContractStatus(entity: Option[String]): List[Property] = {
    db.withConnection(conn => {
      val rs = (conn createStatement).executeQuery(getAtgDetailsQuery(entity.get))
      val countByStatus = new Iterator[Property] {
        def hasNext = rs.next()

        def next() = Property(rs.getString("status") match {
          case "608" => "PENDING"
          case "610" => "CANCELLED"
          case "607" => "LIVE"
          case "609" => "SUSPENDED"
        }, rs.getInt("cnt"))
      }.toStream
      countByStatus.toList
    })
  }


  override def executeAtgQueryForPropertyContractCurrency(entity: Option[String]): List[Property] = {
    db.withConnection(conn => {
      val rs = (conn createStatement).executeQuery(getAtgCntByCurrencyQuery(entity.get))
      val countByCurrency = new Iterator[Property] {
        def hasNext = rs.next()

        def next() = Property(rs.getString("currency"), rs.getInt("cnt"))
      }.toStream
      countByCurrency.toList
    })
  }

  override def executeAtgQueryForPropertyContractModel(entity: Option[String]): List[Property] = {
    db.withConnection(conn => {
      val rs = (conn createStatement).executeQuery(getAtgCntByModelQuery(entity.get))
      val countByModel = new Iterator[Property] {
        def hasNext = rs.next()

        def next() = Property(rs.getString("model") match {
          case "612" => "MARGIN"
          case "611" => "STATIC"
        }, rs.getInt("cnt"))
      }.toStream
      countByModel.toList
    })
  }
}
