package br.com.emmanuel.moneytransfer.domain

abstract class Transaction(val amount: BigDecimal)

case class DepositTransaction(depositAmount: BigDecimal) extends Transaction(depositAmount)

case class WithdrawTransaction(withdrawAmount: BigDecimal) extends Transaction(withdrawAmount)
