package org.sunbird.content.actors

import akka.actor.Props
import org.scalamock.scalatest.MockFactory
import org.sunbird.cloudstore.StorageService
import org.sunbird.common.JsonUtils
import org.sunbird.common.dto.{Request, Response}
import org.sunbird.common.exception.ResponseCode
import org.sunbird.graph.common.enums.GraphDACParams
import org.sunbird.graph.dac.model.{Node, Relation, SearchCriteria}
import org.sunbird.graph.{GraphService, OntologyEngineContext}

import java.util
import scala.collection.JavaConversions.mapAsJavaMap
import scala.collection.JavaConverters.seqAsJavaListConverter
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TestEventSetActor extends BaseSpec with MockFactory {

    "EventSetActor" should "return failed response for 'unknown' operation" in {
        implicit val ss = mock[StorageService]
        implicit val oec: OntologyEngineContext = new OntologyEngineContext
        testUnknownOperation(Props(new EventSetActor()), getContentRequest())
    }

    it should "validate input before creating event set" in {
        implicit val ss = mock[StorageService]
        implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
        val request = getContentRequest()
        val eventSet = mapAsJavaMap(Map(
            "name" -> "New Content", "code" -> "1234",
            "startDate"-> "2021/01/03", //wrong format
            "endDate"-> "2021-01-03",
            "schedule" ->
              mapAsJavaMap(Map("type" -> "NON_RECURRING",
                  "value" -> List(mapAsJavaMap(Map("startDate" -> "2021-01-03", "endDate" -> "2021-01-03", "startTime" -> "11:00:00Z", "endTime" -> "13:00:00Z"))).asJava)),
            "onlineProvider" -> "Zoom",
            "registrationEndDate" -> "2021-02-25",
            "eventType" -> "Online"))
        request.putAll(eventSet)
        assert(true)
        val response = callActor(request, Props(new EventSetActor()))
        println("Response: " + JsonUtils.serialize(response))
    }

    it should "create an eventset and store it in neo4j" in {
        val eventNode = getEventNode()
        val eventSetNode = getEventSetNode()
        enrichFrameworkMasterCategoryMap()
        implicit val ss = mock[StorageService]
        implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
        val graphDB = mock[GraphService]
        (oec.graphService _).expects().returns(graphDB).anyNumberOfTimes()
        (graphDB.addNode _).expects(where { (g: String, n:Node) => {
            n.getObjectType.equals("Event")
        }}).returns(Future(eventNode)).once()
        val loopResult: util.Map[String, Object] = new util.HashMap[String, Object]()
        loopResult.put(GraphDACParams.loop.name, new java.lang.Boolean(false))
        (graphDB.checkCyclicLoop _).expects(*, *, *, *).returns(loopResult).anyNumberOfTimes()
        (graphDB.addNode _).expects(where { (g: String, n:Node) => n.getObjectType.equals("EventSet")}).returns(Future(eventSetNode)).once()
        
        (graphDB.getNodeByUniqueIds(_: String, _: SearchCriteria)).expects(*, *).returns(Future(new util.ArrayList[Node]() {
            {
                add(eventNode)
            }
        })).anyNumberOfTimes()
        (graphDB.createRelation _).expects(*, *).returns(Future(new Response()))
        val request = getContentRequest()
        val eventSet = mapAsJavaMap(Map(
            "name" -> "New Content", "code" -> "1234",
            "startDate"-> "2021-01-03", //wrong format
            "endDate"-> "2021-01-03",
            "schedule" ->
              mapAsJavaMap(Map("type" -> "NON_RECURRING",
                  "value" -> List(mapAsJavaMap(Map("startDate" -> "2021-01-03", "endDate" -> "2021-01-03", "startTime" -> "11:00:00Z", "endTime" -> "13:00:00Z"))).asJava)),
            "onlineProvider" -> "Zoom",
            "registrationEndDate" -> "2021-02-25",
            "eventType" -> "Online"))
        request.putAll(eventSet)
        request.setOperation("createContent")
        val response = callActor(request, Props(new EventSetActor()))
        assert(response.get("identifier") != null)
        assert(response.get("versionKey") != null)
    }

    it should "update an eventset and store it in neo4j" in {
        val eventNode = getEventNode()
        val eventSetNode = getEventSetCollectionNode()
        enrichFrameworkMasterCategoryMap()
        implicit val ss = mock[StorageService]
        implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
        val graphDB = mock[GraphService]
        (oec.graphService _).expects().returns(graphDB).anyNumberOfTimes()
        (graphDB.deleteNode(_: String, _: String, _: Request)).expects(*, *, *).returns(Future(true))
        (graphDB.removeRelation(_: String, _: util.List[util.Map[String, AnyRef]])).expects(*, *).returns(Future(new Response))
        (graphDB.addNode _).expects(where { (g: String, n:Node) => {
            n.getObjectType.equals("Event")
        }}).returns(Future(eventNode))
        val loopResult: util.Map[String, Object] = new util.HashMap[String, Object]()
        loopResult.put(GraphDACParams.loop.name, new java.lang.Boolean(false))
        (graphDB.checkCyclicLoop _).expects(*, *, *, *).returns(loopResult).anyNumberOfTimes()
        (graphDB.upsertNode _).expects(*, *, *).returns(Future(eventSetNode))

        (graphDB.getNodeByUniqueIds(_: String, _: SearchCriteria)).expects(*, *).returns(Future(new util.ArrayList[Node]() {
            {
                add(eventNode)
            }
        })).noMoreThanOnce()
        (graphDB.getNodeByUniqueId _).expects(*, *, *, *).returns(Future(eventSetNode)).anyNumberOfTimes()
        (graphDB.createRelation _).expects(*, *).returns(Future(new Response()))


        val request = getContentRequest()
        val eventSet = mapAsJavaMap(Map(
            "name" -> "New Content", "code" -> "1234",
            "startDate"-> "2021-01-03", //wrong format
            "endDate"-> "2021-01-03",
            "schedule" ->
              mapAsJavaMap(Map("type" -> "NON_RECURRING",
                  "value" -> List(mapAsJavaMap(Map("startDate" -> "2021-01-03", "endDate" -> "2021-01-03", "startTime" -> "11:00:00Z", "endTime" -> "13:00:00Z"))).asJava)),
            "onlineProvider" -> "Zoom",
            "registrationEndDate" -> "2021-02-25",
            "eventType" -> "Online",
            "versionKey" -> "1878141"))
        request.putAll(eventSet)
        request.setOperation("updateContent")
        val response = callActor(request, Props(new EventSetActor()))
        assert(response.get("identifier") != null)
        assert(response.get("versionKey") != null)
    }


    it should "discard node in draft state should return success" in {
        implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
        val graphDB = mock[GraphService]
        (oec.graphService _).expects().returns(graphDB).anyNumberOfTimes()
        (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(getValidDraftNode())).twice()
        (graphDB.deleteNode(_: String, _: String, _: Request)).expects(*, *, *).returns(Future(true))
        implicit val ss = mock[StorageService]
        val request = getContentRequest()
        request.getRequest.putAll(mapAsJavaMap(Map("identifier" -> "do_12346")))
        request.setOperation("discardContent")
        val response = callActor(request, Props(new EventSetActor()))
        assert(response.getResponseCode == ResponseCode.OK)
        assert(response.get("identifier") == "do_12346")
        assert(response.get("message") == "Draft version of the content with id : do_12346 is discarded")

    }

    /*it should "publish node in draft state should return success" in {
        implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
        val graphDB = mock[GraphService]
        (oec.graphService _).expects().returns(graphDB).anyNumberOfTimes()
        val eventSetNode = getEventSetCollectionNode()
        (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(eventSetNode)).anyNumberOfTimes()
        (graphDB.upsertNode _).expects(*, *, *).returns(Future(eventSetNode)).anyNumberOfTimes()
        val nodes: util.List[Node] = getCategoryNode()
        (graphDB.getNodeByUniqueIds(_: String, _: SearchCriteria)).expects(*, *).returns(Future(nodes)).anyNumberOfTimes()

        implicit val ss = mock[StorageService]
        val request = getContentRequest()
        request.getRequest.putAll(mapAsJavaMap(Map()))
        request.setOperation("publishContent")
        val response = callActor(request, Props(new EventSetActor()))
        assert(response.getResponseCode == ResponseCode.OK)
        assert(response.get("identifier") == "do_12345")
    }*/

    it should "discard node in Live state should return client error" in {
        implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
        val graphDB = mock[GraphService]
        (oec.graphService _).expects().returns(graphDB).anyNumberOfTimes()
        val node = getLiveEventSetCollectionNode()
        node.setOutRelations(null)
        (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(node)).anyNumberOfTimes()
        (graphDB.updateNodes(_: String, _: util.List[String], _: util.HashMap[String, AnyRef])).expects(*, *, *).returns(Future(new util.HashMap[String, Node])).anyNumberOfTimes()
        implicit val ss = mock[StorageService]
        val request = getContentRequest()
        request.getRequest.putAll(mapAsJavaMap(Map("identifier" -> "do_12346")))
        request.setOperation("discardContent")
        val response = callActor(request, Props(new EventSetActor()))
        assert(response.getResponseCode == ResponseCode.CLIENT_ERROR)
    }

    it should "return success response for retireContent" in {
        implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
        val graphDB = mock[GraphService]
        (oec.graphService _).expects().returns(graphDB).anyNumberOfTimes()
        val node = getEventSetCollectionNode()
        node.setOutRelations(null)
        (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(node)).anyNumberOfTimes()
        (graphDB.updateNodes(_: String, _: util.List[String], _: util.HashMap[String, AnyRef])).expects(*, *, *).returns(Future(new util.HashMap[String, Node])).anyNumberOfTimes()
        implicit val ss = mock[StorageService]
        val request = getContentRequest()
        request.getContext.put("identifier","do1234")
        request.getRequest.putAll(mapAsJavaMap(Map("identifier" -> "do_1234")))
        request.setOperation("retireContent")
        val response = callActor(request, Props(new EventSetActor()))
        assert("successful".equals(response.getParams.getStatus))
    }

    it should "return success response for 'readContent'" in {
        implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
        val graphDB = mock[GraphService]
        (oec.graphService _).expects().returns(graphDB)
        val node = getNode("EventSet", None)
        (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(node))
        implicit val ss = mock[StorageService]
        val request = getContentRequest()
        request.getContext.put("identifier","do1234")
        request.putAll(mapAsJavaMap(Map("identifier" -> "do_1234", "fields" -> "")))
        request.setOperation("readContent")
        val response = callActor(request, Props(new EventSetActor()))
        assert("successful".equals(response.getParams.getStatus))
    }

    it should "return success response for 'getHierarchy'" in {
        implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
        val graphDB = mock[GraphService]
        (oec.graphService _).expects().returns(graphDB)
        val node = getNode("EventSet", None)
        (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(node))
        implicit val ss = mock[StorageService]
        val request = getContentRequest()
        request.getContext.put("identifier","do1234")
        request.putAll(mapAsJavaMap(Map("identifier" -> "do_1234", "fields" -> "")))
        request.setOperation("getHierarchy")
        val response = callActor(request, Props(new EventSetActor()))
        assert("successful".equals(response.getParams.getStatus))
    }

    private def getContentRequest(): Request = {
        val request = new Request()
        request.setContext(new util.HashMap[String, AnyRef]() {
            {
                put("graph_id", "domain")
                put("version", "1.0")
                put("objectType", "EventSet")
                put("schemaName", "eventset")
                put("X-Channel-Id", "in.ekstep")
            }
        })
        request.setObjectType("EventSet")
        request
    }

    private def getValidDraftNode(): Node = {
        val node = new Node()
        node.setIdentifier("do_12346")
        node.setNodeType("DATA_NODE")
        node.setObjectType("EventSet")
        node.setMetadata(new util.HashMap[String, AnyRef]() {
            {
                put("identifier", "do_12346")
                put("status", "Draft")
                put("contentType", "EventSet")
                put("name", "Node To discard")
            }
        })
        node
    }

    private def getInValidNodeToDiscard(): Node = {
        val node = new Node()
        node.setIdentifier("do_12346")
        node.setNodeType("DATA_NODE")
        node.setObjectType("EventSet")
        node.setMetadata(new util.HashMap[String, AnyRef]() {
            {
                put("identifier", "do_12346")
                put("status", "Live")
                put("contentType", "EventSet")
                put("name", "Node To discard")
            }
        })
        node
    }

    private def getEventNode(): Node = {
        val node = new Node()
        node.setIdentifier("do_1234")
        node.setNodeType("DATA_NODE")
        node.setObjectType("Event")
        node.setMetadata(new util.HashMap[String, AnyRef]() {
            {
                put("identifier", "do_1234")
                put("status", "Live")
                put("name", "Event_1")
                put("code", "event1")
                put("versionKey", "1878141")
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

    private def getEventSetNode(): Node = {
        val node = new Node()
        node.setIdentifier("do_12345")
        node.setNodeType("DATA_NODE")
        node.setObjectType("EventSet")
        node.setMetadata(new util.HashMap[String, AnyRef]() {
            {
                put("identifier", "do_12345")
                put("status", "Draft")
                put("name", "EventSet_1")
                put("code", "eventset1")
                put("versionKey", "1878141")
                put("startDate", "2021-02-02")
                put("endDate", "2021-02-02")
                put("registrationEndDate", "2021-01-02")
                put("eventType", "Online")
                put("schedule",
                  mapAsJavaMap(Map("type" -> "NON_RECURRING",
                      "value" -> List(mapAsJavaMap(Map("startDate" -> "2021-01-03",
                          "endDate" -> "2021-01-03",
                          "startTime" -> "11:00:00Z",
                          "endTime" -> "13:00:00Z"))).asJava)))

            }
        })
        node
    }

    private def getEventSetCollectionNode(): Node = {
        val node = new Node()
        node.setIdentifier("do_12345")
        node.setNodeType("DATA_NODE")
        node.setObjectType("EventSet")
        val rel: Relation = new Relation()
        rel.setEndNodeObjectType("Event")
        rel.setEndNodeId("do_12345.1")
        rel.setStartNodeId("do_12345")
        rel.setRelationType("hasSequenceMember")
        node.setOutRelations(new util.ArrayList[Relation](){
            add(rel)
        })
        node.setMetadata(new util.HashMap[String, AnyRef]() {
            {
                //put("identifier", "do_12345")
                put("status", "Draft")
                put("name", "EventSet_1")
                put("code", "eventset1")
                put("versionKey", "1878141")
                put("startDate", "2021-02-02")
                put("endDate", "2021-02-02")
                put("registrationEndDate", "2021-01-02")
                put("eventType", "Online")
                put("schedule",
                  mapAsJavaMap(Map("type" -> "NON_RECURRING",
                      "value" -> List(mapAsJavaMap(Map("startDate" -> "2021-01-03",
                          "endDate" -> "2021-01-03",
                          "startTime" -> "11:00:00Z",
                          "endTime" -> "13:00:00Z",
                      "status" -> "Draft"))).asJava)))

            }
        })
        node
    }

    private def getLiveEventSetCollectionNode(): Node = {
        val node = new Node()
        node.setIdentifier("do_12345")
        node.setNodeType("DATA_NODE")
        node.setObjectType("EventSet")
        val rel: Relation = new Relation()
        rel.setEndNodeObjectType("Event")
        rel.setEndNodeId("do_12345.1")
        node.setOutRelations(new util.ArrayList[Relation](){
            add(rel)
        })
        node.setMetadata(new util.HashMap[String, AnyRef]() {
            {
                put("identifier", "do_12345")
                put("status", "Live")
                put("name", "EventSet_1")
                put("code", "eventset1")
                put("versionKey", "1878141")
                put("startDate", "2021-02-02")
                put("endDate", "2021-02-02")
                put("registrationEndDate", "2021-01-02")
                put("eventType", "Online")
                put("schedule",
                  mapAsJavaMap(Map("type" -> "NON_RECURRING",
                      "value" -> List(mapAsJavaMap(Map("startDate" -> "2021-01-03",
                          "endDate" -> "2021-01-03",
                          "startTime" -> "11:00:00Z",
                          "endTime" -> "13:00:00Z",
                      "status" -> "Live"))).asJava)))

            }
        })
        node
    }
}
