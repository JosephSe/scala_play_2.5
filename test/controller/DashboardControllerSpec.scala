package controller

import controllers.DashboardController
import model.{System, SystemEntity}
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._

import scala.concurrent.{Await, Future}
import org.scalatestplus.play._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import play.api.mvc._
import play.api.test._
import services.{EntityCountAPIServiceImpl}

/**
  * Created by isugum on 10/19/2016.
  */
class DashboardControllerSpec extends PlaySpec with MockitoSugar with OneAppPerSuite {
  val mockService = mock[EntityCountAPIServiceImpl]
  when(mockService.getDashboardCount) thenReturn Future(List(SystemEntity("Property", List(System("Coherence", 100)))))
  var controller = new DashboardController(mockService)

  "DashboardController" should {
    "use the mocked service to get count" in {
      val result: Future[Result] = controller.getSystemEntityCount().apply(FakeRequest(Helpers.POST, "/dashboard/system/entitycount"))
      val resp = Await.result(result, Duration(100, MILLISECONDS))
      resp.header.status mustBe 200
    }
  }
}