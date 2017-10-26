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
-- Dumping data for table `arachneentityidentification`
--

LOCK TABLES `arachneentityidentification` WRITE;
/*!40000 ALTER TABLE `arachneentityidentification` DISABLE KEYS */;
INSERT INTO `arachneentityidentification` VALUES (1,'fabric',1,0,'2017-04-17 19:49:45'),(2,'fabric',2,0,'2017-04-17 19:49:45'),(3,'fabric',3,0,'2017-04-17 19:49:45'),(4,'fabric',4,0,'2017-04-17 19:49:45'),(5,'fabric',5,0,'2017-04-17 19:49:45'),(6,'fabric',6,0,'2017-04-17 19:49:45'),(7,'fabric',7,0,'2017-04-17 19:49:45'),(8,'test',1,0,'2017-04-17 19:49:45'),(9,'test',2,0,'2017-04-17 19:49:45'),(10,'test',3,0,'2017-04-17 19:49:45'),(11,'fabric',8,0,'2017-04-17 19:52:18'),(12,'fabric',9,0,'2017-04-17 19:52:26'),(13,'test',4,1,'2017-05-02 14:18:14'),(14,'fabric',10,0,'2017-05-02 12:34:54'),(15,'fabric',11,0,'2017-05-02 12:46:00'),(16,'test',6,0,'2017-05-16 09:09:42'),(17,'test',5,0,'2017-05-16 09:18:07'),(18,'test',7,0,'2017-05-16 12:16:01'),(19,'test',11,1,'2017-05-16 13:36:13'),(20,'test',12,0,'2017-05-16 13:18:45'),(21,'test',13,0,'2017-05-16 13:18:49'),(22,'test',15,0,'2017-05-16 13:19:00'),(23,'test',14,0,'2017-05-16 13:21:40'),(24,'test',17,0,'2017-05-16 13:22:13'),(25,'test',8,0,'2017-05-16 15:32:51'),(26,'test',9,0,'2017-05-16 15:32:51'),(27,'test',10,0,'2017-05-16 15:32:51'),(28,'test',16,0,'2017-05-16 15:32:51');
/*!40000 ALTER TABLE `arachneentityidentification` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping data for table `fabric`
--

LOCK TABLES `fabric` WRITE;
/*!40000 ALTER TABLE `fabric` DISABLE KEYS */;
INSERT INTO `fabric` VALUES (1,4,'2017-05-16 13:20:35'),(2,3,'2017-05-16 15:07:02'),(3,3,'2017-05-16 15:06:53'),(4,1,'2017-04-17 17:48:36'),(5,3,'2017-04-17 17:48:37'),(6,2,'2017-04-17 17:23:07'),(7,2,'2017-04-17 17:23:08'),(8,2,'2017-01-01 11:12:12'),(9,1,'2017-01-01 11:12:12'),(10,3,'2017-05-02 12:34:54'),(11,3,'2017-05-02 12:46:00');
/*!40000 ALTER TABLE `fabric` ENABLE KEYS */;
UNLOCK TABLES;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`127.0.0.1`*/ /*!50003 TRIGGER `test`.`createEntityIDFabric` AFTER INSERT ON `fabric` FOR EACH ROW BEGIN 
INSERT INTO  `arachneentityidentification` 
SET  `arachneentityidentification`.`TableName`='fabric', `arachneentityidentification`.`ForeignKey` = NEW.`PS_FabricID` ;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`127.0.0.1`*/ /*!50003 TRIGGER `test`.`deleteEntityIDFabric` AFTER DELETE ON `fabric` FOR EACH ROW
BEGIN update
`arachneentityidentification` set `isDeleted`='1' where `arachneentityidentification`.`TableName`='fabric' and `arachneentityidentification`.`ForeignKey` = OLD.`PS_FabricID`;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Dumping data for table `test`
--

LOCK TABLES `test` WRITE;
/*!40000 ALTER TABLE `test` DISABLE KEYS */;
INSERT INTO `test` VALUES (1,'HansMans','2017-04-17 17:49:39'),(2,'OttoMotto','2017-04-17 17:49:38'),(3,'Test','2017-04-17 17:49:37'),(5,'TestName','2017-05-16 09:18:07'),(6,'TestName2','2017-05-16 09:09:42'),(7,'asd','2017-05-16 12:16:01'),(8,'uhoidhs','2017-05-16 15:32:51'),(9,'wer','2017-05-16 11:22:04'),(10,'sdjfoiu','2017-05-16 15:32:51'),(12,'TestName2','2017-05-16 13:18:45'),(13,'TestName2','2017-05-16 13:18:49'),(14,'TestName2','2017-05-16 13:21:40'),(15,'TestName2','2017-05-16 13:19:00'),(16,'dfg','2017-05-16 11:21:56'),(17,'TestName2','2017-05-16 13:22:13');
/*!40000 ALTER TABLE `test` ENABLE KEYS */;
UNLOCK TABLES;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`127.0.0.1`*/ /*!50003 TRIGGER `test`.`createEntityIDTest` AFTER INSERT ON `test` FOR EACH ROW BEGIN 
INSERT INTO  `arachneentityidentification` 
SET  `arachneentityidentification`.`TableName`='test', `arachneentityidentification`.`ForeignKey` = NEW.`PS_TestID` ;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`127.0.0.1`*/ /*!50003 TRIGGER `test`.`deleteEntityIDTest` AFTER DELETE ON `test` FOR EACH ROW
BEGIN
update `arachneentityidentification` set `isDeleted`='1' where `arachneentityidentification`.`TableName`='test' and `arachneentityidentification`.`ForeignKey` = OLD.`PS_TestID`;
update `fabric` set `kundennr`=null where `kundennr`=OLD.`PS_TestID`;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2017-05-16 18:59:46
