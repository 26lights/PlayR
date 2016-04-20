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

class FilteredRouterTest extends FunSpec with Matchers with PlayRApp{
  class SimpleHttpRequestHandler (router: Router) extends HttpRequestHandler {
    def handlerForRequest(request: RequestHeader) = {
      router.routes.lift(request) match {
        case Some(handler) => (request, handler)
        case None => (request, Action(Results.NotFound))
      }
    }
  }

  def extResourceRouter = {
    val extController = new ExtendedFilteredTestController
    new RestResourceRouter[ExtendedFilteredTestController](extController)
      .add("hello", GET, extController.hello _)
  }

  describe("A RestResourceRouter with filtered controller") {
    it("should return NotFound for an unexpected resource id get"){ withApp(new TestControllerFilteredAll()) { app =>
      val Some(result) = route(app, FakeRequest("GET", "/test"))
      status(result) should be(NOT_FOUND)
    }}
    it("should return Ok(read) for an expected resource id get"){ withApp(new TestControllerFilteredAll()) { app =>
      val Some(result) = route(app, FakeRequest("GET", "/26"))
      status(result) should be(OK)
      header(TestFilter.TestHeader, result) should be(Some(RestRouteActionType.Read.toString()))
      contentAsString(result) should be("read")
    }}
    it("should return Ok(write) for an expected resource id put"){ withApp(new TestControllerFilteredAll()) { app =>
      val Some(result) = route(app, FakeRequest("PUT", "/26"))
      status(result) should be(OK)
      header(TestFilter.TestHeader, result) should be(Some(RestRouteActionType.Write.toString()))
      contentAsString(result) should be("write")
    }}
    it("should return NoContent for an expected resource id delete"){ withApp(new TestControllerFilteredAll()) { app =>
      val Some(result) = route(app, FakeRequest("DELETE", "/26"))
      header(TestFilter.TestHeader, result) should be(Some(RestRouteActionType.Delete.toString()))
      status(result) should be(NO_CONTENT)
    }}
    it("should return Ok(update) for an expected resource id patch"){ withApp(new TestControllerFilteredAll()) { app =>
      val Some(result) = route(app, FakeRequest("PATCH", "/26"))
      status(result) should be(OK)
      header(TestFilter.TestHeader, result) should be(Some(RestRouteActionType.Update.toString()))
      contentAsString(result) should be("update")
    }}
    it("should return MethodNotAllowed for an expected resource id post"){ withApp(new TestControllerFilteredAll()) { app =>
      val Some(result) = route(app, FakeRequest("POST", "/26"))
      header(TestFilter.TestHeader, result) should be(None)
      status(result) should be(METHOD_NOT_ALLOWED)
    }}
    it("should return Ok(list) for an expected resource get"){ withApp(new TestControllerFilteredAll()) { app =>
      val Some(result) = route(app, FakeRequest("GET", "/"))
      status(result) should be(OK)
      header(TestFilter.TestHeader, result) should be(Some(RestRouteActionType.List.toString()))
      contentAsString(result) should be("list")
    }}
    it("should return Created(create) for an expected resource post"){ withApp(new TestControllerFilteredAll()) { app =>
      val Some(result) = route(app, FakeRequest("POST", "/"))
      status(result) should be(CREATED)
      header(TestFilter.TestHeader, result) should be(Some(RestRouteActionType.Create.toString()))
      contentAsString(result) should be("create")
    }}
    it("should return MethodNotAllowed for an unexpected http method"){ withApp(new TestControllerFilteredAll()) { app =>
      val Some(result) = route(app, FakeRequest("PUT", "/"))
      header(TestFilter.TestHeader, result) should be(None)
      status(result) should be(METHOD_NOT_ALLOWED)
    }}
    it("should return MethodNotAllowed for an unsuported post"){ withApp(new TestControllerFilteredRead()) { app =>
      val Some(result) = route(app, FakeRequest("POST", "/"))
      header(TestFilter.TestHeader, result) should be(Some(RestRouteActionType.Create.toString()))
      status(result) should be(METHOD_NOT_ALLOWED)
    }}
    it("should return MethodNotAllowed for an unsuported delete on an expected resource id"){ withApp(new TestControllerFilteredRead()) { app =>
      val Some(result) = route(app, FakeRequest("DELETE", "/26"))
      header(TestFilter.TestHeader, result) should be(Some(RestRouteActionType.Delete.toString()))
      status(result) should be(METHOD_NOT_ALLOWED)
    }}
    it("should return MethodNotAllowed for an unsuported put on an expected resource id"){ withApp(new TestControllerFilteredRead()) { app =>
      val Some(result) = route(app, FakeRequest("PUT", "/26"))
      header(TestFilter.TestHeader, result) should be(Some(RestRouteActionType.Write.toString()))
      status(result) should be(METHOD_NOT_ALLOWED)
    }}
    it("should return NotFound for an unsuported post on an unexpected resource id"){ withApp(new TestControllerFilteredRead()) { app =>
      val Some(result) = route(app, FakeRequest("POST", "/bla"))
      status(result) should be(NOT_FOUND)
    }}

    it("should return Ok('hello world') an expected resource id hello extension"){ withApp(extResourceRouter) { app =>
      val Some(result) = route(app, FakeRequest("GET", "/26/hello"))
      status(result) should be(OK)
      header(TestFilter.TestHeader, result) should be(Some(RestRouteActionType.Custom.toString()))
      contentAsString(result) should be("hello world")
    }}
  }
}

