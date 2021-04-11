package br.com.emmanuel.moneytransfer.infrastructure.factory

import akka.actor.typed.ActorRef
import akka.cluster.sharding.typed.scaladsl.EntityRef
import akka.cluster.sharding.typed.testkit.scaladsl.TestEntityRef
import br.com.emmanuel.moneytransfer.infrastructure.actors.factory.AccountEntityFactory
import br.com.emmanuel.moneytransfer.infrastructure.actors.ledger.AccountLedgerActor

object AccountTestEntityFactory {
  def apply(probe: ActorRef[AccountLedgerActor.Command]): AccountTestEntityFactory = new AccountTestEntityFactory(probe)
}

class AccountTestEntityFactory(probe: ActorRef[AccountLedgerActor.Command]) extends AccountEntityFactory {

  override def getAccountEntity(accountId: String): EntityRef[AccountLedgerActor.Command] = {
    TestEntityRef(AccountLedgerActor.TypeKey, accountId, probe)
  }
}
