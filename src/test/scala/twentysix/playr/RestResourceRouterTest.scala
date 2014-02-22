package twentysix.playr

import org.scalatest.{FunSpec, Matchers}
import play.api.test.FakeApplication
import play.api.test.WithApplication
import play.api.test.FakeRequest
import play.api.test.Helpers._

class RestResourceRouterTest extends FunSpec with Matchers{
  def router = new RestResourceRouter(new TestControllerAll())
  def fakeApp = new FakeApplication() { override lazy val routes = Some(router) }

  describe("A RestResourceRouter") {
    it("should return None for an unexpected resource id"){ new WithApplication(fakeApp) {
        val result = route(FakeRequest(GET, "/test"))
        result should be(None)
    }}
    it("should return Ok(read) for an expected resource id"){ new WithApplication(fakeApp) {
        val Some(result) = route(FakeRequest(GET, "/26"))
        status(result) should be(OK)
        contentAsString(result) should be("read")
    }}
    it("should return Ok(list) for an expected resource"){ new WithApplication(fakeApp) {
        val Some(result) = route(FakeRequest(GET, "/"))
        status(result) should be(OK)
        contentAsString(result) should be("list")
    }}
  }
}

