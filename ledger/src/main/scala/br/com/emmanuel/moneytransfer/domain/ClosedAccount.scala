package br.com.emmanuel.moneytransfer.domain

import br.com.emmanuel.moneytransfer.event.Event

case class ClosedAccount(id: String) extends Account {
  override def applyEvent(event: Event): Account =
    throw new IllegalArgumentException(s"unexpected event=[$event] in state [ClosedAccount]")
}
