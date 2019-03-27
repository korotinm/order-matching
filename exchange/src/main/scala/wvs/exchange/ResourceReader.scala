package wvs.exchange

import scala.io.Source
import javax.inject.Singleton


@Singleton
class ResourceReader {
  def read[B](source: String, transformer: Iterator[String] => B): B =
    transformer(Source.fromURL(getClass.getResource(source)).getLines())

}