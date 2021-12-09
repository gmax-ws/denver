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

import akka.http.scaladsl.model.StatusCode
import akka.http.scaladsl.model.StatusCodes._
import com.typesafe.config._
import spray.json._
import spray.json.DefaultJsonProtocol._

import java.net.URI
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.net.http._
import java.util.Base64
import scala.util.matching.Regex

object KeycloakIntegration {
  case class AuthInfo(active: Boolean, typ: String, clientId: String, authorization: List[String])

  case class Vt(status: StatusCode, message: String)

  val tokenRegex: Regex = "^(?i)Bearer (.*)(?-i)".r

  val config: Config = ConfigFactory.load
  val cfg: Config = config.getConfig("keycloak")
  val host: String = cfg.getString("host")
  val port: Int = cfg.getInt("port")
  val realm: String = cfg.getString("realm")
  val client: String = cfg.getString("client")
  val secret: String = cfg.getString("secret")
  val basic: String =
    Base64.getEncoder.encodeToString(s"$client:$secret".getBytes)

  val tokenUri =
    s"http://$host:$port/auth/realms/$realm/protocol/openid-connect/token"
  val validUri = s"$tokenUri/introspect"
  val mime = "application/x-www-form-urlencoded"

  def getToken: HttpResponse[String] = {
    val request = HttpRequest.newBuilder
      .uri(URI.create(tokenUri))
      .header("Content-Type", mime)
      .POST(BodyPublishers.ofString(s"grant_type=client_credentials&client_id=$client&client_secret=$secret"))
      .build
    HttpClient.newHttpClient.send(request, BodyHandlers.ofString())
  }

  private def validateToken(token: String): (Int, String) = {
    val request = HttpRequest.newBuilder
      .uri(URI.create(validUri))
      .header("Content-Type", mime)
      .header("Authorization", s"Basic $basic")
      .POST(BodyPublishers.ofString(s"token=$token"))
      .build
    val response = HttpClient.newHttpClient.send(request, BodyHandlers.ofString())
    (response.statusCode(), response.body)
  }

  private def parseToken(token: String): AuthInfo = {
    val data = token.parseJson.asJsObject().fields
    val active = data.getOrElse("active", JsFalse).convertTo[Boolean]
    val typ = data.getOrElse("typ", JsString("?")).convertTo[String]
    val clientId = data.getOrElse("client_id", JsString("?")).convertTo[String]
    val realm = data.getOrElse("realm_access", JsNull)
    val roles = if (realm != JsNull) realm.asJsObject.fields.getOrElse("roles", JsArray.empty).convertTo[Array[String]] else Array.empty[String]
    AuthInfo(active, typ, clientId, roles.toList.map(_.toUpperCase))
  }

  private def tokenValidation(authorization: String): Either[String, String] = authorization match {
    case tokenRegex(jwtToken) => Right(jwtToken.trim)
    case _ => Left("This is not a `Bearer` token or has an invalid format")
  }

  def authorize(authorization: String, roles: Seq[String]): Vt =
    tokenValidation(authorization.trim).fold(
      error => Vt(Unauthorized, error),
      token => {
        val (statusCode, body) = validateToken(token)
        statusCode match {
          case OK.intValue =>
            val authInfo = parseToken(body)
            if (!authInfo.active)
              Vt(Unauthorized, "Your token is expired")
            else if (authInfo.clientId != client)
              Vt(Unauthorized, "Your token contains an invalid client ID")
            else if (authInfo.authorization.contains("ROLE_ADMIN"))
              Vt(OK, "By default, ROLE_ADMIN has all rights")
            else if (!roles.exists(authInfo.authorization.contains(_)))
              Vt(Forbidden, "Your assigned roles doesn't allow to do this operation")
            else
              Vt(OK, "Roles are OK")
          case _ =>
            Vt(Unauthorized, "Your token has been invalided by the server")
        }
      }
    )
}
