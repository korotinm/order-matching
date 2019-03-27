package wvs.exchange.services

import javax.inject._
import wvs.exchange._
import java.io.File

import akka.actor.typed.{ActorSystem, Terminated}
import wvs.exchange.services.actors.{ClientHolderActor, OrderHolderActor}

import scala.concurrent.Future

/**
  *
  * This is ugly class just for loading data
  */
@Singleton
class LoaderService @Inject()(reader: ResourceReader,
                              @Named("directory") dir: String) {

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

    def orderTransformer(iter: Iterator[String]) = {
      iter.foldLeft((List.empty[OrderModel], List.empty[OrderModel])) {
        case ((buyList, sellList), v) =>
          v.split("\t") match {
            case Array(clientId, direction, src, count, price) =>
              val model = OrderModel(ClientId(clientId), Direction.withName(direction), Securities.withName(src), count.toInt, price.toInt)
              model.direction match {
                case Direction.s =>
                  (buyList, sellList :+ model)
                case Direction.b =>
                  (buyList :+ model, sellList)
              }
            case _                                             =>
              throw new RuntimeException("Cannot transform file data as Order model")
          }
      }

    }

    val clients = reader.read(s"$dir${File.separator}clients.txt", clientTransformer)

    val (bList, sList) = reader.read(s"$dir${File.separator}orders.txt", orderTransformer)


    val clientHolderActor = ActorSystem(ClientHolderActor(clients, dir), "OrderHolderActor")
    ActorSystem(OrderHolderActor(clientHolderActor, bList, sList.toArray), "OrderHolderActor")

    clientHolderActor.whenTerminated
  }

}