package org.sunbird.mimetype.ecml.processor

import org.sunbird.cloudstore.StorageService
import org.sunbird.common.Platform
import org.sunbird.common.exception.ClientException
import org.sunbird.telemetry.logger.TelemetryManager
import org.sunbird.url.mgr.impl.URLFactoryManager

import scala.collection.JavaConverters._


trait AssetsLicenseValidatorProcessor extends IProcessor {

	val validMediaTypes = Platform.getStringList("asset.media_service_providers", java.util.Arrays.asList("youtube"))

	abstract override def process(ecrf: Plugin)(implicit ss: StorageService): Plugin = {
		validateAssetLicense(ecrf)
		super.process(ecrf)
	}

	def validateAssetLicense(ecrf: Plugin) = {
		if (null != ecrf.manifest) {
			val medias: List[Media] = ecrf.manifest.medias
			if (null != medias && !medias.isEmpty) {
				medias.filter(m => validMediaTypes.contains(m.`type`)).map(media => {
					TelemetryManager.info("Validating License for Media Id::" + media.id + " | Media Type:: " + media.`type`)
					println("Validating License for Media Id::" + media.id + " | Media Type:: " + media.`type`)
					validateLicense(media.id, media.`type`, media.src)
				})
			}
		}
	}

	def validateLicense(id: String, mediaType: String, src: String) = {
		val urlMgr = URLFactoryManager.getUrlManager(mediaType.toLowerCase)
		val data: java.util.Map[String, AnyRef] = urlMgr.validateURL(src, "license")
		if (!data.getOrDefault("valid", false.asInstanceOf[AnyRef]).asInstanceOf[Boolean])
			throw new ClientException("INVALID_MEDIA_LICENSE", s"Please Provide Asset With Valid License For : ${id} | Url : ${src}")
	}


}
