package model.Daos

import slick.driver.JdbcProfile
import play.api.db.slick.HasDatabaseConfigProvider
import model.Daos.DBTableDefinitions

/**
 * Trait that contains generic slick db handling code to be mixed in with DAOs
 */
trait DAOSlick extends DBTableDefinitions with HasDatabaseConfigProvider[JdbcProfile]