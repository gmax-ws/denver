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
package scalable.solutions.denver.mongo

import com.typesafe.config.Config
import org.bson.codecs.configuration.CodecRegistries._
import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala._
import org.mongodb.scala.bson.codecs.Macros._
import scalable.solutions.denver.model._
import scalable.solutions.denver.repo.PersonRepo
import scalable.solutions.denver.system.executionContext

import scala.reflect.ClassTag
import scala.util.Try

case class MongoConfig(mongoClient: MongoClient, dbName: String, collectionName: String, codecRegistry: CodecRegistry)

case class MongoDaemon[A: ClassTag](cfg: MongoConfig) {

  def queryCollection[R](query: MongoCollection[A] => R): Either[Throwable, R] =
    Try {
      val database: MongoDatabase = cfg.mongoClient.getDatabase(cfg.dbName).withCodecRegistry(cfg.codecRegistry)
      val collection: MongoCollection[A] = database.getCollection(cfg.collectionName)
      query(collection)
    }.toEither
}

sealed trait Mongo {

  def client(cfg: Config): MongoClient = {
    val username = cfg.getString("username")
    val password = cfg.getString("password")
    val authSource = cfg.getString("authSource")
    val host = cfg.getString("host")
    val port = cfg.getInt("port")
    val authMechanism = cfg.getString("authMechanism")
    MongoClient(s"mongodb://$username:$password@$host:$port/?authSource=$authSource&authMechanism=$authMechanism")
  }
}

object PersonMongo extends Mongo {

  private def config(config: Config): MongoConfig = {
    val cfg = config.getConfig("mongo.db")
    val mongoClient = client(cfg)
    val dbName = cfg.getString("person.db")
    val collectionName = cfg.getString("person.table")
    val codecRegistry: CodecRegistry = fromRegistries(
      fromProviders(classOf[Person], classOf[Address]), DEFAULT_CODEC_REGISTRY
    )
    MongoConfig(mongoClient, dbName, collectionName, codecRegistry)
  }

  def repo(cfg: Config): PersonRepo = PersonRepo(MongoDaemon[Person](config(cfg)))
}
