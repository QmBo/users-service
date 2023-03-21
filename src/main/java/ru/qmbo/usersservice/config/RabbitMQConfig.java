package ru.qmbo.usersservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    @Value("${rabbit.queue.input.collect}")
    private String collect;
    @Value("${rabbit.queue.input.subscribe}")
    private String subscribe;
    @Value("${rabbit.queue.input.unsubscribe}")
    private String unsubscribe;
    @Value("${rabbit.queue.input.statistic}")
    private String statistic;
    @Value("${rabbit.queue.input.getAllUsers}")
    private String getAllUsers;

    @Value("${rabbit.routing.binding.collect}")
    private String collectBind;
    @Value("${rabbit.routing.binding.subscribe}")
    private String subscribeBind;
    @Value("${rabbit.routing.binding.unsubscribe}")
    private String unsubscribeBind;
    @Value("${rabbit.routing.binding.statistic}")
    private String statisticBind;
    @Value("${rabbit.routing.binding.getAllUsers}")
    private String getAllUsersBind;

    @Value("${rabbit.exchangeTopic}")
    private String topic;

    @Bean
    TopicExchange exchange() {
        return new TopicExchange(topic);
    }

    @Bean
    Declarables query() {
        return new Declarables(
                new Queue(collect),
                new Queue(subscribe),
                new Queue(unsubscribe),
                new Queue(statistic),
                new Queue(getAllUsers)
        );
    }

    @Bean
    Declarables binding(TopicExchange exchange) {
        return new Declarables(
                BindingBuilder.bind(new Queue(collect)).to(exchange).with(collectBind),
                BindingBuilder.bind(new Queue(subscribe)).to(exchange).with(subscribeBind),
                BindingBuilder.bind(new Queue(unsubscribe)).to(exchange).with(unsubscribeBind),
                BindingBuilder.bind(new Queue(statistic)).to(exchange).with(statisticBind),
                BindingBuilder.bind(new Queue(getAllUsers)).to(exchange).with(getAllUsersBind)
        );
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory){
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
