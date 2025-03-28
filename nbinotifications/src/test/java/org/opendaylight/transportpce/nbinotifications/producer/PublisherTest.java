/*
 * Copyright © 2020 Orange, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.transportpce.nbinotifications.producer;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opendaylight.mdsal.binding.api.NotificationService;
import org.opendaylight.mdsal.binding.api.RpcProviderService;
import org.opendaylight.transportpce.common.converter.JsonStringConverter;
import org.opendaylight.transportpce.common.network.NetworkTransactionImpl;
import org.opendaylight.transportpce.common.network.NetworkTransactionService;
import org.opendaylight.transportpce.nbinotifications.impl.NbiNotificationsProvider;
import org.opendaylight.transportpce.nbinotifications.impl.rpc.CreateNotificationSubscriptionServiceImpl;
import org.opendaylight.transportpce.nbinotifications.serialization.ConfigConstants;
import org.opendaylight.transportpce.nbinotifications.serialization.NotificationAlarmServiceSerializer;
import org.opendaylight.transportpce.nbinotifications.serialization.NotificationServiceSerializer;
import org.opendaylight.transportpce.nbinotifications.serialization.TapiNotificationSerializer;
import org.opendaylight.transportpce.nbinotifications.utils.NotificationServiceDataUtils;
import org.opendaylight.transportpce.nbinotifications.utils.TopicManager;
import org.opendaylight.transportpce.test.AbstractTest;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.NotificationAlarmService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.NotificationProcessService;
import org.opendaylight.yang.gen.v1.nbi.notifications.rev230728.NotificationTapiService;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.common.rev221121.Uuid;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.CreateNotificationSubscriptionServiceInputBuilder;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.create.notification.subscription.service.input.SubscriptionFilter;
import org.opendaylight.yang.gen.v1.urn.onf.otcc.yang.tapi.notification.rev221121.create.notification.subscription.service.input.SubscriptionFilterBuilder;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;

@ExtendWith(MockitoExtension.class)
public class PublisherTest extends AbstractTest {

    private static NetworkTransactionService networkTransactionService;

    @Mock
    RpcProviderService rpcProviderRegistry;
    @Mock
    private NotificationService notificationService;

    private JsonStringConverter<NotificationProcessService> converterService;
    private JsonStringConverter<NotificationAlarmService> converterAlarm;
    private JsonStringConverter<NotificationTapiService> converterTapiService;
    private Publisher<NotificationProcessService> publisherService;
    private Publisher<NotificationAlarmService> publisherAlarm;
    private Publisher<NotificationTapiService> publisherTapiService;
    private MockProducer<String, NotificationProcessService> mockProducer;
    private MockProducer<String, NotificationAlarmService> mockAlarmProducer;
    private MockProducer<String, NotificationTapiService> mockTapiProducer;
    private NbiNotificationsProvider nbiNotifications;
    private TopicManager topicManager;


    @BeforeEach
    void setUp() throws ExecutionException, InterruptedException {
        topicManager = TopicManager.getInstance();
        converterService = new JsonStringConverter<>(getDataStoreContextUtil().getBindingDOMCodecServices());
        converterAlarm = new JsonStringConverter<>(getDataStoreContextUtil().getBindingDOMCodecServices());
        converterTapiService = new JsonStringConverter<>(getDataStoreContextUtil().getBindingDOMCodecServices());
        NotificationServiceSerializer serializerService = new NotificationServiceSerializer();
        NotificationAlarmServiceSerializer serializerAlarm = new NotificationAlarmServiceSerializer();
        TapiNotificationSerializer serializerTapi = new TapiNotificationSerializer();
        Map<String, Object> properties = Map.of(ConfigConstants.CONVERTER, converterService);
        Map<String, Object> propertiesAlarm = Map.of(ConfigConstants.CONVERTER, converterAlarm);
        Map<String, Object> propertiesTapi = Map.of(ConfigConstants.CONVERTER, converterTapiService);
        serializerService.configure(properties, false);
        serializerAlarm.configure(propertiesAlarm, false);
        serializerTapi.configure(propertiesTapi, false);
        mockProducer = new MockProducer<>(true, new StringSerializer(), serializerService);
        mockAlarmProducer = new MockProducer<>(true, new StringSerializer(), serializerAlarm);
        mockTapiProducer = new MockProducer<>(true, new StringSerializer(), serializerTapi);
        publisherService = new Publisher<>("test", mockProducer);
        publisherAlarm = new Publisher<>("test", mockAlarmProducer);
        publisherTapiService = new Publisher<>("test", mockTapiProducer);
        networkTransactionService = new NetworkTransactionImpl(getDataBroker());
        topicManager.setTapiConverter(converterTapiService);
        NotificationServiceDataUtils.createTapiContext(networkTransactionService);
        nbiNotifications = new NbiNotificationsProvider("localhost:8080", "localhost:8080",
                rpcProviderRegistry, notificationService, getDataStoreContextUtil().getBindingDOMCodecServices(),
                networkTransactionService);
    }

    @Test
    void sendEventServiceShouldBeSuccessful() throws IOException {
        String json = Files.readString(Path.of("src/test/resources/event.json"));
        NotificationProcessService notificationProcessService = converterService
                .createDataObjectFromJsonString(YangInstanceIdentifier.of(NotificationProcessService.QNAME),
                        json, JSONCodecFactorySupplier.RFC7951);
        publisherService.sendEvent(notificationProcessService, notificationProcessService.getConnectionType().name());
        assertEquals(1, mockProducer.history().size(), "We should have one message");
        assertEquals("test", mockProducer.history().get(0).key(), "Key should be test");
    }

    @Test
    void sendEventAlarmShouldBeSuccessful() throws IOException {
        String json = Files.readString(Path.of("src/test/resources/event_alarm_service.json"));
        NotificationAlarmService notificationAlarmService = converterAlarm
                .createDataObjectFromJsonString(YangInstanceIdentifier.of(NotificationAlarmService.QNAME),
                        json, JSONCodecFactorySupplier.RFC7951);
        publisherAlarm.sendEvent(notificationAlarmService, "alarm"
                + notificationAlarmService.getConnectionType().getName());
        assertEquals(1, mockAlarmProducer.history().size(), "We should have one message");
        assertEquals("test", mockAlarmProducer.history().get(0).key(), "Key should be test");
    }

    @Test
    void sendTapiEventShouldBeSuccessful() throws IOException {
        CreateNotificationSubscriptionServiceInputBuilder builder
            = NotificationServiceDataUtils.buildNotificationSubscriptionServiceInputBuilder();
        SubscriptionFilter subscriptionFilter = new SubscriptionFilterBuilder(builder.getSubscriptionFilter())
            .setRequestedObjectIdentifier(new HashSet<>(List.of(new Uuid("76d8f07b-ead5-4132-8eb8-cf3fdef7e079"))))
            .build();
        builder.setSubscriptionFilter(subscriptionFilter);

        new CreateNotificationSubscriptionServiceImpl(nbiNotifications, topicManager).invoke(builder.build());
        String json = Files.readString(Path.of("src/test/resources/tapi_event.json"));
        NotificationTapiService notificationTapiService = converterTapiService
            .createDataObjectFromJsonString(YangInstanceIdentifier.of(NotificationTapiService.QNAME),
                json, JSONCodecFactorySupplier.RFC7951);
        publisherTapiService.sendEvent(notificationTapiService, "");
        assertEquals(1, mockTapiProducer.history().size(), "We should have one message");
        assertEquals("test", mockTapiProducer.history().get(0).key(), "Key should be test");
    }
}
