package services

import javax.inject.Inject

import com.google.inject.ImplementedBy
import model.{CoherenceEntity, System}
import serviceBroker.CoherenceServiceBroker

/**
  * Created by Joseph Sebastian on 11/10/2016.
  */
@ImplementedBy(classOf[CoherenceAPIServiceImpl])
trait CoherenceAPIService {

  def getCount(collectionType: String): Option[CoherenceEntity]

  def getDashboardCount(): List[CoherenceEntity]
}

object CoherenceQueries {
  def getCountQuery(collectionName: String) = s"select count() from $collectionName"
}

class CoherenceAPIServiceImpl @Inject()(coherenceServiceBroker: CoherenceServiceBroker) extends CoherenceAPIService {

  import CoherenceQueries._

  override def getCount(collectionType: String): Option[CoherenceEntity] = collectionType match {
    case "Property" => Some(queryCoherence(getCountQuery("Property")))
    case _ => None
  }

  private def queryCoherence(query: String): CoherenceEntity = coherenceServiceBroker.executeQuery(query)

  override def getDashboardCount(): List[CoherenceEntity] = {

    List(CoherenceEntity("Property", List(System("coherence", 123L), System("ATG", 345l), System("GC", 543l))))
  }
}
