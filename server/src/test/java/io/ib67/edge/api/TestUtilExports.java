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

package io.ib67.edge.api;

import io.ib67.edge.api.script.ExportToScript;
import io.ib67.edge.api.script.function.Exports;
import io.ib67.kiwi.routine.Uni;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import java.lang.reflect.AccessFlag;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestUtilExports {
    @SneakyThrows
    @Test
    public void testExportUtil() {
        var methods = Exports.class.getMethods();
        for (Method method : methods) {
            if (method.getName().startsWith("export")) {
                assertTrue(method.accessFlags().contains(AccessFlag.PUBLIC)
                                && method.accessFlags().contains(AccessFlag.STATIC),
                        "Export util method should be `public static`. method: " + method);
                assertEquals(1, method.getParameterCount(), "The `Exports.export` util should only accept one argument. method: " + method);
                var returnType = method.getReturnType();
                var argType = method.getParameters()[0].getType();
                assertTrue(argType.isAssignableFrom(returnType),
                        "The parameter type of `Exports.export` should be assignable to its return type (all are FIs). method: " + method);
                checkWraperOverrideMethods(argType, returnType);
                var targetMethod = Uni.of(returnType.getMethods())
                        .filter(it -> it.isAnnotationPresent(ExportToScript.class))
                        .takeOne();
                var mock = mock(argType);
                mock.getClass().getMethod(targetMethod.getName(), targetMethod.getParameterTypes())
                        .invoke(mock, new Object[targetMethod.getParameterCount()]); // mutate mockito states
                when(mock).thenThrow(AssertionError.class);
                var object = assertDoesNotThrow(() -> method.invoke(null, mock));
                try {
                    targetMethod.invoke(object, new Object[targetMethod.getParameterCount()]);
                    assertFalse(false, "The wrapped object doesn't get called. method: "+method); // this is an assertion
                }catch(Exception e) {
                    assertInstanceOf(AssertionError.class, e.getCause(), "The wrapped object doesn't get called. method: "+method);
                }
            }
        }
    }

    private static void checkWraperOverrideMethods(Class<?> argType, Class<?> returnType) throws NoSuchMethodException {
        for (Method argTypeMethod : argType.getMethods()) {
            try {
                var wrapperMethod = returnType.getMethod(argTypeMethod.getName(), argTypeMethod.getParameterTypes());
                if (wrapperMethod.getDeclaringClass() != returnType) continue;
                assertTrue(wrapperMethod.isAnnotationPresent(ExportToScript.class)
                                || !wrapperMethod.accessFlags().contains(AccessFlag.PUBLIC),
                        "Public methods override by wrappers should annotate @ExportToScript. method: " + wrapperMethod);
            } catch (NoSuchMethodException ignored) {
            }
        }
    }
}
