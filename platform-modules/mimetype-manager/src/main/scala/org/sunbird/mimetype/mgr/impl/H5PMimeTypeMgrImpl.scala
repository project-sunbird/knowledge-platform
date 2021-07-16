package org.sunbird.mimetype.mgr.impl

import java.io.File
import java.util.concurrent.CompletionException

import org.apache.commons.lang3.StringUtils
import org.sunbird.models.UploadParams
import org.sunbird.cloudstore.StorageService
import org.sunbird.common.Slug
import org.sunbird.common.exception.ClientException
import org.sunbird.graph.OntologyEngineContext
import org.sunbird.graph.dac.model.Node
import org.sunbird.mimetype.mgr.{BaseMimeTypeManager, MimeTypeManager}
import org.sunbird.telemetry.logger.TelemetryManager

import scala.concurrent.{ExecutionContext, Future}

class H5PMimeTypeMgrImpl(implicit ss: StorageService) extends BaseMimeTypeManager()(ss) with MimeTypeManager {

    override def upload(objectId: String, node: Node, uploadFile: File, filePath: Option[String], params: UploadParams)(implicit ec: ExecutionContext): Future[Map[String, AnyRef]] = {
        validateUploadRequest(objectId, node, uploadFile)
        val validationParams = if (StringUtils.equalsIgnoreCase(params.fileFormat.getOrElse(""), COMPOSED_H5P_ZIP))
            List[String]("/content/h5p.json", "content/h5p.json") else List[String]("h5p.json", "/h5p.json")
        if (isValidPackageStructure(uploadFile, validationParams)) {
            val extractionBasePath = getBasePath(objectId)
            val zipFile = if (!StringUtils.equalsIgnoreCase(params.fileFormat.getOrElse(""), COMPOSED_H5P_ZIP)) {
                val zippedFileName = createH5PZipFile(extractionBasePath, uploadFile, objectId)
                new File(zippedFileName)
            } else {
                extractPackage(uploadFile, extractionBasePath)
                uploadFile
            }
            val urls: Array[String] = uploadArtifactToCloud(zipFile, objectId, filePath)
            if (zipFile.exists) zipFile.delete
            Future {
                extractH5PPackageInCloud(objectId, extractionBasePath, node, "snapshot", false).map(resp =>
                    TelemetryManager.info("H5P content snapshot folder upload success for " + objectId)
                ) onFailure { case e: Throwable =>
                    TelemetryManager.error("H5P content snapshot folder upload failed for " + objectId, e.getCause)
                }
            }
            Future { Map[String, AnyRef]("identifier" -> objectId, "artifactUrl" -> urls(IDX_S3_URL), "size" -> getFileSize(uploadFile).asInstanceOf[AnyRef], "s3Key" -> urls(IDX_S3_KEY)) }
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

    /**
      *
      * @param extractionBasePath
      * @param uploadFiled
      * @param objectId
      * @return
      */
    def createH5PZipFile(extractionBasePath: String, uploadFiled: File, objectId: String): String = {
        // Download the H5P Libraries and Un-Zip the H5P Library Files
        extractH5pPackage(objectId, extractionBasePath)
        // UnZip the Content Package
        extractPackage(uploadFiled, extractionBasePath + File.separator + "content")
        // Create 'ZIP' Package
        val zipFileName = extractionBasePath + File.separator + System.currentTimeMillis + "_" + Slug.makeSlug(objectId) + FILENAME_EXTENSION_SEPARATOR + DEFAULT_ZIP_EXTENSION
        createZipPackage(extractionBasePath, zipFileName)
        zipFileName
    }

    def copyH5P(uploadFile: File, node: Node)(implicit ec: ExecutionContext): Future[Map[String, AnyRef]] = {
        val objectId: String = node.getIdentifier
        val extractionBasePath = getBasePath(objectId)
        extractPackage(uploadFile, extractionBasePath)
        val urls: Array[String] = uploadArtifactToCloud(uploadFile, objectId, None)
        node.getMetadata.put("s3Key", urls(IDX_S3_KEY))
        node.getMetadata.put("artifactUrl", urls(IDX_S3_URL))
        extractH5PPackageInCloud(objectId, extractionBasePath, node, "snapshot", false).map(resp =>
            Map[String, AnyRef]("identifier" -> objectId, "artifactUrl" -> urls(IDX_S3_URL), "size" -> getFileSize(uploadFile).asInstanceOf[AnyRef], "s3Key" -> urls(IDX_S3_KEY))
        ) recoverWith { case e: CompletionException => throw e.getCause }
    }

    override def review(objectId: String, node: Node)(implicit ec: ExecutionContext, ontologyEngineContext: OntologyEngineContext): Future[Map[String, AnyRef]] = {
        validate(node, "| [H5P file should be uploaded for further processing!]")
        Future(getEnrichedMetadata(node.getMetadata.getOrDefault("status", "").asInstanceOf[String]))
    }
}
