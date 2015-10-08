package twentysix.playr

import org.scalatest.{FunSpec, Matchers}
import play.api.http.HttpRequestHandler
import play.api.mvc.{Results, Action, RequestHeader}
import play.api.routing.Router
import play.api.test.FakeApplication
import play.api.test.FakeRequest
import play.api.test.Helpers._
import twentysix.playr.core.BaseResource

class FilteredRouterTest extends FunSpec with Matchers{
  class SimpleHttpRequestHandler (router: Router) extends HttpRequestHandler {
    def handlerForRequest(request: RequestHeader) = {
      router.routes.lift(request) match {
        case Some(handler) => (request, handler)
        case None => (request, Action(Results.NotFound))
      }
    }
  }

  class FakeApp[C<:BaseResource: ResourceWrapper](controller: C) extends FakeApplication {
    override def requestHandler = new SimpleHttpRequestHandler(new RestResourceRouter[C](controller))
  }

  def runningInApp[C<:BaseResource: ResourceWrapper, T](controller: C)(block: => T): T = {
    running(new FakeApp(controller))(block)
  }

  class ExtendedControllerApp extends FakeApplication {
    val extController = new ExtendedFilteredTestController
    val router = new RestResourceRouter[ExtendedFilteredTestController](extController)
      .add("hello", GET, extController.hello _)
    override def requestHandler = new SimpleHttpRequestHandler(router)
  }

  describe("A RestResourceRouter with filtered controller") {
    it("should return NotFound for an unexpected resource id get"){ runningInApp(new TestControllerFilteredAll()) {
      val Some(result) = route(FakeRequest("GET", "/test"))
      status(result) should be(NOT_FOUND)
    }}
    it("should return Ok(read) for an expected resource id get"){ runningInApp(new TestControllerFilteredAll()) {
      val Some(result) = route(FakeRequest("GET", "/26"))
      status(result) should be(OK)
      header(TestFilter.TestHeader, result) should be(Some(RestRouteActionType.Read.toString()))
      contentAsString(result) should be("read")
    }}
    it("should return Ok(write) for an expected resource id put"){ runningInApp(new TestControllerFilteredAll()) {
      val Some(result) = route(FakeRequest("PUT", "/26"))
      status(result) should be(OK)
      header(TestFilter.TestHeader, result) should be(Some(RestRouteActionType.Write.toString()))
      contentAsString(result) should be("write")
    }}
    it("should return NoContent for an expected resource id delete"){ runningInApp(new TestControllerFilteredAll()) {
      val Some(result) = route(FakeRequest("DELETE", "/26"))
      header(TestFilter.TestHeader, result) should be(Some(RestRouteActionType.Delete.toString()))
      status(result) should be(NO_CONTENT)
    }}
    it("should return Ok(update) for an expected resource id patch"){ runningInApp(new TestControllerFilteredAll()) {
      val Some(result) = route(FakeRequest("PATCH", "/26"))
      status(result) should be(OK)
      header(TestFilter.TestHeader, result) should be(Some(RestRouteActionType.Update.toString()))
      contentAsString(result) should be("update")
    }}
    it("should return MethodNotAllowed for an expected resource id post"){ runningInApp(new TestControllerFilteredAll()) {
      val Some(result) = route(FakeRequest("POST", "/26"))
      header(TestFilter.TestHeader, result) should be(None)
      status(result) should be(METHOD_NOT_ALLOWED)
    }}
    it("should return Ok(list) for an expected resource get"){ runningInApp(new TestControllerFilteredAll()) {
      val Some(result) = route(FakeRequest("GET", "/"))
      status(result) should be(OK)
      header(TestFilter.TestHeader, result) should be(Some(RestRouteActionType.List.toString()))
      contentAsString(result) should be("list")
    }}
    it("should return Created(create) for an expected resource post"){ runningInApp(new TestControllerFilteredAll()) {
      val Some(result) = route(FakeRequest("POST", "/"))
      status(result) should be(CREATED)
      header(TestFilter.TestHeader, result) should be(Some(RestRouteActionType.Create.toString()))
      contentAsString(result) should be("create")
    }}
    it("should return MethodNotAllowed for an unexpected http method"){ runningInApp(new TestControllerFilteredAll()) {
      val Some(result) = route(FakeRequest("PUT", "/"))
      header(TestFilter.TestHeader, result) should be(None)
      status(result) should be(METHOD_NOT_ALLOWED)
    }}
    it("should return MethodNotAllowed for an unsuported post"){ runningInApp(new TestControllerFilteredRead()) {
      val Some(result) = route(FakeRequest("POST", "/"))
      header(TestFilter.TestHeader, result) should be(Some(RestRouteActionType.Create.toString()))
      status(result) should be(METHOD_NOT_ALLOWED)
    }}
    it("should return MethodNotAllowed for an unsuported delete on an expected resource id"){ runningInApp(new TestControllerFilteredRead()) {
      val Some(result) = route(FakeRequest("DELETE", "/26"))
      header(TestFilter.TestHeader, result) should be(Some(RestRouteActionType.Delete.toString()))
      status(result) should be(METHOD_NOT_ALLOWED)
    }}
    it("should return MethodNotAllowed for an unsuported put on an expected resource id"){ runningInApp(new TestControllerFilteredRead()) {
      val Some(result) = route(FakeRequest("PUT", "/26"))
      header(TestFilter.TestHeader, result) should be(Some(RestRouteActionType.Write.toString()))
      status(result) should be(METHOD_NOT_ALLOWED)
    }}
    it("should return NotFound for an unsuported post on an unexpected resource id"){ runningInApp(new TestControllerFilteredRead()) {
      val Some(result) = route(FakeRequest("POST", "/bla"))
      status(result) should be(NOT_FOUND)
    }}

    it("should return Ok('hello world') an expected resource id hello extension"){ running(new ExtendedControllerApp) {
      val Some(result) = route(FakeRequest("GET", "/26/hello"))
      status(result) should be(OK)
      header(TestFilter.TestHeader, result) should be(Some(RestRouteActionType.Custom.toString()))
      contentAsString(result) should be("hello world")
    }}
  }
}

