package twentysix.rest

import play.core.Router
import play.api.mvc.EssentialAction
import scala.runtime.AbstractPartialFunction
import play.api.mvc.RequestHeader
import play.api.mvc.Handler
import play.api.mvc.Controller
import play.api.Logger

trait IdentifiedResource[Id] {
  def fromId(id: String): Option[Id]
}

trait ResourceRead[Id] {
  def get(id: Id): EssentialAction
  def list(): EssentialAction
}

trait ResourceOverwrite[Id] {
  def put(id: Id): EssentialAction
}

trait ResourceAction {
  def post(): EssentialAction
}

trait ResourceUpdate[Id] {
  def delete(id: Id): EssentialAction
  def update(id: Id): EssentialAction
}

trait RestController[Id] extends Controller with IdentifiedResource[Id]

trait RestReadController[Id] extends Controller with IdentifiedResource[Id]
                                                  with ResourceRead[Id]

trait RestCrudController[Id] extends Controller with IdentifiedResource[Id]
                                                 with ResourceRead[Id]
                                                 with ResourceOverwrite[Id]
                                                 with ResourceUpdate[Id]
                                                 with ResourceAction
