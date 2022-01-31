package org.acme;

import java.net.URI;
import java.net.URL;
import java.time.ZonedDateTime;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.kie.kogito.jobs.api.Job;
import org.kie.kogito.jobs.api.URIBuilder;
import org.kie.kogito.jobs.api.event.CancelJobRequestEvent;
import org.kie.kogito.jobs.api.event.CreateProcessInstanceJobRequestEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/management/jobs")
public class MessagingResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessagingResource.class);

    @Inject
    MessagingEventProducer eventProducer;

    @Inject
    @ConfigProperty(name = "service.url")
    URI serviceURL;

    @Path("/createJobRequest")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createJobRequest(@Context HttpHeaders httpHeaders, @Context UriInfo uriInfo,
                                     @QueryParam("processId") String processId,
                                     @QueryParam("processInstanceId") String processInstanceId,
                                     @QueryParam("nodeInstanceId") String nodeInstanceId,
                                     @QueryParam("expirationTime") String expirationTime,
                                     @QueryParam("repeatInterval") long repeatInterval,
                                     @QueryParam("repeatLimit") int repeatLimit,
                                     @QueryParam("timerId") int timerId) {

        String jobId = Integer.toString(timerId);
        ZonedDateTime expiration = ZonedDateTime.parse(expirationTime);
        String callback = getCallbackEndpoint(processId, processInstanceId, nodeInstanceId);

        CreateProcessInstanceJobRequestEvent toSend = CreateProcessInstanceJobRequestEvent.builder()
                .source(serviceURL)
                .job(new Job(jobId,
                             expiration,
                             0,
                             callback,
                             processInstanceId,
                             "rootProcessInstanceId",
                             processId,
                             "rootProcessId",
                             repeatInterval,
                             repeatLimit,
                             nodeInstanceId))
                .processInstanceId(processInstanceId)
                .processId(processId)
                .rootProcessInstanceId("rootProcessInstanceId")
                .rootProcessId("rootProcessId")
                .kogitoAddons("experimental-addon")
                .build();
        eventProducer.emitEvent(toSend);
        return Response.ok().build();
    }

    public String getCallbackEndpoint(String processId, String processInstanceId, String nodeId) {
        String callbackEndpoint = serviceURL.toString();
        return URIBuilder.toURI(callbackEndpoint
                                        + "/management/jobs/"
                                        + processId
                                        + "/instances/"
                                        + processInstanceId
                                        + "/timers/"
                                        + nodeId)
                .toString();
    }

    @POST
    @Path("{processId}/instances/{processInstanceId}/timers/{timerId}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response triggerTimer(@PathParam("processId") String processId,
                                 @PathParam("processInstanceId") String processInstanceId,
                                 @PathParam("timerId") String timerId,
                                 @QueryParam("limit") @DefaultValue("0") Integer limit) {

        LOGGER.debug("triggerTimer( processId: {}, processInstanceId: {}, timerId: {}, limit: {})", processId, processInstanceId, timerId, limit);

        if (processInstanceId != null && processInstanceId.contains("fail")) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                                   "An error was produced while executing processInstanceId: " + processInstanceId)
                    .build();
        }
        return Response.status(Response.Status.OK).build();
    }

    @Path("/createCancelJobRequest")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createCancelJobRequest(@Context HttpHeaders httpHeaders, @Context UriInfo uriInfo,
                                           @QueryParam("processId") String processId,
                                           @QueryParam("processInstanceId") String processInstanceId,
                                           @QueryParam("nodeInstanceId") String nodeInstanceId,
                                           @QueryParam("jobId") int jobId) {
        String jobIdStr = Integer.toString(jobId);
        CancelJobRequestEvent toSend = CancelJobRequestEvent.builder()
                .source(serviceURL)
                .jobId(jobIdStr)
                .processInstanceId(processInstanceId)
                .processId(processId)
                .rootProcessInstanceId("rootProcessInstanceId")
                .rootProcessId("rootProcessId")
                .kogitoAddons("experimental-addon")
                .build();
        eventProducer.emitEvent(toSend);
        return Response.ok().build();
    }
}
