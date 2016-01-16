package model

import play.libs.Akka
import akka.actor._
import play.api.libs.json._

object LoggedChatMapActor {
  def props(out: ActorRef, masterServer: ActorRef) = Props(new LoggedChatMapActor(out, masterServer))
}

class LoggedChatMapActor(val out: ActorRef, var masterServer: ActorRef) extends Actor {
  override def preStart() = {
    masterServer ! JoinChatMap
  }
  
  def receive = {
   case msg: JsValue => {
       val t = (msg \ "type").as[String]
       println("Otrzymano " + msg);
       t match {
         case "AddNewRoom" => { 
           println("Przyszlo add new room w logged room");
           var lat = (msg \ "lat").as[Double];
           var lng = (msg \ "lng").as[Double];
           var title = (msg \ "title").as[String];
           masterServer ! AddNewRoom(title, lat, lng) 
         }
         
         case "AddFacebookFriend" => {
           println("Przyszlo AddFacebookFriend");
           var myFacebookID = (msg \ "userFacebookID").as[String];
           var friendFacebookID = (msg \ "friendFacebookID").as[String];
           masterServer ! AddFacebookFriend(myFacebookID, friendFacebookID); 
         }
       }
   }
   
   case RoomPacket(id: Int, title: String, lat: Double, lng: Double) =>
     implicit val RoomPacketFormat = Json.format[RoomPacket]
     out ! Json.obj("type" -> "AddedNewRoom", "id" -> id, "title" -> title, "lat" -> lat, "lng" -> lng)
     println("przyszedl room packet");
     
   case AddedFriend(friend: User) =>
     implicit val FriendPacketFormat = Json.format[FriendPacket]
     var friendPacket = FriendPacket(friend.fullName getOrElse "", friend.avatarURL getOrElse "")
     out ! Json.obj("type" -> "NewFriend", "friend" -> friendPacket)   
  }
}