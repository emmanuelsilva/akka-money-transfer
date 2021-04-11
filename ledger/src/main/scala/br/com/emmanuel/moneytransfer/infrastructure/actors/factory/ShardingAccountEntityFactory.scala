package br.com.emmanuel.moneytransfer.infrastructure.actors.factory
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import br.com.emmanuel.moneytransfer.infrastructure.actors.ledger.AccountLedgerEntityActor

object ShardingAccountEntityFactory {
  def apply(sharding: ClusterSharding): ShardingAccountEntityFactory = new ShardingAccountEntityFactory(sharding)
}

class ShardingAccountEntityFactory(sharding: ClusterSharding) extends AccountEntityFactory {

  override def getAccountEntity(accountId: String): EntityRef[AccountLedgerEntityActor.Command] = {
    sharding.entityRefFor(AccountLedgerEntityActor.TypeKey, accountId)
  }
}
