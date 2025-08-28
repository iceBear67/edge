/*
 *    Copyright 2025 iceBear67 and Contributors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package io.ib67.edge.api.script.function;

import java.util.function.*;

public class Exports {
    public static <A,B> ExportedBiConsumer<A,B> export(BiConsumer<A,B> consumer) {
        return new ExportedBiConsumer<>(consumer);
    }

    public static <A> ExportedConsumer<A> export(Consumer<A> consumer) {
        return new ExportedConsumer<>(consumer);
    }

    public static <A,B> ExportedFunction<A,B> export(Function<A,B> function) {
        return new ExportedFunction<>(function);
    }

    public static <A,B,C> ExportedBiFunction<A,B,C> export(BiFunction<A,B,C> function) {
        return new ExportedBiFunction<>(function);
    }

    public static <A> ExportedPredicate<A> export(Predicate<A> predicate) {
        return new ExportedPredicate<>(predicate);
    }
}
