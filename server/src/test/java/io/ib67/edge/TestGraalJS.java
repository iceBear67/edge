package io.ib67.edge;

import lombok.SneakyThrows;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.io.FileSystem;
import org.graalvm.polyglot.io.IOAccess;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.ByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class TestGraalJS {
    @SneakyThrows
    @Test
    public void test() {
        var nfs = FileSystem.newDefaultFileSystem();
        var pfs = new FileSystem() {
            @Override
            public Path parsePath(URI uri) {
                return nfs.parsePath(uri);
            }

            @Override
            public Path parsePath(String path) {
                return nfs.parsePath(path);
            }

            @Override
            public void checkAccess(Path path, Set<? extends AccessMode> modes, LinkOption... linkOptions) throws IOException {

            }

            @Override
            public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {

            }

            @Override
            public void delete(Path path) throws IOException {

            }

            @Override
            public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
                System.out.println("Reading "+path);
                return new ByteArrayChannel("""
                       let __vertx = _vertx
                       export {
                          __vertx as vertx
                       }
                        """.getBytes(), true);
            }

            @Override
            public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter) throws IOException {
                return null;
            }

            @Override
            public Path toAbsolutePath(Path path) {
                return path.toAbsolutePath();
            }

            @Override
            public Path toRealPath(Path path, LinkOption... linkOptions) throws IOException {
                return nfs.toRealPath(path, linkOptions);
            }

            @Override
            public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
                return Map.of();
            }
        };
        var _fs = FileSystem.newCompositeFileSystem(
                FileSystem.newDenyIOFileSystem(),
                FileSystem.Selector.of(FileSystem.newDefaultFileSystem(), it -> {
                    System.out.println(it);
                    System.out.println(it.toAbsolutePath());
                    return true;
                })
        );
        var fs = FileSystem.newCompositeFileSystem(FileSystem.newDenyIOFileSystem(),
                FileSystem.Selector.of(pfs, it -> {
                    System.out.println(it);
                    return true;
                }));
        var ctx = Context.newBuilder("js")
                .option("js.esm-eval-returns-exports", "true")
                .allowIO(IOAccess.newBuilder().fileSystem(fs).build())
                .allowAllAccess(true).build();
        Runnable runnable = () -> System.out.println("Hello World");
        ctx.getBindings("js").putMember("_vertx", runnable);
        var exports = ctx.eval(Source.newBuilder("js", """
                  import { vertx } from "@vertx/vertx.mjs";
                  function hello() {
                    vertx();
                  }
                  export {
                    hello
                  };
                
                ""","test.mjs").build());
        exports.getMember("hello").as(Runnable.class).run();
        System.out.println(1);
    }
}
