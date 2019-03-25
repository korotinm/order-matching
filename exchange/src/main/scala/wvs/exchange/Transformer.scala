package wvs.exchange

import javax.inject.Singleton

sealed abstract class Transformer[A, B] {
  def transform(input: A): B
}

@Singleton
class ClientT extends Transformer[Iterator[String], Iterator[ClientModel]] {
  def transform(input: Iterator[String]): Iterator[ClientModel] =
    input.map(_.split("\t") match {
      case Array(id, amount, a, b, c, d) =>
        ClientModel(ClientId(id), amount.toInt, a.toInt, b.toInt, c.toInt, d.toInt)
      case _                             =>
        throw new RuntimeException("Cannot transform file data as Client model")
    })
}

@Singleton
class OrderT extends Transformer[Iterator[String], Iterator[OrderModel]] {
  def transform(input: Iterator[String]): Iterator[OrderModel] =
    input.map(_.split("\t") match {
      case Array(clientId, direction, src, count, price) =>
        OrderModel(ClientId(clientId), Direction.withName(direction), Securities.withName(src), count.toInt, price.toInt)
      case _                                             =>
        throw new RuntimeException("Cannot transform file data as Order model")
    })

}
