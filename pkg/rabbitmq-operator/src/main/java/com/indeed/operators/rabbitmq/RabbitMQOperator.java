package com.indeed.operators.rabbitmq;

import com.indeed.operators.rabbitmq.controller.crd.NetworkPartitionResourceController;
import com.indeed.operators.rabbitmq.controller.crd.RabbitMQResourceController;
import com.indeed.operators.rabbitmq.model.crd.partition.RabbitMQNetworkPartitionCustomResource;
import com.indeed.operators.rabbitmq.model.crd.rabbitmq.RabbitMQCustomResource;
import io.fabric8.kubernetes.internal.KubernetesDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootApplication(exclude = GsonAutoConfiguration.class)
public class RabbitMQOperator implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(RabbitMQOperator.class);

    private final RabbitMQResourceController rabbitMQResourceController;
    private final NetworkPartitionResourceController networkPartitionResourceController;
    private final RabbitMQEventWatcher rabbitMQEventWatcher;
    private final NetworkPartitionWatcher networkPartitionWatcher;
    private final ScheduledExecutorService scheduledExecutor;
    private final String namespace;

    @Autowired
    public RabbitMQOperator(
            final RabbitMQResourceController rabbitMQResourceController,
            final NetworkPartitionResourceController networkPartitionResourceController,
            final RabbitMQEventWatcher rabbitMQEventWatcher,
            final NetworkPartitionWatcher networkPartitionWatcher,
            final ScheduledExecutorService scheduledExecutor,
            final String namespace
    ) {
        this.rabbitMQResourceController = rabbitMQResourceController;
        this.networkPartitionResourceController = networkPartitionResourceController;
        this.rabbitMQEventWatcher = rabbitMQEventWatcher;
        this.networkPartitionWatcher = networkPartitionWatcher;
        this.scheduledExecutor = scheduledExecutor;
        this.namespace = namespace;
    }

    public static void main(final String[] args) {
        new SpringApplicationBuilder(RabbitMQOperator.class).web(WebApplicationType.NONE).bannerMode(Banner.Mode.OFF).run(args);
    }

    @Override
    public void run(final String[] args) {
        log.info("Starting {}", RabbitMQOperator.class.getName());

        registerCrdDeserializationTypes();

        rabbitMQResourceController.watch(rabbitMQEventWatcher, namespace);
        networkPartitionResourceController.watch(networkPartitionWatcher, namespace);


        scheduledExecutor.scheduleAtFixedRate(() -> {
            try {
                rabbitMQEventWatcher.reconcileAll(namespace);
            } catch (final Throwable t) {
                log.error("Got an error while reconciling all clusters", t);
            }
        }, 10, 60, TimeUnit.SECONDS);

        scheduledExecutor.scheduleAtFixedRate(() -> {
            try {
                networkPartitionWatcher.reconcileAll(namespace);
            } catch (final Throwable t) {
                log.error("Got an error while reconciling all network partitions", t);
            }

        }, 10, 60, TimeUnit.SECONDS);
    }

    private void registerCrdDeserializationTypes() {
        KubernetesDeserializer.registerCustomKind("indeed.com/v1alpha1", "RabbitMQCustomResource", RabbitMQCustomResource.class);
        KubernetesDeserializer.registerCustomKind("indeed.com/v1alpha1", "RabbitMQNetworkPartitionCustomResource", RabbitMQNetworkPartitionCustomResource.class);
    }
}
