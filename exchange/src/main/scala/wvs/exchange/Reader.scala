package wvs.exchange

import scala.io.Source
import javax.inject.Singleton

sealed abstract class Reader[A] {
  def read(source: String): A
}

@Singleton
class ResourceReader extends Reader[Iterator[String]] {
  def read(source: String): Iterator[String] = {
    Source.fromURL(getClass.getResource(source)).getLines
  }
}