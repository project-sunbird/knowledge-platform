package org.sunbird.content.actors

import akka.actor.Props
import org.scalamock.scalatest.MockFactory
import org.sunbird.cloudstore.StorageService
import org.sunbird.common.dto.Request
import org.sunbird.common.exception.ResponseCode
import org.sunbird.graph.dac.model.{Node, SearchCriteria}
import org.sunbird.graph.{GraphService, OntologyEngineContext}
import java.util

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TestEventActor extends BaseSpec with MockFactory {

    it should "discard node in draft state should return success" in {
        implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
        val graphDB = mock[GraphService]
        (oec.graphService _).expects().returns(graphDB).anyNumberOfTimes()
        (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(getValidNodeToDiscard())).anyNumberOfTimes()
        (graphDB.deleteNode(_: String, _: String, _: Request)).expects(*, *, *).returns(Future(true))
        implicit val ss = mock[StorageService]
        val request = getContentRequest()
        request.getRequest.putAll(mapAsJavaMap(Map("identifier" -> "do_12346")))
        request.getContext.put("objectType","Content")
        request.setOperation("discardContent")
        val response = callActor(request, Props(new EventActor()))
        assert(response.getResponseCode == ResponseCode.OK)
        assert(response.get("identifier") == "do_12346")
        assert(response.get("message") == "Draft version of the content with id : do_12346 is discarded")

    }

    it should "publish node in draft state should return success" in {
        implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
        val graphDB = mock[GraphService]
        (oec.graphService _).expects().returns(graphDB).anyNumberOfTimes()
        (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(getDraftNode())).anyNumberOfTimes()
        (graphDB.upsertNode(_: String, _: Node, _: Request)).expects(*, *, *).returns(Future(getDraftNode()))

        val nodes: util.List[Node] = getCategoryNode()
        (graphDB.getNodeByUniqueIds(_: String, _: SearchCriteria)).expects(*, *).returns(Future(nodes)).anyNumberOfTimes()

        implicit val ss = mock[StorageService]
        val request = getContentRequest()
        request.getContext.put("identifier", "do_1234")
        request.getRequest.putAll(mapAsJavaMap(Map()))
        request.setOperation("publishContent")
        val response = callActor(request, Props(new EventActor()))
        assert(response.getResponseCode == ResponseCode.OK)
        assert(response.get("identifier") == "do_1234")

    }

    it should "discard node in Live state should return client error" in {
        implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
        val graphDB = mock[GraphService]
        (oec.graphService _).expects().returns(graphDB).anyNumberOfTimes()
        (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(getInValidNodeToDiscard())).anyNumberOfTimes()
        implicit val ss = mock[StorageService]
        val request = getContentRequest()
        request.getRequest.putAll(mapAsJavaMap(Map("identifier" -> "do_12346")))
        request.getContext.put("objectType","Content")
        request.setOperation("discardContent")
        val response = callActor(request, Props(new EventActor()))
        assert(response.getResponseCode == ResponseCode.CLIENT_ERROR)
    }

    it should "return client error response for retireContent" in {
        implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
        implicit val ss = mock[StorageService]
        val request = getContentRequest()
        request.getContext.put("identifier","do_1234.img")
        request.getRequest.putAll(mapAsJavaMap(Map("identifier" -> "do_1234.img")))
        request.getContext.put("objectType","Content")
        request.setOperation("retireContent")
        val graphDB = mock[GraphService]
        (oec.graphService _).expects().returns(graphDB)
        (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(getInValidNodeToDiscard()))
        val response = callActor(request, Props(new EventActor()))
        assert(response.getResponseCode == ResponseCode.CLIENT_ERROR)
    }

    it should "return success response for retireContent" in {
        implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
        val graphDB = mock[GraphService]
        (oec.graphService _).expects().returns(graphDB).anyNumberOfTimes()
        val node = getNode("Content", None)
        (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(node)).anyNumberOfTimes()
        (graphDB.updateNodes(_: String, _: util.List[String], _: util.HashMap[String, AnyRef])).expects(*, *, *).returns(Future(new util.HashMap[String, Node]))
        implicit val ss = mock[StorageService]
        val request = getContentRequest()
        request.getContext.put("identifier","do1234")
        request.getRequest.putAll(mapAsJavaMap(Map("identifier" -> "do_1234")))
        request.getContext.put("objectType","Content")
        request.setOperation("retireContent")
        val response = callActor(request, Props(new EventActor()))
        assert("successful".equals(response.getParams.getStatus))
    }

    it should "return success response for 'updateContent'" in {
        implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
        implicit val ss = mock[StorageService]
        val graphDB = mock[GraphService]
        (oec.graphService _).expects().returns(graphDB).anyNumberOfTimes()
        val node = getDraftNode()
        (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(node)).anyNumberOfTimes()

        val nodes: util.List[Node] = getCategoryNode()
        (graphDB.getNodeByUniqueIds(_: String, _: SearchCriteria)).expects(*, *).returns(Future(nodes)).anyNumberOfTimes()

        (graphDB.upsertNode(_: String, _: Node, _: Request)).expects(*, *, *).returns(Future(node))
        val request = getContentRequest()
        request.getContext.put("identifier","do_1234")
        request.putAll(mapAsJavaMap(Map("name" -> "New Content", "code" -> "1234",
            "startDate" ->  "2021-03-04", "endDate" -> "2021-03-04", "startTime" -> "11:00:00Z", "endTime" -> "11:00:00Z",
            "registrationEndDate" -> "2021-03-04", "eventType" -> "Online", "versionKey" -> "test_123")))
        request.setOperation("updateContent")
        val response = callActor(request, Props(new EventActor()))
        assert("successful".equals(response.getParams.getStatus))
        assert("do_1234".equals(response.get("identifier")))
        assert("test_123".equals(response.get("versionKey")))
    }

    private def getContentRequest(): Request = {
        val request = new Request()
        request.setContext(new util.HashMap[String, AnyRef]() {
            {
                put("graph_id", "domain")
                put("version", "1.0")
                put("objectType", "Event")
                put("schemaName", "event")
                put("X-Channel-Id", "in.ekstep")
            }
        })
        request.setObjectType("Event")
        request
    }

    private def getValidNodeToDiscard(): Node = {
        val node = new Node()
        node.setIdentifier("do_12346")
        node.setNodeType("DATA_NODE")
        node.setObjectType("Content")
        node.setMetadata(new util.HashMap[String, AnyRef]() {
            {
                put("identifier", "do_12346")
                put("mimeType", "application/pdf")
                put("status", "Draft")
                put("contentType", "Resource")
                put("name", "Node To discard")
                put("primaryCategory", "Learning Resource")
            }
        })
        node
    }

    private def getInValidNodeToDiscard(): Node = {
        val node = new Node()
        node.setIdentifier("do_12346")
        node.setNodeType("DATA_NODE")
        node.setObjectType("Content")
        node.setMetadata(new util.HashMap[String, AnyRef]() {
            {
                put("identifier", "do_12346")
                put("mimeType", "application/pdf")
                put("status", "Live")
                put("contentType", "Resource")
                put("name", "Node To discard")
                put("primaryCategory", "Learning Resource")
            }
        })
        node
    }

    private def getNode(): Node = {
        val node = new Node()
        node.setIdentifier("do_1234")
        node.setNodeType("DATA_NODE")
        node.setObjectType("Event")
        node.setMetadata(new util.HashMap[String, AnyRef]() {
            {
                put("identifier", "do_1234")
                put("status", "Live")
                put("name", "Resource_1")
                put("versionKey", "test_321")
                put("channel", "in.ekstep")
                put("code", "Resource_1")
                put("startDate", "2021-02-02")
                put("endDate", "2021-02-02")
                put("startTime", "11:00:00Z")
                put("endTime", "12:00:00Z")
                put("registrationEndDate", "2021-01-02")
                put("eventType", "Online")
            }
        })
        node
    }
    private def getDraftNode(): Node = {
        val node = new Node()
        node.setIdentifier("do_1234")
        node.setNodeType("DATA_NODE")
        node.setObjectType("Event")
        node.setMetadata(new util.HashMap[String, AnyRef]() {
            {
                //put("identifier", "do_1234")
                put("status", "Draft")
                put("name", "Resource_1")
                put("versionKey", "test_321")
                //put("channel", "in.ekstep")
                put("code", "Resource_1")
                put("startDate", "2021-02-02")
                put("endDate", "2021-02-02")
                put("startTime", "11:00:00Z")
                put("endTime", "12:00:00Z")
                put("registrationEndDate", "2021-01-02")
                put("eventType", "Online")
            }
        })
        node
    }
}