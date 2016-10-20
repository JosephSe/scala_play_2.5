package serviceBroker

import javax.inject.Inject
import javax.xml.ws.spi.http.HttpContext

import com.google.inject.ImplementedBy
import model.System
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
  * Created by Joseph Sebastian on 11/10/2016.
  */
@ImplementedBy(classOf[CoherenceServiceBrokerImpl])
trait CoherenceServiceBroker {
  def executeQuery(query: String): Future[System]
}

object CoherenceQueries {
  def getCohCountQuery(collectionName: String) = s"select count() from $collectionName"
}

class CoherenceServiceBrokerImpl @Inject()(ws: WSClient, conf: play.api.Configuration) extends CoherenceServiceBroker {
  import CoherenceQueries._

  private var jsessionId = ""

  val urlPath = conf.getString("coherence.ui.loginPath").get
  val uname = conf.getString("coherence.ui.uname").get
  val pass = conf.getString("coherence.ui.pwd").get


  private def login() = {
    ws.url(urlPath).withHeaders("Content-Type" -> "application/x-www-form-urlencoded")
      .post(Map("password" -> Seq(pass), "username" -> Seq(uname), "submit" -> Seq("Login"))) map { response =>
      val jsessionId = response.cookie("JSESSIONID")

      this.jsessionId = jsessionId.get.value.get
    }

  }

  override def executeQuery(entity: String): Future[System] = {
  // login()

    ws.url(conf.getString("coherence.ui.url").get)
      .withHeaders("Cookie" -> conf.getString("coherence.jsessionId").get, "Accept" -> "application/json", "Content-Type" ->"application/x-www-form-urlencoded")
      .withFollowRedirects(true)
      .post(Map("cohql" -> Seq(getCohCountQuery(entity))))
      .map {
        response => System(conf.getString("system.coherence").get, (response.json \ "result").as[Long])
      }.recoverWith {
      case e => Future {System(conf.getString("system.coherence").get, 0)}
    }
  }
}
