package com.foodmap.common.storage;

import java.io.InputStream;

public interface ObjectStorageClient {

    ObjectStorageResult putObject(ObjectStorageCommand command, InputStream inputStream);
}
