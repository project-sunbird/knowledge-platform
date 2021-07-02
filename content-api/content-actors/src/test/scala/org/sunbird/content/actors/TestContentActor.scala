package org.sunbird.content.actors

import akka.actor.Props
import com.google.common.io.Resources
import org.scalamock.scalatest.MockFactory
import org.sunbird.cloudstore.StorageService
import org.sunbird.common.dto.{Property, Request, Response, ResponseHandler}
import org.sunbird.common.exception.ResponseCode
import org.sunbird.common.{HttpUtil, JsonUtils}
import org.sunbird.graph.dac.model.{Node, SearchCriteria}
import org.sunbird.graph.utils.ScalaJsonUtils
import org.sunbird.graph.{GraphService, OntologyEngineContext}
import org.sunbird.kafka.client.KafkaClient

import java.io.File
import java.util
import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TestContentActor extends BaseSpec with MockFactory {

    "ContentActor" should "return failed response for 'unknown' operation" in {
        implicit val ss = mock[StorageService]
        implicit val oec: OntologyEngineContext = new OntologyEngineContext
        testUnknownOperation(Props(new ContentActor()), getContentRequest())
    }

    it should "validate input before creating content" in {
        implicit val ss = mock[StorageService]
        implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
        val request = getContentRequest()
        val content = mapAsJavaMap(Map("name" -> "New Content", "code" -> "1234", "mimeType"-> "application/pdf", "contentType" -> "Resource",
            "framework" ->  "NCF", "organisationBoardIds" -> new util.ArrayList[String](){{add("ncf_board_cbse")}}))
        request.put("content", content)
        assert(true)
        val response = callActor(request, Props(new ContentActor()))
    }

    it should "create a node and store it in neo4j" in {
        implicit val ss = mock[StorageService]
        implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
        val graphDB = mock[GraphService]
        (oec.graphService _).expects().returns(graphDB).anyNumberOfTimes()
        // Uncomment below line if running individual file in local.
        //(graphDB.readExternalProps(_: Request, _: List[String])).expects(*, *).returns(Future(new Response()))
        (graphDB.addNode(_: String, _: Node)).expects(*, *).returns(Future(getValidNode())).anyNumberOfTimes()
        (graphDB.getNodeByUniqueIds(_: String, _: SearchCriteria)).expects(*, *).returns(Future(new util.ArrayList[Node]() {
            {
                add(getBoardNode())
            }
        }))

        val request = getContentRequest()
        request.getRequest.putAll( mapAsJavaMap(Map("channel"-> "in.ekstep","name" -> "New Content", "code" -> "1234", "mimeType"-> "application/vnd.ekstep.content-collection", "contentType" -> "Course", "primaryCategory" -> "Learning Resource", "channel" -> "in.ekstep", "targetBoardIds" -> new util.ArrayList[String](){{add("ncf_board_cbse")}})))
        request.setOperation("createContent")
        val response = callActor(request, Props(new ContentActor()))
        assert(response.get("identifier") != null)
        assert(response.get("versionKey") != null)
    }

    it should "create a plugin node and store it in neo4j" in {
        implicit val ss = mock[StorageService]
        implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
        val graphDB = mock[GraphService]
        (oec.graphService _).expects().returns(graphDB).anyNumberOfTimes()
        (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(getDefinitionNode())).anyNumberOfTimes()
        (graphDB.addNode(_: String, _: Node)).expects(*, *).returns(Future(getValidNode()))
        val nodes: util.List[Node] = getCategoryNode()
        (graphDB.getNodeByUniqueIds(_: String, _: SearchCriteria)).expects(*, *).returns(Future(nodes)).anyNumberOfTimes()

        val request = getContentRequest()
        request.getRequest.putAll( mapAsJavaMap(Map("name" -> "New Content", "code" -> "1234", "mimeType"-> "application/vnd.ekstep.plugin-archive", "contentType" -> "Course", "primaryCategory" -> "Learning Resource", "channel" -> "in.ekstep", "framework"-> "NCF", "organisationBoardIds" -> new util.ArrayList[String](){{add("ncf_board_cbse")}})))
        request.setOperation("createContent")
        val response = callActor(request, Props(new ContentActor()))
        assert(response.get("identifier") != null)
        assert(response.get("versionKey") != null)
    }

    it should "create a plugin node with invalid request, should through client exception" in {
        implicit val ss = mock[StorageService]
        implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
        val graphDB = mock[GraphService]
        val request = getContentRequest()
        request.getRequest.putAll( mapAsJavaMap(Map("name" -> "New Content", "mimeType"-> "application/vnd.ekstep.plugin-archive", "contentType" -> "Course", "primaryCategory" -> "Learning Resource", "channel" -> "in.ekstep")))
        request.setOperation("createContent")
        val response = callActor(request, Props(new ContentActor()))
        assert(response.getResponseCode == ResponseCode.CLIENT_ERROR)
    }

    it should "generate and return presigned url" in {
        implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
        val graphDB = mock[GraphService]
        (oec.graphService _).expects().returns(graphDB)
        (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(getValidNode()))
        implicit val ss = mock[StorageService]
        (ss.getSignedURL(_: String, _: Option[Int], _: Option[String])).expects(*, *, *).returns("cloud store url")
        val request = getContentRequest()
        request.getRequest.putAll(mapAsJavaMap(Map("fileName" -> "presigned_url", "filePath" -> "/data/cloudstore/", "type" -> "assets", "identifier" -> "do_1234")))
        request.setOperation("uploadPreSignedUrl")
        val response = callActor(request, Props(new ContentActor()))
        assert(response.get("identifier") != null)
        assert(response.get("pre_signed_url") != null)
        assert(response.get("url_expiry") != null)
    }

    it should "discard node in draft state should return success" in {
        implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
        val graphDB = mock[GraphService]
        (oec.graphService _).expects().returns(graphDB).repeated(2)
        (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(getValidNodeToDiscard()))
        (graphDB.deleteNode(_: String, _: String, _: Request)).expects(*, *, *).returns(Future(true))
        implicit val ss = mock[StorageService]
        val request = getContentRequest()
        request.getRequest.putAll(mapAsJavaMap(Map("identifier" -> "do_12346")))
        request.setOperation("discardContent")
        val response = callActor(request, Props(new ContentActor()))
        assert(response.getResponseCode == ResponseCode.OK)
        assert(response.get("identifier") == "do_12346")
        assert(response.get("message") == "Draft version of the content with id : do_12346 is discarded")

    }

    it should "discard node in Live state should return client error" in {
        implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
        val graphDB = mock[GraphService]
        (oec.graphService _).expects().returns(graphDB).repeated(1)
        (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(getInValidNodeToDiscard()))
        implicit val ss = mock[StorageService]
        val request = getContentRequest()
        request.getRequest.putAll(mapAsJavaMap(Map("identifier" -> "do_12346")))
        request.setOperation("discardContent")
        val response = callActor(request, Props(new ContentActor()))
        assert(response.getResponseCode == ResponseCode.CLIENT_ERROR)
    }

    it should "return client error response for retireContent" in {
        implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
        implicit val ss = mock[StorageService]
        val request = getContentRequest()
        request.getContext.put("identifier","do_1234.img")
        request.getRequest.putAll(mapAsJavaMap(Map("identifier" -> "do_1234.img")))
        request.setOperation("retireContent")
        val response = callActor(request, Props(new ContentActor()))
        assert(response.getResponseCode == ResponseCode.CLIENT_ERROR)
    }

    private def getContentRequest(): Request = {
        val request = new Request()
        request.setContext(new util.HashMap[String, AnyRef]() {
            {
                put("graph_id", "domain")
                put("version", "1.0")
                put("objectType", "Content")
                put("schemaName", "content")
                put("X-Channel-Id", "in.ekstep")
            }
        })
        request.setObjectType("Content")
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

    private def getValidNode(): Node = {
        val node = new Node()
        node.setIdentifier("do_1234")
        node.setNodeType("DATA_NODE")
        node.setObjectType("Content")
        node.setMetadata(new util.HashMap[String, AnyRef]() {
            {
                put("identifier", "do_1234")
                put("mimeType", "application/vnd.ekstep.content-collection")
                put("status", "Draft")
                put("contentType", "Course")
                put("name", "Course_1")
                put("versionKey", "1878141")
                put("primaryCategory", "Learning Resource")
            }
        })
        node
    }

    it should "return success response for retireContent" in {
        implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
        val graphDB = mock[GraphService]
        (oec.graphService _).expects().returns(graphDB).repeated(2)
        val node = getNode("Content", None)
        (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(node)).anyNumberOfTimes()
        (graphDB.updateNodes(_: String, _: util.List[String], _: util.HashMap[String, AnyRef])).expects(*, *, *).returns(Future(new util.HashMap[String, Node]))
        implicit val ss = mock[StorageService]
        val request = getContentRequest()
        request.getContext.put("identifier","do1234")
        request.getRequest.putAll(mapAsJavaMap(Map("identifier" -> "do_1234")))
        request.setOperation("retireContent")
        val response = callActor(request, Props(new ContentActor()))
        assert("successful".equals(response.getParams.getStatus))
    }

    it should "return success response for 'readContent'" in {
        implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
        val graphDB = mock[GraphService]
        (oec.graphService _).expects().returns(graphDB)
        val node = getNode("Content", None)
        (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(node))
        implicit val ss = mock[StorageService]
        val request = getContentRequest()
        request.getContext.put("identifier","do1234")
        request.putAll(mapAsJavaMap(Map("identifier" -> "do_1234", "fields" -> "")))
        request.setOperation("readContent")
        val response = callActor(request, Props(new ContentActor()))
        assert("successful".equals(response.getParams.getStatus))
    }

    it should "return success response for 'updateContent'" in {
        implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
        val graphDB = mock[GraphService]
        (oec.graphService _).expects().returns(graphDB).anyNumberOfTimes()
        val node = getNode()
        (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(node)).anyNumberOfTimes()
        (graphDB.getNodeProperty(_: String, _: String, _: String)).expects(*, *, *).returns(Future(new Property("versionKey", new org.neo4j.driver.internal.value.StringValue("test_123"))))
        (graphDB.upsertNode(_: String, _: Node, _: Request)).expects(*, *, *).returns(Future(node))
        val nodes: util.List[Node] = getCategoryNode()
        (graphDB.getNodeByUniqueIds(_: String, _: SearchCriteria)).expects(*, *).returns(Future(nodes)).anyNumberOfTimes()

        implicit val ss = mock[StorageService]
        val request = getContentRequest()
        request.getContext.put("identifier","do_1234")
        request.putAll(mapAsJavaMap(Map("description" -> "test desc", "versionKey" -> "test_123")))
        request.setOperation("updateContent")
        val response = callActor(request, Props(new ContentActor()))
        assert("successful".equals(response.getParams.getStatus))
        assert("do_1234".equals(response.get("identifier")))
        assert("test_123".equals(response.get("versionKey")))
    }

    it should "return client exception for 'updateContent' with invalid versionKey" in {
        implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
        val graphDB = mock[GraphService]
        (oec.graphService _).expects().returns(graphDB).anyNumberOfTimes()
        val node = getNode()
        (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(node)).anyNumberOfTimes()
        (graphDB.getNodeProperty(_: String, _: String, _: String)).expects(*, *, *).returns(Future(new Property("versionKey", new org.neo4j.driver.internal.value.StringValue("test_xyz"))))
        val nodes: util.List[Node] = getCategoryNode()
        (graphDB.getNodeByUniqueIds(_: String, _: SearchCriteria)).expects(*, *).returns(Future(nodes)).anyNumberOfTimes()

        implicit val ss = mock[StorageService]
        val request = getContentRequest()
        request.getContext.put("identifier","do_1234")
        request.putAll(mapAsJavaMap(Map("description" -> "test desc", "versionKey" -> "test_123")))
        request.setOperation("updateContent")
        val response = callActor(request, Props(new ContentActor()))
        assert("failed".equals(response.getParams.getStatus))
        assert("CLIENT_ERROR".equals(response.getParams.getErr))
        assert("Invalid version Key".equals(response.getParams.getErrmsg))
    }

    it should "return client exception for 'updateContent' without versionKey" in {
        implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
        implicit val ss = mock[StorageService]
        val request = getContentRequest()
        request.getContext.put("identifier","do1234")
        request.putAll(mapAsJavaMap(Map("description" -> "updated description","framework" ->  "NCF", "organisationBoardIds" -> new util.ArrayList[String](){{add("ncf_board_cbse")}})))
        request.setOperation("updateContent")
        val response = callActor(request, Props(new ContentActor()))
        assert("failed".equals(response.getParams.getStatus))
        assert("ERR_INVALID_REQUEST".equals(response.getParams.getErr))
        assert("Please Provide Version Key!".equals(response.getParams.getErrmsg))
    }

    it should "return success response for 'copyContent'" in {
        implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
        val graphDB = mock[GraphService]
        (oec.graphService _).expects().returns(graphDB).anyNumberOfTimes()
        val node = getNode()
        (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(node)).anyNumberOfTimes()
        (graphDB.addNode(_: String, _: Node)).expects(*, *).returns(Future(node)).anyNumberOfTimes()
        (graphDB.readExternalProps(_: Request, _: List[String])).expects(*, *).returns(Future(new Response())).anyNumberOfTimes()
        val nodes: util.List[Node] = getCategoryNode()
        (graphDB.getNodeByUniqueIds(_: String, _: SearchCriteria)).expects(*, *).returns(Future(nodes)).anyNumberOfTimes()

        implicit val ss = mock[StorageService]
        val request = getContentRequest()
        request.getContext.put("identifier","do1234")
        request.putAll(mapAsJavaMap(Map("identifier" -> "do_1234", "createdBy" -> "username_1",
            "createdFor" -> new util.ArrayList[String]() {{ add("NCF2") }}, "framework" -> "NCF",
            "organisation" -> new util.ArrayList[String]() {{ add("NCF2") }})))
        request.setOperation("copy")
        val response = callActor(request, Props(new ContentActor()))
        assert("successful".equals(response.getParams.getStatus))
        assert(response.getResult.containsKey("node_id"))
        assert("test_321".equals(response.get("versionKey")))
    }

    it should "send events to kafka topic" in {
        implicit val ss = mock[StorageService]
        implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
        val kfClient = mock[KafkaClient]
        val hUtil = mock[HttpUtil]
        (oec.httpUtil _).expects().returns(hUtil)
        val resp :Response = ResponseHandler.OK()
        resp.put("content", new util.HashMap[String, AnyRef](){{
            put("framework", "NCF")
            put("artifactUrl", "http://test.com/test.pdf")
            put("channel", "test")
        }})
        (hUtil.get(_: String, _: String, _: util.Map[String, String])).expects(*, *, *).returns(resp)
        (oec.kafkaClient _).expects().returns(kfClient)
        (kfClient.send(_: String, _: String)).expects(*, *).returns(None)
        val request = getContentRequest()
        request.getRequest.put("content", new util.HashMap[String, AnyRef](){{
            put("source", "https://dock.sunbirded.org/api/content/v1/read/do_11307822356267827219477")
            put("metadata", new util.HashMap[String, AnyRef](){{
                put("name", "Test Content")
                put("description", "Test Content")
                put("mimeType", "application/pdf")
                put("code", "test.res.1")
                put("contentType", "Resource")
                put("primaryCategory", "Learning Resource")
            }})
        }})
        request.setOperation("importContent")
        request.setObjectType("Content")
        val response = callActor(request, Props(new ContentActor()))
        assert(response.get("processId") != null)
    }

    it should "return success response for 'uploadContent' with jpeg asset" ignore  {
        implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
        val graphDB = mock[GraphService]
        implicit val ss = mock[StorageService]
        (oec.graphService _).expects().returns(graphDB).anyNumberOfTimes()
        val node = getAssetNodeToUpload()
        (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(node)).anyNumberOfTimes()
        (graphDB.getNodeProperty(_: String, _: String, _: String)).expects(*, *, *).returns(Future(new Property("versionKey", new org.neo4j.driver.internal.value.StringValue("1234"))))
        (ss.uploadFile(_:String, _: File, _: Option[Boolean])).expects(*, *, *).returns(Array("do_1234", "do_1234"))
        (graphDB.upsertNode(_: String, _: Node, _: Request)).expects(*, *, *).returns(Future(node))
        val request = getContentRequest()
        request.getContext.put("identifier", "do_1234")
        request.putAll(mapAsJavaMap(Map("identifier" -> "do_1234", "createdBy" -> "username_1",
            "createdFor" -> new util.ArrayList[String]() {{ add("NCF2") }}, "framework" -> "NCF",
            "organisation" -> new util.ArrayList[String]() {{ add("NCF2") }})))
        request.put("file", new File(Resources.getResource("jpegImage.jpeg").toURI))
        request.setOperation("uploadContent")
        val response = callActor(request, Props(new ContentActor()))
        assert("successful".equals(response.getParams.getStatus))
    }

    it should "return success response for 'systemUpdateContent'" in {
        implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
        val graphDB = mock[GraphService]
        (oec.graphService _).expects().returns(graphDB).anyNumberOfTimes()
        val node = getNode()
        (graphDB.readExternalProps(_: Request, _: List[String])).expects(*, *).returns(Future(new Response())).anyNumberOfTimes()
        (graphDB.getNodeByUniqueIds(_: String, _: SearchCriteria)).expects(*, *).returns(Future(List(node))).once()
        (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(node)).anyNumberOfTimes()
        (graphDB.getNodeProperty(_: String, _: String, _: String)).expects(*, *, *).returns(Future(new Property("versionKey", new org.neo4j.driver.internal.value.StringValue("test_123"))))
        (graphDB.upsertNode(_: String, _: Node, _: Request)).expects(*, *, *).returns(Future(node))

        val nodes: util.List[Node] = getCategoryNode()
        (graphDB.getNodeByUniqueIds(_: String, _: SearchCriteria)).expects(*, *).returns(Future(nodes)).anyNumberOfTimes()

        implicit val ss = mock[StorageService]
        val request = getContentRequest()
        request.getContext.put("identifier","do_1234")
        request.putAll(mapAsJavaMap(Map("description" -> "test desc", "versionKey" -> "test_123")))
        request.setOperation("systemUpdate")
        val response = callActor(request, Props(new ContentActor()))
        assert("successful".equals(response.getParams.getStatus))
        assert("do_1234".equals(response.get("identifier")))
        assert("success".equals(response.get("status")))
    }

    it should "return success response for 'rejectContent'" in {
        implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
        val graphDB = mock[GraphService]
        (oec.graphService _).expects().returns(graphDB).anyNumberOfTimes()
        implicit val ss = mock[StorageService]
        val node =  getValidNodeToReject()
        (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(node)).anyNumberOfTimes()
        (graphDB.getNodeProperty(_: String, _: String, _: String)).expects(*, *, *).returns(Future(new Property("versionKey", new org.neo4j.driver.internal.value.StringValue("test_123")))).anyNumberOfTimes()
        (graphDB.upsertNode(_: String, _: Node, _: Request)).expects(*, *, *).returns(Future(node)).anyNumberOfTimes()
        val request = getContentRequest()
        request.getContext.put("identifier","do1234")
        request.putAll(mapAsJavaMap(Map("description" -> "test desc", "versionKey" -> "test_123")))
        request.setOperation("rejectContent")
        val response = callActor(request, Props(new ContentActor()))
        println("Response_message" + response)
        assert("successful".equals(response.getParams.getStatus))
    }

    private def getAssetNodeToUpload(): Node = {
        val node = new Node()
        node.setIdentifier("do_1234")
        node.setNodeType("DATA_NODE")
        node.setObjectType("Asset")
        node.setMetadata(new util.HashMap[String, AnyRef]() {
            {
                put("identifier", "do_1234")
                put("mimeType", "image/jpeg")
                put("contentType", "Asset")
                put("name", "Asset_1")
                put("versionKey", "test_321")
                put("channel", "in.ekstep")
                put("code", "Resource_1")
                put("primaryCategory", "Asset")
                put("versionKey", "1234")
            }
        })
        node
    }

    private def getNode(): Node = {
        val node = new Node()
        node.setIdentifier("do_1234")
        node.setNodeType("DATA_NODE")
        node.setObjectType("Content")
        node.setMetadata(new util.HashMap[String, AnyRef]() {
            {
                put("identifier", "do_1234")
                put("mimeType", "application/pdf")
                put("status", "Live")
                put("contentType", "Resource")
                put("name", "Resource_1")
                put("versionKey", "test_321")
                put("channel", "in.ekstep")
                put("code", "Resource_1")
                put("primaryCategory", "Learning Resource")
            }
        })
        node
    }


    def getDefinitionNode(): Node = {
        val node = new Node()
        node.setIdentifier("obj-cat:learning-resource_content_in.ekstep")
        node.setNodeType("DATA_NODE")
        node.setObjectType("Content")
        node.setGraphId("domain")
        node.setMetadata(mapAsJavaMap(
            ScalaJsonUtils.deserialize[Map[String,AnyRef]]("{\n    \"objectCategoryDefinition\": {\n      \"name\": \"Learning Resource\",\n      \"description\": \"Content Playlist\",\n      \"categoryId\": \"obj-cat:learning-resource\",\n      \"targetObjectType\": \"Content\",\n      \"objectMetadata\": {\n        \"config\": {},\n        \"schema\": {\n          \"required\": [\n            \"author\",\n            \"copyright\",\n            \"license\",\n            \"audience\"\n          ],\n          \"properties\": {\n            \"audience\": {\n              \"type\": \"array\",\n              \"items\": {\n                \"type\": \"string\",\n                \"enum\": [\n                  \"Student\",\n                  \"Teacher\"\n                ]\n              },\n              \"default\": [\n                \"Student\"\n              ]\n            },\n            \"mimeType\": {\n              \"type\": \"string\",\n              \"enum\": [\n                \"application/pdf\"\n              ]\n            }\n          }\n        }\n      }\n    }\n  }")))
        node
    }

    def getContentSchema(): util.Map[String, AnyRef]  = {
       val schema:String =  "{\n    \"$id\": \"content-schema.json\",\n    \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n    \"title\": \"Content\",\n    \"type\": \"object\",\n    \"required\": [\n        \"name\",\n        \"status\",\n        \"mimeType\",\n        \"channel\",\n        \"contentType\",\n        \"code\",\n        \"contentEncoding\",\n        \"contentDisposition\",\n        \"mediaType\",\n        \"primaryCategory\"\n    ],\n    \"properties\": {\n        \"name\": {\n            \"type\": \"string\",\n            \"minLength\": 5\n        },\n        \"code\": {\n            \"type\": \"string\"\n        },\n        \"createdOn\": {\n            \"type\": \"string\"\n        },\n        \"lastUpdatedOn\": {\n            \"type\": \"string\"\n        },\n        \"status\": {\n            \"type\": \"string\",\n            \"enum\": [\n                \"Draft\",\n                \"Review\",\n                \"Redraft\",\n                \"Flagged\",\n                \"Live\",\n                \"Unlisted\",\n                \"Retired\",\n                \"Mock\",\n                \"Processing\",\n                \"FlagDraft\",\n                \"FlagReview\",\n                \"Failed\"\n            ],\n            \"default\": \"Draft\"\n        },\n        \"channel\": {\n            \"type\": \"string\"\n        },\n        \"mimeType\": {\n            \"type\": \"string\",\n            \"enum\": [\n                \"application/vnd.ekstep.ecml-archive\",\n                \"application/vnd.ekstep.html-archive\",\n                \"application/vnd.android.package-archive\",\n                \"application/vnd.ekstep.content-archive\",\n                \"application/vnd.ekstep.content-collection\",\n                \"application/vnd.ekstep.plugin-archive\",\n                \"application/vnd.ekstep.h5p-archive\",\n                \"application/epub\",\n                \"text/x-url\",\n                \"video/x-youtube\",\n                \"application/octet-stream\",\n                \"application/msword\",\n                \"application/pdf\",\n                \"image/jpeg\",\n                \"image/jpg\",\n                \"image/png\",\n                \"image/tiff\",\n                \"image/bmp\",\n                \"image/gif\",\n                \"image/svg+xml\",\n                \"video/avi\",\n                \"video/mpeg\",\n                \"video/quicktime\",\n                \"video/3gpp\",\n                \"video/mp4\",\n                \"video/ogg\",\n                \"video/webm\",\n                \"audio/mp3\",\n                \"audio/mp4\",\n                \"audio/mpeg\",\n                \"audio/ogg\",\n                \"audio/webm\",\n                \"audio/x-wav\",\n                \"audio/wav\"\n            ]\n        },\n        \"osId\": {\n            \"type\": \"string\",\n            \"default\": \"org.ekstep.launcher\"\n        },\n        \"contentEncoding\": {\n            \"type\": \"string\",\n            \"enum\": [\n                \"gzip\",\n                \"identity\"\n            ],\n            \"default\": \"gzip\"\n        },\n        \"contentDisposition\": {\n            \"type\": \"string\",\n            \"enum\": [\n                \"inline\",\n                \"online\",\n                \"attachment\",\n                \"online-only\"\n            ],\n            \"default\": \"inline\"\n        },\n        \"mediaType\": {\n            \"type\": \"string\",\n            \"enum\": [\n                \"content\",\n                \"collection\",\n                \"image\",\n                \"video\",\n                \"audio\",\n                \"voice\",\n                \"ecml\",\n                \"document\",\n                \"pdf\",\n                \"text\",\n                \"other\"\n            ],\n            \"default\": \"content\"\n        },\n        \"os\": {\n            \"type\": \"array\",\n            \"items\": {\n                \"type\" : \"string\",\n                \"enum\": [\n                    \"All\",\n                    \"Android\",\n                    \"iOS\",\n                    \"Windows\"\n                ]\n            },\n            \"default\": [\"All\"]\n        },\n        \"minOsVersion\": {\n            \"type\": \"string\"\n        },\n        \"compatibilityLevel\": {\n            \"type\": \"number\",\n            \"default\": 1\n        },\n        \"minGenieVersion\": {\n            \"type\": \"string\"\n        },\n        \"minSupportedVersion\": {\n            \"type\": \"string\"\n        },\n        \"filter\": {\n            \"type\": \"array\",\n            \"items\": {\n                \"type\": \"string\"\n            }\n        },\n        \"variants\": {\n            \"type\": \"object\"\n        },\n        \"config\": {\n            \"type\": \"object\"\n        },\n        \"visibility\": {\n            \"type\": \"string\",\n            \"enum\": [\n                \"Default\",\n                \"Parent\"\n            ],\n            \"default\": \"Default\"\n        },\n        \"audience\": {\n            \"type\": \"array\",\n            \"items\": {\n                \"type\": \"string\",\n                \"enum\": [\n                    \"Student\",\n                    \"Teacher\",\n                    \"Administrator\"\n                ]\n            },\n            \"default\": [\"Student\"]\n        },\n        \"posterImage\": {\n            \"type\": \"string\",\n            \"format\": \"url\"\n        },\n        \"badgeAssertions\": {\n            \"type\": \"array\",\n            \"items\": {\n                \"type\": \"object\"\n            }\n        },\n        \"targets\": {\n            \"type\": \"array\",\n            \"items\": {\n                \"type\": \"object\"\n            }\n        },\n        \"contentCredits\": {\n            \"type\": \"array\",\n            \"items\": {\n                \"type\": \"object\"\n            }\n        },\n        \"appIcon\": {\n            \"type\": \"string\",\n            \"format\": \"url\"\n        },\n        \"grayScaleAppIcon\": {\n            \"type\": \"string\",\n            \"format\": \"url\"\n        },\n        \"thumbnail\": {\n            \"type\": \"string\",\n            \"format\": \"url\"\n        },\n        \"screenshots\": {\n            \"type\": \"string\"\n        },\n        \"format\": {\n            \"type\": \"string\"\n        },\n        \"duration\": {\n            \"type\": \"string\"\n        },\n        \"size\": {\n            \"type\": \"number\"\n        },\n        \"idealScreenSize\": {\n            \"type\": \"string\",\n            \"enum\": [\n                \"small\",\n                \"normal\",\n                \"large\",\n                \"xlarge\",\n                \"other\"\n            ],\n            \"default\": \"normal\"\n        },\n        \"idealScreenDensity\": {\n            \"type\": \"string\",\n            \"enum\": [\n                \"ldpi\",\n                \"mdpi\",\n                \"hdpi\",\n                \"xhdpi\",\n                \"xxhdpi\",\n                \"xxxhdpi\"\n            ],\n            \"default\": \"hdpi\"\n        },\n        \"releaseNotes\": {\n            \"type\": \"array\"\n        },\n        \"pkgVersion\": {\n            \"type\": \"number\"\n        },\n        \"semanticVersion\": {\n            \"type\": \"string\"\n        },\n        \"versionKey\": {\n            \"type\": \"string\"\n        },\n        \"resources\": {\n            \"type\": \"array\",\n            \"items\": {\n                \"type\": \"string\",\n                \"enum\": [\n                    \"Speaker\",\n                    \"Touch\",\n                    \"Microphone\",\n                    \"GPS\",\n                    \"Motion Sensor\",\n                    \"Compass\"\n                ]\n            }\n        },\n        \"downloadUrl\": {\n            \"type\": \"string\",\n            \"format\": \"url\"\n        },\n        \"artifactUrl\": {\n            \"type\": \"string\",\n            \"format\": \"url\"\n        },\n        \"previewUrl\": {\n            \"type\": \"string\",\n            \"format\": \"url\"\n        },\n        \"streamingUrl\": {\n            \"type\": \"string\",\n            \"format\": \"url\"\n        },\n        \"objects\": {\n            \"type\": \"array\"\n        },\n        \"organization\": {\n            \"type\": \"array\"\n        },\n        \"createdFor\": {\n            \"type\": \"array\"\n        },\n        \"developer\": {\n            \"type\": \"string\"\n        },\n        \"source\": {\n            \"type\": \"string\"\n        },\n        \"notes\": {\n            \"type\": \"string\"\n        },\n        \"pageNumber\": {\n            \"type\": \"string\"\n        },\n        \"publication\": {\n            \"type\": \"string\"\n        },\n        \"edition\": {\n            \"type\": \"string\"\n        },\n        \"publisher\": {\n            \"type\": \"string\"\n        },\n        \"author\": {\n            \"type\": \"string\"\n        },\n        \"owner\": {\n            \"type\": \"string\"\n        },\n        \"attributions\": {\n            \"type\": \"array\"\n        },\n        \"collaborators\": {\n            \"type\": \"array\"\n        },\n        \"creators\": {\n            \"type\": \"string\"\n        },\n        \"contributors\": {\n            \"type\": \"string\"\n        },\n        \"voiceCredits\": {\n            \"type\": \"array\"\n        },\n        \"soundCredits\": {\n            \"type\": \"array\"\n        },\n        \"imageCredits\": {\n            \"type\": \"array\"\n        },\n        \"copyright\": {\n            \"type\": \"string\"\n        },\n        \"license\": {\n            \"type\": \"string\",\n            \"default\": \"CC BY 4.0\"\n        },\n        \"language\": {\n            \"type\": \"array\",\n            \"items\": {\n                \"type\": \"string\",\n                \"enum\": [\n                    \"English\",\n                    \"Hindi\",\n                    \"Assamese\",\n                    \"Bengali\",\n                    \"Gujarati\",\n                    \"Kannada\",\n                    \"Malayalam\",\n                    \"Marathi\",\n                    \"Nepali\",\n                    \"Odia\",\n                    \"Punjabi\",\n                    \"Tamil\",\n                    \"Telugu\",\n                    \"Urdu\",\n                    \"Sanskrit\",\n                    \"Maithili\",\n                    \"Munda\",\n                    \"Santali\",\n                    \"Juang\",\n                    \"Ho\",\n                    \"Other\"\n                ]\n            },\n            \"default\": [\"English\"]\n        },\n        \"words\": {\n            \"type\": \"array\"\n        },\n        \"text\": {\n            \"type\": \"array\"\n        },\n        \"forkable\": {\n            \"type\": \"boolean\"\n        },\n        \"translatable\": {\n            \"type\": \"boolean\"\n        },\n        \"ageGroup\": {\n            \"type\": \"array\",\n            \"items\": {\n                \"type\": \"string\",\n                \"enum\": [\n                    \"<5\",\n                    \"5-6\",\n                    \"6-7\",\n                    \"7-8\",\n                    \"8-10\",\n                    \">10\",\n                    \"Other\"\n                ]\n            }\n        },\n        \"interactivityLevel\": {\n            \"type\": \"string\",\n            \"enum\": [\n                \"High\",\n                \"Medium\",\n                \"Low\"\n            ]\n        },\n        \"contentType\": {\n            \"type\": \"string\",\n            \"enum\": [\n                \"Resource\",\n                \"Collection\",\n                \"TextBook\",\n                \"LessonPlan\",\n                \"Course\",\n                \"Template\",\n                \"Asset\",\n                \"Plugin\",\n                \"LessonPlanUnit\",\n                \"CourseUnit\",\n                \"TextBookUnit\",\n                \"TeachingMethod\",\n                \"PedagogyFlow\",\n                \"FocusSpot\",\n                \"LearningOutcomeDefinition\",\n                \"PracticeQuestionSet\",\n                \"CuriosityQuestionSet\",\n                \"MarkingSchemeRubric\",\n                \"ExplanationResource\",\n                \"ExperientialResource\",\n                \"ConceptMap\",\n                \"SelfAssess\",\n                \"PracticeResource\",\n                \"eTextBook\",\n                \"OnboardingResource\",\n                \"ExplanationVideo\",\n                \"ClassroomTeachingVideo\",\n                \"ExplanationReadingMaterial\",\n                \"LearningActivity\",\n                \"PreviousBoardExamPapers\",\n                \"LessonPlanResource\",\n                \"TVLesson\"\n            ]\n        },\n        \"resourceType\": {\n            \"type\": \"string\",\n            \"enum\": [\n                \"Read\",\n                \"Learn\",\n                \"Teach\",\n                \"Play\",\n                \"Test\",\n                \"Practice\",\n                \"Experiment\",\n                \"Collection\",\n                \"Book\",\n                \"Lesson Plan\",\n                \"Course\",\n                \"Theory\",\n                \"Worksheet\",\n                \"Practical\"\n            ]\n        },\n        \"category\": {\n            \"type\": \"array\",\n            \"items\": {\n                \"type\": \"string\",\n                \"enum\": [\n                    \"core\",\n                    \"learning\",\n                    \"literacy\",\n                    \"math\",\n                    \"science\",\n                    \"time\",\n                    \"wordnet\",\n                    \"game\",\n                    \"mcq\",\n                    \"mtf\",\n                    \"ftb\",\n                    \"library\"\n                ]\n            }\n        },\n        \"templateType\": {\n            \"type\": \"string\",\n            \"enum\": [\n                \"story\",\n                \"worksheet\",\n                \"mcq\",\n                \"ftb\",\n                \"mtf\",\n                \"recognition\",\n                \"activity\",\n                \"widget\",\n                \"other\"\n            ]\n        },\n        \"genre\": {\n            \"type\": \"array\",\n            \"items\": {\n                \"type\": \"string\",\n                \"enum\": [\n                    \"Picture Books\",\n                    \"Chapter Books\",\n                    \"Flash Cards\",\n                    \"Serial Books\",\n                    \"Alphabet Books\",\n                    \"Folktales\",\n                    \"Fiction\",\n                    \"Non-Fiction\",\n                    \"Poems/Rhymes\",\n                    \"Plays\",\n                    \"Comics\",\n                    \"Words\"\n                ]\n            }\n        },\n        \"theme\": {\n            \"type\": \"array\",\n            \"items\": {\n                \"type\": \"string\",\n                \"enum\": [\n                    \"History\",\n                    \"Adventure\",\n                    \"Mystery\",\n                    \"Science\",\n                    \"Nature\",\n                    \"Art\",\n                    \"Music\",\n                    \"Funny\",\n                    \"Family\",\n                    \"Life Skills\",\n                    \"Scary\",\n                    \"School Stories\",\n                    \"Holidays\",\n                    \"Hobby\",\n                    \"Geography\",\n                    \"Rural\",\n                    \"Urban\"\n                ]\n            }\n        },\n        \"themes\": {\n            \"type\": \"array\"\n        },\n        \"rating\": {\n            \"type\": \"number\"\n        },\n        \"rating_a\": {\n            \"type\": \"number\"\n        },\n        \"quality\": {\n            \"type\": \"number\"\n        },\n        \"genieScore\": {\n            \"type\": \"number\"\n        },\n        \"authoringScore\": {\n            \"type\": \"number\"\n        },\n        \"popularity\": {\n            \"type\": \"number\"\n        },\n        \"downloads\": {\n            \"type\": \"number\"\n        },\n        \"launchUrl\": {\n            \"type\": \"string\"\n        },\n        \"activity_class\": {\n            \"type\": \"string\"\n        },\n        \"draftImage\": {\n            \"type\": \"string\"\n        },\n        \"scaffolding\": {\n            \"type\": \"array\",\n            \"items\": {\n                \"type\": \"string\",\n                \"enum\": [\n                    \"Tutorial\",\n                    \"Help\",\n                    \"Practice\"\n                ]\n            }\n        },\n        \"feedback\": {\n            \"type\": \"array\",\n            \"items\": {\n                \"type\": \"string\",\n                \"enum\": [\n                    \"Right/Wrong\",\n                    \"Reflection\",\n                    \"Guidance\",\n                    \"Learn from Mistakes\",\n                    \"Adaptive Feedback\",\n                    \"Interrupts\",\n                    \"Rich Feedback\"\n                ]\n            }\n        },\n        \"feedbackType\": {\n            \"type\": \"array\",\n            \"items\": {\n                \"type\": \"string\",\n                \"enum\": [\n                    \"Audio\",\n                    \"Visual\",\n                    \"Textual\",\n                    \"Tactile\"\n                ]\n            }\n        },\n        \"teachingMode\": {\n            \"type\": \"string\",\n            \"enum\": [\n                \"Abstract\",\n                \"Concrete\",\n                \"Pictorial\"\n            ]\n        },\n        \"skills\": {\n            \"type\": \"array\",\n            \"items\": {\n                \"type\": \"string\",\n                \"enum\": [\n                    \"Listening\",\n                    \"Speaking\",\n                    \"Reading\",\n                    \"Writing\",\n                    \"Touch\",\n                    \"Gestures\",\n                    \"Draw\"\n                ]\n            }\n        },\n        \"keywords\": {\n            \"type\": \"array\"\n        },\n        \"domain\": {\n            \"type\": \"array\"\n        },\n        \"dialcodes\": {\n            \"type\": \"array\"\n        },\n        \"optStatus\": {\n            \"type\": \"string\",\n            \"enum\": [\n                \"Pending\",\n                \"Processing\",\n                \"Error\",\n                \"Complete\"\n            ]\n        },\n        \"description\": {\n            \"type\": \"string\"\n        },\n        \"instructions\": {\n            \"type\": \"string\"\n        },\n        \"body\": {\n            \"type\": \"string\"\n        },\n        \"oldBody\": {\n            \"type\": \"string\"\n        },\n        \"stageIcons\": {\n            \"type\": \"string\"\n        },\n        \"editorState\": {\n            \"type\": \"string\"\n        },\n        \"data\": {\n            \"type\": \"array\"\n        },\n        \"loadingMessage\": {\n            \"type\": \"string\"\n        },\n        \"checksum\": {\n            \"type\": \"string\"\n        },\n        \"learningObjective\": {\n            \"type\": \"array\"\n        },\n        \"createdBy\": {\n            \"type\": \"string\"\n        },\n        \"creator\": {\n            \"type\": \"string\"\n        },\n        \"reviewer\": {\n            \"type\": \"string\"\n        },\n        \"lastUpdatedBy\": {\n            \"type\": \"string\"\n        },\n        \"lastSubmittedBy\": {\n            \"type\": \"string\"\n        },\n        \"lastSubmittedOn\": {\n            \"type\": \"string\"\n        },\n        \"lastPublishedBy\": {\n            \"type\": \"string\"\n        },\n        \"lastPublishedOn\": {\n            \"type\": \"string\"\n        },\n        \"versionDate\": {\n            \"type\": \"string\"\n        },\n        \"origin\": {\n            \"type\": \"string\"\n        },\n        \"originData\": {\n            \"type\": \"object\"\n        },\n        \"versionCreatedBy\": {\n            \"type\": \"string\"\n        },\n        \"me_totalSessionsCount\": {\n            \"type\": \"number\"\n        },\n        \"me_creationSessions\": {\n            \"type\": \"number\"\n        },\n        \"me_creationTimespent\": {\n            \"type\": \"number\"\n        },\n        \"me_totalTimespent\": {\n            \"type\": \"number\"\n        },\n        \"me_totalInteractions\": {\n            \"type\": \"number\"\n        },\n        \"me_averageInteractionsPerMin\": {\n            \"type\": \"number\"\n        },\n        \"me_averageSessionsPerDevice\": {\n            \"type\": \"number\"\n        },\n        \"me_totalDevices\": {\n            \"type\": \"number\"\n        },\n        \"me_averageTimespentPerSession\": {\n            \"type\": \"number\"\n        },\n        \"me_averageRating\": {\n            \"type\": \"number\"\n        },\n        \"me_totalDownloads\": {\n            \"type\": \"number\"\n        },\n        \"me_totalSideloads\": {\n            \"type\": \"number\"\n        },\n        \"me_totalRatings\": {\n            \"type\": \"number\"\n        },\n        \"me_totalComments\": {\n            \"type\": \"number\"\n        },\n        \"me_totalUsage\": {\n            \"type\": \"number\"\n        },\n        \"me_totalLiveContentUsage\": {\n            \"type\": \"number\"\n        },\n        \"me_usageLastWeek\": {\n            \"type\": \"number\"\n        },\n        \"me_deletionsLastWeek\": {\n            \"type\": \"number\"\n        },\n        \"me_lastUsedOn\": {\n            \"type\": \"string\"\n        },\n        \"me_lastRemovedOn\": {\n            \"type\": \"string\"\n        },\n        \"me_hierarchyLevel\": {\n            \"type\": \"number\"\n        },\n        \"me_totalDialcodeAttached\": {\n            \"type\": \"number\"\n        },\n        \"me_totalDialcodeLinkedToContent\": {\n            \"type\": \"number\"\n        },\n        \"me_totalDialcode\": {\n            \"type\": \"array\"\n        },\n        \"flagReasons\": {\n            \"type\": \"array\",\n            \"items\": {\n                \"type\": \"string\",\n                \"enum\": [\n                    \"Inappropriate Content\",\n                    \"Copyright Violation\",\n                    \"Privacy Violation\",\n                    \"Other\"\n                ]\n            }\n        },\n        \"flaggedBy\": {\n            \"type\": \"array\"\n        },\n        \"flags\": {\n            \"type\": \"array\"\n        },\n        \"lastFlaggedOn\": {\n            \"type\": \"string\"\n        },\n        \"tempData\": {\n            \"type\": \"string\"\n        },\n        \"copyType\": {\n            \"type\": \"string\"\n        },\n        \"pragma\": {\n            \"type\": \"array\",\n            \"items\": {\n                \"type\": \"string\",\n                \"enum\": [\n                    \"external\",\n                    \"ads\"\n                ]\n            }\n        },\n        \"publishChecklist\": {\n            \"type\": \"array\"\n        },\n        \"publishComment\": {\n            \"type\": \"string\"\n        },\n        \"rejectReasons\": {\n            \"type\": \"array\"\n        },\n        \"rejectComment\": {\n            \"type\": \"string\"\n        },\n        \"totalQuestions\": {\n            \"type\": \"number\"\n        },\n        \"totalScore\": {\n            \"type\": \"number\"\n        },\n        \"ownershipType\": {\n            \"type\": \"array\",\n            \"items\": {\n                \"type\": \"string\",\n                \"enum\": [\n                    \"createdBy\",\n                    \"createdFor\"\n                ]\n            },\n            \"default\": [\"createdBy\"]\n        },\n        \"reservedDialcodes\": {\n            \"type\": \"object\"\n        },\n        \"dialcodeRequired\": {\n            \"type\": \"string\",\n            \"enum\": [\n                \"Yes\",\n                \"No\"\n            ],\n            \"default\": \"No\"\n        },\n        \"lockKey\": {\n            \"type\": \"string\"\n        },\n        \"badgeAssociations\": {\n            \"type\": \"object\"\n        },\n        \"framework\": {\n            \"type\": \"string\",\n            \"default\": \"NCF\"\n        },\n        \"lastStatusChangedOn\": {\n            \"type\": \"string\"\n        },\n        \"uploadError\": {\n            \"type\": \"string\"\n        },\n        \"appId\": {\n            \"type\": \"string\"\n        },\n        \"s3Key\": {\n            \"type\": \"string\"\n        },\n        \"consumerId\": {\n            \"type\": \"string\"\n        },\n        \"organisation\": {\n            \"type\": \"array\"\n        },\n        \"nodeType\": {\n            \"type\": \"string\"\n        },\n        \"prevState\": {\n            \"type\": \"string\"\n        },\n        \"publishError\": {\n            \"type\": \"string\"\n        },\n        \"publish_type\": {\n            \"type\": \"string\"\n        },\n        \"ownedBy\": {\n            \"type\": \"string\"\n        },\n        \"purpose\": {\n            \"type\": \"string\"\n        },\n        \"toc_url\": {\n            \"type\": \"string\",\n            \"format\": \"url\"\n        },\n        \"reviewError\": {\n            \"type\": \"string\"\n        },\n        \"mimeTypesCount\": {\n            \"type\": \"string\"\n        },\n        \"contentTypesCount\": {\n            \"type\": \"string\"\n        },\n        \"childNodes\": {\n            \"type\": \"array\"\n        },\n        \"leafNodesCount\": {\n            \"type\": \"number\"\n        },\n        \"depth\": {\n            \"type\": \"number\"\n        },\n        \"SYS_INTERNAL_LAST_UPDATED_ON\": {\n            \"type\": \"string\"\n        },\n        \"assets\": {\n            \"type\": \"array\"\n        },\n        \"version\": {\n            \"type\": \"number\",\n            \"default\": 2\n        },\n        \"qrCodeProcessId\": {\n            \"type\": \"string\"\n        },\n        \"migratedUrl\": {\n            \"type\": \"string\",\n            \"format\": \"url\"\n        },\n        \"totalCompressedSize\": {\n            \"type\": \"number\"\n        },\n        \"programId\": {\n            \"type\": \"string\"\n        },\n        \"leafNodes\": {\n            \"type\": \"array\"\n        },\n        \"editorVersion\": {\n            \"type\": \"number\"\n        },\n        \"unitIdentifiers\": {\n            \"type\": \"array\"\n        },\n        \"questionCategories\": {\n            \"type\": \"array\"\n        },\n        \"certTemplate\": {\n            \"type\": \"array\",\n            \"items\": {\n                \"type\": \"object\"\n            }\n        },\n        \"subject\" : {\n            \"type\": \"array\"\n        },\n        \"medium\" : {\n            \"type\": \"array\"\n        },\n        \"gradeLevel\" : {\n            \"type\": \"array\"\n        },\n        \"topic\" : {\n            \"type\": \"array\"\n        },\n        \"subDomains\" : {\n            \"type\": \"array\"\n        },\n        \"subjectCodes\" : {\n            \"type\": \"array\"\n        },\n        \"difficultyLevel\" : {\n            \"type\": \"string\"\n        },\n        \"board\" : {\n            \"type\": \"string\"\n        },\n        \"licenseterms\" : {\n            \"type\": \"string\"\n        },\n        \"copyrightYear\" : {\n            \"type\": \"number\"\n        },\n        \"organisationId\" : {\n            \"type\": \"string\"\n        },\n        \"programId\": {\n            \"type\": \"string\"\n        },\n        \"itemSetPreviewUrl\": {\n            \"type\": \"string\"\n        },\n        \"textbook_name\" : {\n            \"type\": \"array\"\n        },\n        \"level1Name\" : {\n            \"type\": \"array\"\n        },\n        \"level1Concept\" : {\n            \"type\": \"array\"\n        },\n        \"level2Name\" : {\n            \"type\": \"array\"\n        },\n        \"level2Concept\" : {\n            \"type\": \"array\"\n        },\n        \"level3Name\" : {\n            \"type\": \"array\"\n        },\n        \"level3Concept\" : {\n            \"type\": \"array\"\n        },\n        \"sourceURL\" : {\n            \"type\": \"string\"\n        },\n        \"me_totalTimeSpentInSec\": {\n            \"type\": \"object\"\n        },\n        \"me_totalPlaySessionCount\": {\n            \"type\": \"object\"\n        },\n        \"me_totalRatingsCount\": {\n            \"type\": \"number\"\n        },\n        \"monitorable\": {\n            \"type\": \"array\",\n            \"items\": {\n                \"type\": \"string\",\n                \"enum\": [\n                    \"progress-report\",\n                    \"score-report\"\n                ]\n            }\n        },\n        \"userConsent\": {\n            \"type\": \"string\",\n            \"enum\": [\n                \"Yes\",\n                \"No\"\n            ]\n        },\n        \"trackable\": {\n            \"type\": \"object\",\n            \"properties\": {\n                \"enabled\": {\n                    \"type\": \"string\",\n                    \"enum\": [\"Yes\",\"No\"],\n                    \"default\": \"No\"\n                },\n                \"autoBatch\": {\n                    \"type\": \"string\",\n                    \"enum\": [\"Yes\",\"No\"],\n                    \"default\": \"No\"\n                }\n            },\n            \"default\": {\n                \"enabled\": \"No\",\n                \"autoBatch\": \"No\"\n            },\n            \"additionalProperties\": false\n        },\n        \"credentials\": {\n            \"type\": \"object\",\n            \"properties\": {\n                \"enabled\": {\n                    \"type\": \"string\",\n                    \"enum\": [\"Yes\",\"No\"],\n                    \"default\": \"No\"\n                }\n            },\n            \"default\": {\n                \"enabled\": \"No\"\n            },\n            \"additionalProperties\": false\n        },\n        \"processId\": {\n            \"type\": \"string\"\n        },\n        \"primaryCategory\": {\n            \"type\": \"string\",\n            \"enum\": [\n                \"Explanation Content\",\n                \"Learning Resource\",\n                \"Course\",\n                \"Practice Question Set\",\n                \"eTextbook\",\n                \"Teacher Resource\",\n                \"Course Assessment\",\n                \"Digital Textbook\",\n                \"Content Playlist\",\n                \"Template\",\n                \"Asset\",\n                \"Plugin\",\n                \"Lesson Plan Unit\",\n                \"Course Unit\",\n                \"Textbook Unit\"\n            ]\n        },\n        \"additionalCategories\": {\n            \"type\": \"array\",\n            \"items\": {\n                \"type\": \"string\",\n                \"enum\": [\n                    \"Textbook\",\n                    \"TV Lesson\",\n                    \"Previous Board Exam Papers\",\n                    \"Pedagogy Flow\",\n                    \"Marking Scheme Rubric\",\n                    \"Lesson Plan\",\n                    \"Learning Outcome Definition\",\n                    \"Focus Spot\",\n                    \"Explanation Video\",\n                    \"Experiential Resource\",\n                    \"Curiosity Question Set\",\n                    \"Concept Map\",\n                    \"Classroom Teaching Video\"\n                ]\n            }\n        },\n        \"prevStatus\": {\n            \"type\": \"string\"\n        },\n        \"cloudStorageKey\": {\n            \"type\": \"string\"\n        },\n      \"batches\": {\n        \"type\": \"array\",\n        \"items\": {\n          \"type\": \"object\"\n        }\n      },\n        \"year\": {\n            \"type\": \"string\"\n        },\n        \"plugins\": {\n            \"type\": \"array\",\n            \"items\": {\n                \"type\": \"object\"\n            }\n        },\n        \"showNotification\": {\n            \"type\": \"boolean\"\n        },\n        \"collectionId\" : {\n            \"type\": \"string\"\n        },\n        \"learningOutcome\": {\n            \"type\": \"array\",\n            \"items\": {\n                \"type\": \"string\"\n            }\n        }\n    }\n}"
       JsonUtils.deserialize(schema, classOf[util.Map[String, AnyRef]])
    }

    def getContentConfig(): util.Map[String, AnyRef]  = {
        val schema:String =  "{\n    \"restrictProps\": {\n        \"create\" : [\n            \"status\", \"dialcodes\"\n        ],\n        \"copy\" : [\n            \"status\"\n        ],\n        \"update\": []\n    },\n    \"objectType\": \"Content\",\n    \"external\": {\n        \"tableName\": \"content_data\",\n        \"properties\": {\n            \"body\": {\n                \"type\": \"blob\"\n            },\n            \"oldBody\": {\n                \"type\": \"blob\"\n            },\n            \"stageIcons\": {\n                \"type\": \"blob\"\n            },\n            \"screenshots\": {\n                \"type\": \"blob\"\n            },\n            \"last_updated_on\": {\n                \"type\": \"timestamp\"\n            },\n            \"externallink\": {\n                \"type\": \"text\"\n            }\n        },\n        \"primaryKey\": [\"content_id\"]\n    },\n    \"relations\": {\n        \"concepts\": {\n            \"type\": \"associatedTo\",\n            \"direction\": \"out\",\n            \"objects\": [\"Concept\"]\n        },\n        \"questions\": {\n            \"type\": \"associatedTo\",\n            \"direction\": \"out\",\n            \"objects\": [\"AssessmentItem\"]\n        },\n        \"children\": {\n            \"type\": \"hasSequenceMember\",\n            \"direction\": \"out\",\n            \"objects\": [\"Content\", \"ContentImage\"]\n        },\n        \"collections\": {\n            \"type\": \"hasSequenceMember\",\n            \"direction\": \"in\",\n            \"objects\": [\"Content\", \"ContentImage\"]\n        },\n        \"usedByContent\": {\n            \"type\": \"associatedTo\",\n            \"direction\": \"in\",\n            \"objects\": [\"Content\"]\n        },\n        \"usesContent\": {\n            \"type\": \"associatedTo\",\n            \"direction\": \"out\",\n            \"objects\": [\"Content\"]\n        },\n        \"itemSets\": {\n            \"type\": \"associatedTo\",\n            \"direction\": \"out\",\n            \"objects\": [\"ItemSet\"]\n        }\n    },\n    \"version\": \"enable\",\n    \"versionCheckMode\": \"ON\",\n    \"frameworkCategories\": [\"board\",\"medium\",\"subject\",\"gradeLevel\",\"difficultyLevel\",\"topic\", \"subDomains\", \"subjectCodes\"],\n    \"edge\": {\n        \"properties\": {\n            \"license\": \"License\"\n        }\n    },\n    \"copy\": {\n        \"scheme\": {\n            \"TextBookToCourse\": {\n                \"TextBook\": \"Course\",\n                \"TextBookUnit\": \"CourseUnit\"\n            },\n            \"TextBookToLessonPlan\": {\n            }\n        }\n    },\n    \"cacheEnabled\": true,\n    \"searchProps\": {\n        \"status\": [\"Live\"],\n        \"softConstraints\": {\n            \"medium\": 15,\n            \"subject\": 15,\n            \"ageGroup\": 1,\n            \"gradeLevel\": 7,\n            \"board\": 4,\n            \"relatedBoards\": 4\n        }\n    },\n    \"schema_restrict_api\": true\n}"
        JsonUtils.deserialize(schema, classOf[util.Map[String, AnyRef]])
    }

    def getFrameworkNode(): Node = {
        val node = new Node()
        node.setIdentifier("NCF")
        node.setNodeType("DATA_NODE")
        node.setObjectType("Framework")
        node.setGraphId("domain")
        node.setMetadata(mapAsJavaMap(Map("name"-> "NCF")))
        node
    }

    def getBoardNode(): Node = {
        val node = new Node()
        node.setIdentifier("ncf_board_cbse")
        node.setNodeType("DATA_NODE")
        node.setObjectType("Term")
        node.setGraphId("domain")
        node.setMetadata(mapAsJavaMap(Map("name"-> "CBSE")))
        node
    }

    def getValidNodeToReject(): Node = {
        val node = new Node()
        node.setIdentifier("do_1234")
        node.setNodeType("DATA_NODE")
        node.setObjectType("Content")
        node.setMetadata(new util.HashMap[String, AnyRef]() {
            {
                put("identifier", "do_1234")
                put("mimeType", "application/pdf")
                put("status", "Review")
                put("reviewStatus", "InReview")
                put("contentType", "Resource")
                put("name", "Node To Reject")
                put("versionKey", "test_123")
                put("channel", "in.ekstep")
                put("code", "Resource_1")
                put("primaryCategory", "Learning Resource")
            }
        })
        node
    }
}
