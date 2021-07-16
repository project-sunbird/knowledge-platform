package org.sunbird.mimetype.ecml

import org.sunbird.mimetype.ecml.processor.{AssetsLicenseValidatorProcessor, AssetsValidatorProcessor, BaseProcessor, LocalizeAssetProcessor, MissingAssetValidatorProcessor, MissingControllerValidatorProcessor}

class ECMLProcessor(basePath: String, identifier: String) extends BaseProcessor(basePath, identifier) with AssetsLicenseValidatorProcessor with MissingControllerValidatorProcessor with AssetsValidatorProcessor with MissingAssetValidatorProcessor with LocalizeAssetProcessor {

}
