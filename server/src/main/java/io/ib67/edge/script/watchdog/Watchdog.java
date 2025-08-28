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

package io.ib67.edge.script.watchdog;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.graalvm.polyglot.Context;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Watchdog will bark if a program exceed its time limit in watchdog's execution scope.
 */
@RequiredArgsConstructor
@Log4j2
public class Watchdog {
    protected final ScheduledExecutorService watchdogExecutor;
    protected final Duration interruptionTimeout;

    public Runnable setTimeout(Context context) {
        var handle = watchdogExecutor.schedule(() -> {
            try{
                context.interrupt(Duration.ZERO);
            }catch (TimeoutException ex){
                log.error("Error while interrupting context {}", context, ex);
            }
        }, interruptionTimeout.toMillis(), TimeUnit.MILLISECONDS);
        return () -> handle.cancel(true);
    }
}
