package modules

import com.google.inject.AbstractModule
import org.sunbird.actors.{FrameworkActor, FrameworkCategoryActor, FrameworkTermActor, HealthActor}
import play.libs.akka.AkkaGuiceSupport
import utils.ActorNames

class TaxonomyModule extends AbstractModule with AkkaGuiceSupport {

    override def configure() = {
        super.configure()
        bindActor(classOf[HealthActor], ActorNames.HEALTH_ACTOR)
        bindActor(classOf[FrameworkActor], ActorNames.FRAMEWORK_ACTOR)
        bindActor(classOf[FrameworkCategoryActor], ActorNames.FRAMEWORK_CATEGORY_ACTOR)
        bindActor(classOf[FrameworkTermActor], ActorNames.FRAMEWORK_TERM_ACTOR)
        println("Initialized application actors for assessment-service")
    }
}
