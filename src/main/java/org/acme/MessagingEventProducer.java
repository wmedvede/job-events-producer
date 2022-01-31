package org.acme;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.kie.kogito.jobs.api.event.JobCloudEvent;
import org.kie.kogito.jobs.api.event.serialization.JobCloudEventSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class MessagingEventProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessagingEventProducer.class);

    private static final String KOGITO_JOB_SERVICE_JOB_ACTION_EVENTS = "kogito-job-service-job-action-events";

    private JobCloudEventSerializer serializer;

    @PostConstruct
    void setUp() {
        this.serializer = new JobCloudEventSerializer();
    }

    @Inject
    @Channel(KOGITO_JOB_SERVICE_JOB_ACTION_EVENTS)
    Emitter<String> eventsEmitter;
    public void emitEvent(JobCloudEvent<?> event) {

        if (eventsEmitter.hasRequests()) {
            LOGGER.debug("Emitter {} is not ready to send messages", KOGITO_JOB_SERVICE_JOB_ACTION_EVENTS);
        }

        LOGGER.debug("About to publish event {} to topic {}", event, KOGITO_JOB_SERVICE_JOB_ACTION_EVENTS);
        try {
            String json = serializer.serialize(event);
            LOGGER.trace("json value: {}", json);
            eventsEmitter.send(json);
            LOGGER.trace("Successfully published event {} to topic {}", event, KOGITO_JOB_SERVICE_JOB_ACTION_EVENTS);
        } catch (Exception e) {
            LOGGER.error("Error while publishing event to topic {} for event {}", KOGITO_JOB_SERVICE_JOB_ACTION_EVENTS, event, e);
        }
    }
}
