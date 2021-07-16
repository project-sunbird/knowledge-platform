package controllers.v4


import controllers.base.BaseSpec
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import play.api.test.FakeRequest
import play.api.test.Helpers.{OK, status}
import play.api.test.Helpers._
import play.api.libs.json.JsValue
import play.api.libs.json.Json

@RunWith(classOf[JUnitRunner])
class CollectionSpec extends BaseSpec {

    "Collection Controller " should {
        "return success response for create API" in {
            val controller = app.injector.instanceOf[controllers.v4.CollectionController]
            val json: JsValue = Json.parse("""{"request": {"collection": {"name": "Collection","primaryCategory": "Digital Textbook"}}}""")
            val fakeRequest = FakeRequest("POST", "/collection/v4/create ").withJsonBody(json)
            val result = controller.create()(fakeRequest)
            isOK(result)
            status(result) must equalTo(OK)
        }

        "return success response for update API" in {
            val controller = app.injector.instanceOf[controllers.v4.CollectionController]
            val result = controller.update("do_123")(FakeRequest())
            isOK(result)
            status(result) must equalTo(OK)
        }

        "return success response for read API" in {
            val controller = app.injector.instanceOf[controllers.v4.CollectionController]
            val result = controller.read("do_123", None, None)(FakeRequest())
            isOK(result)
            status(result) must equalTo(OK)
        }

        "return success response for hierarchy add API" in {
            val controller = app.injector.instanceOf[controllers.v4.CollectionController]
            val result = controller.addHierarchy()(FakeRequest())
            isOK(result)
            status(result) must equalTo(OK)
        }

        "return success response for hierarchy remove API" in {
            val controller = app.injector.instanceOf[controllers.v4.CollectionController]
            val result = controller.removeHierarchy()(FakeRequest())
            isOK(result)
            status(result) must equalTo(OK)
        }

        "return success response for hierarchy get API" in {
            val controller = app.injector.instanceOf[controllers.v4.CollectionController]
            val result = controller.getHierarchy("do_123", None)(FakeRequest())
            isOK(result)
            status(result) must equalTo(OK)
        }
        "return success response for flag API" in {
            val controller = app.injector.instanceOf[controllers.v4.CollectionController]
            val result = controller.flag("do_123")(FakeRequest())
            isOK(result)
            status(result) must equalTo(OK)
        }
        "return success response for acceptFlag API" in {
            val controller = app.injector.instanceOf[controllers.v4.CollectionController]
            val result = controller.acceptFlag("do_123")(FakeRequest())
            isOK(result)
            status(result) must equalTo(OK)
        }

        "return success response for discard API" in {
            val controller = app.injector.instanceOf[controllers.v4.CollectionController]
            val result = controller.discard("0123")(FakeRequest())
            isOK(result)
            status(result) must equalTo(OK)
        }
        "return success response for retire API" in {
            val controller = app.injector.instanceOf[controllers.v4.CollectionController]
            val result = controller.retire("0123")(FakeRequest())
            isOK(result)
            status(result) must equalTo(OK)
        }

        "return success response for collectionLinkDialCode API" in {
            val controller = app.injector.instanceOf[controllers.v4.CollectionController]
            val result = controller.collectionLinkDialCode("do_123")(FakeRequest())
            isOK(result)
            status(result) must equalTo(OK)
        }

        "return success response for collection review reject API" in {
          val controller = app.injector.instanceOf[controllers.v4.CollectionController]
          val result = controller.reviewReject("do_123")(FakeRequest())
          isOK(result)
          status(result) must equalTo(OK)
        }

        "return success response for collection review API" in {
            val controller = app.injector.instanceOf[controllers.v4.CollectionController]
            val result = controller.review("do_123")(FakeRequest())
            isOK(result)
            status(result) must equalTo(OK)
        }
    }

    "return success response for hierarchy update API" in {
        val controller = app.injector.instanceOf[controllers.v4.CollectionController]
        val json: JsValue = Json.parse("""{"request": {"data": {"mimeType": "application/vnd.ekstep.content-collection"}}}""")
        val fakeRequest = FakeRequest("POST", "/collection/v4/hierarchy/update").withJsonBody(json)
        val result = controller.updateHierarchy()(fakeRequest)
        isOK(result)
        status(result) must equalTo(OK)
    }

    "return success response for bookmark hierarchy API" in {
        val controller = app.injector.instanceOf[controllers.v4.CollectionController]
        val result = controller.getBookmarkHierarchy("do_123", "do_1234", Option.apply("read"))(FakeRequest())
        isOK(result)
        status(result) must equalTo(OK)
    }

    "return success response for copy API" in {
        val controller = app.injector.instanceOf[controllers.v4.CollectionController]
        val json: JsValue = Json.parse("""{"request": {"collection": {"primaryCategory": "Asset"}}}""")
        val fakeRequest = FakeRequest("POST", "/collection/v4/copy/do_123").withJsonBody(json)
        val result = controller.copy("do_123", Option.apply("read"), "shallowCopy")(fakeRequest)
        isOK(result)
        status(result) must equalTo(OK)
    }

    "Collection Controller with invalid request " should {
        "return client error response for create API" in {
            val controller = app.injector.instanceOf[controllers.v4.CollectionController]
            val json: JsValue = Json.parse("""{"request": {"collection": { "contentType": "TextBook"}}}""")
            val fakeRequest = FakeRequest("POST", "/collection/v4/create ").withJsonBody(json)
            val result = controller.create()(fakeRequest)
            status(result) must equalTo(BAD_REQUEST)
        }
    }

    "Collection Controller with invalid request " should {
        "return client error response for create API" in {
            val controller = app.injector.instanceOf[controllers.v4.CollectionController]
            val json: JsValue = Json.parse("""{"request": {"collection": { "name": "Textbook"}}}""")
            val fakeRequest = FakeRequest("POST", "/collection/v4/create ").withJsonBody(json)
            val result = controller.create()(fakeRequest)
            status(result) must equalTo(BAD_REQUEST)
        }
    }

    "Collection Controller with valid request " should {
        "return success response for systemUpdate API" in {
            val controller = app.injector.instanceOf[controllers.v4.CollectionController]
            val json: JsValue = Json.parse("""{"request": {"collection": {"primaryCategory": "Asset"}}}""")
            val fakeRequest = FakeRequest("POST", "/collection/v4/system/update/do_123").withJsonBody(json)
            val result = controller.systemUpdate("do_123")(fakeRequest)
            isOK(result)
            status(result) must equalTo(OK)
        }
    }
}
