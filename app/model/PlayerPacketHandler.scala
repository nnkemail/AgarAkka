package model

import akka.actor.{Actor, ActorRef}
import play.api.libs.json._
import model.Util.Position
import model.Entities._
import scala.collection.mutable.ListBuffer


trait PlayerPacketHandler{ this: PlayerActor =>    
  var _mousePosition: Position = Position(0, 0);
  def mousePosition = _mousePosition
  
  def sendUpdatesAndRemoves(toRemoveEntities: List[Entity], toUpdateEntities: List[Entity]) = {   
    implicit val updateFormat = Json.format[UpdateData]
    var toRemoveData = List.empty[Int]
    var toUpdateData = List[UpdateData]()
    
    for (e: Entity <- toRemoveEntities) 
      toRemoveData =  e.id :: toRemoveData
     
    val toRemoveJson = Json.toJson(toRemoveData)
    
    for (e: Entity <- toUpdateEntities)  
    e match {
        case e: Cell => 
          toUpdateData = UpdateData(e.id, e.position.x, e.position.y, e.getPhysicalSize(), 
              e.color.getRed, e.color.getGreen, e.color.getBlue, e.entityType.id, e.owner.name) ::
          toUpdateData
          
        case _ => 
          toUpdateData = UpdateData(e.id, e.position.x, e.position.y, e.getPhysicalSize(), 
              e.color.getRed, e.color.getGreen, e.color.getBlue, e.entityType.id, null) ::
          toUpdateData  
    }
    
    if (!toRemoveEntities.isEmpty) {   
      //println("to remove ent");    
      //println(toRemoveEntities);
    }
          
    out ! Json.obj("type" -> "modf", "toRemoveData" -> toRemoveJson, "toUpdateData" -> toUpdateData);
  } 
  
  def sendMyFristCellID(firstCellId: Int) = {
    out ! Json.obj("type" -> "spawnMyFirstCell", "firstCellId" -> firstCellId);
  }
  
  def sendNewCellId(newId: Int) = {
    out ! Json.obj("type" -> "myId", "nId" -> newId);
  }
  
  def showEndMenu(score: Int) = {
    out ! Json.obj("type" -> "showEndMenu", "score" -> score);
  }
  
  def sendNewScore(score: Int) = {
    out ! Json.obj("type" -> "newScore", "score" -> score);
  }
  
  def sendLeaderBoard(leaderBoard: List[LeaderBoardEntry]) = {
    implicit val leaderBoardEntryPacketFormat = Json.format[LeaderBoardEntryPacket]
    var toSendLeaderBoard: List[LeaderBoardEntryPacket] = List.empty
    
    for (entry <- leaderBoard)
      toSendLeaderBoard = LeaderBoardEntryPacket(entry.id, entry.name)::toSendLeaderBoard
      
    //println(toSendLeaderBoard);  
    out ! Json.obj("type"->"nLb", "lB" -> toSendLeaderBoard)   
  }
  
  def playerPacketHandler: Receive = {
     case msg: JsValue => {
       val t = (msg \ "type").as[String]
       
       t match {
         //case "Name"  => { this.name = (msg \ "name").as[String]; this.server ! Join }
         case "JoinRoom" => 
           println("Packet Hander Join Room");
           var idRoom = (msg \ "idRoom").as[Int]; 
           //this.name = (msg \ "name").as[String] 
           this.server ! JoinRoom(idRoom, this.userID)
           
         case "Coord" => { mousePosition.x = (msg \ "x").as[Double]; mousePosition.y = (msg \ "y").as[Double];}
         case "space" => splitCells(); //println("przyszlo space");
         case "w"     => ejectMass(); //println("przyszlo eject mass");
         case "RestartGame" => roomActor ! RestartMyGame; println("Przyszlo restart")
         case _ => sender ! "Error"
       }
     }
  }   
}