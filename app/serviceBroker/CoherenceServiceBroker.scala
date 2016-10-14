package serviceBroker

import javax.inject.Inject

import com.google.inject.ImplementedBy
import model.{CoherenceEntity, System}
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext.Implicits.global
/**
  * Created by Joseph Sebastian on 11/10/2016.
  */
@ImplementedBy(classOf[CoherenceServiceBrokerImpl])
trait CoherenceServiceBroker {

  def executeQuery(query: String): CoherenceEntity

}

class CoherenceServiceBrokerImpl @Inject()(ws: WSClient, conf: play.api.Configuration) extends CoherenceServiceBroker {
  private var jsessionId = ""

  val urlPath = conf.getString("coherence.ui.loginPath").get
  val unamne = conf.getString("coherence.ui.uname").get
  val pass = conf.getString("coherence.ui.pwd").get


  private def login() = {
    ws.url(urlPath).withHeaders("Content-Type" -> "application/x-www-form-urlencoded")
      .post(Map("password" -> Seq(pass), "username" -> Seq(unamne), "submit" -> Seq("Login"))) map { response =>
      println(response.allHeaders)
      val jsessionId = response.cookie("JSESSIONID")

      this.jsessionId = jsessionId.get.value.get
    }

  }

  override def executeQuery(query: String): CoherenceEntity = {
//    login()

    CoherenceEntity("Property", List(System("Coherence", 345L)))
  }

}
