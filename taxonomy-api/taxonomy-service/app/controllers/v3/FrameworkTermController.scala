package controllers.v3

import akka.actor.{ActorRef, ActorSystem}
import com.google.inject.Singleton
import controllers.BaseController
import javax.inject.{Inject, Named}
import play.api.mvc.ControllerComponents
import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}
import org.sunbird.common.dto.ResponseHandler
import utils.{ActorNames, ApiId, JavaJsonUtils}
@Singleton
class FrameworkTermController @Inject()(@Named(ActorNames.FRAMEWORK_TERM_ACTOR) frameworkTermActor: ActorRef, cc: ControllerComponents, actorSystem: ActorSystem)(implicit exec: ExecutionContext)  extends BaseController(cc) {

    val objectType = "Term"
    val schemaName: String = "term"
    val version = "1.0"

    def create(identifier:String, category: String) = Action.async { implicit request =>
        val headers = commonHeaders()
        val body = requestBody()
        val term = body
        term.putAll(headers)
        term.putAll(Map("frameworkId" -> identifier, "categoryId" -> category).asInstanceOf[Map[String, Object]])
        val termRequest = getRequest(term, headers, "createTerm")
        setRequestContext(termRequest, version, objectType, schemaName)
        getResult(ApiId.CREATE_FRAMEWORK_TERM, frameworkTermActor, termRequest)
    }

    def read(identifier: String, category: String) = Action.async { implicit request =>
        val result = ResponseHandler.OK()
        val response = JavaJsonUtils.serialize(result)
        Future(Ok(response).as("application/json"))
    }

    def update(identifier: String, framework: String, category: String) = Action.async { implicit request =>
        val headers = commonHeaders()
        val body = requestBody()
        val term = body.getOrElse("term",new java.util.HashMap()).asInstanceOf[java.util.Map[String, AnyRef]]
        term.putAll(headers)
        term.putAll(Map("termId"-> identifier,"frameworkId" -> framework, "categoryId" -> category).asInstanceOf[Map[String, Object]])
        val termRequest = getRequest(term, headers, "updateTerm")
        setRequestContext(termRequest, version, objectType, schemaName)
        getResult(ApiId.UPDATE_FRAMEWORK_TERM, frameworkTermActor, termRequest)
    }

    def search(category: String) = Action.async { implicit request =>
        val result = ResponseHandler.OK()
        val response = JavaJsonUtils.serialize(result)
        Future(Ok(response).as("application/json"))
    }
    def retire(identifier: String, framework: String, category: String) = Action.async { implicit request =>
        val headers = commonHeaders()
        val body = requestBody()
        val term = body
        term.putAll(headers)
        term.putAll(Map("termId" -> identifier, "frameworkId" -> framework, "categoryId" -> category).asInstanceOf[Map[String, Object]])
        val retiretermRequest = getRequest(term, headers, "retireTerm")
        setRequestContext(retiretermRequest, version, objectType, schemaName)
        getResult(ApiId.RETIRE_FRAMEWORK_TERM, frameworkTermActor, retiretermRequest)
    }

}
