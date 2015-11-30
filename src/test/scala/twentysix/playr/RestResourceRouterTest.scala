package twentysix.playr

import org.scalatest.{FunSpec, Matchers}
import play.api.http.HttpRequestHandler
import play.api.mvc.{Results, Action, RequestHeader}
import play.api.routing.Router
import play.api.test.FakeApplication
import play.api.test.FakeRequest
import play.api.test.Helpers._
import twentysix.playr.core.BaseResource

class RestResourceRouterTest extends FunSpec with Matchers{
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
    val extController = new ExtendedTestController
    val router = new RestResourceRouter[ExtendedTestController](extController)
      .add("hello", GET, extController.hello _)
      .add("multi") {
        case GET => extController.multiGet _
        case PUT => extController.multiPut _
      }

    override def requestHandler = new SimpleHttpRequestHandler(router)
  }

  describe("A RestResourceRouter") {
    it("should return NotFound for an unexpected resource id get"){ runningInApp(new TestControllerAll()) {
      val Some(result) = route(FakeRequest("GET", "/test"))
      status(result) should be(NOT_FOUND)
    }}
    it("should return Ok(read) for an expected resource id get"){ runningInApp(new TestControllerAll()) {
      val Some(result) = route(FakeRequest("GET", "/26"))
      status(result) should be(OK)
      header(TestFilter.TestHeader, result) should be(None)
      contentAsString(result) should be("read")
    }}
    it("should return Ok(write) for an expected resource id put"){ runningInApp(new TestControllerAll()) {
      val Some(result) = route(FakeRequest("PUT", "/26"))
      status(result) should be(OK)
      header(TestFilter.TestHeader, result) should be(None)
      contentAsString(result) should be("write")
    }}
    it("should return NoContent for an expected resource id delete"){ runningInApp(new TestControllerAll()) {
      val Some(result) = route(FakeRequest("DELETE", "/26"))
      status(result) should be(NO_CONTENT)
      header(TestFilter.TestHeader, result) should be(None)
    }}
    it("should return Ok(update) for an expected resource id patch"){ runningInApp(new TestControllerAll()) {
      val Some(result) = route(FakeRequest("PATCH", "/26"))
      status(result) should be(OK)
      contentAsString(result) should be("update")
      header(TestFilter.TestHeader, result) should be(None)
    }}
    it("should return MethodNotAllowed for an expected resource id post"){ runningInApp(new TestControllerAll()) {
      val Some(result) = route(FakeRequest("POST", "/26"))
      status(result) should be(METHOD_NOT_ALLOWED)
      header(TestFilter.TestHeader, result) should be(None)
    }}
    it("should return Ok(list) for an expected resource get"){ runningInApp(new TestControllerAll()) {
      val Some(result) = route(FakeRequest("GET", "/"))
      status(result) should be(OK)
      contentAsString(result) should be("list")
      header(TestFilter.TestHeader, result) should be(None)
    }}
    it("should return Created(create) for an expected resource post"){ runningInApp(new TestControllerAll()) {
      val Some(result) = route(FakeRequest("POST", "/"))
      status(result) should be(CREATED)
      contentAsString(result) should be("create")
      header(TestFilter.TestHeader, result) should be(None)
    }}
    it("should return MethodNotAllowed for an unexpected http method"){ runningInApp(new TestControllerAll()) {
      val Some(result) = route(FakeRequest("PUT", "/"))
      status(result) should be(METHOD_NOT_ALLOWED)
      header(TestFilter.TestHeader, result) should be(None)
    }}
    it("should return MethodNotAllowed for an unsuported post"){ runningInApp(new TestControllerRead()) {
      val Some(result) = route(FakeRequest("POST", "/"))
      status(result) should be(METHOD_NOT_ALLOWED)
      header(TestFilter.TestHeader, result) should be(None)
    }}
    it("should return MethodNotAllowed for an unsuported delete on an expected resource id"){ runningInApp(new TestControllerRead()) {
      val Some(result) = route(FakeRequest("DELETE", "/26"))
      status(result) should be(METHOD_NOT_ALLOWED)
      header(TestFilter.TestHeader, result) should be(None)
    }}
    it("should return MethodNotAllowed for an unsuported put on an expected resource id"){ runningInApp(new TestControllerRead()) {
      val Some(result) = route(FakeRequest("PUT", "/26"))
      status(result) should be(METHOD_NOT_ALLOWED)
      header(TestFilter.TestHeader, result) should be(None)
    }}
    it("should return NotFound for an unsuported post on an unexpected resource id"){ runningInApp(new TestControllerRead()) {
      val Some(result) = route(FakeRequest("POST", "/bla"))
      status(result) should be(NOT_FOUND)
    }}

    it("should return Ok('hello world') an expected resource id hello extension"){ running(new ExtendedControllerApp) {
      val Some(result) = route(FakeRequest("GET", "/26/hello"))
      status(result) should be(OK)
      contentAsString(result) should be("hello world")
      header(TestFilter.TestHeader, result) should be(None)
    }}

    it("should return Ok('multi get') for an expected resource id multi extension"){ running(new ExtendedControllerApp) {
      val Some(result) = route(FakeRequest("GET", "/26/multi"))
      status(result) should be(OK)
      contentAsString(result) should be("multi get")
      header(TestFilter.TestHeader, result) should be(None)
    }}

    it("should return Ok('multi put') for an expected resource id multi extension"){ running(new ExtendedControllerApp) {
      val Some(result) = route(FakeRequest("PUT", "/26/multi"))
      status(result) should be(OK)
      contentAsString(result) should be("multi put")
      header(TestFilter.TestHeader, result) should be(None)
    }}

    it("should return MethodNotAllowed for an unsupported delete on a resource id multi extension"){ running(new ExtendedControllerApp) {
      val Some(result) = route(FakeRequest("DELETE", "/26/multi"))
      status(result) should be(METHOD_NOT_ALLOWED)
      header(TestFilter.TestHeader, result) should be(None)
    }}

    it("should return GET, PUT for an OPTIONS request on a resource id multi extension"){ running(new ExtendedControllerApp) {
      val Some(result) = route(FakeRequest("OPTIONS", "/26/multi"))
      header(ALLOW, result) should not be(None)
      header(ALLOW, result).get should include("GET")
      header(ALLOW, result).get should include("PUT")
      header(ALLOW, result).get should not include("DELETE")
      header(TestFilter.TestHeader, result) should be(None)
    }}
  }
}

