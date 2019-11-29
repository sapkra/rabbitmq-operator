package com.indeed.operators.rabbitmq.config;

import com.indeed.operators.rabbitmq.executor.ClusterAwareExecutor;
import com.indeed.operators.rabbitmq.reconciliation.lock.NamedSemaphores;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import okhttp3.OkHttpClient;
import org.apache.commons.text.RandomStringGenerator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

@Configuration
public class AppConfig {

    // TODO: We've discussed making this configurable via a ConfigMap so that users can easily tune
    // it to match their needs.
    private static final int RECONCILIATION_THREAD_POOL_SIZE = 4;
    private static final int SCHEDULED_THREAD_POOL_SIZE = 4;

    @Bean
    public KubernetesClient kubernetesClient() {
        return new DefaultKubernetesClient();
    }

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient();
    }

    @Bean
    public NamedSemaphores namedSemaphores() {
        return new NamedSemaphores();
    }

    @Bean
    public Function<Integer, String> randomStringFunction() {
        return new RandomStringGenerator.Builder().withinRange(new char[][]{{'A','Z'}, {'a', 'z'}, {'0', '9'}}).build()::generate;
    }

    @Bean
    public String namespace(final KubernetesClient client) {
        return client.getNamespace();
    }

    @Bean
    @Qualifier("STANDARD_EXECUTOR")
    public ExecutorService executorService() {
        return Executors.newFixedThreadPool(RECONCILIATION_THREAD_POOL_SIZE);
    }

    @Bean
    public ClusterAwareExecutor clusterAwareExecutor(
            @Qualifier("STANDARD_EXECUTOR") final ExecutorService executor,
            final NamedSemaphores namedSemaphores
    ) {
        return new ClusterAwareExecutor(executor, namedSemaphores);
    }

    @Bean
    @Qualifier("SCHEDULED_EXECUTOR")
    public ScheduledExecutorService scheduledExecutorService() {
        return Executors.newScheduledThreadPool(SCHEDULED_THREAD_POOL_SIZE);
    }
}
