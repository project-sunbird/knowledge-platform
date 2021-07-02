package org.sunbird.content.util

import org.apache.commons.lang.StringUtils
import org.sunbird.common.dto.{Request, Response, ResponseHandler}
import org.sunbird.common.exception.{ResourceNotFoundException, ResponseCode}
import org.sunbird.graph.OntologyEngineContext
import org.sunbird.graph.nodes.DataNode

import java.util.concurrent.CompletionException
import scala.concurrent.{ExecutionContext, Future}

object RejectFlagManager {

  def rejectFlag(request: Request)(implicit ec: ExecutionContext, oec: OntologyEngineContext): Future[Response] = {
    DataNode.read(request).map(node => {
      if (!ContentConstants.VALID_FLAG_OBJECT_TYPES.contains(node.getObjectType)) {
        throw new ResourceNotFoundException("RESOURCE_NOT_FOUND", s"${node.getObjectType} ${node.getIdentifier} not found")
      }
      if (StringUtils.equals(ContentConstants.FLAGGED, node.getMetadata.getOrDefault(ContentConstants.STATUS, "").asInstanceOf[String])) {
        request.getRequest.put("flagReasons", null)
        request.getRequest.put(ContentConstants.STATUS, ContentConstants.LIVE)
        request.getRequest.put("versionKey", node.getMetadata.get("versionKey"))
        DataNode.update(request).map(node => {
          ResponseHandler.OK().put(ContentConstants.NODE_ID, node.getIdentifier).put(ContentConstants.IDENTIFIER, node.getIdentifier)
            .put(ContentConstants.VERSION_KEY, node.getMetadata.get(ContentConstants.VERSION_KEY))
        })
      } else {
        Future(ResponseHandler.ERROR(ResponseCode.CLIENT_ERROR, ContentConstants.ERR_INVALID_CONTENT, "Invalid Flagged Content! Content Can Not Be Rejected."))
      }
    }).flatMap(f => f) recoverWith { case e: CompletionException => throw e.getCause }
  }

}


