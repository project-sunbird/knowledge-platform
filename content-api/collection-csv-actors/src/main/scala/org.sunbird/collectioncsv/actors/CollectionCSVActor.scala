package org.sunbird.collectioncsv.actors

import org.sunbird.actor.core.BaseActor
import org.sunbird.collectioncsv.manager.CollectionCSVManager.{getCloudPath, readInputCSV, updateCollection, validateCollection}
import org.sunbird.collectioncsv.util.CollectionTOCConstants
import org.sunbird.collectioncsv.validator.CollectionCSVValidator.{collectionNodeIdentifierHeader, validateCSVHeadersFormat, validateCSVRecordsDataAuthenticity, validateCSVRecordsDataFormat}
import org.sunbird.common.{JsonUtils, Platform}
import org.sunbird.common.dto.{Request, Response, ResponseParams}
import org.sunbird.common.exception.{ClientException, ServerException}
import org.sunbird.graph.OntologyEngineContext
import org.sunbird.graph.utils.ScalaJsonUtils
import org.sunbird.managers.HierarchyManager
import org.sunbird.telemetry.logger.TelemetryManager

import java.io.IOException
import javax.inject.{Inject}
import scala.collection.JavaConversions.mapAsJavaMap
import scala.collection.JavaConverters.{mapAsJavaMapConverter, mapAsScalaMapConverter}
import scala.collection.immutable.{HashMap, Map}
import scala.concurrent.{ExecutionContext, Future}

class CollectionCSVActor @Inject() (implicit oec: OntologyEngineContext) extends BaseActor {

  implicit val ec: ExecutionContext = getContext().dispatcher

  override def onReceive(request: Request): Future[Response] = {
    request.getOperation match {
      case CollectionTOCConstants.COLLECTION_CSV_TOC_UPLOAD => uploadTOC(request)
      case CollectionTOCConstants.COLLECTION_CSV_TOC_DOWNLOAD => getTOCUrl(request)
      case _ => ERROR(request.getOperation)
    }
  }

  private def uploadTOC(request:Request): Future[Response] = {
    try {
      val csvFileParser = readInputCSV(request)
      try {
        val csvHeaders: Map[String, Integer] =  if (!csvFileParser.getHeaderMap.isEmpty) csvFileParser.getHeaderMap.asScala.toMap else HashMap.empty
        // Reading input CSV File - END
        val csvRecords = csvFileParser.getRecords

        //Check if CSV Headers are empty
        if (null == csvHeaders || csvHeaders.isEmpty) throw new ClientException("BLANK_CSV_DATA", "Did not find any Table of Contents data. Please check and upload again.")

        //Check if the input CSV is 'CREATE' TOC file format or 'UPDATE' TOC file format
        val mode = if (csvHeaders.containsKey(collectionNodeIdentifierHeader.head)) CollectionTOCConstants.UPDATE else CollectionTOCConstants.CREATE
        println("CollectionCSVActor --> uploadTOC --> mode identified: " + mode)
        val collectionId = request.get(CollectionTOCConstants.IDENTIFIER).asInstanceOf[String]
        println("CollectionCSVActor --> uploadTOC --> collectionId: " + collectionId)

        request.put("rootId",collectionId)
        request.put("mode","edit")
        request.put("fields", new java.util.ArrayList[String]())

        HierarchyManager.getHierarchy(request).flatMap(getHierarchyResponse => {
          val collectionHierarchyDeSer = ScalaJsonUtils.deserialize[Map[String, AnyRef]](JsonUtils.serialize(getHierarchyResponse))
          val collectionHierarchy = collectionHierarchyDeSer(CollectionTOCConstants.RESULT).asInstanceOf[Map[String, AnyRef]](CollectionTOCConstants.CONTENT).asInstanceOf[Map[String, AnyRef]]
          println("CollectionCSVActor --> uploadTOC --> after fetching collection Hierarchy: " + collectionHierarchy(CollectionTOCConstants.IDENTIFIER))

          // Validate if the mode is CREATE and children already exist in collection
          val children = collectionHierarchy(CollectionTOCConstants.CHILDREN).asInstanceOf[List[AnyRef]]
          if (mode.equals(CollectionTOCConstants.CREATE) && children.nonEmpty)
            throw new ClientException("COLLECTION_CHILDREN_EXISTS", "Collection is already having children.")
          println("CollectionCSVActor --> uploadTOC --> after Validating if the mode is CREATE and children already exist in collection")

          //Validate the headers format of the input CSV
          validateCSVHeadersFormat(csvHeaders, mode)
          println("CollectionCSVActor --> uploadTOC --> after validating CSV Headers format: ")

          //Validate the data format of the input CSV records

          validateCSVRecordsDataFormat(csvRecords, mode)
          println("CollectionCSVActor --> uploadTOC --> after validating CSV Records data format: ")

          val linkedContentsDetails: List[Map[String, AnyRef]] = {
            if (mode.equals(CollectionTOCConstants.UPDATE)) {
              // validate the data authenticity of the input CSV records' - Mapped Topics, QR Codes, Linked Contents
              validateCSVRecordsDataAuthenticity(csvRecords, collectionHierarchy)
            }
            else List.empty[Map[String, AnyRef]]
          }
          println("CollectionCSVActor --> uploadTOC --> after validating the data authenticity of the input CSV records' - Mapped Topics, QR Codes, Linked Contents: ")

          // update the collection hierarchy
          updateCollection(collectionHierarchy, csvRecords, mode, linkedContentsDetails)
        })
      } catch {
        case e: IllegalArgumentException =>
          println("CollectionCSVActor --> IllegalArgumentException: " + e.getMessage)
          throw new ClientException("CLIENT_ERROR", e.getMessage)
        case e: ClientException =>
          println("CollectionCSVActor --> ClientException: " + e.getMessage)
          throw e
        case e: Exception =>
          println("CollectionCSVActor --> Exception: " + e.getMessage)
          println("Exception" + e.getMessage)
          throw new ServerException("SERVER_ERROR","Something went wrong while processing the file")
      } finally {
        try if (null != csvFileParser) csvFileParser.close()
        catch {
          case e: IOException =>
            println("CollectionCSVActor:readAndValidateCSV : Exception occurred while closing stream" + e)
        }

      }
    } catch {
      case e: Exception =>
        throw new ClientException("CLIENT_ERROR", e.getMessage)
    }
  }

   private def getTOCUrl(request: Request): Future[Response] = {
     
     val collectionId = request.get("identifier").asInstanceOf[String]
     if (collectionId.isBlank) {
       println("CollectionCSVActor:getTOCUrl -> Invalid Collection Id Provided")
       throw new ClientException("INVALID_COLLECTION", "Invalid Collection. Please Provide Valid Collection Identifier.")
     }

     request.put("rootId",collectionId)
     request.put("mode","edit")
     request.put("fields", new java.util.ArrayList[String]())

     HierarchyManager.getHierarchy(request).map(getHierarchyResponse => {
       val collectionHierarchyDeSer = ScalaJsonUtils.deserialize[Map[String, AnyRef]] (JsonUtils.serialize (getHierarchyResponse) )
       val collectionHierarchy = collectionHierarchyDeSer (CollectionTOCConstants.RESULT).asInstanceOf[Map[String, AnyRef]] (CollectionTOCConstants.CONTENT).asInstanceOf[Map[String, AnyRef]]
       validateCollection (collectionHierarchy)

       val cloudPath = getCloudPath (collectionHierarchy)
       println ("CollectionCSVActor:getTOCUrl -> cloudPath: " + cloudPath)
       println ("CollectionCSVActor:getTOCUrl -> Sending Response for Toc Download API for Collection | Id: " + collectionId)
       val collectionCSV = HashMap[String, AnyRef] (CollectionTOCConstants.TOC_URL -> cloudPath, CollectionTOCConstants.TTL -> Platform.config.getString ("cloud_storage.upload.url.ttl") )

       val response = new Response
       val resParams = new ResponseParams
       resParams.setStatus ("successful")
       response.setParams (resParams)
       response.put(CollectionTOCConstants.COLLECTION, collectionCSV.asJava)
     })

  }

}