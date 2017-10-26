-- MySQL dump 10.13  Distrib 5.7.18, for Linux (x86_64)
--
-- Host: localhost    Database: test
-- ------------------------------------------------------
-- Server version	5.5.5-10.1.22-MariaDB

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `arachneentityidentification`
--

DROP TABLE IF EXISTS `arachneentityidentification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `arachneentityidentification` (
  `ArachneEntityID` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'EinEinDeutiger Identifier fuer Arachne Datensaetze',
  `TableName` varchar(255) CHARACTER SET utf8 NOT NULL COMMENT 'Tabellenname',
  `ForeignKey` bigint(20) unsigned NOT NULL COMMENT 'Primärschluessel des Datensatzes in der Tabelle',
  `isDeleted` tinyint(4) NOT NULL DEFAULT '0' COMMENT 'Ist der Datensatz geloescht',
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`ArachneEntityID`),
  UNIQUE KEY `TableName` (`TableName`,`ForeignKey`)
) ENGINE=MyISAM AUTO_INCREMENT=16 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT='Diese Tabelle soll unseren Datensaetzen eineindeutige ids ve';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `arachneentityidentification`
--

LOCK TABLES `arachneentityidentification` WRITE;
/*!40000 ALTER TABLE `arachneentityidentification` DISABLE KEYS */;
INSERT INTO `arachneentityidentification` VALUES (1,'fabric',1,0,'2017-04-17 19:49:45'),(2,'fabric',2,0,'2017-04-17 19:49:45'),(3,'fabric',3,0,'2017-04-17 19:49:45'),(4,'fabric',4,0,'2017-04-17 19:49:45'),(5,'fabric',5,0,'2017-04-17 19:49:45'),(6,'fabric',6,0,'2017-04-17 19:49:45'),(7,'fabric',7,0,'2017-04-17 19:49:45'),(8,'test',1,0,'2017-04-17 19:49:45'),(9,'test',2,0,'2017-04-17 19:49:45'),(10,'test',3,0,'2017-04-17 19:49:45'),(11,'fabric',8,0,'2017-04-17 19:52:18'),(12,'fabric',9,0,'2017-04-17 19:52:26'),(13,'test',4,1,'2017-05-02 14:18:14'),(14,'fabric',10,0,'2017-05-02 12:34:54'),(15,'fabric',11,0,'2017-05-02 12:46:00');
/*!40000 ALTER TABLE `arachneentityidentification` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2017-05-02 19:54:42
