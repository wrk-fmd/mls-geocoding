package at.wrk.fmd.mls.geocoding.service;

import static java.util.Objects.requireNonNull;

import at.wrk.fmd.mls.geocoding.api.dto.GeocodingRequest;
import at.wrk.fmd.mls.geocoding.api.dto.GeocodingResult;
import at.wrk.fmd.mls.geocoding.api.dto.LatLng;
import at.wrk.fmd.mls.geocoding.api.queues.GeocodingQueueNames;
import at.wrk.fmd.mls.geocoding.config.GeocoderProperties;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class GeocodingRequestListener {

    private final Geocoder geocoder;
    private final GeocoderProperties properties;
    private final AmqpTemplate amqpTemplate;

    @Autowired
    public GeocodingRequestListener(Geocoder geocoder, GeocoderProperties properties, AmqpTemplate amqpTemplate) {
        this.geocoder = requireNonNull(geocoder, "The Geocoder implementation must not be null");
        this.properties = requireNonNull(properties, "GeocoderProperties must not be null");
        this.amqpTemplate = requireNonNull(amqpTemplate, "AmqpTemplate must not be null");
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue,
            exchange = @Exchange(value = GeocodingQueueNames.GEOCODE_REQUESTS),
            key = "${application.geocoder.priority}"
    ))
    public void handleGeocodingRequest(@Payload GeocodingRequest request, Message message) {
        MessageProperties properties = message.getMessageProperties();
        if (properties.getReplyTo() != null) {
            GeocodingResult result = geocoder.geocode(request);
            send(GeocodingQueueNames.GEOCODE_REQUESTS, request, result, properties.getReplyTo(), properties.getCorrelationId());
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue,
            exchange = @Exchange(value = GeocodingQueueNames.GEOCODE_REVERSE),
            key = "${application.geocoder.priority}"
    ))
    public void handleReverseRequest(@Payload LatLng request, Message message) {
        MessageProperties properties = message.getMessageProperties();
        if (properties.getReplyTo() != null) {
            GeocodingResult result = geocoder.reverse(request);
            send(GeocodingQueueNames.GEOCODE_REVERSE, request, result, properties.getReplyTo(), properties.getCorrelationId());
        }
    }

    private <T> void send(String queue, T request, GeocodingResult result, String replyTo, String correlationId) {
        if (result != null) {
            amqpTemplate.convertAndSend(null, replyTo, result, m -> {
                m.getMessageProperties().setCorrelationId(correlationId);
                return m;
            });
        } else if (properties.isRequeue()) {
            String level = String.valueOf(properties.getPriority() == null ? 1 : properties.getPriority() + 1);
            amqpTemplate.convertAndSend(queue, level, request, m -> {
                m.getMessageProperties().setCorrelationId(correlationId);
                m.getMessageProperties().setReplyTo(replyTo);
                return m;
            });
        }
    }
}
