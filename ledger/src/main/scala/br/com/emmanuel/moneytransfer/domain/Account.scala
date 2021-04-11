package br.com.emmanuel.moneytransfer.domain

import br.com.emmanuel.moneytransfer.event.Event
import br.com.emmanuel.moneytransfer.infrastructure.serialization.SerializableMessage

trait Account extends SerializableMessage {
  def applyEvent(event: Event): Account
}
