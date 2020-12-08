package br.com.emmanuel.moneytransfer.domain

sealed trait Transaction

case class DepositTransaction(amount: BigDecimal) extends Transaction

case class WithdrawTransaction(amount: BigDecimal) extends Transaction
