package org.sunbird.mimetype.mgr

import java.io.File

import org.sunbird.graph.OntologyEngineContext
import org.sunbird.models.UploadParams
import org.sunbird.graph.dac.model.Node

import scala.concurrent.{ExecutionContext, Future}

trait MimeTypeManager {

	@throws[Exception]
	def upload(objectId: String, node: Node, uploadFile: File, filePath: Option[String], params: UploadParams)(implicit ec: ExecutionContext): Future[Map[String, AnyRef]]

	@throws[Exception]
	def upload(objectId: String, node: Node, fileUrl: String, filePath: Option[String], params: UploadParams)(implicit ec: ExecutionContext): Future[Map[String, AnyRef]]

	@throws[Exception]
	def review(objectId: String, node: Node)(implicit ec: ExecutionContext, ontologyEngineContext: OntologyEngineContext): Future[Map[String, AnyRef]]
}
