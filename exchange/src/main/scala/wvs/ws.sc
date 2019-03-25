import java.io.{File, PrintWriter}
import java.net.URI

import scala.io.Source


val sellArr = Array[Int](1,2,3,4)

private def delCurrentSellOrder(sellIndex: Int): Array[Int] =
  sellArr.take(sellIndex) ++ sellArr.takeRight(sellArr.length - sellIndex - 1)

delCurrentSellOrder(0)
delCurrentSellOrder(1)
delCurrentSellOrder(2)
delCurrentSellOrder(3)
delCurrentSellOrder(4)
delCurrentSellOrder(-1)
delCurrentSellOrder(10)


var ind = 0

ind += 1
ind
ind += 1
ind


var list = List(1,2,3,4)

list = (list.head + 10) :: list.tail
list

//new File(new URI(getClass.getResource("/test1").toString + "/result.txt")).createNewFile()
