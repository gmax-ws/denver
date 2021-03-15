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
package scalable.solutions.denver.actors

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import scalable.solutions.denver.actors.WebServer._

object Guardian {

  sealed trait GuardianCommand

  final case object Start extends GuardianCommand

  final case object Stop extends GuardianCommand

  def apply(): Behavior[GuardianCommand] = {
    Behaviors.setup[GuardianCommand] { context =>
      val serverBehavior = WebServer()
      Behaviors.supervise(serverBehavior).
        onFailure[Exception](SupervisorStrategy.restart)
      val webServer: ActorRef[WebServer.Command] =
        context.spawn(serverBehavior, "http-server")

      Behaviors.receiveMessage {
        case Start =>
          webServer ! Startup
          Behaviors.ignore
        case Stop =>
          webServer ! Shutdown
          Behaviors.stopped
      }
    }
  }
}
