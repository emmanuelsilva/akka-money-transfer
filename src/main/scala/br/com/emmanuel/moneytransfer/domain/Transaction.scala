package br.com.emmanuel.moneytransfer.domain

sealed trait Transaction

sealed abstract class DebitTransaction(val amount: BigDecimal) extends Transaction
sealed abstract class CreditTransaction(val amount: BigDecimal) extends Transaction

case class DepositTransaction(override val amount: BigDecimal) extends CreditTransaction(amount)
case class WithdrawTransaction(override val amount: BigDecimal) extends DebitTransaction(amount)
case class P2PTransferTransaction(override val amount: BigDecimal, destinationAccount: Account) extends DebitTransaction(amount)
