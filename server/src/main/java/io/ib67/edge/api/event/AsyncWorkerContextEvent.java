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

import io.ib67.edge.Deployment;
import io.ib67.edge.api.script.ExportToScript;
import io.ib67.edge.worker.ScriptWorker;
import io.ib67.kiwi.event.api.Event;
import org.graalvm.polyglot.Value;

import java.util.Map;

/**
 * The event is fired when initializing a {@link ScriptWorker}'s binding. Before using this, make sure
 * you have seen comments on {@link AsyncPrivilegeContextEvent}. It's recommended that you inject dependencies
 * in privileged context and access them via some glue code.
 * Also, you should annotate {@link ExportToScript} on methods of these exposed objects for visibility.
 *
 * @param plugins The `plugins` object in guest context
 * @param env The `env` object in guest context
 * @param binding The raw binding of context
 * @param deployment corresponding deployment
 */
@AsyncEvent
public record AsyncWorkerContextEvent(
        Map<String, String> env,
        Map<String, Object> plugins,
        Value binding,
        Deployment deployment
) implements Event {
}
