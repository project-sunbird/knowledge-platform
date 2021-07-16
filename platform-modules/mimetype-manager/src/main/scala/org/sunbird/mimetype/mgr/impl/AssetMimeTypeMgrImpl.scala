package org.sunbird.mimetype.mgr.impl

import java.io.File

import org.sunbird.cloudstore.StorageService
import org.sunbird.common.exception.ClientException
import org.sunbird.graph.OntologyEngineContext
import org.sunbird.graph.dac.model.Node
import org.sunbird.mimetype.mgr.{BaseMimeTypeManager, MimeTypeManager}
import org.sunbird.models.UploadParams
import org.sunbird.telemetry.logger.TelemetryManager

import scala.concurrent.{ExecutionContext, Future}

class AssetMimeTypeMgrImpl(implicit ss: StorageService) extends BaseMimeTypeManager with MimeTypeManager {

	override def upload(objectId: String, node: Node, uploadFile: File, filePath: Option[String], params: UploadParams)(implicit ec: ExecutionContext): Future[Map[String, AnyRef]] = {
		validateUploadRequest(objectId, node, uploadFile)
		val nodeMimeType = node.getMetadata.getOrDefault("mimeType", "").asInstanceOf[String]
		if (!isValidMimeType(uploadFile, nodeMimeType)) {
			TelemetryManager.log("Uploaded File MimeType is not same as Asset MimeType. [Asset MimeType: " + nodeMimeType + "]")
			if(mimeTypesToValidate.contains(nodeMimeType) || mimeTypesToValidate.contains(getFileMimeType(uploadFile)))
				throw new ClientException("VALIDATION_ERROR", "Uploaded File MimeType is not same as Asset MimeType.")
		}
		val result: Array[String] = uploadArtifactToCloud(uploadFile, objectId, filePath)
		//TODO: depreciate s3Key. use cloudStorageKey instead
		Future {
			Map("identifier" -> objectId, "artifactUrl" -> result(1), "downloadUrl" -> result(1), "cloudStorageKey" -> result(0), "s3Key" -> result(0), "size" -> getCloudStoredFileSize(result(0)).asInstanceOf[AnyRef])
		}
	}

	override def upload(objectId: String, node: Node, fileUrl: String, filePath: Option[String], params: UploadParams)(implicit ec: ExecutionContext): Future[Map[String, AnyRef]] = {
		validateUploadRequest(objectId, node, fileUrl)
		Future {Map[String, AnyRef]("identifier" -> objectId, "artifactUrl" -> fileUrl, "downloadUrl" -> fileUrl,"size" -> getMetadata(fileUrl).get("Content-Length"))}
	}

	override def review(objectId: String, node: Node)(implicit ec: ExecutionContext, ontologyEngineContext: OntologyEngineContext): Future[Map[String, AnyRef]] = {
		Future(getEnrichedMetadata(node.getMetadata.getOrDefault("status", "").asInstanceOf[String]))
	}

}
