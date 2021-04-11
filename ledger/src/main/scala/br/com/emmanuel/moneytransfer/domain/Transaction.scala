package br.com.emmanuel.moneytransfer.domain

import java.util.Calendar

final case class Transaction(entryType: String, kind: String, instant: Calendar, amount: BigDecimal)
