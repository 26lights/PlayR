package twentysix.playr

class TestController extends Resource[Boolean] {
  def fromId(id: String): Option[Boolean] = Some(id=="26")
  def name: String = "test"
  def list: play.api.mvc.EssentialAction = ???
  def read(id: Boolean): play.api.mvc.EssentialAction = ???
  def write(id: Boolean): play.api.mvc.EssentialAction = ???
  def update(id: Boolean): play.api.mvc.EssentialAction = ???
  def delete(id: Boolean): play.api.mvc.EssentialAction = ???
  def create: play.api.mvc.EssentialAction = ???
}

class TestControllerRead extends TestController with BaseResourceRead
class TestControllerWrite extends TestController with BaseResourceWrite
class TestControllerUpdate extends TestController with BaseResourceUpdate
class TestControllerDelete extends TestController with BaseResourceDelete
class TestControllerCreate extends TestController with BaseResourceCreate
class TestControllerAll extends TestController with RestCrudController[Boolean] with BaseResourceUpdate
