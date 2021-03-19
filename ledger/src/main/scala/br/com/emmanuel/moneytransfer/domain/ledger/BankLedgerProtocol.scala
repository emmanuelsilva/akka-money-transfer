package br.com.emmanuel.moneytransfer.domain.ledger

import akka.actor.typed.ActorRef

import java.util.Calendar

object BankLedgerProtocol {

  sealed trait Command

  sealed trait CommandWithReply extends Command {
    def reply: ActorRef[Response]
  }

  //command model
  final case class CreateAccount(account: Account) extends Command
  final case class Credit(kind: String, instant: Calendar, account: Account, amount: BigDecimal, reply: ActorRef[Response]) extends CommandWithReply
  final case class Debit(kind: String, instant: Calendar, account: Account, amount: BigDecimal, reply: ActorRef[Response]) extends CommandWithReply

  //query model
  final case class GetAccounts(reply: ActorRef[Response]) extends CommandWithReply
  final case class GetAccountBalance(account: Account, reply: ActorRef[Response]) extends CommandWithReply
  final case class GetTransactions(account: Account, kind: String, reply: ActorRef[Response]) extends CommandWithReply
  final case class GetAllTransactions(account: Account, reply: ActorRef[Response]) extends CommandWithReply

  //response model
  sealed trait Response
  final case class Accounts(accounts: Seq[Account]) extends Response
  final case class Balance(account: Account, amount: BigDecimal) extends Response
  final case class Transactions(account: Account, transactions: Seq[Transaction]) extends Response
  final case class DepositConfirmed() extends Response
  final case class WithdrawConfirmed() extends Response

  //invalid response model
  final case class AccountNotFound(account: Account) extends Response
  final case class InsufficientFunds(account: Account, command: Command) extends Response
}
