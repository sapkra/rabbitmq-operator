FROM gradle:6.0.1-jdk11
COPY . /tmp/rabbitmq-operator
WORKDIR /tmp/rabbitmq-operator/rabbitmq-operator
RUN gradle build
WORKDIR /app
RUN cp /tmp/rabbitmq-operator/rabbitmq-operator/build/distributions/rabbitmq-operator.zip /app/rabbitmq-operator.zip
RUN rm -rf /tmp/rabbitmq-operator
RUN chown 1001:0 -R /app
USER 1001
WORKDIR /app
RUN unzip rabbitmq-operator.zip && rm -rf rabbitmq-operator.zip
CMD ["java", "-cp", "rabbitmq-operator/lib/*", "com.indeed.operators.rabbitmq.RabbitMQOperator"]
