package org.sunbird.mimetype.ecml.processor

import org.apache.commons.lang3.StringUtils
import org.sunbird.common.exception.ClientException
import org.sunbird.graph.utils.ScalaJsonUtils
import scala.collection.mutable.ListBuffer

object JsonParser {
    val nonPluginElements: List[String] = List("manifest", "controller", "media", "events", "event", "__cdata", "__text")

    def parse(jsonString: String): Plugin = {
        val jsonMap:Map[String, AnyRef] = ScalaJsonUtils.deserialize[Map[String, AnyRef]](jsonString)
        processDocument(jsonMap)
    }

    def processDocument(json: Map[String, AnyRef]): Plugin = {
        if(json.keySet.contains("theme")){
            val root = json.get("theme").get.asInstanceOf[Map[String, AnyRef]]
            Plugin(getId(root), getData(root, "theme"), getInnerText(root), getCdata(root), getChildrenPlugin(root), getManifest(root, true), getControllers(root), getEvents(root))
        } else classOf[Plugin].newInstance()
    }


    def getDatafromJsonObject(jsonObject: Map[String, AnyRef], elementName: String): String = {
        if(null != jsonObject && jsonObject.keySet.contains(elementName)){
            jsonObject.get(elementName).get.asInstanceOf[String]
        }else ""
    }


    def getId(jsonObject: Map[String, AnyRef]): String = getDatafromJsonObject(jsonObject, "id")

    def getData(jsonObject: Map[String, AnyRef], elementName: String): Map[String, AnyRef] = {
        if(null != jsonObject && StringUtils.isNotBlank(elementName)){
            var result = jsonObject.filter(p => !p._1.equalsIgnoreCase("__cdata") && !p._1.equalsIgnoreCase("__text"))
            result += ("cwp_element_name" -> elementName)
            result
        } else Map[String, AnyRef]()
    }

    def getInnerText(jsonObject: Map[String, AnyRef]): String = getDatafromJsonObject(jsonObject, "__text")

    def getCdata(jsonObject: Map[String, AnyRef]): String = ScalaJsonUtils.serialize(jsonObject.getOrElse("__cdata",""))

    def getChildrenPlugin(jsonObject: Map[String, AnyRef]): List[Plugin] = {
        var childPluginList: ListBuffer[Plugin] = ListBuffer()
        val filteredObject = jsonObject.filter(entry => null != entry._2)
        childPluginList ++= filteredObject.filter(entry=> entry._2.isInstanceOf[List[Map[String, AnyRef]]] && !nonPluginElements.contains(entry._1)).map(entry => {
            val objectList:List[Map[String, AnyRef]] = entry._2.asInstanceOf[List[Map[String, AnyRef]]]
            objectList.map(obj => Plugin(getId(obj), getData(obj, entry._1), getInnerText(obj), getCdata(obj), getChildrenPlugin(obj), getManifest(obj, false), getControllers(obj), getEvents(obj)))
        }).toList.flatten
        childPluginList ++= filteredObject.filter(entry => entry._2.isInstanceOf[Map[String, AnyRef]] && !nonPluginElements.contains(entry._2)).map(entry => {
            val obj = entry._2.asInstanceOf[Map[String, AnyRef]]
            Plugin(getId(obj), getData(obj, entry._1), getInnerText(obj), getCdata(obj), getChildrenPlugin(obj), getManifest(obj, false), getControllers(obj), getEvents(obj))
        }).toList
        childPluginList.toList
    }

    def getManifest(jsonObject: Map[String, AnyRef], validateMedia: Boolean): Manifest = {
        if(null != jsonObject && jsonObject.keySet.contains("manifest") && jsonObject.get("manifest").get.isInstanceOf[List[Map[String, AnyRef]]]) throw new ClientException("EXPECTED_JSON_OBJECT", "Error! JSON Object is Expected for the Element. manifest")
        else if(jsonObject.get("manifest").isDefined && jsonObject.get("manifest").get.isInstanceOf[Map[String, AnyRef]] && jsonObject.get("manifest").get.asInstanceOf[Map[String, AnyRef]].keySet.contains("media")){
            val manifestObject = jsonObject.get("manifest").get.asInstanceOf[Map[String, AnyRef]]
            Manifest(getId(manifestObject), getData(manifestObject, "manifest"), getInnerText(manifestObject), getCdata(manifestObject), getMedias(manifestObject.get("media").get, validateMedia))
        }else classOf[Manifest].newInstance()
    }

    def getControllers(jsonObject: Map[String, AnyRef]): List[Controller] = {
        if(null != jsonObject &&  jsonObject.keySet.contains("controller") && jsonObject.get("controller").get.isInstanceOf[List[Map[String, Object]]]){
            val controllerList:List[Map[String, AnyRef]] = jsonObject.get("controller").get.asInstanceOf[List[Map[String, Object]]]
            controllerList.map(obj =>{
                validateController(obj)
                Controller(getId(obj), getData(obj, "controller"), getInnerText(obj), getCdata(obj))
            })
        } else if(null != jsonObject &&  jsonObject.keySet.contains("controller") && jsonObject.get("controller").isInstanceOf[Map[String, Object]]) {
            val obj = jsonObject.get("controller").get.asInstanceOf[Map[String, Object]]
            validateController(obj)
            List(Controller(getId(obj), getData(obj, "controller"), getInnerText(obj), getCdata(obj)))
        }else List()
    }

    def validateController(obj: Map[String, AnyRef]) = {
        val id = obj.get("id").get.asInstanceOf[String]
        val `type` = obj.get("type").get.asInstanceOf[String]

        if(null == id || StringUtils.isBlank(id.toString))
            throw new ClientException("INVALID_CONTROLLER", "Error! Invalid Controller ('id' is required.)")
        if(null == `type` || StringUtils.isBlank(`type`))
            throw new ClientException("INVALID_CONTROLLER", "Error! Invalid Controller ('type' is required.)")
        if(!"items".equalsIgnoreCase(`type`) && !"data".equalsIgnoreCase(`type`))
            throw new ClientException("INVALID_CONTROLLER", "Error! Invalid Controller ('type' should be either 'items' or 'data')")
    }
    
    def getEventsfromObject(jsonObject: AnyRef): List[Event] = {
        if(null != jsonObject && jsonObject.isInstanceOf[List[Map[String, AnyRef]]]){
            val jsonList:List[Map[String, AnyRef]] = jsonObject.asInstanceOf[List[Map[String, AnyRef]]]
            jsonList.map(obj => Event(getId(obj), getData(obj, "event"), getInnerText(obj), getCdata(obj), getChildrenPlugin(obj)))
        }else if(null != jsonObject && jsonObject.isInstanceOf[Map[String, AnyRef]]) {
            List(Event(getId(jsonObject.asInstanceOf[Map[String, AnyRef]]), getData(jsonObject.asInstanceOf[Map[String, AnyRef]], "event"), getInnerText(jsonObject.asInstanceOf[Map[String, AnyRef]]), getCdata(jsonObject.asInstanceOf[Map[String, AnyRef]]), getChildrenPlugin(jsonObject.asInstanceOf[Map[String, AnyRef]])))
        }else List()
    }

    def getEvents(jsonObject: Map[String, AnyRef]): List[Event] = {
        var eventList: ListBuffer[Event] = ListBuffer()
        if(null != jsonObject && jsonObject.keySet.contains("events")) {
            val value = jsonObject.get("events").get
            if(value.isInstanceOf[List[Map[String, AnyRef]]]){
                val jsonList:List[Map[String, AnyRef]] = value.asInstanceOf[List[Map[String, AnyRef]]]
                eventList ++= jsonList.map(obj => Event(getId(obj), getData(obj, "event"), getInnerText(obj), getCdata(obj), getChildrenPlugin(obj)))
            } else if(value.isInstanceOf[Map[String, AnyRef]]){
                eventList ++= getEventsfromObject(value)
            }
        } else if(jsonObject.keySet.contains("event")) {
            eventList ++= getEventsfromObject(jsonObject.get("event").get)
        }
        eventList.toList
    }

    def getMedias(manifestObject: AnyRef, validateMedia: Boolean): List[Media] = {
        if(null != manifestObject && manifestObject.isInstanceOf[List[Map[String, AnyRef]]]) {
            val jsonList:List[Map[String, AnyRef]] = manifestObject.asInstanceOf[List[Map[String, AnyRef]]]
            jsonList.map(json => getMedia(json, validateMedia))
        } else if(null != manifestObject && manifestObject.isInstanceOf[Map[String, AnyRef]]) {
            List(getMedia(manifestObject.asInstanceOf[Map[String, AnyRef]], validateMedia))
        } else List()
    }

    def getMedia(mediaJson: Map[String, AnyRef], validateMedia: Boolean): Media = {
        if(null != mediaJson && validateMedia) {
            val id = getDatafromJsonObject(mediaJson, "id")
            val src = getDatafromJsonObject(mediaJson, "src")
            val `type` = getDatafromJsonObject(mediaJson, "type")
            if(StringUtils.isBlank(id) && isMediaIdRequired(`type`))
                throw new ClientException("INVALID_MEDIA", "Error! Invalid Media ('id' is required.)")
            if(StringUtils.isBlank(`type`))
                throw new ClientException("INVALID_MEDIA", "Error! Invalid Media ('type' is required.)")
            if(StringUtils.isBlank(src))
                throw new ClientException("INVALID_MEDIA", "Error! Invalid Media ('src' is required.)")
            Media(id, getData(mediaJson, "media"), getInnerText(mediaJson), getCdata(mediaJson), src, `type`, getChildrenPlugin(mediaJson))
        } else classOf[Media].newInstance()
    }

    def isMediaIdRequired(`type`: String): Boolean = {
        if(StringUtils.isNotBlank(`type`) &&(`type`.equalsIgnoreCase("js") || `type`.equalsIgnoreCase("css"))) false else true
    }


    /**
     * serialize
     * @param plugin
     * @return
     */
    def toString(plugin: Plugin): String = {
        val map:Map[String, AnyRef] = plugin.data ++ Map[String, AnyRef]("__text" -> plugin.innerText, "__cdata" -> plugin.cData) ++ getManifestMap(plugin.manifest) ++ getControllersMap(plugin.controllers) ++ getEventsMap(plugin.events)
        ScalaJsonUtils.serialize(Map[String, AnyRef]("theme" -> map))
    }

    def getManifestMap(manifest: Manifest):Map[String, AnyRef] = {
        if(null != manifest && null != manifest.medias && !manifest.medias.isEmpty){
            val map:Map[String, AnyRef] = manifest.data ++ Map[String, AnyRef]("__text" -> manifest.innerText, "__cdata" -> manifest.cData) ++ getMediaMap(manifest.medias)
            Map[String, AnyRef]("manifest" -> map)
        }else Map[String, AnyRef]()
    }

    def getControllersMap(controllers: List[Controller]):Map[String, AnyRef] = {
        if(null != controllers && !controllers.isEmpty){
           val controllerMap:List[Map[String, AnyRef]] =  controllers.map(controller => {
                controller.data ++ Map[String, AnyRef]("__text" -> controller.innerText, "__cdata" -> controller.cData)
            })
            Map[String, AnyRef]("controller" -> controllerMap)
        }else Map[String, AnyRef]()
    }

    def getEventsMap(events: List[Event]):Map[String, AnyRef] = {
        if(null != events && !events.isEmpty){
            val eventsList: List[Map[String, AnyRef]] = events.map(event => event.data ++ Map[String, AnyRef]("__text" -> event.innerText, "__cdata" -> event.cData) ++ getChildPluginMap(event.childrenPlugin))

            if(eventsList.length > 1) {
                Map[String, AnyRef]("events" -> Map[String, AnyRef]("event" -> eventsList))
            }else {
                Map[String, AnyRef]("event" -> eventsList.head)
            }
        } else Map[String, AnyRef]()
    }

    def getMediaMap(medias: List[Media]): Map[String, AnyRef] = {
        val mediasMap = medias.map(media => media.data ++ Map[String, AnyRef]("__text" -> media.innerText, "__cdata" -> media.cData) ++ getChildPluginMap(media.childrenPlugin))
        Map[String, AnyRef]("media" -> mediasMap)
    }

    def getChildPluginMap(plugins: List[Plugin]):Map[String, AnyRef] = {
        if(null != plugins && !plugins.isEmpty){
            plugins.filter(plugin => StringUtils.isNotBlank(plugin.data.get("cwp_element_name").get.asInstanceOf[String]))
                    .groupBy(plugin => plugin.data.get("cwp_element_name").get.asInstanceOf[String])
                    .map(entry => {
                        if(entry._2.size == 1)
                            entry._1 -> entry._2.map(plugin => plugin.data ++ Map[String, AnyRef]("__text" -> plugin.innerText, "__cdata" -> plugin.cData) ++ getManifestMap(plugin.manifest) ++ getControllersMap(plugin.controllers) ++ getEventsMap(plugin.events)).head
                        else
                            entry._1 -> entry._2.map(plugin => plugin.data ++ Map[String, AnyRef]("__text" -> plugin.innerText, "__cdata" -> plugin.cData) ++ getManifestMap(plugin.manifest) ++ getControllersMap(plugin.controllers) ++ getEventsMap(plugin.events))
                    })
        }else  Map[String, AnyRef]()
    }
    
}
