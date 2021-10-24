package at.wrk.fmd.mls.geocoding.service;

import static java.util.Objects.requireNonNull;

import at.wrk.fmd.mls.geocoding.api.dto.GeocodingResult;
import at.wrk.fmd.mls.geocoding.api.dto.LatLng;
import at.wrk.fmd.mls.geocoding.api.dto.PointDto;
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

import java.util.Collection;

@Component
public class GeocodingRequestListener {

    private final Geocoder geocoder;
    private final GeocoderProperties geocoderProperties;
    private final AmqpTemplate amqpTemplate;

    @Autowired
    public GeocodingRequestListener(Geocoder geocoder, GeocoderProperties geocoderProperties, AmqpTemplate amqpTemplate) {
        this.geocoder = requireNonNull(geocoder, "The Geocoder implementation must not be null");
        this.geocoderProperties = requireNonNull(geocoderProperties, "GeocoderProperties must not be null");
        this.amqpTemplate = requireNonNull(amqpTemplate, "AmqpTemplate must not be null");
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue,
            exchange = @Exchange(value = GeocodingQueueNames.GEOCODE_REQUESTS)
    ))
    public void handleGeocodingRequest(@Payload PointDto request, Message message) {
        MessageProperties properties = message.getMessageProperties();
        if (properties.getReplyTo() == null) {
            // No reply-to given, ignore request
            return;
        }

        Collection<PointDto> results = geocoder.geocode(request);
        if (results == null) {
            // No matching results found
            return;
        }

        // Send all the results to the reply-to target with the given correlation id
        results.forEach(point -> send(point, properties.getReplyTo(), properties.getCorrelationId()));
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue,
            exchange = @Exchange(value = GeocodingQueueNames.GEOCODE_REVERSE)
    ))
    public void handleReverseRequest(@Payload LatLng request, Message message) {
        MessageProperties properties = message.getMessageProperties();
        if (properties.getReplyTo() == null) {
            // No reply-to given, ignore request
            return;
        }

        PointDto result = geocoder.reverse(request);
        if (result == null) {
            // No matching result found
            return;
        }

        // Send the result to the reply-to target with the given correlation id
        send(result, properties.getReplyTo(), properties.getCorrelationId());
    }

    private void send(PointDto point, String replyTo, String correlationId) {
        GeocodingResult result = new GeocodingResult(point, geocoderProperties.getName(), geocoderProperties.getPriority());
        amqpTemplate.convertAndSend(null, replyTo, result, m -> {
            // Set the correlation id in the message
            m.getMessageProperties().setCorrelationId(correlationId);
            return m;
        });
    }
}
