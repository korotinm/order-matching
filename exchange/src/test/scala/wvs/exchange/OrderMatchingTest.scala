package wvs.exchange

import java.io.File
import java.net.URL

import com.google.inject._
import com.google.inject.name.Names
import net.codingwell.scalaguice.ScalaModule
import org.scalatest.concurrent.{PatienceConfiguration, ScalaFutures}
import org.scalatest.time.{Millis, Span}
import org.scalatest.{BeforeAndAfterEach, Matchers, WordSpec}
import wvs.exchange.services.LoaderService

import scala.concurrent.duration._
import scala.io.Source

class OrderMatchingTest extends WordSpec with Matchers with BeforeAndAfterEach with ScalaFutures{

  import wvs.util.TestHelper.implicits._

  val atMost = PatienceConfiguration.Timeout(30 seconds)
  val interval = PatienceConfiguration.Interval(Span(200, Millis))
  implicit override val patienceConfig: PatienceConfig = PatienceConfig(atMost.value, interval.value)

  var injector: Injector = _
  var testModule: Module = new Test1Module
  var directory: String = _
  var service: LoaderService = _

  override def beforeEach() {
    super.beforeEach()
    injector = Guice.createInjector(testModule)
    service = injector.getInstance(classOf[LoaderService])
    directory = injector.getInstance(Key.get(classOf[String], Names.named("directory")))
  }


  "OrderMatchingTest" should {

    "main test" in {
      /*service.start().futureValue

      val url = resultUrl()
      Source.fromURL(url).getLines.nonEmpty shouldBe true
      "/result/test1.txt".content shouldBe Source.fromURL(url).mkString*/

      testModule = new Test2Module()

      succeed
    }

    "simple test" in {
      service.start().futureValue

      val url = resultUrl()
      Source.fromURL(url).getLines.nonEmpty shouldBe true
      "/result/test2.txt".content shouldBe Source.fromURL(url).mkString

      testModule = new Test3Module()

      succeed
    }

    "no opponent in order (only C1)" in {
      service.start().futureValue

      val url = resultUrl()
      Source.fromURL(url).getLines.nonEmpty shouldBe true
      "/result/test3.txt".content shouldBe Source.fromURL(url).mkString

      testModule = new Test4Module()

      succeed
    }

    "without orders" in {
      service.start().futureValue

      val url = resultUrl()
      Source.fromURL(url).getLines.nonEmpty shouldBe true
      "/result/test4.txt".content shouldBe Source.fromURL(url).mkString

      testModule = new Test5Module()

      succeed
    }

    "without orders and clients" in {
      service.start().futureValue

      val url = resultUrl()
      Source.fromURL(url).getLines.isEmpty shouldBe true
      "/result/test5.txt".content shouldBe Source.fromURL(url).mkString

      testModule = new Test6Module()

      succeed
    }

    "counts have zero value" in {
      service.start().futureValue

      val url = resultUrl()
      Source.fromURL(url).getLines.nonEmpty shouldBe true
      "/result/test6.txt".content shouldBe Source.fromURL(url).mkString

    }
  }

  private def resultUrl(): URL =
    new URL(s"${getClass.getResource(directory).toString}${File.separator}result.txt")


  class Test1Module() extends AbstractModule with ScalaModule {
    override def configure(): Unit = {
      bind[LoaderService].asEagerSingleton()
      bind[String]
        .annotatedWith(Names.named("directory"))
        .toInstance(s"${File.separator}test1")
    }
  }

  class Test2Module() extends AbstractModule with ScalaModule {
    override def configure(): Unit = {
      bind[LoaderService].asEagerSingleton()
      bind[String]
        .annotatedWith(Names.named("directory"))
        .toInstance(s"${File.separator}test2")
    }
  }

  class Test3Module() extends AbstractModule with ScalaModule {
    override def configure(): Unit = {
      bind[LoaderService].asEagerSingleton()
      bind[String]
        .annotatedWith(Names.named("directory"))
        .toInstance(s"${File.separator}test3")
    }
  }

  class Test4Module() extends AbstractModule with ScalaModule {
    override def configure(): Unit = {
      bind[LoaderService].asEagerSingleton()
      bind[String]
        .annotatedWith(Names.named("directory"))
        .toInstance(s"${File.separator}test4")
    }
  }

  class Test5Module() extends AbstractModule with ScalaModule {
    override def configure(): Unit = {
      bind[LoaderService].asEagerSingleton()
      bind[String]
        .annotatedWith(Names.named("directory"))
        .toInstance(s"${File.separator}test5")
    }
  }

  class Test6Module() extends AbstractModule with ScalaModule {
    override def configure(): Unit = {
      bind[LoaderService].asEagerSingleton()
      bind[String]
        .annotatedWith(Names.named("directory"))
        .toInstance(s"${File.separator}test6")
    }
  }

}
