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
}
