package org.sunbird.mimetype.mgr.impl

import java.io.File

import org.sunbird.models.UploadParams
import org.sunbird.cloudstore.StorageService
import org.sunbird.common.exception.ClientException
import org.sunbird.graph.OntologyEngineContext
import org.sunbird.graph.dac.model.Node
import org.sunbird.mimetype.mgr.{BaseMimeTypeManager, MimeTypeManager}

import scala.concurrent.{ExecutionContext, Future}

class ApkMimeTypeMgrImpl(implicit ss: StorageService) extends BaseMimeTypeManager with MimeTypeManager {

	override def upload(objectId: String, node: Node, uploadFile: File, filePath: Option[String], params: UploadParams)(implicit ec: ExecutionContext): Future[Map[String, AnyRef]] = {
		validateUploadRequest(objectId, node, uploadFile)
		val result: Array[String] = uploadArtifactToCloud(uploadFile, objectId, filePath)
		Future {
			Map("identifier" -> objectId, "artifactUrl" -> result(1), "downloadUrl" -> result(1), "cloudStorageKey" -> result(0), "s3Key" -> result(0), "size" -> getCloudStoredFileSize(result(0)).asInstanceOf[AnyRef])
		}
	}

	override def upload(objectId: String, node: Node, fileUrl: String, filePath: Option[String], params: UploadParams)(implicit ec: ExecutionContext): Future[Map[String, AnyRef]] = {
		validateUploadRequest(objectId, node, fileUrl)
		val file = copyURLToFile(objectId, fileUrl)
		Future {
			Map[String, AnyRef]("identifier" -> objectId, "artifactUrl" -> fileUrl, "downloadUrl" -> fileUrl, "size" -> getFileSize(file).asInstanceOf[AnyRef])
		}
	}

	override def review(objectId: String, node: Node)(implicit ec: ExecutionContext, ontologyEngineContext: OntologyEngineContext): Future[Map[String, AnyRef]] = {
		validate(node, " |[APK file should be uploaded for further processing!]")
		Future(getEnrichedMetadata(node.getMetadata.getOrDefault("status", "").asInstanceOf[String]))
	}

}
