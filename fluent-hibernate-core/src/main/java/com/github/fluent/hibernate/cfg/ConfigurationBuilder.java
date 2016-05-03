package com.github.fluent.hibernate.cfg;

import static com.github.fluent.hibernate.internal.util.InternalUtils.Asserts.fail;
import static com.github.fluent.hibernate.internal.util.InternalUtils.Asserts.isTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.hibernate.SessionFactory;
import org.hibernate.boot.model.naming.ImplicitNamingStrategy;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import com.github.fluent.hibernate.cfg.scanner.EntityScanner;
import com.github.fluent.hibernate.cfg.strategy.StrategyOptions;
import com.github.fluent.hibernate.cfg.strategy.hibernate5.Hibernate5NamingStrategy;
import com.github.fluent.hibernate.internal.util.InternalUtils;

/**
 *
 * @author V.Ladynev
 */
class ConfigurationBuilder {

    private final Configuration result = new Configuration();

    public void configure(String hibernateCfgXml) {
        if (hibernateCfgXml == null) {
            result.configure();
        } else {
            result.configure(hibernateCfgXml);
        }
    }

    public void addDatabaseOptions(DatabaseOptions options) {
        addProperties(options.getOptionsAsProperties());
    }

    public SessionFactory buildSessionFactory() {
        return result.buildSessionFactory();
    }

    public void addPropertiesFromClassPath(String classPathResourceName) {
        InputStream stream = createBootstrapServiceRegistry().getService(ClassLoaderService.class)
                .locateResourceStream(classPathResourceName);
        addProperties(loadProperties(stream));
    }

    public void addPropertiesFromFile(File pathToPropertiesFile) {
        try {
            addProperties(loadProperties(new FileInputStream(pathToPropertiesFile)));
        } catch (Exception ex) {
            throw InternalUtils.toRuntimeException(ex);
        }
    }

    private Properties loadProperties(InputStream stream) {
        Properties result = new Properties();
        try {
            result.load(stream);
            return result;
        } catch (Exception ex) {
            throw InternalUtils.toRuntimeException(ex);
        } finally {
            InternalUtils.closeQuietly(stream);
        }
    }

    private void addProperties(Properties properties) {
        result.addProperties(properties);
    }

    public void addAnnotatedClasses(Class<?>[] annotatedClasses) {
        if (annotatedClasses == null) {
            return;
        }

        for (Class<?> annotatedClass : annotatedClasses) {
            result.addAnnotatedClass(annotatedClass);
        }
    }

    public void addPackagesToScan(String[] packagesToScan) {
        EntityScanner.scanPackages(packagesToScan).addTo(result);
    }

    public void useNamingStrategy() {
        result.setImplicitNamingStrategy(new Hibernate5NamingStrategy());
    }

    public void useNamingStrategy(StrategyOptions options) {
        if (options.isAutodetectMaxLength()) {
            options.setMaxLength(
                    detectMaxLength(Environment.getProperties().getProperty(Environment.DIALECT)));
        }

        result.setImplicitNamingStrategy(new Hibernate5NamingStrategy(options));
    }

    public void useNamingStrategy(ImplicitNamingStrategy startegy) {
        result.setImplicitNamingStrategy(startegy);
    }

    private int detectMaxLength(String dialect) {
        isTrue(!InternalUtils.StringUtils.isEmpty(dialect), String.format(
                "Can't autodetect a max length. Property %s is not set", Environment.DIALECT));
        String dialectClass = InternalUtils.ClassUtils.getShortName(dialect);

        if (dialectClass.contains("H2Dialect")) {
            return 0; // no limitations
        }

        if (dialectClass.contains("MySQL")) {
            return 64;
        }

        if (dialectClass.contains("Oracle")) {
            return 30;
        }

        if (dialectClass.contains("PostgreSQL")) {
            return 63;
        }

        fail("Can't autodetect a max length. Specify it with StrategyOptions.setMaxLength()");
        return 0;
    }

    private BootstrapServiceRegistry createBootstrapServiceRegistry() {
        return new BootstrapServiceRegistryBuilder().enableAutoClose().build();
    }

}