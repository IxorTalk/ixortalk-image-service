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
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;

import static com.ixortalk.test.oauth2.OAuth2TestTokens.adminToken;
import static com.ixortalk.test.oauth2.OAuth2TestTokens.userToken;
import static com.jayway.restassured.RestAssured.given;
import static java.net.HttpURLConnection.*;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.restassured.RestAssuredRestDocumentation.document;

public class ImageController_GetImage_IntegrationAndRestDocTest extends AbstractSpringIntegrationTest {

    @Test
    public void success() throws IOException {

        InputStream inputStream = given()
                .auth().preemptive().oauth2(adminToken().getValue())
                .filter(
                        document("images/get",
                                preprocessRequest(staticUris(), prettyPrint()),
                                preprocessResponse(prettyPrint(), removeBinaryContent()),
                                requestHeaders(describeAuthorizationTokenHeader())
                        )
                )
                .when()
                .get("/download/"+ location)
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
                                requestHeaders(describeAuthorizationTokenHeader())
                        )
                )
                .get("/download/" + location + location)
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
                                requestHeaders(describeAuthorizationTokenHeader())
                        )
                )
                .when()
                .get("/download/"+ location)
                .then()
                .statusCode(HTTP_FORBIDDEN);
    }

    @Test
    public void s3TemplateReturnsError() {
        Mockito.reset(awsS3Template);

        given()
                .auth().preemptive().oauth2(adminToken().getValue())
                .when()
                .get("/download/"+ location)
                .then()
                .statusCode(HTTP_BAD_REQUEST);
    }
}