/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.qpid.server.protocol.converter.v0_8_v1_0;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.io.ByteStreams;
import org.mockito.ArgumentCaptor;

import org.apache.qpid.server.bytebuffer.QpidByteBuffer;
import org.apache.qpid.server.model.NamedAddressSpace;
import org.apache.qpid.server.protocol.converter.MessageConversionException;
import org.apache.qpid.server.protocol.v0_10.transport.mimecontentconverter.AmqpListToListConverter;
import org.apache.qpid.server.protocol.v0_10.transport.mimecontentconverter.AmqpMapToMapConverter;
import org.apache.qpid.server.protocol.v0_10.transport.mimecontentconverter.ListToAmqpListConverter;
import org.apache.qpid.server.protocol.v0_10.transport.mimecontentconverter.MapToAmqpMapConverter;
import org.apache.qpid.server.protocol.v0_8.AMQMessage;
import org.apache.qpid.server.protocol.v1_0.MessageMetaData_1_0;
import org.apache.qpid.server.protocol.v1_0.Message_1_0;
import org.apache.qpid.server.protocol.v1_0.type.Binary;
import org.apache.qpid.server.protocol.v1_0.type.Symbol;
import org.apache.qpid.server.protocol.v1_0.type.messaging.AmqpSequence;
import org.apache.qpid.server.protocol.v1_0.type.messaging.AmqpValue;
import org.apache.qpid.server.protocol.v1_0.type.messaging.ApplicationProperties;
import org.apache.qpid.server.protocol.v1_0.type.messaging.Data;
import org.apache.qpid.server.protocol.v1_0.type.messaging.DeliveryAnnotations;
import org.apache.qpid.server.protocol.v1_0.type.messaging.EncodingRetainingSection;
import org.apache.qpid.server.protocol.v1_0.type.messaging.Footer;
import org.apache.qpid.server.protocol.v1_0.type.messaging.Header;
import org.apache.qpid.server.protocol.v1_0.type.messaging.MessageAnnotations;
import org.apache.qpid.server.protocol.v1_0.type.messaging.Properties;
import org.apache.qpid.server.protocol.v1_0.type.messaging.Source;
import org.apache.qpid.server.store.StoredMessage;
import org.apache.qpid.server.typedmessage.mimecontentconverter.JmsMapMessageToMap;
import org.apache.qpid.server.typedmessage.mimecontentconverter.JmsStreamMessageToList;
import org.apache.qpid.server.typedmessage.mimecontentconverter.ListToJmsStreamMessage;
import org.apache.qpid.server.typedmessage.mimecontentconverter.MapToJmsMapMessage;
import org.apache.qpid.server.util.ByteBufferUtils;
import org.apache.qpid.test.utils.QpidTestCase;

public class MessageConverter_1_0_to_v0_8Test extends QpidTestCase
{
    private static final MessageAnnotations MESSAGE_MESSAGE_ANNOTATION =
            new MessageAnnotations(Collections.singletonMap(Symbol.valueOf("x-opt-jms-msg-type"), (byte) 0));
    private static final MessageAnnotations OBJECT_MESSAGE_MESSAGE_ANNOTATION =
            new MessageAnnotations(Collections.singletonMap(Symbol.valueOf("x-opt-jms-msg-type"), (byte) 1));
    private static final MessageAnnotations MAP_MESSAGE_MESSAGE_ANNOTATION =
            new MessageAnnotations(Collections.singletonMap(Symbol.valueOf("x-opt-jms-msg-type"), (byte) 2));
    private static final MessageAnnotations BYTE_MESSAGE_MESSAGE_ANNOTATION =
            new MessageAnnotations(Collections.singletonMap(Symbol.valueOf("x-opt-jms-msg-type"), (byte) 3));
    private static final MessageAnnotations STREAM_MESSAGE_MESSAGE_ANNOTATION =
            new MessageAnnotations(Collections.singletonMap(Symbol.valueOf("x-opt-jms-msg-type"), (byte) 4));
    private static final MessageAnnotations TEXT_MESSAGE_MESSAGE_ANNOTATION =
            new MessageAnnotations(Collections.singletonMap(Symbol.valueOf("x-opt-jms-msg-type"), (byte) 5));
    private MessageConverter_1_0_to_v0_8 _converter;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        _converter = new MessageConverter_1_0_to_v0_8();
    }

    public void testAmqpValueWithNullWithTextMessageAnnotation() throws Exception
    {
        final Object expected = null;
        final AmqpValue amqpValue = new AmqpValue(expected);
        Message_1_0 sourceMessage =
                createTestMessage(TEXT_MESSAGE_MESSAGE_ANNOTATION, amqpValue.createEncodingRetainingSection());

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        assertEquals("Unexpected mime type", "text/plain", convertedMessage.getMessageHeader().getMimeType());
        assertEquals("Unexpected content size", 0, convertedMessage.getMessageMetaData().getContentSize());
    }

    public void testAmqpValueWithNullWithMessageAnnotation() throws Exception
    {
        final Object expected = null;
        final AmqpValue amqpValue = new AmqpValue(expected);
        Message_1_0 sourceMessage =
                createTestMessage(MESSAGE_MESSAGE_ANNOTATION, amqpValue.createEncodingRetainingSection());

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        assertEquals("Unexpected mime type", null, convertedMessage.getMessageHeader().getMimeType());
        assertEquals("Unexpected content size", 0, convertedMessage.getMessageMetaData().getContentSize());
    }

    public void testAmqpValueWithNullWithObjectMessageAnnotation() throws Exception
    {
        final Object expected = null;
        final AmqpValue amqpValue = new AmqpValue(expected);
        Message_1_0 sourceMessage = createTestMessage(OBJECT_MESSAGE_MESSAGE_ANNOTATION, amqpValue.createEncodingRetainingSection());

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        final Collection<QpidByteBuffer> content = convertedMessage.getContent(0, (int) convertedMessage.getSize());

        assertEquals("Unexpected mime type",
                     "application/java-object-stream",
                     convertedMessage.getMessageHeader().getMimeType());
        assertArrayEquals("Unexpected content size", getObjectBytes(null), getBytes(content));
    }

    public void testAmqpValueWithNullWithMapMessageAnnotation() throws Exception
    {
        final Object expected = null;
        final AmqpValue amqpValue = new AmqpValue(expected);
        Message_1_0 sourceMessage = createTestMessage(MAP_MESSAGE_MESSAGE_ANNOTATION, amqpValue.createEncodingRetainingSection());

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        final Collection<QpidByteBuffer> content = convertedMessage.getContent(0, (int) convertedMessage.getSize());

        assertEquals("Unexpected mime type", "jms/map-message", convertedMessage.getMessageHeader().getMimeType());
        assertArrayEquals("Unexpected content size",
                          new MapToJmsMapMessage().toMimeContent(Collections.emptyMap()),
                          getBytes(content));
    }

    public void testAmqpValueWithNullWithBytesMessageAnnotation() throws Exception
    {
        final Object expected = null;
        final AmqpValue amqpValue = new AmqpValue(expected);
        Message_1_0 sourceMessage = createTestMessage(BYTE_MESSAGE_MESSAGE_ANNOTATION, amqpValue.createEncodingRetainingSection());

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        assertEquals("Unexpected mime type",
                     "application/octet-stream",
                     convertedMessage.getMessageHeader().getMimeType());
        assertEquals("Unexpected content size", 0, convertedMessage.getMessageMetaData().getContentSize());
    }

    public void testAmqpValueWithNullWithStreamMessageAnnotation() throws Exception
    {
        final Object expected = null;
        final AmqpValue amqpValue = new AmqpValue(expected);
        Message_1_0 sourceMessage = createTestMessage(STREAM_MESSAGE_MESSAGE_ANNOTATION, amqpValue.createEncodingRetainingSection());

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        assertEquals("Unexpected mime type", "jms/stream-message", convertedMessage.getMessageHeader().getMimeType());
        assertEquals("Unexpected content size", 0, convertedMessage.getMessageMetaData().getContentSize());
    }

    public void testAmqpValueWithNullWithUnknownMessageAnnotation() throws Exception
    {
        final Object expected = null;
        final AmqpValue amqpValue = new AmqpValue(expected);
        Message_1_0 sourceMessage =
                createTestMessage(new MessageAnnotations(Collections.singletonMap(Symbol.valueOf("x-opt-jms-msg-type"),
                                                                                  (byte) 11)),
                                  amqpValue.createEncodingRetainingSection());

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        assertEquals("Unexpected mime type", null, convertedMessage.getMessageHeader().getMimeType());
        assertEquals("Unexpected content size", 0, convertedMessage.getMessageMetaData().getContentSize());
    }

    public void testAmqpValueWithNullWithContentTypeApplicationOctetStream() throws Exception
    {
        Properties properties = new Properties();
        properties.setContentType(Symbol.valueOf("application/octet-stream"));
        final Object expected = null;
        final AmqpValue amqpValue = new AmqpValue(expected);
        Message_1_0 sourceMessage = createTestMessage(properties, amqpValue.createEncodingRetainingSection());

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        assertEquals("Unexpected mime type", "application/octet-stream", convertedMessage.getMessageHeader().getMimeType());
        assertEquals("Unexpected content size", 0, convertedMessage.getMessageMetaData().getContentSize());
    }

    public void testAmqpValueWithNullWithObjectMessageContentType() throws Exception
    {
        final Properties properties = new Properties();
        properties.setContentType(Symbol.valueOf("application/x-java-serialized-object"));
        final Object expected = null;
        final AmqpValue amqpValue = new AmqpValue(expected);
        Message_1_0 sourceMessage = createTestMessage(properties, amqpValue.createEncodingRetainingSection());

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        assertEquals("Unexpected mime type",
                     "application/java-object-stream",
                     convertedMessage.getMessageHeader().getMimeType());

        assertEquals("Unexpected content size",
                     getObjectBytes(null).length,
                     convertedMessage.getMessageMetaData().getContentSize());
    }

    public void testAmqpValueWithNullWithJmsMapContentType() throws Exception
    {
        final Properties properties = new Properties();
        properties.setContentType(Symbol.valueOf("jms/map-message"));
        final Object expected = null;
        final AmqpValue amqpValue = new AmqpValue(expected);
        Message_1_0 sourceMessage = createTestMessage(properties, amqpValue.createEncodingRetainingSection());

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        final Collection<QpidByteBuffer> content = convertedMessage.getContent(0, (int) convertedMessage.getSize());

        assertEquals("Unexpected mime type", "jms/map-message", convertedMessage.getMessageHeader().getMimeType());

        assertArrayEquals("Unexpected content size",
                          new MapToJmsMapMessage().toMimeContent(Collections.emptyMap()),
                          getBytes(content));
    }

    public void testAmqpValueWithNull() throws Exception
    {
        final Object expected = null;
        final AmqpValue amqpValue = new AmqpValue(expected);
        Message_1_0 sourceMessage = createTestMessage(amqpValue.createEncodingRetainingSection());

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        assertEquals("Unexpected mime type", null, convertedMessage.getMessageHeader().getMimeType());
        assertEquals("Unexpected content size", 0, convertedMessage.getMessageMetaData().getContentSize());
    }

    public void testAmqpValueWithString() throws Exception
    {
        final String expected = "testContent";
        final AmqpValue amqpValue = new AmqpValue(expected);
        Message_1_0 sourceMessage = createTestMessage(amqpValue.createEncodingRetainingSection());

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        assertEquals("Unexpected mime type", "text/plain", convertedMessage.getMessageHeader().getMimeType());
        final Collection<QpidByteBuffer> content = convertedMessage.getContent(0, (int) convertedMessage.getSize());
        assertEquals("Unexpected content", expected, new String(getBytes(content), UTF_8));
    }

    public void testAmqpValueWithStringWithTextMessageAnnotation() throws Exception
    {
        final String expected = "testContent";
        final AmqpValue amqpValue = new AmqpValue(expected);
        Message_1_0 sourceMessage =
                createTestMessage(TEXT_MESSAGE_MESSAGE_ANNOTATION, amqpValue.createEncodingRetainingSection());

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        assertEquals("Unexpected mime type", "text/plain", convertedMessage.getMessageHeader().getMimeType());
        final Collection<QpidByteBuffer> content = convertedMessage.getContent(0, (int) convertedMessage.getSize());
        assertEquals("Unexpected content", expected, new String(getBytes(content), UTF_8));
    }

    public void testAmqpValueWithMap() throws Exception
    {
        final Map<String, Object> originalMap = new LinkedHashMap<>();
        originalMap.put("binaryEntry", new Binary(new byte[]{0x00, (byte) 0xFF}));
        originalMap.put("intEntry", 42);
        originalMap.put("nullEntry", null);
        final AmqpValue amqpValue = new AmqpValue(originalMap);
        Message_1_0 sourceMessage = createTestMessage(amqpValue.createEncodingRetainingSection());

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        assertEquals("Unexpected mime type", "jms/map-message", convertedMessage.getMessageHeader().getMimeType());
        final Collection<QpidByteBuffer> content = convertedMessage.getContent(0, (int) convertedMessage.getSize());

        Map<String, Object> convertedMap = new JmsMapMessageToMap().toObject(getBytes(content));
        assertEquals("Unexpected size", originalMap.size(), convertedMap.size());
        assertArrayEquals("Unexpected binary entry", ((Binary) originalMap.get("binaryEntry")).getArray(),
                          (byte[]) convertedMap.get("binaryEntry"));
        assertEquals("Unexpected int entry", originalMap.get("intEntry"), convertedMap.get("intEntry"));
        assertEquals("Unexpected null entry", originalMap.get("nullEntry"), convertedMap.get("nullEntry"));
    }

    public void testAmqpValueWithMapContainingMap() throws Exception
    {
        final Map<String, Object> originalMap =
                Collections.singletonMap("testMap", Collections.singletonMap("innerKey", "testValue"));

        final AmqpValue amqpValue = new AmqpValue(originalMap);
        Message_1_0 sourceMessage = createTestMessage(amqpValue.createEncodingRetainingSection());

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        assertEquals("Unexpected mime type", "amqp/map", convertedMessage.getMessageHeader().getMimeType());
        final Collection<QpidByteBuffer> content = convertedMessage.getContent(0, (int) convertedMessage.getSize());

        Map<String, Object> convertedMap = new AmqpMapToMapConverter().toObject(getBytes(content));
        assertEquals("Unexpected size", originalMap.size(), convertedMap.size());
        assertEquals("Unexpected binary entry", new HashMap((Map<String, Object>) originalMap.get("testMap")),
                     new HashMap((Map<String, Object>) convertedMap.get("testMap")));
    }

    public void testAmqpValueWithMapContainingNonFieldTableCompliantEntries() throws Exception
    {
        final AmqpValue amqpValue = new AmqpValue(Collections.<Object, Object>singletonMap(13, 42));
        Message_1_0 sourceMessage = createTestMessage(amqpValue.createEncodingRetainingSection());

        try
        {
            _converter.convert(sourceMessage, mock(NamedAddressSpace.class));
            fail("expected exception not thrown.");
        }
        catch (MessageConversionException e)
        {
            // pass
        }
    }

    public void testAmqpValueWithList() throws Exception
    {
        final List<Object> originalList = new ArrayList<>();
        originalList.add(new Binary(new byte[]{0x00, (byte) 0xFF}));
        originalList.add(42);
        originalList.add(null);
        final AmqpValue amqpValue = new AmqpValue(originalList);
        Message_1_0 sourceMessage = createTestMessage(amqpValue.createEncodingRetainingSection());

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        assertEquals("Unexpected mime type", "jms/stream-message", convertedMessage.getMessageHeader().getMimeType());
        final Collection<QpidByteBuffer> content = convertedMessage.getContent(0, (int) convertedMessage.getSize());

        List<Object> convertedList = new JmsStreamMessageToList().toObject(getBytes(content));
        assertEquals("Unexpected size", originalList.size(), convertedList.size());
        assertArrayEquals("Unexpected binary item", ((Binary) originalList.get(0)).getArray(),
                          (byte[]) convertedList.get(0));
        assertEquals("Unexpected int item", originalList.get(1), convertedList.get(1));
        assertEquals("Unexpected null item", originalList.get(2), convertedList.get(2));
    }

    public void testAmqpValueWithListContainingMap() throws Exception
    {
        final List<Object> originalList = Collections.singletonList(Collections.singletonMap("testKey", "testValue"));
        final AmqpValue amqpValue = new AmqpValue(originalList);
        Message_1_0 sourceMessage = createTestMessage(amqpValue.createEncodingRetainingSection());

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        assertEquals("Unexpected mime type", "amqp/list", convertedMessage.getMessageHeader().getMimeType());
        final Collection<QpidByteBuffer> content = convertedMessage.getContent(0, (int) convertedMessage.getSize());

        List<Object> convertedList = new AmqpListToListConverter().toObject(getBytes(content));
        assertEquals("Unexpected size", originalList.size(), convertedList.size());
        assertEquals("Unexpected map item", new HashMap<String, Object>((Map) originalList.get(0)),
                     new HashMap<String, Object>((Map) convertedList.get(0)));
    }

    public void testAmqpValueWithListContainingUnsupportedType() throws Exception
    {
        final List<Object> originalList = Collections.singletonList(new Source());
        final AmqpValue amqpValue = new AmqpValue(originalList);
        Message_1_0 sourceMessage = createTestMessage(amqpValue.createEncodingRetainingSection());

        try
        {
            _converter.convert(sourceMessage, mock(NamedAddressSpace.class));
            fail("expected exception not thrown.");
        }
        catch (MessageConversionException e)
        {
            // pass
        }
    }

    public void testAmqpValueWithUnsupportedType() throws Exception
    {
        final Integer originalValue = 42;
        final AmqpValue amqpValue = new AmqpValue(originalValue);
        Message_1_0 sourceMessage = createTestMessage(amqpValue.createEncodingRetainingSection());

        try
        {
            _converter.convert(sourceMessage, mock(NamedAddressSpace.class));
            fail("expected exception not thrown.");
        }
        catch (MessageConversionException e)
        {
            // pass
        }
    }

    public void testAmqpSequenceWithSimpleTypes() throws Exception
    {
        final List<Integer> expected = new ArrayList<>();
        expected.add(37);
        expected.add(42);
        final AmqpSequence amqpSequence = new AmqpSequence(expected);
        Message_1_0 sourceMessage = createTestMessage(amqpSequence.createEncodingRetainingSection());

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        assertEquals("Unexpected mime type", "jms/stream-message", convertedMessage.getMessageHeader().getMimeType());
        final Collection<QpidByteBuffer> content = convertedMessage.getContent(0, (int) convertedMessage.getSize());
        assertEquals("Unexpected content", expected, new JmsStreamMessageToList().toObject(getBytes(content)));
    }

    public void testAmqpSequenceWithMap() throws Exception
    {
        final List<Object> originalList = Collections.singletonList(Collections.singletonMap("testKey", "testValue"));
        final AmqpSequence amqpSequence = new AmqpSequence(originalList);
        Message_1_0 sourceMessage = createTestMessage(amqpSequence.createEncodingRetainingSection());

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        assertEquals("Unexpected mime type", "amqp/list", convertedMessage.getMessageHeader().getMimeType());
        final Collection<QpidByteBuffer> content = convertedMessage.getContent(0, (int) convertedMessage.getSize());

        List<Object> convertedList = new AmqpListToListConverter().toObject(getBytes(content));
        assertEquals("Unexpected size", originalList.size(), convertedList.size());
        assertEquals("Unexpected map item", new HashMap<String, Object>((Map) originalList.get(0)),
                     new HashMap<String, Object>((Map) convertedList.get(0)));
    }

    public void testAmqpSequenceWithUnsupportedType() throws Exception
    {
        final List<Object> originalList = Collections.singletonList(new Source());
        final AmqpSequence amqpSequence = new AmqpSequence(originalList);
        Message_1_0 sourceMessage = createTestMessage(amqpSequence.createEncodingRetainingSection());

        try
        {
            _converter.convert(sourceMessage, mock(NamedAddressSpace.class));
            fail("expected exception not thrown.");
        }
        catch (MessageConversionException e)
        {
            // pass
        }
    }

    public void testDataWithMessageAnnotation() throws Exception
    {
        doTestDataWithAnnotation("helloworld".getBytes(UTF_8), MESSAGE_MESSAGE_ANNOTATION, "application/octet-stream");
    }

    public void testDataWithObjectMessageAnnotation() throws Exception
    {
        byte[] bytes = "helloworld".getBytes(UTF_8);
        final byte[] expected = getObjectBytes(bytes);
        doTestDataWithAnnotation(expected, OBJECT_MESSAGE_MESSAGE_ANNOTATION, "application/java-object-stream");
    }

    public void testDataWithMapMessageAnnotation() throws Exception
    {
        doTestDataWithAnnotation("helloworld".getBytes(UTF_8),
                                 MAP_MESSAGE_MESSAGE_ANNOTATION,
                                 "application/octet-stream");
    }

    public void testDataWithMapMessageAnnotationAndContentTypeJmsMapMessage() throws Exception
    {
        Map<String, Object> originalMap = Collections.singletonMap("testKey", "testValue");
        byte[] data = new MapToJmsMapMessage().toMimeContent(originalMap);
        String expectedMimeType = "jms/map-message";
        final Data value = new Data(new Binary(data));
        Properties properties = new Properties();
        properties.setContentType(Symbol.valueOf(expectedMimeType));
        Message_1_0 sourceMessage = createTestMessage(properties,
                                                      MAP_MESSAGE_MESSAGE_ANNOTATION,
                                                      value.createEncodingRetainingSection());

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        assertEquals("Unexpected mime type",
                     expectedMimeType, convertedMessage.getMessageHeader().getMimeType());
        final Collection<QpidByteBuffer> content = convertedMessage.getContent(0, (int) convertedMessage.getSize());
        assertArrayEquals("Unexpected content", data, getBytes(content));
    }

    public void testDataWithMapMessageAnnotationAndContentTypeAmqpMap() throws Exception
    {
        Map<String, Object> originalMap = Collections.singletonMap("testKey", "testValue");
        byte[] data = new MapToAmqpMapConverter().toMimeContent(originalMap);
        String expectedMimeType = "amqp/map";
        final Data value = new Data(new Binary(data));
        Properties properties = new Properties();
        properties.setContentType(Symbol.valueOf(expectedMimeType));
        Message_1_0 sourceMessage = createTestMessage(properties,
                                                      MAP_MESSAGE_MESSAGE_ANNOTATION,
                                                      value.createEncodingRetainingSection());

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        assertEquals("Unexpected mime type",
                     expectedMimeType, convertedMessage.getMessageHeader().getMimeType());
        final Collection<QpidByteBuffer> content = convertedMessage.getContent(0, (int) convertedMessage.getSize());
        assertArrayEquals("Unexpected content", data, getBytes(content));
    }

    public void testDataWithBytesMessageAnnotation() throws Exception
    {
        doTestDataWithAnnotation("helloworld".getBytes(UTF_8),
                                 BYTE_MESSAGE_MESSAGE_ANNOTATION,
                                 "application/octet-stream");
    }

    public void testDataWithStreamMessageAnnotation() throws Exception
    {
        doTestDataWithAnnotation("helloworld".getBytes(UTF_8), STREAM_MESSAGE_MESSAGE_ANNOTATION,
                                 "application/octet-stream");
    }

    public void testDataWithStreamMessageAnnotationAndContentTypeJmsStreamMessage() throws Exception
    {
        List<Object> originalList = Collections.singletonList("testValue");
        byte[] data = new ListToJmsStreamMessage().toMimeContent(originalList);
        String expectedMimeType = "jms/stream-message";
        final Data value = new Data(new Binary(data));
        Properties properties = new Properties();
        properties.setContentType(Symbol.valueOf(expectedMimeType));
        Message_1_0 sourceMessage = createTestMessage(properties,
                                                      STREAM_MESSAGE_MESSAGE_ANNOTATION,
                                                      value.createEncodingRetainingSection());

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        assertEquals("Unexpected mime type",
                     expectedMimeType, convertedMessage.getMessageHeader().getMimeType());
        final Collection<QpidByteBuffer> content = convertedMessage.getContent(0, (int) convertedMessage.getSize());
        assertArrayEquals("Unexpected content", data, getBytes(content));
    }

    public void testDataWithStreamMessageAnnotationAndContentTypeAmqpList() throws Exception
    {
        List<Object> originalList = Collections.singletonList("testValue");
        byte[] data = new ListToAmqpListConverter().toMimeContent(originalList);
        String expectedMimeType = "amqp/list";
        final Data value = new Data(new Binary(data));
        Properties properties = new Properties();
        properties.setContentType(Symbol.valueOf(expectedMimeType));
        Message_1_0 sourceMessage = createTestMessage(properties,
                                                      STREAM_MESSAGE_MESSAGE_ANNOTATION,
                                                      value.createEncodingRetainingSection());

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        assertEquals("Unexpected mime type",
                     expectedMimeType, convertedMessage.getMessageHeader().getMimeType());
        final Collection<QpidByteBuffer> content = convertedMessage.getContent(0, (int) convertedMessage.getSize());
        assertArrayEquals("Unexpected content", data, getBytes(content));
    }

    public void testDataWithTextMessageAnnotation() throws Exception
    {
        doTestDataWithAnnotation("helloworld".getBytes(UTF_8), TEXT_MESSAGE_MESSAGE_ANNOTATION, "text/plain");
    }

    public void testDataWithUnsupportedMessageAnnotation() throws Exception
    {
        doTestDataWithAnnotation("helloworld".getBytes(UTF_8),
                                 new MessageAnnotations(Collections.singletonMap(Symbol.valueOf("x-opt-jms-msg-type"),
                                                                                 (byte) 11)),
                                 "application/octet-stream");
    }

    public void testDataWithContentTypeText() throws Exception
    {
        doTestConvertOfDataSectionForTextualType("text/foobar");
    }

    public void testDataWithContentTypeApplicationXml() throws Exception
    {
        doTestConvertOfDataSectionForTextualType("application/xml");
    }

    public void testDataWithContentTypeApplicationXmlDtd() throws Exception
    {
        doTestConvertOfDataSectionForTextualType("application/xml-dtd");
    }

    public void testDataWithContentTypeApplicationFooXml() throws Exception
    {
        doTestConvertOfDataSectionForTextualType("application/foo+xml");
    }

    public void testDataWithContentTypeApplicationJson() throws Exception
    {
        doTestConvertOfDataSectionForTextualType("application/json");
    }

    public void testDataWithContentTypeApplicationFooJson() throws Exception
    {
        doTestConvertOfDataSectionForTextualType("application/foo+json");
    }

    public void testDataWithContentTypeApplicationJavascript() throws Exception
    {
        doTestConvertOfDataSectionForTextualType("application/javascript");
    }

    public void testDataWithContentTypeApplicationEcmascript() throws Exception
    {
        doTestConvertOfDataSectionForTextualType("application/ecmascript");
    }

    public void testDataWithContentTypeAmqpMap() throws Exception
    {
        Map<String, Object> originalMap = Collections.singletonMap("testKey", "testValue");
        byte[] bytes = new MapToAmqpMapConverter().toMimeContent(originalMap);

        final Data value = new Data(new Binary(bytes));
        Properties properties = new Properties();
        properties.setContentType(Symbol.valueOf("amqp/map"));
        Message_1_0 sourceMessage = createTestMessage(properties, value.createEncodingRetainingSection());

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        assertEquals("Unexpected mime type", "amqp/map", convertedMessage.getMessageHeader().getMimeType());
        final Collection<QpidByteBuffer> content = convertedMessage.getContent(0, (int) convertedMessage.getSize());
        assertArrayEquals("Unexpected content", bytes, getBytes(content));
    }

    public void testDataWithContentTypeJmsMapMessage() throws Exception
    {
        Map<String, Object> originalMap = Collections.singletonMap("testKey", "testValue");
        byte[] bytes = new MapToJmsMapMessage().toMimeContent(originalMap);

        final Data value = new Data(new Binary(bytes));
        Properties properties = new Properties();
        properties.setContentType(Symbol.valueOf("jms/map-message"));
        Message_1_0 sourceMessage = createTestMessage(properties, value.createEncodingRetainingSection());

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        assertEquals("Unexpected mime type", "jms/map-message", convertedMessage.getMessageHeader().getMimeType());
        final Collection<QpidByteBuffer> content = convertedMessage.getContent(0, (int) convertedMessage.getSize());
        assertArrayEquals("Unexpected content", bytes, getBytes(content));
    }

    public void testDataWithContentTypeAmqpList() throws Exception
    {
        List<Object> originalMap = Collections.singletonList("testValue");
        byte[] bytes = new ListToAmqpListConverter().toMimeContent(originalMap);

        final Data value = new Data(new Binary(bytes));
        Properties properties = new Properties();
        properties.setContentType(Symbol.valueOf("amqp/list"));
        Message_1_0 sourceMessage = createTestMessage(properties, value.createEncodingRetainingSection());

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        assertEquals("Unexpected mime type", "amqp/list", convertedMessage.getMessageHeader().getMimeType());
        final Collection<QpidByteBuffer> content = convertedMessage.getContent(0, (int) convertedMessage.getSize());
        assertArrayEquals("Unexpected content", bytes, getBytes(content));
    }

    public void testDataWithContentTypeJmsStreamMessage() throws Exception
    {
        List<Object> originalMap = Collections.singletonList("testValue");
        byte[] bytes = new ListToJmsStreamMessage().toMimeContent(originalMap);

        final Data value = new Data(new Binary(bytes));
        Properties properties = new Properties();
        properties.setContentType(Symbol.valueOf("jms/stream-message"));
        Message_1_0 sourceMessage = createTestMessage(properties, value.createEncodingRetainingSection());

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        assertEquals("Unexpected mime type", "jms/stream-message", convertedMessage.getMessageHeader().getMimeType());
        final Collection<QpidByteBuffer> content = convertedMessage.getContent(0, (int) convertedMessage.getSize());
        assertArrayEquals("Unexpected content", bytes, getBytes(content));
    }

    public void testDataWithContentTypeJavaSerializedObject() throws Exception
    {
        final byte[] expected = getObjectBytes("helloworld".getBytes(UTF_8));
        final Data value = new Data(new Binary(expected));
        Properties properties = new Properties();
        properties.setContentType(Symbol.valueOf("application/x-java-serialized-object"));
        Message_1_0 sourceMessage = createTestMessage(properties, value.createEncodingRetainingSection());

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        assertEquals("Unexpected mime type",
                     "application/java-object-stream",
                     convertedMessage.getMessageHeader().getMimeType());
        final Collection<QpidByteBuffer> content = convertedMessage.getContent(0, (int) convertedMessage.getSize());
        assertArrayEquals("Unexpected content", expected, getBytes(content));
    }


    public void testDataWithContentTypeJavaObjectStream() throws Exception
    {
        final byte[] expected = getObjectBytes("helloworld".getBytes(UTF_8));
        final Data value = new Data(new Binary(expected));
        Properties properties = new Properties();
        properties.setContentType(Symbol.valueOf("application/java-object-stream"));
        Message_1_0 sourceMessage = createTestMessage(properties, value.createEncodingRetainingSection());

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        assertEquals("Unexpected mime type",
                     "application/java-object-stream",
                     convertedMessage.getMessageHeader().getMimeType());
        final Collection<QpidByteBuffer> content = convertedMessage.getContent(0, (int) convertedMessage.getSize());
        assertArrayEquals("Unexpected content", expected, getBytes(content));
    }

    public void testDataWithContentTypeOther() throws Exception
    {
        final byte[] expected = "helloworld".getBytes(UTF_8);
        final Data value = new Data(new Binary(expected));
        final Properties properties = new Properties();
        properties.setContentType(Symbol.valueOf("application/bin"));
        Message_1_0 sourceMessage = createTestMessage(properties, value.createEncodingRetainingSection());

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        assertEquals("Unexpected mime type",
                     "application/octet-stream",
                     convertedMessage.getMessageHeader().getMimeType());
        final Collection<QpidByteBuffer> content = convertedMessage.getContent(0, (int) convertedMessage.getSize());
        assertArrayEquals("Unexpected content", expected, getBytes(content));
    }

    public void testData() throws Exception
    {
        final byte[] expected = getObjectBytes("helloworld".getBytes(UTF_8));
        final Data value = new Data(new Binary(expected));
        final Message_1_0 sourceMessage = createTestMessage(value.createEncodingRetainingSection());

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        assertEquals("Unexpected mime type",
                     "application/octet-stream",
                     convertedMessage.getMessageHeader().getMimeType());
        final Collection<QpidByteBuffer> content = convertedMessage.getContent(0, (int) convertedMessage.getSize());
        assertArrayEquals("Unexpected content", expected, getBytes(content));
    }

    public void testNoBodyWithMessageAnnotation() throws Exception
    {
        Message_1_0 sourceMessage = createTestMessage(MESSAGE_MESSAGE_ANNOTATION, null);

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        assertEquals("Unexpected mime type", null, convertedMessage.getMessageHeader().getMimeType());
        assertEquals("Unexpected content size", 0, convertedMessage.getMessageMetaData().getContentSize());
    }

    public void testNoBodyWithObjectMessageAnnotation() throws Exception
    {
        Message_1_0 sourceMessage = createTestMessage(OBJECT_MESSAGE_MESSAGE_ANNOTATION, null);

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        final Collection<QpidByteBuffer> content = convertedMessage.getContent(0, (int) convertedMessage.getSize());

        assertEquals("Unexpected mime type",
                     "application/java-object-stream",
                     convertedMessage.getMessageHeader().getMimeType());
        assertArrayEquals("Unexpected content size", getObjectBytes(null), getBytes(content));
    }

    public void testNoBodyWithMapMessageAnnotation() throws Exception
    {
        Message_1_0 sourceMessage = createTestMessage(MAP_MESSAGE_MESSAGE_ANNOTATION, null);

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        final Collection<QpidByteBuffer> content = convertedMessage.getContent(0, (int) convertedMessage.getSize());

        assertEquals("Unexpected mime type", "jms/map-message", convertedMessage.getMessageHeader().getMimeType());
        assertArrayEquals("Unexpected content size",
                          new MapToJmsMapMessage().toMimeContent(Collections.emptyMap()),
                          getBytes(content));
    }

    public void testNoBodyWithBytesMessageAnnotation() throws Exception
    {
        Message_1_0 sourceMessage = createTestMessage(BYTE_MESSAGE_MESSAGE_ANNOTATION, null);

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        assertEquals("Unexpected mime type",
                     "application/octet-stream",
                     convertedMessage.getMessageHeader().getMimeType());
        assertEquals("Unexpected content size", 0, convertedMessage.getMessageMetaData().getContentSize());
    }

    public void testNoBodyWithStreamMessageAnnotation() throws Exception
    {
        Message_1_0 sourceMessage = createTestMessage(STREAM_MESSAGE_MESSAGE_ANNOTATION, null);

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        assertEquals("Unexpected mime type", "jms/stream-message", convertedMessage.getMessageHeader().getMimeType());
        assertEquals("Unexpected content size", 0, convertedMessage.getMessageMetaData().getContentSize());
    }

    public void testNoBodyWithTextMessageAnnotation() throws Exception
    {
        Message_1_0 sourceMessage = createTestMessage(TEXT_MESSAGE_MESSAGE_ANNOTATION, null);

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        assertEquals("Unexpected mime type", "text/plain", convertedMessage.getMessageHeader().getMimeType());
        assertEquals("Unexpected content size", 0, convertedMessage.getMessageMetaData().getContentSize());
    }

    public void testNoBodyWithUnknownMessageAnnotation() throws Exception
    {
        Message_1_0 sourceMessage =
                createTestMessage(new MessageAnnotations(Collections.singletonMap(Symbol.valueOf("x-opt-jms-msg-type"),
                                                                                  (byte) 11)), null);

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        assertEquals("Unexpected mime type", null, convertedMessage.getMessageHeader().getMimeType());
        assertEquals("Unexpected content size", 0, convertedMessage.getMessageMetaData().getContentSize());
    }

    public void testNoBody() throws Exception
    {
        final Message_1_0 sourceMessage = createTestMessage(null);

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        assertEquals("Unexpected mime type", null, convertedMessage.getMessageHeader().getMimeType());
        assertEquals("Unexpected content size", 0, convertedMessage.getMessageMetaData().getContentSize());
    }

    public void testNoBodyWithContentTypeApplicationOctetStream() throws Exception
    {
        Properties properties = new Properties();
        properties.setContentType(Symbol.valueOf("application/octet-stream"));
        final Message_1_0 sourceMessage = createTestMessage(properties, null);

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        assertEquals("Unexpected mime type", "application/octet-stream", convertedMessage.getMessageHeader().getMimeType());
        assertEquals("Unexpected content size", 0, convertedMessage.getMessageMetaData().getContentSize());
    }

    public void testNoBodyWithObjectMessageContentType() throws Exception
    {
        final Properties properties = new Properties();
        properties.setContentType(Symbol.valueOf("application/x-java-serialized-object"));
        final Message_1_0 sourceMessage = createTestMessage(properties, null);

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        assertEquals("Unexpected mime type",
                     "application/java-object-stream",
                     convertedMessage.getMessageHeader().getMimeType());

        assertEquals("Unexpected content size",
                     getObjectBytes(null).length,
                     convertedMessage.getMessageMetaData().getContentSize());
    }

    public void testNoBodyWithJmsMapContentType() throws Exception
    {
        final Properties properties = new Properties();
        properties.setContentType(Symbol.valueOf("jms/map-message"));
        final Message_1_0 sourceMessage = createTestMessage(properties, null);

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        final Collection<QpidByteBuffer> content = convertedMessage.getContent(0, (int) convertedMessage.getSize());

        assertEquals("Unexpected mime type", "jms/map-message", convertedMessage.getMessageHeader().getMimeType());

        assertArrayEquals("Unexpected content size",
                          new MapToJmsMapMessage().toMimeContent(Collections.emptyMap()),
                          getBytes(content));
    }

    public void testMessageAnnotationTakesPrecedenceOverContentType() throws Exception
    {
        final Properties properties = new Properties();
        properties.setContentType(Symbol.valueOf("application/octet-stream"));
        final Message_1_0 sourceMessage = createTestMessage(OBJECT_MESSAGE_MESSAGE_ANNOTATION, null);

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        assertEquals("Unexpected mime type",
                     "application/java-object-stream",
                     convertedMessage.getMessageHeader().getMimeType());
        assertEquals("Unexpected content size",
                     getObjectBytes(null).length,
                     convertedMessage.getMessageMetaData().getContentSize());
    }

    private void doTestConvertOfDataSectionForTextualType(final String contentType) throws Exception
    {
        final String expected = "testContent";
        final Data value = new Data(new Binary(expected.getBytes(UTF_8)));
        final Properties properties = new Properties();
        properties.setContentType(Symbol.valueOf(contentType));
        Message_1_0 sourceMessage = createTestMessage(properties, value.createEncodingRetainingSection());

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        final Collection<QpidByteBuffer> content = convertedMessage.getContent(0, (int) convertedMessage.getSize());
        assertEquals("Unexpected content", expected, new String(getBytes(content), UTF_8));

        assertEquals("Unexpected mime type", "text/plain", convertedMessage.getMessageHeader().getMimeType());
    }

    private byte[] getBytes(final Collection<QpidByteBuffer> content) throws Exception
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (QpidByteBuffer buf : content)
        {
            ByteStreams.copy(buf.asInputStream(), bos);
            buf.dispose();
        }
        return bos.toByteArray();
    }

    private Message_1_0 createTestMessage(final EncodingRetainingSection encodingRetainingSection)
    {
        return createTestMessage(new Properties(), encodingRetainingSection);
    }

    private Message_1_0 createTestMessage(final Properties properties, final EncodingRetainingSection section)
    {
        return createTestMessage(new Header(),
                                 new DeliveryAnnotations(Collections.emptyMap()),
                                 new MessageAnnotations(Collections.emptyMap()),
                                 properties,
                                 new ApplicationProperties(Collections.emptyMap()),
                                 0,
                                 section);
    }

    private Message_1_0 createTestMessage(final Properties properties,
                                          final MessageAnnotations messageAnnotations,
                                          final EncodingRetainingSection section)
    {
        return createTestMessage(new Header(),
                                 new DeliveryAnnotations(Collections.emptyMap()),
                                 messageAnnotations,
                                 properties,
                                 new ApplicationProperties(Collections.emptyMap()),
                                 0,
                                 section);
    }

    private Message_1_0 createTestMessage(final MessageAnnotations messageAnnotations,
                                          final EncodingRetainingSection section)
    {
        return createTestMessage(new Header(),
                                 new DeliveryAnnotations(Collections.emptyMap()),
                                 messageAnnotations,
                                 new Properties(),
                                 new ApplicationProperties(Collections.emptyMap()),
                                 0,
                                 section);
    }

    private Message_1_0 createTestMessage(final Header header,
                                          final DeliveryAnnotations deliveryAnnotations,
                                          final MessageAnnotations messageAnnotations,
                                          final Properties properties,
                                          final ApplicationProperties applicationProperties,
                                          final long arrivalTime,
                                          final EncodingRetainingSection section)
    {
        final StoredMessage<MessageMetaData_1_0> storedMessage = mock(StoredMessage.class);
        MessageMetaData_1_0 metaData = new MessageMetaData_1_0(header.createEncodingRetainingSection(),
                                                               deliveryAnnotations.createEncodingRetainingSection(),
                                                               messageAnnotations.createEncodingRetainingSection(),
                                                               properties.createEncodingRetainingSection(),
                                                               applicationProperties.createEncodingRetainingSection(),
                                                               new Footer(Collections.emptyMap()).createEncodingRetainingSection(),
                                                               arrivalTime,
                                                               0);
        when(storedMessage.getMetaData()).thenReturn(metaData);

        if (section != null)
        {
            final QpidByteBuffer combined = QpidByteBuffer.wrap(ByteBufferUtils.combine(section.getEncodedForm()));
            when(storedMessage.getContentSize()).thenReturn((int) section.getEncodedSize());
            final ArgumentCaptor<Integer> offsetCaptor = ArgumentCaptor.forClass(Integer.class);
            final ArgumentCaptor<Integer> sizeCaptor = ArgumentCaptor.forClass(Integer.class);

            when(storedMessage.getContent(offsetCaptor.capture(),
                                          sizeCaptor.capture())).then(invocation ->
                                                                      {
                                                                          final QpidByteBuffer view = combined.view(
                                                                                  offsetCaptor.getValue(),
                                                                                  sizeCaptor.getValue());
                                                                          return Collections.singleton(view);
                                                                      });
        }
        return new Message_1_0(storedMessage);
    }

    private byte[] getObjectBytes(final Object object) throws IOException
    {
        final byte[] expected;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos))
        {
            oos.writeObject(object);
            expected = baos.toByteArray();
        }
        return expected;
    }

    private void doTestDataWithAnnotation(final byte[] data,
                                          final MessageAnnotations messageAnnotations,
                                          final String expectedMimeType) throws Exception
    {
        final Data value = new Data(new Binary(data));
        Message_1_0 sourceMessage = createTestMessage(messageAnnotations, value.createEncodingRetainingSection());

        final AMQMessage convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));

        assertEquals("Unexpected mime type",
                     expectedMimeType, convertedMessage.getMessageHeader().getMimeType());
        final Collection<QpidByteBuffer> content = convertedMessage.getContent(0, (int) convertedMessage.getSize());
        assertArrayEquals("Unexpected content", data, getBytes(content));
    }
}
