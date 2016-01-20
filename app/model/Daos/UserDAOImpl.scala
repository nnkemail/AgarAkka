package model.Daos

import java.util.UUID
import com.mohiva.play.silhouette.api.LoginInfo
import model.User
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import scala.concurrent.Future
import scala.collection.mutable.ListBuffer


/**
 * Give access to the user object using Slick
 */
class UserDAOImpl @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends UserDAO with DAOSlick {

  import driver.api._

  /**
   * Finds a user by its login info.
   *
   * @param loginInfo The login info of the user to find.
   * @return The found user or None if no user for the given login info could be found.
   */
  def find(loginInfo: LoginInfo) = {
    val userQuery = for {
      dbLoginInfo <- loginInfoQuery(loginInfo)
      dbUserLoginInfo <- slickUserLoginInfos.filter(_.loginInfoId === dbLoginInfo.id)
      dbUser <- slickUsers.filter(_.id === dbUserLoginInfo.userID)
    } yield dbUser
    db.run(userQuery.result.headOption).map { dbUserOption =>
      dbUserOption.map { user =>
        User(UUID.fromString(user.userID), loginInfo, user.firstName, user.lastName, user.fullName, user.email, user.avatarURL)
      }
    }
  }  

  /**
   * Finds a user by its user ID.
   *
   * @param userID The ID of the user to find.
   * @return The found user or None if no user for the given ID could be found.
   */
  def find(userID: UUID) = {
    val query = for {
      dbUser <- slickUsers.filter(_.id === userID.toString)
      dbUserLoginInfo <- slickUserLoginInfos.filter(_.userID === dbUser.id)
      dbLoginInfo <- slickLoginInfos.filter(_.id === dbUserLoginInfo.loginInfoId)
    } yield (dbUser, dbLoginInfo)
    db.run(query.result.headOption).map { resultOption =>
      resultOption.map {
        case (user, loginInfo) =>
          User(
            UUID.fromString(user.userID),
            LoginInfo(loginInfo.providerID, loginInfo.providerKey),
            user.firstName,
            user.lastName,
            user.fullName,
            user.email,
            user.avatarURL)
      }
    }
  }

  /**
   * Saves a user.
   *
   * @param user The user to save.
   * @return The saved user.
   */
  def save(user: User) = {
    val dbUser = DBUser(user.userID.toString, user.firstName, user.lastName, user.fullName, user.email, user.avatarURL)
    val dbLoginInfo = DBLoginInfo(None, user.loginInfo.providerID, user.loginInfo.providerKey)
    // We don't have the LoginInfo id so we try to get it first.
    // If there is no LoginInfo yet for this user we retrieve the id on insertion.    
    val loginInfoAction = {
      val retrieveLoginInfo = slickLoginInfos.filter(
        info => info.providerID === user.loginInfo.providerID &&
        info.providerKey === user.loginInfo.providerKey).result.headOption
      val insertLoginInfo = slickLoginInfos.returning(slickLoginInfos.map(_.id)).
        into((info, id) => info.copy(id = Some(id))) += dbLoginInfo
      for {
        loginInfoOption <- retrieveLoginInfo
        loginInfo <- loginInfoOption.map(DBIO.successful(_)).getOrElse(insertLoginInfo)
      } yield loginInfo
    }
    // combine database actions to be run sequentially
    val actions = (for {
      _ <- slickUsers.insertOrUpdate(dbUser)
      loginInfo <- loginInfoAction
      _ <- slickUserLoginInfos += DBUserLoginInfo(dbUser.userID, loginInfo.id.get)
    } yield ()).transactionally
    // run actions and return user afterwards
    db.run(actions).map(_ => user)
  }
  
  def addFriend(userLoginInfo: LoginInfo, friendLoginInfo: LoginInfo): Future[Option[User]] = {
     val searchQuery = for {
      userOption <- find(userLoginInfo)
      friendOption <- find(friendLoginInfo)
     } yield (userOption, friendOption);
     
   searchQuery flatMap { resultOption => 
      resultOption match {
       case (Some(user), Some(friend)) => 
         val insertIfNotExistsAction = (slickFriends.filter(row => row.userID === user.userID.toString && 
             row.friendID === friend.userID.toString).exists.result.flatMap
             { exists => 
               if (!exists) {
                 slickFriends += DBFriends(user.userID.toString, friend.userID.toString)
               } else {
                 DBIO.successful(None) // no-op
               }
             }
        ).transactionally
        db.run(insertIfNotExistsAction) map {resp => resp match {
          case None => None
          case _ => Some(friend)
          }
         }
   
       
       case (_, _)  => Future.successful(None);
      }
    }
  }

  def getFriends(userLoginInfo: LoginInfo): Future[List[Option[User]]] = {
    val futures = new ListBuffer[Future[Option[User]]]

    var userFuture = find(userLoginInfo)
    userFuture flatMap { userOption => 
      userOption match {
        case Some(user) => 
          //println("get Friends User :" + user)
          var action = for {
            friendRow <- slickFriends if (friendRow.userID === user.userID.toString) 
          } yield (friendRow)    
          
          db.run(action.result) flatMap { friendRows =>
            for (friend <- friendRows) {
              futures += find (UUID.fromString(friend.friendID))
            }
            
            val f = Future.sequence(futures.toList)
            f
           }
                                  
        case None => Future.successful(List.empty)
      }
    }
  }
  
  def getFriends(userID: UUID): Future[List[Option[User]]] = {
    val futures = new ListBuffer[Future[Option[User]]]

    var userFuture = find(userID)
    userFuture flatMap { userOption => 
      userOption match {
        case Some(user) => 
          //println("get Friends User :" + user)
          var action = for {
            friendRow <- slickFriends if (friendRow.userID === user.userID.toString) 
          } yield (friendRow)    
          
          db.run(action.result) flatMap { friendRows =>
            for (friend <- friendRows) {
              futures += find (UUID.fromString(friend.friendID))
            }
            
            val f = Future.sequence(futures.toList)
            f
           }
                                  
        case None => Future.successful(List.empty)
      }
    }  
  }
  
//  def setUserRoom(userID: UUID, roomID: Option[Long]) = {
//    val q = for { u <- slickUsers if u.id === userID.toString } yield u.roomID
//    val updateAction = q.update(roomID)
//  }
}
