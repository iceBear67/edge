package io.ib67.edge.script.io;

import org.graalvm.polyglot.io.FileSystem;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.util.Map;
import java.util.Set;

public class DelegatedFileSystem implements FileSystem {
    protected final FileSystem delegatedFS;

    public DelegatedFileSystem(FileSystem delegatedFS) {
        this.delegatedFS = delegatedFS;
    }

    @Override
    public Path parsePath(URI uri) {
        return delegatedFS.parsePath(uri);
    }

    @Override
    public Path parsePath(String path) {
        return delegatedFS.parsePath(path);
    }

    @Override
    public void checkAccess(Path path, Set<? extends AccessMode> modes, LinkOption... linkOptions) throws IOException {
        delegatedFS.checkAccess(path, modes, linkOptions);
    }

    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        delegatedFS.createDirectory(dir, attrs);
    }

    @Override
    public void delete(Path path) throws IOException {
        delegatedFS.delete(path);
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        return delegatedFS.newByteChannel(path, options, attrs);
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
        return delegatedFS.newDirectoryStream(dir, filter);
    }

    @Override
    public Path toAbsolutePath(Path path) {
        return delegatedFS.toAbsolutePath(path);
    }

    @Override
    public Path toRealPath(Path path, LinkOption... linkOptions) throws IOException {
        return delegatedFS.toRealPath(path, linkOptions);
    }

    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        return delegatedFS.readAttributes(path, attributes, options);
    }
}
