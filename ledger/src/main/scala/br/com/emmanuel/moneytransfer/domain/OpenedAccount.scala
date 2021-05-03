package br.com.emmanuel.moneytransfer.domain

import br.com.emmanuel.moneytransfer.event.AccountEvent.{AccountClosed, Debited, Deposited}
import br.com.emmanuel.moneytransfer.event.Event

final case class OpenedAccount(id: String, customerId: String, balance: BigDecimal, transactions: Seq[Transaction]) extends Account {

  require(balance >= 0, "Account balance can't be negative")

  override def applyEvent(event: Event): Account =
    event match {
      case deposited: Deposited =>
        val transaction = Transaction("credit", deposited.kind, deposited.instant, deposited.amount)
        copy(balance = balance + deposited.amount, transactions = transactions :+ transaction)
      case debited: Debited =>
        val transaction = Transaction("debit", debited.kind, debited.instant, debited.amount)
        copy(balance = balance - debited.amount, transactions = transactions :+ transaction)
      case AccountClosed => ClosedAccount(id)
    }

  def canDebit(debitAmount: BigDecimal): Boolean = balance >= debitAmount

  def canClose: Boolean = balance == 0
}
