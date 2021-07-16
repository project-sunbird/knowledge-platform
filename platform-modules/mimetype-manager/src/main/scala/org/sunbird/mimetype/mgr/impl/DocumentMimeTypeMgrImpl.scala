package org.sunbird.mimetype.mgr.impl

import java.io.{File, IOException}
import java.util

import org.apache.commons.io.{FileUtils, FilenameUtils}
import org.apache.commons.lang3.StringUtils
import org.sunbird.models.UploadParams
import org.sunbird.cloudstore.StorageService
import org.sunbird.common.Platform
import org.sunbird.common.exception.ClientException
import org.sunbird.graph.OntologyEngineContext
import org.sunbird.graph.dac.model.Node
import org.sunbird.mimetype.mgr.{BaseMimeTypeManager, MimeTypeManager}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

class DocumentMimeTypeMgrImpl(implicit ss: StorageService) extends BaseMimeTypeManager with MimeTypeManager {

	val DEFAULT_ALLOWED_EXTENSIONS_WORD = util.Arrays.asList("doc", "docx", "ppt", "pptx", "key", "odp", "pps", "odt", "wpd", "wps", "wks")
	val ALLOWED_EXTENSIONS_WORD: List[String] = Platform.getStringList("mimetype.allowed_extensions.word", DEFAULT_ALLOWED_EXTENSIONS_WORD).asScala.toList

	override def upload(objectId: String, node: Node, uploadFile: File, filePath: Option[String], params: UploadParams)(implicit ec: ExecutionContext): Future[Map[String, AnyRef]] = {
		validateUploadRequest(objectId, node, uploadFile)
		val mimeType = node.getMetadata().getOrDefault("mimeType", "").asInstanceOf[String]
		validateFileExtension(mimeType, uploadFile)
		val file: File =
			if (StringUtils.equalsAnyIgnoreCase("application/epub", mimeType) && StringUtils.endsWith(uploadFile.getName(), ".epub")) {
				val basePath = getBasePath(objectId) + "/index.epub"
				val tempFile = new File(basePath)
				try FileUtils.moveFile(uploadFile, tempFile)
				catch {
					case e: IOException => e.printStackTrace()
				}
				tempFile
			} else uploadFile
		val result: Array[String] = uploadArtifactToCloud(file, objectId, filePath)
		//TODO: depreciate s3Key. use cloudStorageKey instead
		Future {
			Map("identifier" -> objectId, "artifactUrl" -> result(1), "cloudStorageKey" -> result(0), "s3Key" -> result(0), "size" -> getCloudStoredFileSize(result(0)).asInstanceOf[AnyRef])
		}
	}

	override def upload(objectId: String, node: Node, fileUrl: String, filePath: Option[String], params: UploadParams)(implicit ec: ExecutionContext): Future[Map[String, AnyRef]] = {
		validateUploadRequest(objectId, node, fileUrl)
		validateFileUrlExtension(node.getMetadata.getOrDefault("mimeType", "").asInstanceOf[String], fileUrl)
		Future {
			Map[String, AnyRef]("identifier" -> objectId, "artifactUrl" -> fileUrl, "downloadUrl" -> fileUrl, "size" -> getMetadata(fileUrl).get("Content-Length"))
		}
	}

	def validateFileExtension(mimeType: String, file: File) = {
		val fileType = getFileMimeType(file)
		val fileExt = FilenameUtils.getExtension(file.getPath)
		mimeType match {
			case "application/pdf" => {
				if (!(StringUtils.equalsIgnoreCase(fileExt, "pdf") && StringUtils.equals("application/pdf", fileType)))
					throw new ClientException("ERR_INVALID_FILE", "Uploaded file is not a pdf file. Please upload a valid pdf file.")
			}
			case "application/epub" => {
				if (!(StringUtils.equalsIgnoreCase(fileExt, "epub") && StringUtils.equals("application/epub+zip", fileType)))
					throw new ClientException("ERR_INVALID_FILE", "Uploaded file is not a epub file. Please upload a valid epub file.")
			}
			case "application/msword" => {
				if (!(StringUtils.isNotBlank(fileExt) && ALLOWED_EXTENSIONS_WORD.contains(fileExt)))
					throw new ClientException("ERR_INVALID_FILE", "Uploaded file is not a word file. Please upload a valid word file.")
			}
		}
	}

	def validateFileUrlExtension(mimeType: String, fileUrl: String) = {
		val fileExt = FilenameUtils.getExtension(fileUrl)
		mimeType match {
			case "application/pdf" => {
				if (!StringUtils.equalsIgnoreCase(fileExt, "pdf"))
					throw new ClientException("ERR_INVALID_FILE_URL", "Please Provide Valid Pdf File Url!")
			}
			case "application/epub" => {
				if (!StringUtils.equalsIgnoreCase(fileExt, "epub"))
					throw new ClientException("ERR_INVALID_FILE_URL", "Please Provide Valid Epub File Url!")
			}
			case "application/msword" => {
				if (!(StringUtils.isNotBlank(fileExt) && ALLOWED_EXTENSIONS_WORD.contains(fileExt)))
					throw new ClientException("ERR_INVALID_FILE_URL", "Please Provide Valid Document File Url!")
			}
		}
	}

	override def review(objectId: String, node: Node)(implicit ec: ExecutionContext, ontologyEngineContext: OntologyEngineContext): Future[Map[String, AnyRef]] = {
		validate(node, " | [Either artifactUrl is missing or invalid!]")
		Future(getEnrichedMetadata(node.getMetadata.getOrDefault("status", "").asInstanceOf[String]))
	}

}
