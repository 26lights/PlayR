package twentysix.playr

import twentysix.playr.core.BaseResource
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder

trait PlayRApp {
  def withApp[T](router: RestResourceRouter[_])(block: (Application) => T): T = {
    block(
      new GuiceApplicationBuilder().router(router).build()
    )
  }

  def withApp[C<:BaseResource: ResourceWrapper, T](controller: C)(block: (Application) => T): T =
    withApp(new RestResourceRouter[C](controller))(block)

  def withFilteredApp[T](router: RestResourceRouter[_])(block: (Application) => T)(implicit filter: RestRouterFilter): T =
    withApp(router.withFilter(filter))(block)

  def withFilteredApp[C<:BaseResource: ResourceWrapper, T](controller: C)(block: (Application) => T)(implicit filter: RestRouterFilter): T =
    withFilteredApp(new RestResourceRouter[C](controller))(block)
}
