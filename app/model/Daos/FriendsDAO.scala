/*package model.Daos

import play.api.db.slick.DatabaseConfigProvider
import javax.inject.Inject
import com.mohiva.play.silhouette.api.LoginInfo
import play.api.libs.concurrent.Execution.Implicits._

class FriendsDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider, val userDAO: UserDAO) extends DAOSlick {
  import driver.api._
  
  def makeFriends(userLoginInfo: LoginInfo, friendLoginInfo: LoginInfo) = {
     var userFuture = userDAO.find(userLoginInfo);
     var friendFuture = userDAO.find(friendLoginInfo);
     
     val f3 = for {
      user <- userFuture
      friend <- friendFuture
     } 
  }
  
  protected def addAction(loginInfo: LoginInfo, loginInfo: LoginInfo) =
     
} */