package br.com.emmanuel.moneytransfer.event

import br.com.emmanuel.moneytransfer.infrastructure.serialization.SerializableMessage

import java.util.Calendar

sealed trait Event extends SerializableMessage

object AccountEvent {

  case object AccountOpened extends Event
  case object AccountClosed extends Event
  final case class Deposited(kind: String, instant: Calendar, amount: BigDecimal) extends Event
  final case class Debited(kind: String, instant: Calendar, amount: BigDecimal) extends Event

}
