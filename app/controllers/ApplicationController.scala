package controllers

import play.api._
import play.api.mvc._
import play.api.Play.current
import model._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.libs.Akka
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import model.Util.util._
import model.RoomPacket
import scala.concurrent.Future
import scala.collection.mutable.ListBuffer
import java.util.UUID
import scala.collection.mutable.HashMap

import javax.inject.Inject

import com.mohiva.play.silhouette.api.{ Environment, LogoutEvent, Silhouette }
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.SocialProviderRegistry
import com.mohiva.play.silhouette.impl.daos.DelegableAuthInfoDAO
import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import com.mohiva.play.silhouette.api.LoginInfo
import scala.collection.JavaConversions._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import model.Daos.UserDAO

//import forms._
import model.User
import play.api.i18n.MessagesApi

import scala.concurrent.Future
import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.types.{User => UserFb}

class ApplicationController @Inject() (
  val messagesApi: MessagesApi,
  val userDao: UserDAO,
  val env: Environment[User, CookieAuthenticator],
  socialProviderRegistry: SocialProviderRegistry,
  authInfoRepository: AuthInfoRepository,
  val o2Dao: DelegableAuthInfoDAO[OAuth2Info])
  extends Silhouette[User, CookieAuthenticator] {
  
  val system = ActorSystem("mySystem")
  val masterServer = system.actorOf (MasterServerActor.props(userDao)) 

  //def index = Action { implicit request =>
  //  Ok(views.html.index())
  //}

  
def roomsFacebook = SecuredAction.async { implicit request =>
    var roomsFuture = getRooms();
    var friendsFuture = getFriends(request.identity.loginInfo);
    var friendsInfoToSend = List.empty[FriendPacket]
       
    o2Dao.find(request.identity.loginInfo) flatMap { 
      resultInfo => resultInfo match  {
        case Some(authInfo) => {
          var accessToken = authInfo.accessToken
          
          var roomsFriendsFuture = for {
            rooms <- roomsFuture
            friends <- friendsFuture
          } yield (rooms, friends)
          
          roomsFriendsFuture flatMap { roomsFriends =>
            var rooms = roomsFriends._1
            var friends = roomsFriends._2
            
            var friendsRoomsFuture = getUsersRooms(friends map {friend => friend.userID})
            friendsRoomsFuture map { friendsRooms => 
            
              for (friend <- friends) {
                var roomID: String = friendsRooms.get(friend.userID) match {
                  case Some(optionRoom) => (optionRoom getOrElse "").toString
                  case None => ""
                }
            
                 friendsInfoToSend = FriendPacket(friend.userID.toString, roomID, 
                    friend.fullName getOrElse "", friend.avatarURL getOrElse "") :: friendsInfoToSend
              }
              
              Ok(views.html.roomsFacebook(rooms, friendsInfoToSend, accessToken, 
                 request.identity.loginInfo.providerKey.toString()))
            }  
          }
        }
              
        case None => Future.successful(NotFound(<h1>Page not found</h1>))
      }
    }
  }
  
  def index = UserAwareAction.async { implicit request =>
    request.identity match {
      case Some(user) => Future.successful(Redirect(routes.ApplicationController.roomsFacebook()))
      case None => Future.successful(Ok(views.html.index()))
    }
  }
  
  def signIn = Action.async { implicit request =>
     Future.successful(Ok(views.html.signIn(socialProviderRegistry)))
  }
  
  def game(idRoom: Int) = Action.async { implicit request =>
    println("przyszlo game idRoom: " + idRoom); 
    implicit val timeout = Timeout(5 seconds)
    var resp = ask(masterServer, GiveServer(idRoom)).mapTo[(String, Int)] 
    resp map ((res) => {println(res._1); Ok(views.html.game(res._1, res._2))})
  }
  
  def roomsMap() = Action.async { implicit request =>
    var resp = getRooms();
    resp map (roomList => Ok(views.html.roomsMap(roomList)))
  }
    
  //MOVE TO SERVER
  def socketGame = WebSocket.acceptWithActor[JsValue, JsValue] {request => out =>
    println("socket game");
    PlayerActor.props(out, masterServer)
  }
    
  def socketChatMap = WebSocket.acceptWithActor[JsValue, JsValue] {request => out =>
    ChatMapActor.props(out, masterServer)
  }
  
  def socketLoggedChatMap = WebSocket.tryAcceptWithActor[JsValue, JsValue] { request => 
    implicit val req = Request(request, AnyContentAsEmpty)
    SecuredRequestHandler { securedRequest =>
      Future.successful(HandlerResult(Ok, Some(securedRequest.identity)))
    }.map {
      case HandlerResult(r, Some(user)) => Right(out => LoggedChatMapActor.props(out, masterServer, user))
      case HandlerResult(r, None) => Left(r)
    }
  }
  
  //def socketLoggedChatMap = WebSocket.acceptWithActor[JsValue, JsValue] { request => out =>
  //  LoggedChatMapActor.props(out, masterServer);
  //}
  
  def getRooms(): Future[List[RoomPacket]] = {
    implicit val timeout = Timeout(5 seconds)
    var resp = ask(masterServer, GiveRooms()).mapTo[List[RoomPacket]] 
    resp
  }
  
  def getFriends(userLoginInfo: LoginInfo): Future[List[User]] = {
    implicit val timeout = Timeout(5 seconds)
    var resp = ask(masterServer, GetFriends(userLoginInfo)).mapTo[List[User]]
    resp
  }
  
  def getUsersRooms(users: List[UUID]): Future[HashMap[UUID, Option[Long]]] = {
    implicit val timeout = Timeout(5 seconds)
    var resp = ask(masterServer, GetUsersRooms(users)).mapTo[HashMap[UUID, Option[Long]]]
    resp
  }
}
