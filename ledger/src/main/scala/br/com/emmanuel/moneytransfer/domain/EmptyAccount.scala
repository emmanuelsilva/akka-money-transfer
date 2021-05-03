package br.com.emmanuel.moneytransfer.domain

import br.com.emmanuel.moneytransfer.event.AccountEvent.AccountOpened
import br.com.emmanuel.moneytransfer.event.Event

case class EmptyAccount(id: String) extends Account {
  override def applyEvent(event: Event): Account = event match {
    case AccountOpened(customerId) => OpenedAccount(id, customerId, 0, Seq.empty)
    case _                         => throw new IllegalStateException(s"unexpected event=[$event] in state [EmptyAccount]")
  }
}
