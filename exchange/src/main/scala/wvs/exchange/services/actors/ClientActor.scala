package wvs.exchange.services.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import wvs.exchange.services.actors.ClientActor._
import wvs.exchange.services.actors.ClientHolderActor.ResponseClient
import wvs.exchange.services.actors.OrderHolderActor.DealResult
import wvs.exchange.{ClientId, ClientModel, Direction, Securities}

object ClientActor {

  def apply(clientModel: ClientModel): Behavior[ClientMessage] = {
    Behaviors.setup(context => new ClientActor(clientModel, context))
  }

  sealed trait ClientMessage

  final case class Deal(amount: Int, direction: Direction, typ: Securities, count: Int, replyTo: ActorRef[DealResult]) extends ClientMessage

  final case class SaveModel(clientModel: ClientModel) extends ClientMessage

  final case class Get(replyTo: ActorRef[ClientHolderActor.ResponseClient]) extends ClientMessage

}

class ClientActor(clientModel: ClientModel, context: ActorContext[ClientMessage])
  extends AbstractBehavior[ClientMessage] {

  import ClientActor._

  private var model: ClientModel = clientModel

  override def onMessage(msg: ClientMessage): Behavior[ClientMessage] =
    msg match {
      case Deal(amnt, direction, typ, count, replyTo) =>
        val (a, c) = direction match {
          case Direction.b => (-amnt, count)
          case Direction.s => (amnt, -count)
        }

        replyTo ! update(a, typ, c)
        this

      case SaveModel(v) =>
        model = v
        this

      case Get(replyTo) =>
        replyTo ! ResponseClient(model)
        this
    }

  private def update(amount: Int, typ: Securities, count: Int): DealResult = {
    import wvs.exchange.Securities._

    val amnt = model.amount + (amount * math.abs(count))
    if (amnt >= 0)
      typ match {
        case A if model.aCount + count >= 0 =>
          DealResult(Some(model.copy(amount = amnt, aCount = model.aCount + count)))
        case B if model.bCount + count >= 0 =>
          DealResult(Some(model.copy(amount = amnt, bCount = model.bCount + count)))

        case C if model.cCount + count >= 0 =>
          DealResult(Some(model.copy(amount = amnt, cCount = model.cCount + count)))
        case D if model.dCount + count >= 0 =>
          DealResult(Some(model.copy(amount = amnt, dCount = model.dCount + count)))
        case _                              =>
          DealResult(None)
      }
    else DealResult(None)
  }

}