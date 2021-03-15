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
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.typesafe.config.ConfigFactory

import scala.jdk.CollectionConverters._

object Cors {

  private val cfg = ConfigFactory.load.getConfig("akka.http.cors")
  private val allowOrigin = cfg.getString("allowOrigin")
  private val allowCredentials = cfg.getBoolean("allowCredentials")
  private val allowHeaders = cfg.getStringList("allowHeaders")
  private val allowMethods = cfg.getStringList("allowMethods")
  private val exposeHeaders = cfg.getStringList("exposeHeaders")
  private val maxAge = cfg.getInt("maxAge")

  private val accessControlAllowOrigin = configAccessControlAllowOrigin(allowOrigin)
  private val accessControlAllowCredentials = `Access-Control-Allow-Credentials`(allowCredentials)
  private val accessControlAllowHeaders = `Access-Control-Allow-Headers`(allowHeaders.asScala.toList)
  private val accessControlAllowMethods = `Access-Control-Allow-Methods`(allowMethods.asScala.toList.map(toHttpMethod))
  private val accessControlMaxAge = `Access-Control-Max-Age`(maxAge)
  private val accessControlExposeHeaders = `Access-Control-Expose-Headers`(exposeHeaders.asScala.toList)

  private val corsHeaders = List(
    accessControlAllowOrigin,
    accessControlAllowCredentials,
    accessControlAllowHeaders,
    accessControlAllowMethods,
    accessControlMaxAge,
    accessControlExposeHeaders
  )

  private def toHttpMethod(method: String) =
    method match {
      case "CONNECT" => CONNECT
      case "DELETE" => DELETE
      case "GET" => GET
      case "HEAD" => HEAD
      case "OPTIONS" => OPTIONS
      case "PATCH" => PATCH
      case "POST" => POST
      case "PUT" => PUT
      case "TRACE" => TRACE
    }

  private def configAccessControlAllowOrigin(allowOrigin: String) =
    allowOrigin match {
      case "*" => `Access-Control-Allow-Origin`.*
      case "null" => `Access-Control-Allow-Origin`.`null`
      case _ => `Access-Control-Allow-Origin`(HttpOrigin(allowOrigin))
    }

  def cors(inner: => Route): Route =
    options { // preflight
      respondWithHeaders(corsHeaders) {
        complete(NoContent)
      }
    } ~ respondWithHeaders(accessControlAllowOrigin) {
      inner
    }
}