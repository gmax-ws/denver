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
package scalable.solutions.denver.json

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import scalable.solutions.denver.repo.{Address, Person}
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

final case class SimpleResponse(message: String, timestamp: String = ISODateTimeFormat.dateTime().print(new DateTime()))

object PersonJsonProtocol extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val addressFormat: RootJsonFormat[Address] = jsonFormat3(Address)
  implicit val personFormat: RootJsonFormat[Person] = jsonFormat4(Person)
  implicit val simpleResponse: RootJsonFormat[SimpleResponse] = jsonFormat2(SimpleResponse)
}
