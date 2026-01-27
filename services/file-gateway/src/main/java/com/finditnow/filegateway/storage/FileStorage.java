package com.finditnow.filegateway.storage;

import java.io.IOException;
import java.io.InputStream;

public interface FileStorage {
    void save(InputStream input, String fileKey) throws IOException;

    InputStream read(String fileKey) throws IOException;

    boolean exists(String fileKey) throws IOException;
}
