package org.sunbird.mimetype.ecml.processor

import java.io.{File, IOException}
import java.net.URL

import org.apache.commons.io.{FileUtils, FilenameUtils}
import org.apache.commons.lang3.StringUtils
import org.sunbird.cloudstore.StorageService
import org.sunbird.common.Platform
import org.sunbird.common.exception.ClientException

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.Duration

trait LocalizeAssetProcessor extends IProcessor {

	val PLUGIN_MEDIA_BASE_URL: String = Platform.getString("plugin.media.base.url", "https://dev.sunbirded.org")
	val CONTENT_MEDIA_BASE_URL: String = Platform.config.getString("content.media.base.url")
	val timeout: Long = if(Platform.config.hasPath("asset.max_download_time")) Platform.config.getLong("asset.max_download_time") else 60

	abstract override def process(ecrf: Plugin)(implicit ss: StorageService): Plugin = {
		val manifest = ecrf.manifest
		val updatedMedias:List[Media] = localizeAssets(manifest.medias)
		val updatedManifest:Manifest = Manifest(manifest.id, manifest.data, manifest.innerText, manifest.cData, updatedMedias)
		super.process(Plugin(ecrf.id, ecrf.data, ecrf.innerText, ecrf.cData, ecrf.childrenPlugin, updatedManifest, ecrf.controllers, ecrf.events))
	}

	def localizeAssets(medias: List[Media])(implicit ss: StorageService, ec: ExecutionContext =  concurrent.ExecutionContext.Implicits.global): List[Media] = {
		if(null != medias) {
			val future:Future[List[Media]] = Future.sequence(medias.filter(media=> StringUtils.isNotBlank(media.id) && StringUtils.isNotBlank(media.src) && StringUtils.isNotBlank(media.`type`) && !StringUtils.equalsIgnoreCase("youtube", media.`type`))
			  .map(media => {
				  Future {
					  val dPath: String = if (widgetTypeAssets.contains(media.`type`)) getBasePath() + File.separator + "widgets" else getBasePath() + File.separator + "assets"
					  val sFolder = getSubFolder(media)
					  val downloadPath = if(StringUtils.isNotBlank(sFolder)) dPath + File.separator + sFolder else dPath
					  val file: File = downloadFile(downloadPath, getUrlWithPrefix(media.src))
					  if(null != media.data && !media.data.isEmpty && null != file && file.exists()) {
						  val mSrc = if(StringUtils.isNotBlank(sFolder)) sFolder + File.separator + file.getName else file.getName
						  val mData = media.data ++ Map("src" -> mSrc)
						  Media(media.id, mData, media.innerText, media.cData, mSrc, media.`type`, media.childrenPlugin)
					  } else media
				  }
			  }))
			val mediaList:List[Media] = Await.result(future, Duration.apply(timeout, "second"))
			if(null != mediaList && !mediaList.isEmpty) mediaList ++ medias.filter(m => StringUtils.equalsIgnoreCase("youtube", m.`type`)) else medias
		} else medias
	}

	def getSubFolder(media: Media): String = {
		if (!media.src.startsWith("http")) {
			val f = new File(media.src)
			val subFolder: String = StringUtils.stripStart(f.getParent, File.separator)
			if (f.exists) f.delete
			subFolder
		} else ""
	}

	def getUrlWithPrefix(src: String) = {
		if (StringUtils.isNotBlank(src) && !src.startsWith("http")) {
			if (src.contains("content-plugins/")) PLUGIN_MEDIA_BASE_URL + File.separator + src else CONTENT_MEDIA_BASE_URL + src
		} else src
	}

	def downloadFile(downloadPath: String, fileUrl: String): File = try {
		createDirectory(downloadPath)
		val file = new File(downloadPath + File.separator + getFileNameFromURL(fileUrl))
		FileUtils.copyURLToFile(new URL(fileUrl), file)
		file
	} catch {
		case e: IOException =>
			throw new ClientException("ERR_INVALID_FILE_URL", "Please Provide Valid File Url!")
	}

	def createDirectory(directoryName: String): Unit = {
		val theDir = new File(directoryName)
		if (!theDir.exists) theDir.mkdirs
	}

	def getFileNameFromURL(fileUrl: String): String = {
		var fileName = FilenameUtils.getBaseName(fileUrl) + "_" + System.currentTimeMillis
		if (!FilenameUtils.getExtension(fileUrl).isEmpty) fileName += "." + FilenameUtils.getExtension(fileUrl)
		fileName
	}
}
