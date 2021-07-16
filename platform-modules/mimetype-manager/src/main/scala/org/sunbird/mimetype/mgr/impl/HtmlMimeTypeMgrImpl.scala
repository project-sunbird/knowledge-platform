package org.sunbird.mimetype.mgr.impl

import java.io.File

import org.sunbird.models.UploadParams
import org.sunbird.cloudstore.StorageService
import org.sunbird.common.exception.ClientException
import org.sunbird.graph.OntologyEngineContext
import org.sunbird.graph.dac.model.Node
import org.sunbird.mimetype.mgr.{BaseMimeTypeManager, MimeTypeManager}
import org.sunbird.telemetry.logger.TelemetryManager

import scala.concurrent.{ExecutionContext, Future}

class HtmlMimeTypeMgrImpl(implicit ss: StorageService) extends BaseMimeTypeManager with MimeTypeManager {

    override def upload(objectId: String, node: Node, uploadFile: File, filePath: Option[String], params: UploadParams)(implicit ec: ExecutionContext): Future[Map[String, AnyRef]] = {
        validateUploadRequest(objectId, node, uploadFile)
        if (isValidPackageStructure(uploadFile, List[String]("index.html"))) {
            val urls = uploadArtifactToCloud(uploadFile, objectId, filePath)
            node.getMetadata.put("s3Key", urls(IDX_S3_KEY))
            node.getMetadata.put("artifactUrl", urls(IDX_S3_URL))
            extractPackageInCloud(objectId, uploadFile, node, "snapshot", false)
            Future(Map[String, AnyRef]("identifier" -> objectId, "artifactUrl" -> urls(IDX_S3_URL), "s3Key" -> urls(IDX_S3_KEY), "size" -> getFileSize(uploadFile).asInstanceOf[AnyRef]))
        } else {
            TelemetryManager.error("ERR_INVALID_FILE" + "Please Provide Valid File! with file name: " + uploadFile.getName)
            throw new ClientException("ERR_INVALID_FILE", "Please Provide Valid File!")
        }

    }

    override def upload(objectId: String, node: Node, fileUrl: String, filePath: Option[String], params: UploadParams)(implicit ec: ExecutionContext): Future[Map[String, AnyRef]] = {
        validateUploadRequest(objectId, node, fileUrl)
        val file = copyURLToFile(objectId, fileUrl)
        upload(objectId, node, file, filePath, params)
    }

    override def review(objectId: String, node: Node)(implicit ec: ExecutionContext, ontologyEngineContext: OntologyEngineContext): Future[Map[String, AnyRef]] = {
        validate(node, " | [HTML archive should be uploaded for further processing!]")
        Future(getEnrichedMetadata(node.getMetadata.getOrDefault("status", "").asInstanceOf[String]))
    }
}
