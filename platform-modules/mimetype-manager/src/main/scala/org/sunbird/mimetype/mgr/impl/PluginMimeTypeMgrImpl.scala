package org.sunbird.mimetype.mgr.impl

import java.io.File
import java.nio.charset.StandardCharsets

import org.apache.commons.io.FileUtils
import org.sunbird.models.UploadParams
import org.sunbird.common.JsonUtils
import org.sunbird.common.exception.ClientException
import org.sunbird.cloudstore.StorageService
import org.sunbird.graph.OntologyEngineContext
import org.sunbird.graph.dac.model.Node
import org.sunbird.graph.utils.ScalaJsonUtils
import org.sunbird.mimetype.mgr.{BaseMimeTypeManager, MimeTypeManager}

import scala.collection.JavaConverters
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

class PluginMimeTypeMgrImpl(implicit ss: StorageService) extends BaseMimeTypeManager with MimeTypeManager {
	private val DEF_CONTENT_PACKAGE_MIME_TYPE: String = "application/zip"
	
	override def upload(objectId: String, node: Node, uploadFile: File, filePath: Option[String], params: UploadParams)(implicit ec: ExecutionContext): Future[Map[String, AnyRef]] = {
		validateUploadRequest(objectId, node, uploadFile)
		validatePluginPackage(uploadFile)
		val basePath = getBasePath(objectId)
		extractPackage(uploadFile, basePath)
		val manifestFile = new File(basePath + File.separator + "manifest.json")
		val data:Map[String, AnyRef] = readDataFromManifest(manifestFile, objectId)
		FileUtils.deleteDirectory(new File(basePath))
		val result = uploadArtifactToCloud(uploadFile, objectId, filePath)
		extractPackageInCloud(objectId, uploadFile, node, "snapshot", true)
		Future{data ++ Map("identifier"->objectId,"artifactUrl" -> result(1), "cloudStorageKey" -> result(0), "s3Key" -> result(0))}
	}

	override def upload(objectId: String, node: Node, fileUrl: String, filePath: Option[String], params: UploadParams)(implicit ec: ExecutionContext): Future[Map[String, AnyRef]] = {
		validateUploadRequest(objectId, node, fileUrl)
		val file = copyURLToFile(objectId, fileUrl)
		upload(objectId, node, file, filePath, params)
	}

	def validatePluginPackage(uploadFile: File) = {
		if(!isValidMimeType(uploadFile, DEF_CONTENT_PACKAGE_MIME_TYPE)) throw new ClientException("VALIDATION_ERROR", "Error! Invalid Content Package Mime Type.")
		if(!isValidPackageStructure(uploadFile, List("manifest.json"))) throw new ClientException("VALIDATION_ERROR", "Error !Invalid Content Package File Structure. | [manifest.json should be at root location]" )
	}

	def readDataFromManifest(manifestFile: File, objectId: String): Map[String, AnyRef] = {
		if(manifestFile.exists()){
			val json: String = FileUtils.readFileToString(manifestFile, StandardCharsets.UTF_8)
			val dataMap: Map[String, AnyRef] = ScalaJsonUtils.deserialize[Map[String, AnyRef]](json)
			if(!objectId.contentEquals(dataMap.getOrElse("id", "").asInstanceOf[String]))
				throw new ClientException("ERR_INVALID_PLUGIN_ID", "'id' in manifest.json is not same as the plugin identifier.")
			val version = dataMap.getOrElse("ver", throw new ClientException("ERR_MISSING_VERSION", "'ver' is not specified in the plugin manifest.json."))
			val targets = dataMap.getOrElse("targets", List[AnyRef]())
			val targetList: java.util.List[Object] = {
				if(targets.isInstanceOf[String]) JsonUtils.deserialize(targets.asInstanceOf[String], classOf[java.util.List[Object]])
				else targets.asInstanceOf[List[AnyRef]].asJava.asInstanceOf[java.util.List[Object]]
			}
			Map[String, AnyRef]("semanticVersion" -> version, "targets" -> targetList)
		}else Map[String, AnyRef]()
	}

	override def review(objectId: String, node: Node)(implicit ec: ExecutionContext, ontologyEngineContext: OntologyEngineContext): Future[Map[String, AnyRef]] = {
		if(!isValidArtifact(node))
			throw new ClientException("VALIDATOR_ERROR", MISSING_REQUIRED_FIELDS + " | [Either artifactUrl is missing or invalid!]")
		Future(getEnrichedMetadata(node.getMetadata.getOrDefault("status", "").asInstanceOf[String]))
	}
}
