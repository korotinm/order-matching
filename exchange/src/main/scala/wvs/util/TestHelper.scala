package wvs.util

import java.io.File

import scala.io.Source

object TestHelper {

  object implicits {

    implicit class Resource(src: String) {
      val source = if(src.startsWith("/")) src else s"${File.separator}$src"

      def content =  Source.fromURL(getClass.getResource(source)).mkString
    }
  }
}
