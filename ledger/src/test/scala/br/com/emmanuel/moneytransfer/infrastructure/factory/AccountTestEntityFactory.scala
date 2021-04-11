package br.com.emmanuel.moneytransfer.infrastructure.factory

import akka.actor.typed.ActorRef
import akka.cluster.sharding.typed.scaladsl.EntityRef
import akka.cluster.sharding.typed.testkit.scaladsl.TestEntityRef
import br.com.emmanuel.moneytransfer.infrastructure.actors.factory.AccountEntityFactory
import br.com.emmanuel.moneytransfer.infrastructure.actors.ledger.AccountLedgerEntityActor

object AccountTestEntityFactory {
  def apply(probe: ActorRef[AccountLedgerEntityActor.Command]): AccountTestEntityFactory = new AccountTestEntityFactory(probe)
}

class AccountTestEntityFactory(probe: ActorRef[AccountLedgerEntityActor.Command]) extends AccountEntityFactory {

  override def getAccountEntity(accountId: String): EntityRef[AccountLedgerEntityActor.Command] = {
    TestEntityRef(AccountLedgerEntityActor.TypeKey, accountId, probe)
  }
}
