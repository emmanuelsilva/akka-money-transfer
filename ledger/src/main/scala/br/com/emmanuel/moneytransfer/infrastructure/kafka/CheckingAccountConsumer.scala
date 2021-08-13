package br.com.emmanuel.moneytransfer.infrastructure.kafka

import akka.Done
import akka.actor.typed.{ActorSystem, Scheduler}
import akka.cluster.sharding.typed.scaladsl.{ClusterSharding, EntityRef}
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.kafka.scaladsl.{Committer, Consumer}
import akka.kafka.{CommitterSettings, ConsumerSettings, Subscriptions}
import akka.pattern.StatusReply
import akka.stream.Materializer
import akka.util.Timeout
import br.com.emmanuel.moneytransfer.infrastructure.actors.factory.ShardingAccountEntityFactory
import br.com.emmanuel.moneytransfer.infrastructure.actors.ledger.AccountLedgerActor
import br.com.emmanuel.moneytransfer.infrastructure.actors.ledger.AccountLedgerActor.OpenAccount
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.{DefaultScalaModule, ScalaObjectMapper}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{LongDeserializer, StringDeserializer}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

private case class Customer(id: Long, name: String)
private case class CheckingAccount(id: Long, version: Long, iban: String, currency: String, customer: Customer)
private case class AccountEvent(@JsonProperty("type") eventType: String, timestamp: String, checkingAccount: CheckingAccount)

object CheckingAccountConsumer {

  implicit val timeout: Timeout = 30.seconds

  def start(system: ActorSystem[Nothing], sharding: ClusterSharding)(implicit scheduler: Scheduler,
                                                                     executionContext: ExecutionContext): Unit = {
    system.log.info("starting kafka consumer")

    implicit val materializer: Materializer = Materializer(system)
    val entityFactory = ShardingAccountEntityFactory(sharding)

    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)

    val consumerSettings = ConsumerSettings(
      system,
      new LongDeserializer,
      new StringDeserializer
    ).withBootstrapServers("kafka.kafka.svc.cluster.local:9092")
     .withGroupId("ledger")
     .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")

    val defaultCommitterSettings = CommitterSettings(system)

    Consumer
      .committableSource(consumerSettings, Subscriptions.topics("checking_account_event"))
      .mapAsync(1)(message => {
        system.log.debug(s"received new account kafka message key=${message.record.key()} - value=${message.record.value()}")
        Future.successful((mapper.readValue[AccountEvent](message.record.value()), message))
      })
      .mapAsync(1)(message => {
        val (event, kafkaMessage) = message
        val accountEntity = entityFactory.getAccountEntity(event.checkingAccount.id.toString)
        processEvent(event, accountEntity)
          .map(result => {
            system.log.debug(s"${event.checkingAccount.id} - ${event.eventType} - creation result - $result")
            kafkaMessage.committableOffset
          })
      })
      .toMat(Committer.sink(defaultCommitterSettings))(DrainingControl.apply)
      .run()
  }

  private def processEvent(accountEvent: AccountEvent,
                           accountEntity: EntityRef[AccountLedgerActor.Command])(implicit scheduler: Scheduler,
                                                                                 executionContext: ExecutionContext): Future[StatusReply[Done]] = {
    accountEvent.eventType match {
      case "opened" => accountEntity.ask(rep => OpenAccount(accountEvent.checkingAccount.customer.id.toString, rep))
      case _        => Future.successful(StatusReply.Error("Event not mapped"))
    }
  }
}