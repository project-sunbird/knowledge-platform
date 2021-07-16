package org.sunbird.mimetype.mgr.impl

import java.io.File
import java.util.regex.Pattern

import org.apache.commons.lang3.StringUtils
import org.sunbird.models.UploadParams
import org.sunbird.cloudstore.StorageService
import org.sunbird.common.exception.ClientException
import org.sunbird.graph.OntologyEngineContext
import org.sunbird.graph.dac.model.Node
import org.sunbird.mimetype.mgr.{BaseMimeTypeManager, MimeTypeManager}
import org.sunbird.url.mgr.impl.URLFactoryManager

import scala.concurrent.{ExecutionContext, Future}


class YouTubeMimeTypeMgrImpl(implicit ss: StorageService) extends BaseMimeTypeManager with MimeTypeManager {

	private val YOUTUBE_REGEX = "^(http(s)?:\\/\\/)?((w){3}.)?youtu(be|.be)?(\\.com)?\\/.+";

	override def upload(objectId: String, node: Node, uploadFile: File, filePath: Option[String], params: UploadParams)(implicit ec: ExecutionContext): Future[Map[String, AnyRef]] = {
		throw new ClientException("UPLOAD_DENIED", UPLOAD_DENIED_ERR_MSG)
	}

	override def upload(objectId: String, node: Node, fileUrl: String, filePath: Option[String], params: UploadParams)(implicit ec: ExecutionContext): Future[Map[String, AnyRef]] = {
		validateUploadRequest(objectId, node, fileUrl)
		Future{Map[String, AnyRef]("identifier" -> objectId, "artifactUrl" -> fileUrl)}
	}

	override def review(objectId: String, node: Node)(implicit ec: ExecutionContext, ontologyEngineContext: OntologyEngineContext): Future[Map[String, AnyRef]] = {
		validate(node)
		val license = node.getMetadata.getOrDefault("license", "").asInstanceOf[String]
		val licenseData = if(StringUtils.isBlank(license)) validateAndGetLicense(node) else Map()
		val data = getEnrichedMetadata(node.getMetadata.getOrDefault("status", "").asInstanceOf[String])
		Future(data ++ licenseData)
	}

	def validate(node: Node): Unit = {
		if(isValidArtifact(node)) {
			val isValidYouTubeUrl = Pattern.matches(YOUTUBE_REGEX, node.getMetadata.get("artifactUrl").toString)
			if(!isValidYouTubeUrl)
				throw new ClientException("VALIDATION_ERROR", "Invalid Youtube Url Detected!")
		} else throw new ClientException("VALIDATOR_ERROR", "Either artifactUrl is missing or invalid!")
	}

	def validateAndGetLicense(node: Node): Map[String, AnyRef] = {
		val urlMgr = URLFactoryManager.getUrlManager("youtube")
		val data = urlMgr.validateURL(node.getMetadata.getOrDefault("artifactUrl", "").asInstanceOf[String], "license")
		Map("license" -> data.getOrDefault("value", ""))
	}
}
