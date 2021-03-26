package org.sunbird.managers

import java.util

import org.apache.commons.lang3.BooleanUtils
import org.parboiled.common.StringUtils
import org.sunbird.common.JsonUtils
import org.sunbird.common.dto.Request
import org.sunbird.common.exception.{ClientException, ResourceNotFoundException}
import org.sunbird.graph.OntologyEngineContext
import org.sunbird.utils.HierarchyConstants

class TestUpdateHierarchy extends BaseSpec {

    private val KEYSPACE_CREATE_SCRIPT = "CREATE KEYSPACE IF NOT EXISTS hierarchy_store WITH replication = {'class': 'SimpleStrategy','replication_factor': '1'};"
    private val TABLE_CREATE_SCRIPT = "CREATE TABLE IF NOT EXISTS hierarchy_store.content_hierarchy (identifier text, hierarchy text,PRIMARY KEY (identifier));"
    private val HIERARCHY_TO_MIGRATE_SCRIPT = "INSERT INTO hierarchy_store.content_hierarchy(identifier, hierarchy) values ('do_11283193441064550414.img', '{\"identifier\":\"do_11283193441064550414\",\"children\":[{\"parent\":\"do_11283193441064550414\",\"identifier\":\"do_11283193463014195215\",\"copyright\":\"Sunbird\",\"lastStatusChangedOn\":\"2019-08-21T14:37:50.281+0000\",\"code\":\"2e837725-d663-45da-8ace-9577ab111982\",\"visibility\":\"Parent\",\"index\":1,\"mimeType\":\"application/vnd.ekstep.content-collection\",\"createdOn\":\"2019-08-21T14:37:50.281+0000\",\"versionKey\":\"1566398270281\",\"framework\":\"tpd\",\"depth\":1,\"children\":[],\"name\":\"U1\",\"lastUpdatedOn\":\"2019-08-21T14:37:50.281+0000\",\"contentType\":\"CourseUnit\",\"primaryCategory\":\"Learning Resource\",\"status\":\"Draft\"}]}');"

    private val CATEGORY_STORE_KEYSPACE = "CREATE KEYSPACE IF NOT EXISTS category_store WITH replication = {'class': 'SimpleStrategy','replication_factor': '1'};"
    private val CATEGORY_DEF_DATA_TABLE = "CREATE TABLE IF NOT EXISTS category_store.category_definition_data (identifier text PRIMARY KEY, forms map<text, text>, objectmetadata map<text, text>);"
    private val CATEGORY_DEF_INPUT = List("INSERT INTO category_store.category_definition_data(identifier) values ('obj-cat:digital-textbook_collection_all')",
      "INSERT INTO category_store.category_definition_data(identifier) values ('obj-cat:textbook-unit_collection_all')",
      "INSERT INTO category_store.category_definition_data(identifier) values ('obj-cat:course-unit_collection_all')",
      "INSERT INTO category_store.category_definition_data(identifier) values ('obj-cat:content-playlist_collection_all')",
      "INSERT INTO category_store.category_definition_data(identifier) values ('obj-cat:asset_asset_all')")

//    private val EXISTING_HIERARCHY_do_112949210157768704111 = "INSERT INTO hierarchy_store.content_hierarchy(identifier, hierarchy) values ('do_112949210157768704111', '{\"identifier\":\"do_112949210157768704111\",\"children\":[{\"ownershipType\":[\"createdBy\"],\"parent\":\"do_112949210157768704111\",\"code\":\"b9a50833-eff6-4ef5-a2a4-2413f2d51f6c\",\"channel\":\"in.ekstep\",\"description\":\"Test_CourseUnit_desc_1\",\"language\":[\"English\"],\"mimeType\":\"application/vnd.ekstep.content-collection\",\"idealScreenSize\":\"normal\",\"createdOn\":\"2020-02-04T10:53:23.491+0530\",\"objectType\":\"Content\",\"children\":[{\"ownershipType\":[\"createdFor\"],\"parent\":\"do_11294986283819827217\",\"previewUrl\":\"https://youtu.be/v7YZhQ86Adw\",\"keywords\":[\"10th\",\"Science\",\"Jnana Prabodhini\",\"Maharashtra Board\",\"#gyanqr\"],\"subject\":[\"Science\"],\"channel\":\"01261732844414566415\",\"downloadUrl\":\"https://ntpproductionall.blob.core.windows.net/ntp-content-production/ecar_files/do_312776559940476928110909/vnsptiinmdhye-laingik-prjnn_1560157123850_do_312776559940476928110909_1.0.ecar\",\"organisation\":[\"Jnana Prabodhini\"],\"language\":[\"English\"],\"mimeType\":\"video/x-youtube\",\"variants\":{\"spine\":{\"ecarUrl\":\"https://ntpproductionall.blob.core.windows.net/ntp-content-production/ecar_files/do_312776559940476928110909/vnsptiinmdhye-laingik-prjnn_1560157123878_do_312776559940476928110909_1.0_spine.ecar\",\"size\":51205.0}},\"objectType\":\"Content\",\"gradeLevel\":[\"Class 10\"],\"appIcon\":\"https://ntpproductionall.blob.core.windows.net/ntp-content-production/content/do_312776559940476928110909/artifact/10th_mar_2_1547715340679.thumb.png\",\"appId\":\"prod.diksha.portal\",\"contentEncoding\":\"identity\",\"artifactUrl\":\"https://youtu.be/v7YZhQ86Adw\",\"lockKey\":\"772b40b3-4de0-44c3-8474-0fe8f8ec2d91\",\"sYS_INTERNAL_LAST_UPDATED_ON\":\"2019-07-31T01:57:11.210+0000\",\"contentType\":\"Resource\",\"lastUpdatedBy\":\"bf4df886-bb42-4f91-9f33-c88da1653535\",\"identifier\":\"do_312776559940476928110909\",\"audience\":[\"Student\"],\"visibility\":\"Default\",\"consumerId\":\"89490534-126f-4f0b-82ac-3ff3e49f3468\",\"index\":1,\"mediaType\":\"content\",\"osId\":\"org.ekstep.quiz.app\",\"lastPublishedBy\":\"7a3358d5-e290-49a4-b7ea-3e3d47a2af30\",\"languageCode\":[\"en\"],\"version\":1,\"pragma\":[\"external\"],\"license\":\"Creative Commons Attribution (CC BY)\",\"prevState\":\"Review\",\"size\":51204.0,\"lastPublishedOn\":\"2019-06-10T08:58:43.846+0000\",\"name\":\"वनस्पतींमध्ये लैंगिक प्रजनन\",\"attributions\":[\"Jnana Prabodhini\"],\"status\":\"Live\",\"code\":\"99a9c6e4-ec56-40ab-9a8c-b66e3a551273\",\"creators\":\"Jnana Prabodhini\",\"description\":\"सजीवांतील जीवनप्रक्रिया भाग - २\",\"streamingUrl\":\"https://youtu.be/v7YZhQ86Adw\",\"medium\":[\"Marathi\"],\"posterImage\":\"https://ntpproductionall.blob.core.windows.net/ntp-content-production/content/do_31267888406854041612953/artifact/10th_mar_2_1547715340679.png\",\"idealScreenSize\":\"normal\",\"createdOn\":\"2018-09-06T06:35:10.427+0000\",\"contentDisposition\":\"online\",\"lastUpdatedOn\":\"6019-06-10T08:35:10.582+0000\",\"dialcodeRequired\":\"No\",\"owner\":\"Jnana Prabodhini\",\"lastStatusChangedOn\":\"2019-06-10T08:58:43.925+0000\",\"createdFor\":[\"01261732844414566415\"],\"creator\":\"Pallavi Paradkar\",\"os\":[\"All\"],\"pkgVersion\":1.0,\"versionKey\":\"1560157123651\",\"idealScreenDensity\":\"hdpi\",\"framework\":\"NCF\",\"depth\":2,\"s3Key\":\"ecar_files/do_312776559940476928110909/vnsptiinmdhye-laingik-prjnn_1560157123850_do_312776559940476928110909_1.0.ecar\",\"me_averageRating\":3.0,\"lastSubmittedOn\":\"2019-06-04T09:02:44.995+0000\",\"createdBy\":\"bf4df886-bb42-4f91-9f33-c88da1653535\",\"compatibilityLevel\":4,\"ownedBy\":\"01261732844414566415\",\"board\":\"State (Maharashtra)\",\"resourceType\":\"Learn\"}],\"contentDisposition\":\"inline\",\"lastUpdatedOn\":\"2020-02-04T10:53:23.490+0530\",\"contentEncoding\":\"gzip\",\"contentType\":\"TextBookUnit\",\"dialcodeRequired\":\"No\",\"identifier\":\"do_11294986283819827217\",\"lastStatusChangedOn\":\"2020-02-04T10:53:23.492+0530\",\"audience\":[\"Student\"],\"os\":[\"All\"],\"visibility\":\"Parent\",\"index\":1,\"mediaType\":\"content\",\"osId\":\"org.ekstep.quiz.app\",\"languageCode\":[\"en\"],\"versionKey\":\"1580793803491\",\"license\":\"CC BY 4.0\",\"idealScreenDensity\":\"hdpi\",\"depth\":1,\"compatibilityLevel\":1,\"name\":\"Test_CourseUnit_1\",\"status\":\"Draft\"}]}');"

    implicit val oec: OntologyEngineContext = new OntologyEngineContext

    override def beforeAll(): Unit = {
        super.beforeAll()
        graphDb.execute("UNWIND [" +
            "{identifier:\"obj-cat:learning-resource_content_0123221617357783046602\",name:\"Learning Resource\",description:\"Learning resource\",categoryId:\"obj-cat:learning-resource\",targetObjectType:\"Content\",status:\"Live\",objectMetadata:\"{\\\"config\\\":{},\\\"schema\\\":{\\\"properties\\\":{\\\"trackable\\\":{\\\"type\\\":\\\"object\\\",\\\"properties\\\":{\\\"enabled\\\":{\\\"type\\\":\\\"string\\\",\\\"enum\\\":[\\\"Yes\\\",\\\"No\\\"],\\\"default\\\":\\\"Yes\\\"},\\\"autoBatch\\\":{\\\"type\\\":\\\"string\\\",\\\"enum\\\":[\\\"Yes\\\",\\\"No\\\"],\\\"default\\\":\\\"Yes\\\"}},\\\"additionalProperties\\\":false}}}}\",IL_SYS_NODE_TYPE:\"DATA_NODE\",IL_FUNC_OBJECT_TYPE:\"ObjectCategoryDefinition\",IL_UNIQUE_ID:\"obj-cat:learning-resource_content_0123221617357783046602\"}," +
            "{identifier:\"obj-cat:learning-resource_content_all\",name:\"Learning Resource\",description:\"Learning resource\",categoryId:\"obj-cat:learning-resource\",targetObjectType:\"Content\",status:\"Live\",objectMetadata:\"{\\\"config\\\":{},\\\"schema\\\":{\\\"properties\\\":{\\\"trackable\\\":{\\\"type\\\":\\\"object\\\",\\\"properties\\\":{\\\"enabled\\\":{\\\"type\\\":\\\"string\\\",\\\"enum\\\":[\\\"Yes\\\",\\\"No\\\"],\\\"default\\\":\\\"Yes\\\"},\\\"autoBatch\\\":{\\\"type\\\":\\\"string\\\",\\\"enum\\\":[\\\"Yes\\\",\\\"No\\\"],\\\"default\\\":\\\"Yes\\\"}},\\\"additionalProperties\\\":false}}}}\",IL_SYS_NODE_TYPE:\"DATA_NODE\",IL_FUNC_OBJECT_TYPE:\"ObjectCategoryDefinition\",IL_UNIQUE_ID:\"obj-cat:learning-resource_content_all\"}," +
            "{ownershipType:[\"createdBy\"],code:\"citrusCode\",channel:\"in.ekstep\",language:[\"English\"],mimeType:\"application/vnd.ekstep.content-collection\",idealScreenSize:\"normal\",createdOn:\"2020-01-29T17:45:55.620+0530\",contentDisposition:\"inline\",contentEncoding:\"gzip\",lastUpdatedOn:\"2020-01-29T17:46:35.471+0530\",contentType:\"TextBook\",primaryCategory:\"Digital Textbook\",dialcodeRequired:\"No\",identifier:\"do_11294581887465881611\",audience:[\"Student\"],lastStatusChangedOn:\"2020-01-29T17:45:55.620+0530\",os:[\"All\"],visibility:\"Default\",IL_SYS_NODE_TYPE:\"DATA_NODE\",nodeType:\"DATA_NODE\",mediaType:\"content\",osId:\"org.ekstep.quiz.app\",version:2,versionKey:\"1580300195471\",idealScreenDensity:\"hdpi\",license:\"CC BY 4.0\",framework:\"NCF\",compatibilityLevel:1,IL_FUNC_OBJECT_TYPE:\"Collection\",name:\"Update Hierarchy Test_03\",IL_UNIQUE_ID:\"do_11294581887465881611\",status:\"Draft\"}," +
            "{ownershipType:[\"createdBy\"],code:\"citrusCode\",channel:\"in.ekstep\",description:\"New text book description_01\",language:[\"English\"],mimeType:\"application/vnd.ekstep.content-collection\",idealScreenSize:\"normal\",createdOn:\"2020-02-03T12:45:30.605+0530\",contentDisposition:\"inline\",contentEncoding:\"gzip\",lastUpdatedOn:\"2020-02-03T12:46:01.439+0530\",contentType:\"TextBook\",primaryCategory:\"Digital Textbook\",dialcodeRequired:\"No\",identifier:\"do_112949210157768704111\",audience:[\"Student\"],lastStatusChangedOn:\"2020-02-03T12:45:30.605+0530\",os:[\"All\"],visibility:\"Default\",IL_SYS_NODE_TYPE:\"DATA_NODE\",nodeType:\"DATA_NODE\",childNodes:[\"do_112949210410000384114\"],mediaType:\"content\",osId:\"org.ekstep.quiz.app\",version:2,versionKey:\"1580714161439\",license:\"CC BY 4.0\",idealScreenDensity:\"hdpi\",depth:0,framework:\"NCF\",compatibilityLevel:1,IL_FUNC_OBJECT_TYPE:\"Collection\",name:\"Update Hierarchy Test_01\",IL_UNIQUE_ID:\"do_112949210157768704111\",status:\"Draft\"}," +
            "{ownershipType:[\"createdBy\"],previewUrl:\"https://www.youtube.com/watch?v=JEjUtGkUqus\",downloadUrl:\"https://ntpproductionall.blob.core.windows.net/ntp-content-production/ecar_files/do_31250856200414822416938/mh_chapter-1_science-part-2_grade-10_2_1539192600492_do_31250856200414822416938_2.0.ecar\",channel:\"0123221617357783046602\",showNotification:true,language:[\"English\"],variants:\"{\\\"spine\\\":{\\\"ecarUrl\\\":\\\"https://ntpproductionall.blob.core.windows.net/ntp-content-production/ecar_files/do_31250856200414822416938/mh_chapter-1_science-part-2_grade-10_2_1539192600579_do_31250856200414822416938_2.0_spine.ecar\\\",\\\"size\\\":29518}}\",mimeType:\"video/x-youtube\",appIcon:\"https://ntpproductionall.blob.core.windows.net/ntp-content-production/content/do_31250856200414822416938/artifact/logo_4221_1513150964_1513150964262.thumb.jpg\",appId:\"prod.diksha.app\",artifactUrl:\"https://www.youtube.com/watch?v=JEjUtGkUqus\",contentEncoding:\"identity\",contentType:\"Resource\",primaryCategory:\"Learning Resource\",lastUpdatedBy:\"ekstep\",audience:[\"Student\"],visibility:\"Default\",consumerId:\"89490534-126f-4f0b-82ac-3ff3e49f3468\",mediaType:\"content\",osId:\"org.ekstep.quiz.app\",lastPublishedBy:\"ekstep\",version:1,pragma:[\"external\"],license:\"CC BY-SA 4.0\",prevState:\"Live\",size:29519,lastPublishedOn:\"2018-10-10T17:30:00.491+0000\",IL_FUNC_OBJECT_TYPE:\"Content\",name:\"MH_Chapter 1_Science Part 2_Grade 10_2\",attributions:[\"MSCERT\"],status:\"Live\",code:\"b2099ea5-0070-4930-ae13-b08aa83e5853\",description:\"अानुवांशिकता और उत्क्रांती\",streamingUrl:\"https://www.youtube.com/watch?v=JEjUtGkUqus\",posterImage:\"https://ntpproductionall.blob.core.windows.net/ntp-content-production/content/do_31239573269507276815296/artifact/logo_4221_1513150964_1513150964262.jpg\",idealScreenSize:\"normal\",createdOn:\"0021-02-04T08:05:49.480+0000\",contentDisposition:\"online\",lastUpdatedOn:\"25182518-10-10T17:29:59.456+0000\",SYS_INTERNAL_LAST_UPDATED_ON:\"2019-07-31T01:57:26.148+0000\",dialcodeRequired:\"No\",lastStatusChangedOn:\"2019-06-17T05:41:05.507+0000\",createdFor:[\"0123221617357783046602\"],creator:\"Alaka Potdar\",os:[\"All\"],IL_SYS_NODE_TYPE:\"DATA_NODE\",nodeType:\"DATA_NODE\",pkgVersion:2,versionKey:\"1539192599456\",idealScreenDensity:\"hdpi\",framework:\"NCF\",s3Key:\"ecar_files/do_31250856200414822416938/mh_chapter-1_science-part-2_grade-10_2_1539192600492_do_31250856200414822416938_2.0.ecar\",me_averageRating:3,lastSubmittedOn:\"2018-05-21T17:37:16.466+0000\",createdBy:\"c5d09e49-6f1d-474b-b6cc-2e590ae15ef8\",compatibilityLevel:4,IL_UNIQUE_ID:\"do_31250856200414822416938\",resourceType:\"Learn\"}," +
            "{copyright:\"\",code:\"org.ekstep.asset.Pant.1773380908\",sources:\"\",channel:\"in.ekstep\",downloadUrl:\"https://ekstep-public-prod.s3-ap-south-1.amazonaws.com/content/do_30109819/artifact/clothes-1294974_960_720_658_1483340550_1483340551056.png\",language:[\"English\"],mimeType:\"image/png\",variants:\"{\\\"high\\\":\\\"https://ekstep-public-prod.s3-ap-south-1.amazonaws.com/content/do_30109819/artifact/clothes-1294974_960_720_658_1483340550_1483340551056.png\\\",\\\"low\\\":\\\"https://ekstep-public-prod.s3-ap-south-1.amazonaws.com/content/do_30109819/artifact/clothes-1294974_960_720_658_1483340550_1483340551056.low.png\\\",\\\"medium\\\":\\\"https://ekstep-public-prod.s3-ap-south-1.amazonaws.com/content/do_30109819/artifact/clothes-1294974_960_720_658_1483340550_1483340551056.medium.png\\\"}\",idealScreenSize:\"normal\",createdOn:\"2017-01-02T07:02:31.021+0000\",contentDisposition:\"inline\",artifactUrl:\"https://ekstep-public-prod.s3-ap-south-1.amazonaws.com/content/do_30109819/artifact/clothes-1294974_960_720_658_1483340550_1483340551056.png\",contentEncoding:\"identity\",lastUpdatedOn:\"2017-03-10T20:48:09.283+0000\",contentType:\"Asset\",primaryCategory:\"Asset\",owner:\"\",os:[\"All\"],visibility:\"Default\",IL_SYS_NODE_TYPE:\"DATA_NODE\",nodeType:\"DATA_NODE\",portalOwner:\"658\",mediaType:\"image\",osId:\"org.ekstep.quiz.app\",ageGroup:[\"5-6\"],versionKey:\"1489178889283\",license:\"CC BY-SA 4.0\",idealScreenDensity:\"hdpi\",framework:\"NCF\",s3Key:\"content/do_30109819/artifact/clothes-1294974_960_720_658_1483340550_1483340551056.png\",size:167939,createdBy:\"658\",compatibilityLevel:1,IL_FUNC_OBJECT_TYPE:\"Asset\",name:\"Pant\",publisher:\"\",IL_UNIQUE_ID:\"do_30109819\",status:\"Live\"}," +
            "{ownershipType:[\"createdBy\"],code:\"citrusCode\",channel:\"in.ekstep\",language:[\"English\"],mimeType:\"application/vnd.ekstep.content-collection\",idealScreenSize:\"normal\",createdOn:\"2020-01-29T17:45:55.620+0530\",contentDisposition:\"inline\",contentEncoding:\"gzip\",lastUpdatedOn:\"2020-01-29T17:46:35.471+0530\",contentType:\"TextBook\",primaryCategory:\"Digital Textbook\",dialcodeRequired:\"No\",identifier:\"do_test_book_1\",audience:[\"Student\"],lastStatusChangedOn:\"2020-01-29T17:45:55.620+0530\",os:[\"All\"],visibility:\"Default\",IL_SYS_NODE_TYPE:\"DATA_NODE\",nodeType:\"DATA_NODE\",mediaType:\"content\",osId:\"org.ekstep.quiz.app\",version:2,versionKey:\"1580300195471\",idealScreenDensity:\"hdpi\",license:\"CC BY 4.0\",framework:\"NCF\",compatibilityLevel:1,IL_FUNC_OBJECT_TYPE:\"Collection\",name:\"Update Hierarchy Test For Origin Data\",IL_UNIQUE_ID:\"do_test_book_1\",status:\"Draft\"}," +
            "{identifier:\"obj-cat:learning-resource_content_all\",name:\"Learning Resource\",description:\"Learning resource\",categoryId:\"obj-cat:learning-resource\",targetObjectType:\"Content\",status:\"Live\",objectMetadata:\"{\\\"config\\\":{},\\\"schema\\\":{\\\"properties\\\":{\\\"trackable\\\":{\\\"type\\\":\\\"object\\\",\\\"properties\\\":{\\\"enabled\\\":{\\\"type\\\":\\\"string\\\",\\\"enum\\\":[\\\"Yes\\\",\\\"No\\\"],\\\"default\\\":\\\"Yes\\\"},\\\"autoBatch\\\":{\\\"type\\\":\\\"string\\\",\\\"enum\\\":[\\\"Yes\\\",\\\"No\\\"],\\\"default\\\":\\\"Yes\\\"}},\\\"additionalProperties\\\":false}}}}\",IL_SYS_NODE_TYPE:\"DATA_NODE\",IL_FUNC_OBJECT_TYPE:\"ObjectCategoryDefinition\",IL_UNIQUE_ID:\"obj-cat:learning-resource_content_all\"}," +
            "{identifier:\"obj-cat:learning-resource_content_b00bc992ef25f1a9a8d63291e20efc8d\",name:\"Learning Resource\",description:\"Learning resource\",categoryId:\"obj-cat:learning-resource\",targetObjectType:\"Content\",status:\"Live\",objectMetadata:\"{\\\"config\\\":{},\\\"schema\\\":{\\\"properties\\\":{\\\"trackable\\\":{\\\"type\\\":\\\"object\\\",\\\"properties\\\":{\\\"enabled\\\":{\\\"type\\\":\\\"string\\\",\\\"enum\\\":[\\\"Yes\\\",\\\"No\\\"],\\\"default\\\":\\\"Yes\\\"},\\\"autoBatch\\\":{\\\"type\\\":\\\"string\\\",\\\"enum\\\":[\\\"Yes\\\",\\\"No\\\"],\\\"default\\\":\\\"Yes\\\"}},\\\"additionalProperties\\\":false}}}}\",IL_SYS_NODE_TYPE:\"DATA_NODE\",IL_FUNC_OBJECT_TYPE:\"ObjectCategoryDefinition\",IL_UNIQUE_ID:\"obj-cat:learning-resource_content_b00bc992ef25f1a9a8d63291e20efc8d\"}," +
            "{identifier:\"obj-cat:digital-textbook_collection_all\",name:\"Digital Textbook\",description:\"Learning resource\",categoryId:\"obj-cat:digital-textbook\",targetObjectType:\"Content\",status:\"Live\",objectMetadata:\"{\\\"config\\\":{},\\\"schema\\\":{\\\"properties\\\":{\\\"trackable\\\":{\\\"type\\\":\\\"object\\\",\\\"properties\\\":{\\\"enabled\\\":{\\\"type\\\":\\\"string\\\",\\\"enum\\\":[\\\"Yes\\\",\\\"No\\\"],\\\"default\\\":\\\"Yes\\\"},\\\"autoBatch\\\":{\\\"type\\\":\\\"string\\\",\\\"enum\\\":[\\\"Yes\\\",\\\"No\\\"],\\\"default\\\":\\\"Yes\\\"}},\\\"additionalProperties\\\":false}}}}\",IL_SYS_NODE_TYPE:\"DATA_NODE\",IL_FUNC_OBJECT_TYPE:\"ObjectCategoryDefinition\",IL_UNIQUE_ID:\"obj-cat:digital-textbook_collection_all\"}," +
            "{identifier:\"obj-cat:textbook-unit_collection_all\",name:\"Learning Resource\",description:\"Learning resource\",categoryId:\"obj-cat:textbook-unit\",targetObjectType:\"Content\",status:\"Live\",objectMetadata:\"{\\\"config\\\":{},\\\"schema\\\":{\\\"properties\\\":{\\\"trackable\\\":{\\\"type\\\":\\\"object\\\",\\\"properties\\\":{\\\"enabled\\\":{\\\"type\\\":\\\"string\\\",\\\"enum\\\":[\\\"Yes\\\",\\\"No\\\"],\\\"default\\\":\\\"Yes\\\"},\\\"autoBatch\\\":{\\\"type\\\":\\\"string\\\",\\\"enum\\\":[\\\"Yes\\\",\\\"No\\\"],\\\"default\\\":\\\"Yes\\\"}},\\\"additionalProperties\\\":false}}}}\",IL_SYS_NODE_TYPE:\"DATA_NODE\",IL_FUNC_OBJECT_TYPE:\"ObjectCategoryDefinition\",IL_UNIQUE_ID:\"obj-cat:textbook-unit_collection_all\"}," +
            "{identifier:\"obj-cat:course_collection_all\",name:\"Learning Resource\",description:\"Learning resource\",categoryId:\"obj-cat:course\",targetObjectType:\"Content\",status:\"Live\",objectMetadata:\"{\\\"config\\\":{},\\\"schema\\\":{\\\"properties\\\":{\\\"trackable\\\":{\\\"type\\\":\\\"object\\\",\\\"properties\\\":{\\\"enabled\\\":{\\\"type\\\":\\\"string\\\",\\\"enum\\\":[\\\"Yes\\\",\\\"No\\\"],\\\"default\\\":\\\"Yes\\\"},\\\"autoBatch\\\":{\\\"type\\\":\\\"string\\\",\\\"enum\\\":[\\\"Yes\\\",\\\"No\\\"],\\\"default\\\":\\\"Yes\\\"}},\\\"additionalProperties\\\":false}}}}\",IL_SYS_NODE_TYPE:\"DATA_NODE\",IL_FUNC_OBJECT_TYPE:\"ObjectCategoryDefinition\",IL_UNIQUE_ID:\"obj-cat:course_collection_all\"}," +
            "{identifier:\"obj-cat:course-unit_collection_all\",name:\"Learning Resource\",description:\"Learning resource\",categoryId:\"obj-cat:course_unit\",targetObjectType:\"Content\",status:\"Live\",objectMetadata:\"{\\\"config\\\":{},\\\"schema\\\":{\\\"properties\\\":{\\\"trackable\\\":{\\\"type\\\":\\\"object\\\",\\\"properties\\\":{\\\"enabled\\\":{\\\"type\\\":\\\"string\\\",\\\"enum\\\":[\\\"Yes\\\",\\\"No\\\"],\\\"default\\\":\\\"Yes\\\"},\\\"autoBatch\\\":{\\\"type\\\":\\\"string\\\",\\\"enum\\\":[\\\"Yes\\\",\\\"No\\\"],\\\"default\\\":\\\"Yes\\\"}},\\\"additionalProperties\\\":false}}}}\",IL_SYS_NODE_TYPE:\"DATA_NODE\",IL_FUNC_OBJECT_TYPE:\"ObjectCategoryDefinition\",IL_UNIQUE_ID:\"obj-cat:course-unit_collection_all\"}," +
            "{identifier:\"obj-cat:asset_asset_all\",name:\"Learning Resource\",description:\"Learning resource\",categoryId:\"obj-cat:asset\",targetObjectType:\"Content\",status:\"Live\",objectMetadata:\"{\\\"config\\\":{},\\\"schema\\\":{\\\"properties\\\":{\\\"trackable\\\":{\\\"type\\\":\\\"object\\\",\\\"properties\\\":{\\\"enabled\\\":{\\\"type\\\":\\\"string\\\",\\\"enum\\\":[\\\"Yes\\\",\\\"No\\\"],\\\"default\\\":\\\"Yes\\\"},\\\"autoBatch\\\":{\\\"type\\\":\\\"string\\\",\\\"enum\\\":[\\\"Yes\\\",\\\"No\\\"],\\\"default\\\":\\\"Yes\\\"}},\\\"additionalProperties\\\":false}}}}\",IL_SYS_NODE_TYPE:\"DATA_NODE\",IL_FUNC_OBJECT_TYPE:\"ObjectCategoryDefinition\",IL_UNIQUE_ID:\"obj-cat:asset_asset_all\"}," +
            "{identifier:\"obj-cat:content-playlist_collection_all\",name:\"Learning Resource\",description:\"Learning resource\",categoryId:\"obj-cat:content-playlist\",targetObjectType:\"Content\",status:\"Live\",objectMetadata:\"{\\\"config\\\":{},\\\"schema\\\":{\\\"properties\\\":{\\\"trackable\\\":{\\\"type\\\":\\\"object\\\",\\\"properties\\\":{\\\"enabled\\\":{\\\"type\\\":\\\"string\\\",\\\"enum\\\":[\\\"Yes\\\",\\\"No\\\"],\\\"default\\\":\\\"Yes\\\"},\\\"autoBatch\\\":{\\\"type\\\":\\\"string\\\",\\\"enum\\\":[\\\"Yes\\\",\\\"No\\\"],\\\"default\\\":\\\"Yes\\\"}},\\\"additionalProperties\\\":false}}}}\",IL_SYS_NODE_TYPE:\"DATA_NODE\",IL_FUNC_OBJECT_TYPE:\"ObjectCategoryDefinition\",IL_UNIQUE_ID:\"obj-cat:content-playlist_collection_all\"}" +
            "] as row CREATE (n:domain) SET n += row")
        executeCassandraQuery(KEYSPACE_CREATE_SCRIPT, TABLE_CREATE_SCRIPT, CATEGORY_STORE_KEYSPACE, CATEGORY_DEF_DATA_TABLE)
      executeCassandraQuery(CATEGORY_DEF_INPUT:_*)
        insert20NodesAndOneCourse()
    }

    override def beforeEach(): Unit = {
        executeCassandraQuery( HIERARCHY_TO_MIGRATE_SCRIPT)
    }

    def getContext(): java.util.Map[String, AnyRef] = {
        new util.HashMap[String, AnyRef](){{
            put(HierarchyConstants.GRAPH_ID, HierarchyConstants.TAXONOMY_ID)
            put(HierarchyConstants.VERSION , HierarchyConstants.SCHEMA_VERSION)
            put(HierarchyConstants.OBJECT_TYPE , HierarchyConstants.COLLECTION_OBJECT_TYPE)
            put(HierarchyConstants.SCHEMA_NAME, "content")
            put(HierarchyConstants.CHANNEL, "b00bc992ef25f1a9a8d63291e20efc8d")
        }}
    }

    "deleteHierarchy" should "Delete the hierarchy data from cassandra from identifier with .img extension" in {
        val request = new Request()
        val context = getContext()
        context.put(HierarchyConstants.SCHEMA_NAME, "collection")
        context.put(HierarchyConstants.ROOT_ID, "do_11283193441064550414")
        request.setContext(context)
        val oldHierarchy = readFromCassandra("Select hierarchy from hierarchy_store.content_hierarchy where identifier='do_11283193441064550414.img'")
            .one().getString("hierarchy")
        assert(StringUtils.isNotEmpty(oldHierarchy))
        UpdateHierarchyManager.deleteHierarchy(request).map(response => {
            assert(response.getResponseCode.code() == 200)
            assert(BooleanUtils.isFalse(readFromCassandra("Select hierarchy from hierarchy_store.content_hierarchy where identifier='do_11283193441064550414.img'").iterator().hasNext))
        })
    }

    "deleteHierarchy with image id" should "Delete the hierarchy data from cassandra from identifier with .img extension" in {
        val request = new Request()
        val context = getContext()
        context.put(HierarchyConstants.SCHEMA_NAME, "collection")
        context.put(HierarchyConstants.ROOT_ID, "do_11283193441064550414.img")
        request.setContext(context)
        val oldHierarchy = readFromCassandra("Select hierarchy from hierarchy_store.content_hierarchy where identifier='do_11283193441064550414.img'")
            .one().getString("hierarchy")
        assert(StringUtils.isNotEmpty(oldHierarchy))
        UpdateHierarchyManager.deleteHierarchy(request).map(response => {
            assert(response.getResponseCode.code() == 200)
            assert(BooleanUtils.isFalse(readFromCassandra("Select hierarchy from hierarchy_store.content_hierarchy where identifier='do_11283193441064550414.img'").iterator().hasNext))
        })
    }

    "deleteHierarchy with invalid id" should "Delete the hierarchy data from cassandra from identifier with .img extension" in {
        val request = new Request()
        val context = getContext()
        context.put(HierarchyConstants.SCHEMA_NAME, "collection")
        context.put(HierarchyConstants.ROOT_ID, "123")
        request.setContext(context)
        UpdateHierarchyManager.deleteHierarchy(request).map(response => {
            assert(response.getResponseCode.code() == 200)
            val hierarchy = readFromCassandra("Select hierarchy from hierarchy_store.content_hierarchy where identifier='do_11283193441064550414.img'")
                .one().getString("hierarchy")
            assert(StringUtils.isNotEmpty(hierarchy))
        })
    }

//        ResourceId = "do_31250856200414822416938" and TextBook id ="do_112945818874658816"
    "updateHierarchy with One New Unit and One Live Resource" should "update text book node, create unit and store the hierarchy in cassandra" in {
        val request = new Request()
        val context = getContext()
        context.put(HierarchyConstants.SCHEMA_NAME, "collection")
        request.setContext(context)
        request.setObjectType(HierarchyConstants.COLLECTION_OBJECT_TYPE)
        request.put(HierarchyConstants.NODES_MODIFIED, getNodesModified_1())
        request.put(HierarchyConstants.HIERARCHY, getHierarchy_1())
        UpdateHierarchyManager.updateHierarchy(request).map(response => {
            assert(response.getResponseCode.code() == 200)
            val hierarchy = readFromCassandra("Select hierarchy from hierarchy_store.content_hierarchy where identifier='do_11294581887465881611'")
                .one().getString("hierarchy")
            assert(StringUtils.isNotEmpty(hierarchy))
        })
    }

    "updateHierarchy on already existing hierarchy" should "recompose the hierarchy structure and store in in cassandra and also update neo4j" in {
        UpdateHierarchyManager.getContentNode("do_31250856200414822416938", HierarchyConstants.TAXONOMY_ID).map(node => {
            println("Node data from neo4j ----- id: " + node.getIdentifier + "node type:  " + node.getNodeType + " node metadata : " + node.getMetadata)
            val request = new Request()
            val context = getContext()
            context.put(HierarchyConstants.SCHEMA_NAME, "collection")
            request.setContext(context)
            request.put(HierarchyConstants.NODES_MODIFIED, getNodesModified_1())
            request.put(HierarchyConstants.HIERARCHY, getHierarchy_1())
            UpdateHierarchyManager.updateHierarchy(request).map(response => {
                assert(response.getResponseCode.code() == 200)
                val identifiers =  response.get(HierarchyConstants.IDENTIFIERS).asInstanceOf[util.Map[String, AnyRef]]
                val hierarchy = readFromCassandra("Select hierarchy from hierarchy_store.content_hierarchy where identifier='do_11294581887465881611'")
                    .one().getString(HierarchyConstants.HIERARCHY)
                assert(StringUtils.isNotEmpty(hierarchy))
                request.put(HierarchyConstants.NODES_MODIFIED, getNodesModified_2("do_11294581887465881611", identifiers.get("b9a50833-eff6-4ef5-a2a4-2413f2d51f6c").asInstanceOf[String]))
                request.put(HierarchyConstants.HIERARCHY, getHierarchy_2("do_11294581887465881611", identifiers.get("b9a50833-eff6-4ef5-a2a4-2413f2d51f6c").asInstanceOf[String]))
                UpdateHierarchyManager.updateHierarchy(request).map(resp => {
                    assert(response.getResponseCode.code() == 200)
                    val hierarchy_updated = readFromCassandra("Select hierarchy from hierarchy_store.content_hierarchy where identifier='do_11294581887465881611'")
                        .one().getString(HierarchyConstants.HIERARCHY)
                    assert(!StringUtils.equalsIgnoreCase(hierarchy, hierarchy_updated))
                })
            }).flatMap(f => f)
        }).flatMap(f => f)
    }

    "updateHierarchy with New Unit and Invalid Resource" should "throw resource not found exception" in {
        val request = new Request()
        val context = getContext()
        context.put(HierarchyConstants.SCHEMA_NAME, "collection")
        request.setContext(context)
        request.put(HierarchyConstants.NODES_MODIFIED, getNodesModified_1())
        request.put(HierarchyConstants.HIERARCHY, getHierarchy_Content_Resource_Invalid_ID())
        recoverToSucceededIf[ResourceNotFoundException](UpdateHierarchyManager.updateHierarchy(request))
    }

    "updateHierarchy with empty nodes modified and hierarchy" should "throw client exception" in {
        val request = new Request()
        val context = getContext()
        context.put(HierarchyConstants.SCHEMA_NAME, "collection")
        request.setContext(context)
        request.put(HierarchyConstants.NODES_MODIFIED, new util.HashMap())
        request.put(HierarchyConstants.HIERARCHY, new util.HashMap())
        val exception = intercept[ClientException] {
            UpdateHierarchyManager.updateHierarchy(request)
        }
        exception.getMessage shouldEqual "Please Provide Valid Root Node Identifier"
    }

    "updateHierarchy on already existing hierarchy empty hierarcy request" should "recompose the hierarchy structure with existing hierarchy" in {
        UpdateHierarchyManager.getContentNode("do_31250856200414822416938", HierarchyConstants.TAXONOMY_ID).map(node => {
            println("Node data from neo4j ----- id: " + node.getIdentifier + "node type:  " + node.getNodeType + " node metadata : " + node.getMetadata)
            val request = new Request()
            val context = getContext()
            context.put(HierarchyConstants.SCHEMA_NAME, "collection")
            request.setContext(context)
            request.put(HierarchyConstants.NODES_MODIFIED, getNodesModified_1())
            request.put(HierarchyConstants.HIERARCHY, getHierarchy_1())
            UpdateHierarchyManager.updateHierarchy(request).map(response => {
                assert(response.getResponseCode.code() == 200)
                val hierarchy = readFromCassandra("Select hierarchy from hierarchy_store.content_hierarchy where identifier='do_11294581887465881611'")
                    .one().getString(HierarchyConstants.HIERARCHY)
                assert(StringUtils.isNotEmpty(hierarchy))
                request.put(HierarchyConstants.NODES_MODIFIED, getNodesModified_TOC_UPLOAD_STYLE())
                request.put(HierarchyConstants.HIERARCHY, getHierarchy_Null())
                UpdateHierarchyManager.updateHierarchy(request).map(resp => {
                    assert(response.getResponseCode.code() == 200)
                    val hierarchy_updated = readFromCassandra("Select hierarchy from hierarchy_store.content_hierarchy where identifier='do_11294581887465881611'")
                        .one().getString(HierarchyConstants.HIERARCHY)
                    assert(StringUtils.isNotEmpty(hierarchy_updated))
                })
            }).flatMap(f => f)
        }).flatMap(f => f)
    }

    "updateHierarchy test proper ordering" should "succeed with proper hierarchy structure" in {
        val nodesModified = "{\"do_113031517435822080121\":{\"metadata\":{\"license\":\"CC BY 4.0\"},\"root\":true,\"isNew\":false},\"u1\":{\"metadata\":{\"name\":\"U1\",\"dialcodeRequired\":\"No\",\"mimeType\":\"application/vnd.ekstep.content-collection\",\"contentType\":\"CourseUnit\",\"primaryCategory\": \"Course Unit\",\"license\":\"CC BY 4.0\"},\"root\":false,\"isNew\":true},\"u2\":{\"metadata\":{\"name\":\"U2\",\"dialcodeRequired\":\"No\",\"mimeType\":\"application/vnd.ekstep.content-collection\",\"contentType\":\"CourseUnit\",\"primaryCategory\": \"Course Unit\",\"license\":\"CC BY 4.0\"},\"root\":false,\"isNew\":true}}";
        val hierarchy = "{\"do_113031517435822080121\":{\"children\":[\"u1\",\"u2\"],\"root\":true,\"name\":\"Untitled Textbook\",\"contentType\":\"Course\",\"primaryCategory\": \"Learning Resource\"},\"u1\":{\"children\":[\"do_11303151546543308811\",\"do_11303151571010355212\",\"do_11303151584773734413\",\"do_11303151594263347214\",\"do_11303151604402585615\",\"do_11303151612719104016\",\"do_11303151623148339217\",\"do_11303151631740928018\",\"do_11303151638961356819\",\"do_113031516469411840110\"],\"root\":false,\"name\":\"U1\",\"contentType\":\"CourseUnit\",\"primaryCategory\": \"Learning Resource\"},\"u2\":{\"children\":[\"do_113031516541870080111\",\"do_113031516616491008112\",\"do_113031516693184512113\",\"do_113031516791406592114\",\"do_113031516862660608115\",\"do_113031516945334272116\",\"do_113031517024190464117\",\"do_113031517104939008118\",\"do_113031517200171008119\",\"do_113031517276520448120\"],\"root\":false,\"name\":\"U2\",\"contentType\":\"CourseUnit\",\"primaryCategory\": \"Learning Resource\"},\"do_11303151546543308811\":{\"name\":\"prad PDF Content-1\",\"contentType\":\"Resource\",\"primaryCategory\": \"Learning Resource\",\"children\":[],\"root\":false},\"do_11303151571010355212\":{\"name\":\"prad PDF Content-2\",\"contentType\":\"Resource\",\"primaryCategory\": \"Learning Resource\",\"children\":[],\"root\":false},\"do_11303151584773734413\":{\"name\":\"prad PDF Content-3\",\"contentType\":\"Resource\",\"primaryCategory\": \"Learning Resource\",\"children\":[],\"root\":false},\"do_11303151594263347214\":{\"name\":\"prad PDF Content-4\",\"contentType\":\"Resource\",\"primaryCategory\": \"Learning Resource\",\"children\":[],\"root\":false},\"do_11303151604402585615\":{\"name\":\"prad PDF Content-5\",\"contentType\":\"Resource\",\"primaryCategory\": \"Learning Resource\",\"children\":[],\"root\":false},\"do_11303151612719104016\":{\"name\":\"prad PDF Content-6\",\"contentType\":\"Resource\",\"primaryCategory\": \"Learning Resource\",\"children\":[],\"root\":false},\"do_11303151623148339217\":{\"name\":\"prad PDF Content-7\",\"contentType\":\"Resource\",\"primaryCategory\": \"Learning Resource\",\"children\":[],\"root\":false},\"do_11303151631740928018\":{\"name\":\"prad PDF Content-8\",\"contentType\":\"Resource\",\"primaryCategory\": \"Learning Resource\",\"children\":[],\"root\":false},\"do_11303151638961356819\":{\"name\":\"prad PDF Content-9\",\"contentType\":\"Resource\",\"primaryCategory\": \"Learning Resource\",\"children\":[],\"root\":false},\"do_113031516469411840110\":{\"name\":\"prad PDF Content-10\",\"contentType\":\"Resource\",\"primaryCategory\": \"Learning Resource\",\"children\":[],\"root\":false},\"do_113031516541870080111\":{\"name\":\"prad PDF Content-11\",\"contentType\":\"Resource\",\"primaryCategory\": \"Learning Resource\",\"children\":[],\"root\":false},\"do_113031516616491008112\":{\"name\":\"prad PDF Content-12\",\"contentType\":\"Resource\",\"primaryCategory\": \"Learning Resource\",\"children\":[],\"root\":false},\"do_113031516693184512113\":{\"name\":\"prad PDF Content-13\",\"contentType\":\"Resource\",\"primaryCategory\": \"Learning Resource\",\"children\":[],\"root\":false},\"do_113031516791406592114\":{\"name\":\"prad PDF Content-14\",\"contentType\":\"Resource\",\"primaryCategory\": \"Learning Resource\",\"children\":[],\"root\":false},\"do_113031516862660608115\":{\"name\":\"prad PDF Content-15\",\"contentType\":\"Resource\",\"primaryCategory\": \"Learning Resource\",\"children\":[],\"root\":false},\"do_113031516945334272116\":{\"name\":\"prad PDF Content-16\",\"contentType\":\"Resource\",\"primaryCategory\": \"Learning Resource\",\"children\":[],\"root\":false},\"do_113031517024190464117\":{\"name\":\"prad PDF Content-17\",\"contentType\":\"Resource\",\"primaryCategory\": \"Learning Resource\",\"children\":[],\"root\":false},\"do_113031517104939008118\":{\"name\":\"prad PDF Content-18\",\"contentType\":\"Resource\",\"primaryCategory\": \"Learning Resource\",\"children\":[],\"root\":false},\"do_113031517200171008119\":{\"name\":\"prad PDF Content-19\",\"contentType\":\"Resource\",\"primaryCategory\": \"Learning Resource\",\"children\":[],\"root\":false},\"do_113031517276520448120\":{\"name\":\"prad PDF Content-20\",\"contentType\":\"Resource\",\"primaryCategory\": \"Learning Resource\",\"children\":[],\"root\":false}}";
        val request = new Request()
        val context = getContext()
        context.put(HierarchyConstants.SCHEMA_NAME, "collection")
        request.setContext(context)
        request.put(HierarchyConstants.NODES_MODIFIED, JsonUtils.deserialize(nodesModified, classOf[util.HashMap[String, AnyRef]]))
        request.put(HierarchyConstants.HIERARCHY, JsonUtils.deserialize(hierarchy, classOf[util.HashMap[String, AnyRef]]))
        UpdateHierarchyManager.updateHierarchy(request).map(response => {
            assert(response.getResponseCode.code() == 200)
            val identifiers =  response.get(HierarchyConstants.IDENTIFIERS).asInstanceOf[util.Map[String, AnyRef]]
            val hierarchyResp = readFromCassandra("Select hierarchy from hierarchy_store.content_hierarchy where identifier='do_113031517435822080121'")
                    .one().getString(HierarchyConstants.HIERARCHY)
            assert(StringUtils.isNotEmpty(hierarchyResp))
            val children = JsonUtils.deserialize(hierarchyResp, classOf[util.HashMap[String, AnyRef]]).get("children").asInstanceOf[util.List[util.Map[String, AnyRef]]]
            assert(StringUtils.equalsIgnoreCase(children.get(0).get("identifier").asInstanceOf[String], identifiers.get("u1").asInstanceOf[String]))
            val u1Children = children.get(0).get("children").asInstanceOf[util.List[util.Map[String, AnyRef]]]
            assert(StringUtils.equalsIgnoreCase(u1Children.get(0).get("identifier").asInstanceOf[String], "do_11303151546543308811"))
            assert(StringUtils.equalsIgnoreCase(u1Children.get(1).get("identifier").asInstanceOf[String], "do_11303151571010355212"))
            assert(StringUtils.equalsIgnoreCase(u1Children.get(2).get("identifier").asInstanceOf[String], "do_11303151584773734413"))
            assert(StringUtils.equalsIgnoreCase(u1Children.get(3).get("identifier").asInstanceOf[String], "do_11303151594263347214"))
            assert(StringUtils.equalsIgnoreCase(u1Children.get(4).get("identifier").asInstanceOf[String], "do_11303151604402585615"))
            assert(StringUtils.equalsIgnoreCase(u1Children.get(5).get("identifier").asInstanceOf[String], "do_11303151612719104016"))
            assert(StringUtils.equalsIgnoreCase(u1Children.get(6).get("identifier").asInstanceOf[String], "do_11303151623148339217"))
            assert(StringUtils.equalsIgnoreCase(u1Children.get(7).get("identifier").asInstanceOf[String], "do_11303151631740928018"))
            assert(StringUtils.equalsIgnoreCase(u1Children.get(8).get("identifier").asInstanceOf[String], "do_11303151638961356819"))
            assert(StringUtils.equalsIgnoreCase(u1Children.get(9).get("identifier").asInstanceOf[String], "do_113031516469411840110"))

            assert(StringUtils.equalsIgnoreCase(children.get(1).get("identifier").asInstanceOf[String], identifiers.get("u2").asInstanceOf[String]))
            val u2Children = children.get(1).get("children").asInstanceOf[util.List[util.Map[String, AnyRef]]]
            assert(StringUtils.equalsIgnoreCase(u2Children.get(0).get("identifier").asInstanceOf[String], "do_113031516541870080111"))
            assert(StringUtils.equalsIgnoreCase(u2Children.get(1).get("identifier").asInstanceOf[String], "do_113031516616491008112"))
            assert(StringUtils.equalsIgnoreCase(u2Children.get(2).get("identifier").asInstanceOf[String], "do_113031516693184512113"))
            assert(StringUtils.equalsIgnoreCase(u2Children.get(3).get("identifier").asInstanceOf[String], "do_113031516791406592114"))
            assert(StringUtils.equalsIgnoreCase(u2Children.get(4).get("identifier").asInstanceOf[String], "do_113031516862660608115"))
            assert(StringUtils.equalsIgnoreCase(u2Children.get(5).get("identifier").asInstanceOf[String], "do_113031516945334272116"))
            assert(StringUtils.equalsIgnoreCase(u2Children.get(6).get("identifier").asInstanceOf[String], "do_113031517024190464117"))
            assert(StringUtils.equalsIgnoreCase(u2Children.get(7).get("identifier").asInstanceOf[String], "do_113031517104939008118"))
            assert(StringUtils.equalsIgnoreCase(u2Children.get(8).get("identifier").asInstanceOf[String], "do_113031517200171008119"))
            assert(StringUtils.equalsIgnoreCase(u2Children.get(9).get("identifier").asInstanceOf[String], "do_113031517276520448120"))

            val nodesModified = "{\"do_113031517435822080121\":{\"metadata\":{\"license\":\"CC BY 4.0\"},\"root\":true,\"isNew\":false}}";
            val request1 = new Request()
            val context = getContext()
            context.put(HierarchyConstants.SCHEMA_NAME, "collection")
            request1.setContext(context)
            request1.put(HierarchyConstants.NODES_MODIFIED, JsonUtils.deserialize(nodesModified, classOf[util.HashMap[String, AnyRef]]))
            request1.put(HierarchyConstants.HIERARCHY, new util.HashMap())
            UpdateHierarchyManager.updateHierarchy(request1).map(response => {
                assert(response.getResponseCode.code() == 200)
                val hierarchyResp = readFromCassandra("Select hierarchy from hierarchy_store.content_hierarchy where identifier='do_113031517435822080121'")
                        .one().getString(HierarchyConstants.HIERARCHY)
                assert(StringUtils.isNotEmpty(hierarchyResp))
                val children = JsonUtils.deserialize(hierarchyResp, classOf[util.HashMap[String, AnyRef]]).get("children").asInstanceOf[util.List[util.Map[String, AnyRef]]]
                val u1Children = children.get(0).get("children").asInstanceOf[util.List[util.Map[String, AnyRef]]]
                assert(StringUtils.equalsIgnoreCase(u1Children.get(0).get("identifier").asInstanceOf[String], "do_11303151546543308811"))
                assert(StringUtils.equalsIgnoreCase(u1Children.get(1).get("identifier").asInstanceOf[String], "do_11303151571010355212"))
                assert(StringUtils.equalsIgnoreCase(u1Children.get(2).get("identifier").asInstanceOf[String], "do_11303151584773734413"))
                assert(StringUtils.equalsIgnoreCase(u1Children.get(3).get("identifier").asInstanceOf[String], "do_11303151594263347214"))
                assert(StringUtils.equalsIgnoreCase(u1Children.get(4).get("identifier").asInstanceOf[String], "do_11303151604402585615"))
                assert(StringUtils.equalsIgnoreCase(u1Children.get(5).get("identifier").asInstanceOf[String], "do_11303151612719104016"))
                assert(StringUtils.equalsIgnoreCase(u1Children.get(6).get("identifier").asInstanceOf[String], "do_11303151623148339217"))
                assert(StringUtils.equalsIgnoreCase(u1Children.get(7).get("identifier").asInstanceOf[String], "do_11303151631740928018"))
                assert(StringUtils.equalsIgnoreCase(u1Children.get(8).get("identifier").asInstanceOf[String], "do_11303151638961356819"))
                assert(StringUtils.equalsIgnoreCase(u1Children.get(9).get("identifier").asInstanceOf[String], "do_113031516469411840110"))

                val u2Children = children.get(1).get("children").asInstanceOf[util.List[util.Map[String, AnyRef]]]
                assert(StringUtils.equalsIgnoreCase(u2Children.get(0).get("identifier").asInstanceOf[String], "do_113031516541870080111"))
                assert(StringUtils.equalsIgnoreCase(u2Children.get(1).get("identifier").asInstanceOf[String], "do_113031516616491008112"))
                assert(StringUtils.equalsIgnoreCase(u2Children.get(2).get("identifier").asInstanceOf[String], "do_113031516693184512113"))
                assert(StringUtils.equalsIgnoreCase(u2Children.get(3).get("identifier").asInstanceOf[String], "do_113031516791406592114"))
                assert(StringUtils.equalsIgnoreCase(u2Children.get(4).get("identifier").asInstanceOf[String], "do_113031516862660608115"))
                assert(StringUtils.equalsIgnoreCase(u2Children.get(5).get("identifier").asInstanceOf[String], "do_113031516945334272116"))
                assert(StringUtils.equalsIgnoreCase(u2Children.get(6).get("identifier").asInstanceOf[String], "do_113031517024190464117"))
                assert(StringUtils.equalsIgnoreCase(u2Children.get(7).get("identifier").asInstanceOf[String], "do_113031517104939008118"))
                assert(StringUtils.equalsIgnoreCase(u2Children.get(8).get("identifier").asInstanceOf[String], "do_113031517200171008119"))
                assert(StringUtils.equalsIgnoreCase(u2Children.get(9).get("identifier").asInstanceOf[String], "do_113031517276520448120"))
            })
        }).flatMap(f => f)
    }

    "updateHierarchy test proper ordering" should "succeed with proper hierarchy structure on same resources" in {
        val nodesModified = "{\"do_113031517435822080121\":{\"metadata\":{},\"root\":false,\"isNew\":false},\"u1\":{\"metadata\":{\"name\":\"U1\",\"dialcodeRequired\":\"No\",\"mimeType\":\"application/vnd.ekstep.content-collection\",\"primaryCategory\": \"Content Playlist\",\"contentType\":\"Collection\",\"license\":\"CC BY 4.0\"},\"root\":false,\"isNew\":true},\"u1.1\":{\"metadata\":{\"name\":\"U1.1\",\"dialcodeRequired\":\"No\",\"mimeType\":\"application/vnd.ekstep.content-collection\",\"primaryCategory\": \"Content Playlist\",\"contentType\":\"Collection\",\"license\":\"CC BY 4.0\"},\"root\":false,\"isNew\":true}}";
        val hierarchy = "{\"do_113031517435822080121\":{\"children\":[\"u1\"],\"root\":true,\"name\":\"CollectionTest\",\"primaryCategory\": \"Learning Resource\",\"contentType\":\"Collection\"},\"u1\":{\"children\":[\"do_11303151546543308811\",\"do_11303151571010355212\",\"do_11303151584773734413\",\"do_11303151594263347214\",\"u1.1\"],\"root\":false,\"name\":\"U1\",\"primaryCategory\": \"Learning Resource\",\"contentType\":\"Collection\"},\"u1.1\":{\"children\":[\"do_11303151594263347214\",\"do_11303151584773734413\",\"do_11303151571010355212\",\"do_11303151546543308811\"],\"root\":false,\"name\":\"U1.1\",\"primaryCategory\": \"Learning Resource\",\"contentType\":\"Collection\"},\"do_11303151546543308811\":{\"name\":\"prad PDF Content-1\",\"primaryCategory\": \"Learning Resource\",\"contentType\":\"Resource\",\"children\":[],\"root\":false},\"do_11303151571010355212\":{\"name\":\"prad PDF Content-2\",\"primaryCategory\": \"Learning Resource\",\"contentType\":\"Resource\",\"children\":[],\"root\":false},\"do_11303151584773734413\":{\"name\":\"prad PDF Content-3\",\"primaryCategory\": \"Learning Resource\",\"contentType\":\"Resource\",\"children\":[],\"root\":false},\"do_11303151594263347214\":{\"name\":\"prad PDF Content-4\",\"primaryCategory\": \"Learning Resource\",\"contentType\":\"Resource\",\"children\":[],\"root\":false}}";
        val request = new Request()
        val context = getContext()
        context.put(HierarchyConstants.SCHEMA_NAME, "collection")
        request.setContext(context)
        request.put(HierarchyConstants.NODES_MODIFIED, JsonUtils.deserialize(nodesModified, classOf[util.HashMap[String, AnyRef]]))
        request.put(HierarchyConstants.HIERARCHY, JsonUtils.deserialize(hierarchy, classOf[util.HashMap[String, AnyRef]]))
        UpdateHierarchyManager.updateHierarchy(request).map(response => {
            assert(response.getResponseCode.code() == 200)
            val identifiers = response.get(HierarchyConstants.IDENTIFIERS).asInstanceOf[util.Map[String, AnyRef]]
            val hierarchyResp = readFromCassandra("Select hierarchy from hierarchy_store.content_hierarchy where identifier='do_113031517435822080121'")
                    .one().getString(HierarchyConstants.HIERARCHY)
            assert(StringUtils.isNotEmpty(hierarchyResp))
            val children = JsonUtils.deserialize(hierarchyResp, classOf[util.HashMap[String, AnyRef]]).get("children").asInstanceOf[util.List[util.Map[String, AnyRef]]]
            assert(StringUtils.equalsIgnoreCase(children.get(0).get("identifier").asInstanceOf[String], identifiers.get("u1").asInstanceOf[String]))
            val u1Children = children.get(0).get("children").asInstanceOf[util.List[util.Map[String, AnyRef]]]
            assert(StringUtils.equalsIgnoreCase(u1Children.get(0).get("identifier").asInstanceOf[String], "do_11303151546543308811"))
            assert(StringUtils.equalsIgnoreCase(u1Children.get(1).get("identifier").asInstanceOf[String], "do_11303151571010355212"))
            assert(StringUtils.equalsIgnoreCase(u1Children.get(2).get("identifier").asInstanceOf[String], "do_11303151584773734413"))
            assert(StringUtils.equalsIgnoreCase(u1Children.get(3).get("identifier").asInstanceOf[String], "do_11303151594263347214"))
            assert(StringUtils.equalsIgnoreCase(u1Children.get(4).get("identifier").asInstanceOf[String], identifiers.get("u1.1").asInstanceOf[String]))
            val u11Children = u1Children.get(4).get("children").asInstanceOf[util.List[util.Map[String, AnyRef]]]
            assert(StringUtils.equalsIgnoreCase(u11Children.get(3).get("identifier").asInstanceOf[String], "do_11303151546543308811"))
            assert(StringUtils.equalsIgnoreCase(u11Children.get(2).get("identifier").asInstanceOf[String], "do_11303151571010355212"))
            assert(StringUtils.equalsIgnoreCase(u11Children.get(1).get("identifier").asInstanceOf[String], "do_11303151584773734413"))
            assert(StringUtils.equalsIgnoreCase(u11Children.get(0).get("identifier").asInstanceOf[String], "do_11303151594263347214"))
        })
    }

    "updateHierarchy with originData under root metadata" should "successfully store originData in root node" in {
        val nodesModified = "{\"do_test_book_1\":{\"isNew\":true,\"root\":true,\"metadata\":{\"origin\":\"do_113000859727618048110\",\"originData\":{\"channel\":\"012983850117177344161\"}}},\"TestBookUnit-01\":{\"isNew\":true,\"root\":false,\"metadata\":{\"mimeType\":\"application/vnd.ekstep.content-collection\",\"keywords\":[],\"name\":\"U-1\",\"description\":\"U-1\",\"primaryCategory\": \"Textbook Unit\",\"contentType\":\"TextBookUnit\",\"code\":\"TestBookUnit-01\"}},\"TestBookUnit-02\":{\"isNew\":true,\"root\":false,\"metadata\":{\"mimeType\":\"application/vnd.ekstep.content-collection\",\"keywords\":[],\"name\":\"U-2\",\"description\":\"U-2\",\"primaryCategory\": \"Textbook Unit\",\"contentType\":\"TextBookUnit\",\"code\":\"TestBookUnit-02\"}}}"
        val hierarchy = "{\"do_test_book_1\":{\"name\":\"Update Hierarchy Test For Origin Data\",\"primaryCategory\": \"Learning Resource\",\"contentType\":\"TextBook\",\"children\":[\"TestBookUnit-01\",\"TestBookUnit-02\"],\"root\":true},\"TestBookUnit-01\":{\"name\":\"U-1\",\"primaryCategory\": \"Learning Resource\",\"contentType\":\"TextBookUnit\",\"children\":[],\"root\":false},\"TestBookUnit-02\":{\"name\":\"U-2\",\"primaryCategory\": \"Learning Resource\",\"contentType\":\"TextBookUnit\",\"children\":[],\"root\":false}}"
        val request = new Request()
        val context = getContext()
        context.put(HierarchyConstants.SCHEMA_NAME, "collection")
        request.setContext(context)
        request.put(HierarchyConstants.NODES_MODIFIED, JsonUtils.deserialize(nodesModified, classOf[util.HashMap[String, AnyRef]]))
        request.put(HierarchyConstants.HIERARCHY, JsonUtils.deserialize(hierarchy, classOf[util.HashMap[String, AnyRef]]))
        UpdateHierarchyManager.updateHierarchy(request).map(response => {
            assert(response.getResponseCode.code() == 200)
            val identifiers = response.get(HierarchyConstants.IDENTIFIERS).asInstanceOf[util.Map[String, AnyRef]]
            val hierarchyResp = readFromCassandra("Select hierarchy from hierarchy_store.content_hierarchy where identifier='do_test_book_1'")
              .one().getString(HierarchyConstants.HIERARCHY)
            assert(StringUtils.isNotEmpty(hierarchyResp))
            val children = JsonUtils.deserialize(hierarchyResp, classOf[util.HashMap[String, AnyRef]]).get("children").asInstanceOf[util.List[util.Map[String, AnyRef]]]
            assert(StringUtils.equalsIgnoreCase(children.get(0).get("identifier").asInstanceOf[String], identifiers.get("TestBookUnit-01").asInstanceOf[String]))
            val getHierarchyReq = new Request()
            val reqContext = getContext()
            reqContext.put(HierarchyConstants.SCHEMA_NAME, "collection")
            getHierarchyReq.setContext(reqContext)
            getHierarchyReq.put("rootId", "do_test_book_1")
            getHierarchyReq.put("mode","edit")
            val future = HierarchyManager.getHierarchy(getHierarchyReq)
            future.map(response => {
                assert(response.getResponseCode.code() == 200)
                assert(null != response.getResult.get("content"))
                val content = response.getResult.get("content").asInstanceOf[util.Map[String, AnyRef]]
                assert(null != content.get("originData"))
                assert(null != content.get("children"))
            })
        }).flatMap(f=>f)
    }


    //Text Book -> root, New Unit
    def getNodesModified_1(): util.HashMap[String, AnyRef] = {
        val nodesModifiedString: String =    "{\n"+
            "                \"do_11294581887465881611\": {\n"+
            "                    \"isNew\": false,\n"+
            "                    \"root\": true\n"+
            "                },\n"+
            "                \"b9a50833-eff6-4ef5-a2a4-2413f2d51f6c\": {\n"+
            "                    \"isNew\": true,\n"+
            "                    \"root\": false,\n"+
            "                    \"metadata\": {\n"+
            "                        \"mimeType\": \"application/vnd.ekstep.content-collection\",\n"+
            "                        \"contentType\": \"TextBookUnit\",\n"+
            "                        \"code\": \"updateHierarchy\",\n"+
            "                        \"name\": \"Test_CourseUnit_1\",\n"+
            "                        \"description\": \"updated hierarchy\",\n"+
            "                        \"channel\": \"in.ekstep\",\n"+
            "                         \"primaryCategory\": \"Textbook Unit\"\n"+
            "                    }\n"+
            "                }\n"+
            "            }"
        JsonUtils.deserialize(nodesModifiedString, classOf[util.HashMap[String, AnyRef]])
    }
    //Text
    def getHierarchy_1(): util.HashMap[String, AnyRef] = {
        val hierarchyString =     "{\n"+
            "            \t\"do_11294581887465881611\" : {\n"+
            "            \t\t\"root\": true,\n"+
            "            \t\t\"children\": [\"b9a50833-eff6-4ef5-a2a4-2413f2d51f6c\"]\n"+
            "            \t},\n"+
            "            \t\"b9a50833-eff6-4ef5-a2a4-2413f2d51f6c\": {\n"+
            "            \t\t\"root\": false,\n"+
            "            \t\t\"children\": [\"do_31250856200414822416938\"]\n"+
            "            \t}\n"+
            "            }"
        JsonUtils.deserialize(hierarchyString, classOf[util.HashMap[String, AnyRef]])
    }

    def getNodesModified_2(rootId:String, unit_1: String): util.HashMap[String, AnyRef] = {
        val nodesModifiedString: String = "{\n"+
            "        \""+ rootId +"\": {\n"+
            "          \"isNew\": false,\n"+
            "          \"root\": true,\n"+
            "          \"metadata\": {\n"+
            "            \"name\": \"updated text book name check\"\n"+
            "          }\n"+
            "        },\n"+
            "        \""+ unit_1 +"\": {\n"+
            "          \"isNew\": false,\n"+
            "          \"root\": false\n"+
            "        },\n"+
            "        \"b9a50833-eff6-4ef5-a2a4-2413f2d51f6c\": {\n"+
            "          \"isNew\": true,\n"+
            "          \"root\": false,\n"+
            "          \"metadata\": {\n"+
            "            \"mimeType\": \"application/vnd.ekstep.content-collection\",\n"+
            "            \"contentType\": \"TextBookUnit\",\n"+
            "            \"code\": \"updateHierarchy\",\n"+
            "            \"name\": \"Test_CourseUnit_1\",\n"+
            "            \"description\": \"Test_CourseUnit_desc_1\",\n"+
            "            \"primaryCategory\": \"Textbook Unit\"\n"+
            "          }\n"+
            "        }" +
            "}"
        JsonUtils.deserialize(nodesModifiedString, classOf[util.HashMap[String, AnyRef]])
    }

    def getHierarchy_2(rootId: String, unit_2: String): util.HashMap[String, AnyRef] = {
        val hierarchyString: String = "{\n" +
            "        \"" + rootId + "\": {\n" +
            "          \"root\": true,\n" +
            "          \"children\": [\n" +
            "            \"b9a50833-eff6-4ef5-a2a4-2413f2d51f6c\"\n" +
            "          ]\n" +
            "        },\n" +
            "        \"b9a50833-eff6-4ef5-a2a4-2413f2d51f6c\": {\n" +
            "          \"root\": false,\n" +
            "          \"children\": [\n" +
            "            \"" + unit_2 + "\",\n" +
            "            \"do_30109819\"\n" +
            "          ]\n" +
            "        },\n" +
            "        \"" + unit_2 + "\": {\n" +
            "          \"root\": false,\n" +
            "          \"children\": [\n" +
            "            \"do_31250856200414822416938\"\n" +
            "          ]\n" +
            "        }\n" +
            "      }"
        JsonUtils.deserialize(hierarchyString, classOf[util.HashMap[String, AnyRef]])
    }

    def getNodesModified_TOC_UPLOAD_STYLE(): util.HashMap[String, AnyRef] = {
        val nodesModifiedString: String = "{" +
            "\"do_112971262874558464179\": {\n" +
            "                    \"root\": false,\n" +
            "                    \"isNew\": true,\n" +
            "                    \"metadata\": {\n" +
            "                        \"attributions\": [\n" +
            "                            \"mk\"\n" +
            "                        ],\n" +
            "                        \"name\": \"Test update hierarchy-30918948914\",\n" +
            "                        \"contentType\": \"TextBookUnit\",\n" +
            "                        \"mimeType\": \"application/vnd.ekstep.content-collection\",\n" +
            "                        \"versionKey\": \"1583406182466\"\n" +
            "                    }\n" +
            "                },\n" +
            "                \"do_11294581887465881611\": {\n" +
            "                    \"isNew\": false,\n" +
            "                    \"root\": true,\n" +
            "                    \"metadata\": {}\n" +
            "                }" +
            "}"
        JsonUtils.deserialize(nodesModifiedString, classOf[util.HashMap[String, AnyRef]])
    }

    def getHierarchy_Null(): util.HashMap[String, AnyRef] = {
        val hierarchyString: String = "{}"
        JsonUtils.deserialize(hierarchyString, classOf[util.HashMap[String, AnyRef]])
    }

    //Text
    def getHierarchy_Content_Resource_Invalid_ID(): util.HashMap[String, AnyRef] = {
        val hierarchyString =     "{\n"+
            "            \t\"do_11294581887465881611\" : {\n"+
            "            \t\t\"root\": true,\n"+
            "            \t\t\"children\": [\"b9a50833-eff6-4ef5-a2a4-2413f2d51f6c\"]\n"+
            "            \t},\n"+
            "            \t\"b9a50833-eff6-4ef5-a2a4-2413f2d51f6c\": {\n"+
            "            \t\t\"root\": false,\n"+
            "            \t\t\"children\": [\"do_3125085620041482241\"]\n"+
            "            \t}\n"+
            "            }"
        JsonUtils.deserialize(hierarchyString, classOf[util.HashMap[String, AnyRef]])
    }


    def insert20NodesAndOneCourse() = {
        graphDb.execute("UNWIND [{ownershipType:[\"createdBy\"],code:\"txtbk\",channel:\"in.ekstep\",description:\"Text Book Test\",language:[\"English\"],mimeType:\"application/vnd.ekstep.content-collection\",idealScreenSize:\"normal\",createdOn:\"2020-05-29T19:40:05.744+0530\",contentDisposition:\"inline\",contentEncoding:\"gzip\",lastUpdatedOn:\"2020-05-29T23:19:49.635+0530\",contentType:\"Course\",primaryCategory:\"Course\",dialcodeRequired:\"No\",identifier:\"do_113031517435822080121\",audience:[\"Student\"],lastStatusChangedOn:\"2020-05-29T19:40:05.744+0530\",os:[\"All\"],visibility:\"Default\",IL_SYS_NODE_TYPE:\"DATA_NODE\",mediaType:\"content\",osId:\"org.ekstep.quiz.app\",version:2,versionKey:\"1590774589635\",license:\"CC BY 4.0\",idealScreenDensity:\"hdpi\",depth:0,framework:\"NCF\",compatibilityLevel:1,IL_FUNC_OBJECT_TYPE:\"Collection\",name:\"TextBook\",IL_UNIQUE_ID:\"do_113031517435822080121\",status:\"Draft\"},\n{ownershipType:[\"createdBy\"],code:\"test-Resourcce\",channel:\"in.ekstep\",language:[\"English\"],mimeType:\"application/pdf\",idealScreenSize:\"normal\",createdOn:\"2020-05-29T19:38:16.618+0530\",contentDisposition:\"inline\",contentEncoding:\"identity\",lastUpdatedOn:\"2020-05-29T19:38:16.618+0530\",contentType:\"Resource\",primaryCategory:\"Learning Resource\",dialcodeRequired:\"No\",audience:[\"Student\"],lastStatusChangedOn:\"2020-05-29T19:38:16.618+0530\",os:[\"All\"],visibility:\"Default\",IL_SYS_NODE_TYPE:\"DATA_NODE\",mediaType:\"content\",osId:\"org.ekstep.quiz.app\",version:2,versionKey:\"1590761296618\",idealScreenDensity:\"hdpi\",license:\"CC BY 4.0\",framework:\"NCF\",compatibilityLevel:1,IL_FUNC_OBJECT_TYPE:\"Content\",name:\"prad PDF Content-11\",IL_UNIQUE_ID:\"do_113031516541870080111\",status:\"Draft\"},\n{ownershipType:[\"createdBy\"],code:\"test-Resourcce\",channel:\"in.ekstep\",language:[\"English\"],mimeType:\"application/pdf\",idealScreenSize:\"normal\",createdOn:\"2020-05-29T19:36:35.092+0530\",contentDisposition:\"inline\",contentEncoding:\"identity\",lastUpdatedOn:\"2020-05-29T19:36:35.092+0530\",contentType:\"Resource\",primaryCategory:\"Learning Resource\",dialcodeRequired:\"No\",audience:[\"Student\"],lastStatusChangedOn:\"2020-05-29T19:36:35.092+0530\",os:[\"All\"],visibility:\"Default\",IL_SYS_NODE_TYPE:\"DATA_NODE\",mediaType:\"content\",osId:\"org.ekstep.quiz.app\",version:2,versionKey:\"1590761195092\",idealScreenDensity:\"hdpi\",license:\"CC BY 4.0\",framework:\"NCF\",compatibilityLevel:1,IL_FUNC_OBJECT_TYPE:\"Content\",name:\"prad PDF Content-2\",IL_UNIQUE_ID:\"do_11303151571010355212\",status:\"Draft\"},\n{ownershipType:[\"createdBy\"],code:\"test-Resourcce\",channel:\"in.ekstep\",language:[\"English\"],mimeType:\"application/pdf\",idealScreenSize:\"normal\",createdOn:\"2020-05-29T19:38:25.737+0530\",contentDisposition:\"inline\",contentEncoding:\"identity\",lastUpdatedOn:\"2020-05-29T19:38:25.737+0530\",contentType:\"Resource\",primaryCategory:\"Learning Resource\",dialcodeRequired:\"No\",audience:[\"Student\"],lastStatusChangedOn:\"2020-05-29T19:38:25.737+0530\",os:[\"All\"],visibility:\"Default\",IL_SYS_NODE_TYPE:\"DATA_NODE\",mediaType:\"content\",osId:\"org.ekstep.quiz.app\",version:2,versionKey:\"1590761305737\",idealScreenDensity:\"hdpi\",license:\"CC BY 4.0\",framework:\"NCF\",compatibilityLevel:1,IL_FUNC_OBJECT_TYPE:\"Content\",name:\"prad PDF Content-12\",IL_UNIQUE_ID:\"do_113031516616491008112\",status:\"Draft\"},\n{ownershipType:[\"createdBy\"],code:\"test-Resourcce\",channel:\"in.ekstep\",language:[\"English\"],mimeType:\"application/pdf\",idealScreenSize:\"normal\",createdOn:\"2020-05-29T19:37:15.852+0530\",contentDisposition:\"inline\",contentEncoding:\"identity\",lastUpdatedOn:\"2020-05-29T19:37:15.852+0530\",contentType:\"Resource\",primaryCategory:\"Learning Resource\",dialcodeRequired:\"No\",audience:[\"Student\"],lastStatusChangedOn:\"2020-05-29T19:37:15.852+0530\",os:[\"All\"],visibility:\"Default\",IL_SYS_NODE_TYPE:\"DATA_NODE\",mediaType:\"content\",osId:\"org.ekstep.quiz.app\",version:2,versionKey:\"1590761235852\",idealScreenDensity:\"hdpi\",license:\"CC BY 4.0\",framework:\"NCF\",compatibilityLevel:1,IL_FUNC_OBJECT_TYPE:\"Content\",name:\"prad PDF Content-5\",IL_UNIQUE_ID:\"do_11303151604402585615\",status:\"Draft\"},\n{ownershipType:[\"createdBy\"],code:\"test-Resourcce\",channel:\"in.ekstep\",language:[\"English\"],mimeType:\"application/pdf\",idealScreenSize:\"normal\",createdOn:\"2020-05-29T19:37:03.470+0530\",contentDisposition:\"inline\",contentEncoding:\"identity\",lastUpdatedOn:\"2020-05-29T19:37:03.470+0530\",contentType:\"Resource\",primaryCategory:\"Learning Resource\",dialcodeRequired:\"No\",audience:[\"Student\"],lastStatusChangedOn:\"2020-05-29T19:37:03.470+0530\",os:[\"All\"],visibility:\"Default\",IL_SYS_NODE_TYPE:\"DATA_NODE\",mediaType:\"content\",osId:\"org.ekstep.quiz.app\",version:2,versionKey:\"1590761223470\",idealScreenDensity:\"hdpi\",license:\"CC BY 4.0\",framework:\"NCF\",compatibilityLevel:1,IL_FUNC_OBJECT_TYPE:\"Content\",name:\"prad PDF Content-4\",IL_UNIQUE_ID:\"do_11303151594263347214\",status:\"Draft\"},\n{ownershipType:[\"createdBy\"],code:\"test-Resourcce\",channel:\"in.ekstep\",language:[\"English\"],mimeType:\"application/pdf\",idealScreenSize:\"normal\",createdOn:\"2020-05-29T19:39:15.501+0530\",contentDisposition:\"inline\",contentEncoding:\"identity\",lastUpdatedOn:\"2020-05-29T19:39:15.501+0530\",contentType:\"Resource\",primaryCategory:\"Learning Resource\",dialcodeRequired:\"No\",audience:[\"Student\"],lastStatusChangedOn:\"2020-05-29T19:39:15.501+0530\",os:[\"All\"],visibility:\"Default\",IL_SYS_NODE_TYPE:\"DATA_NODE\",mediaType:\"content\",osId:\"org.ekstep.quiz.app\",version:2,versionKey:\"1590761355501\",idealScreenDensity:\"hdpi\",license:\"CC BY 4.0\",framework:\"NCF\",compatibilityLevel:1,IL_FUNC_OBJECT_TYPE:\"Content\",name:\"prad PDF Content-17\",IL_UNIQUE_ID:\"do_113031517024190464117\",status:\"Draft\"},\n{ownershipType:[\"createdBy\"],code:\"test-Resourcce\",channel:\"in.ekstep\",language:[\"English\"],mimeType:\"application/pdf\",idealScreenSize:\"normal\",createdOn:\"2020-05-29T19:38:47.078+0530\",contentDisposition:\"inline\",contentEncoding:\"identity\",lastUpdatedOn:\"2020-05-29T19:38:47.078+0530\",contentType:\"Resource\",primaryCategory:\"Learning Resource\",dialcodeRequired:\"No\",audience:[\"Student\"],lastStatusChangedOn:\"2020-05-29T19:38:47.078+0530\",os:[\"All\"],visibility:\"Default\",IL_SYS_NODE_TYPE:\"DATA_NODE\",mediaType:\"content\",osId:\"org.ekstep.quiz.app\",version:2,versionKey:\"1590761327078\",idealScreenDensity:\"hdpi\",license:\"CC BY 4.0\",framework:\"NCF\",compatibilityLevel:1,IL_FUNC_OBJECT_TYPE:\"Content\",name:\"prad PDF Content-14\",IL_UNIQUE_ID:\"do_113031516791406592114\",status:\"Draft\"},\n{ownershipType:[\"createdBy\"],code:\"test-Resourcce\",channel:\"in.ekstep\",language:[\"English\"],mimeType:\"application/pdf\",idealScreenSize:\"normal\",createdOn:\"2020-05-29T19:37:58.033+0530\",contentDisposition:\"inline\",contentEncoding:\"identity\",lastUpdatedOn:\"2020-05-29T19:37:58.033+0530\",contentType:\"Resource\",primaryCategory:\"Learning Resource\",dialcodeRequired:\"No\",audience:[\"Student\"],lastStatusChangedOn:\"2020-05-29T19:37:58.033+0530\",os:[\"All\"],visibility:\"Default\",IL_SYS_NODE_TYPE:\"DATA_NODE\",mediaType:\"content\",osId:\"org.ekstep.quiz.app\",version:2,versionKey:\"1590761278033\",idealScreenDensity:\"hdpi\",license:\"CC BY 4.0\",framework:\"NCF\",compatibilityLevel:1,IL_FUNC_OBJECT_TYPE:\"Content\",name:\"prad PDF Content-9\",IL_UNIQUE_ID:\"do_11303151638961356819\",status:\"Draft\"},\n{ownershipType:[\"createdBy\"],code:\"test-Resourcce\",channel:\"in.ekstep\",language:[\"English\"],mimeType:\"application/pdf\",idealScreenSize:\"normal\",createdOn:\"2020-05-29T19:39:46.297+0530\",contentDisposition:\"inline\",contentEncoding:\"identity\",lastUpdatedOn:\"2020-05-29T19:39:46.297+0530\",contentType:\"Resource\",primaryCategory:\"Learning Resource\",dialcodeRequired:\"No\",audience:[\"Student\"],lastStatusChangedOn:\"2020-05-29T19:39:46.297+0530\",os:[\"All\"],visibility:\"Default\",IL_SYS_NODE_TYPE:\"DATA_NODE\",mediaType:\"content\",osId:\"org.ekstep.quiz.app\",version:2,versionKey:\"1590761386297\",idealScreenDensity:\"hdpi\",license:\"CC BY 4.0\",framework:\"NCF\",compatibilityLevel:1,IL_FUNC_OBJECT_TYPE:\"Content\",name:\"prad PDF Content-20\",IL_UNIQUE_ID:\"do_113031517276520448120\",status:\"Draft\"},\n{ownershipType:[\"createdBy\"],code:\"test-Resourcce\",channel:\"in.ekstep\",language:[\"English\"],mimeType:\"application/pdf\",idealScreenSize:\"normal\",createdOn:\"2020-05-29T19:36:51.893+0530\",contentDisposition:\"inline\",contentEncoding:\"identity\",lastUpdatedOn:\"2020-05-29T19:36:51.893+0530\",contentType:\"Resource\",primaryCategory:\"Learning Resource\",dialcodeRequired:\"No\",audience:[\"Student\"],lastStatusChangedOn:\"2020-05-29T19:36:51.893+0530\",os:[\"All\"],visibility:\"Default\",IL_SYS_NODE_TYPE:\"DATA_NODE\",mediaType:\"content\",osId:\"org.ekstep.quiz.app\",version:2,versionKey:\"1590761211893\",idealScreenDensity:\"hdpi\",license:\"CC BY 4.0\",framework:\"NCF\",compatibilityLevel:1,IL_FUNC_OBJECT_TYPE:\"Content\",name:\"prad PDF Content-3\",IL_UNIQUE_ID:\"do_11303151584773734413\",status:\"Draft\"},\n{ownershipType:[\"createdBy\"],code:\"test-Resourcce\",channel:\"in.ekstep\",language:[\"English\"],mimeType:\"application/pdf\",idealScreenSize:\"normal\",createdOn:\"2020-05-29T19:39:25.353+0530\",contentDisposition:\"inline\",contentEncoding:\"identity\",lastUpdatedOn:\"2020-05-29T19:39:25.353+0530\",contentType:\"Resource\",primaryCategory:\"Learning Resource\",dialcodeRequired:\"No\",audience:[\"Student\"],lastStatusChangedOn:\"2020-05-29T19:39:25.353+0530\",os:[\"All\"],visibility:\"Default\",IL_SYS_NODE_TYPE:\"DATA_NODE\",mediaType:\"content\",osId:\"org.ekstep.quiz.app\",version:2,versionKey:\"1590761365353\",idealScreenDensity:\"hdpi\",license:\"CC BY 4.0\",framework:\"NCF\",compatibilityLevel:1,IL_FUNC_OBJECT_TYPE:\"Content\",name:\"prad PDF Content-18\",IL_UNIQUE_ID:\"do_113031517104939008118\",status:\"Draft\"},\n{ownershipType:[\"createdBy\"],code:\"test-Resourcce\",channel:\"in.ekstep\",language:[\"English\"],mimeType:\"application/pdf\",idealScreenSize:\"normal\",createdOn:\"2020-05-29T19:38:35.090+0530\",contentDisposition:\"inline\",contentEncoding:\"identity\",lastUpdatedOn:\"2020-05-29T19:38:35.090+0530\",contentType:\"Resource\",primaryCategory:\"Learning Resource\",dialcodeRequired:\"No\",audience:[\"Student\"],lastStatusChangedOn:\"2020-05-29T19:38:35.090+0530\",os:[\"All\"],visibility:\"Default\",IL_SYS_NODE_TYPE:\"DATA_NODE\",mediaType:\"content\",osId:\"org.ekstep.quiz.app\",version:2,versionKey:\"1590761315090\",idealScreenDensity:\"hdpi\",license:\"CC BY 4.0\",framework:\"NCF\",compatibilityLevel:1,IL_FUNC_OBJECT_TYPE:\"Content\",name:\"prad PDF Content-13\",IL_UNIQUE_ID:\"do_113031516693184512113\",status:\"Draft\"},\n{ownershipType:[\"createdBy\"],code:\"test-Resourcce\",channel:\"in.ekstep\",language:[\"English\"],mimeType:\"application/pdf\",idealScreenSize:\"normal\",createdOn:\"2020-05-29T19:39:05.869+0530\",contentDisposition:\"inline\",contentEncoding:\"identity\",lastUpdatedOn:\"2020-05-29T19:39:05.869+0530\",contentType:\"Resource\",primaryCategory:\"Learning Resource\",dialcodeRequired:\"No\",audience:[\"Student\"],lastStatusChangedOn:\"2020-05-29T19:39:05.869+0530\",os:[\"All\"],visibility:\"Default\",IL_SYS_NODE_TYPE:\"DATA_NODE\",mediaType:\"content\",osId:\"org.ekstep.quiz.app\",version:2,versionKey:\"1590761345869\",idealScreenDensity:\"hdpi\",license:\"CC BY 4.0\",framework:\"NCF\",compatibilityLevel:1,IL_FUNC_OBJECT_TYPE:\"Content\",name:\"prad PDF Content-16\",IL_UNIQUE_ID:\"do_113031516945334272116\",status:\"Draft\"},\n{ownershipType:[\"createdBy\"],code:\"test-Resourcce\",channel:\"in.ekstep\",language:[\"English\"],mimeType:\"application/pdf\",idealScreenSize:\"normal\",createdOn:\"2020-05-29T19:38:07.777+0530\",contentDisposition:\"inline\",contentEncoding:\"identity\",lastUpdatedOn:\"2020-05-29T19:38:07.777+0530\",contentType:\"Resource\",primaryCategory:\"Learning Resource\",dialcodeRequired:\"No\",audience:[\"Student\"],lastStatusChangedOn:\"2020-05-29T19:38:07.777+0530\",os:[\"All\"],visibility:\"Default\",IL_SYS_NODE_TYPE:\"DATA_NODE\",mediaType:\"content\",osId:\"org.ekstep.quiz.app\",version:2,versionKey:\"1590761287777\",idealScreenDensity:\"hdpi\",license:\"CC BY 4.0\",framework:\"NCF\",compatibilityLevel:1,IL_FUNC_OBJECT_TYPE:\"Content\",name:\"prad PDF Content-10\",IL_UNIQUE_ID:\"do_113031516469411840110\",status:\"Draft\"},\n{ownershipType:[\"createdBy\"],code:\"test-Resourcce\",channel:\"in.ekstep\",language:[\"English\"],mimeType:\"application/pdf\",idealScreenSize:\"normal\",createdOn:\"2020-05-29T19:36:08.181+0530\",contentDisposition:\"inline\",contentEncoding:\"identity\",lastUpdatedOn:\"2020-05-29T19:36:08.181+0530\",contentType:\"Resource\",primaryCategory:\"Learning Resource\",dialcodeRequired:\"No\",audience:[\"Student\"],lastStatusChangedOn:\"2020-05-29T19:36:08.181+0530\",os:[\"All\"],visibility:\"Default\",IL_SYS_NODE_TYPE:\"DATA_NODE\",mediaType:\"content\",osId:\"org.ekstep.quiz.app\",version:2,versionKey:\"1590761168181\",idealScreenDensity:\"hdpi\",license:\"CC BY 4.0\",framework:\"NCF\",compatibilityLevel:1,IL_FUNC_OBJECT_TYPE:\"Content\",name:\"prad PDF Content-1\",IL_UNIQUE_ID:\"do_11303151546543308811\",status:\"Draft\"},\n{ownershipType:[\"createdBy\"],code:\"test-Resourcce\",channel:\"in.ekstep\",language:[\"English\"],mimeType:\"application/pdf\",idealScreenSize:\"normal\",createdOn:\"2020-05-29T19:39:36.978+0530\",contentDisposition:\"inline\",contentEncoding:\"identity\",lastUpdatedOn:\"2020-05-29T19:39:36.978+0530\",contentType:\"Resource\",primaryCategory:\"Learning Resource\",dialcodeRequired:\"No\",audience:[\"Student\"],lastStatusChangedOn:\"2020-05-29T19:39:36.978+0530\",os:[\"All\"],visibility:\"Default\",IL_SYS_NODE_TYPE:\"DATA_NODE\",mediaType:\"content\",osId:\"org.ekstep.quiz.app\",version:2,versionKey:\"1590761376978\",idealScreenDensity:\"hdpi\",license:\"CC BY 4.0\",framework:\"NCF\",compatibilityLevel:1,IL_FUNC_OBJECT_TYPE:\"Content\",name:\"prad PDF Content-19\",IL_UNIQUE_ID:\"do_113031517200171008119\",status:\"Draft\"},\n{ownershipType:[\"createdBy\"],code:\"test-Resourcce\",channel:\"in.ekstep\",language:[\"English\"],mimeType:\"application/pdf\",idealScreenSize:\"normal\",createdOn:\"2020-05-29T19:37:25.999+0530\",contentDisposition:\"inline\",contentEncoding:\"identity\",lastUpdatedOn:\"2020-05-29T19:37:25.999+0530\",contentType:\"Resource\",primaryCategory:\"Learning Resource\",dialcodeRequired:\"No\",audience:[\"Student\"],lastStatusChangedOn:\"2020-05-29T19:37:25.999+0530\",os:[\"All\"],visibility:\"Default\",IL_SYS_NODE_TYPE:\"DATA_NODE\",mediaType:\"content\",osId:\"org.ekstep.quiz.app\",version:2,versionKey:\"1590761245999\",idealScreenDensity:\"hdpi\",license:\"CC BY 4.0\",framework:\"NCF\",compatibilityLevel:1,IL_FUNC_OBJECT_TYPE:\"Content\",name:\"prad PDF Content-6\",IL_UNIQUE_ID:\"do_11303151612719104016\",status:\"Draft\"},\n{ownershipType:[\"createdBy\"],code:\"test-Resourcce\",channel:\"in.ekstep\",language:[\"English\"],mimeType:\"application/pdf\",idealScreenSize:\"normal\",createdOn:\"2020-05-29T19:37:38.731+0530\",contentDisposition:\"inline\",contentEncoding:\"identity\",lastUpdatedOn:\"2020-05-29T19:37:38.731+0530\",contentType:\"Resource\",primaryCategory:\"Learning Resource\",dialcodeRequired:\"No\",audience:[\"Student\"],lastStatusChangedOn:\"2020-05-29T19:37:38.731+0530\",os:[\"All\"],visibility:\"Default\",IL_SYS_NODE_TYPE:\"DATA_NODE\",mediaType:\"content\",osId:\"org.ekstep.quiz.app\",version:2,versionKey:\"1590761258731\",idealScreenDensity:\"hdpi\",license:\"CC BY 4.0\",framework:\"NCF\",compatibilityLevel:1,IL_FUNC_OBJECT_TYPE:\"Content\",name:\"prad PDF Content-7\",IL_UNIQUE_ID:\"do_11303151623148339217\",status:\"Draft\"},\n{ownershipType:[\"createdBy\"],code:\"test-Resourcce\",channel:\"in.ekstep\",language:[\"English\"],mimeType:\"application/pdf\",idealScreenSize:\"normal\",createdOn:\"2020-05-29T19:37:49.223+0530\",contentDisposition:\"inline\",contentEncoding:\"identity\",lastUpdatedOn:\"2020-05-29T19:37:49.223+0530\",contentType:\"Resource\",primaryCategory:\"Learning Resource\",dialcodeRequired:\"No\",audience:[\"Student\"],lastStatusChangedOn:\"2020-05-29T19:37:49.223+0530\",os:[\"All\"],visibility:\"Default\",IL_SYS_NODE_TYPE:\"DATA_NODE\",mediaType:\"content\",osId:\"org.ekstep.quiz.app\",version:2,versionKey:\"1590761269223\",idealScreenDensity:\"hdpi\",license:\"CC BY 4.0\",framework:\"NCF\",compatibilityLevel:1,IL_FUNC_OBJECT_TYPE:\"Content\",name:\"prad PDF Content-8\",IL_UNIQUE_ID:\"do_11303151631740928018\",status:\"Draft\"},\n{ownershipType:[\"createdBy\"],code:\"test-Resourcce\",channel:\"in.ekstep\",language:[\"English\"],mimeType:\"application/pdf\",idealScreenSize:\"normal\",createdOn:\"2020-05-29T19:38:55.782+0530\",contentDisposition:\"inline\",contentEncoding:\"identity\",lastUpdatedOn:\"2020-05-29T19:38:55.782+0530\",contentType:\"Resource\",primaryCategory:\"Learning Resource\",dialcodeRequired:\"No\",audience:[\"Student\"],lastStatusChangedOn:\"2020-05-29T19:38:55.782+0530\",os:[\"All\"],visibility:\"Default\",IL_SYS_NODE_TYPE:\"DATA_NODE\",mediaType:\"content\",osId:\"org.ekstep.quiz.app\",version:2,versionKey:\"1590761335782\",idealScreenDensity:\"hdpi\",license:\"CC BY 4.0\",framework:\"NCF\",compatibilityLevel:1,IL_FUNC_OBJECT_TYPE:\"Content\",name:\"prad PDF Content-15\",IL_UNIQUE_ID:\"do_113031516862660608115\",status:\"Draft\"}" +
            ",{owner:\"in.ekstep\",code:\"NCF\",IL_SYS_NODE_TYPE:\"DATA_NODE\",apoc_json:\"{\\\"batch\\\": true}\",consumerId:\"9393568c-3a56-47dd-a9a3-34da3c821638\",channel:\"in.ekstep\",description:\"NCF \",type:\"K-12\",createdOn:\"2018-01-23T09:53:50.189+0000\",versionKey:\"1545195552163\",apoc_text:\"APOC\",appId:\"dev.sunbird.portal\",IL_FUNC_OBJECT_TYPE:\"Framework\",name:\"State (Uttar Pradesh)\",lastUpdatedOn:\"2018-12-19T04:59:12.163+0000\",IL_UNIQUE_ID:\"NCF\",status:\"Live\",apoc_num:1}" +
            ",{owner:\"in.ekstep\",code:\"K-12\",IL_SYS_NODE_TYPE:\"DATA_NODE\",apoc_json:\"{\\\"batch\\\": true}\",consumerId:\"9393568c-3a56-47dd-a9a3-34da3c821638\",channel:\"in.ekstep\",description:\"NCF \",type:\"K-12\",createdOn:\"2018-01-23T09:53:50.189+0000\",versionKey:\"1545195552163\",apoc_text:\"APOC\",appId:\"dev.sunbird.portal\",IL_FUNC_OBJECT_TYPE:\"Framework\",name:\"State (Uttar Pradesh)\",lastUpdatedOn:\"2018-12-19T04:59:12.163+0000\",IL_UNIQUE_ID:\"K-12\",status:\"Live\",apoc_num:1}" +
            ",{owner:\"in.ekstep\",code:\"tpd\",IL_SYS_NODE_TYPE:\"DATA_NODE\",apoc_json:\"{\\\"batch\\\": true}\",consumerId:\"9393568c-3a56-47dd-a9a3-34da3c821638\",channel:\"in.ekstep\",description:\"NCF \",type:\"K-12\",createdOn:\"2018-01-23T09:53:50.189+0000\",versionKey:\"1545195552163\",apoc_text:\"APOC\",appId:\"dev.sunbird.portal\",IL_FUNC_OBJECT_TYPE:\"Framework\",name:\"State (Uttar Pradesh)\",lastUpdatedOn:\"2018-12-19T04:59:12.163+0000\",IL_UNIQUE_ID:\"tpd\",status:\"Live\",apoc_num:1}] as row CREATE (n:domain) SET n += row;");
    }
}
