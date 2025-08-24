// JooqLoggingConfig.java
package org.jdk.qq_bot.config;

import org.jooq.ExecuteContext;
import org.jooq.Result;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;
import org.jooq.impl.DefaultExecuteListener;
import org.jooq.impl.DefaultExecuteListenerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JooqLoggingConfig {

    // 关闭格式化，多数情况下单行更清爽；如果你喜欢多行就改成 true
    @Bean
    public Settings jooqSettings() {
        return new Settings().withRenderFormatted(false);
    }

    @Bean
    public DefaultExecuteListenerProvider compactSqlLogger() {
        return new DefaultExecuteListenerProvider(new DefaultExecuteListener() {
            final Logger log = LoggerFactory.getLogger("jooq.sql");

            @Override
            public void executeStart(ExecuteContext ctx) {
                ctx.data("t0", System.nanoTime());
            }

            @Override
            public void renderEnd(ExecuteContext ctx) {
                // 把参数仅在日志中内联（不会影响实际执行）
                if (ctx.query() != null) {
                    String sql = DSL.using(ctx.configuration()).renderInlined(ctx.query());
                    ctx.data("sql_inline", sql);
                }
            }

            @Override
            public void executeEnd(ExecuteContext ctx) {
                long t0 = (long) ctx.data("t0");
                long ms = (System.nanoTime() - t0) / 1_000_000;

                String sql = String.valueOf(ctx.data("sql_inline"));
                if (sql.length() > 800) sql = sql.substring(0, 800) + " ...";

                int rows = -1;
                if (ctx.result() != null) {
                    Result<?> r = ctx.result();
                    rows = r.size();
                } else if (ctx.rows() >= 0) {
                    rows = ctx.rows(); // DML 影响行数
                }

                log.info("{} | rows={} | {}ms", sql, rows, ms);
            }
        });
    }
}
