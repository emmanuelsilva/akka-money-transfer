package br.com.emmanuel.moneytransfer.domain

trait Transaction

case class DepositTransaction(amount: BigDecimal) extends Transaction()

case class WithdrawTransaction(amount: BigDecimal) extends Transaction()
