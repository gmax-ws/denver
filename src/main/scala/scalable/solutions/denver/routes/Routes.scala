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

import akka.actor.typed.ActorRef
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import scalable.solutions.denver.actors.WebServer.Command
import scalable.solutions.denver.cors.Cors.cors
import scalable.solutions.denver.handlers.Handlers._
import scalable.solutions.denver.services.PersonServices
import scalable.solutions.oauth.KeycloakIntegration._

sealed trait WellKnownRoutes {

  val wellKnown: Route = pathPrefix(".well-known") {
    get {
      path("ready") {
        complete("I'm here!")
      } ~ path("token") {
        parameters("secret") { secretParam =>
          if (secretParam == secret) complete(getToken.body) else complete(BadRequest, "???")
        }
      } ~ path("version") {
        complete(
          s"""Copyright ${'\u00A9'} 2021 Scalable Solutions, all rights reserved.
             |
             |version: 1.0.0
             |date: Monday, March 15, 2021
             |author: Marius Gligor
             |contact: <marius.gligor@gmail.com>
             |""".stripMargin
        )
      }
    }
  }
}

trait DenverRoutes {
  val routes: Route
}

case class Routes(server: ActorRef[Command]) extends DenverRoutes with WellKnownRoutes {
  // Person services and routes
  private val services = PersonServices(server)
  private val personRoutes = PersonRoutes(services)

  val routes: Route = handleExceptions(exceptionHandler) {
    handleRejections(rejectionHandler) {
      cors {
        wellKnown ~ personRoutes.routes
      }
    }
  }
}
