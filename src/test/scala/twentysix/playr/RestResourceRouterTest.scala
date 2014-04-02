package twentysix.playr

import org.scalatest.{FunSpec, Matchers}
import play.api.test.FakeApplication
import play.api.test.WithApplication
import play.api.test.FakeRequest
import play.api.test.Helpers._

class RestResourceRouterTest extends FunSpec with Matchers{
  class FakeApp[C<:core.BaseResource: ResourceWrapper](controller: C) extends FakeApplication {
    override lazy val routes = Some(new RestResourceRouter[C](controller))
  }

  abstract class InApp[C<:core.BaseResource: ResourceWrapper](controller: C) extends
    WithApplication(new FakeApp(controller))

  describe("A RestResourceRouter") {
    it("should return None for an unexpected resource id get"){ new InApp(new TestControllerAll()) {
      val result = route(FakeRequest("GET", "/test"))
      result should be(None)
    }}
    it("should return Ok(read) for an expected resource id get"){ new InApp(new TestControllerAll()) {
      val Some(result) = route(FakeRequest("GET", "/26"))
      status(result) should be(OK)
      contentAsString(result) should be("read")
    }}
    it("should return Ok(write) for an expected resource id put"){ new InApp(new TestControllerAll()) {
      val Some(result) = route(FakeRequest("PUT", "/26"))
      status(result) should be(OK)
      contentAsString(result) should be("write")
    }}
    it("should return NoContent for an expected resource id delete"){ new InApp(new TestControllerAll()) {
      val Some(result) = route(FakeRequest("DELETE", "/26"))
      status(result) should be(NO_CONTENT)
    }}
    it("should return Ok(update) for an expected resource id patch"){ new InApp(new TestControllerAll()) {
      val Some(result) = route(FakeRequest("PATCH", "/26"))
      status(result) should be(OK)
      contentAsString(result) should be("update")
    }}
    it("should return MethodNotAllowed for an expected resource id post"){ new InApp(new TestControllerAll()) {
      val Some(result) = route(FakeRequest("POST", "/26"))
      status(result) should be(METHOD_NOT_ALLOWED)
    }}
    it("should return Ok(list) for an expected resource get"){ new InApp(new TestControllerAll()) {
      val Some(result) = route(FakeRequest("GET", "/"))
      status(result) should be(OK)
      contentAsString(result) should be("list")
    }}
    it("should return Created(create) for an expected resource post"){ new InApp(new TestControllerAll()) {
      val Some(result) = route(FakeRequest("POST", "/"))
      status(result) should be(CREATED)
      contentAsString(result) should be("create")
    }}
    it("should return MethodNotAllowed for an unexpected http method"){ new InApp(new TestControllerAll()) {
      val Some(result) = route(FakeRequest("PUT", "/"))
      status(result) should be(METHOD_NOT_ALLOWED)
    }}
    it("should return MethodNotAllowed for an unsuported post"){ new InApp(new TestControllerRead()) {
      val Some(result) = route(FakeRequest("POST", "/"))
      status(result) should be(METHOD_NOT_ALLOWED)
    }}
    it("should return MethodNotAllowed for an unsuported delete on an expected resource id"){ new InApp(new TestControllerRead()) {
      val Some(result) = route(FakeRequest("DELETE", "/26"))
      status(result) should be(METHOD_NOT_ALLOWED)
    }}
    it("should return MethodNotAllowed for an unsuported put on an expected resource id"){ new InApp(new TestControllerRead()) {
      val Some(result) = route(FakeRequest("PUT", "/26"))
      status(result) should be(METHOD_NOT_ALLOWED)
    }}
    it("should return None for an unsuported post on an unexpected resource id"){ new InApp(new TestControllerRead()) {
      val result = route(FakeRequest("POST", "/bla"))
      result should be(None)
    }}
  }
}

