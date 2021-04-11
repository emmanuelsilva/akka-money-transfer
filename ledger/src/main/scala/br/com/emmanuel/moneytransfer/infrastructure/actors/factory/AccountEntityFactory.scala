package br.com.emmanuel.moneytransfer.infrastructure.actors.factory

import akka.cluster.sharding.typed.scaladsl.EntityRef
import br.com.emmanuel.moneytransfer.infrastructure.actors.ledger.AccountLedgerActor

trait AccountEntityFactory {

  def getAccountEntity(accountId: String): EntityRef[AccountLedgerActor.Command]

}
