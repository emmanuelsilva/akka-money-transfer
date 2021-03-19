package br.com.emmanuel.moneytransfer

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import br.com.emmanuel.moneytransfer.domain.ledger.BankLedgerProtocol.{CreateAccount, Credit, GetAccountBalance, Debit}
import br.com.emmanuel.moneytransfer.domain.ledger.{Account, BankLedgerProtocol}
import br.com.emmanuel.moneytransfer.infrastructure.actors.ledger.BankLedgerActor

import java.util.Calendar
import java.util.concurrent.Executors
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.util.Random

object Run extends App {

  implicit val timeout: Timeout = 3.seconds
  implicit val bankLedgerActor = ActorSystem(BankLedgerActor(), "bankLedgerActor")
  implicit val ec = bankLedgerActor.executionContext

  val instant = Calendar.getInstance()

  println("press a command...")

  Iterator.continually(io.StdIn.readLine)
    .takeWhile(_ != "x")
    .foreach {
      case s"c:$accountId" => createAccount(accountId)
      case s"d:$accountId" => doDeposit(accountId)
      case s"w:$accountId" => doWithdraw(accountId)
      case s"b:$accountId" => printBalance(accountId)
      case s"p:$accountId" => simulateParallelRequests(accountId)
      case "e" => System.exit(0)
      case _   => println("unknown")
    }

  def createAccount(accountId: String) = {
    bankLedgerActor ! CreateAccount(Account(accountId))
    println(s"account ${accountId} created")
  }

  def doDeposit(accountId: String) = {
    val result = bankLedgerActor.ask(rep => Credit("deposit", instant, Account(accountId), 100, rep))
    val response = Await.result(result, 5.seconds)
    println(s"deposit response: ${response}")
  }

  def doWithdraw(accountId: String) = {
    val result = bankLedgerActor.ask(rep => Debit("withdraw", instant, Account(accountId), 100, rep))
    val response = Await.result(result, 5.seconds)
    println(s"withdraw response: ${response}")
  }

  def printBalance(accountId: String) = {
    val result = bankLedgerActor.ask(rep => GetAccountBalance(Account(accountId), rep))
    val response = Await.result(result, 5.seconds)
    println(s"balance response ${response}")
  }

  def simulateParallelRequests(accountId: String): Unit = {
    val pool = Executors.newFixedThreadPool(8)
    pool.execute(new SimulateParallelRequests(accountId))
    pool.execute(new SimulateParallelRequests(accountId))
    pool.execute(new SimulateParallelRequests(accountId))
    pool.execute(new SimulateParallelRequests(accountId))
  }

  class SimulateParallelRequests(accountId: String) extends Runnable{
    def run(): Unit = {
      val r = Random.between(1, 10)
      println(r)
      (1 to 10000).foreach(i => {
        var future: Future[BankLedgerProtocol.Response] = null

        if (i % r == 0) {
          future = bankLedgerActor.ask(rep => Credit("deposit", instant, Account(accountId), 100, rep))
        } else {
          future = bankLedgerActor.ask(rep => Debit("withdraw", instant, Account(accountId), 100, rep))
        }

        val response =  Await.result(future, 5.seconds)
        println(s"${Thread.currentThread.getName} $i request response: $response")
      })
    }
  }

}
