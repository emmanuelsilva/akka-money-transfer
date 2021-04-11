package br.com.emmanuel.moneytransfer.infrastructure.rest.request

import java.util.Calendar

sealed trait TransactionRequest
case class DebitTransactionRequest(id: String, kind: String, instant: Calendar, amount: BigDecimal) extends TransactionRequest
case class CreditTransactionRequest(id: String, kind: String, instant: Calendar, amount: BigDecimal) extends TransactionRequest