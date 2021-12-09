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
import akka.actor.typed.scaladsl.Behaviors
import scalable.solutions.denver.PersonOp._
import scalable.solutions.denver.PersonQuery
import scalable.solutions.denver.repo.PersonRepo

import scala.concurrent.Future

object Worker {

  sealed trait WorkerCommand

  final case class PersonsResponse[A](persons: Future[A]) extends WorkerCommand

  final case class PersonsRequest[A](replyTo: ActorRef[PersonsResponse[A]], repo: PersonRepo, query: PersonQuery) extends WorkerCommand

  def apply(): Behavior[WorkerCommand] = Behaviors.setup { context =>
    context.log.debug("Starting worker actor")
    context.log.debug(context.self.path.toString)

    Behaviors.receiveMessage[WorkerCommand] {
      case PersonsRequest(replyTo, repo, query) =>
        query match {
          case PersonQuery(GetPersons, _, _) =>
            replyTo ! PersonsResponse(repo.getPersons)
          case PersonQuery(FindPerson, id, _) =>
            replyTo ! PersonsResponse(repo.findPerson(id))
          case PersonQuery(DeletePerson, id, _) =>
            replyTo ! PersonsResponse(repo.deletePerson(id))
          case PersonQuery(CreatePerson, _, maybePerson) =>
            maybePerson.foreach(person => replyTo ! PersonsResponse(repo.createPerson(person)))
          case PersonQuery(UpdatePerson, _, maybePerson) =>
            maybePerson.foreach(person => replyTo ! PersonsResponse(repo.updatePerson(person)))
          case _ =>
            replyTo ! PersonsResponse(Future.successful("Received an unexpected PersonQuery message!"))
        }
        Behaviors.same
      case _ =>
        context.system.log.warn("Received an unknown message!")
        Behaviors.ignore
    }.receiveSignal {
      case (_, signal) =>
        context.log.debug(s"Received signal $signal")
        Behaviors.same
    }
  }
}

