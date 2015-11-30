package twentysix.playr

import scala.reflect.runtime.universe._
import play.api.mvc._
import twentysix.playr.core.ResourceWrite
import twentysix.playr.core.ResourceUpdate
import twentysix.playr.core.ResourceRead
import twentysix.playr.core.ResourceDelete
import twentysix.playr.core.ResourceCreate
import twentysix.playr.core.BaseResource
import twentysix.playr.core.ResourceRouteFilter
import twentysix.playr.wrappers._


trait ResourceWrapper[T<:BaseResource]{
  def readWrapper: ReadResourceWrapper[T]
  def writeWrapper: WriteResourceWrapper[T]
  def updateWrapper: UpdateResourceWrapper[T]
  def deleteWrapper: DeleteResourceWrapper[T]
  def listWrapper: ListResourceWrapper[T]
  def createWrapper: CreateResourceWrapper[T]
  def routeFilterWrapper: ResourceRouteFilterWrapper[T]
  def controllerType: Type
}
object ResourceWrapper {
  implicit def resourceWrapperImpl[C<:BaseResource
                                     :TypeTag
                                     :ListResourceWrapper
                                     :ReadResourceWrapper
                                     :WriteResourceWrapper
                                     :UpdateResourceWrapper
                                     :DeleteResourceWrapper
                                     :CreateResourceWrapper
                                     :ResourceRouteFilterWrapper] =
    new ResourceWrapper[C] {
      val readWrapper = implicitly[ReadResourceWrapper[C]]
      val writeWrapper = implicitly[WriteResourceWrapper[C]]
      val updateWrapper = implicitly[UpdateResourceWrapper[C]]
      val deleteWrapper = implicitly[DeleteResourceWrapper[C]]
      val createWrapper = implicitly[CreateResourceWrapper[C]]
      val listWrapper = implicitly[ListResourceWrapper[C]]
      val routeFilterWrapper = implicitly[ResourceRouteFilterWrapper[C]]
      def controllerType = typeOf[C]
  }
}
