package org.gradle.process.internal.worker.messaging;

import org.gradle.api.Action;
import org.gradle.api.logging.LogLevel;
import org.gradle.internal.io.ClassLoaderObjectInputStream;
import org.gradle.internal.remote.internal.inet.MultiChoiceAddress;
import org.gradle.internal.remote.internal.inet.MultiChoiceAddressSerializer;
import org.gradle.internal.serialize.Decoder;
import org.gradle.internal.serialize.Encoder;
import org.gradle.internal.serialize.Serializer;
import org.gradle.process.internal.worker.WorkerProcessContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Serializes and de-serializes {@link WorkerConfig}s.
 */
public class WorkerConfigSerializer implements Serializer<WorkerConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkerConfigSerializer.class);

    @Override
    public WorkerConfig read(Decoder decoder) throws IOException {
        LogLevel logLevel = LogLevel.values()[decoder.readSmallInt()];
        boolean shouldPublishJvmMemoryInfo = decoder.readBoolean();
        String gradleUserHomeDirPath = decoder.readString();
        MultiChoiceAddress serverAddress = new MultiChoiceAddressSerializer().read(decoder);
        final long workerId = decoder.readSmallLong();
        final String displayName = decoder.readString();
        Action<? super WorkerProcessContext> workerAction = deserializeWorker(decoder.readBinary(), getClass().getClassLoader());

        return new WorkerConfig(logLevel, shouldPublishJvmMemoryInfo, gradleUserHomeDirPath, serverAddress, workerId, displayName, workerAction);
    }

    @Override
    public void write(Encoder encoder, WorkerConfig config) throws IOException {
        encoder.writeSmallInt(config.getLogLevel().ordinal());
        encoder.writeBoolean(config.shouldPublishJvmMemoryInfo());
        encoder.writeString(config.getGradleUserHomeDirPath());
        new MultiChoiceAddressSerializer().write(encoder, config.getServerAddress());
        encoder.writeSmallLong(config.getWorkerId());
        encoder.writeString(config.getDisplayName());
        encoder.writeBinary(serializeWorker(config.getWorkerAction()));
    }

    private static Action<? super WorkerProcessContext> deserializeWorker(byte[] serializedWorker, ClassLoader loader) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(serializedWorker);
        ObjectInputStream in = null;
        try {
            in = new ClassLoaderObjectInputStream(bais, loader);

            @SuppressWarnings("unchecked")
            Action<? super WorkerProcessContext> workerAction = (Action<? super WorkerProcessContext>) in.readObject();
            return workerAction;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot load worker action's class", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    LOGGER.debug("Error closing ObjectInputStream", e);
                }
            }
        }
    }

    private static byte[] serializeWorker(Action<? super WorkerProcessContext> action) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(baos);
            out.writeObject(action);

            return baos.toByteArray();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    LOGGER.debug("Error closing ObjectOutputStream", e);
                }
            }
        }
    }
}