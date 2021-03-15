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
package scalable.solutions.denver.model

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import spray.json._

// Person model
final case class Person(_id: Int, name: String, age: Int, address: Option[Address] = None)

final case class Address(street: String, no: Int, zip: Int)

// Http model
final case class SimpleResponse(message: String, timestamp: String = ISODateTimeFormat.dateTime().print(new DateTime()))

object JsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val addressFormat: RootJsonFormat[Address] = jsonFormat3(Address)
  implicit val personFormat: RootJsonFormat[Person] = jsonFormat4(Person)
  implicit val simpleResponse: RootJsonFormat[SimpleResponse] = jsonFormat2(SimpleResponse)
}

