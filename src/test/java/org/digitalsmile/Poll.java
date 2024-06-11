package org.digitalsmile;



@Native(header = "poll.h")
@Structs({
        @Struct(name = "pollfd", javaName = "PollingFileDescriptor")
})
public interface Poll {
    @Function(name = "poll", parameters = {
            @Parameter(name = "descriptor", pointerType = "BY_VALUE")
    })
    PollingFileDescriptor poll(PollingFileDescriptor descriptor, int size, int timeout);
}