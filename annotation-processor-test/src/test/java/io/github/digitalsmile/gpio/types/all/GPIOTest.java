package io.github.digitalsmile.gpio.types.all;

import io.github.digitalsmile.gpio.types.all.structs.*;
import org.junit.jupiter.api.Test;

import java.lang.foreign.Arena;
import java.nio.ByteBuffer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class GPIOTest {

    private static final byte[] STRING = "Test".getBytes();
    private static final byte[] BYTE_ARRAY = new byte[]{0x01, 0x02, 0x03};
    private static final int[] INT_ARRAY = new int[]{1, 2, 3};

    @Test
    public void testGPIOV1AllTypes() {
        try (var arena = Arena.ofConfined()) {
            var chipInfo = new GpiochipInfo(STRING, STRING, 3);
            var memoryBuffer = arena.allocate(chipInfo.getMemoryLayout());
            chipInfo.toBytes(memoryBuffer);
            var newChipInfo = GpiochipInfo.createEmpty().fromBytes(memoryBuffer);
            assertArrayEquals(newChipInfo.name(), prepareByteArray(32, STRING));
            assertArrayEquals(newChipInfo.label(), prepareByteArray(32, STRING));
            assertEquals(3, newChipInfo.lines());
            assertTrue(GpiochipInfo.createEmpty().isEmpty());

            assertEquals(40, GpioConstants.class.getFields().length);

            //assertEquals(GpioConstants.values().length, 3);

            var eventData = new GpioeventData(100, 5);
            memoryBuffer = arena.allocate(eventData.getMemoryLayout());
            eventData.toBytes(memoryBuffer);
            var newEventData = GpioeventData.createEmpty().fromBytes(memoryBuffer);
            assertEquals(100, newEventData.timestamp());
            assertEquals(5, newEventData.id());
            assertTrue(GpioeventData.createEmpty().isEmpty());

            var eventRequest = new GpioeventRequest(1, 2, 3, STRING, 4);
            memoryBuffer = arena.allocate(eventRequest.getMemoryLayout());
            eventRequest.toBytes(memoryBuffer);
            var newEventRequest = GpioeventRequest.createEmpty().fromBytes(memoryBuffer);
            assertEquals(1, newEventRequest.lineoffset());
            assertEquals(2, newEventRequest.handleflags());
            assertEquals(3, newEventRequest.eventflags());
            assertArrayEquals(newEventRequest.consumerLabel(), prepareByteArray(32, STRING));
            assertEquals(4, newEventRequest.fd());
            assertTrue(GpioeventRequest.createEmpty().isEmpty());

            var handleConfig = new GpiohandleConfig(1, BYTE_ARRAY, INT_ARRAY);
            memoryBuffer = arena.allocate(handleConfig.getMemoryLayout());
            handleConfig.toBytes(memoryBuffer);
            var newHandleConfig = GpiohandleConfig.createEmpty().fromBytes(memoryBuffer);
            assertEquals(1, newHandleConfig.flags());
            assertArrayEquals(newHandleConfig.defaultValues(), prepareByteArray(64, BYTE_ARRAY));
            assertArrayEquals(newHandleConfig.padding(), prepareIntArray(4, INT_ARRAY));
            assertTrue(GpiohandleConfig.createEmpty().isEmpty());

            var handleData = new GpiohandleData(BYTE_ARRAY);
            memoryBuffer = arena.allocate(handleData.getMemoryLayout());
            handleData.toBytes(memoryBuffer);
            var newHandleData = GpiohandleData.createEmpty().fromBytes(memoryBuffer);
            assertArrayEquals(newHandleData.values(), prepareByteArray(64, BYTE_ARRAY));
            assertTrue(GpiohandleData.createEmpty().isEmpty());

            var handleRequest = new GpiohandleRequest(INT_ARRAY, 1, BYTE_ARRAY, STRING, 2, 3);
            memoryBuffer = arena.allocate(handleRequest.getMemoryLayout());
            handleRequest.toBytes(memoryBuffer);
            var newHandleRequest = GpiohandleRequest.createEmpty().fromBytes(memoryBuffer);
            assertArrayEquals(newHandleRequest.lineoffsets(), prepareIntArray(64, INT_ARRAY));
            assertEquals(1, newHandleRequest.flags());
            assertArrayEquals(newHandleRequest.defaultValues(), prepareByteArray(64, BYTE_ARRAY));
            assertArrayEquals(newHandleRequest.consumerLabel(), prepareByteArray(32, STRING));
            assertEquals(2, newHandleRequest.lines());
            assertEquals(3, newHandleRequest.fd());
            assertTrue(GpiohandleRequest.createEmpty().isEmpty());

            var lineInfo = new GpiolineInfo(1, 2, STRING, STRING);
            memoryBuffer = arena.allocate(lineInfo.getMemoryLayout());
            lineInfo.toBytes(memoryBuffer);
            var newLineInfo = GpiolineInfo.createEmpty().fromBytes(memoryBuffer);
            assertEquals(1, newLineInfo.lineOffset());
            assertEquals(2, newLineInfo.flags());
            assertArrayEquals(newLineInfo.name(), prepareByteArray(32, STRING));
            assertArrayEquals(newLineInfo.consumer(), prepareByteArray(32, STRING));
            assertTrue(GpiolineInfo.createEmpty().isEmpty());

            var lineInfoChanged = new GpiolineInfoChanged(newLineInfo, 1, 2, INT_ARRAY);
            memoryBuffer = arena.allocate(lineInfoChanged.getMemoryLayout());
            lineInfoChanged.toBytes(memoryBuffer);
            var newLineInfoChanged = GpiolineInfoChanged.createEmpty().fromBytes(memoryBuffer);
            assertThat(newLineInfoChanged.info()).usingRecursiveComparison().isEqualTo(newLineInfo);
            assertEquals(1, newLineInfoChanged.timestamp());
            assertEquals(2, newLineInfoChanged.eventType());
            assertArrayEquals(newLineInfoChanged.padding(), prepareIntArray(5, INT_ARRAY));
            assertTrue(GpiolineInfoChanged.createEmpty().isEmpty());
        } catch (Throwable e) {
            fail(e);
        }

    }

    private byte[] prepareByteArray(int size, byte... values) {
        return ByteBuffer.allocate(size).put(values).array();
    }

    private int[] prepareIntArray(int size, int... values) {
        var array = new int[size];
        System.arraycopy(values, 0, array, 0, values.length);
        return array;
    }

}
