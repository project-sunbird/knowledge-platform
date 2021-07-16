package org.sunbird.mimetype.ecml.processor

import java.io.File

import org.sunbird.cloudstore.StorageService
import org.sunbird.common.exception.ClientException

trait MissingAssetValidatorProcessor extends IProcessor {

    abstract override def process(ecrf: Plugin)(implicit ss: StorageService): Plugin = {
        validateMissingAssets(ecrf)
        super.process(ecrf)
    }

    def getMediaId(media: Media): String = {
        if(null != media.data && !media.data.isEmpty){
            val plugin = media.data.get("plugin")
            val ver = media.data.get("version")
            if((null != plugin && plugin.isDefined && plugin.toString.nonEmpty) && (null != ver && ver.isDefined && ver.toString.nonEmpty))
                media.id + "_" + plugin+ "_" + ver
            else media.id
        }else media.id
    }

    def validateMissingAssets(ecrf: Plugin) = {
        if(null != ecrf.manifest){
            val medias:List[Media] = ecrf.manifest.medias
            if(null != medias){
                val mediaIds = medias.map(media => getMediaId(media)).toList
                if(mediaIds.size != mediaIds.distinct.size)
                    throw new ClientException("DUPLICATE_ASSET_ID", "Error! Duplicate Asset Id used in the manifest. Asset Ids are: "
                            + mediaIds.groupBy(identity).mapValues(_.size).filter(p => p._2 > 1).keySet)

                val nonYoutubeMedias = medias.filter(media => !"youtube".equalsIgnoreCase(media.`type`))
                nonYoutubeMedias.map(media => {
                    if(widgetTypeAssets.contains(media.`type`) && !new File(getBasePath() + File.separator + "widgets" + File.separator + media.src).exists())
                        throw new ClientException("MISSING_ASSETS", "Error! Missing Asset.  | [Asset Id '" + media.id)
                    else if(!widgetTypeAssets.contains(media.`type`) && !new File(getBasePath() + File.separator + "assets" + File.separator + media.src).exists())
                        throw new ClientException("MISSING_ASSETS", "Error! Missing Asset.  | [Asset Id '" + media.id)
                })
            }
            
        }
    }
}
