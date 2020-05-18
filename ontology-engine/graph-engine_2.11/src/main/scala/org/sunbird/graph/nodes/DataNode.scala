package org.sunbird.graph.nodes

import java.util
import java.util.Optional
import java.util.concurrent.CompletionException

import org.apache.commons.collections4.{CollectionUtils, MapUtils}
import org.apache.commons.lang3.StringUtils
import org.sunbird.common.dto.{Request, Response}
import org.sunbird.common.exception.{ClientException, ErrorCodes}
import org.sunbird.graph.OntologyEngineContext
import org.sunbird.graph.common.enums.SystemProperties
import org.sunbird.graph.dac.model.{Filter, MetadataCriterion, Node, Relation, SearchConditions, SearchCriteria}
import org.sunbird.graph.external.ExternalPropsManager
import org.sunbird.graph.schema.DefinitionNode
import org.sunbird.graph.service.operation.{GraphAsyncOperations, NodeAsyncOperations, SearchAsyncOperations}
import org.sunbird.parseq.Task

import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}


object DataNode {
    @throws[Exception]
    def create(request: Request)(implicit oec: OntologyEngineContext, ec: ExecutionContext): Future[Node] = {
        val graphId: String = request.getContext.get("graph_id").asInstanceOf[String]
        DefinitionNode.validate(request).map(node => {
            val response = oec.graphService.addNode(graphId, node)
            response.map(node => DefinitionNode.postProcessor(request, node)).map(result => {
                val futureList = Task.parallel[Response](
                    saveExternalProperties(node.getIdentifier, node.getExternalData, request.getContext, request.getObjectType),
                    createRelations(graphId, node, request.getContext))
                futureList.map(list => result)
            }).flatMap(f => f) recoverWith { case e: CompletionException => throw e.getCause}
        }).flatMap(f => f)
    }

    @throws[Exception]
    def update(request: Request)(implicit oec: OntologyEngineContext, ec: ExecutionContext): Future[Node] = {
        val graphId: String = request.getContext.get("graph_id").asInstanceOf[String]
        val identifier: String = request.getContext.get("identifier").asInstanceOf[String]
        DefinitionNode.validate(identifier, request).map(node => {
            val response = oec.graphService.upsertNode(graphId, node, request)
            response.map(node => DefinitionNode.postProcessor(request, node)).map(result => {
                val futureList = Task.parallel[Response](
                    saveExternalProperties(node.getIdentifier, node.getExternalData, request.getContext, request.getObjectType),
                    updateRelations(graphId, node, request.getContext))
                futureList.map(list => result)
            }).flatMap(f => f)  recoverWith { case e: CompletionException => throw e.getCause}
        }).flatMap(f => f)
    }

    @throws[Exception]
    def read(request: Request)(implicit oec: OntologyEngineContext, ec: ExecutionContext): Future[Node] = {
        val schemaName: String = request.getContext.get("schemaName").asInstanceOf[String]
        DefinitionNode.getNode(request).map(node => {
            val fields: List[String] = Optional.ofNullable(request.get("fields").asInstanceOf[util.List[String]]).orElse(new util.ArrayList[String]()).toList
            val extPropNameList = DefinitionNode.getExternalProps(request.getContext.get("graph_id").asInstanceOf[String], request.getContext.get("version").asInstanceOf[String], schemaName)
            if (CollectionUtils.isNotEmpty(extPropNameList) && null != fields && fields.exists(field => extPropNameList.contains(field)))
                populateExternalProperties(fields, node, request, extPropNameList)
            else
                Future(node)
        }).flatMap(f => f) recoverWith {
            case e: CompletionException => throw e.getCause
        }
    }


    @throws[Exception]
    def list(request: Request)(implicit ec: ExecutionContext, oec: OntologyEngineContext): Future[util.List[Node]] = {
        val identifiers:util.List[String] = request.get("identifiers").asInstanceOf[util.List[String]]

        if(null == identifiers || identifiers.isEmpty) {
            throw new ClientException(ErrorCodes.ERR_BAD_REQUEST.name(), "identifiers is mandatory")
        } else {
            val mc: MetadataCriterion = MetadataCriterion.create(new util.ArrayList[Filter](){{
                if(identifiers.size() == 1)
                    add(new Filter(SystemProperties.IL_UNIQUE_ID.name(), SearchConditions.OP_EQUAL, identifiers.get(0)))
                if(identifiers.size() > 1)
                    add(new Filter(SystemProperties.IL_UNIQUE_ID.name(), SearchConditions.OP_IN, identifiers))
                new Filter("status", SearchConditions.OP_NOT_EQUAL, "Retired")
            }})

            val searchCriteria =  new SearchCriteria {{
                addMetadata(mc)
                setCountQuery(false)
            }}
            oec.graphService.getNodeByUniqueIds(request.getContext.get("graph_id").asInstanceOf[String], searchCriteria)
        }
    }

    @throws[Exception]
    def bulkUpdate(request: Request)(implicit ec: ExecutionContext, oec: OntologyEngineContext): Future[util.Map[String, Node]] = {
        val graphId: String = request.getContext.get("graph_id").asInstanceOf[String]
        val identifiers: util.List[String] = request.get("identifiers").asInstanceOf[util.List[String]]
        val metadata: util.Map[String, AnyRef] = request.get("metadata").asInstanceOf[util.Map[String, AnyRef]]
        oec.graphService.updateNodes(graphId, identifiers, metadata)
    }

    @throws[Exception]
    def deleteNode(request: Request)(implicit ec: ExecutionContext, oec: OntologyEngineContext): Future[java.lang.Boolean] = {
        val graphId: String = request.getContext.getOrDefault("graph_id", "").asInstanceOf[String]
        val identifier: String = request.getRequest.getOrDefault("identifier", "").asInstanceOf[String]
        oec.graphService.deleteNode(graphId, identifier, request)
    }

    private def saveExternalProperties(identifier: String, externalProps: util.Map[String, AnyRef], context: util.Map[String, AnyRef], objectType: String)(implicit ec: ExecutionContext): Future[Response] = {
        if (MapUtils.isNotEmpty(externalProps)) {
            externalProps.put("identifier", identifier)
            val request = new Request(context, externalProps, "", objectType)
            ExternalPropsManager.saveProps(request)
        } else {
            Future(new Response)
        }
    }
    
    private def createRelations(graphId: String, node: Node, context: util.Map[String, AnyRef])(implicit ec: ExecutionContext) : Future[Response] = {
        val relations: util.List[Relation] = node.getAddedRelations
        if (CollectionUtils.isNotEmpty(relations)) {
            GraphAsyncOperations.createRelation(graphId,getRelationMap(relations))
        } else {
            Future(new Response)
        }
    }

    private def populateExternalProperties(fields: List[String], node: Node, request: Request, externalProps: List[String])(implicit ec: ExecutionContext): Future[Node] = {
        if(StringUtils.equalsIgnoreCase(request.get("mode").asInstanceOf[String], "edit"))
            request.put("identifier", node.getIdentifier)
        val externalPropsResponse = ExternalPropsManager.fetchProps(request, externalProps.filter(prop => fields.contains(prop)))
        externalPropsResponse.map(response => {
            node.getMetadata.putAll(response.getResult)
            Future {
                node
            }
        }).flatMap(f => f)
    }

    private def updateRelations(graphId: String, node: Node, context: util.Map[String, AnyRef])(implicit ec: ExecutionContext) : Future[Response] = {
        val request: Request = new Request
        request.setContext(context)

        if (CollectionUtils.isEmpty(node.getAddedRelations) && CollectionUtils.isEmpty(node.getDeletedRelations)) {
            Future(new Response)
        } else {
            if (CollectionUtils.isNotEmpty(node.getDeletedRelations))
                GraphAsyncOperations.removeRelation(graphId, getRelationMap(node.getDeletedRelations))
            if (CollectionUtils.isNotEmpty(node.getAddedRelations))
                GraphAsyncOperations.createRelation(graphId,getRelationMap(node.getAddedRelations))
            Future(new Response)
        }
    }

    // TODO: this method should be in GraphAsyncOperations.
    private def getRelationMap(relations:util.List[Relation]):java.util.List[util.Map[String, AnyRef]]={
        val list = new util.ArrayList[util.Map[String, AnyRef]]
        for (rel <- relations) {
            if ((StringUtils.isNotBlank(rel.getStartNodeId) && StringUtils.isNotBlank(rel.getEndNodeId)) && StringUtils.isNotBlank(rel.getRelationType)) {
                val map = new util.HashMap[String, AnyRef]
                map.put("startNodeId", rel.getStartNodeId)
                map.put("endNodeId", rel.getEndNodeId)
                map.put("relation", rel.getRelationType)
                if (MapUtils.isNotEmpty(rel.getMetadata)) map.put("relMetadata", rel.getMetadata)
                else map.put("relMetadata", new util.HashMap[String,AnyRef]())
                list.add(map)
            }
            else throw new ClientException("ERR_INVALID_RELATION_OBJECT", "Invalid Relation Object Found.")
        }
        list
    }
}
