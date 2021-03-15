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
package scalable.solutions.denver
package repo

import org.bson.conversions.Bson
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.result._
import scalable.solutions.denver.model.Person
import scalable.solutions.denver.mongo.MongoDaemon

import scala.concurrent._

sealed trait PersonDSL[F[_]] {
  def findPerson(id: Int): F[Option[Person]]

  def getPersons: F[Seq[Person]]

  def createPerson(person: Person): F[InsertOneResult]

  def deletePerson(id: Int): F[DeleteResult]

  def updatePerson(person: Person): F[UpdateResult]
}

sealed trait MongoRepo

class PersonRepo(mongo: MongoDaemon[Person])(implicit ec: ExecutionContextExecutor)
  extends MongoRepo with PersonDSL[Future] {

  def idEqual(objectId: Int): Bson =
    equal("_id", objectId)

  // equal("_id", new ObjectId(objectId))

  def async[A](result: Either[Throwable, Future[A]]): Future[A] =
    result fold(th => Future.failed(th), identity)

  def getPersons: Future[Seq[Person]] =
    async(mongo.queryCollection { collection =>
      collection.find().toFuture()
    })

  def findPerson(id: Int): Future[Option[Person]] =
    async(mongo.queryCollection { collection =>
      collection.find(idEqual(id)).first().headOption()
    })

  def createPerson(person: Person): Future[InsertOneResult] =
    async(mongo.queryCollection { collection =>
      collection.insertOne(person).toFuture()
    })

  def deletePerson(id: Int): Future[DeleteResult] =
    async(mongo.queryCollection { collection =>
      collection.deleteOne(idEqual(id)).toFuture()
    })

  def updatePerson(person: Person): Future[UpdateResult] =
    async(mongo.queryCollection { collection =>
      collection.replaceOne(idEqual(person._id), person).toFuture()
    })
}

object PersonRepo {
  def apply(mongo: MongoDaemon[Person])(implicit ec: ExecutionContextExecutor): PersonRepo =
    new PersonRepo(mongo)
}