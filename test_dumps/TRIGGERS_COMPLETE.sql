-- MySQL dump 10.13  Distrib 5.7.18, for Linux (x86_64)
--
-- Host: localhost    Database: ceramalex
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
) ENGINE=MyISAM AUTO_INCREMENT=16833 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT='Diese Tabelle soll unseren Datensaetzen eineindeutige ids ve';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `befund`
--

DROP TABLE IF EXISTS `befund`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `befund` (
  `PS_BefundID` mediumint(9) NOT NULL AUTO_INCREMENT,
  `PS_FMPBefundID` mediumint(9) NOT NULL DEFAULT '-1',
  `Befund` varchar(256) DEFAULT NULL,
  `Abhub` varchar(256) DEFAULT NULL,
  `Areal` varchar(256) DEFAULT NULL,
  `Kommentar` text,
  `Grabungsort` varchar(256) DEFAULT NULL,
  `DatensatzGruppeBefund` varchar(128) NOT NULL DEFAULT 'ceramalex',
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`PS_BefundID`),
  KEY `PS_FMPContextID` (`PS_FMPBefundID`)
) ENGINE=InnoDB AUTO_INCREMENT=856 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'NO_AUTO_VALUE_ON_ZERO' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `createEntityIDBefund` AFTER INSERT ON `befund` FOR EACH ROW BEGIN 
INSERT INTO  `arachneentityidentification` 
SET  `arachneentityidentification`.`TableName`='befund', `arachneentityidentification`.`ForeignKey` = NEW.`PS_BefundID` ;
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
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `DeleteBefundConnections` AFTER DELETE ON `befund` FOR EACH ROW BEGIN
DELETE FROM `datierung` WHERE `datierung`.`FS_BefundID` = OLD.`PS_BefundID`;
DELETE FROM `literaturzitat` WHERE `literaturzitat`.`FS_BefundID` = OLD.`PS_BefundID`;
DELETE FROM `ortsbezug` WHERE `ortsbezug`.`FS_BefundID` = OLD.`PS_BefundID`;
UPDATE `mainabstract` SET `FS_BefundID` = NULL WHERE `mainabstract`.`FS_BefundID` = OLD.`PS_BefundID`;
UPDATE `arachneentityidentification` SET `isDeleted` = 1
WHERE `arachneentityidentification`.`ForeignKey` = OLD.`PS_BefundID` AND `arachneentityidentification`.`TableName` = "befund";
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `datierung`
--

DROP TABLE IF EXISTS `datierung`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `datierung` (
  `PS_DatierungID` int(11) NOT NULL AUTO_INCREMENT,
  `PS_FMPDatierungID` int(11) NOT NULL DEFAULT '-1',
  `FS_BefundID` int(11) DEFAULT NULL,
  `FS_FabricID` int(11) DEFAULT NULL,
  `FS_MorphologyID` int(11) DEFAULT NULL,
  `FS_FabricDescriptionID` int(11) DEFAULT NULL,
  `FS_MainAbstractID` int(11) DEFAULT NULL,
  `FS_IsolatedSherdID` int(11) DEFAULT NULL,
  `AnfDatJh` varchar(256) DEFAULT NULL,
  `AnfDatvn` varchar(256) DEFAULT NULL,
  `AnfDatZeitraum` varchar(256) DEFAULT NULL,
  `AnfEpoche` varchar(256) DEFAULT NULL,
  `AnfPraezise` varchar(256) DEFAULT NULL,
  `AnfPraezise_Schwankung` varchar(256) DEFAULT NULL,
  `AnfTerminus` varchar(256) DEFAULT NULL,
  `Autor` varchar(256) DEFAULT NULL,
  `EndDatJh` varchar(256) DEFAULT NULL,
  `EndDatvn` varchar(256) DEFAULT NULL,
  `EndDatZeitraum` varchar(256) DEFAULT NULL,
  `EndEpoche` varchar(256) DEFAULT NULL,
  `EndPraezise` varchar(256) DEFAULT NULL,
  `EndPraezise_Schwankung` varchar(256) DEFAULT NULL,
  `EndTerminus` varchar(256) DEFAULT NULL,
  `FestDat` varchar(256) DEFAULT NULL,
  `Grundlage` varchar(256) DEFAULT NULL,
  `KommentarDat` varchar(256) DEFAULT NULL,
  `nachantik` varchar(256) DEFAULT NULL,
  `AnfPhase` varchar(256) DEFAULT NULL,
  `EndPhase` varchar(256) DEFAULT NULL,
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`PS_DatierungID`)
) ENGINE=InnoDB AUTO_INCREMENT=39 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `createEntityIDDatierung` AFTER INSERT ON `datierung` FOR EACH ROW BEGIN 
INSERT INTO  `arachneentityidentification` 
SET  `arachneentityidentification`.`TableName`='datierung', `arachneentityidentification`.`ForeignKey` = NEW.`PS_DatierungID` ;
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
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `DeleteDatierungConnections` AFTER DELETE ON `datierung` FOR EACH ROW BEGIN
UPDATE `literaturzitat` SET `FS_DatierungID` = NULL WHERE `literaturzitat`.`FS_DatierungID` = OLD.`PS_DatierungID`;
UPDATE `arachneentityidentification` SET `isDeleted` = 1
WHERE `arachneentityidentification`.`ForeignKey` = OLD.`PS_DatierungID` AND `arachneentityidentification`.`TableName` = "datierung";
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `einschluss`
--

DROP TABLE IF EXISTS `einschluss`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `einschluss` (
  `PS_EinschlussID` mediumint(9) NOT NULL AUTO_INCREMENT,
  `PS_FMPEinschlussID` mediumint(9) NOT NULL DEFAULT '-1',
  `FS_FabricDescriptionID` mediumint(9) DEFAULT NULL,
  `Haeufigkeit` varchar(256) DEFAULT NULL,
  `DurchschnittlicheGroesse` varchar(256) DEFAULT NULL,
  `Verteilung` varchar(256) DEFAULT NULL,
  `Form` varchar(256) DEFAULT NULL,
  `Opacity` varchar(256) DEFAULT NULL,
  `Einschlussart` varchar(256) DEFAULT NULL,
  `ColourFreeVon` varchar(256) DEFAULT NULL,
  `ColourFreeBis` varchar(256) DEFAULT NULL,
  `ColourQualifierVon` varchar(256) DEFAULT NULL,
  `ColourQualifierBis` varchar(256) DEFAULT NULL,
  `DatensatzGruppeEinschluss` varchar(128) NOT NULL DEFAULT 'ceramalex',
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`PS_EinschlussID`)
) ENGINE=InnoDB AUTO_INCREMENT=177 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'NO_AUTO_VALUE_ON_ZERO' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `createEntityIDEinschluss` AFTER INSERT ON `einschluss` FOR EACH ROW BEGIN 
INSERT INTO  `arachneentityidentification` 
SET  `arachneentityidentification`.`TableName`='einschluss', `arachneentityidentification`.`ForeignKey` = NEW.`PS_EinschlussID` ;
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
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `DeleteEinschlussConnections` AFTER DELETE ON `einschluss` FOR EACH ROW BEGIN
UPDATE `arachneentityidentification` SET `isDeleted` = 1
WHERE `arachneentityidentification`.`ForeignKey` = OLD.`PS_EinschlussID` AND `arachneentityidentification`.`TableName` = "einschluss";
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `fabric`
--

DROP TABLE IF EXISTS `fabric`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `fabric` (
  `PS_FabricID` int(11) NOT NULL AUTO_INCREMENT,
  `PS_FMPFabricID` mediumint(9) NOT NULL DEFAULT '-1',
  `Name` varchar(256) DEFAULT NULL,
  `CommonName` varchar(256) DEFAULT NULL,
  `Origin` varchar(128) DEFAULT NULL,
  `Comments` text,
  `DatensatzGruppeFabric` varchar(128) NOT NULL DEFAULT 'ceramalex',
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`PS_FabricID`)
) ENGINE=InnoDB AUTO_INCREMENT=288 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'NO_AUTO_VALUE_ON_ZERO' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `createEntityIDFabric` AFTER INSERT ON `fabric` FOR EACH ROW BEGIN 
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
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `DeleteFabricConnections` AFTER DELETE ON `fabric` FOR EACH ROW BEGIN
DELETE FROM `fabricdescription` WHERE `fabricdescription`.`FS_FabricID` = OLD.`PS_FabricID`;
DELETE FROM `datierung` WHERE `datierung`.`FS_FabricID` = OLD.`PS_FabricID`;
DELETE FROM `ortsbezug` WHERE `ortsbezug`.`FS_FabricID` = OLD.`PS_FabricID`;
DELETE FROM `image` WHERE `image`.`FS_FabricID` = OLD.`PS_FabricID`;
UPDATE `mainabstract` SET `FS_FabricID` = NULL WHERE `mainabstract`.`FS_FabricID` = OLD.`PS_FabricID`;
UPDATE `arachneentityidentification` SET `isDeleted` = 1
WHERE `arachneentityidentification`.`ForeignKey` = OLD.`PS_FabricID` AND `arachneentityidentification`.`TableName` = "fabric";
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `fabricdescription`
--

DROP TABLE IF EXISTS `fabricdescription`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `fabricdescription` (
  `PS_FabricDescriptionID` mediumint(9) NOT NULL AUTO_INCREMENT,
  `ColourFreeBis` varchar(64) DEFAULT NULL,
  `BasedOnSherdID` varchar(256) DEFAULT NULL,
  `FS_FabricID` mediumint(9) NOT NULL,
  `PS_FMPFabricDescriptionID` mediumint(9) NOT NULL DEFAULT '-1',
  `ColourOfBreakVon` varchar(256) DEFAULT NULL,
  `ColourOfBreakMunsellVon` varchar(128) DEFAULT NULL,
  `ColourOfBreakMunsellBis` varchar(128) DEFAULT NULL,
  `ColourOfCoreVon` varchar(256) DEFAULT NULL,
  `ColourOfCoreMunsellVon` varchar(128) DEFAULT NULL,
  `ColourOfCoreMunsellBis` varchar(128) DEFAULT NULL,
  `ColourOfOutsideVon` varchar(256) DEFAULT NULL,
  `ColourOfOutsideMunsellVon` varchar(128) DEFAULT NULL,
  `ColourOfOutsideMunsellBis` varchar(128) DEFAULT NULL,
  `Zoning` varchar(128) DEFAULT NULL,
  `Manufacture` varchar(256) DEFAULT NULL,
  `Hardness` varchar(256) DEFAULT NULL,
  `Fracture` varchar(256) DEFAULT NULL,
  `Porosity` varchar(256) DEFAULT NULL,
  `Comments` text,
  `ColourOfBreakBis` varchar(256) DEFAULT NULL,
  `ColourOfCoreBis` varchar(256) DEFAULT NULL,
  `ColourOfOutsideBis` varchar(256) DEFAULT NULL,
  `ColourOfBreakQualifierVon` varchar(256) DEFAULT NULL,
  `ColourOfBreakQualifierBis` varchar(256) DEFAULT NULL,
  `ColourOfCoreQualifierVon` varchar(256) DEFAULT NULL,
  `ColourOfCoreQualifierBis` varchar(256) DEFAULT NULL,
  `ColourOfOutsideQualifierVon` varchar(256) DEFAULT NULL,
  `ColourOfOutsideQualifierBis` varchar(256) DEFAULT NULL,
  `InclusionsVisible` varchar(64) DEFAULT NULL,
  `Condition` varchar(64) DEFAULT NULL,
  `SurfaceFeel` varchar(64) DEFAULT NULL,
  `ColourFreeVon` varchar(64) DEFAULT NULL,
  `ColourQualifierVon` varchar(64) DEFAULT NULL,
  `ColourQualifierBis` varchar(64) DEFAULT NULL,
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `ColourMunsellVon` varchar(128) DEFAULT NULL,
  `ColourMunsellBis` varchar(128) DEFAULT NULL,
  `DatensatzGruppeFabricdescription` varchar(128) NOT NULL DEFAULT 'ceramalex',
  PRIMARY KEY (`PS_FabricDescriptionID`)
) ENGINE=InnoDB AUTO_INCREMENT=132 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'NO_AUTO_VALUE_ON_ZERO' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `createEntityIDFabricDescription` AFTER INSERT ON `fabricdescription` FOR EACH ROW BEGIN 
INSERT INTO  `arachneentityidentification` 
SET  `arachneentityidentification`.`TableName`='fabricdescription', `arachneentityidentification`.`ForeignKey` = NEW.`PS_FabricDescriptionID` ;
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
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `DeleteFabricDescriptionConnections` AFTER DELETE ON `fabricdescription` FOR EACH ROW BEGIN
DELETE FROM `einschluss` WHERE `einschluss`.`FS_FabricDescriptionID` = OLD.`PS_FabricDescriptionID`;
DELETE FROM `datierung` WHERE `datierung`.`FS_FabricDescriptionID` = OLD.`PS_FabricDescriptionID`;
DELETE FROM `image` WHERE `image`.`FS_FabricDescriptionID` = OLD.`PS_FabricDescriptionID`;
DELETE FROM `ortsbezug` WHERE `ortsbezug`.`FS_FabricDescriptionID` = OLD.`PS_FabricDescriptionID`;
UPDATE `arachneentityidentification` SET `isDeleted` = 1
WHERE `arachneentityidentification`.`ForeignKey` = OLD.`PS_FabricDescriptionID` AND `arachneentityidentification`.`TableName` = "fabricdescription";
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `image`
--

DROP TABLE IF EXISTS `image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `image` (
  `PS_ImageID` int(11) NOT NULL AUTO_INCREMENT,
  `PS_FMPImageID` bigint(20) NOT NULL DEFAULT '-1',
  `FS_MorphologyID` bigint(20) DEFAULT NULL,
  `FS_IsolatedSherdID` bigint(20) DEFAULT NULL,
  `FS_FabricID` bigint(20) DEFAULT NULL,
  `FS_FabricDescriptionID` bigint(20) DEFAULT NULL,
  `Dateiname` varchar(256) DEFAULT NULL,
  `Dateipfad` varchar(1024) DEFAULT NULL,
  `PfadNeu` varchar(512) DEFAULT NULL,
  `Description` varchar(1024) DEFAULT NULL,
  `NegativNr` varchar(256) DEFAULT NULL,
  `Scannummer` varchar(256) DEFAULT NULL,
  `AufnahmeDatum` timestamp NULL DEFAULT NULL,
  `ErstellungsDatum` timestamp NULL DEFAULT NULL,
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`PS_ImageID`)
) ENGINE=InnoDB AUTO_INCREMENT=4723 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `createEntityIDImage` AFTER INSERT ON `image` FOR EACH ROW BEGIN 
INSERT INTO  `arachneentityidentification` 
SET  `arachneentityidentification`.`TableName`='image', `arachneentityidentification`.`ForeignKey` = NEW.`PS_ImageID` ;
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
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `DeleteImageConnections` AFTER DELETE ON `image` FOR EACH ROW BEGIN
UPDATE `arachneentityidentification` SET `isDeleted` = 1
WHERE `arachneentityidentification`.`ForeignKey` = OLD.`PS_ImageID` AND `arachneentityidentification`.`TableName` = "image";
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `individualvessel`
--

DROP TABLE IF EXISTS `individualvessel`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `individualvessel` (
  `PS_IndividualVesselID` int(9) NOT NULL AUTO_INCREMENT,
  `PS_FMPIndividualVesselID` int(9) NOT NULL DEFAULT '-1',
  `FS_MainAbstractID` int(9) NOT NULL,
  `InventoryNumber` varchar(256) DEFAULT NULL,
  `RimCount` smallint(6) DEFAULT NULL,
  `HandleCount` smallint(6) DEFAULT NULL,
  `BaseCount` smallint(6) DEFAULT NULL,
  `BodySherdCount` smallint(6) DEFAULT NULL,
  `OthersCount` smallint(6) DEFAULT NULL,
  `Height` float(10,2) DEFAULT NULL,
  `Width` float(10,2) DEFAULT NULL,
  `LengthSize` float(10,2) DEFAULT NULL,
  `Thickness` float(10,2) DEFAULT NULL,
  `BaseDiameter` float(10,2) DEFAULT NULL,
  `RimDiameter` float(10,2) DEFAULT NULL,
  `WidestDiameter` float(10,2) DEFAULT NULL,
  `Volume` float(10,2) DEFAULT NULL,
  `Joins` smallint(6) DEFAULT NULL,
  `RimPercentage` smallint(6) DEFAULT NULL,
  `DatensatzGruppeIndividualvessel` varchar(128) NOT NULL DEFAULT 'ceramalex',
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`PS_IndividualVesselID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'NO_AUTO_VALUE_ON_ZERO' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `createEntityIDIndividualVessel` AFTER INSERT ON `individualvessel` FOR EACH ROW BEGIN 
INSERT INTO  `arachneentityidentification` 
SET  `arachneentityidentification`.`TableName`='individualvessel', `arachneentityidentification`.`ForeignKey` = NEW.`PS_IndividualVesselID` ;
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
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `DeleteIndividualVesselConnections` AFTER DELETE ON `individualvessel` FOR EACH ROW BEGIN
UPDATE `arachneentityidentification` SET `isDeleted` = 1
WHERE `arachneentityidentification`.`ForeignKey` = OLD.`PS_IndividualVesselID` AND `arachneentityidentification`.`TableName` = "individualvessel";
DELETE FROM `xsherdtovesselx` WHERE `xsherdtovesselx`.`FS_IndividualVesselID` = OLD.`PS_IndividualVesselID`;
UPDATE `isolatedsherd` SET `FS_IndividualVesselID` = NULL WHERE `isolatedsherd`.`FS_IndividualVesselID` = OLD.`PS_IndividualVesselID`;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `isolatedsherd`
--

DROP TABLE IF EXISTS `isolatedsherd`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `isolatedsherd` (
  `PS_IsolatedSherdID` int(11) NOT NULL AUTO_INCREMENT,
  `PS_FMPIsolatedSherdID` bigint(11) NOT NULL DEFAULT '-1',
  `FS_MainAbstractID` bigint(11) DEFAULT NULL,
  `FS_IndividualVesselID` bigint(11) DEFAULT NULL,
  `SherdType` varchar(128) DEFAULT NULL,
  `Diameter` varchar(64) DEFAULT NULL,
  `InventoryNumber` varchar(128) DEFAULT NULL,
  `NitonAnalysisID` varchar(256) DEFAULT NULL,
  `Height` float(10,2) DEFAULT NULL,
  `Width` float(10,2) DEFAULT NULL,
  `LengthSize` float(10,2) DEFAULT NULL,
  `Thickness` varchar(64) DEFAULT NULL,
  `Weight` float(10,2) DEFAULT NULL,
  `RimPercentage` float(10,2) DEFAULT NULL,
  `DatensatzGruppeIsolatedsherd` varchar(128) NOT NULL DEFAULT 'ceramalex',
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`PS_IsolatedSherdID`)
) ENGINE=InnoDB AUTO_INCREMENT=2224 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'NO_AUTO_VALUE_ON_ZERO' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `createEntityIDIsolatedSherd` AFTER INSERT ON `isolatedsherd` FOR EACH ROW BEGIN 
INSERT INTO  `arachneentityidentification` 
SET  `arachneentityidentification`.`TableName`='isolatedsherd', `arachneentityidentification`.`ForeignKey` = NEW.`PS_IsolatedSherdID` ;
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
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `DeleteIsolatedsherdConnections` AFTER DELETE ON `isolatedsherd` FOR EACH ROW BEGIN
DELETE FROM `xsurfacetreatmentx` WHERE `xsurfacetreatmentx`.`FS_IsolatedSherdID` = OLD.`PS_IsolatedSherdID`;
UPDATE `arachneentityidentification` SET `isDeleted` = 1
WHERE `arachneentityidentification`.`ForeignKey` = OLD.`PS_IsolatedSherdID` AND `arachneentityidentification`.`TableName` = "isolatedsherd";
DELETE FROM `xsherdtovesselx` WHERE `xsherdtovesselx`.`FS_IsolatedSherdID` = OLD.`PS_IsolatedSherdID`;
DELETE FROM `datierung` WHERE `datierung`.`FS_IsolatedSherdID` = OLD.`PS_IsolatedSherdID`;
UPDATE `image` SET `FS_IsolatedSherdID` = NULL WHERE `image`.`FS_IsolatedSherdID` = OLD.`PS_IsolatedSherdID`;
UPDATE `ortsbezug` SET `FS_IsolatedSherdID` = NULL WHERE `ortsbezug`.`FS_IsolatedSherdID` = OLD.`PS_IsolatedSherdID`;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `literatur`
--

DROP TABLE IF EXISTS `literatur`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `literatur` (
  `PS_LiteraturID` mediumint(8) unsigned NOT NULL AUTO_INCREMENT,
  `PS_FMPLiteraturID` int(11) NOT NULL DEFAULT '-1',
  `Abkuerzungen` varchar(255) DEFAULT NULL,
  `AenderungsdatumLit` varchar(255) DEFAULT NULL,
  `Auflage` varchar(255) DEFAULT NULL,
  `Ausstellungsjahr` varchar(255) DEFAULT NULL,
  `Ausstellungsort` varchar(255) DEFAULT NULL,
  `Band` varchar(255) DEFAULT NULL,
  `BearbeiterLit` varchar(255) DEFAULT NULL,
  `KorrektorLit` varchar(255) DEFAULT NULL,
  `DAIRichtlinien` text,
  `Erstellungsdatum` varchar(255) DEFAULT NULL,
  `Festschrift` varchar(255) DEFAULT NULL,
  `HKoerperschaft` varchar(255) DEFAULT NULL,
  `Jahr` varchar(255) DEFAULT NULL,
  `Katalogort` varchar(255) DEFAULT NULL,
  `Kongressjahr` varchar(255) DEFAULT NULL,
  `Kongressort` varchar(255) DEFAULT NULL,
  `A1Nachname` varchar(255) DEFAULT NULL,
  `A2Nachname` varchar(255) DEFAULT NULL,
  `A3Nachname` varchar(255) DEFAULT NULL,
  `Ort` varchar(255) DEFAULT NULL,
  `Reihe` varchar(255) DEFAULT NULL,
  `Titel` varchar(255) DEFAULT NULL,
  `A1Vorname` varchar(255) DEFAULT NULL,
  `A2Vorname` varchar(255) DEFAULT NULL,
  `A3Vorname` varchar(255) DEFAULT NULL,
  `Zeitschrift` varchar(255) DEFAULT NULL,
  `Nachschlagewerk` varchar(255) DEFAULT NULL,
  `H1Vorname` varchar(255) DEFAULT NULL,
  `H2Vorname` varchar(255) DEFAULT NULL,
  `H3Vorname` varchar(255) DEFAULT NULL,
  `H1Nachname` varchar(255) DEFAULT NULL,
  `H2Nachname` varchar(255) DEFAULT NULL,
  `H3Nachname` varchar(255) DEFAULT NULL,
  `StichwortSortierung` varchar(255) DEFAULT NULL,
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`PS_LiteraturID`)
) ENGINE=InnoDB AUTO_INCREMENT=66 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `createEntityIDLiteratur` AFTER INSERT ON `literatur` FOR EACH ROW BEGIN 
INSERT INTO  `arachneentityidentification` 
SET  `arachneentityidentification`.`TableName`='literatur', `arachneentityidentification`.`ForeignKey` = NEW.`PS_LiteraturID` ;
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
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `DeleteLitCitation` AFTER DELETE ON `literatur` FOR EACH ROW BEGIN
DELETE FROM `literaturzitat` WHERE `literaturzitat`.`FS_LiteraturID` = OLD.`PS_LiteraturID`;
UPDATE `arachneentityidentification` SET `isDeleted` = 1
WHERE `arachneentityidentification`.`ForeignKey` = OLD.`PS_LiteraturID` AND `arachneentityidentification`.`TableName` = "literatur";
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `literaturzitat`
--

DROP TABLE IF EXISTS `literaturzitat`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `literaturzitat` (
  `PS_LiteraturzitatID` mediumint(8) unsigned NOT NULL AUTO_INCREMENT,
  `PS_FMPLiteraturzitatID` int(11) NOT NULL DEFAULT '-1',
  `FS_BefundID` int(11) DEFAULT NULL,
  `FS_MorphologyID` int(11) DEFAULT NULL,
  `FS_SurfaceTreatmentID` int(11) DEFAULT NULL,
  `FS_DatierungID` int(10) unsigned DEFAULT NULL,
  `FS_LiteraturID` mediumint(8) unsigned DEFAULT NULL,
  `Abbildung` varchar(255) DEFAULT NULL,
  `Anmerkung` text,
  `Beilage` varchar(32) DEFAULT NULL,
  `Figur` varchar(255) DEFAULT NULL,
  `Katnummer` varchar(255) DEFAULT NULL,
  `Kommentar` text,
  `Seite` varchar(255) DEFAULT NULL,
  `subVoce` varchar(255) DEFAULT NULL,
  `Tafel` varchar(255) DEFAULT NULL,
  `Stichwort` varchar(255) DEFAULT NULL,
  `WortZitat` varchar(512) DEFAULT NULL,
  `lastModified` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`PS_LiteraturzitatID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `createEntityIDLiteraturzitat` AFTER INSERT ON `literaturzitat` FOR EACH ROW BEGIN 
INSERT INTO  `arachneentityidentification` 
SET  `arachneentityidentification`.`TableName`='literaturzitat', `arachneentityidentification`.`ForeignKey` = NEW.`PS_LiteraturzitatID` ;
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
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`::1`*/ /*!50003 TRIGGER `ceramalex`.`literaturzitat_AFTER_DELETE` AFTER DELETE ON `literaturzitat` FOR EACH ROW
BEGIN
UPDATE `arachneentityidentification` SET `isDeleted` = 1
WHERE `arachneentityidentification`.`ForeignKey` = OLD.`PS_LiteraturzitatID` AND `arachneentityidentification`.`TableName` = "literaturzitat";
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `mainabstract`
--

DROP TABLE IF EXISTS `mainabstract`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `mainabstract` (
  `PS_MainAbstractID` mediumint(9) NOT NULL AUTO_INCREMENT,
  `PS_FMPMainAbstractID` mediumint(9) NOT NULL DEFAULT '-1',
  `FS_FabricID` mediumint(9) DEFAULT NULL,
  `FS_QuantitiesID` mediumint(9) DEFAULT NULL,
  `FS_MorphologyID` mediumint(9) DEFAULT NULL,
  `FS_BefundID` mediumint(9) DEFAULT NULL,
  `GrabungsinterneTypennummer` varchar(128) DEFAULT NULL,
  `GrabungsinterneTypennummerSub` varchar(128) DEFAULT NULL,
  `DatensatzGruppeMainabstract` varchar(128) NOT NULL DEFAULT 'ceramalex',
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `ImportSource` varchar(255) DEFAULT NULL,
  `Editor` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`PS_MainAbstractID`)
) ENGINE=InnoDB AUTO_INCREMENT=3001 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'NO_AUTO_VALUE_ON_ZERO' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `createEntityIDMainAbstract` AFTER INSERT ON `mainabstract` FOR EACH ROW BEGIN 
INSERT INTO  `arachneentityidentification` 
SET  `arachneentityidentification`.`TableName`='mainabstract', `arachneentityidentification`.`ForeignKey` = NEW.`PS_MainAbstractID` ;
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
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `DeleteMainConnections` AFTER DELETE ON `mainabstract` FOR EACH ROW BEGIN
DELETE FROM `xmorphologyx` WHERE `xmorphologyx`.`FS_MainAbstractID` = OLD.`PS_MainAbstractID`;
DELETE FROM `datierung` WHERE `datierung`.`FS_MainAbstractID` = OLD.`PS_MainAbstractID`;
DELETE FROM `quantities` WHERE `quantities`.`PS_QuantitiesID` = OLD.`FS_QuantitiesID`;
DELETE FROM `xsurfacetreatmentx` WHERE `xsurfacetreatmentx`.`FS_MainAbstractID` = OLD.`PS_MainAbstractID`;
DELETE FROM `individualvessel` WHERE `individualvessel`.`FS_MainAbstractID` = OLD.`PS_MainAbstractID`;
DELETE FROM `isolatedsherd` WHERE `isolatedsherd`.`FS_MainAbstractID` = OLD.`PS_MainAbstractID`;
UPDATE `arachneentityidentification` SET `isDeleted` = 1
WHERE `arachneentityidentification`.`ForeignKey` = OLD.`PS_MainAbstractID` AND `arachneentityidentification`.`TableName` = "mainabstract";
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `morphology`
--

DROP TABLE IF EXISTS `morphology`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `morphology` (
  `PS_MorphologyID` mediumint(9) NOT NULL AUTO_INCREMENT,
  `PS_FMPMorphologyID` mediumint(9) NOT NULL DEFAULT '-1',
  `Level1` varchar(512) DEFAULT NULL,
  `Level2` varchar(512) DEFAULT NULL,
  `Level3` varchar(512) DEFAULT NULL,
  `Level4` varchar(512) DEFAULT NULL,
  `Level5` varchar(512) DEFAULT NULL,
  `Level6` varchar(512) DEFAULT NULL,
  `Level1_6` varchar(256) DEFAULT NULL,
  `DatensatzGruppeMorphology` varchar(128) NOT NULL DEFAULT 'ceramalex',
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`PS_MorphologyID`)
) ENGINE=InnoDB AUTO_INCREMENT=2224 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'NO_AUTO_VALUE_ON_ZERO' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `createEntityIDMorphology` AFTER INSERT ON `morphology` FOR EACH ROW BEGIN 
INSERT INTO  `arachneentityidentification` 
SET  `arachneentityidentification`.`TableName`='morphology', `arachneentityidentification`.`ForeignKey` = NEW.`PS_MorphologyID` ;
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
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `DeleteMorphologyConnections` AFTER DELETE ON `morphology` FOR EACH ROW BEGIN
DELETE FROM `xmorphologyx` WHERE `xmorphologyx`.`FS_MorphologyID` = OLD.`PS_MorphologyID`;
UPDATE `mainabstract` SET `FS_MorphologyID` = NULL WHERE `mainabstract`.`FS_MorphologyID` = OLD.`PS_MorphologyID`;
DELETE FROM `image` WHERE `image`.`FS_MorphologyID` = OLD.`PS_MorphologyID`;
DELETE FROM `datierung` WHERE `datierung`.`FS_MorphologyID` = OLD.`PS_MorphologyID`;
UPDATE `arachneentityidentification` SET `isDeleted` = 1
WHERE `arachneentityidentification`.`ForeignKey` = OLD.`PS_MorphologyID` AND `arachneentityidentification`.`TableName` = "morphology";
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `niton`
--

DROP TABLE IF EXISTS `niton`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `niton` (
  `PS_NitonID` int(11) NOT NULL AUTO_INCREMENT,
  `Time` datetime NOT NULL,
  `Type` varchar(255) DEFAULT NULL,
  `Duration` float NOT NULL,
  `Units` varchar(128) NOT NULL DEFAULT 'ppm',
  `SigmaValue` smallint(6) DEFAULT NULL,
  `Sequence` varchar(128) DEFAULT NULL,
  `Flags` varchar(255) DEFAULT NULL,
  `Sample` varchar(255) NOT NULL,
  `Location` varchar(255) DEFAULT NULL,
  `Inspector` varchar(255) DEFAULT NULL,
  `Misc` varchar(255) DEFAULT NULL,
  `Note` varchar(255) DEFAULT NULL,
  `Si` float DEFAULT NULL,
  `Ti` float DEFAULT NULL,
  `Al` float DEFAULT NULL,
  `Fe` float DEFAULT NULL,
  `Mn` float DEFAULT NULL,
  `Mg` float DEFAULT NULL,
  `Ca` float DEFAULT NULL,
  `K` float DEFAULT NULL,
  `P` float DEFAULT NULL,
  `S` float DEFAULT NULL,
  `V` float DEFAULT NULL,
  `Cr` float DEFAULT NULL,
  `Ni` float DEFAULT NULL,
  `Cu` float DEFAULT NULL,
  `Zn` float DEFAULT NULL,
  `Rb` float DEFAULT NULL,
  `Sr` float DEFAULT NULL,
  `Y` float DEFAULT NULL,
  `Zr` float DEFAULT NULL,
  `Nb` float DEFAULT NULL,
  `Pb` float DEFAULT NULL,
  `Ba` float DEFAULT NULL,
  `Sb` float DEFAULT NULL,
  `Sn` float DEFAULT NULL,
  `Cd` float DEFAULT NULL,
  `Ag` float DEFAULT NULL,
  `Bal` float DEFAULT NULL,
  `Mo` float DEFAULT NULL,
  `Bi` float DEFAULT NULL,
  `Au` float DEFAULT NULL,
  `Se` float DEFAULT NULL,
  `As` float DEFAULT NULL,
  `W` float DEFAULT NULL,
  `Co` float DEFAULT NULL,
  `Cl` float DEFAULT NULL,
  `Ce` float DEFAULT NULL,
  PRIMARY KEY (`PS_NitonID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `createEntityIDNiton` AFTER INSERT ON `niton` FOR EACH ROW BEGIN 
INSERT INTO  `arachneentityidentification` 
SET  `arachneentityidentification`.`TableName`='niton', `arachneentityidentification`.`ForeignKey` = NEW.`PS_NitonID` ;
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
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `DeleteNitonConnections` AFTER DELETE ON `niton` FOR EACH ROW BEGIN
UPDATE `arachneentityidentification` SET `isDeleted` = 1
WHERE `arachneentityidentification`.`ForeignKey` = OLD.`PS_NitonID` AND `arachneentityidentification`.`TableName` = "niton";
UPDATE `isolatedsherd` SET `NitonAnalysisID` = NULL WHERE `isolatedsherd`.`NitonAnalysisID` = OLD.`PS_NitonID`;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `ort`
--

DROP TABLE IF EXISTS `ort`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ort` (
  `PS_OrtID` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `PS_FMPOrtID` mediumint(9) NOT NULL DEFAULT '-1',
  `Aufbewahrungsort` varchar(255) DEFAULT '',
  `Aufbewahrungsort_Synonym` varchar(255) DEFAULT '',
  `Ort_antik` text,
  `City` varchar(256) DEFAULT '',
  `Stadt_Synonym` varchar(255) DEFAULT '',
  `Country` varchar(256) DEFAULT '',
  `continent` varchar(255) DEFAULT NULL,
  `continentCode` varchar(255) DEFAULT NULL,
  `Longitude` double DEFAULT NULL,
  `Latitude` double DEFAULT NULL,
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `oaipmhsetOrt` varchar(256) NOT NULL DEFAULT 'ort',
  `Countrycode` varchar(2) DEFAULT NULL,
  `Geonamesid` int(11) DEFAULT NULL,
  `Genauigkeit` varchar(255) DEFAULT NULL,
  `region` varchar(128) DEFAULT NULL,
  `subregion` varchar(128) DEFAULT NULL,
  `RoughAllocation` varchar(256) DEFAULT NULL,
  `ConcatPlace` varchar(256) DEFAULT NULL,
  `Location` varchar(256) DEFAULT NULL,
  PRIMARY KEY (`PS_OrtID`),
  KEY `Aufbewahrungsort` (`Aufbewahrungsort`),
  KEY `Stadt` (`City`(255)),
  KEY `Land` (`Country`(255)),
  KEY `continent` (`continent`),
  KEY `continentCode` (`continentCode`)
) ENGINE=InnoDB AUTO_INCREMENT=350 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `createEntityIDOrt` AFTER INSERT ON `ort` FOR EACH ROW BEGIN 
INSERT INTO  `arachneentityidentification` 
SET  `arachneentityidentification`.`TableName`='ort', `arachneentityidentification`.`ForeignKey` = NEW.`PS_OrtID` ;
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
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `DeleteOrtConnections` AFTER DELETE ON `ort` FOR EACH ROW BEGIN
DELETE FROM `ortsbezug` WHERE `ortsbezug`.`FS_OrtID` = OLD.`PS_OrtID`;
DELETE FROM `xortx` WHERE `xortx`.`FS_OrtID` = OLD.`PS_OrtID`;
UPDATE `arachneentityidentification` SET `isDeleted` = 1
WHERE `arachneentityidentification`.`ForeignKey` = OLD.`PS_OrtID` AND `arachneentityidentification`.`TableName` = "ort";
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `ortsbezug`
--

DROP TABLE IF EXISTS `ortsbezug`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ortsbezug` (
  `PS_OrtsbezugID` int(11) NOT NULL AUTO_INCREMENT,
  `PS_FMPOrtsbezugID` int(11) NOT NULL DEFAULT '-1',
  `FS_OrtID` mediumint(9) NOT NULL,
  `FS_FabricID` mediumint(9) DEFAULT NULL,
  `FS_BefundID` mediumint(9) DEFAULT NULL,
  `FS_XOrtIDX` mediumint(9) DEFAULT NULL,
  `FS_IsolatedSherdID` mediumint(9) DEFAULT NULL,
  `FS_FabricDescriptionID` mediumint(9) DEFAULT NULL,
  `TypeOfPlace` varchar(256) DEFAULT NULL,
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`PS_OrtsbezugID`),
  KEY `PS_FMPOrtsbezugID` (`PS_FMPOrtsbezugID`),
  KEY `FS_PlaceID` (`FS_OrtID`)
) ENGINE=InnoDB AUTO_INCREMENT=79 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `createEntityIDOrtsbezug` AFTER INSERT ON `ortsbezug` FOR EACH ROW BEGIN 
INSERT INTO  `arachneentityidentification` 
SET  `arachneentityidentification`.`TableName`='ortsbezug', `arachneentityidentification`.`ForeignKey` = NEW.`PS_OrtsbezugID` ;
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
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `DeleteOrtsbezugConnections` AFTER DELETE ON `ortsbezug` FOR EACH ROW BEGIN
UPDATE `arachneentityidentification` SET `isDeleted` = 1
WHERE `arachneentityidentification`.`ForeignKey` = OLD.`PS_OrtsbezugID` AND `arachneentityidentification`.`TableName` = "ortsbezug";
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `quantities`
--

DROP TABLE IF EXISTS `quantities`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `quantities` (
  `PS_QuantitiesID` mediumint(11) NOT NULL AUTO_INCREMENT,
  `PS_FMPQuantitiesID` mediumint(11) NOT NULL DEFAULT '-1',
  `RimCount` smallint(6) DEFAULT NULL,
  `HandleCount` smallint(6) DEFAULT NULL,
  `BaseCount` smallint(6) DEFAULT NULL,
  `BodySherdCount` smallint(6) DEFAULT NULL,
  `OthersCount` smallint(6) DEFAULT NULL,
  `RimWeight` float(10,2) DEFAULT NULL,
  `HandleWeight` float(10,2) DEFAULT NULL,
  `BodySherdWeight` float(10,2) DEFAULT NULL,
  `BaseWeight` float(10,2) DEFAULT NULL,
  `OthersWeight` float(10,2) DEFAULT NULL,
  `Joins` varchar(128) DEFAULT NULL,
  `MNI` smallint(6) DEFAULT NULL,
  `MXI` smallint(6) DEFAULT NULL,
  `RimPercentage` float(10,2) DEFAULT NULL,
  `MNIWeighted` float(10,2) DEFAULT NULL,
  `TotalSherds` smallint(6) DEFAULT NULL,
  `DatensatzGruppeQuantities` varchar(128) NOT NULL DEFAULT 'ceramalex',
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`PS_QuantitiesID`),
  KEY `PS_FMPQuantitiesID` (`PS_FMPQuantitiesID`)
) ENGINE=InnoDB AUTO_INCREMENT=7566 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'NO_AUTO_VALUE_ON_ZERO' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `createEntityIDQuantities` AFTER INSERT ON `quantities` FOR EACH ROW BEGIN 
INSERT INTO  `arachneentityidentification` 
SET  `arachneentityidentification`.`TableName`='quantities', `arachneentityidentification`.`ForeignKey` = NEW.`PS_QuantitiesID` ;
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
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `DeleteQuantitiesConnections` AFTER DELETE ON `quantities` FOR EACH ROW BEGIN
UPDATE `arachneentityidentification` SET `isDeleted` = 1
WHERE `arachneentityidentification`.`ForeignKey` = OLD.`PS_QuantitiesID` AND `arachneentityidentification`.`TableName` = "quantities";
UPDATE `mainabstract` SET `FS_QuantitiesID` = NULL WHERE `mainabstract`.`FS_QuantitiesID` = OLD.`PS_QuantitiesID`;
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `surfacetreatment`
--

DROP TABLE IF EXISTS `surfacetreatment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `surfacetreatment` (
  `PS_SurfaceTreatmentID` mediumint(9) NOT NULL AUTO_INCREMENT,
  `Bezeichner` varchar(256) DEFAULT NULL,
  `PS_FMPSurfaceTreatmentID` mediumint(9) NOT NULL DEFAULT '-1',
  `ConnectedActions` text,
  `DatensatzGruppeSurfacetreatment` varchar(128) NOT NULL DEFAULT 'ceramalex',
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`PS_SurfaceTreatmentID`)
) ENGINE=InnoDB AUTO_INCREMENT=483 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'NO_AUTO_VALUE_ON_ZERO' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `createEntityIDSurfacetreatment` AFTER INSERT ON `surfacetreatment` FOR EACH ROW BEGIN 
INSERT INTO  `arachneentityidentification` 
SET  `arachneentityidentification`.`TableName`='surfacetreatment', `arachneentityidentification`.`ForeignKey` = NEW.`PS_SurfaceTreatmentID` ;
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
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `DeleteCrossTableReferenceST` AFTER DELETE ON `surfacetreatment` FOR EACH ROW BEGIN
DELETE FROM `xsurfacetreatmentx` WHERE `xsurfacetreatmentx`.`FS_SurfaceTreatmentID` = OLD.`PS_SurfaceTreatmentID`;
DELETE FROM `surfacetreatmentaction` WHERE `surfacetreatmentaction`.`FS_SurfaceTreatmentID` = OLD.`PS_SurfaceTreatmentID`;
UPDATE `literaturzitat` SET `FS_SurfaceTreatmentID` = NULL WHERE `literaturzitat`.`FS_SurfaceTreatmentID` = OLD.`PS_SurfaceTreatmentID`;
UPDATE `arachneentityidentification` SET `isDeleted` = 1
WHERE `arachneentityidentification`.`ForeignKey` = OLD.`PS_SurfaceTreatmentID` AND `arachneentityidentification`.`TableName` = "surfacetreatment";
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `surfacetreatmentaction`
--

DROP TABLE IF EXISTS `surfacetreatmentaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `surfacetreatmentaction` (
  `PS_SurfaceTreatmentActionID` int(11) NOT NULL AUTO_INCREMENT,
  `PS_FMPSurfaceTreatmentActionID` int(11) NOT NULL DEFAULT '-1',
  `FS_SurfaceTreatmentID` int(11) NOT NULL,
  `TreatmentPosition` varchar(256) DEFAULT NULL,
  `Quality` varchar(256) DEFAULT NULL,
  `Homogenity` varchar(256) DEFAULT NULL,
  `Adherence` varchar(256) DEFAULT NULL,
  `PartOfSurfaceTreated` varchar(256) DEFAULT NULL,
  `MomentOfSurfaceTreatmentAction` varchar(256) DEFAULT NULL,
  `Alignment` varchar(256) DEFAULT NULL,
  `TreatmentAction` varchar(256) DEFAULT NULL,
  `TreatmentActionSubCoating` varchar(256) DEFAULT NULL,
  `Condition` varchar(256) DEFAULT NULL,
  `ColourFreeQualifierVon` varchar(256) DEFAULT NULL,
  `ColourFreeVon` varchar(256) DEFAULT NULL,
  `ColourFreeQualifierBis` varchar(256) DEFAULT NULL,
  `ColourFreeBis` varchar(256) DEFAULT NULL,
  `ColourMunsellVon` varchar(256) DEFAULT NULL,
  `ColourMunsellBis` varchar(256) DEFAULT NULL,
  `Thickness` varchar(256) DEFAULT NULL,
  `Glossiness` varchar(256) DEFAULT NULL,
  `Conservation` varchar(256) DEFAULT NULL,
  `AddedMaterialType` varchar(256) DEFAULT NULL,
  `StampedStamp` varchar(256) DEFAULT NULL,
  `NumberOfSherds` varchar(64) DEFAULT NULL,
  `FreeDescription` text,
  `IncisionDescription` text,
  `DatensatzGruppeSurfacetreatmentaction` varchar(128) NOT NULL DEFAULT 'ceramalex',
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`PS_SurfaceTreatmentActionID`)
) ENGINE=InnoDB AUTO_INCREMENT=600 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'NO_AUTO_VALUE_ON_ZERO' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `createEntityIDSurfaceTreatmentAction` AFTER INSERT ON `surfacetreatmentaction` FOR EACH ROW BEGIN 
INSERT INTO  `arachneentityidentification` 
SET  `arachneentityidentification`.`TableName`='surfacetreatmentaction', `arachneentityidentification`.`ForeignKey` = NEW.`PS_SurfaceTreatmentActionID` ;
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
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `DeletesurfacetreatmentactionConnections` AFTER DELETE ON `surfacetreatmentaction` FOR EACH ROW BEGIN
UPDATE `arachneentityidentification` SET `isDeleted` = 1
WHERE `arachneentityidentification`.`ForeignKey` = OLD.`PS_SurfaceTreatmentActionID` AND `arachneentityidentification`.`TableName` = "surfacetreatmentaction";
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `xmorphologyx`
--

DROP TABLE IF EXISTS `xmorphologyx`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `xmorphologyx` (
  `PS_XMorphologyXID` int(11) NOT NULL AUTO_INCREMENT,
  `FS_MainAbstractID` int(11) NOT NULL,
  `FS_MorphologyID` int(11) NOT NULL,
  `PS_FMPXMorphologyXID` int(11) NOT NULL DEFAULT '-1',
  `Level1` varchar(512) DEFAULT NULL,
  `Level2` varchar(512) DEFAULT NULL,
  `Level3` varchar(512) DEFAULT NULL,
  `Level4` varchar(512) DEFAULT NULL,
  `Level5` varchar(512) DEFAULT NULL,
  `Level6` varchar(512) DEFAULT NULL,
  `Level1_6` varchar(256) DEFAULT NULL,
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`PS_XMorphologyXID`)
) ENGINE=InnoDB AUTO_INCREMENT=9854 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `createEntityIDXMorphologyX` AFTER INSERT ON `xmorphologyx` FOR EACH ROW BEGIN 
INSERT INTO  `arachneentityidentification` 
SET  `arachneentityidentification`.`TableName`='xmorphologyx', `arachneentityidentification`.`ForeignKey` = NEW.`PS_XMorphologyXID` ;
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
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `DeleteXMorphologyXConnections` AFTER DELETE ON `xmorphologyx` FOR EACH ROW BEGIN
UPDATE `arachneentityidentification` SET `isDeleted` = 1
WHERE `arachneentityidentification`.`ForeignKey` = OLD.`PS_XMorphologyXID` AND `arachneentityidentification`.`TableName` = "xmorphologyx";
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `xortx`
--

DROP TABLE IF EXISTS `xortx`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `xortx` (
  `PS_XOrtIDX` int(11) NOT NULL AUTO_INCREMENT,
  `PS_FMPXOrtIDX` int(11) NOT NULL DEFAULT '-1',
  `FS_OrtID` int(11) NOT NULL,
  `RoughAllocation` varchar(256) DEFAULT NULL,
  `Continent` varchar(256) DEFAULT NULL,
  `Country` varchar(256) DEFAULT NULL,
  `Region` varchar(256) DEFAULT NULL,
  `Subregion` varchar(256) DEFAULT NULL,
  `City` varchar(256) DEFAULT NULL,
  `AncientName` varchar(256) DEFAULT NULL,
  `Location` varchar(256) DEFAULT NULL,
  `Longitude` varchar(256) DEFAULT NULL,
  `Latitude` varchar(256) DEFAULT NULL,
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`PS_XOrtIDX`)
) ENGINE=InnoDB AUTO_INCREMENT=81 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `createEntityIDXOrtX` AFTER INSERT ON `xortx` FOR EACH ROW BEGIN 
INSERT INTO  `arachneentityidentification` 
SET  `arachneentityidentification`.`TableName`='xortx', `arachneentityidentification`.`ForeignKey` = NEW.`PS_XOrtIDX` ;
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
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `DeletexortxConnections` AFTER DELETE ON `xortx` FOR EACH ROW BEGIN
UPDATE `arachneentityidentification` SET `isDeleted` = 1
WHERE `arachneentityidentification`.`ForeignKey` = OLD.`PS_XOrtIDX` AND `arachneentityidentification`.`TableName` = "xortx";
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `xsherdtovesselx`
--

DROP TABLE IF EXISTS `xsherdtovesselx`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `xsherdtovesselx` (
  `PS_XSherdToVesselXID` int(11) NOT NULL AUTO_INCREMENT,
  `PS_FMPSherdToVesselID` int(11) DEFAULT '-1',
  `FS_IsolatedSherdID` int(11) NOT NULL,
  `FS_IndividualVesselID` int(11) NOT NULL,
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`PS_XSherdToVesselXID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `createEntityIDxsherdtovesselx` AFTER INSERT ON `xsherdtovesselx` FOR EACH ROW BEGIN 
INSERT INTO  `arachneentityidentification` 
SET  `arachneentityidentification`.`TableName`='xsherdtovesselx', `arachneentityidentification`.`ForeignKey` = NEW.`PS_XSherdToVesselXID` ;
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
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `DeletexsherdtovesselxConnections` AFTER DELETE ON `xsherdtovesselx` FOR EACH ROW BEGIN
UPDATE `arachneentityidentification` SET `isDeleted` = 1
WHERE `arachneentityidentification`.`ForeignKey` = OLD.`PS_XSherdToVesselXID` AND `arachneentityidentification`.`TableName` = "xsherdtovesselx";
END */;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Table structure for table `xsurfacetreatmentx`
--

DROP TABLE IF EXISTS `xsurfacetreatmentx`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `xsurfacetreatmentx` (
  `PS_XSurfacetreatmentXID` int(11) NOT NULL AUTO_INCREMENT,
  `FS_SurfaceTreatmentID` int(11) NOT NULL,
  `FS_MainAbstractID` int(11) DEFAULT NULL,
  `FS_IsolatedSherdID` int(11) DEFAULT NULL,
  `lastModified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `PS_FMPXSurfaceTreatmentXID` int(11) DEFAULT NULL,
  PRIMARY KEY (`PS_XSurfacetreatmentXID`)
) ENGINE=InnoDB AUTO_INCREMENT=2417 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8 */ ;
/*!50003 SET character_set_results = utf8 */ ;
/*!50003 SET collation_connection  = utf8_general_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `createEntityIDxsurfacetreatmentx` AFTER INSERT ON `xsurfacetreatmentx` FOR EACH ROW BEGIN 
INSERT INTO  `arachneentityidentification` 
SET  `arachneentityidentification`.`TableName`='xsurfacetreatmentx', `arachneentityidentification`.`ForeignKey` = NEW.`PS_XSurfaceTreatmentXID` ;
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
/*!50003 CREATE*/ /*!50017 DEFINER=`root`@`localhost`*/ /*!50003 TRIGGER `DeleteXSurfaceTreatmentXConnections` AFTER DELETE ON `xsurfacetreatmentx` FOR EACH ROW BEGIN
UPDATE `arachneentityidentification` SET `isDeleted` = 1
WHERE `arachneentityidentification`.`ForeignKey` = OLD.`PS_XSurfaceTreatmentXID` AND `arachneentityidentification`.`TableName` = "xsurfacetreatmentx";
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

-- Dump completed on 2017-05-04 17:15:18
