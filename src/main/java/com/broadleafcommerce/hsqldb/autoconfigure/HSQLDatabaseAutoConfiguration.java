/*-
 * #%L
 * BroadleafCommerce HSQLDB Database Starter
 * %%
 * Copyright (C) 2009 - 2019 Broadleaf Commerce
 * %%
 * Licensed under the Broadleaf Fair Use License Agreement, Version 1.0
 * (the "Fair Use License" located  at http://license.broadleafcommerce.org/fair_use_license-1.0.txt)
 * unless the restrictions on use therein are violated and require payment to Broadleaf in which case
 * the Broadleaf End User License Agreement (EULA), Version 1.1
 * (the "Commercial License" located at http://license.broadleafcommerce.org/commercial_license-1.1.txt)
 * shall apply.
 * 
 * Alternatively, the Commercial License may be replaced with a mutually agreed upon license (the "Custom License")
 * between you and Broadleaf Commerce. You may not use this file except in compliance with the applicable license.
 * #L%
 */
package com.broadleafcommerce.hsqldb.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

@Configuration
@EnableConfigurationProperties(HSQLDBProperties.class)
@ConditionalOnProperty(prefix = "demo.database", name = "autoConfigEnabled", matchIfMissing = true)
@AutoConfigureAfter(name = "com.broadleafcommerce.autoconfigure.DatabaseAutoConfiguration")
@AutoConfigureBefore(JpaRepositoriesAutoConfiguration.class)
public class HSQLDatabaseAutoConfiguration {

    private final HSQLDBProperties props;

    public HSQLDatabaseAutoConfiguration(HSQLDBProperties props) {
        this.props = props;
    }

    @ConditionalOnMissingBean(name = {"blDS", "webDS"})
    @Bean
    public HSQLDBServer blEmbeddedDatabase(Environment environment) {
        return new HSQLDBServer(props, environment);
    }

    @ConditionalOnMissingBean(name = {"blDS", "webDS"})
    @DependsOn("blEmbeddedDatabase")
    @Bean
    @Primary
    public DataSource blDS() {
        return buildDataSource(props.getPort(), props.getDbName());
    }

    @Configuration
    @ConditionalOnBean(name = "blMergedDataSources")
    public static class MultiDatasourceConfiguration {

        private final HSQLDBProperties props;

        public MultiDatasourceConfiguration(HSQLDBProperties props) {
            this.props = props;
        }

        @ConditionalOnMissingBean(name = {"webSecureDS"})
        @DependsOn("blEmbeddedDatabase")
        @Bean
        public DataSource webSecureDS() {
            return buildDataSource(props.getPort(), props.getDbName());
        }

        @ConditionalOnMissingBean(name = {"webStorageDS"})
        @DependsOn("blEmbeddedDatabase")
        @Bean
        public DataSource webStorageDS() {
            return buildDataSource(props.getPort(), props.getDbName());
        }

        @ConditionalOnMissingBean(name = {"webEventDS"})
        @DependsOn("blEmbeddedDatabase")
        @Bean
        public DataSource webEventDS() {
            return buildDataSource(props.getPort(), props.getDbName());
        }

        @ConditionalOnMissingBean(name = {"demoDS"})
        @ConditionalOnClass(name = "com.blcdemo.core.domain.PDSite")
        @DependsOn("blEmbeddedDatabase")
        @Bean
        public DataSource demoDS() {
            return buildDataSource(props.getPort(), props.getDbName());
        }
    }

    protected static DataSource buildDataSource(int port, String dbName) {
        String url = "jdbc:hsqldb:hsql://127.0.0.1:" + port + "/" + dbName;
        DatabaseDriver driver = DatabaseDriver.fromJdbcUrl(url);
        HikariDataSource ds = DataSourceBuilder
                .create()
                .username("SA")
                .password("")
                .url(url)
                .driverClassName(driver.getDriverClassName())
                .type(HikariDataSource.class)
                .build();

        String validationQuery = driver.getValidationQuery();
        if (validationQuery != null) {
            ds.setConnectionTestQuery(validationQuery);
        }
        ds.setConnectionInitSql("SET DATABASE TRANSACTION CONTROL MVCC");
        return ds;
    }

}
