package model

import play.libs.Akka
import akka.actor._
import scala.collection.mutable.HashMap
import model.Util.util
import model.Util.util.settings
import model.Util.util._
import scala.collection.mutable.HashSet
import model.Daos.UserDAO
import com.mohiva.play.silhouette.api.LoginInfo
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.Future
import scala.collection.mutable.ListBuffer
import java.util.UUID

class MasterServerActor(userDao: UserDAO) extends Actor {
  var rooms = HashMap.empty[Int, RoomDescription]
  var mapChatParticipants = HashSet.empty[ActorRef]
  var mapChatLoggedParticipants = HashMap.empty[UUID,ActorRef]
  
  //SERVER
  var serverActor = context.actorOf(ServerActor.props())
  
  for (defaultRoom <- settings.defaultRooms) {
    val roomId = util.nextSysId()
    defaultRoom.roomActor = context.actorOf(RoomActor.props(roomId))
    rooms += ((roomId, defaultRoom))
  }
  
  def notifyFriendsAboutNewRoom(userID: UUID, roomID: Option[Long]) = {
	  userDao.getFriends(userID) map {
		  friendsOptionList =>
		  for (friendOption <- friendsOptionList) {
			  friendOption match {
			  case Some(user) => mapChatLoggedParticipants.get(user.userID) map {
				  actorAddres => actorAddres ! NotifyFriendAboutMyNewRoom(userID, roomID) 
			  }
			  case None => ;
			  }
		  }
	  }
  }
  
  def getUserRoom(userID: UUID): Option[Long] = {
    mapChatLoggedParticipants.get(userID) match {
      case None => None 
      case actorAddress => Some(0)    
    }
  }
  
  def getUsersRooms(users: List[UUID]): Future[HashMap[UUID, Option[Long]]] = {
    var usersRooms = HashMap.empty[UUID, Option[Long]]
    Future {
      for (userID <- users) {
        usersRooms += userID -> getUserRoom(userID)
      } 
    } map {_ => usersRooms }
  }
    
  def receive = {
    //MOVE TO SERVER
    case JoinRoom(idRoom: Int) => 
      println("przyszlo join Room");
      val roomDscOption = rooms.get(idRoom)
      
      roomDscOption match {
        case Some(roomDsc: RoomDescription) => roomDsc.roomActor forward Join
        case None =>
      }  
      
    case JoinChatMap(userIDOption: Option[UUID]) => {    
      //sender ! SpawnData(util.nextSysId(), util.getRandomPosition(), worldGrid, worldActor)
      //context.watch(sender)
      userIDOption match { 
        case Some(userID) => {
          mapChatLoggedParticipants += userID -> sender 
          notifyFriendsAboutNewRoom(userID, Some(0))
        }
        
        case None => mapChatParticipants += sender;
      }
      println("Przyszlo JoinChatMap");
    }
    
    case LeaveChatMap(userIDOption: Option[UUID]) => 
      userIDOption match { 
        case Some(userID) => 
          mapChatLoggedParticipants -= userID
          notifyFriendsAboutNewRoom(userID, None)
        case None => mapChatParticipants = mapChatParticipants - sender;
      }
         
    //case Terminated(terminatedActorRef) =>
    //  mapChatParticipants = mapChatParticipants - terminatedActorRef;
    //  println(mapChatParticipants);
      
    case AddNewRoom(title: String, lat: Double, lng: Double) => 
      val roomId = util.nextSysId()
      //var roomActor = context.actorOf(RoomActor.props(roomId))
      
      var newRoom = RoomDescription(title, lat, lng, serverActor, "ws://localhost:80/socket/game")
      rooms += ((roomId, newRoom))
      mapChatParticipants.foreach(_ ! RoomPacket(roomId, title, lat, lng))
      mapChatLoggedParticipants.values.foreach(_ ! RoomPacket(roomId, title, lat, lng))  
       
    case GiveServer(idRoom: Int) =>
      println("przyszlo give server");
      if (idRoom == 0) {
        var room = rooms.last
        var idRoom = room._1
        sender ! ("ws://localhost:80/socket/game", idRoom)
      } else {
        val roomDscOption = rooms.get(idRoom)
      
        roomDscOption match {
          case Some(roomDsc: RoomDescription) => sender ! (roomDsc.serverAddress, idRoom) 
          case None =>
        }
      } 
      
    case GiveRooms() =>
      var roomPacketList = List.empty[RoomPacket];
      for ((idRoom, roomDsc) <- rooms)
        roomPacketList = RoomPacket(idRoom, roomDsc.title, roomDsc.lat, roomDsc.lng) :: roomPacketList
      println(roomPacketList)
      println("GIVE_ROOMS SENDER: " + sender)
      sender ! roomPacketList
      
    case AddFacebookFriend(myFacebookID, friendFacebookID) => 
      //userDao.getFriends(LoginInfo("facebook", myFacebookID)) map {friends => friends map {friend => println(friend)}}  
      val _sender = sender
      var addFriendFuture = userDao.addFriend(LoginInfo("facebook", myFacebookID), LoginInfo("facebook", friendFacebookID));
      
      addFriendFuture onSuccess {
        case Some(friend) =>
           var roomIDOption = getUserRoom(friend.userID)
          _sender ! AddedFriend(friend, roomIDOption)
        case None => ;
      }
      
    case GetFriends(userLoginInfo: LoginInfo) =>
      val _sender = sender
      var listOfFriends = ListBuffer.empty[User]
      
      userDao.getFriends(userLoginInfo) map { friendsOptionList =>
        for (friendOption <- friendsOptionList) {
          friendOption match {
            case Some(user) => listOfFriends += user
            case None => ;
          }
        };
        _sender ! listOfFriends.toList
      }
      
    case GetUsersRooms(users: List[UUID]) =>
      val _sender = sender
      getUsersRooms(users) map { usersRooms => _sender ! usersRooms }   
  }  
}

object MasterServerActor {
  def props(userDao: UserDAO) = Props(new MasterServerActor(userDao))
}

