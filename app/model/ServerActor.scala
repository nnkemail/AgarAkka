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
  val masterServerActor = context.actorSelection("/user/MasterServerActor")
  var rooms = HashMap.empty[Int, ActorRef]
  
  def receive = {
    case JoinRoom(roomID: Int, userID: Option[String]) =>
      println("przyszlo join Room")
      val roomActorOption = rooms.get(roomID)
      
      roomActorOption match {
        case Some(roomActor) => roomActor forward Join(userID)
        case None =>;  //TODO
      }  
          
    case AddNewServerRoom (roomID: Int) =>
      if (!rooms.isDefinedAt(roomID)) {
        rooms += roomID -> context.actorOf(RoomActor.props(roomID))
        sender ! AddNewServerRoomResponse(Some(roomID))
      } else 
        sender ! AddNewServerRoomResponse(None)

    case UserJoinedGame(userID, roomID) =>
      masterServerActor ! UserJoinedGame(userID, roomID)
      
    case UserLeftGame(userID) =>
      masterServerActor ! UserLeftGame(userID)
      
    case SaveMyScore(score, uID) =>
      masterServerActor ! SaveMyScore(score, uID)
  }
}

object ServerActor {
  def props() = Props(new ServerActor())
}

