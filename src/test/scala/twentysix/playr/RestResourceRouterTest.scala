package twentysix.playr

import org.scalatest.{FunSpec, Matchers}
import play.api.http.HttpRequestHandler
import play.api.mvc.{Results, Action, RequestHeader}
import play.api.routing.Router
import play.api.test.FakeRequest
import play.api.test.Helpers._
import twentysix.playr.core.BaseResource

class RestResourceRouterTest extends FunSpec with Matchers with PlayRApp {
  def extRestRouter = {
    val extController = injector.instanceOf[ExtendedTestController]
    new RestResourceRouter[ExtendedTestController](extController)
      .add("hello", GET, extController.hello _)
      .add("multi") {
        case GET => extController.multiGet _
        case PUT => extController.multiPut _
      }
  }

  describe("A RestResourceRouter") {
    it("should return NotFound for an unexpected resource id get") {
      withApp(injector.instanceOf[TestControllerAll]) { app =>
        val Some(result) = route(app, FakeRequest("GET", "/test"))
        status(result) should be(NOT_FOUND)
      }
    }
    it("should return Ok(read) for an expected resource id get") {
      withApp(injector.instanceOf[TestControllerAll]) { app =>
        val Some(result) = route(app, FakeRequest("GET", "/26"))
        status(result) should be(OK)
        header(TestFilter.TestHeader, result) should be(None)
        contentAsString(result) should be("read")
      }
    }
    it("should return Ok(write) for an expected resource id put") {
      withApp(injector.instanceOf[TestControllerAll]) { app =>
        val Some(result) = route(app, FakeRequest("PUT", "/26"))
        status(result) should be(OK)
        header(TestFilter.TestHeader, result) should be(None)
        contentAsString(result) should be("write")
      }
    }
    it("should return NoContent for an expected resource id delete") {
      withApp(injector.instanceOf[TestControllerAll]) { app =>
        val Some(result) = route(app, FakeRequest("DELETE", "/26"))
        status(result) should be(NO_CONTENT)
        header(TestFilter.TestHeader, result) should be(None)
      }
    }
    it("should return Ok(update) for an expected resource id patch") {
      withApp(injector.instanceOf[TestControllerAll]) { app =>
        val Some(result) = route(app, FakeRequest("PATCH", "/26"))
        status(result) should be(OK)
        contentAsString(result) should be("update")
        header(TestFilter.TestHeader, result) should be(None)
      }
    }
    it("should return MethodNotAllowed for an expected resource id post") {
      withApp(injector.instanceOf[TestControllerAll]) { app =>
        val Some(result) = route(app, FakeRequest("POST", "/26"))
        status(result) should be(METHOD_NOT_ALLOWED)
        header(TestFilter.TestHeader, result) should be(None)
      }
    }
    it("should return Ok(list) for an expected resource get") {
      withApp(injector.instanceOf[TestControllerAll]) { app =>
        val Some(result) = route(app, FakeRequest("GET", "/"))
        status(result) should be(OK)
        contentAsString(result) should be("list")
        header(TestFilter.TestHeader, result) should be(None)
      }
    }
    it("should return Created(create) for an expected resource post") {
      withApp(injector.instanceOf[TestControllerAll]) { app =>
        val Some(result) = route(app, FakeRequest("POST", "/"))
        status(result) should be(CREATED)
        contentAsString(result) should be("create")
        header(TestFilter.TestHeader, result) should be(None)
      }
    }
    it("should return MethodNotAllowed for an unexpected http method") {
      withApp(injector.instanceOf[TestControllerAll]) { app =>
        val Some(result) = route(app, FakeRequest("PUT", "/"))
        status(result) should be(METHOD_NOT_ALLOWED)
        header(TestFilter.TestHeader, result) should be(None)
      }
    }
    it("should return MethodNotAllowed for an unsuported post") {
      withApp(injector.instanceOf[TestControllerRead]) { app =>
        val Some(result) = route(app, FakeRequest("POST", "/"))
        status(result) should be(METHOD_NOT_ALLOWED)
        header(TestFilter.TestHeader, result) should be(None)
      }
    }
    it("should return MethodNotAllowed for an unsuported delete on an expected resource id") {
      withApp(injector.instanceOf[TestControllerRead]) { app =>
        val Some(result) = route(app, FakeRequest("DELETE", "/26"))
        status(result) should be(METHOD_NOT_ALLOWED)
        header(TestFilter.TestHeader, result) should be(None)
      }
    }
    it("should return MethodNotAllowed for an unsuported put on an expected resource id") {
      withApp(injector.instanceOf[TestControllerRead]) { app =>
        val Some(result) = route(app, FakeRequest("PUT", "/26"))
        status(result) should be(METHOD_NOT_ALLOWED)
        header(TestFilter.TestHeader, result) should be(None)
      }
    }
    it("should return NotFound for an unsuported post on an unexpected resource id") {
      withApp(injector.instanceOf[TestControllerRead]) { app =>
        val Some(result) = route(app, FakeRequest("POST", "/bla"))
        status(result) should be(NOT_FOUND)
      }
    }

    it("should return Ok('hello world') an expected resource id hello extension") {
      withApp(extRestRouter) { app =>
        val Some(result) = route(app, FakeRequest("GET", "/26/hello"))
        status(result) should be(OK)
        contentAsString(result) should be("hello world")
        header(TestFilter.TestHeader, result) should be(None)
      }
    }

    it("should return Ok('multi get') for an expected resource id multi extension") {
      withApp(extRestRouter) { app =>
        val Some(result) = route(app, FakeRequest("GET", "/26/multi"))
        status(result) should be(OK)
        contentAsString(result) should be("multi get")
        header(TestFilter.TestHeader, result) should be(None)
      }
    }

    it("should return Ok('multi put') for an expected resource id multi extension") {
      withApp(extRestRouter) { app =>
        val Some(result) = route(app, FakeRequest("PUT", "/26/multi"))
        status(result) should be(OK)
        contentAsString(result) should be("multi put")
        header(TestFilter.TestHeader, result) should be(None)
      }
    }

    it("should return MethodNotAllowed for an unsupported delete on a resource id multi extension") {
      withApp(extRestRouter) { app =>
        val Some(result) = route(app, FakeRequest("DELETE", "/26/multi"))
        status(result) should be(METHOD_NOT_ALLOWED)
        header(TestFilter.TestHeader, result) should be(None)
      }
    }

    it("should return GET, PUT for an OPTIONS request on a resource id multi extension") {
      withApp(extRestRouter) { app =>
        val Some(result) = route(app, FakeRequest("OPTIONS", "/26/multi"))
        header(ALLOW, result) should not be (None)
        header(ALLOW, result).get should include("GET")
        header(ALLOW, result).get should include("PUT")
        header(ALLOW, result).get should not include ("DELETE")
        header(TestFilter.TestHeader, result) should be(None)
      }
    }
  }
}
