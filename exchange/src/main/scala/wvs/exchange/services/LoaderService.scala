package wvs.exchange.services

import javax.inject._
import wvs.exchange._
import java.io.File

import akka.actor.typed.{ActorSystem, Terminated}
import wvs.exchange.services.actors.{ClientHolderActor, OrderHolderActor}

import scala.concurrent.Future

@Singleton
class LoaderService @Inject()(reader: ResourceReader,
                              clientT: ClientT,
                              orderT: OrderT,
                              @Named("directory") dir: String) {

  def start(): Future[Terminated] = {
    var clientMap = Map.empty[ClientId, ClientModel]
    var orderBuyList = List[OrderModel]()
    var orderSellList = List[OrderModel]()

    val clients = reader.read(s"$dir${File.separator}clients.txt")
    clientT.transform(clients)
      .foreach(cm => clientMap += (cm.id -> cm))

    val orders = reader.read(s"$dir${File.separator}orders.txt")
    orderT.transform(orders)
      .foreach(om => om.direction match {
        case Direction.s =>
          orderSellList = orderSellList :+ om
        case Direction.b =>
          orderBuyList = orderBuyList :+ om
      })

    val clientHolderActor = ActorSystem(ClientHolderActor(clientMap, dir), "OrderHolderActor")
    ActorSystem(OrderHolderActor(clientHolderActor, orderBuyList, orderSellList.toArray), "OrderHolderActor")

    clientHolderActor.whenTerminated
  }

}