package serviceBroker

import java.util
import java.util.concurrent.TimeUnit
import javax.inject.Inject

import com.google.inject.ImplementedBy
import model.System
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.impl.client._
import org.apache.http.message.BasicNameValuePair
import org.apache.http.protocol.BasicHttpContext
import org.apache.http.impl.client.DefaultHttpClient
import play.api.cache.CacheApi
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration

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

//noinspection ScalaDeprecation
class CoherenceServiceBrokerImpl @Inject()(ws: WSClient, conf: play.api.Configuration, cache: CacheApi) extends CoherenceServiceBroker {
  import CoherenceQueries._

  /**
    *
    * login to coherence and get the jsessionid for future requests
    * This has been done using apache HttpClient API,
    * as play doesn't seem to be returning the jsessionid in the response.
    *
    * @return
    */
  private def getSessionId() : String = {
    var jsessionId : String = ""
    val httpClient: DefaultHttpClient = new DefaultHttpClient()
        httpClient.setRedirectStrategy(new LaxRedirectStrategy())

    val httpPost = new HttpPost(conf.getString("coherence.ui.loginPath").get)
        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded")
    val nvplist: util.ArrayList[BasicNameValuePair] = new util.ArrayList[BasicNameValuePair]
        nvplist.add(new BasicNameValuePair("username", conf.getString("coherence.ui.uname").get))
        nvplist.add(new BasicNameValuePair("password", conf.getString("coherence.ui.pwd").get))
        nvplist.add(new BasicNameValuePair("submit", "Login"))
        httpPost.setEntity(new UrlEncodedFormEntity(nvplist))

    val httpContext = new BasicHttpContext
    val cookieStore = new BasicCookieStore
        httpContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore)

    try {
      httpClient.execute(httpPost, httpContext).getStatusLine.getStatusCode match {
        case 200 => jsessionId = cookieStore.getCookies.get(0).getName + "=" + cookieStore.getCookies.get(0).getValue
          jsessionId
       }
    }catch {
        case e: Exception => httpClient.execute(httpPost, httpContext).getStatusLine.getStatusCode match {
          case 200 => jsessionId = cookieStore.getCookies.get(0).getName + "=" + cookieStore.getCookies.get(0).getValue
            jsessionId
        }
    }
  }

  override def executeQuery(entity: String): Future[System] = {

    val sessionId = cache.getOrElse[String]("jsessionId", Duration.create(conf.getInt("jsessionId.timeout").get, TimeUnit.MINUTES)){
      getSessionId()
    }
    println("::::::::::::::::: "+ sessionId)
    ws.url(conf.getString("coherence.ui.url").get)
      .withHeaders("Cookie" -> sessionId, "Accept" -> "application/json", "Content-Type" -> "application/x-www-form-urlencoded")
      .withFollowRedirects(true)
      .post(Map("cohql" -> Seq(getCohCountQuery(entity))))
      .map {
        response => System(conf.getString("system.coherence").get, (response.json \ "result").as[Long])
      }.recoverWith {
      case e => Future {
        System(conf.getString("system.coherence").get, 0)
      }
    }
  }
}
