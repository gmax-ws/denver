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
package scalable.solutions.denver.services

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.AskPattern.Askable
import org.mongodb.scala.result.{DeleteResult, InsertOneResult, UpdateResult}
import scalable.solutions.denver.PersonOp.{DeletePerson, FindPerson, GetPersons, CreatePerson, UpdatePerson}
import scalable.solutions.denver.actors.WebServer.{Command, PersonsResult, QueryPersons}
import scalable.solutions.denver.repo.Person
import scalable.solutions.denver.{PersonQuery, _}

import scala.concurrent.Future

case class PersonServices(server: ActorRef[Command]) {

  def getPersons: Future[Seq[Person]] =
    queryPersons[Seq[Person]](PersonQuery(GetPersons))

  def findPerson(id: Int): Future[Option[Person]] = queryPersons[Option[Person]](PersonQuery(FindPerson, id))

  def createPerson(person: Person): Future[InsertOneResult] =
    queryPersons[InsertOneResult](PersonQuery(CreatePerson, person = Some(person)))

  def updatePerson(person: Person): Future[UpdateResult] =
    queryPersons[UpdateResult](PersonQuery(UpdatePerson, person = Some(person)))

  def deletePerson(id: Int): Future[DeleteResult] =
    queryPersons[DeleteResult](PersonQuery(DeletePerson, id))

  def queryPersons[A](query: PersonQuery): Future[A] =
    for {
      out <- server.ask((ref: ActorRef[PersonsResult[A]]) => QueryPersons(ref, query))
      response <- out.persons
      result <- response.persons
    } yield result
}

