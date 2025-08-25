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

/**
 * The event is delivered when initializing a {@link ScriptWorker}'s binding.
 * You may listen to this event to supply new members to script context. When doing this,
 * you should also annotate {@link ExportToScript} on methods of these exposed objects, for visibility.
 *
 */
@AsyncEvent
public record AsyncWorkerDependencyEvent(Value binding, Deployment deployment) implements Event {
}
