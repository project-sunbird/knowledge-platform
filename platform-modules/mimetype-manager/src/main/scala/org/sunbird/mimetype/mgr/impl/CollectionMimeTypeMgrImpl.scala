package org.sunbird.mimetype.mgr.impl

import java.io.File

import org.apache.commons.lang3.StringUtils
import org.sunbird.models.UploadParams
import org.sunbird.cloudstore.StorageService
import org.sunbird.common.Platform
import org.sunbird.common.dto.{Request, ResponseHandler}
import org.sunbird.common.exception.{ClientException, ServerException}
import org.sunbird.graph.OntologyEngineContext
import org.sunbird.graph.dac.model.Node
import org.sunbird.graph.nodes.DataNode
import org.sunbird.graph.utils.ScalaJsonUtils
import org.sunbird.mimetype.mgr.{BaseMimeTypeManager, MimeTypeManager}

import scala.collection.JavaConverters._
import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}

class CollectionMimeTypeMgrImpl(implicit ss: StorageService) extends BaseMimeTypeManager with MimeTypeManager {

	private val validResourceStatus = List("Live", "Unlisted")

	override def upload(objectId: String, node: Node, uploadFile: File, filePath: Option[String], params: UploadParams)(implicit ec: ExecutionContext): Future[Map[String, AnyRef]] = {
		throw new ClientException("UPLOAD_DENIED", UPLOAD_DENIED_ERR_MSG)
	}

	override def upload(objectId: String, node: Node, fileUrl: String, filePath: Option[String], params: UploadParams)(implicit ec: ExecutionContext): Future[Map[String, AnyRef]] = {
		throw new ClientException("UPLOAD_DENIED", UPLOAD_DENIED_ERR_MSG)
	}

	override def review(objectId: String, node: Node)(implicit ec: ExecutionContext, ontologyEngineContext: OntologyEngineContext): Future[Map[String, AnyRef]] = {
		validate(node).map(res => getEnrichedMetadata(node.getMetadata.getOrDefault("status", "").asInstanceOf[String]))
	}

	def validate(node: Node)(implicit ec: ExecutionContext, ontologyEngineContext: OntologyEngineContext) = {
		val req = new Request()
		req.setContext(Map[String, AnyRef]("schemaName" -> node.getObjectType.toLowerCase.replaceAll("image", ""), "version"->"1.0").asJava)
		req.put("identifier", node.getIdentifier)
		getCollectionHierarchy(req, node).map(hierarchyString => {
			val childMap = getChildren(hierarchyString.asInstanceOf[String])
			val readReq = new Request(req)
			readReq.getContext.put("graph_id", "domain")
			readReq.put("identifiers", childMap.keySet.toList.asJava)
			DataNode.list(readReq).map(nodes => {
				if (nodes.size() != childMap.keySet.size) {
					val filteredList = childMap.keySet.toList.filter(id => !nodes.contains(id))
					throw new ClientException("ERR_COLLECTION_REVIEW", "Children which are not available are: " + filteredList)
				} else {
					val fNodes: List[String] = nodes.filter(node => !validResourceStatus.contains(node.getMetadata.getOrDefault("status", "").asInstanceOf[String])).toList.map(node => node.getIdentifier)
					if (Platform.getBoolean("collection.children_status_validation", true) && fNodes.nonEmpty)
						throw new ClientException("ERR_COLLECTION_REVIEW", "Unpublished Children Found With Identifier : " + fNodes)
				}
				true
			})
		}).flatMap(f => f)
	}

	def getCollectionHierarchy(request: Request, rootNode: Node)(implicit ec: ExecutionContext, oec: OntologyEngineContext): Future[Any] = {
		oec.graphService.readExternalProps(request, List("hierarchy")).flatMap(response => {
			if (ResponseHandler.checkError(response) && !ResponseHandler.isResponseNotFoundError(response)) {
				throw new ServerException("ERR_COLLECTION_REVIEW", "Unable to fetch hierarchy for identifier:" + rootNode.getIdentifier)
			} else if (ResponseHandler.checkError(response) && ResponseHandler.isResponseNotFoundError(response)) {
				throw new ClientException("ERR_COLLECTION_REVIEW", "No hierarchy is present in external-store for identifier:" + rootNode.getIdentifier)
			} else Future(response.getResult.toMap.getOrElse("hierarchy", "{}").asInstanceOf[String])
		})
	}

	def getChildren(hierarchyStr: String): Map[String, AnyRef] = {
		val hierarchy: Map[String, AnyRef] = if (hierarchyStr.asInstanceOf[String].nonEmpty) {
			ScalaJsonUtils.deserialize[Map[String, AnyRef]](hierarchyStr)
		} else Map()
		val children = hierarchy.getOrElse("children", List()).asInstanceOf[List[Map[String, AnyRef]]]
		getChildrenMap(children, Map())
	}

	def getChildrenMap(children: List[Map[String, AnyRef]], childrenMap: Map[String, AnyRef]): Map[String, AnyRef] = {
		children.flatMap(child => {
			val visibility = child.getOrElse("visibility", "").asInstanceOf[String]
			val updatedChildrenMap: Map[String, AnyRef] =
				if (StringUtils.equalsIgnoreCase("Default", visibility)) {
					Map(child.getOrElse("identifier", "").asInstanceOf[String] -> child) ++ childrenMap
				} else childrenMap
			val nextChild: List[Map[String, AnyRef]] = if(StringUtils.equalsIgnoreCase("Parent", visibility)) child.getOrElse("children", List()).asInstanceOf[List[Map[String, AnyRef]]] else List()
			val map = getChildrenMap(nextChild, updatedChildrenMap)
			map ++ updatedChildrenMap
		}).toMap
	}
}
