package model

import play.libs.Akka
import akka.actor._
import play.api.libs.json._

object ChatMapActor {
  def props(out: ActorRef, masterServer: ActorRef) = Props(new ChatMapActor(out, masterServer))
}

class ChatMapActor(val out: ActorRef, var masterServer: ActorRef) extends Actor {
  override def preStart() = {
    masterServer ! JoinChatMap
  }
  
  def receive = {
   case msg: JsValue => {
       val t = (msg \ "type").as[String]
       
       t match {
         case "AddNewRoom" => { 
           println("Przyszlo add new room");
           var lat = (msg \ "lat").as[Double];
           var lng = (msg \ "lng").as[Double];
           var title = (msg \ "title").as[String];
           masterServer ! AddNewRoom(title, lat, lng) 
         }
       }
   }
   
   case RoomPacket(id: Int, title: String, lat: Double, lng: Double) =>
     implicit val RoomPacketFormat = Json.format[RoomPacket]
     out ! Json.obj("type" -> "AddedNewRoom", "id" -> id, "title" -> title, "lat" -> lat, "lng" -> lng)
     println("przyszedl room packet");
  }
}