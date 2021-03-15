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
package scalable.solutions.denver.cors

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.StatusCodes.NoContent
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives.{complete, options, respondWithHeaders, _}
import akka.http.scaladsl.server.Route

trait Cors {

  private val responseHeaders = List(
    `Access-Control-Allow-Origin`.*,
    `Access-Control-Allow-Credentials`(true),
    `Access-Control-Allow-Headers`("Authorization", "Content-Type", "X-Requested-With"),
    `Access-Control-Allow-Methods`(GET, POST, PUT, DELETE, OPTIONS)
  )

  /** CORS preflight request */
  private val preFlight: Route = options {
    respondWithHeaders(responseHeaders) {
      complete(NoContent)
    }
  }

  protected def cors(enabled: Boolean)(inner: => Route): Route =
    if (enabled) {
      respondWithHeaders(`Access-Control-Allow-Origin`.*) {
        preFlight ~ inner
      }
    } else {
      inner
    }
}