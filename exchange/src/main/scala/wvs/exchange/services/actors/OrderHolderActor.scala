package wvs.exchange.services.actors

import akka.actor.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.util.Timeout
import wvs.exchange.services.actors.ClientActor.{ClientMessage, Deal, SaveModel}
import wvs.exchange.services.actors.ClientHolderActor.{ClientHolderMessage, GetClients}
import wvs.exchange._

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._


object OrderHolderActor {

  def apply(clientHolder: ActorRef[ClientHolderMessage],
            orderBuyList: List[OrderModel],
            orderSellArr: Array[OrderModel]): Behavior[OrderHolderMessage] =
    Behaviors.setup { context =>
      val behavior = new OrderHolderActor(clientHolder, orderBuyList, orderSellArr, context)

      context.self ! Next

      behavior
    }

  sealed trait OrderHolderMessage

  final case class ResponseClients(buyerActor: Option[ActorRef[ClientMessage]], sellerActor: Option[ActorRef[ClientMessage]]) extends OrderHolderMessage

  final case class DealResult(clientModel: Option[ClientModel]) extends OrderHolderMessage

  final case class ProcessOrders(buyerModel: ClientModel, sellerModel: ClientModel,
                                 buyOrder: OrderModel, sellOrder: OrderModel,
                                 buyerActor: ActorRef[ClientMessage], sellerActor: ActorRef[ClientMessage]) extends OrderHolderMessage

  final case object Next extends OrderHolderMessage

  final case object Stop extends OrderHolderMessage

  final case class ErrorMsg(str: String, th: Option[Throwable] = None) extends OrderHolderMessage {
    override def toString: String =
      s"\nMESSAGE: $str${th.map(v => s"\nCAUSE: $v\n").getOrElse("\n")}"
  }

  final case class FatalMsg(str: String, th: Option[Throwable] = None) extends OrderHolderMessage {
    override def toString: String =
      s"\nMESSAGE: $str${th.map(v => s"\nCAUSE: $v\n").getOrElse("\n")}"
  }

}

import wvs.exchange.services.actors.OrderHolderActor._

class OrderHolderActor(clientHolder: ActorRef[ClientHolderMessage],
                       orderBuyList: List[OrderModel],
                       orderSellArr: Array[OrderModel],
                       context: ActorContext[OrderHolderMessage]) extends AbstractBehavior[OrderHolderMessage] {

  implicit val scheduler: Scheduler = context.system.scheduler
  implicit val timeout: Timeout = 3.seconds

  private var buyList = orderBuyList
  private var sellArr = orderSellArr

  private var sellIndex = 0

  override def onMessage(msg: OrderHolderMessage): Behavior[OrderHolderMessage] = {

    def next(): Unit = {
      if (sellArr.isEmpty)
        buyList = Nil
      else if (sellIndex >= sellArr.length) {
        sellIndex = 0
        buyList = buyList.tail
      }
    }

    msg match {
      case Next =>
        next()
        buyList match {
          case buyOrder :: _ =>
            val sellerOrder = sellArr(sellIndex)
            if (buyOrder.clientId != sellerOrder.clientId && buyOrder.src == sellerOrder.src)
              clientHolder ! GetClients(buyOrder.clientId, sellerOrder.clientId, context.self)
            else {
              sellIndex += 1
              context.self ! Next
            }

          case Nil =>
            context.self ! Stop
        }

        this

      case ResponseClients(mbBuyerActor, mbSellerActor) =>
        (mbBuyerActor, mbSellerActor) match {
          case (Some(buyerActor), Some(sellerActor)) =>

            val buyOrder = buyList.head
            val sellOrder = sellArr(sellIndex)

            val minCount =
              if (buyOrder.count > sellOrder.count) sellOrder.count
              else buyOrder.count

            val buyRes: Future[DealResult] = buyerActor ? (ref => Deal(sellOrder.price, Direction.b, buyOrder.src, minCount, ref))
            val sellRes: Future[DealResult] = sellerActor ? (ref => Deal(sellOrder.price, Direction.s, sellOrder.src, minCount, ref))

            val bsFut = for {
              b <- buyRes
              s <- sellRes
            } yield (b, s)

            val res = bsFut.map[OrderHolderMessage] {
              case (DealResult(Some(b)), DealResult(Some(s))) =>
                ProcessOrders(b, s, buyOrder, sellOrder, buyerActor, sellerActor)

              case _ =>
                sellIndex += 1
                Next
            }

            context.pipeToSelf(res) {
              case Success(v)  =>
                v
              case Failure(th) =>
                FatalMsg(s"Error happened during in processing ResponseClient", Some(th))
            }

          case (None, Some(_)) =>
            context.self ! ErrorMsg(
              s"""Error happened during in processing ResponseClient:
                    buyer ActorRef is empty (client id = '${buyList.head.clientId}')""")
            buyList = buyList.tail
            context.self ! Next

          case (Some(_), None) =>
            context.self ! ErrorMsg(
              s"""Error happened during in processing ResponseClient:
                    seller ActorRef is empty (client id = '${sellArr(sellIndex).clientId}')""")
            sellArr = delCurrentSellOrder()
            context.self ! Next

          case (None, None) =>
            context.self ! ErrorMsg(
              s"""Error happened during in processing ResponseClient:
                    buyer and seller ActorRef are empty
                    (buyer client id = '${buyList.head.clientId}'; seller client id = '${sellArr(sellIndex).clientId}')""")
            buyList = buyList.tail
            sellArr = delCurrentSellOrder()
            context.self ! Next
        }

        this

      case ProcessOrders(buyer, seller, buyOrder, sellOrder, buyerActor, sellerActor) =>
        buyerActor ! SaveModel(buyer)
        sellerActor ! SaveModel(seller)

        if (buyOrder.count > sellOrder.count) {
          sellArr = delCurrentSellOrder()
          buyList = buyList.head.copy(count = buyOrder.count - sellOrder.count) :: buyList.tail

        } else if (buyOrder.count == sellOrder.count) {
          sellArr = delCurrentSellOrder()
          buyList = buyList.tail
          sellIndex = 0

        } else {
          sellArr.update(sellIndex, sellOrder.copy(count = sellOrder.count - buyOrder.count))
          buyList = buyList.tail
          sellIndex = 0
        }

        context.self ! Next
        this

      case err: ErrorMsg =>
        context.log.error(err.toString)
        this

      case err: FatalMsg =>
        context.log.error(err.toString)
        context.self ! Stop
        this

      case Stop =>
        clientHolder ! ClientHolderActor.SaveResult
        Behavior.stopped
    }
  }

  private def delCurrentSellOrder(): Array[OrderModel] =
    sellArr.take(sellIndex) ++ sellArr.takeRight(sellArr.length - sellIndex - 1)
}