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

import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Directives._
import scalable.solutions.denver.model.JsonProtocol._
import scalable.solutions.denver.model.SimpleResponse
import scalable.solutions.oauth.KeycloakIntegration.authorize

object Directives {
  def withAuthorizationRoles(roles: String*): Directive0 =
    headerValueByName("Authorization").flatMap { token =>
      val vt = authorize(token, roles)
      vt.status match {
        case OK => pass
        case _ => complete(vt.status, SimpleResponse(vt.message))
      }
    }

}
