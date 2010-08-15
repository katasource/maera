package org.maera.plugin.servlet.util;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

/**
 * A {@link javax.servlet.http.HttpServletResponse} that captures its output (in platform specific charset!)
 *
 * @since v4.0
 */
public class CapturingHttpServletResponse implements HttpServletResponse {
    private final ByteArrayOutputStream baos;
    private final ServletOutputStream servletOutputStream;
    private PrintWriter printWriter;

    public CapturingHttpServletResponse() {
        baos = new ByteArrayOutputStream();
        servletOutputStream = new ServletOutputStream() {
            @Override
            public void write(final int b) throws IOException {
                baos.write(b);
            }
        };
        printWriter = new PrintWriter(servletOutputStream);
    }


    public ServletOutputStream getOutputStream() throws IOException {
        return servletOutputStream;
    }

    public PrintWriter getWriter() throws IOException {
        return printWriter;
    }

    public String toString() {
        return baos.toString();
    }

    //===================================================

    public void addCookie(final Cookie cookie) {
    }

    public boolean containsHeader(final String name) {
        return false;
    }

    public String encodeURL(final String url) {
        return null;
    }

    public String encodeRedirectURL(final String url) {
        return null;
    }

    public String encodeUrl(final String url) {
        return null;
    }

    public String encodeRedirectUrl(final String url) {
        return null;
    }

    public void sendError(final int sc, final String msg) throws IOException {
    }

    public void sendError(final int sc) throws IOException {
    }

    public void sendRedirect(final String location) throws IOException {
    }

    public void setDateHeader(final String name, final long date) {
    }

    public void addDateHeader(final String name, final long date) {
    }

    public void setHeader(final String name, final String value) {
    }

    public void addHeader(final String name, final String value) {
    }

    public void setIntHeader(final String name, final int value) {
    }

    public void addIntHeader(final String name, final int value) {
    }

    public void setStatus(final int sc) {
    }

    public void setStatus(final int sc, final String sm) {
    }

    public String getCharacterEncoding() {
        return null;
    }

    public void setContentLength(final int len) {
    }

    public void setContentType(final String type) {
    }

    public void setBufferSize(final int size) {
    }

    public int getBufferSize() {
        return 0;
    }

    public void flushBuffer() throws IOException {
    }

    public void resetBuffer() {
    }

    public boolean isCommitted() {
        return false;
    }

    public void reset() {
    }

    public void setLocale(final Locale loc) {
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public void setCharacterEncoding(String charset) {

    }

    public Locale getLocale() {
        return null;
    }
}
