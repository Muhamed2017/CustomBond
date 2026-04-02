package com.custombond.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A lightweight {@link MultipartFile} implementation backed by a plain {@code byte[]}.
 *
 * <p>Used by {@link com.custombond.pipeline.steps.UploadDocumentStep} to wrap the
 * raw file bytes that were eagerly read from the incoming multipart request (before
 * handing off to the async executor thread), so they can be passed to
 * {@link com.custombond.service.DXC_UploadDocument_Service#uploadDocument(MultipartFile, String)}
 * without requiring a live HTTP stream.
 *
 * <p>This class intentionally does <em>not</em> persist the bytes to disk; the transfer
 * ({@link #transferTo(java.io.File)}) operation is not supported.
 */
public class BytesMultipartFile implements MultipartFile {

    private final String name;
    private final String originalFilename;
    private final String contentType;
    private final byte[] content;

    /**
     * Constructs a new {@code BytesMultipartFile}.
     *
     * @param name             the form field name (e.g. {@code "File"})
     * @param originalFilename the original file name from the multipart upload
     * @param contentType      the MIME type (e.g. {@code "application/pdf"})
     * @param content          the raw file bytes; must not be {@code null}
     */
    public BytesMultipartFile(String name, String originalFilename,
                              String contentType, byte[] content) {
        this.name = name;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.content = content != null ? content : new byte[0];
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return content.length == 0;
    }

    @Override
    public long getSize() {
        return content.length;
    }

    @Override
    public byte[] getBytes() throws IOException {
        return content;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(content);
    }

    /**
     * Not supported – this implementation is in-memory only.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
        throw new UnsupportedOperationException(
                "BytesMultipartFile does not support transferTo – use getBytes() instead");
    }
}
