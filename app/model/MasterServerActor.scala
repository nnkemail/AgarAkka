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
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import scala.concurrent.duration._

class MasterServerActor(userDao: UserDAO) extends Actor {
  var rooms = HashMap.empty[Int, RoomDescription]
  var mapChatParticipants = HashSet.empty[ActorRef]
  var mapChatLoggedParticipants = HashMap.empty[UUID,ActorRef]
  var loggedPlayersPlaying = HashMap.empty[UUID, Int]
  val serverActor = context.actorSelection("/user/ServerActor")
  implicit val timeout = Timeout(5 seconds)
  
  for (defaultRoom <- settings.defaultRooms) {
    val roomID = util.nextSysId()
    var resp: Future[AddNewServerRoomResponse] = ask(serverActor, AddNewServerRoom(roomID)).mapTo[AddNewServerRoomResponse]
    
    resp map { addNewServerRoomResponse => addNewServerRoomResponse.roomID map {roomID =>
      self ! AddedNewServerRoom(roomID, RoomDescription(defaultRoom.title, defaultRoom.lat, defaultRoom.lng, serverActor, "ws://eatme.io:90/socket/game"))
      }
    }
  }
  
  def notifyFriendsAboutNewRoom(userID: UUID, roomID: Option[Int]) = {
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
	  
	  userDao.getObservators(userID) map {
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
  
  def getUserRoom(userID: UUID): Option[Int] = {
    mapChatLoggedParticipants.get(userID) match {
      case None => loggedPlayersPlaying.get(userID)
      case actorAddress => Some(0)    
    }
  }
  
  def getUsersRooms(users: List[UUID]): Future[HashMap[UUID, Option[Int]]] = {
    var usersRooms = HashMap.empty[UUID, Option[Int]]
    Future {
      for (userID <- users) {
        usersRooms += userID -> getUserRoom(userID)
      } 
    } map {_ => usersRooms }
  }
    
  def receive = {       
    case JoinChatMap(userIDOption: Option[UUID]) => {    
      //sender ! SpawnData(util.nextSysId(), util.getRandomPosition(), worldGrid, worldActor)
      //context.watch(sender)
      userIDOption match { 
        case Some(userID) => {
          mapChatLoggedParticipants += userID -> sender 
          notifyFriendsAboutNewRoom(userID, Some(0))
          
          userDao.find(userID) map { user => user match {
            case Some(user) => LoggedUserInfo(user.fullName getOrElse "", user.avatarURL getOrElse "", user.score)
            case None => ;
            } 
          } pipeTo sender
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
      
    case UserJoinedGame(userID: String, roomID: Int) =>
      loggedPlayersPlaying += UUID.fromString(userID) -> roomID
      notifyFriendsAboutNewRoom(UUID.fromString(userID), Some(roomID))
     
    case UserLeftGame(userID: String) =>
      loggedPlayersPlaying -= UUID.fromString(userID)
      notifyFriendsAboutNewRoom(UUID.fromString(userID), None)
                       
    //case Terminated(terminatedActorRef) =>
    //  mapChatParticipants = mapChatParticipants - terminatedActorRef;
    //  println(mapChatParticipants);
      
    case AddNewRoom(title: String, lat: Double, lng: Double) => 
      val roomID = util.nextSysId()
      //var roomActor = context.actorOf(RoomActor.props(roomId))
      var resp: Future[AddNewServerRoomResponse] = ask(serverActor, AddNewServerRoom(roomID)).mapTo[AddNewServerRoomResponse]
    
      resp map { addNewServerRoomResponse => addNewServerRoomResponse.roomID map {roomID =>
        self ! AddedNewServerRoom(roomID, RoomDescription(title, lat, lng, serverActor, "ws://eatme.io:90/socket/game"))    
      }
    }
      
    case AddedNewServerRoom(roomID, roomDsc) =>
       rooms += ((roomID, roomDsc))
       mapChatParticipants.foreach(_ ! RoomPacket(roomID, roomDsc.title, roomDsc.lat, roomDsc.lng))
       mapChatLoggedParticipants.values.foreach(_ ! RoomPacket(roomID, roomDsc.title, roomDsc.lat, roomDsc.lng))  
                   
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
      
    case SaveMyScore(score, uID) => 
      userDao.saveScore(score, uID)
      
  }  
}

object MasterServerActor {
  def props(userDao: UserDAO) = Props(new MasterServerActor(userDao))
}

