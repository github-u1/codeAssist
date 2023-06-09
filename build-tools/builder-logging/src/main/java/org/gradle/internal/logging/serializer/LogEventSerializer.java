package org.gradle.internal.logging.serializer;

import org.gradle.api.logging.LogLevel;
import org.gradle.internal.logging.events.LogEvent;
import org.gradle.internal.operations.OperationIdentifier;
import org.gradle.internal.serialize.Decoder;
import org.gradle.internal.serialize.Encoder;
import org.gradle.internal.serialize.Serializer;

public class LogEventSerializer implements Serializer<LogEvent> {
    private final Serializer<Throwable> throwableSerializer;
    private final Serializer<LogLevel> logLevelSerializer;

    public LogEventSerializer(Serializer<LogLevel> logLevelSerializer, Serializer<Throwable> throwableSerializer) {
        this.logLevelSerializer = logLevelSerializer;
        this.throwableSerializer = throwableSerializer;
    }

    @Override
    public void write(Encoder encoder, LogEvent event) throws Exception {
        encoder.writeLong(event.getTimestamp());
        encoder.writeString(event.getCategory());
        logLevelSerializer.write(encoder, event.getLogLevel());
        encoder.writeNullableString(event.getMessage());
        throwableSerializer.write(encoder, event.getThrowable());
        if (event.getBuildOperationId() == null) {
            encoder.writeBoolean(false);
        } else {
            encoder.writeBoolean(true);
            encoder.writeSmallLong(event.getBuildOperationId().getId());
        }
    }

    @Override
    public LogEvent read(Decoder decoder) throws Exception {
        long timestamp = decoder.readLong();
        String category = decoder.readString();
        LogLevel logLevel = logLevelSerializer.read(decoder);
        String message = decoder.readNullableString();
        Throwable throwable = throwableSerializer.read(decoder);
        OperationIdentifier buildOperationId = decoder.readBoolean() ? new OperationIdentifier(decoder.readSmallLong()) : null;
        return new LogEvent(timestamp, category, logLevel, message, throwable, buildOperationId);
    }
}
