package serviceBroker

import java.util
import java.util.concurrent.TimeUnit
import javax.inject.Inject

import com.google.inject.ImplementedBy
import model.{Property, System}
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.impl.client._
import org.apache.http.message.BasicNameValuePair
import org.apache.http.protocol.BasicHttpContext
import org.apache.http.impl.client.DefaultHttpClient
import play.api.cache.CacheApi
import play.api.libs.json.JsArray
import play.api.libs.ws.{WSClient, WSResponse}
import scala.collection.immutable.List

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.util.Success

/**
  * Created by Joseph Sebastian on 11/10/2016.
  */
@ImplementedBy(classOf[CoherenceServiceBrokerImpl])
trait CoherenceServiceBroker {
  def executeQuery(query: String): Future[System]
  def executeQueryForPropertyContractStatus(query: String) : List[Property]
  def executeQueryForPropertyContractCurrency(query: String) : List[Property]
  def executeQueryForPropertyContractModel(query: String) : List[Property]
}

object CoherenceQueries {
  def getCohCountQuery(collectionName: String) = s"select count() from $collectionName"
  def getCohCntByStatusQuery(collectionName: String, status: String) = s"SELECT COUNT() FROM $collectionName WHERE value().contractStatus like '$status'"
  def getCohCntByCurrencyQuery(collectionName: String) = s"SELECT value().currency, COUNT() FROM $collectionName WHERE  value().currency IS NOT NULL GROUP BY value().currency"
  def getCohCntByModelQuery(collectionName: String) = s"SELECT value().contractModel, COUNT() FROM $collectionName  GROUP BY value().contractModel"
}

//noinspection ScalaDeprecation
class CoherenceServiceBrokerImpl @Inject()(ws: WSClient, conf: play.api.Configuration, cache: CacheApi) extends CoherenceServiceBroker {

  import CoherenceQueries._

  private def getSessionId(): String = {
    var jsessionId: String = ""
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
    } catch {
      case e: Exception => httpClient.execute(httpPost, httpContext).getStatusLine.getStatusCode match {
        case 200 => jsessionId = cookieStore.getCookies.get(0).getName + "=" + cookieStore.getCookies.get(0).getValue
          jsessionId
      }
    }
  }

  override def executeQuery(entity: String): Future[System] = {
    val sessionId = cache.getOrElse[String]("jsessionId", Duration.create(conf.getInt("jsessionId.timeout").get, TimeUnit.MINUTES)) {
      getSessionId()
    }
    println("::::::::::::::::: " + sessionId)
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

  override def executeQueryForPropertyContractStatus(entity: String): List[Property] = {
    val sessionId = cache.getOrElse[String]("jsessionId", Duration.create(conf.getInt("jsessionId.timeout").get, TimeUnit.MINUTES)) {
      getSessionId()
    }
    println("::::::::::::::::: " + sessionId)
    entity match {
      case "PropertyContract" =>
        val statusList = conf.getList("coherence.contStatus").get.unwrapped().toArray.toList

        val res : List[Future[Property]] = statusList.map { s =>
            ws.url(conf.getString("coherence.ui.url").get)
              .withHeaders("Cookie" -> sessionId, "Accept" -> "application/json", "Content-Type" -> "application/x-www-form-urlencoded")
              .withFollowRedirects(true)
              .post(Map("cohql" -> Seq(getCohCntByStatusQuery(entity, s.toString))))
              .map {
                response => { Property(s.toString, (response.json \ "result").as[Long])
                }
              }.recoverWith {
              case e => Future {
                Property(s.toString, 0)
              }
            }
        }
        val futset: Future[List[Property]] = Future.sequence(res)
        Await.result(futset.map(lp => lp), 5 seconds)
    }
  }

  override def executeQueryForPropertyContractCurrency(entity: String): List[Property] = {
    val sessionId = cache.getOrElse[String]("jsessionId", Duration.create(conf.getInt("jsessionId.timeout").get, TimeUnit.MINUTES)) {
      getSessionId()
    }
    println("::::::::::::::::: " + sessionId)
    entity match {
      case "PropertyContract" =>
          val res: Future[List[Property]] = {
          ws.url(conf.getString("coherence.ui.url").get)
            .withHeaders("Cookie" -> sessionId, "Accept" -> "application/json", "Content-Type" -> "application/x-www-form-urlencoded")
            .withFollowRedirects(true)
            .post(Map("cohql" -> Seq(getCohCntByCurrencyQuery(entity))))
            .map {
              response => {
                processCountByCurrencyResp(response)
               }
            }.recoverWith {
            case e => Future{ List(Property("currency",0))}
            }
          }
        Await.result(res.map(lp=>lp), 5 seconds)
    }
    }

  override def executeQueryForPropertyContractModel(entity: String): List[Property] = {
    val sessionId = cache.getOrElse[String]("jsessionId", Duration.create(conf.getInt("jsessionId.timeout").get, TimeUnit.MINUTES)) {
      getSessionId()
    }
    println("::::::::::::::::: " + sessionId)
    entity match {
      case "PropertyContract" =>
        val res: Future[List[Property]] = {
          ws.url(conf.getString("coherence.ui.url").get)
            .withHeaders("Cookie" -> sessionId, "Accept" -> "application/json", "Content-Type" -> "application/x-www-form-urlencoded")
            .withFollowRedirects(true)
            .post(Map("cohql" -> Seq(getCohCntByModelQuery(entity))))
            .map {
              response => {
                processCountByModelResp(response)
              }
            }.recoverWith {
            case e => Future{ List(Property("Model",0))}
          }
        }
        Await.result(res.map(lp=>lp), 5 seconds)
    }
  }

  private def processCountByCurrencyResp(response: WSResponse): List[Property] = {
      var buffer = new ListBuffer[Property]
      val keys = response.json.as[JsArray]
      val map = {
        keys.value.map(f => (f \ "key").as[String] -> (f \ "value").as[Long]).toMap.toList sortBy (_._2)
      }
      val top5 = map.takeRight(5)
      val itr = top5.iterator
      while (itr.hasNext) {
        val top5Val = itr.next()
        buffer += Property(top5Val._1, top5Val._2)
      }
      var Others: Long = 0
      for (i <- 1 to map.size - 5) {
        Others = Others + map(i)._2
      }
      buffer += (Property("Others", Others))
      buffer.toList
    }

  private def processCountByModelResp(response: WSResponse): List[Property] = {
    var buffer = new ListBuffer[Property]
    val keys = response.json.as[JsArray]
    val map = {
      keys.value.map(f => (f \ "key").as[String] -> (f \ "value").as[Long]).toMap.toList
    }
    val itr = map.iterator
    while (itr.hasNext) {
      val value = itr.next()
      buffer += Property(value._1, value._2)
    }
    buffer.toList
  }
}
