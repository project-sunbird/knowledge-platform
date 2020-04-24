package org.sunbird.actors

import java.util
import java.util.Collections
import java.util.concurrent.{CompletionException, ConcurrentHashMap, CopyOnWriteArrayList}
import scala.collection.JavaConverters._
import org.sunbird.graph.dac.model.Relation
import javax.inject.Inject
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.StringUtils
import org.sunbird.common.{Platform, Slug}
import org.sunbird.common.dto.{Request, Response, ResponseHandler}
import org.sunbird.common.exception.{ClientException, ResponseCode}
import org.sunbird.graph.OntologyEngineContext
import org.sunbird.graph.nodes.DataNode
import org.sunbird.manager.BaseTaxonomyActor
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}

class FrameworkTermActor @Inject()(implicit oec: OntologyEngineContext) extends BaseTaxonomyActor {

    override def onReceive(request: Request): Future[Response] = request.getOperation match {
        case "createTerm" => create(request)
        case "retireTerm" => retire(request)
        case "updateTerm" => update(request)
        case _ => ERROR(request.getOperation)
    }

    private val TERM_CREATION_LIMIT = {
        if (Platform.config.hasPath("framework.max_term_creation_limit")) Platform.config.getInt("framework.max_term_creation_limit")
        else 200
    }

    @SuppressWarnings(Array("unchecked"))
    private def create(request: Request): Future[Response] = {
        val requestList: util.List[util.Map[String, AnyRef]] = getRequestData(request)
        if (null == request.get("term") || null == requestList || requestList.isEmpty)
            throw new ClientException("ERR_INVALID_TERM_OBJECT", "Invalid Request")
        if (TERM_CREATION_LIMIT < requestList.size)
            throw new ClientException("ERR_INVALID_TERM_REQUEST", "No. of request exceeded max limit of " + TERM_CREATION_LIMIT)
        val responseList: util.List[util.List[String]] = new util.ArrayList[util.List[String]]()
        val response = getTermList(requestList, request, responseList)
        response.map(resp => {
            if (resp != null) {
                val response = ResponseHandler.OK()
                response.put("identifier", responseList)
                response
            }
            else {
                ResponseHandler.ERROR(ResponseCode.SERVER_ERROR, "ERR_SERVER_ERROR", "Internal Server Error")
            }
        })
    }

    private def getTermList(requestList: util.List[util.Map[String, AnyRef]], request: Request, responseList:util.List[util.List[String]])(implicit ec : ExecutionContext)= {
        var categoryId: String = request.get("categoryId").asInstanceOf[String]
        val scopeId: String = request.get("frameworkId").asInstanceOf[String]
        if (null != scopeId) {
            categoryId = generateIdentifier(scopeId, categoryId)
            validateRequest(scopeId, categoryId, request)
        }
        else {
            validateCategoryNode(request)
        }
        var id: String = null
        var futureList = new ListBuffer[Future[Response]]
        val identifiers: util.List[String] = new util.ArrayList[String]()
        val futureResponseList = requestList.asScala.foreach((requestMap: util.Map[String, AnyRef])=> {
            val req = new Request()
            req.getRequest.putAll(requestMap)
            req.setContext(request.getContext)
            val code: String = req.get("code").asInstanceOf[String]
            if (StringUtils.isNotBlank(code)) {
                if (!requestMap.containsKey("parents") || req.get("parents").asInstanceOf[util.List[AnyRef]].isEmpty) {
                    setRelations(categoryId, req)
                }
                id = generateIdentifier(categoryId, code)
                if (id != null) {
                    req.put("identifier", id)
                }
                req.put("category", request.get("categoryId").asInstanceOf[String])
                val response =  createTerm(req, identifiers)
                response.map(resp => {
                    val response = resp.asInstanceOf[Future[Response]]
                    futureList += response
                    Future.sequence(futureList.toList)
                })
            }
            else {
                futureList
            }
        })
        Future(futureResponseList)
    }
    
    private def generateIdentifier(scopeId: String, code: String) = {
        var id: String = null
        if (StringUtils.isNotBlank(scopeId))
            id = Slug.makeSlug(scopeId + "_" + code)
        id
    }

    private def validateRequest(scopeId: String, categoryId: String, request: Request) = {
        var valid: Boolean = false
        val originalCategory: String = request.get("categoryId").asInstanceOf[String]
        if (StringUtils.isNotBlank(scopeId) && StringUtils.isNotBlank(categoryId) && !StringUtils.equals(categoryId, "_") && !StringUtils.equals(categoryId, scopeId + "_")) {
            request.put("identifier", "categoryId")
            DataNode.read(request).map(resp => {
                if (resp != null) {
                    if (!StringUtils.equals(originalCategory, resp.getMetadata.get("code").asInstanceOf[String]))
                        throw new ClientException("ERR_INVALID_CATEGORY", "Please provide a valid category")
                    if (StringUtils.equalsIgnoreCase(categoryId, resp.getIdentifier)) {
                        val inRelation: List[Relation] = resp.getInRelations.asInstanceOf[List[Relation]]
                        if (!inRelation.isEmpty) {
                            for (relation <- inRelation) {
                                if (StringUtils.equalsIgnoreCase(scopeId, relation.getStartNodeId)) {
                                    valid = true
                                }
                            }
                        }
                    }
                }
                else throw new ClientException("ERR_INVALID_CATEGORY_ID", "Please provide valid category.")
                if (!valid) throw new ClientException("ERR_INVALID_CATEGORY_ID", "Please provide valid category framework.")
            })
        }
        else throw new ClientException("ERR_INVALID_CATEGORY_ID", "Please provide required fields. category framework should not be empty.")
    }

    private def validateCategoryNode(request: Request) = {
        val categoryId: String = request.get("categoryId").asInstanceOf[String]
        if (StringUtils.isNotBlank(categoryId)) {
            request.put("identifier", categoryId)
            val response: Response = DataNode.read(request).asInstanceOf[Response]
            if (ResponseHandler.checkError(response))
                throw new ClientException("ERR_INVALID_CATEGORY_ID", "Please provide valid category.")
        }
        else
            throw new ClientException("ERR_INVALID_CATEGORY_ID", "Please provide valid category. It should not be empty.")
    }

    @SuppressWarnings(Array("unchecked"))
    private def getRequestData(request: Request): util.List[util.Map[String, AnyRef]] = {
        if(request.get("term").isInstanceOf[util.List[AnyRef]]){
            request.get("term").asInstanceOf[util.List[util.Map[String, AnyRef]]]
        }
        else
        {
            val requestObj:util.List[util.Map[String, AnyRef]] = new CopyOnWriteArrayList[util.Map[String, AnyRef]]
            requestObj.add(request.get("term").asInstanceOf[util.Map[String, AnyRef]])
            requestObj
        }
    }

    def createTerm(request: Request, identifiers: util.List[String]):Future[Response]= {
        validateTranslations(request)
        DataNode.create(request).map(resp => {
            identifiers.add(resp.getIdentifier)
            ResponseHandler.OK()
        })
    }

    private def setRelations(scopeId: String, request: Request) = {
        request.put("identifier", scopeId)
        DataNode.read(request).map(resp => {
            val objectType: String = resp.getObjectType
            val relationList: util.List[util.Map[String, AnyRef]] = new util.ArrayList[util.Map[String, AnyRef]]()
            val relationMap: util.Map[String, AnyRef] = new ConcurrentHashMap[String, AnyRef]
            relationMap.put("identifier", scopeId)
            relationMap.put("relation", "hasSequenceMember")
            if (StringUtils.isNotBlank(request.get("index").asInstanceOf[String]))
                relationMap.put("index", request.get("index"))
            relationList.add(relationMap)
            objectType.toLowerCase match {
                case "framework" =>
                    request.put("frameworks", relationList)
                case "category" =>
                    request.put("categories", relationList)
                case "categoryinstance" =>
                    request.put("categories", relationList)
                case "channel" =>
                    request.put("channels", relationList)
                case "term" =>
                    request.put("terms", relationList)
            }
        })
    }

    def retire(request: Request):Future[Response] = {
        var newCategoryId = request.get("categoryId").asInstanceOf[String]
        val scopeId = request.get("frameworkId").asInstanceOf[String]
        if(scopeId != null) {
            newCategoryId = generateIdentifier(scopeId, newCategoryId)
            validateRequest(scopeId, newCategoryId, request)
        }
        else {
            validateCategoryNode(request)
        }
        val newTermId:String = generateIdentifier(newCategoryId,request.get("termId").asInstanceOf[String])
        validateScopeNode(newTermId,request).map(respNode => {
            if(respNode){
                request.getContext.put("identifier",newTermId)
                request.put("status", "Retired")
                DataNode.update(request).map(node => {
                    val response = ResponseHandler.OK
                    response.put("identifier", node.getIdentifier)
                    response
                })
            }
            else
            {
                throw new ClientException("ERR_CATEGORY_NOT_FOUND", "Category/CategoryInstance is not related Term");
            }
        }).flatMap(f => f) recoverWith { case e: CompletionException => throw e.getCause }
    }

    def validateScopeNode(categoryId:String, request: Request): Future[Boolean] = {
        val frameworkIdentifier = request.get("identifier").asInstanceOf[String]
        request.put("identifier",categoryId)
        DataNode.read(request).map(node =>
        {
            if(node != null){
                val relations: java.util.List[Relation] = node.getInRelations
                if (CollectionUtils.isNotEmpty(relations)) {
                    val startNodeId = relations.asScala.map(resp => resp.getStartNodeId).filter(startNodeId => StringUtils.equalsIgnoreCase(frameworkIdentifier,startNodeId)).toList.distinct
                    if(startNodeId != null){
                        true
                    }
                    else
                        false
                }
                else
                    false
            }
            else
                false
        })
    }

    def update(request: Request):Future[Response] = {
        if(request == null){
            throw new ClientException("ERR_INVALID_CATEGORY_INSTANCE_OBJECT", "Invalid Request");
        }
        if(request.getRequest.containsKey("code")) {
            throw new ClientException("ERR_CODE_UPDATION_NOT_ALLOWED", "Term Code cannot be updated");
        }
        var categoryId:String = request.get("categoryId").asInstanceOf[String]
        val scopeId:String = request.get("frameworkId").asInstanceOf[String]
        if(scopeId != null) {
            categoryId = generateIdentifier(scopeId, categoryId)
            validateRequest(scopeId,categoryId,request)
        }
        else
            validateCategoryNode(request)
        val newTermId = generateIdentifier(categoryId, request.get("termId").asInstanceOf[String])
        if(!request.getRequest.containsKey("parents")) {
            setRelations(categoryId, request)
            request.put("parents", Collections.emptyList())
        }
        else {
            request.getContext.put("identifier", categoryId)
            DataNode.read(request).map(node => {
                val objectType: String = node.getObjectType
                if (StringUtils.equalsAnyIgnoreCase(StringUtils.lowerCase(objectType), "categoryinstance")) {
                    request.put("categories", Collections.emptyList() )
                }
            })
        }
        request.put("category",request.get("categoryId").asInstanceOf[String])
        request.getContext.put("identifier",newTermId)
        validateTranslations(request)
        DataNode.update(request).map(resp => {
            val response = ResponseHandler.OK()
            response.put("identifier", resp.getIdentifier)
            response
        })
    }
}
