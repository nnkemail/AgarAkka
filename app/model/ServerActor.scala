package model

import play.libs.Akka
import akka.actor._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.duration._
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.HashSet
import model.Util.util
import model.Util.Position
import model.Entities._
import model.Util.Settings
import model.Util.util.settings
import scala.collection.mutable.HashMap

class ServerActor() extends Actor {
  val system = ActorSystem("mySystem")
  var rooms = HashMap.empty[Int, ActorRef]
  
  def receive = {
    case Join =>
      
      
    case AddNewServerRoom (roomID: Int) =>
      if (!rooms.isDefinedAt(roomID)) {
        rooms += roomID -> context.actorOf(RoomActor.props(roomID))
        sender ! Some(roomID)
      } else 
        sender ! None
  }
}

object ServerActor {
  def props() = Props(new ServerActor())
}

