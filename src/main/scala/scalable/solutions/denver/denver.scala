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
package scalable.solutions

import akka.actor.typed.{ActorSystem, Scheduler}
import akka.util.Timeout
import scalable.solutions.denver.PersonOp.PersonOp
import scalable.solutions.denver.actors.Guardian
import scalable.solutions.denver.actors.Guardian.GuardianCommand
import scalable.solutions.denver.repo.Person

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt

package object denver {
  implicit val system: ActorSystem[GuardianCommand] =
    ActorSystem[GuardianCommand](Guardian(), "mongo-system")
  implicit val ec: ExecutionContextExecutor = system.executionContext
  implicit val scheduler: Scheduler = system.scheduler
  implicit val timeout: Timeout = 3.seconds

  final case class PersonQuery(query: PersonOp, id: Int = 0, person: Option[Person] = None)

  object PersonOp extends Enumeration {
    type PersonOp = Value
    val GetPersons, FindPerson, CreatePerson, UpdatePerson, DeletePerson = Value
  }
}
