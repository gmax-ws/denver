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
package scalable.solutions.oauth

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.StatusCodes.{Forbidden, Unauthorized}
import akka.http.scaladsl.server.Directive0
import akka.http.scaladsl.server.Directives.{complete, headerValueByName, pass}
import com.jayway.jsonpath.{Configuration, JsonPath}
import com.typesafe.config.{Config, ConfigFactory}
import scalable.solutions.denver.json.PersonJsonProtocol._
import scalable.solutions.denver.json.SimpleResponse

import java.net.URI
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.util.Base64
import scala.jdk.CollectionConverters.ListHasAsScala

case class AuthInfo(active: Boolean, typ: String, clientId: String, authorization: List[String])

object KeycloackIntegration {
  private val config: Config = ConfigFactory.load
  private val tokenRegex = "^(?i)Bearer (.*)(?-i)".r

  private val cfg = config.getConfig("keycloak")
  private val host = cfg.getString("host")
  private val port = cfg.getInt("port")
  private val realm = cfg.getString("realm")
  private val client = cfg.getString("client")
  private val secret = cfg.getString("secret")
  private val basic = Base64.getEncoder.encodeToString(s"$client:$secret".getBytes)

  private val tokenUri = s"http://$host:$port/auth/realms/$realm/protocol/openid-connect/token"
  private val validUri = s"$tokenUri/introspect"

  def getSecret: String = secret

  def getToken: HttpResponse[String] = {
    val request = HttpRequest.newBuilder
      .uri(URI.create(tokenUri))
      .header("Content-Type", "application/x-www-form-urlencoded")
      .POST(BodyPublishers.ofString(s"grant_type=client_credentials&client_id=$client&client_secret=$secret"))
      .build
    HttpClient.newHttpClient.send(request, BodyHandlers.ofString())
  }

  private def validateToken(token: String): (Int, String) = {
    val request = HttpRequest.newBuilder
      .uri(URI.create(validUri))
      .header("Content-Type", "application/x-www-form-urlencoded")
      .header("Authorization", s"Basic $basic")
      .POST(BodyPublishers.ofString(s"token=$token"))
      .build
    val response = HttpClient.newHttpClient.send(request, BodyHandlers.ofString())
    (response.statusCode(), response.body)
  }

  private def parseToken(token: String): AuthInfo = {
    val document = Configuration.defaultConfiguration.jsonProvider.parse(token)

    val active: Boolean = JsonPath.read(document, "$.active")
    if (active) {
      val typ: String = JsonPath.read(document, "$.typ")
      val clientId: String = JsonPath.read(document, "$.clientId")
      val roles: java.util.List[String] = JsonPath.read(document, "$.realm_access.roles")

      AuthInfo(
        active,
        typ,
        clientId,
        roles.asScala.toList.map(_.toUpperCase).filter(_.startsWith("ROLE_"))
      )
    } else {
      AuthInfo(active, "?", "?", Nil)
    }
  }

  private def tokenValidation(authorization: String): Either[String, String] = authorization match {
    case tokenRegex(jwtToken) => Right(jwtToken.trim)
    case _ => Left("This is not a `Bearer` token or has an invalid format")
  }

  def authorize(authorization: String, roles: Seq[String]): Either[(StatusCodes.ClientError, String), String] = {
    tokenValidation(authorization.trim) match {
      case Right(jwtToken) =>
        val (statusCode, body) = validateToken(jwtToken)
        if (statusCode == 200) {
          val authInfo = parseToken(body)
          if (!authInfo.active)
            Left(Unauthorized, "Your token is expired")
          else if (authInfo.clientId != client)
            Left(Unauthorized, "Your token contains an invalid client ID")
          else if (authInfo.authorization.contains("ROLE_ADMIN"))
            Right("By default, ROLE_ADMIN has all rights")
          else {
            val matchRoles = roles.filter(authInfo.authorization.contains(_))
            if (matchRoles.isEmpty)
              Left(Forbidden, "Your assigned roles doesn't allow to do this operation")
            else
              Right("Roles are OK")
          }
        } else {
          Left(Unauthorized, "Your token has been invalided by the server")
        }
      case Left(m) => Left(Unauthorized, m)
    }
  }

  def withAuthorizationRoles(roles: String*): Directive0 =
    headerValueByName("Authorization").flatMap { token =>
      authorize(token, roles).fold(left => complete(left._1, SimpleResponse(left._2)), _ => pass)
    }
}