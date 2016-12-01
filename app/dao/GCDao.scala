package dao

import javax.inject.Inject

import com.google.inject.ImplementedBy
import model.{Property, System, SystemEntity}
import play.api.db.{Database, NamedDatabase}

/**
  * Created by isugum on 10/17/2016.
  */
@ImplementedBy(classOf[GCDaoImpl])
trait GCDao {
  def executeQuery(query: Option[String]): System

  def executeGcQueryForPropertyContractStatus(query: Option[String]): List[Property]
  def executeGcQueryForPropertyContractCurrency(query: Option[String]): List[Property]
  def executeGcQueryForPropertyContractModel(query: Option[String]): List[Property]

  def executeGcQueryForPropertyType(query: Option[String]): List[Property]
  def executeGcQueryForPropertyCategory(query: Option[String]): List[Property]
  def executeGcQueryForPropertyProvision(query: Option[String]): List[Property]

}

object GCQueries {
  def getGcCountQuery(collectionName: String) = s"SELECT COUNT(1) FROM $collectionName"

  //Property Contract Query Details
  def getGcCntByStatusQuery(collectionName: String) = s"SELECT status, COUNT(1) AS cnt FROM $collectionName WHERE status IS NOT NULL GROUP BY status"
  def getGcCntByCurrencyQuery(collectionName: String) = s"SELECT currency, cnt FROM (SELECT currency, COUNT(PROVIDER_ID) AS cnt FROM $collectionName " +
    s"GROUP BY currency ORDER BY cnt DESC) WHERE ROWNUM <= 5 " +
    s"UNION " +
    s"SELECT 'Others', COUNT(currency) AS cnt FROM $collectionName WHERE currency " +
    s"NOT IN (SELECT currency FROM (SELECT currency, COUNT(PROVIDER_ID) AS cnt FROM $collectionName " +
    s"GROUP BY currency ORDER BY cnt DESC) WHERE ROWNUM <= 5)"
  def getGcCntByModelQuery(collectionName: String) = s"SELECT model, COUNT(1) AS cnt FROM $collectionName WHERE model IS NOT NULL GROUP BY model"

  //Property Query Details
  def getGcCntByPropertyType(collectionName: String) = s"SELECT type, COUNT(1) AS cnt FROM $collectionName WHERE TYPE IS NOT NULL GROUP BY type"
  def getGcCntByPropertyCategory(collectionName: String, propertyTName:String, propCatTName:String) = s"SELECT pc.code AS category, COUNT(1) AS cnt FROM $propertyTName p, $propCatTName " +
    s"pc WHERE p.category_id = pc.id GROUP BY pc.code"
  def getGcCntByPropertyProvisionQuery(collectionName: String, propProvTName:String) = s"SELECT pp.provision_type AS type, COUNT(1) AS cnt FROM $collectionName p, $propProvTName pp " +
    s"WHERE p.id = pp.property_id GROUP BY pp.provision_type"
}

class GCDaoImpl @Inject()(@NamedDatabase("gc") gcDb: Database, @NamedDatabase("rm") rmDb: Database, conf: play.api.Configuration) extends GCDao {

  import GCQueries._

  lazy val propertyTNameGC = conf.getString("gc.property").get
  lazy val propertyCatTNameGC = conf.getString("gc.propertycategory").get
  lazy val propProvTName = conf.getString("gc.propertyprovision").get

  override def executeQuery(entity: Option[String]): System = entity.get match {
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

  override def executeGcQueryForPropertyContractStatus(entity: Option[String]): List[Property] = {
    gcDb.withConnection(conn => {
      val rs = (conn createStatement).executeQuery(getGcCntByStatusQuery(entity.get))
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

    })
  }

  override def executeGcQueryForPropertyContractCurrency(entity: Option[String]): List[Property] = {
    gcDb.withConnection(conn => {
      val rs = (conn createStatement).executeQuery(getGcCntByCurrencyQuery(entity.get))
      val countByCurrency = new Iterator[Property] {
        def hasNext = rs.next()
        def next() = Property(rs.getString("currency"), rs.getInt("cnt"))
      }.toStream
      countByCurrency.toList
    })
  }

  override def executeGcQueryForPropertyContractModel(entity: Option[String]): List[Property] = {
    gcDb.withConnection(conn => {
      val rs = (conn createStatement).executeQuery(getGcCntByModelQuery(entity.get))
      val countByCurrency = new Iterator[Property] {
        def hasNext = rs.next()
        def next() = Property(rs.getString("model") match {
          case "M" => "MARGIN"
          case "S" => "STATIC"
        }, rs.getInt("cnt"))
      }.toStream
      countByCurrency.toList
    })
  }

  override def executeGcQueryForPropertyType(entity: Option[String]): List[Property] = {
    gcDb.withConnection(conn => {
      val rs = (conn createStatement).executeQuery(getGcCntByPropertyType(entity.get))
      val countByPropType = new Iterator[Property] {
        def hasNext = rs.next()
        def next() = Property(rs.getString("type") match {
          case "H" => "HOTEL"
          case "A" => "APARTMENT"
        }, rs.getInt("cnt"))
      }.toStream
      countByPropType.toList
    })
  }

  override def executeGcQueryForPropertyCategory(entity: Option[String]): List[Property] = {
    gcDb.withConnection(conn => {
      val rs = (conn createStatement).executeQuery(getGcCntByPropertyCategory(entity.get, propertyTNameGC, propertyCatTNameGC))
      
      val countByPropCate = new Iterator[Property] {
        def hasNext = rs.next()
        def next() = Property(rs.getString("category") match {
          case "SD" => "SUPERIOR DELUXE"
          case "D" => "DELUXE"
          case "T" => "TOURIST CLASS"
          case "SF" => "SUPERIOR FIRST"
          case "F" => "FIRST CLASS"
          case "ST" => "SUPERIOR TOURIST"
        }, rs.getInt("cnt"))
      }.toStream
      countByPropCate.toList
    })
  }

  override def executeGcQueryForPropertyProvision(entity: Option[String]): List[Property] = {
    gcDb.withConnection(conn => {
      val rs = (conn createStatement).executeQuery(getGcCntByPropertyProvisionQuery(entity.get, propProvTName))
      val countByPropProv = new Iterator[Property] {
        def hasNext = rs.next()
        def next() = Property(rs.getString("type") match {
          case "S" => "SHUTTLE"
          case "P" => "PARKING"
        }, rs.getInt("cnt"))
      }.toStream
      countByPropProv.toList
    })
  }

}
