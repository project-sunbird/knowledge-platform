package controllers.v4

import akka.actor.{ActorRef, ActorSystem}
import com.google.inject.Singleton
import controllers.BaseController
import javax.inject.{Inject, Named}
import org.sunbird.models.UploadParams
import play.api.mvc.ControllerComponents
import utils.{ActorNames, ApiId}

import scala.collection.JavaConverters._

import scala.concurrent.{ExecutionContext}

@Singleton
class ContentController @Inject()(@Named(ActorNames.CONTENT_ACTOR) contentActor: ActorRef, cc: ControllerComponents, actorSystem: ActorSystem)(implicit exec: ExecutionContext) extends BaseController(cc) {

    val objectType = "Content"
    val schemaName: String = "content"
    val version = "1.0"
    val apiVersion = "4.0"


    def create() = Action.async { implicit request =>
        val headers = commonHeaders()
        val body = requestBody()
        val content = body.getOrDefault(schemaName, new java.util.HashMap()).asInstanceOf[java.util.Map[String, Object]];
        content.putAll(headers)
        if(!validatePrimaryCategory(content))
            getErrorResponse(ApiId.CREATE_CONTENT, apiVersion, "VALIDATION_ERROR", "primaryCategory is a mandatory parameter")
        else if(validateContentType(content))
            getErrorResponse(ApiId.CREATE_CONTENT, apiVersion, "VALIDATION_ERROR", "contentType cannot be set from request.")
        else {
            val contentRequest = getRequest(content, headers, "createContent", true)
            setRequestContext(contentRequest, version, objectType, schemaName)
            getResult(ApiId.CREATE_ASSET, contentActor, contentRequest, version = apiVersion)
        }
    }

    /**
      * This Api end point takes 3 parameters
      * Content Identifier the unique identifier of a content
      * Mode in which the content can be viewed (default read or edit)
      * Fields are metadata that should be returned to visualize
      *
      * @param identifier Identifier of the content
      * @param mode Mode to read the data edit or published
      * @param fields List of fields to return in the response
      */
    def read(identifier: String, mode: Option[String], fields: Option[String]) = Action.async { implicit request =>
        val headers = commonHeaders()
        val content = new java.util.HashMap().asInstanceOf[java.util.Map[String, Object]]
        content.putAll(headers)
        content.putAll(Map("identifier" -> identifier, "mode" -> mode.getOrElse("read"), "fields" -> fields.getOrElse("")).asJava)
        val readRequest = getRequest(content, headers, "readContent")
        setRequestContext(readRequest, version, objectType, schemaName)
        getResult(ApiId.READ_CONTENT, contentActor, readRequest, version = apiVersion)
    }

    def update(identifier: String) = Action.async { implicit request =>
        val headers = commonHeaders()
        val body = requestBody()
        val content = body.getOrDefault(schemaName, new java.util.HashMap()).asInstanceOf[java.util.Map[String, Object]];
        content.putAll(headers)
        val contentRequest = getRequest(content, headers, "updateContent")
        setRequestContext(contentRequest, version, objectType, schemaName)
        contentRequest.getContext.put("identifier", identifier);
        getResult(ApiId.UPDATE_CONTENT, contentActor, contentRequest, version = apiVersion)
    }

    def flag(identifier: String) = Action.async { implicit request =>
        val headers = commonHeaders()
        val body = requestBody()
        val content = body
        content.putAll(headers)
        content.putAll(Map("identifier" -> identifier).asJava)
        val contentRequest = getRequest(content, headers, "flagContent")
        setRequestContext(contentRequest, version, objectType, schemaName)
        contentRequest.getContext.put("identifier", identifier)
        getResult(ApiId.FlAG_CONTENT, contentActor, contentRequest, version = apiVersion)
    }

    def acceptFlag(identifier: String) = Action.async { implicit request =>
        val headers = commonHeaders()
        val content = new java.util.HashMap().asInstanceOf[java.util.Map[String, Object]]
        content.putAll(headers)
        content.putAll(Map("identifier" -> identifier).asJava)
        val acceptRequest = getRequest(content, headers, "acceptFlag")
        setRequestContext(acceptRequest, version, objectType, schemaName)
        getResult(ApiId.ACCEPT_FLAG, contentActor, acceptRequest)
    }

    def rejectFlag(identifier: String) = Action.async { implicit request =>
        val headers = commonHeaders()
        val body = requestBody()
        val content = body
        content.putAll(headers)
        content.putAll(Map("identifier" -> identifier).asJava)
        val rejectRequest = getRequest(content, headers, "rejectFlag")
        setRequestContext(rejectRequest, version, objectType, schemaName)
        rejectRequest.getContext.put("identifier", identifier);
        getResult(ApiId.REJECT_FLAG, contentActor, rejectRequest)
    }

    def discard(identifier: String) = Action.async { implicit request =>
        val headers = commonHeaders()
        val content = new java.util.HashMap().asInstanceOf[java.util.Map[String, Object]]
        content.putAll(headers)
        content.putAll(Map("identifier" -> identifier).asJava)
        val discardRequest = getRequest(content, headers, "discardContent")
        setRequestContext(discardRequest, version, objectType, schemaName)
        getResult(ApiId.DISCARD_CONTENT, contentActor, discardRequest)
    }
    def retire(identifier: String) = Action.async { implicit request =>
        val headers = commonHeaders()
        val body = requestBody()
        val content = body.getOrDefault(schemaName, new java.util.HashMap()).asInstanceOf[java.util.Map[String, Object]]
        content.put("identifier", identifier)
        content.putAll(headers)
        val contentRequest = getRequest(content, headers, "retireContent")
        setRequestContext(contentRequest, version, objectType, schemaName)
        getResult(ApiId.RETIRE_CONTENT, contentActor, contentRequest, version = apiVersion)
    }

    def linkDialCode() = Action.async { implicit request =>
        val headers = commonHeaders()
        val body = requestBody()
        body.putAll(headers)
        val contentRequest = getRequest(body, headers, "linkDIALCode")
        setRequestContext(contentRequest, version, objectType, schemaName)
        contentRequest.getContext.put("linkType", "content")
        getResult(ApiId.LINK_DIAL_CONTENT, contentActor, contentRequest, version = apiVersion)
    }

    def upload(identifier: String, fileFormat: Option[String], validation: Option[String]) = Action.async { implicit request =>
        val headers = commonHeaders()
        val content = requestFormData(identifier)
        content.putAll(headers)
        val contentRequest = getRequest(content, headers, "uploadContent")
        setRequestContext(contentRequest, version, objectType, schemaName)
        contentRequest.getContext.putAll(Map("identifier" ->  identifier, "params" -> UploadParams(fileFormat, validation.map(_.toBoolean))).asJava)
        getResult(ApiId.UPLOAD_CONTENT, contentActor, contentRequest, version = apiVersion)
    }


    def copy(identifier: String, mode: Option[String], copyType: String) = Action.async { implicit request =>
        val headers = commonHeaders()
        val body = requestBody()
        val content = body.getOrDefault(schemaName, new java.util.HashMap()).asInstanceOf[java.util.Map[String, Object]]
        content.putAll(headers)
        content.putAll(Map("identifier" -> identifier, "mode" -> mode.getOrElse(""), "copyType" -> copyType).asJava)
        val contentRequest = getRequest(content, headers, "copy")
        setRequestContext(contentRequest, version, objectType, schemaName)
        getResult(ApiId.COPY_CONTENT, contentActor, contentRequest, version = apiVersion)
    }

    def uploadPreSigned(identifier: String, `type`: Option[String])= Action.async { implicit request =>
        val headers = commonHeaders()
        val body = requestBody()
        val content = body.getOrDefault(schemaName, new java.util.HashMap()).asInstanceOf[java.util.Map[String, Object]]
        content.putAll(headers)
        content.putAll(Map("identifier" -> identifier, "type" -> `type`.getOrElse("assets")).asJava)
        val contentRequest = getRequest(content, headers, "uploadPreSignedUrl")
        setRequestContext(contentRequest, version, objectType, schemaName)
        getResult(ApiId.UPLOAD_PRE_SIGNED_CONTENT, contentActor, contentRequest, version = apiVersion)
    }

    def importContent() = Action.async { implicit request =>
        val headers = commonHeaders()
        val body = requestBody()
        body.putAll(headers)
        val contentRequest = getRequest(body, headers, "importContent")
        setRequestContext(contentRequest, version, objectType, schemaName)
        getResult(ApiId.IMPORT_CONTENT, contentActor, contentRequest, version = apiVersion)
    }

    def systemUpdate(identifier: String) = Action.async { implicit request =>
        val headers = commonHeaders()
        val body = requestBody()
        val content = body.getOrDefault(schemaName, new java.util.HashMap()).asInstanceOf[java.util.Map[String, Object]];
        content.putAll(headers)
        val contentRequest = getRequest(content, headers, "systemUpdate")
        setRequestContext(contentRequest, version, objectType, schemaName)
        contentRequest.getContext.put("identifier", identifier);
        getResult(ApiId.SYSTEM_UPDATE_CONTENT, contentActor, contentRequest, version = apiVersion)
    }

    def rejectContent(identifier: String) = Action.async { implicit request =>
        val headers = commonHeaders()
        val body = requestBody()
        val content = body
        content.putAll(headers)
        content.putAll(Map("identifier" -> identifier).asJava)
        val contentRequest = getRequest(content, headers, "rejectContent")
        setRequestContext(contentRequest, version, objectType, schemaName)
        contentRequest.getContext.put("identifier", identifier);
        getResult(ApiId.REJECT_CONTENT, contentActor, contentRequest, version = apiVersion)
    }

}
