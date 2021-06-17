package org.sunbird.content.actors

import java.util

import akka.actor.Props
import org.apache.hadoop.util.StringUtils
import org.scalamock.scalatest.MockFactory
import org.sunbird.cloudstore.StorageService
import org.sunbird.common.dto.Request
import org.sunbird.common.exception.ResponseCode
import org.sunbird.graph.{GraphService, OntologyEngineContext}
import org.sunbird.graph.dac.model.{Node, SearchCriteria}

import scala.collection.JavaConversions.mapAsJavaMap
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class TestLicenseActor extends BaseSpec with MockFactory {

  "LicenseActor" should "return failed response for 'unknown' operation" in {
    implicit val oec: OntologyEngineContext = new OntologyEngineContext
    testUnknownOperation(Props(new LicenseActor()), getLicenseRequest())
  }

  it should "create a licenseNode and store it in neo4j" in {
    implicit val ss = mock[StorageService]
    implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
    val graphDB = mock[GraphService]
    (oec.graphService _).expects().returns(graphDB).anyNumberOfTimes()
    (graphDB.addNode(_: String, _: Node)).expects(*, *).returns(Future(getValidNode())).anyNumberOfTimes()

    val nodes: util.List[Node] = getCategoryNode()
    (graphDB.getNodeByUniqueIds(_: String, _: SearchCriteria)).expects(*, *).returns(Future(nodes)).anyNumberOfTimes()

    val request = getLicenseRequest()
    request.put("name", "do_1234")
    request.setOperation("createLicense")
    val response = callActor(request, Props(new LicenseActor()))
    assert(response.get("identifier") != null)
    assert(response.get("identifier").equals("do_1234"))
  }

  it should "return exception for create license without name" in {
    implicit val ss = mock[StorageService]
    implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
    val request = getLicenseRequest()
    request.setOperation("createLicense")
    val response = callActor(request, Props(new LicenseActor()))
    assert(response.getResponseCode == ResponseCode.CLIENT_ERROR)
    assert(StringUtils.equalsIgnoreCase(response.get("messages").asInstanceOf[util.ArrayList[String]].get(0).asInstanceOf[String], "Required Metadata name not set"))
  }

  it should "return exception for licenseNode with identifier" in {
    implicit val ss = mock[StorageService]
    implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
    val request = getLicenseRequest()
    request.putAll(mapAsJavaMap(Map("identifier" -> "do_1234")))
    request.setOperation("createLicense")
    val response = callActor(request, Props(new LicenseActor()))
    assert(response.getResponseCode == ResponseCode.CLIENT_ERROR)
    assert(StringUtils.equalsIgnoreCase(response.getParams.getErrmsg, "name will be set as identifier"))
  }

  it should "return success response for updateLicense" in {
    implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
    val graphDB = mock[GraphService]
    (oec.graphService _).expects().returns(graphDB).anyNumberOfTimes()

    val nodes: util.List[Node] = getCategoryNode()
    (graphDB.getNodeByUniqueIds(_: String, _: SearchCriteria)).expects(*, *).returns(Future(nodes)).anyNumberOfTimes()

    val node = getValidNode()
    (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(node)).anyNumberOfTimes()
    (graphDB.upsertNode(_: String, _: Node, _: Request)).expects(*, *, *).returns(Future(getValidNode()))

    implicit val ss = mock[StorageService]
    val request = getLicenseRequest()
    request.putAll(mapAsJavaMap(Map("description" -> "test desc")))
    request.setOperation("updateLicense")
    val response = callActor(request, Props(new LicenseActor()))
    assert("successful".equals(response.getParams.getStatus))
    assert(response.get("identifier").equals("do_1234"))
  }

  it should "return success response for readLicense" in {
    implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
    val graphDB = mock[GraphService]
    (oec.graphService _).expects().returns(graphDB).repeated(1)
    val node = getValidNode()
    (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(node)).anyNumberOfTimes()
    implicit val ss = mock[StorageService]
    val request = getLicenseRequest()
    request.putAll(mapAsJavaMap(Map("fields" -> "")))
    request.setOperation("readLicense")
    val response = callActor(request, Props(new LicenseActor()))
    assert("successful".equals(response.getParams.getStatus))
    assert(StringUtils.equalsIgnoreCase(response.get("license").asInstanceOf[util.Map[String, AnyRef]].get("identifier").asInstanceOf[String], "do_1234"))
    assert(StringUtils.equalsIgnoreCase(response.get("license").asInstanceOf[util.Map[String, AnyRef]].get("status").asInstanceOf[String], "Live"))
  }

  it should "return success response for retireLicense" in {
    implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
    val graphDB = mock[GraphService]
    (oec.graphService _).expects().returns(graphDB).anyNumberOfTimes()//.repeated(2)

    val node = getValidNode()
    (graphDB.getNodeByUniqueId(_: String, _: String, _: Boolean, _: Request)).expects(*, *, *, *).returns(Future(node)).anyNumberOfTimes()
    (graphDB.upsertNode(_: String, _: Node, _: Request)).expects(*, *, *).returns(Future(getValidNode()))

    val nodes: util.List[Node] = getCategoryNode()
    (graphDB.getNodeByUniqueIds(_: String, _: SearchCriteria)).expects(*, *).returns(Future(nodes)).anyNumberOfTimes()

    implicit val ss = mock[StorageService]
    val request = getLicenseRequest()
    request.setOperation("retireLicense")
    val response = callActor(request, Props(new LicenseActor()))
    assert("successful".equals(response.getParams.getStatus))
    assert(response.get("identifier").equals("do_1234"))
  }

  private def getLicenseRequest(): Request = {
    val request = new Request()
    request.setContext(new util.HashMap[String, AnyRef]() {
      {
        put("graph_id", "domain")
        put("version", "1.0")
        put("objectType", "License")
        put("schemaName", "license")

      }
    })
    request.setObjectType("License")
    request
  }

  private def getValidNode(): Node = {
    val node = new Node()
    node.setIdentifier("do_1234")
    node.setNodeType("DATA_NODE")
    node.setObjectType("License")
    node.setMetadata(new util.HashMap[String, AnyRef]() {
      {
        put("status", "Live")
        put("name", "do_1234")
      }
    })
    node
  }
}
