FROM gradle:5.1.1-jdk11
USER root
COPY . .
RUN gradle build

FROM openjdk:11.0.1-jre
WORKDIR /app
COPY --from=0 /home/gradle/rabbitmq-operator/build/distributions/rabbitmq-operator.zip .
RUN unzip rabbitmq-operator.zip && rm -rf rabbitmq-operator.zip
USER 1001
CMD ["java", "-cp", "rabbitmq-operator/lib/*", "com.indeed.operators.rabbitmq.RabbitMQOperator"]