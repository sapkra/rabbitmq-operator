package com.indeed.operators.rabbitmq.config;

import com.indeed.operators.rabbitmq.NetworkPartitionWatcher;
import com.indeed.operators.rabbitmq.RabbitMQEventWatcher;
import com.indeed.operators.rabbitmq.api.RabbitMQPasswordConverter;
import com.indeed.operators.rabbitmq.api.RabbitManagementApiProvider;
import com.indeed.operators.rabbitmq.controller.PersistentVolumeClaimController;
import com.indeed.operators.rabbitmq.controller.PodController;
import com.indeed.operators.rabbitmq.controller.PodDisruptionBudgetController;
import com.indeed.operators.rabbitmq.controller.SecretsController;
import com.indeed.operators.rabbitmq.controller.ServicesController;
import com.indeed.operators.rabbitmq.controller.StatefulSetController;
import com.indeed.operators.rabbitmq.controller.crd.NetworkPartitionResourceController;
import com.indeed.operators.rabbitmq.controller.crd.RabbitMQResourceController;
import com.indeed.operators.rabbitmq.executor.ClusterAwareExecutor;
import com.indeed.operators.rabbitmq.operations.AreQueuesEmptyOperation;
import com.indeed.operators.rabbitmq.reconciliation.ClusterReconciliationOrchestrator;
import com.indeed.operators.rabbitmq.reconciliation.NetworkPartitionReconciler;
import com.indeed.operators.rabbitmq.reconciliation.RabbitMQClusterReconciler;
import com.indeed.operators.rabbitmq.reconciliation.rabbitmq.UserReconciler;
import com.indeed.operators.rabbitmq.reconciliation.rabbitmq.OperatorPolicyReconciler;
import com.indeed.operators.rabbitmq.reconciliation.rabbitmq.PolicyReconciler;
import com.indeed.operators.rabbitmq.reconciliation.rabbitmq.RabbitMQClusterFactory;
import com.indeed.operators.rabbitmq.reconciliation.rabbitmq.ShovelReconciler;
import com.indeed.operators.rabbitmq.resources.RabbitMQContainers;
import com.indeed.operators.rabbitmq.resources.RabbitMQPods;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReconcilerConfig {

    @Bean
    public RabbitMQEventWatcher rabbitEventWatcher(
            final RabbitMQClusterReconciler reconciler,
            final RabbitMQResourceController controller,
            final ClusterReconciliationOrchestrator orchestrator
    ) {
        return new RabbitMQEventWatcher(reconciler, controller, orchestrator);
    }

    @Bean
    public NetworkPartitionWatcher networkPartitionWatcher(
            final NetworkPartitionReconciler partitionReconciler,
            final NetworkPartitionResourceController controller,
            final ClusterReconciliationOrchestrator orchestrator
    ) {
        return new NetworkPartitionWatcher(partitionReconciler, controller, orchestrator);
    }

    @Bean
    public ClusterReconciliationOrchestrator clusterReconciliationOrchestrator(
            final ClusterAwareExecutor executor
    ) {
        return new ClusterReconciliationOrchestrator(executor);
    }

    @Bean
    public RabbitMQClusterReconciler rabbitClusterReconciler(
            final RabbitMQClusterFactory clusterFactory,
            final RabbitMQResourceController controller,
            final SecretsController secretsController,
            final ServicesController servicesController,
            final StatefulSetController statefulSetController,
            final PodDisruptionBudgetController podDisruptionBudgetController,
            final PersistentVolumeClaimController persistentVolumeClaimController,
            final ShovelReconciler shovelReconciler,
            final UserReconciler usersReconciler,
            final PolicyReconciler policyReconciler,
            final OperatorPolicyReconciler operatorPolicyReconciler
    ) {
        return new RabbitMQClusterReconciler(
                clusterFactory,
                controller,
                secretsController,
                servicesController,
                statefulSetController,
                podDisruptionBudgetController,
                persistentVolumeClaimController,
                shovelReconciler,
                usersReconciler,
                policyReconciler,
                operatorPolicyReconciler
        );
    }

    @Bean
    public ShovelReconciler shovelReconciler(
            final RabbitManagementApiProvider apiProvider,
            final SecretsController secretsController
    ) {
        return new ShovelReconciler(apiProvider, secretsController);
    }

    @Bean
    public UserReconciler rabbitMQUserReconciler(
            final SecretsController secretsController,
            final RabbitManagementApiProvider managementApiProvider,
            final RabbitMQPasswordConverter passwordConverter
    ) {
        return new UserReconciler(secretsController, managementApiProvider, passwordConverter);
    }

    @Bean
    public NetworkPartitionReconciler networkPartitionReconciler(
            final RabbitMQResourceController rabbitMQResourceController,
            final NetworkPartitionResourceController networkPartitionResourceController,
            final AreQueuesEmptyOperation queuesEmptyOperation,
            final RabbitMQPods rabbitMQPods,
            final RabbitMQContainers rabbitMQContainers,
            final StatefulSetController statefulSetController,
            final PodController podController,
            final String namespace
    ) {
        return new NetworkPartitionReconciler(rabbitMQResourceController, networkPartitionResourceController, queuesEmptyOperation, rabbitMQPods, rabbitMQContainers, statefulSetController, podController, namespace);
    }

    @Bean
    public PolicyReconciler policyReconciler(
            final RabbitManagementApiProvider apiProvider
    ) {
        return new PolicyReconciler(apiProvider);
    }

    @Bean
    public OperatorPolicyReconciler operatorPolicyReconciler(
            final RabbitManagementApiProvider apiProvider
    ) {
        return new OperatorPolicyReconciler(apiProvider);
    }
}
