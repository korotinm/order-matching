package wvs.exchange.services

import javax.inject._
import wvs.exchange._
import java.io.File

import akka.actor.typed.{ActorSystem, Terminated}
import wvs.exchange.services.actors.{ClientHolderActor, OrderHolderActor}

import scala.concurrent.Future
import scala.io.Source

@Singleton
class LoaderService @Inject()(@Named("directory") dir: String) {

  def start(): Future[Terminated] = {

    def clientTransformer(iter: Iterator[String]) = {
      iter.foldLeft(Map.empty[ClientId, ClientModel]) {
        case (acc, v) =>
          acc + (v.split("\t") match {
            case Array(id, amount, a, b, c, d) =>
              ClientId(id) -> ClientModel(ClientId(id), amount.toInt, a.toInt, b.toInt, c.toInt, d.toInt)
            case _                             =>
              throw new RuntimeException(s"Cannot transform file data as Client model(line = $v)")
          })
      }
    }

    def orderTransformer(iter: Iterator[String]): (List[OrderModel], Map[Securities, Array[OrderModel]]) = {
      iter.foldLeft((List.empty[OrderModel], Map.empty[Securities, Array[OrderModel]])) {
        case ((buyList, sellMap), v) =>
          v.split("\t") match {
            case Array(clientId, direction, src, count, price) =>
              val model = OrderModel(ClientId(clientId), Direction.withName(direction), Securities.withName(src), count.toInt, price.toInt)
              model.direction match {
                case Direction.s =>
                  val arr = sellMap.getOrElse(model.src, Array.empty[OrderModel])
                  (buyList, sellMap + (model.src -> (arr :+ model)))
                case Direction.b =>
                  (buyList :+ model, sellMap)
              }
            case _                                             =>
              throw new RuntimeException("Cannot transform file data as Order model")
          }
      }
    }

    val clients = read(s"$dir${File.separator}clients.txt", clientTransformer)
    val (bList, sMap) = read(s"$dir${File.separator}orders.txt", orderTransformer)

    val clientHolderActor = ActorSystem(ClientHolderActor(clients, dir), "OrderHolderActor")
    ActorSystem(OrderHolderActor(clientHolderActor, bList, sMap), "OrderHolderActor")

    clientHolderActor.whenTerminated
  }

  private def read[B](source: String, transformer: Iterator[String] => B): B =
    transformer(Source.fromURL(getClass.getResource(source)).getLines())

}