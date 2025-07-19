package com.amberclient.utils.blaze3d;

import com.amberclient.utils.blaze3d.buffers.GpuBuffer;
import com.amberclient.utils.blaze3d.buffers.GpuBufferSlice;
import com.amberclient.utils.blaze3d.buffers.MappableRingBuffer;
import com.amberclient.utils.blaze3d.systems.GpuDevice;
import com.amberclient.utils.blaze3d.systems.DeviceManager;
import com.mojang.logging.LogUtils;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class DynamicUniformStorage<T extends DynamicUniformStorage.Uploadable> implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final List<MappableRingBuffer> oldBuffers = new ArrayList();
    private final int blockSize;
    private MappableRingBuffer buffer;
    private int size;
    private int capacity;
    @Nullable
    private T lastWrittenValue;
    private final String name;

    public DynamicUniformStorage(String name, int blockSize, int capacity) {
        GpuDevice gpuDevice = DeviceManager.getRequiredDevice();
        this.blockSize = MathHelper.roundUpToMultiple(blockSize, gpuDevice.getUniformOffsetAlignment());
        this.capacity = MathHelper.smallestEncompassingPowerOfTwo(capacity);
        this.size = 0;
        this.buffer = new MappableRingBuffer(() -> name + " x" + this.blockSize, 130, this.blockSize * this.capacity);
        this.name = name;
    }

    public void clear() {
        this.size = 0;
        this.lastWrittenValue = null;
        this.buffer.rotate();
        if (!this.oldBuffers.isEmpty()) {
            for (MappableRingBuffer mappableRingBuffer : this.oldBuffers) {
                mappableRingBuffer.close();
            }

            this.oldBuffers.clear();
        }
    }

    private void growBuffer(int capacity) {
        this.capacity = capacity;
        this.size = 0;
        this.lastWrittenValue = null;
        this.oldBuffers.add(this.buffer);
        this.buffer = new MappableRingBuffer(() -> this.name + " x" + this.blockSize, 130, this.blockSize * this.capacity);
    }

    public GpuBufferSlice write(T value) {
        if (this.lastWrittenValue != null && this.lastWrittenValue.equals(value)) {
            return this.buffer.getBlocking().slice((this.size - 1) * this.blockSize, this.blockSize);
        } else {
            if (this.size >= this.capacity) {
                int i = this.capacity * 2;
                LOGGER.info("Resizing " + this.name + ", capacity limit of {} reached during a single frame. New capacity will be {}.", this.capacity, i);
                this.growBuffer(i);
            }

            int i = this.size * this.blockSize;

            try (GpuBuffer.MappedView mappedView = DeviceManager.getRequiredDevice()
                    .createCommandEncoder()
                    .mapBuffer(this.buffer.getBlocking().slice(i, this.blockSize), false, true)) {
                value.write(mappedView.data());
            }

            this.size++;
            this.lastWrittenValue = value;
            return this.buffer.getBlocking().slice(i, this.blockSize);
        }
    }

    public GpuBufferSlice[] writeAll(T[] values) {
        if (values.length == 0) {
            return new GpuBufferSlice[0];
        } else {
            if (this.size + values.length > this.capacity) {
                int i = MathHelper.smallestEncompassingPowerOfTwo(Math.max(this.capacity + 1, values.length));
                LOGGER.info("Resizing " + this.name + ", capacity limit of {} reached during a single frame. New capacity will be {}.", this.capacity, i);
                this.growBuffer(i);
            }

            int i = this.size * this.blockSize;
            GpuBufferSlice[] gpuBufferSlices = new GpuBufferSlice[values.length];

            try (GpuBuffer.MappedView mappedView = DeviceManager.getRequiredDevice()
                    .createCommandEncoder()
                    .mapBuffer(this.buffer.getBlocking().slice(i, values.length * this.blockSize), false, true)) {
                ByteBuffer byteBuffer = mappedView.data();

                for (int j = 0; j < values.length; j++) {
                    T uploadable = values[j];
                    gpuBufferSlices[j] = this.buffer.getBlocking().slice(i + j * this.blockSize, this.blockSize);
                    byteBuffer.position(j * this.blockSize);
                    uploadable.write(byteBuffer);
                }
            }

            this.size += values.length;
            this.lastWrittenValue = values[values.length - 1];
            return gpuBufferSlices;
        }
    }

    public void close() {
        for (MappableRingBuffer mappableRingBuffer : this.oldBuffers) {
            mappableRingBuffer.close();
        }

        this.buffer.close();
    }

    @Environment(EnvType.CLIENT)
    public interface Uploadable {
        void write(ByteBuffer buffer);
    }
}