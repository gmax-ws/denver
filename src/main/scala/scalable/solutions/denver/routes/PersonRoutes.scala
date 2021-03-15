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
package scalable.solutions.denver.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.StatusCodes.BadRequest
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import scalable.solutions.denver.cors.Cors
import scalable.solutions.denver.handlers.Handlers._
import scalable.solutions.denver.handlers.PersonRejection
import scalable.solutions.denver.json.PersonJsonProtocol._
import scalable.solutions.denver.json.SimpleResponse
import scalable.solutions.denver.repo.Person
import scalable.solutions.denver.services.PersonServices
import scalable.solutions.oauth.KeycloackIntegration.{getSecret, getToken, withAuthorizationRoles}

import scala.util.{Failure, Success}

sealed trait WellKnownRoutes {

  val wellKnown: Route = pathPrefix(".well-known") {
    get {
      path("ready") {
        complete("I'm ready!")
      } ~ path("token") {
        parameters("secret") { secretParam =>
          if (secretParam == getSecret) complete(getToken.body) else complete(BadRequest, "???")
        }
      } ~ path("version") {
        val version =
          s"""Copyright ${'\u00A9'} 2021 Scalable Solutions, all rights reserved.
             |
             |version: 1.0.0
             |date: Monday, March 15, 2021
             |author: Marius Gligor
             |contact: <marius.gligor@gmail.com>
             |""".stripMargin
        complete(version)
      }
    }
  }
}

case class PersonRoutes(service: PersonServices) extends WellKnownRoutes with Cors {

  val personsRoute: Route = path("persons") {

    get {
      withAuthorizationRoles("ROLE_USER") {
        onComplete(service.getPersons) {
          case Success(persons) =>
            complete(persons)
          case Failure(th) =>
            reject(PersonRejection(th.getMessage))
        }
      }
    }
  }

  val personRoute: Route = path("person" / IntNumber) { id =>
    get {
      withAuthorizationRoles("ROLE_USER") {
        onComplete(service.findPerson(id)) {
          case Success(maybePerson) =>
            maybePerson match {
              case Some(person) =>
                complete(person)
              case None =>
                complete(StatusCodes.NotFound,
                  SimpleResponse(s"Person having id=$id was not found"))
            }
          case Failure(th) =>
            reject(PersonRejection(th.getMessage))
        }
      }
    }
  }

  val createPersonRoute: Route = path("person") {
    post {
      withAuthorizationRoles("ROLE_WRITE", "ROLE_TEMPLATE") {
        entity(as[Person]) { person =>
          onComplete(service.createPerson(person)) {
            case Success(insertOneResult) =>
              val status = if (insertOneResult.wasAcknowledged) StatusCodes.Created else StatusCodes.BadRequest
              complete(status, person)
            case Failure(th) =>
              reject(PersonRejection(th.getMessage))
          }
        }
      }
    }
  }

  val updatePersonRoute: Route = path("person") {
    put {
      withAuthorizationRoles("ROLE_WRITE", "ROLE_TEMPLATE") {
        entity(as[Person]) { person =>
          onComplete(service.updatePerson(person)) {
            case Success(updateResult) =>
              val status = if (updateResult.wasAcknowledged) StatusCodes.OK else StatusCodes.BadRequest
              complete(status, person)
            case Failure(th) =>
              reject(PersonRejection(th.getMessage))
          }
        }
      }
    }
  }

  val deletePersonRoute: Route = path("person" / IntNumber) { id =>
    delete {
      withAuthorizationRoles("ROLE_TEMPLATE") {
        onComplete(service.deletePerson(id)) {
          case Success(deleteResult) =>
            complete(SimpleResponse(s"Number of deleted persons: ${deleteResult.getDeletedCount}"))
          case Failure(th) =>
            reject(PersonRejection(th.getMessage))
        }
      }
    }
  }

  def routes(corsEnabled: Boolean): Route = handleExceptions(exceptionHandler) {
    handleRejections(rejectionHandler) {
      pathPrefix("api") {
        cors(corsEnabled) {
          personsRoute ~ personRoute ~ createPersonRoute ~ updatePersonRoute ~ deletePersonRoute
        }
      } ~ wellKnown
    }
  }
}
