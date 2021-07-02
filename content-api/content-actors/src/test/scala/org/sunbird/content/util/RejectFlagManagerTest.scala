package org.sunbird.content.util

import java.util
import akka.actor.Props
import org.scalamock.scalatest.MockFactory
import org.sunbird.cloudstore.StorageService
import org.sunbird.common.dto.{Property, Request, Response}
import org.sunbird.common.exception.ResponseCode
import org.sunbird.content.actors.{BaseSpec, ContentActor}
import org.sunbird.graph.{GraphService, OntologyEngineContext}
import org.sunbird.graph.dac.model.Node

import scala.collection.JavaConversions.mapAsJavaMap
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RejectFlagManagerTest extends BaseSpec with MockFactory {

  "RejectFlagManager" should "return success response for rejectFlag for Resource" in {
    implicit val ss = mock[StorageService]
    implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
    val graphDB = mock[GraphService]
    (oec.graphService _).expects().returns(graphDB).anyNumberOfTimes()
    val node = getNode()
    (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(node)).anyNumberOfTimes()
    (graphDB.upsertNode(_:String, _: Node, _: Request)).expects(*, *, *).returns(Future(node)).anyNumberOfTimes()
    (graphDB.getNodeProperty(_: String, _: String, _: String)).expects(*, *, *).returns(Future(new Property("versionKey", new org.neo4j.driver.internal.value.StringValue("1234")))).anyNumberOfTimes()
    (graphDB.readExternalProps(_: Request, _: List[String])).expects(*, *).returns(Future(new Response())).anyNumberOfTimes()
    val request = getRequest()
    request.getContext.put("identifier","domain")
    request.getRequest.putAll(mapAsJavaMap(Map("identifier" -> "domain")))
    request.setOperation("rejectFlag")
    val response = callActor(request, Props(new ContentActor()))
    assert("successful".equals(response.getParams.getStatus))
  }

  it should "return resource not found exception for rejectFlag if object type is invalid" in {
    implicit val ss = mock[StorageService]
    implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
    val graphDB = mock[GraphService]
    (oec.graphService _).expects().returns(graphDB).anyNumberOfTimes()
    val node = getNode()
    node.setObjectType("Event")
    (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(node)).anyNumberOfTimes()
    (graphDB.upsertNode(_:String, _: Node, _: Request)).expects(*, *, *).returns(Future(node)).anyNumberOfTimes()
    (graphDB.getNodeProperty(_: String, _: String, _: String)).expects(*, *, *).returns(Future(new Property("versionKey", new org.neo4j.driver.internal.value.StringValue("1234")))).anyNumberOfTimes()
    (graphDB.readExternalProps(_: Request, _: List[String])).expects(*, *).returns(Future(new Response())).anyNumberOfTimes()
    val request = getRequest()
    request.getContext.put("identifier","domain")
    request.getRequest.putAll(mapAsJavaMap(Map("identifier" -> "domain")))
    request.setOperation("rejectFlag")
    val response = callActor(request, Props(new ContentActor()))
    assert("failed".equals(response.getParams.getStatus))
    assert(response.getResponseCode == ResponseCode.RESOURCE_NOT_FOUND)
  }

  it should "return client error for rejectFlag if status is not 'Flagged'" in {
    implicit val ss = mock[StorageService]
    implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
    val graphDB = mock[GraphService]
    (oec.graphService _).expects().returns(graphDB).anyNumberOfTimes()
    val node = getNode()
    node.getMetadata.put("status","Draft")
    (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(node)).anyNumberOfTimes()
    (graphDB.upsertNode(_:String, _: Node, _: Request)).expects(*, *, *).returns(Future(node)).anyNumberOfTimes()
    (graphDB.getNodeProperty(_: String, _: String, _: String)).expects(*, *, *).returns(Future(new Property("versionKey", new org.neo4j.driver.internal.value.StringValue("1234")))).anyNumberOfTimes()
    (graphDB.readExternalProps(_: Request, _: List[String])).expects(*, *).returns(Future(new Response())).anyNumberOfTimes()
    val request = getRequest()
    request.getContext.put("identifier","domain")
    request.getRequest.putAll(mapAsJavaMap(Map("identifier" -> "domain")))
    request.setOperation("rejectFlag")
    val response = callActor(request, Props(new ContentActor()))
    assert("failed".equals(response.getParams.getStatus))
    assert("ERR_INVALID_CONTENT".equals(response.getParams.getErr))
    assert("Invalid Flagged Content! Content Can Not Be Rejected.".equals(response.getParams.getErrmsg))
  }

  private def getRequest(): Request = {
    val request = new Request()
    request.setContext(new util.HashMap[String, AnyRef]() {
      {
        put("objectType", "Content")
        put("graph_id", "domain")
        put("version", "1.0")
        put("schemaName", "content")
      }
    })
    request
  }

  def getNode(): Node = {
    val node = new Node()
    node.setIdentifier("domain")
    node.setNodeType("DATA_NODE")
    node.setObjectType("Content")
    node.setMetadata(new util.HashMap[String, AnyRef]() {
      {
        put("name", "Domain")
        put("code", "domain")
        put("status", "Flagged")
        put("identifier", "domain")
        put("versionKey", "1234")
        put("contentType", "Resource")
        put("channel", "Test")
        put("mimeType", "application/pdf")
        put("primaryCategory", "Learning Resource")
      }
    })
    node
  }

}
