package com.sercapnp.lang;

/**
 * Created by striky on 6/26/17.
 */



import com.intellij.openapi.fileTypes.*;
import org.jetbrains.annotations.NotNull;

public class CapnpFileTypeFactory extends FileTypeFactory {
    @Override
    public void createFileTypes(@NotNull FileTypeConsumer fileTypeConsumer) {
        fileTypeConsumer.consume(CapnpFileType.INSTANCE, "Capnp");
    }
}