package twentysix.playr

import org.scalatest.{Matchers, FunSpec}

class ResourceWrapperTest extends FunSpec with Matchers {

  def getCaps(wrapper: ResourceWrapper[_]) = {
    wrapper.readWrapper.caps ++
    wrapper.writeWrapper.caps ++
    wrapper.updateWrapper.caps ++
    wrapper.deleteWrapper.caps ++
    wrapper.createWrapper.caps
  }

  describe("A ResourceWrapper") {
    describe("when created for a BaseResourceRead controller") {
      val wrapper = ResourceWrapper.resourceWrapperImpl[TestControllerRead]
      it("should have Read in it's caps") {
        getCaps(wrapper) should be(ResourceCaps.ValueSet(ResourceCaps.Read))
      }
      it("should not have Write in it's caps") {
        getCaps(wrapper) should not be(ResourceCaps.ValueSet(ResourceCaps.Write))
      }
    }

    describe("when created for a BaseResourceWrite controller") {
      val wrapper = ResourceWrapper.resourceWrapperImpl[TestControllerWrite]
      it("should have Write in it's caps") {
        getCaps(wrapper) should be(ResourceCaps.ValueSet(ResourceCaps.Write))
      }
      it("should not have Read in it's caps") {
        getCaps(wrapper) should not be(ResourceCaps.ValueSet(ResourceCaps.Read))
      }
    }

    describe("when created for a BaseResourceCreate controller") {
      val wrapper = ResourceWrapper.resourceWrapperImpl[TestControllerCreate]
      it("should have Create in it's caps") {
        getCaps(wrapper) should be(ResourceCaps.ValueSet(ResourceCaps.Create))
      }
      it("should not have Read in it's caps") {
        getCaps(wrapper) should not be(ResourceCaps.ValueSet(ResourceCaps.Read))
      }
    }

    describe("when created for a BaseResourceUpdate controller") {
      val wrapper = ResourceWrapper.resourceWrapperImpl[TestControllerUpdate]
      it("should have Update in it's caps") {
        getCaps(wrapper) should be(ResourceCaps.ValueSet(ResourceCaps.Update))
      }
      it("should not have Read in it's caps") {
        getCaps(wrapper) should not be(ResourceCaps.ValueSet(ResourceCaps.Read))
      }
    }

    describe("when created for a BaseResourceDelete controller") {
      val wrapper = ResourceWrapper.resourceWrapperImpl[TestControllerDelete]
      it("should have Delete in it's caps") {
        getCaps(wrapper) should be(ResourceCaps.ValueSet(ResourceCaps.Delete))
      }
      it("should not have Read in it's caps") {
        getCaps(wrapper) should not be(ResourceCaps.ValueSet(ResourceCaps.Read))
      }
    }

    describe("when created for a Resource controller") {
      val wrapper = ResourceWrapper.resourceWrapperImpl[TestController]
      it("should not have any caps") {
        getCaps(wrapper) should be(ResourceCaps.ValueSet.empty)
      }
    }

    describe("when created for a controller with all resource traits") {
      val wrapper = ResourceWrapper.resourceWrapperImpl[TestControllerAll]
      it("should have Read in it's caps") {
        getCaps(wrapper) should contain(ResourceCaps.Read)
      }
      it("should have Write in it's caps") {
        getCaps(wrapper) should contain(ResourceCaps.Write)
      }
      it("should have Create in it's caps") {
        getCaps(wrapper) should contain(ResourceCaps.Create)
      }
      it("should have Update in it's caps") {
        getCaps(wrapper) should contain(ResourceCaps.Update)
      }
      it("should have Delete in it's caps") {
        getCaps(wrapper) should contain(ResourceCaps.Delete)
      }
    }

  }

  class TestController extends Resource[Long] {
    def fromId(id: String): Option[Long] = ???
    def name: String = ???
    def list: play.api.mvc.EssentialAction = ???
    def read(id: Long): play.api.mvc.EssentialAction = ???
    def write(id: Long): play.api.mvc.EssentialAction = ???
    def update(id: Long): play.api.mvc.EssentialAction = ???
    def delete(id: Long): play.api.mvc.EssentialAction = ???
    def create: play.api.mvc.EssentialAction = ???
  }

  class TestControllerRead extends TestController with BaseResourceRead
  class TestControllerWrite extends TestController with BaseResourceWrite
  class TestControllerUpdate extends TestController with BaseResourceUpdate
  class TestControllerDelete extends TestController with BaseResourceDelete
  class TestControllerCreate extends TestController with BaseResourceCreate
  class TestControllerAll extends TestController with RestCrudController[Long] with BaseResourceUpdate
}

