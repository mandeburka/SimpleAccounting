package simple.accounting.hashing

import akka.routing.ConsistentHashingPool

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration._

object Main {
  implicit val timeout = Timeout(25.seconds)
  def main(args: Array[String]) {
    val system = ActorSystem("simpleAccounting")
    val router = system.actorOf(ConsistentHashingPool(10).props(Props[AccountActor]), "router")

    system.actorOf(Props(classOf[Terminator], router), "terminator")
    assert(Await.ready(router ? Init(1), Duration.Inf).value.get.get == Right(0))
    assert(Await.ready(router ? Debit(1, 10), Duration.Inf).value.get.get == Right(10))
    assert(Await.ready(router ? Credit(1, 10), Duration.Inf).value.get.get == Right(0))
    assert(Await.ready(router ? Credit(1, 10), Duration.Inf).value.get.get.asInstanceOf[Either[String, Int]].isLeft)

    router ! PoisonPill
  }
}

class Terminator(ref: ActorRef) extends Actor with ActorLogging {
  context watch ref
  def receive = {
    case Terminated(_) =>
      log.info("{} has terminated, shutting down system", ref.path)
      context.system.shutdown()
  }
}