package io.github.siyual_park.event

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Configuration

@Configuration
class EventConfiguration(
    private val applicationContext: ApplicationContext,
    private val eventFilterFactory: EventFilterFactory
) {
    @Autowired(required = true)
    fun configEventEmitter(eventEmitter: EventEmitter) {
        applicationContext.getBeansOfType(EventConsumer::class.java).values.forEach {
            it.javaClass.annotations.filterIsInstance<Subscribe>()
                .forEach { annotation ->
                    val filter = eventFilterFactory.create(annotation)
                    eventEmitter.on(filter, it)
                }
        }
    }
}
