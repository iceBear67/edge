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

package io.ib67.edge;

import io.ib67.edge.worker.Worker;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Log4j2
@RequiredArgsConstructor
public class WorkerRouter {
    protected final Vertx vertx;
    protected final Map<String, WorkerInfo> nameToWorkers = new ConcurrentHashMap<>();

    public boolean containsWorker(String name) {
        return nameToWorkers.containsKey(name.toLowerCase());
    }

    public Worker getWorker(String name) {
        name = name.toLowerCase();
        var wi = nameToWorkers.get(name);
        if (wi == null) return null;
        var status = wi.status;
        if (status != WorkerInfo.Status.NORMAL) {
            log.warn("Worker {} is in status {}", name, status);
            return null;
        }
        return wi.worker;
    }

    public Future<String> registerWorker(String _name, Supplier<Worker> worker) {
        var name = _name.toLowerCase();
        var oldWorker = nameToWorkers.get(name);
        if (oldWorker != null) {
            if (oldWorker.status != WorkerInfo.Status.NORMAL) {
                throw new IllegalStateException("Worker " + name + " is " + oldWorker.status + ", please try again later!");
            }
            var w = oldWorker.worker;
            oldWorker.status = WorkerInfo.Status.DEPLOYING;
            log.info("Redeploying worker {}", name);
            return vertx.undeploy(w.deploymentID())
                    .onFailure(t -> log.error("Error occurred when undeploying worker {}", name, t))
                    .compose(deployWorker(name, worker))
                    .onFailure(f -> {
                        oldWorker.status = WorkerInfo.Status.ERROR;
                        log.error("Failed to redeploy worker {}", name, f);
                    });
        }
        var wi = new WorkerInfo(null, WorkerInfo.Status.DEPLOYING);
        nameToWorkers.put(name, wi);
        log.info("Deploying worker {}", name);
        return deployWorker(name, worker).apply(null)
                .onFailure(f -> {
                    log.error("Error occurred when deploying worker {}", name, f);
                    wi.status = WorkerInfo.Status.ERROR;
                });
    }

    protected <A> Function<A, Future<String>> deployWorker(String name, Supplier<Worker> worker) {
        return z -> vertx.executeBlocking(worker::get)
                .flatMap(t -> vertx.deployVerticle(t).map(t))
                .onSuccess(v -> {
                    nameToWorkers.put(name, new WorkerInfo(v, WorkerInfo.Status.NORMAL));
                    log.info("Deployed worker {}", name);
                })
                .map(Worker::deploymentID);
    }

    public Map<String, Worker> getWorkers() {
        return nameToWorkers.entrySet()
                .stream()
                .<Map.Entry<String, Worker>>
                        mapMulti((entry, sink) -> {
                    var value = entry.getValue();
                    if (value.status != WorkerInfo.Status.NORMAL) return;
                    sink.accept(Map.entry(entry.getKey(), entry.getValue().worker));
                }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @AllArgsConstructor
    protected static class WorkerInfo {
        protected final Worker worker;
        protected volatile Status status = Status.DEPLOYING;

        protected enum Status {
            DEPLOYING, ERROR, NORMAL
        }
    }
}
