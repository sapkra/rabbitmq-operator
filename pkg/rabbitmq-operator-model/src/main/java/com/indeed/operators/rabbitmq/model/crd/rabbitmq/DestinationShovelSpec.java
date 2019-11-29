package com.indeed.operators.rabbitmq.model.crd.rabbitmq;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import java.util.List;

@JsonPropertyOrder({"addresses", "clusterName"})
public class DestinationShovelSpec {

    private final List<AddressAndVhost> addresses;
    private final String secretName;
    private final String secretNamespace;

    @JsonCreator
    public DestinationShovelSpec(
            @JsonProperty("addresses") final List<AddressAndVhost> addresses,
            @JsonProperty("secretName") final String secretName,
            @JsonProperty("secretNamespace") final String secretNamespace
    ) {
        Preconditions.checkArgument(addresses != null && !addresses.isEmpty(), "Shovel destination 'addresses' cannot be empty or null");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(secretName), "Shovel destination 'secretName' cannot be empty or null");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(secretNamespace), "Shovel destination 'secretNamespace' cannot be empty or null");

        this.addresses = addresses;
        this.secretName = secretName;
        this.secretNamespace = secretNamespace;
    }

    public List<AddressAndVhost> getAddresses() {
        return addresses;
    }

    public String getSecretName() {
        return secretName;
    }

    public String getSecretNamespace() {
        return secretNamespace;
    }
}
