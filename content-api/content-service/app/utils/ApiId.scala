package utils

object ApiId {

	final val APPLICATION_HEALTH = "api.content.health"
	final val APPLICATION_SERVICE_HEALTH = "api.content.service.health"

	//Content APIs
	val CREATE_CONTENT = "api.content.create"
	val READ_CONTENT = "api.content.read"
	val UPDATE_CONTENT = "api.content.update"
	val UPLOAD_CONTENT = "api.content.upload"
	val RETIRE_CONTENT = "api.content.retire"
	val COPY_CONTENT = "api.content.copy"
	val UPLOAD_PRE_SIGNED_CONTENT = "api.content.upload.url"
	val DISCARD_CONTENT = "api.content.discard"
	val FlAG_CONTENT = "api.content.flag"
	val ACCEPT_FLAG = "api.content.flag.accept"
	val REJECT_FLAG = "api.content.flag.reject"
	val LINK_DIAL_CONTENT = "api.content.dialcode.link"
	val IMPORT_CONTENT = "api.content.import"
	val SYSTEM_UPDATE_CONTENT = "api.content.system.update"
	val REJECT_CONTENT = "api.content.review.reject"

	// Collection APIs
	val ADD_HIERARCHY = "api.content.hierarchy.add"
	val REMOVE_HIERARCHY = "api.content.hierarchy.remove"
	val UPDATE_HIERARCHY = "api.content.hierarchy.update"
	val GET_HIERARCHY = "api.content.hierarchy.get"
	val LINK_DIAL_COLLECTION = "api.collection.dialcode.link"

	//License APIs
	val CREATE_LICENSE = "api.license.create"
	val READ_LICENSE = "api.license.read"
	val UPDATE_LICENSE = "api.license.update"
	val RETIRE_LICENSE = "api.license.retire"

	//Channel APIs
	val CREATE_CHANNEL = "api.channel.create"
	val READ_CHANNEL = "api.channel.read"
	val UPDATE_CHANNEL = "api.channel.update"
	val LIST_CHANNEL = "api.channel.list"
	val RETIRE_CHANNEL = "api.channel.retire"

	//Category APIs
	val CREATE_CATEGORY = "api.category.create"
	val READ_CATEGORY = "api.category.read"
	val UPDATE_CATEGORY = "api.category.update"
	val RETIRE_CATEGORY = "api.category.retire"

	//Asset V4 apis
	val CREATE_ASSET = "api.asset.create"
	val READ_ASSET = "api.asset.read"
	val UPDATE_ASSET = "api.asset.update"
	val UPLOAD_ASSET = "api.asset.upload"
	val UPLOAD_PRE_SIGNED_ASSET= "api.asset.upload.url"
	val COPY_ASSET = "api.asset.copy"



	//Collection V4 apis
	val CREATE_COLLECTION = "api.collection.create"
	val READ_COLLECTION = "api.collection.read"
	val UPDATE_COLLECTION = "api.collection.update"
	val RETIRE_COLLECTION = "api.collection.retire"
	val COPY_COLLECTION = "api.collection.copy"
	val DISCARD_COLLECTION = "api.collection.discard"
	val FlAG_COLLECTION = "api.collection.flag"
	val ACCEPT_FLAG_COLLECTION = "api.collection.flag.accept"
	val ADD_HIERARCHY_V4 = "api.collection.hierarchy.add"
	val REMOVE_HIERARCHY_V4 = "api.collection.hierarchy.remove"
	val UPDATE_HIERARCHY_V4 = "api.collection.hierarchy.update"
	val GET_HIERARCHY_V4 = "api.collection.hierarchy.get"
	val SYSTEM_UPDATE_COLLECTION = "api.collection.system.update"

  //App APIs
  val REGISTER_APP = "api.app.register"
  val READ_APP = "api.app.read"
  val UPDATE_APP = "api.app.update"
  val APPROVE_APP = "api.app.approve"
  val REJECT_APP = "api.app.reject"
  val RETIRE_APP = "api.app.retire"

  val CREATE_EVENT = "api.event.create"
  val UPDATE_EVENT = "api.event.update"

  val CREATE_EVENT_SET = "api.eventset.create"
  val UPDATE_EVENT_SET = "api.eventset.update"
  val PUBLISH_EVENT_SET = "api.eventset.publish"
  val PUBLISH_EVENT = "api.event.publish"

	//Object APIs
	val READ_OBJECT = "api.object.read"

}
