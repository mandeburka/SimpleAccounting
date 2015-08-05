package simple.accounting.naive

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration._

object Main {
  def main (args: Array[String]) {
    val system = ActorSystem("simpleAccounting")
    val router = system.actorOf(Props[Accounting], "router")
    system.actorOf(Props(classOf[Terminator], router), "terminator")
    implicit val timeout = Timeout(25.seconds)

    assert(Await.ready(router ? (1, Debit(10)), Duration.Inf).value.get.get == Right(10))
    assert(Await.ready(router ? (1, Credit(10)), Duration.Inf).value.get.get == Right(0))
    assert(Await.ready(router ? (1, Credit(10)), Duration.Inf).value.get.get.asInstanceOf[Either[String, Int]].isLeft)

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