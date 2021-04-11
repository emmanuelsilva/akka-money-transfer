package br.com.emmanuel.moneytransfer.infrastructure.actors.factory

import akka.cluster.sharding.typed.scaladsl.EntityRef
import br.com.emmanuel.moneytransfer.infrastructure.actors.ledger.AccountLedgerEntityActor

trait AccountEntityFactory {

  def getAccountEntity(accountId: String): EntityRef[AccountLedgerEntityActor.Command]

}
