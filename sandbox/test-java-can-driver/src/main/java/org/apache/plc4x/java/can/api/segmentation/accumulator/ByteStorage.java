package org.apache.plc4x.java.can.api.segmentation.accumulator;

import org.apache.plc4x.java.canopen.readwrite.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class ByteStorage<T> implements Storage<T, byte[]> {

    private final List<byte[]> segments = new ArrayList<>();
    private final Function<T, byte[]> extractor;
    private long size = 0;

    public ByteStorage(Function<T, byte[]> extractor) {
        this.extractor = extractor;
    }

    @Override
    public void append(T frame) {
        segments.add(extractor.apply(frame));
        size += segments.get(segments.size() - 1).length;
    }

    public long size() {
        return size;
    }

    @Override
    public byte[] get() {
        Optional<byte[]> collect = segments.stream().reduce((b1, b2) -> {
            byte[] combined = new byte[b1.length + b2.length];
            System.arraycopy(b1, 0, combined, 0, b1.length);
            System.arraycopy(b2, 0, combined, b1.length, b2.length);
            return combined;
        });
        return collect.orElse(new byte[0]);
    }

    public static class SDOUploadStorage extends ByteStorage<SDOResponse> {
        public SDOUploadStorage() {
            super((sdoResponse -> {
                if (sdoResponse instanceof SDOSegmentUploadResponse) {
                    return ((SDOSegmentUploadResponse) sdoResponse).getData();
                }
                if (sdoResponse instanceof SDOInitiateUploadResponse) {
                    SDOInitiateUploadResponse initiate = (SDOInitiateUploadResponse) sdoResponse;

                    if (initiate.getPayload() instanceof SDOInitiateExpeditedUploadResponse) {
                        return ((SDOInitiateExpeditedUploadResponse) initiate.getPayload()).getData();
                    }
                }
                return new byte[0];
            }));
        }
    }

    public static class SDODownloadStorage extends ByteStorage<SDORequest> {
        public SDODownloadStorage() {
            super((sdoRequest -> {
                if (sdoRequest instanceof SDOSegmentDownloadRequest) {
                    return ((SDOSegmentDownloadRequest) sdoRequest).getData();
                }
                if (sdoRequest instanceof  SDOInitiateDownloadRequest) {
                    SDOInitiateDownloadRequest initiate = (SDOInitiateDownloadRequest) sdoRequest;

                    if (initiate.getPayload() instanceof SDOInitiateExpeditedUploadResponse) {
                        return ((SDOInitiateExpeditedUploadResponse) initiate.getPayload()).getData();
                    }
                }
                return new byte[0];
            }));
        }
    }

}
