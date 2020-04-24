package org.sunbird.actors


import java.util.{ArrayList, HashMap, List, Map}

import javax.inject.Inject
import org.sunbird.common.Slug
import org.sunbird.actor.core.BaseActor
import org.apache.commons.lang3.StringUtils
import org.sunbird.common.exception.ClientException
import org.sunbird.graph.dac.model.Relation
import org.sunbird.common.Platform
import org.apache.commons.collections4.CollectionUtils
import org.sunbird.common.dto.{Request, Response, ResponseHandler}
import org.sunbird.graph.OntologyEngineContext
import org.sunbird.graph.nodes.DataNode
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

class FrameworkCategoryActor @Inject() (implicit oec: OntologyEngineContext) extends BaseActor {

  implicit val ec: ExecutionContext = getContext().dispatcher

  private val CATEGORY_INSTANCE_OBJECT_TYPE = "CategoryInstance"

  val category_master = {
    if(Platform.config.hasPath("category.master"))
      Platform.config.getStringList("category.master")
    else{
      java.util.Arrays.asList("subject", "medium", "gradeLevel", "board", "topic")
    }
  }

  override def onReceive(request: Request): Future[Response] = request.getOperation match {
    case "createFrameworkCategory" => create(request)
    case "readFrameworkCategory" => read(request)
    case "updateFrameworkCategory" => update(request)
    case "retireFrameworkCategory" => retire(request)
    case _ => ERROR(request.getOperation)
  }

  def create(request: Request): Future[Response] =
    validateFrameworkId(request: Request).map( resp => {
      if (resp) {
        if (null == request.get("code") || StringUtils.isBlank(request.get("code").asInstanceOf[String])) {
          throw new ClientException("ERR_CATEGORY_INSTANCE_CODE_REQUIRED", "Unique code is mandatory for categoryInstance " + request.get("code"))
        }
        validateCategoryNode(request: Request)
        val frameworkId = request.get("identifier").asInstanceOf[String]
        val categoryId = generateIdentifier(frameworkId, request.get("code").asInstanceOf[String])
        if (null != categoryId)
          request.put("identifier", categoryId)
        setRelations(frameworkId: String, request: Request)
        DataNode.create(request).map(node => {
          val response = ResponseHandler.OK
          response.put("identifier", node.getIdentifier)
          response
        })
      }
      else
        throw new ClientException("ERR_INVALID_FRAMEWORK_ID", "Invalid FrameworkId: " + request.get("identifier"))
    }).flatMap(f =>f)

  def read(request:Request): Future[Response] =
    validateFrameworkId(request: Request).map( resp => {
      if (resp) {
        val categoryId = generateIdentifier(request.get("identifier").asInstanceOf[String],request.get("categoryInstanceId").asInstanceOf[String])
        validateScopeNode(categoryId:String, request:Request).map(node => {
          if(node){
            request.put("objectType",CATEGORY_INSTANCE_OBJECT_TYPE)
            DataNode.read(request).map(node =>
            {
              val response = ResponseHandler.OK
              response.put("response",node.getMetadata)
              response
            })
          }
          else
            throw new ClientException("ERR_INVALID_FRAMEWORK_ID",  "Given framework is not related to given category" + request.get("identifier"))

        }).flatMap(f => f)
      }
      else
        throw new ClientException("ERR_FRAMEWORK_NOT_FOUND", "Invalid FrameworkId: " + request.get("identifier"))
    }).flatMap(f => f)

  def update(request:Request): Future[Response] =
    validateFrameworkId(request: Request).map( resp => {
      if (resp) {
        val categoryId = generateIdentifier(request.get("identifier").asInstanceOf[String],request.get("categoryInstanceId").asInstanceOf[String])
        validateScopeNode(categoryId:String, request:Request).map(node => {
          if(node){
            request.put("objectType",CATEGORY_INSTANCE_OBJECT_TYPE)
            request.getContext.put("identifier", categoryId);
            DataNode.update(request).map(node =>
            {
              val response = ResponseHandler.OK
              response.put("response",node.getIdentifier)
              response.put("response",node.getMetadata)
              response
            })
          }
          else
            throw new ClientException("ERR_INVALID_FRAMEWORK_ID",  "Given framework is not related to given category" + request.get("identifier"))
        }).flatMap(f => f)
      }
      else
        throw new ClientException("ERR_FRAMEWORK_NOT_FOUND", "Invalid FrameworkId : " + request.get("identifier"))
    }).flatMap(f => f)


  def retire(request:Request): Future[Response] =
    validateFrameworkId(request: Request).map( resp => {
      if (resp) {
        val categoryId = generateIdentifier(request.get("identifier").asInstanceOf[String],request.get("categoryInstanceId").asInstanceOf[String])
        validateScopeNode(categoryId:String, request:Request).map(node => {
          if(node){
            request.put("status", "Retired")
            request.put("objectType",CATEGORY_INSTANCE_OBJECT_TYPE)
            request.getContext.put("identifier", categoryId);
            DataNode.update(request).map(node =>
            {
              val response = ResponseHandler.OK
              response.put("response",node.getMetadata)
              response.put("response",node.getIdentifier)
              response
            })
          }
          else
            throw new ClientException("ERR_INVALID_FRAMEWORK_ID",  "Given framework is not related to given category" + request.get("identifier"))
        }).flatMap(f => f)
      }
      else
        throw new ClientException("ERR_FRAMEWORK_NOT_FOUND", "Invalid FrameworkId : " + request.get("identifier"))
    }).flatMap(f => f)

  def validateFrameworkId(request:Request): Future[Boolean] = {
    if(StringUtils.isBlank(request.get("identifier").asInstanceOf[String]))
      Future(false)
    else {
      DataNode.read(request).map(resp =>
      {
        if(resp != null) {
          if(StringUtils.equalsIgnoreCase(request.get("identifier").asInstanceOf[String], resp.getIdentifier)) {
            true
          } else
            false
        } else
          false
      })

    }
  }

  def generateIdentifier(frameworkIdentifier: String, code: String) = {
    var id: String = null
    if (StringUtils.isNotBlank(frameworkIdentifier))
      id = Slug.makeSlug(frameworkIdentifier + "_" + code)
    id
  }

  def validateCategoryNode(request: Request) = {
    val categoryCode = request.get("code").asInstanceOf[String]
    if(!category_master.contains(categoryCode))
      throw new ClientException("ERR_CATEGORY_NOT_FOUND", "Given category does not belong to master category data");
  }

  def setRelations(frameworkId: String, request: Request) = {
    val relationList: List[Map[String, AnyRef]] = new ArrayList[Map[String, AnyRef]]()
    val relationMap: Map[String, AnyRef] = new HashMap[String, AnyRef]()
    relationMap.put("identifier", frameworkId)
    relationMap.put("relation", "hasSequenceMember")
    if (request.get("index") != null) {
      relationMap.put("index", request.get("index"))
    }
    relationList.add(relationMap)
    request.put("CategoryInstance", relationList)
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
}

