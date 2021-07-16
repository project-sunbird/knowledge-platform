package org.sunbird.mimetype.mgr.impl

import java.io.File
import java.util

import org.scalamock.scalatest.AsyncMockFactory
import org.scalatest.{AsyncFlatSpec, Matchers}
import org.sunbird.models.UploadParams
import org.sunbird.cloudstore.StorageService
import org.sunbird.common.dto.{Request, Response}
import org.sunbird.common.exception.ClientException
import org.sunbird.graph.{GraphService, OntologyEngineContext}
import org.sunbird.graph.dac.model.{Node, SearchCriteria}

import scala.concurrent.Future

class CollectionMimeTypeMgrImplTest  extends AsyncFlatSpec with Matchers with AsyncMockFactory {

	implicit val ss: StorageService = new StorageService

	"upload with file" should "throw client exception" in {
		val exception = intercept[ClientException] {
			new CollectionMimeTypeMgrImpl().upload("do_123", new Node(), new File("/tmp/test.pdf"), None, UploadParams())
		}
		exception.getMessage shouldEqual "FILE_UPLOAD_ERROR | Upload operation not supported for given mimeType"
	}

	"upload with fileUrl" should "throw client exception" in {
		val exception = intercept[ClientException] {
			new CollectionMimeTypeMgrImpl().upload("do_123", new Node(), "https://abc.com/content/sample.pdf", None, UploadParams())
		}
		exception.getMessage shouldEqual "FILE_UPLOAD_ERROR | Upload operation not supported for given mimeType"
	}

	"review with collection" should "review and validate resources successfully" in {
		implicit val ss = mock[StorageService]
		implicit val oec: OntologyEngineContext = mock[OntologyEngineContext]
		val graphDB = mock[GraphService]
		(oec.graphService _).expects().returns(graphDB).anyNumberOfTimes()
		val hierarchy = """{"ownershipType":["createdBy"],"year":"2010","subject":["Mathematics"],"channel":"01309282781705830427","organisation":["NIT"],"language":["English"],"mimeType":"application/vnd.ekstep.content-collection","objectType":"Collection","gradeLevel":["Class 1"],"primaryCategory":"Digital Textbook","children":[{"ownershipType":["createdBy"],"parent":"do_113320291783286784118","code":"410290e4-80b8-426a-9374-1c84ce7a8134","credentials":{"enabled":"No"},"channel":"01309282781705830427","description":"Unit-1","language":["English"],"mimeType":"application/vnd.ekstep.content-collection","idealScreenSize":"normal","createdOn":"2021-07-11T14:05:22.993+0000","objectType":"Content","primaryCategory":"Textbook Unit","children":[{"ownershipType":["createdBy"],"parent":"do_113320292949958656119","copyright":"2020","previewUrl":"https://sunbirddev.blob.core.windows.net/sunbird-content-dev/content/do_113117748227260416119/artifact/vidyadaan-guidlines-updated-1.pdf","subject":["Mathematics"],"channel":"01309282781705830427","downloadUrl":"https://sunbirddev.blob.core.windows.net/sunbird-content-dev/ecar_files/do_113117748227260416119/pdf_1601287628731_do_113117748227260416119_1.0.ecar","language":["English"],"mimeType":"application/pdf","variants":{"spine":{"ecarUrl":"https://sunbirddev.blob.core.windows.net/sunbird-content-dev/ecar_files/do_113117748227260416119/pdf_1601287628979_do_113117748227260416119_1.0_spine.ecar","size":1244}},"objectType":"Content","apoc_text":"APOC","se_mediums":["English"],"gradeLevel":["Class 1"],"primaryCategory":"Learning Resource","appId":"dev.dock.portal","contentEncoding":"identity","artifactUrl":"https://sunbirddev.blob.core.windows.net/sunbird-content-dev/content/do_113117748227260416119/artifact/vidyadaan-guidlines-updated-1.pdf","sYS_INTERNAL_LAST_UPDATED_ON":"2020-09-28T10:07:09.302+0000","contentType":"LearningActivity","apoc_num":1,"identifier":"do_113117748227260416119","audience":["Student"],"visibility":"Default","index":1,"mediaType":"content","osId":"org.ekstep.quiz.app","languageCode":["en"],"lastPublishedBy":"5a587cc1-e018-4859-a0a8-e842650b9d64","version":2,"pragma":["external"],"license":"CC BY 4.0","prevState":"Review","size":91022,"lastPublishedOn":"2020-09-28T10:07:08.730+0000","name":"pdf","status":"Live","code":"9d3504c4-7976-1555-0d89-30435aa2031c","apoc_json":"{\"batch\": true}","credentials":{"enabled":"No"},"prevStatus":"Processing","origin":"do_1131177479094026241694","streamingUrl":"https://sunbirddev.blob.core.windows.net/sunbird-content-dev/content/do_113117748227260416119/artifact/vidyadaan-guidlines-updated-1.pdf","medium":["English"],"idealScreenSize":"normal","createdOn":"2020-09-28T10:07:05.403+0000","processId":"5c6f2720-0172-11eb-90f6-35d24aa9a959","contentDisposition":"inline","lastUpdatedOn":"2020-09-28T10:07:08.450+0000","originData":{"identifier":"do_1131177479094026241694","repository":"https://dock.sunbirded.org/api/content/v1/read/do_1131177479094026241694"},"collectionId":"do_1131177466774405121681","dialcodeRequired":"No","lastStatusChangedOn":"2020-09-28T10:07:09.279+0000","creator":"anusha","os":["All"],"cloudStorageKey":"content/do_113117748227260416119/artifact/vidyadaan-guidlines-updated-1.pdf","pkgVersion":1,"versionKey":"1601287628450","idealScreenDensity":"hdpi","framework":"ekstep_ncert_k-12","depth":2,"s3Key":"ecar_files/do_113117748227260416119/pdf_1601287628731_do_113117748227260416119_1.0.ecar","lastSubmittedOn":"2020-09-28T10:07:07.043+0000","createdBy":"19ba0e4e-9285-4335-8dd0-f674bf03fa4d","compatibilityLevel":4,"board":"CBSE","programId":"de0f50d0-0171-11eb-90f6-35d24aa9a959","resourceType":"Read"}],"contentDisposition":"inline","lastUpdatedOn":"2021-07-11T14:05:22.992+0000","contentEncoding":"gzip","generateDIALCodes":"No","contentType":"TextBookUnit","dialcodeRequired":"No","identifier":"do_113320292949958656119","lastStatusChangedOn":"2021-07-11T14:05:22.993+0000","audience":["Student"],"os":["All"],"visibility":"Parent","discussionForum":{"enabled":"Yes"},"index":1,"mediaType":"content","osId":"org.ekstep.launcher","languageCode":["en"],"version":2,"versionKey":"1626012322993","license":"CC BY 4.0","idealScreenDensity":"hdpi","framework":"ekstep_ncert_k-12","depth":1,"compatibilityLevel":1,"name":"Unit-1","status":"Draft"}],"contentEncoding":"gzip","lockKey":"52a7eff4-04da-4241-8dfc-38348fac6c53","generateDIALCodes":"No","contentType":"TextBook","trackable":{"enabled":"No","autoBatch":"No"},"identifier":"do_113320291783286784118","audience":["Student"],"visibility":"Default","consumerId":"273f3b18-5dda-4a27-984a-060c7cd398d3","childNodes":["do_113117748227260416119","do_113320292949958656119"],"discussionForum":{"enabled":"Yes"},"mediaType":"content","osId":"org.ekstep.quiz.app","languageCode":["en"],"version":2,"license":"CC BY 4.0","name":"Test_G_01","status":"Draft","code":"org.sunbird.IgdX9L","credentials":{"enabled":"No"},"description":"Enter description for TextBook","medium":["English"],"idealScreenSize":"normal","createdOn":"2021-07-11T14:03:00.588+0000","contentDisposition":"inline","additionalCategories":["Textbook"],"lastUpdatedOn":"2021-07-11T14:05:23.550+0000","dialcodeRequired":"No","lastStatusChangedOn":"2021-07-11T14:03:00.588+0000","createdFor":["01309282781705830427"],"creator":"N131","os":["All"],"versionKey":"1626012323550","idealScreenDensity":"hdpi","framework":"ekstep_ncert_k-12","depth":0,"createdBy":"0b71985d-fcb0-4018-ab14-83f10c3b0426","compatibilityLevel":1,"userConsent":"Yes","board":"CBSE","resourceType":"Book"}"""
		val response = new Response
		response.put("hierarchy", hierarchy)
		(graphDB.readExternalProps(_: Request, _: List[String])).expects(*, *).returns(Future(response)).anyNumberOfTimes()
		(graphDB.getNodeByUniqueIds(_: String, _: SearchCriteria)).expects(*, *).returns(Future(getNodes())).anyNumberOfTimes()
		val node = getNode("do_113320291783286784118", "application/vnd.ekstep.content-collection", "Draft", "Collection", "TextBook")
		val identifier = "do_113320291783286784118"
		val resFuture = new CollectionMimeTypeMgrImpl().review(identifier, node)
		resFuture.map(result => {
			assert(null != result)
			assert(result.nonEmpty)
			assert("Review" == result.getOrElse("status",""))
		})
	}

	def getNodes(): util.List[Node] = {
		val result = new util.ArrayList[Node](){{
			add(getNode("do_113117748227260416119", "application/pdf", "Live", "Content", "Resource"))
		}}
		result
	}

	def getNode(identifier: String, mimeType: String, status: String, objectType: String, contentType: String): Node = {
		val node = new Node()
		node.setIdentifier(identifier)
		node.setObjectType(objectType)
		node.setMetadata(new util.HashMap[String, AnyRef](){{
			put("identifier", identifier)
			put("mimeType", mimeType)
			put("status",status)
			put("contentType", contentType)
		}})
		node
	}


}
