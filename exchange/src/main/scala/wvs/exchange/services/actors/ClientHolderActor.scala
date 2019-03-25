package wvs.exchange.services.actors

import java.io.{File, PrintWriter}
import java.net.URI

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.util.Timeout
import wvs.exchange.{ClientId, ClientModel}
import wvs.exchange.services.actors.ClientActor.{ClientMessage, Get}
import wvs.exchange.services.actors.OrderHolderActor.{OrderHolderMessage, ResponseClients}

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object ClientHolderActor {

  def apply(clientMap: Map[ClientId, ClientModel], directory: String): Behavior[ClientHolderMessage] = {
    Behaviors.setup(context => new ClientHolderActor(clientMap, directory, context))
  }

  sealed trait ClientHolderMessage

  final case class GetClients(clientBuyerId: ClientId, clientSellerId: ClientId, replyTo: ActorRef[OrderHolderMessage]) extends ClientHolderMessage

  final case object SaveResult extends ClientHolderMessage

  final case object Stop extends ClientHolderMessage

  final case class ResponseClient(model: ClientModel) extends ClientHolderMessage

}

import wvs.exchange.services.actors.ClientHolderActor._

class ClientHolderActor(clientMap: Map[ClientId, ClientModel], directory: String, context: ActorContext[ClientHolderMessage])
  extends AbstractBehavior[ClientHolderMessage] {

  import ClientHolderActor._

  implicit val scheduler = context.system.scheduler
  implicit val timeout: Timeout = 3.seconds

  private val clientActorMap: Map[ClientId, ActorRef[ClientMessage]] =
    clientMap.map {
      case (id, model) =>
        id -> context.spawn(ClientActor(model), s"ClientActor_${model.id}")
    }

  override def onMessage(msg: ClientHolderMessage): Behavior[ClientHolderMessage] =
    msg match {
      case GetClients(buyerId, sellerId, replyTo) =>
        replyTo ! ResponseClients(clientActorMap.get(buyerId), clientActorMap.get(sellerId))
        this

      case SaveResult =>
        val clientsFut = clientActorMap.map {
          case (_, clientRef) =>
            clientRef.?[ResponseClient](ref => Get(ref))
        }

        val fut = Future.sequence(clientsFut)

        context.log.info("Writing result ...")

        val src = s"${getClass.getResource(directory).toString}${File.separator}result.txt"
        val pw = new PrintWriter(new File(new URI(src)))
        val res = fut.map { seq =>
          seq.map(_.model).toSeq
            .sortBy(v => v.id.suffix)
            .foreach(model => pw.write(s"${model.toString}\n"))
          pw.close()
        }

        context.pipeToSelf(res){
          case Success(_) =>
            Stop
          case Failure(th) =>
            context.log.error(s"Error happened during in processing case SaveResult: ${th.getMessage}")
            Stop
        }

        this

      case Stop =>
        Behavior.stopped
    }

}
