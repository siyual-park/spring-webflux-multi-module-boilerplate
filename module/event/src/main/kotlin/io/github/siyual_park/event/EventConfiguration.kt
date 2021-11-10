package io.github.siyual_park.event

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Configuration

@Configuration
class EventConfiguration(
    private val applicationContext: ApplicationContext
) {
    @Autowired(required = true)
    fun configEventEmitter(eventEmitter: EventEmitter) {
        applicationContext.getBeansOfType(EventListener::class.java).values.forEach {
            it.javaClass.annotations.filter { it is Subscribe }
                .forEach { annotation ->
                    if (annotation !is Subscribe) return@forEach
                    eventEmitter.on(annotation.type, it)
                }
        }
    }
}
