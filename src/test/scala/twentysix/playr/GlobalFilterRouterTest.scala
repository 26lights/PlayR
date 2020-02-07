package twentysix.playr

import org.scalatest.{FunSpec, Matchers}
import play.api.http.HttpRequestHandler
import play.api.mvc.{Results, Action, RequestHeader}
import play.api.routing.Router
import play.api.test.FakeRequest
import play.api.test.Helpers._
import twentysix.playr.core.BaseResource
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Handler
import play.api.mvc.EssentialAction

class GlobalFilterRouterTest extends FunSpec with Matchers with PlayRApp {
  def extResourceRouter = {
    val extController = injector.instanceOf[ExtendedTestController]
    extController.setControllerComponents(stubControllerComponents())
    new RestResourceRouter[ExtendedTestController](extController)
      .add("hello", GET, extController.hello _)
  }

  implicit val filter = new RestRouterFilter() {
    def filter = {
      case RouterFilterContext(actionType, path) =>
        (requestHeader: RequestHeader, next: () => Option[Handler]) =>
          next().map {
            case action: EssentialAction => TestFilter.testAction(actionType, path)(action)
          }
    }
  }

  describe("A RestResourceRouter with a global filter") {
    it("should return NotFound for an unexpected resource id get") {
      withFilteredApp(injector.instanceOf[TestControllerAll]) { app =>
        val Some(result) = route(app, FakeRequest("GET", "/test"))
        status(result) should be(NOT_FOUND)
      }
    }
    it("should return Ok(read) for an expected resource id get") {
      withFilteredApp(injector.instanceOf[TestControllerAll]) { app =>
        val Some(result) = route(app, FakeRequest("GET", "/26"))
        status(result) should be(OK)
        header(TestFilter.TestHeader, result) should be(Some(RestRouteActionType.Read.toString()))
        header(TestFilter.TestPathHeader, result) should be(Some("test"))
        contentAsString(result) should be("read")
      }
    }
    it("should return Ok(write) for an expected resource id put") {
      withFilteredApp(injector.instanceOf[TestControllerAll]) { app =>
        val Some(result) = route(app, FakeRequest("PUT", "/26"))
        status(result) should be(OK)
        header(TestFilter.TestHeader, result) should be(Some(RestRouteActionType.Write.toString()))
        header(TestFilter.TestPathHeader, result) should be(Some("test"))
        contentAsString(result) should be("write")
      }
    }
    it("should return NoContent for an expected resource id delete") {
      withFilteredApp(injector.instanceOf[TestControllerAll]) { app =>
        val Some(result) = route(app, FakeRequest("DELETE", "/26"))
        header(TestFilter.TestHeader, result) should be(Some(RestRouteActionType.Delete.toString()))
        header(TestFilter.TestPathHeader, result) should be(Some("test"))
        status(result) should be(NO_CONTENT)
      }
    }
    it("should return Ok(update) for an expected resource id patch") {
      withFilteredApp(injector.instanceOf[TestControllerAll]) { app =>
        val Some(result) = route(app, FakeRequest("PATCH", "/26"))
        status(result) should be(OK)
        header(TestFilter.TestHeader, result) should be(Some(RestRouteActionType.Update.toString()))
        header(TestFilter.TestPathHeader, result) should be(Some("test"))
        contentAsString(result) should be("update")
      }
    }
    it("should return MethodNotAllowed for an expected resource id post") {
      withFilteredApp(injector.instanceOf[TestControllerAll]) { app =>
        val Some(result) = route(app, FakeRequest("POST", "/26"))
        header(TestFilter.TestHeader, result) should be(None)
        header(TestFilter.TestPathHeader, result) should be(None)
        status(result) should be(METHOD_NOT_ALLOWED)
      }
    }
    it("should return Ok(list) for an expected resource get") {
      withFilteredApp(injector.instanceOf[TestControllerAll]) { app =>
        val Some(result) = route(app, FakeRequest("GET", "/"))
        status(result) should be(OK)
        header(TestFilter.TestHeader, result) should be(Some(RestRouteActionType.List.toString()))
        header(TestFilter.TestPathHeader, result) should be(Some("test"))
        contentAsString(result) should be("list")
      }
    }
    it("should return Created(create) for an expected resource post") {
      withFilteredApp(injector.instanceOf[TestControllerAll]) { app =>
        val Some(result) = route(app, FakeRequest("POST", "/"))
        status(result) should be(CREATED)
        header(TestFilter.TestHeader, result) should be(Some(RestRouteActionType.Create.toString()))
        header(TestFilter.TestPathHeader, result) should be(Some("test"))
        contentAsString(result) should be("create")
      }
    }
    it("should return MethodNotAllowed for an unexpected http method") {
      withFilteredApp(injector.instanceOf[TestControllerAll]) { app =>
        val Some(result) = route(app, FakeRequest("PUT", "/"))
        header(TestFilter.TestHeader, result) should be(None)
        header(TestFilter.TestPathHeader, result) should be(None)
        status(result) should be(METHOD_NOT_ALLOWED)
      }
    }
    it("should return MethodNotAllowed for an unsuported post") {
      withFilteredApp(injector.instanceOf[TestControllerRead]) { app =>
        val Some(result) = route(app, FakeRequest("POST", "/"))
        header(TestFilter.TestHeader, result) should be(Some(RestRouteActionType.Create.toString()))
        header(TestFilter.TestPathHeader, result) should be(Some("test"))
        status(result) should be(METHOD_NOT_ALLOWED)
      }
    }
    it("should return MethodNotAllowed for an unsuported delete on an expected resource id") {
      withFilteredApp(injector.instanceOf[TestControllerRead]) { app =>
        val Some(result) = route(app, FakeRequest("DELETE", "/26"))
        header(TestFilter.TestHeader, result) should be(Some(RestRouteActionType.Delete.toString()))
        header(TestFilter.TestPathHeader, result) should be(Some("test"))
        status(result) should be(METHOD_NOT_ALLOWED)
      }
    }
    it("should return MethodNotAllowed for an unsuported put on an expected resource id") {
      withFilteredApp(injector.instanceOf[TestControllerRead]) { app =>
        val Some(result) = route(app, FakeRequest("PUT", "/26"))
        header(TestFilter.TestHeader, result) should be(Some(RestRouteActionType.Write.toString()))
        header(TestFilter.TestPathHeader, result) should be(Some("test"))
        status(result) should be(METHOD_NOT_ALLOWED)
      }
    }
    it("should return NotFound for an unsuported post on an unexpected resource id") {
      withFilteredApp(injector.instanceOf[TestControllerRead]) { app =>
        val Some(result) = route(app, FakeRequest("POST", "/bla"))
        status(result) should be(NOT_FOUND)
      }
    }

    it("should return Ok('hello world') an expected resource id hello extension") {
      withFilteredApp(extResourceRouter) { app =>
        val Some(result) = route(app, FakeRequest("GET", "/26/hello"))
        status(result) should be(OK)
        header(TestFilter.TestHeader, result) should be(Some(RestRouteActionType.Custom.toString()))
        header(TestFilter.TestPathHeader, result) should be(Some("test/hello"))
        contentAsString(result) should be("hello world")
      }
    }
  }
}
