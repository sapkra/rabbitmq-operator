package com.indeed.operators.rabbitmq.reconciliation.rabbitmq;

import com.indeed.operators.rabbitmq.controller.SecretsController;
import com.indeed.operators.rabbitmq.model.Labels;
import com.indeed.operators.rabbitmq.model.crd.rabbitmq.RabbitMQCustomResource;
import com.indeed.operators.rabbitmq.model.crd.rabbitmq.RabbitMQCustomResourceSpec;
import com.indeed.operators.rabbitmq.model.crd.rabbitmq.RabbitMQStorageResources;
import com.indeed.operators.rabbitmq.model.rabbitmq.RabbitMQCluster;
import com.indeed.operators.rabbitmq.model.rabbitmq.RabbitMQUser;
import com.indeed.operators.rabbitmq.reconciliation.RabbitClusterConfigurationException;
import com.indeed.operators.rabbitmq.reconciliation.validators.RabbitClusterValidator;
import com.indeed.operators.rabbitmq.resources.RabbitMQContainers;
import com.indeed.operators.rabbitmq.resources.RabbitMQPods;
import com.indeed.operators.rabbitmq.resources.RabbitMQSecrets;
import com.indeed.operators.rabbitmq.resources.RabbitMQServices;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.fabric8.kubernetes.api.model.policy.PodDisruptionBudget;
import io.fabric8.kubernetes.api.model.policy.PodDisruptionBudgetBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.indeed.operators.rabbitmq.Constants.RABBITMQ_STORAGE_NAME;

public class RabbitMQClusterFactory {

    private final List<RabbitClusterValidator> clusterValidators;
    private final RabbitMQContainers rabbitMQContainers;
    private final RabbitMQPods rabbitMQPods;
    private final RabbitMQSecrets rabbitMQSecrets;
    private final RabbitMQServices rabbitMQServices;
    private final SecretsController secretsController;

    public RabbitMQClusterFactory(
            final List<RabbitClusterValidator> clusterValidators,
            final RabbitMQContainers rabbitMQContainers,
            final RabbitMQPods rabbitMQPods,
            final RabbitMQSecrets rabbitMQSecrets,
            final RabbitMQServices rabbitMQServices,
            final SecretsController secretsController
    ) {
        this.clusterValidators = clusterValidators;
        this.rabbitMQContainers = rabbitMQContainers;
        this.rabbitMQPods = rabbitMQPods;
        this.rabbitMQSecrets = rabbitMQSecrets;
        this.rabbitMQServices = rabbitMQServices;
        this.secretsController = secretsController;
    }

    public RabbitMQCluster fromCustomResource(final RabbitMQCustomResource resource) throws RabbitClusterConfigurationException {
        final String clusterName = resource.getName();
        final String namespace = resource.getMetadata().getNamespace();
        final RabbitMQCustomResourceSpec spec = resource.getSpec();

        final List<String> errors = clusterValidators.stream()
                .map(validator -> validator.validate(spec.getClusterSpec()))
                .flatMap(List::stream)
                .collect(Collectors.toList());

        if (!errors.isEmpty()) {
            throw new RabbitClusterConfigurationException(errors);
        }

        final Secret adminSecret = getOrGenerateAdminSecret(resource);
        final Secret erlangCookieSecret = getOrGenerateErlangSecret(resource);
        final Service mainService = rabbitMQServices.buildService(namespace, resource);
        final Service discoveryService = rabbitMQServices.buildDiscoveryService(namespace, resource);

        final Optional<Service> loadBalancerService;
        if (spec.isCreateLoadBalancer()) {
            loadBalancerService = Optional.of(rabbitMQServices.buildLoadBalancerService(namespace, resource));
        } else {
            loadBalancerService = Optional.empty();
        }

        final Optional<Service> nodePortService;
        if (spec.isCreateNodePort()) {
            nodePortService = Optional.of(rabbitMQServices.buildNodePortService(namespace, resource));
        } else {
            nodePortService = Optional.empty();
        }

        final Container container = rabbitMQContainers.buildContainer(
                namespace,
                clusterName,
                spec.getRabbitMQImage(),
                spec.getComputeResources(),
                spec.getClusterSpec().getHighWatermarkFraction());

        final StatefulSet statefulSet = buildStatefulSet(resource, container);

        final PodDisruptionBudget podDisruptionBudget = buildPodDisruptionBudget(resource);

        final List<RabbitMQUser> users = buildUsers(resource);

        return RabbitMQCluster.newBuilder()
                .withName(clusterName)
                .withNamespace(namespace)
                .withAdminSecret(adminSecret)
                .withErlangCookieSecret(erlangCookieSecret)
                .withMainService(mainService)
                .withDiscoveryService(discoveryService)
                .withLoadBalancerService(loadBalancerService)
                .withNodePortService(nodePortService)
                .withStatefulSet(statefulSet)
                .withPodDisruptionBudget(podDisruptionBudget)
                .withShovels(resource.getSpec().getClusterSpec().getShovels())
                .withUsers(users)
                .withPolicies(spec.getClusterSpec().getPolicies())
                .withOperatorPolicies(spec.getClusterSpec().getOperatorPolicies())
                .build();
    }

    private Secret getOrGenerateErlangSecret(final RabbitMQCustomResource resource) {
        final Secret existingErlangSecret = secretsController.get(RabbitMQSecrets.getErlangCookieSecretName(resource.getName()), resource.getMetadata().getNamespace());

        return existingErlangSecret != null ? existingErlangSecret : rabbitMQSecrets.createErlangCookieSecret(resource);
    }

    private Secret getOrGenerateAdminSecret(final RabbitMQCustomResource resource) {
        final Secret existingAdminSecret = secretsController.get(RabbitMQSecrets.getClusterSecretName(resource.getName()), resource.getMetadata().getNamespace());

        return existingAdminSecret != null ? existingAdminSecret : rabbitMQSecrets.createClusterSecret(resource);
    }

    private StatefulSet buildStatefulSet(
            final RabbitMQCustomResource resource,
            final Container container
    ) {
        final String clusterName = resource.getName();
        final String namespace = resource.getMetadata().getNamespace();

        final RabbitMQStorageResources storage = resource.getSpec().getStorageResources();

        return new StatefulSetBuilder()
                .withNewMetadata()
                .withName(clusterName)
                .withNamespace(namespace)
                .withOwnerReferences(
                        new OwnerReference(
                                resource.getApiVersion(),
                                true,
                                true,
                                resource.getKind(),
                                clusterName,
                                resource.getMetadata().getUid()
                        )
                )
                .addToLabels(Labels.Kubernetes.INSTANCE, clusterName)
                .addToLabels(Labels.Kubernetes.MANAGED_BY, Labels.Values.RABBITMQ_OPERATOR)
                .addToLabels(Labels.Kubernetes.PART_OF, Labels.Values.RABBITMQ)
                .endMetadata()
                .withNewSpec()
                .withReplicas(resource.getSpec().getReplicas())
                .withServiceName(RabbitMQServices.getDiscoveryServiceName(clusterName))
                .withNewSelector().addToMatchLabels(Labels.Kubernetes.INSTANCE, clusterName).endSelector()
                .withNewTemplate()
                .withNewMetadata()
                .addToLabels(Labels.Kubernetes.INSTANCE, clusterName)
                .addToLabels(Labels.Kubernetes.MANAGED_BY, Labels.Values.RABBITMQ_OPERATOR)
                .addToLabels(Labels.Kubernetes.PART_OF, Labels.Values.RABBITMQ)
                .addToLabels(Labels.Indeed.getIndeedLabels(resource))
                .addToAnnotations("ad.datadoghq.com/" + clusterName + ".check_names", "[\"rabbitmq\"]")
                .addToAnnotations("ad.datadoghq.com/" + clusterName + ".init_configs", "[{}]")
                .addToAnnotations("ad.datadoghq.com/" + clusterName + ".instances", "[{\"rabbitmq_api_url\":\"http://%%host%%:15672/api\",\"rabbitmq_user\":\"monitoring\",\"rabbitmq_pass\":\"monitoring\"}]")
                .endMetadata()
                .withSpec(rabbitMQPods.buildPodSpec(clusterName, resource.getSpec().getInitContainerImage(), container))
                .endTemplate()
                .addNewVolumeClaimTemplate()
                .withNewMetadata()
                .withName(RABBITMQ_STORAGE_NAME)
                .withOwnerReferences(
                        new OwnerReference(
                                resource.getApiVersion(),
                                true,
                                true,
                                resource.getKind(),
                                clusterName,
                                resource.getMetadata().getUid()
                        )
                )
                .addToLabels(Labels.Kubernetes.INSTANCE, clusterName)
                .addToLabels(Labels.Kubernetes.MANAGED_BY, Labels.Values.RABBITMQ_OPERATOR)
                .addToLabels(Labels.Kubernetes.PART_OF, Labels.Values.RABBITMQ)
                .endMetadata()
                .withNewSpec()
                .withStorageClassName(storage.getStorageClassName())
                .withAccessModes("ReadWriteOnce")
                .withNewResources()
                .withRequests(Collections.singletonMap("storage", storage.getStorage()))
                .endResources()
                .endSpec()
                .endVolumeClaimTemplate()
                .endSpec()
                .build();
    }

    private PodDisruptionBudget buildPodDisruptionBudget(final RabbitMQCustomResource resource) {
        final String namespace = resource.getMetadata().getNamespace();
        return new PodDisruptionBudgetBuilder()
                .withNewMetadata()
                .withName(String.format("%s-poddisruptionbudget", resource.getName()))
                .withNamespace(namespace)
                .withOwnerReferences(
                        new OwnerReference(
                                resource.getApiVersion(),
                                true,
                                true,
                                resource.getKind(),
                                resource.getName(),
                                resource.getMetadata().getUid()
                        )
                )
                .endMetadata()
                .withNewSpec()
                .withMaxUnavailable(new IntOrString(1))
                .withNewSelector()
                .withMatchLabels(Collections.singletonMap(Labels.Kubernetes.INSTANCE, resource.getName()))
                .endSelector()
                .endSpec()
                .build();
    }

    private List<RabbitMQUser> buildUsers(final RabbitMQCustomResource resource) {
        return resource.getSpec().getClusterSpec().getUsers().stream()
                .map(user -> {
                    final Secret maybeExistingUserSecret = secretsController.get(RabbitMQSecrets.getUserSecretName(user.getUsername(), resource.getName()), resource.getMetadata().getNamespace());

                    return new RabbitMQUser(
                            user.getUsername(),
                            maybeExistingUserSecret != null ? maybeExistingUserSecret : rabbitMQSecrets.createUserSecret(user.getUsername(), resource),
                            resource.getMetadata(),
                            new OwnerReference(
                                    resource.getApiVersion(),
                                    true,
                                    true,
                                    resource.getKind(),
                                    resource.getName(),
                                    resource.getMetadata().getUid()
                            ),
                            user.getVhosts(),
                            user.getTags()
                    );
                })
                .collect(Collectors.toList());
    }
}
