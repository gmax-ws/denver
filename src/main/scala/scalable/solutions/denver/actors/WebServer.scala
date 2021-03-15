/*
 * Copyright (c) 2021 Scalable Solutions
 *
 * Author: Marius Gligor <marius.gligor@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111, USA.
 */
package scalable.solutions.denver.actors

import akka.actor.typed._
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl._
import akka.http.scaladsl.Http
import scalable.solutions.denver._
import scalable.solutions.denver.actors.WebServer._
import scalable.solutions.denver.actors.Worker.{PersonsResponse, _}
import scalable.solutions.denver.mongo.{MongoDaemon, PersonConfig}
import scalable.solutions.denver.repo.{Person, PersonRepo}
import scalable.solutions.denver.routes.PersonRoutes
import scalable.solutions.denver.services.PersonServices

import java.util.UUID
import scala.concurrent.Future

object WebServer {

  sealed trait Command

  final case object Startup extends Command

  final case object Shutdown extends Command

  final case class QueryPersons[A](replyTo: ActorRef[PersonsResult[A]], query: PersonQuery) extends Command

  final case class PersonsResult[A](persons: Future[PersonsResponse[A]]) extends Command

  def apply(): Behavior[Command] = Behaviors.setup[Command](context => new WebServer(context))
}

class WebServer(context: ActorContext[Command]) extends AbstractBehavior[Command](context) {
  context.log.info("Web Server started")
  private val config = context.system.settings.config
  private val corsEnabled = config.getBoolean("akka.http.corsEnabled")
  private val host = config.getString("akka.http.host")
  private val port = config.getInt("akka.http.port")
  private val services = PersonServices(context.self)
  private val routes = PersonRoutes(services)

  private val personConfig = PersonConfig.config(config.getConfig("mongo.db"))
  private val repo: PersonRepo = PersonRepo(MongoDaemon[Person](personConfig))
  private val pool: PoolRouter[WorkerCommand] = Routers.pool(poolSize = config.getInt("akka.router.pool-size")) {
    // make sure the workers are restarted if they fail
    Behaviors.supervise(Worker()).onFailure[Exception](SupervisorStrategy.restart)
  }

  override def onMessage(msg: Command): Behavior[Command] = msg match {
    case Startup =>
      Http().newServerAt(host, port).bind(routes.routes(corsEnabled))
      context.log.info(s"Server online at http://$host:$port/")
      Behaviors.same
    case Shutdown =>
      Behaviors.stopped
    case QueryPersons(replyTo, query) =>
      val id = UUID.randomUUID().toString
      val worker = context.spawn(pool, s"worker-pool-$id")
      replyTo ! PersonsResult(worker.ask(ref => PersonsRequest(ref, repo, query)))
      Behaviors.same
    case _ =>
      Behaviors.ignore
  }

  override def onSignal: PartialFunction[Signal, Behavior[Command]] = {
    case PostStop =>
      context.log.info("Web Server stopped")
      this
  }
}