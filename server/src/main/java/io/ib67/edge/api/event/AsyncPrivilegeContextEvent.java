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

package io.ib67.edge.api.event;

import org.graalvm.polyglot.Value;

import java.util.Map;

/**
 * This event is fired when a privilege context is initializing its binding. The event has declared several
 * shared definitions among plugins. When working with new components, you should consider using them first.
 * @param env The `env` object in privileged contexts
 * @param plugins The `plugins` object in privileged contexts
 * @param value The raw value binding.
 */
@AsyncEvent
public record AsyncPrivilegeContextEvent(
        Map<String, String> env,
        Map<String, Object> plugins,
        Value value
) {
}
