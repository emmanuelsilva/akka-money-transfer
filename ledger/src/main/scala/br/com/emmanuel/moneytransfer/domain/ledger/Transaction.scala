package br.com.emmanuel.moneytransfer.domain.ledger

import java.util.Calendar

sealed trait Transaction
case class DebitTransaction(id: String, kind: String, instant: Calendar, account: Account, amount: BigDecimal) extends Transaction
case class CreditTransaction(id: String, kind: String, instant: Calendar, account: Account, amount: BigDecimal) extends Transaction