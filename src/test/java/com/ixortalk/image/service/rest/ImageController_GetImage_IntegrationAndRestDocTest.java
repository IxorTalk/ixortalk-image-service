/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2016-present IxorTalk CVBA
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.ixortalk.image.service.rest;

import com.ixortalk.image.service.AbstractSpringIntegrationTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.restdocs.request.RequestParametersSnippet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static com.ixortalk.test.oauth2.OAuth2TestTokens.adminToken;
import static com.ixortalk.test.oauth2.OAuth2TestTokens.userToken;
import static com.jayway.restassured.RestAssured.given;
import static java.net.HttpURLConnection.*;
import static java.util.UUID.randomUUID;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.restdocs.restassured.RestAssuredRestDocumentation.document;

public class ImageController_GetImage_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

    private static final String TEST_KEY = "the/key";
    private String location = TEST_KEY + "/" + randomUUID() + "/original";
    private byte[] originalImageBytes;

    private final String FILE_NAME = "test-images/original.png";
    private final String path = "src/test/resources";

    private RequestParametersSnippet REQUEST_PARAMETERS = requestParameters(
            parameterWithName("path").description("The actual path to the stored original image")
    );

    @Before
    public void before() throws IOException {
        originalImageBytes = toByteArray(getClass().getClassLoader().getResourceAsStream(FILE_NAME));

        s3Client.putObject(ixorTalkConfigProperties.getBucket(), location, new File(path +"/" + FILE_NAME));
        Mockito.when(awsS3Template.get(ixorTalkConfigProperties.getBucket(), location))
                .thenReturn(s3Client.getObject(ixorTalkConfigProperties.getBucket(), location));
    }

    @Test
    public void success() throws IOException {
        InputStream inputStream = given()
                .auth().preemptive().oauth2(adminToken().getValue())
                .filter(
                        document("images/get",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint(), removeBinaryContent()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                REQUEST_PARAMETERS
                        )
                )
                .when()
                .get("/image?path=" + location)
                .then()
                .statusCode(HTTP_OK).extract().asInputStream();

        verify(awsS3Template)
                .get(
                        eq(ixorTalkConfigProperties.getBucket()),
                        eq(location)
                );
        verifyNoMoreInteractions(awsS3Template);

        assertThat(toByteArray(inputStream)).isEqualTo(originalImageBytes);
    }

    @Test
    public void wrongKey() {
        given()
                .auth().preemptive().oauth2(adminToken().getValue())
                .when()
                .filter(
                        document("images/wrong-key",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                REQUEST_PARAMETERS
                        )
                )
                .get("/image?path=" + location + location)
                .then()
                .statusCode(HTTP_BAD_REQUEST);
    }

    @Test
    public void unauthorizedUser() {
        given()
                .auth().preemptive().oauth2(userToken().getValue())
                .filter(
                        document("images/as-user",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestHeaders(describeAuthorizationTokenHeader()),
                                REQUEST_PARAMETERS
                        )
                )
                .when()
                .get("/image?path=" + location)
                .then()
                .statusCode(HTTP_FORBIDDEN);
    }
}