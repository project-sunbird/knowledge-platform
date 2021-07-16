package org.sunbird.content.review.mgr

import org.sunbird.common.dto.{Request, Response, ResponseHandler}
import org.sunbird.graph.OntologyEngineContext
import org.sunbird.graph.dac.model.Node
import org.sunbird.graph.nodes.DataNode
import org.sunbird.mimetype.factory.MimeTypeManagerFactory

import scala.collection.Map
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

object ReviewManager {

	def review(request: Request, node: Node)(implicit oec: OntologyEngineContext, ec: ExecutionContext): Future[Response] = {
		val identifier: String = node.getIdentifier
		val mimeType = node.getMetadata().getOrDefault("mimeType", "").asInstanceOf[String]
		val mgr = MimeTypeManagerFactory.getManager(node.getObjectType, mimeType)
		val reviewFuture: Future[Map[String, AnyRef]] = mgr.review(identifier, node)
		reviewFuture.map(result => {
			val updateReq = new Request()
			updateReq.setContext(request.getContext)
			updateReq.putAll(result.asJava)
			DataNode.update(updateReq).map(node => {
				ResponseHandler.OK.putAll(Map("identifier" -> node.getIdentifier.replace(".img", ""), "versionKey" -> node.getMetadata.get("versionKey")).asJava)
			})
		}).flatMap(f => f)
	}
}


