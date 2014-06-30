package twentysix.playr

import play.api.mvc._
import twentysix.playr.simple._

class TestController extends Resource[Boolean] {
  def fromId(id: String): Option[Boolean] = if(id=="26") Some(true) else None
  def name: String = "test"
  def list: EssentialAction = Action { Ok("list") }
  def read(id: Boolean): EssentialAction = Action { Ok("read") }
  def write(id: Boolean): EssentialAction = Action { Ok("write") }
  def update(id: Boolean): EssentialAction = Action { Ok("update") }
  def delete(id: Boolean): EssentialAction = Action { NoContent }
  def create: EssentialAction = Action { Created("create") }


}

class TestControllerRead extends TestController with ResourceRead
class TestControllerWrite extends TestController with ResourceWrite
class TestControllerUpdate extends TestController with ResourceUpdate
class TestControllerDelete extends TestController with ResourceDelete
class TestControllerCreate extends TestController with ResourceCreate
class TestControllerAll extends TestController with RestCrudController[Boolean] with ResourceUpdate


class TestFilteredController extends TestController with ResourceRouteFilter {
  def routeFilter = TestFilter()
}
class TestControllerFilteredRead extends TestFilteredController with ResourceRead
class TestControllerFilteredWrite extends TestFilteredController with ResourceWrite
class TestControllerFilteredUpdate extends TestFilteredController with ResourceUpdate
class TestControllerFilteredDelete extends TestFilteredController with ResourceDelete
class TestControllerFilteredCreate extends TestFilteredController with ResourceCreate
class TestControllerFilteredAll extends TestFilteredController with RestCrudController[Boolean] with ResourceUpdate

class ExtendedTestController extends TestController {
  def hello(id: Boolean) = Action { Ok("hello world") }
}

class ExtendedFilteredTestController extends ExtendedTestController with ResourceRouteFilter {
  def routeFilter = TestFilter()

}
