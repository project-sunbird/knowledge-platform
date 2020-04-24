package controllers.v3

import controllers.base.BaseSpec
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import play.api.test.FakeRequest
import play.api.test.Helpers.{OK, status}
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class FrameworkTermControllerSpec extends BaseSpec {

    "Term Category Controller " should {
        "return success response for create API" in {
            val controller = app.injector.instanceOf[controllers.v3.FrameworkTermController]
            val result = controller.create("d0_123","grade level")(FakeRequest())
            isOK(result)
            status(result) must equalTo(OK)
        }
        
        "return success response for read API" in {
            val controller = app.injector.instanceOf[controllers.v3.FrameworkTermController]
            val result = controller.read("d0_123","grade level")(FakeRequest())
            isOK(result)
            status(result) must equalTo(OK)
        }

        "return success response for update API" in {
            val controller = app.injector.instanceOf[controllers.v3.FrameworkTermController]
            val result = controller.update("maths","d0_123","grade level")(FakeRequest())
            isOK(result)
            status(result) must equalTo(OK)
        }
        
        "return success response for search API" in {
            val controller = app.injector.instanceOf[controllers.v3.FrameworkTermController]
            val result = controller.search("grade level")(FakeRequest())
            isOK(result)
            status(result) must equalTo(OK)
        }

        "return success response for retire API" in {
            val controller = app.injector.instanceOf[controllers.v3.FrameworkTermController]
            val result = controller.retire("maths","d0_123","grade level")(FakeRequest())
            isOK(result)
            status(result) must equalTo(OK)
        }
    }

}
