package controllers.v4

import java.io.File

import controllers.base.BaseSpec
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import play.api.test.FakeRequest
import play.api.test.Helpers.{OK, status}
import play.api.test.Helpers._
import play.api.libs.Files.{SingletonTemporaryFileCreator, TemporaryFile}
import play.api.libs.json.JsValue
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData.{BadPart, FilePart}
import play.api.libs.json.Json

@RunWith(classOf[JUnitRunner])
class ContentSpec extends BaseSpec {

    "Content Controller " should {
        "return success response for create API" in {
            val controller = app.injector.instanceOf[controllers.v4.ContentController]
            val json: JsValue = Json.parse("""{"request": {"content": {"primaryCategory": "Learning Resource"}}}""")
            val fakeRequest = FakeRequest("POST", "/content/v4/create ").withJsonBody(json)
            val result = controller.create()(fakeRequest)
            isOK(result)
            status(result) must equalTo(OK)
        }

        "return success response for update API" in {
            val controller = app.injector.instanceOf[controllers.v4.ContentController]
            val result = controller.update("do_123")(FakeRequest())
            isOK(result)
            status(result) must equalTo(OK)
        }

        "return success response for read API" in {
            val controller = app.injector.instanceOf[controllers.v4.ContentController]
            val result = controller.read("do_123", None, None)(FakeRequest())
            isOK(result)
            status(result) must equalTo(OK)
        }

        "return success response for flag API" in {
            val controller = app.injector.instanceOf[controllers.v4.ContentController]
            val result = controller.flag("do_123")(FakeRequest())
            isOK(result)
            status(result) must equalTo(OK)
        }

        "return success response for acceptFlag API" in {
            val controller = app.injector.instanceOf[controllers.v4.ContentController]
            val result = controller.acceptFlag("do_123")(FakeRequest())
            isOK(result)
            status(result) must equalTo(OK)
        }

        "return success response for rejectFlag API" in {
            val controller = app.injector.instanceOf[controllers.v4.ContentController]
            val result = controller.rejectFlag("do_123")(FakeRequest())
            isOK(result)
            status(result) must equalTo(OK)
        }

        "return success response for discard API" in {
            val controller = app.injector.instanceOf[controllers.v4.ContentController]
            val result = controller.discard("0123")(FakeRequest())
            isOK(result)
            status(result) must equalTo(OK)
        }
        "return success response for retire API" in {
            val controller = app.injector.instanceOf[controllers.v4.ContentController]
            val result = controller.retire("0123")(FakeRequest())
            isOK(result)
            status(result) must equalTo(OK)
        }

        "return success response for linkDialCode API" in {
            val controller = app.injector.instanceOf[controllers.v4.ContentController]
            val result = controller.linkDialCode()(FakeRequest())
            isOK(result)
            status(result) must equalTo(OK)
        }

        "return success response for presignedUrl upload API" in {
            val controller = app.injector.instanceOf[controllers.v4.ContentController]
            val result = controller.uploadPreSigned("01234", None)(FakeRequest())
            isOK(result)
            status(result) must equalTo(OK)
        }

        "return success response for rejectContent API" in {
            val controller = app.injector.instanceOf[controllers.v4.ContentController]
            val json: JsValue = Json.parse("""{"request": {"content": {"primaryCategory": "Learning Resource","status": "Review"}}}""")
            val fakeRequest = FakeRequest("POST", "/content/v4/review/reject/do_123").withJsonBody(json)
            val result = controller.systemUpdate("do_123")(fakeRequest)
            isOK(result)
            status(result) must equalTo(OK)
        }
    }
    "return success response for upload API with file" in {
        val controller = app.injector.instanceOf[controllers.v4.ContentController]
        val file = new File("test/resources/sample.pdf")
        val files = Seq[FilePart[TemporaryFile]](FilePart("file", "sample.pdf", None, SingletonTemporaryFileCreator.create(file.toPath)))
        val multipartBody = MultipartFormData(Map[String, Seq[String]](), files, Seq[BadPart]())
        val fakeRequest = FakeRequest().withMultipartFormDataBody(multipartBody)
        val result = controller.upload("01234", None, None)(fakeRequest)
        isOK(result)
        status(result) must equalTo(OK)
    }

    "return success response for upload API with fileUrl" in {
        val controller = app.injector.instanceOf[controllers.v4.ContentController]
        val file = new File("test/resources/sample.pdf")
        val files = Seq[FilePart[TemporaryFile]](FilePart("file", "sample.pdf", None, SingletonTemporaryFileCreator.create(file.toPath)))
        val multipartBody = MultipartFormData(Map[String, Seq[String]]("fileUrl" -> Seq("https://abc.com/content/sample.pdf"), "filePath" -> Seq("/program/id")), files, Seq[BadPart]())
        val fakeRequest = FakeRequest().withMultipartFormDataBody(multipartBody)
        val result = controller.upload("01234", None, None)(fakeRequest)
        isOK(result)
        status(result) must equalTo(OK)
    }

    "return success response for upload API with fileUrl and fileFormat" in {
        val controller = app.injector.instanceOf[controllers.v4.ContentController]
        val file = new File("test/resources/sample.pdf")
        val files = Seq[FilePart[TemporaryFile]](FilePart("file", "sample.pdf", None, SingletonTemporaryFileCreator.create(file.toPath)))
        val multipartBody = MultipartFormData(Map[String, Seq[String]](), files, Seq[BadPart]())
        val fakeRequest = FakeRequest().withMultipartFormDataBody(multipartBody)
        val result = controller.upload("01234", Some("composed-h5p-zip"), None)(fakeRequest)
        isOK(result)
        status(result) must equalTo(OK)
    }

    "return success response for importContent API" in {
        val controller = app.injector.instanceOf[controllers.v4.ContentController]
        val result = controller.importContent()(FakeRequest())
        isOK(result)
        status(result) must equalTo(OK)
    }


    "return success response for copy API" in {
        val controller = app.injector.instanceOf[controllers.v4.ContentController]
        val json: JsValue = Json.parse("""{"request": {"content": {"primaryCategory": "Asset"}}}""")
        val fakeRequest = FakeRequest("POST", "/content/v4/copy/do_123").withJsonBody(json)
        val result = controller.copy("do_123", Option.apply("read"), "shallowCopy")(fakeRequest)
        isOK(result)
        status(result) must equalTo(OK)
    }

    "Content Controller with invalid request " should {
        "return client error response for create API" in {
            val controller = app.injector.instanceOf[controllers.v4.ContentController]
            val json: JsValue = Json.parse("""{"request": {"content": { "contentType": "TextBook"}}}""")
            val fakeRequest = FakeRequest("POST", "/content/v4/create ").withJsonBody(json)
            val result = controller.create()(fakeRequest)
            status(result) must equalTo(BAD_REQUEST)
        }
    }

    "Content Controller with invalid request " should {
        "return client error response for create API" in {
            val controller = app.injector.instanceOf[controllers.v4.ContentController]
            val json: JsValue = Json.parse("""{"request": {"content": { "name": "Resource"}}}""")
            val fakeRequest = FakeRequest("POST", "/content/v4/create ").withJsonBody(json)
            val result = controller.create()(fakeRequest)
            status(result) must equalTo(BAD_REQUEST)
        }
    }

    "Content Controller with valid request " should {
        "return success response for systemUpdate API" in {
            val controller = app.injector.instanceOf[controllers.v4.ContentController]
            val json: JsValue = Json.parse("""{"request": {"content": {"primaryCategory": "Asset"}}}""")
            val fakeRequest = FakeRequest("POST", "/content/v4/system/update/do_123").withJsonBody(json)
            val result = controller.systemUpdate("do_123")(fakeRequest)
            isOK(result)
            status(result) must equalTo(OK)
        }
    }
}
