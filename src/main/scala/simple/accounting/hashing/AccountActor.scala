package simple.accounting.hashing

import akka.actor.Actor
import akka.routing.ConsistentHashingRouter.ConsistentHashable

case class Init(account: Int) extends ConsistentHashable {
  override def consistentHashKey: Any = account
}

case class Balance(account: Int) extends ConsistentHashable {
  override def consistentHashKey: Any = account
}

case class Debit(account: Int, amount: Int) extends ConsistentHashable {
  override def consistentHashKey: Any = account
}

case class Credit(account: Int, amount: Int) extends ConsistentHashable {
  override def consistentHashKey: Any = account
}

class AccountActor extends Actor {
  def receive = accountsBehaviour()
  def accountsBehaviour(accounts: Map[Int, Int] = Map.empty): Actor.Receive = {
    case Init(account) =>
      sender() ! accounts.get(account).map(_ => Left("Account already exist")).getOrElse {
        context.become(accountsBehaviour(accounts.updated(account, 0)))
        Right(0)
      }

    case Balance(account) =>
      sender() ! accounts.get(account).map(Right(_)).getOrElse(noAccount(account))

    case Debit(account, amount) => sender() ! accounts.get(account).map {
      balance =>
        val newBalance = balance + amount
        context.become(accountsBehaviour(accounts.updated(account, newBalance)))
        Right(newBalance)
    }.getOrElse(noAccount(account))

    case Credit(account, amount) => sender() ! accounts.get(account).map {
      balance =>
        val newBalance = balance - amount
        if (newBalance < 0) {
          Left(s"Not enough funds. Account $account; Balance $balance; Credit $amount")
        } else {
          context.become(accountsBehaviour(accounts.updated(account, newBalance)))
          Right(newBalance)
        }
    }.getOrElse(noAccount(account))

    case msg => sender() ! Left(s"Unknown msg $msg")
  }

  def noAccount(account: Int): Either[String, Int] = Left(s"I have no account $account")
}
