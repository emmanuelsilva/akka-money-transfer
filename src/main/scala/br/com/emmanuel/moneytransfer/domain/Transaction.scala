package br.com.emmanuel.moneytransfer.domain

case class Account(id: String)

sealed trait Transaction

case class DepositTransaction(amount: BigDecimal) extends Transaction

case class WithdrawTransaction(amount: BigDecimal) extends Transaction

case class P2PTransferTransaction(amount: BigDecimal, destinationAccount: Account) extends Transaction
