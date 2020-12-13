package br.com.emmanuel.moneytransfer.domain

sealed trait Transaction

sealed abstract class DebitTransaction(val account: Account, val amount: BigDecimal) extends Transaction
sealed abstract class CreditTransaction(val account: Account, val amount: BigDecimal) extends Transaction

case class DepositTransaction(override val account: Account, override val amount: BigDecimal) extends CreditTransaction(account, amount)
case class WithdrawTransaction(override val account: Account, override val amount: BigDecimal) extends DebitTransaction(account, amount)
case class P2PTransferTransaction(override val account: Account, override val amount: BigDecimal, destinationAccount: Account) extends DebitTransaction(account, amount)
