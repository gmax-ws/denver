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
package scalable.solutions.denver.handlers

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import scalable.solutions.denver.model.JsonProtocol._
import scalable.solutions.denver.model._

final case class PersonRejection(msg: String) extends Rejection
final case class AuthorizationRejection(msg: String) extends Rejection

/** Global rejection handler */
trait DenverRejectionHandling {

  implicit val rejectionHandler: RejectionHandler = RejectionHandler.newBuilder()
    .handle { case MissingCookieRejection(cookieName) =>
      complete(BadRequest, SimpleResponse(s"No cookies, $cookieName no service!!!"))
    }
    .handle { case AuthorizationFailedRejection =>
      complete(Forbidden, SimpleResponse("You're out of your depth!"))
    }
    .handle { case PersonRejection(message) =>
      complete(NotFound, SimpleResponse(s"$message"))
    }
    .handle { case ValidationRejection(message, _) =>
      complete(InternalServerError, SimpleResponse(s"That wasn't valid! $message"))
    }
    .handleAll[MethodRejection] { methodRejections =>
      val names = methodRejections.map(_.supported.name)
      complete(MethodNotAllowed, SimpleResponse(s"Can't do that! Supported: ${names mkString " or "}!"))
    }
    .handleNotFound {
      complete(NotFound, SimpleResponse("Not here!"))
    }
    .result()
}

/** Global exception handler */
trait DenverExceptionHandling {
  val exceptionHandler: ExceptionHandler = ExceptionHandler {
    case th: Throwable => complete(InternalServerError, SimpleResponse(th.getMessage))
  }
}

object Handlers extends DenverRejectionHandling with DenverExceptionHandling
