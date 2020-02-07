package twentysix.playr

import twentysix.playr.core.BaseResource
import play.api.Application
import play.api.mvc.ControllerComponents
import play.api.test.Helpers
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.inject.guice.GuiceInjectorBuilder

trait PlayRApp {
  val injector =
    new GuiceInjectorBuilder()
      .bindings(bind[ControllerComponents].toInstance(Helpers.stubControllerComponents()))
      .injector()

  def withApp[T](router: RestResourceRouter[_])(block: (Application) => T): T = {
    block(
      new GuiceApplicationBuilder().router(router).build()
    )
  }

  def withApp[C <: BaseResource: ResourceWrapper, T](controller: C)(block: (Application) => T): T =
    withApp(new RestResourceRouter[C](controller))(block)

  def withFilteredApp[T](
      router: RestResourceRouter[_]
  )(block: (Application) => T)(implicit filter: RestRouterFilter): T =
    withApp(router.withFilter(filter))(block)

  def withFilteredApp[C <: BaseResource: ResourceWrapper, T](
      controller: C
  )(block: (Application) => T)(implicit filter: RestRouterFilter): T =
    withFilteredApp(new RestResourceRouter[C](controller))(block)
}
