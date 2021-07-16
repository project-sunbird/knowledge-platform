package org.sunbird.mimetype.mgr.impl

import java.io.File
import java.util

import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.{AsyncFlatSpec, Matchers}
import org.sunbird.models.UploadParams
import org.sunbird.cloudstore.StorageService
import org.sunbird.common.exception.ClientException
import org.sunbird.graph.OntologyEngineContext
import org.sunbird.graph.dac.model.Node

class YouTubeMimeTypeMgrImplTest extends AsyncFlatSpec with Matchers with AsyncMockFactory {
	implicit val ss: StorageService = new StorageService

	"upload with valid youtube url" should "return artifactUrl with successful response" in {
		val inputUrl = "https://www.youtube.com/watch?v=8irSFvoyLHQ"
		val resFuture = new YouTubeMimeTypeMgrImpl().upload("do_123", new Node(), inputUrl, None, UploadParams())
		resFuture.map(result => {
			assert(null != result)
			assert(!result.isEmpty)
			assert("do_123" == result.getOrElse("identifier",""))
			assert(inputUrl == result.getOrElse("artifactUrl",""))
		})
	}

	"upload with file" should "throw client exception" in {
		val exception = intercept[ClientException] {
			new YouTubeMimeTypeMgrImpl().upload("do_123", new Node(), new File("/tmp/test.pdf"), None, UploadParams())
		}
		exception.getMessage shouldEqual "FILE_UPLOAD_ERROR | Upload operation not supported for given mimeType"
	}

	"review with valid youtube url" should "return successful result" in {
		implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
		val inputUrl = "https://www.youtube.com/watch?v=OQMlqYs22b8&ab_channel=SmartSchoolDhaurahraInUP"
		val node = getNode("1234")
		node.getMetadata.put("artifactUrl", inputUrl)
		val resFuture = new YouTubeMimeTypeMgrImpl().review("do_1234", node)
		resFuture.map(result => {
			assert(null != result)
			assert(!result.isEmpty)
			assert("Review" == result.getOrElse("status",""))
			assert("creativeCommon" == result.getOrElse("license",""))
		})
	}

	"review with invalid youtube url" should "return client error" in {
		implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
		val inputUrl = "http://test.com/sample.pdf"
		val node = getNode("1234")
		node.getMetadata.put("artifactUrl", inputUrl)
		val exception = intercept[ClientException] {
			new YouTubeMimeTypeMgrImpl().review("do_1234", node)
		}
		exception.getMessage shouldEqual "Invalid Youtube Url Detected!"
	}

	"review with invalid artifact url" should "return client error" in {
		implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
		val node = getNode("1234")
		node.getMetadata.put("artifactUrl", "")
		val exception = intercept[ClientException] {
			new YouTubeMimeTypeMgrImpl().review("do_1234", node)
		}
		exception.getMessage shouldEqual "Either artifactUrl is missing or invalid!"
	}

	def getNode(identifier: String): Node = {
		val node = new Node()
		node.setIdentifier(identifier)
		node.setMetadata(new util.HashMap[String, AnyRef](){{
			put("identifier", identifier)
			put("mimeType", "video/x-youtube")
			put("status","Draft")
			put("contentType", "Resource")
		}})
		node.setObjectType("Content")
		node
	}

}
