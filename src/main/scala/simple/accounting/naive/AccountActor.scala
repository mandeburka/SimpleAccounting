package simple.accounting.naive

import akka.actor._
import akka.event.Logging

case class Debit(amount: Int)
case class Credit(amount: Int)
case object Balance

class AccountActor extends Actor {
  def receive = balanceBehaviour(0)
  def balanceBehaviour(balance: Int): Actor.Receive = {
    case Debit(amount) =>
      context.become(balanceBehaviour(balance + amount))
      sender() ! Right(balance + amount)
    case Credit(amount) =>
      if (balance >= amount) {
        context.become(balanceBehaviour(balance - amount))
        sender() ! Right(balance - amount)
      } else {
        sender() ! Left(s"Can not credit $amount from $balance")
      }
    case Balance =>
      sender() ! Right(balance)
  }
}

class Accounting extends Actor {
  """
    
  """.stripMargin
  val log = Logging(context.system, this)
  def receive = routerBehaviour()

  def routerBehaviour(accounts: Map[Int, ActorRef] = Map.empty): Actor.Receive = {
    case (account: Int, action: Any) =>
      val actor = accounts.getOrElse(account, {
        log.info(s"Creating actor for $account")
        val accountActor = context.actorOf(Props[AccountActor])
        context watch accountActor
        context.become(routerBehaviour(accounts + (account -> accountActor)))
        accountActor
      })
      actor.tell(action, sender())

    case Terminated(a) =>
      context.become(routerBehaviour(accounts.filterNot(_ == a)))
  }
}