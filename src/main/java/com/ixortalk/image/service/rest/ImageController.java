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

import com.amazonaws.services.s3.model.S3Object;
import com.ixortalk.aws.s3.library.config.AwsS3Template;
import com.ixortalk.image.service.config.IxorTalkConfigProperties;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static com.google.common.io.ByteStreams.toByteArray;
import static org.springframework.http.MediaType.valueOf;
import static org.springframework.http.ResponseEntity.ok;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class ImageController {

    @Inject
    private AwsS3Template awsS3Template;

    @Inject
    private IxorTalkConfigProperties ixorTalkConfigProperties;

    @GetMapping(path = "/image/**")
    public ResponseEntity<?> getImage(HttpServletRequest request) {
        String requestAttribute = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String key = StringUtils.substringAfterLast(requestAttribute, "image/");

        try {
            S3Object s3Object = awsS3Template.get(ixorTalkConfigProperties.getBucket(), key);
            return ok()
                    .contentType(valueOf(s3Object.getObjectMetadata().getContentType()))
                    .body(toByteArray(s3Object.getObjectContent()));
        } catch (IOException | NullPointerException e) {
            throw new IllegalArgumentException("Could not get key " + key + " from S3: " + e.getMessage(), e);
        }
    }
}
