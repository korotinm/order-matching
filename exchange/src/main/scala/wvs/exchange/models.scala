package wvs.exchange

import enumeratum._

sealed trait Securities extends EnumEntry

object Securities extends Enum[Securities] {
  val values = findValues

  case object A extends Securities
  case object B extends Securities
  case object C extends Securities
  case object D extends Securities
}

sealed trait Direction extends EnumEntry

object Direction extends Enum[Direction] {

  val values = findValues

  case object b extends Direction
  case object s extends Direction
}

case class ClientId(v: String) extends AnyVal {

  def suffix: Int = v.substring(1).toInt

  override def toString(): String = v
}

case class ClientModel(id: ClientId, 
                       amount: Int, 
                       aCount: Int, 
                       bCount: Int, 
                       cCount: Int, 
                       dCount: Int) {
  override def toString: String =
    s"${id.v}\t$amount\t$aCount\t$bCount\t$cCount\t$dCount"
}

case class OrderModel(clientId: ClientId, 
                      direction: Direction, 
                      src: Securities,
                      price: Int,
                      count: Int)
