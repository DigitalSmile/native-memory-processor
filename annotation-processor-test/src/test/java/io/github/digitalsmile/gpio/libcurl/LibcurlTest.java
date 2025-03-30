package io.github.digitalsmile.gpio.libcurl;

import io.github.digitalsmile.annotation.NativeMemoryException;
import io.github.digitalsmile.gpio.libcurl.enums.CURLCode;
import io.github.digitalsmile.gpio.libcurl.enums.CURLOption;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LibcurlTest {

    @Test
    public void libcurl() throws NativeMemoryException {
        try (var libcurl = new LibcurlNative()) {
            var code = libcurl.globalInit(CurlConstants.CURL_GLOBAL_DEFAULT);
            assertEquals(CURLCode.CURLE_OK, code);
            var curl = libcurl.easyInit();
            code = libcurl.easySetOpt(curl, CURLOption.CURLOPT_URL, "https://example.com");
            assertEquals(CURLCode.CURLE_OK, code);
            code = libcurl.easySetOpt(curl, CURLOption.CURLOPT_FOLLOWLOCATION, 1L);
            assertEquals(CURLCode.CURLE_OK, code);

            code = libcurl.easyPerform(curl);
            assertEquals(CURLCode.CURLE_OK, code);
        }
    }
}
