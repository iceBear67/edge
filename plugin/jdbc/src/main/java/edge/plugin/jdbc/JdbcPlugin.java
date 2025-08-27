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

package edge.plugin.jdbc;

import io.ib67.edge.api.event.AsyncPrivilegeContextEvent;
import io.ib67.edge.api.plugin.EdgePlugin;
import io.ib67.edge.api.plugin.PluginConfig;
import io.ib67.kiwi.event.api.EventBus;
import io.ib67.kiwi.event.api.EventListenerHost;
import io.ib67.kiwi.event.api.annotation.SubscribeEvent;
import io.vertx.core.Vertx;
import io.vertx.jdbcclient.JDBCConnectOptions;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;

import java.util.Objects;

public class JdbcPlugin implements EdgePlugin<JdbcPlugin.JdbcConfig>, EventListenerHost {
    protected Pool pool;

    @Override
    public String getName() {
        return "jdbc";
    }

    @Override
    public void init(Vertx vertx, EventBus bus, JdbcConfig config) {
        this.registerTo(bus);
        pool = JDBCPool.pool(vertx, config.connectOptions(), config.poolOptions());
    }

    @SubscribeEvent
    void registerBinding(AsyncPrivilegeContextEvent event){
        event.plugins().put("jdbc", pool);
    }

    public record JdbcConfig(
            JDBCConnectOptions connectOptions,
            PoolOptions poolOptions
    ) implements PluginConfig {
        public JdbcConfig() {
            this(new JDBCConnectOptions(), new PoolOptions());
        }

        public JdbcConfig {
            Objects.requireNonNull(connectOptions);
            Objects.requireNonNull(poolOptions);
        }
    }
}
