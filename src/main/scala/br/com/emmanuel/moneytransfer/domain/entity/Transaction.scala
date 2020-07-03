package br.com.emmanuel.moneytransfer.domain.entity

abstract class Transaction(val amount: BigDecimal)

case class DepositTransaction(depositAmount: BigDecimal) extends Transaction(depositAmount)
