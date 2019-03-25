package wvs

import com.google.inject.AbstractModule
import com.google.inject.name.Names
import net.codingwell.scalaguice.ScalaModule
import wvs.exchange.services.LoaderService

class MainModule extends AbstractModule with ScalaModule {
  override def configure(): Unit = {
    bind[String].annotatedWith(Names.named("directory")).to[String]
    bind[LoaderService].asEagerSingleton()
  }
}
