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
import static org.apache.qpid.server.protocol.v1_0.MessageConverter_from_1_0.getContentType;
import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.Lists;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.apache.qpid.server.bytebuffer.QpidByteBuffer;
import org.apache.qpid.server.message.AMQMessageHeader;
import org.apache.qpid.server.model.NamedAddressSpace;
import org.apache.qpid.server.protocol.v0_10.transport.mimecontentconverter.ListToAmqpListConverter;
import org.apache.qpid.server.protocol.v0_10.transport.mimecontentconverter.MapToAmqpMapConverter;
import org.apache.qpid.server.protocol.v0_8.AMQMessage;
import org.apache.qpid.server.protocol.v0_8.MessageMetaData;
import org.apache.qpid.server.protocol.v0_8.transport.BasicContentHeaderProperties;
import org.apache.qpid.server.protocol.v0_8.transport.ContentHeaderBody;
import org.apache.qpid.server.protocol.v0_8.transport.MessagePublishInfo;
import org.apache.qpid.server.protocol.v1_0.JmsMessageTypeAnnotation;
import org.apache.qpid.server.protocol.v1_0.Message_1_0;
import org.apache.qpid.server.protocol.v1_0.messaging.SectionDecoder;
import org.apache.qpid.server.protocol.v1_0.messaging.SectionDecoderImpl;
import org.apache.qpid.server.protocol.v1_0.type.Binary;
import org.apache.qpid.server.protocol.v1_0.type.Symbol;
import org.apache.qpid.server.protocol.v1_0.type.codec.AMQPDescribedTypeRegistry;
import org.apache.qpid.server.protocol.v1_0.type.messaging.AmqpSequenceSection;
import org.apache.qpid.server.protocol.v1_0.type.messaging.AmqpValueSection;
import org.apache.qpid.server.protocol.v1_0.type.messaging.DataSection;
import org.apache.qpid.server.protocol.v1_0.type.messaging.EncodingRetainingSection;
import org.apache.qpid.server.protocol.v1_0.type.messaging.MessageAnnotationsSection;
import org.apache.qpid.server.store.StoredMessage;
import org.apache.qpid.server.typedmessage.TypedBytesContentWriter;
import org.apache.qpid.test.utils.QpidTestCase;

public class MessageConverter_0_8_to_1_0Test extends QpidTestCase
{
    private final MessageConverter_0_8_to_1_0 _converter = new MessageConverter_0_8_to_1_0();
    private final AMQPDescribedTypeRegistry _typeRegistry = AMQPDescribedTypeRegistry.newInstance()
                                                                                     .registerTransportLayer()
                                                                                     .registerMessagingLayer()
                                                                                     .registerTransactionLayer()
                                                                                     .registerSecurityLayer();

    private final StoredMessage<MessageMetaData> _handle = mock(StoredMessage.class);

    private final MessageMetaData _metaData = mock(MessageMetaData.class);
    private final AMQMessageHeader _header = mock(AMQMessageHeader.class);
    private final ContentHeaderBody _contentHeaderBody = mock(ContentHeaderBody.class);
    private final BasicContentHeaderProperties _basicContentHeaderProperties = mock(BasicContentHeaderProperties.class);

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        when(_handle.getMetaData()).thenReturn(_metaData);
        when(_metaData.getMessageHeader()).thenReturn(_header);
        when(_metaData.getMessagePublishInfo()).thenReturn(new MessagePublishInfo());
        when(_metaData.getContentHeaderBody()).thenReturn(_contentHeaderBody);
        when(_contentHeaderBody.getProperties()).thenReturn(_basicContentHeaderProperties);
    }

    public void testConvertStringMessageBody() throws Exception
    {
        doTestTextMessage("helloworld", "text/plain");
    }

    public void testConvertEmptyStringMessageBody() throws Exception
    {
        doTestTextMessage(null, "text/plain");
    }

    public void testConvertStringXmlMessageBody() throws Exception
    {
        doTestTextMessage("<helloworld></helloworld>", "text/xml");
    }

    public void testConvertEmptyStringXmlMessageBody() throws Exception
    {
        doTestTextMessage(null, "text/xml");
    }

    public void testConvertEmptyStringApplicationXmlMessageBody() throws Exception
    {
        doTestTextMessage(null, "application/xml");
    }

    public void testConvertStringWithContentTypeText() throws Exception
    {
        doTestTextMessage("foo","text/foobar");
    }

    public void testConvertStringWithContentTypeApplicationXml() throws Exception
    {
        doTestTextMessage("<helloworld></helloworld>","application/xml");
    }

    public void testConvertStringWithContentTypeApplicationXmlDtd() throws Exception
    {
        doTestTextMessage("<!DOCTYPE name []>","application/xml-dtd");
    }

    public void testConvertStringWithContentTypeApplicationFooXml() throws Exception
    {
        doTestTextMessage("<helloworld></helloworld>","application/foo+xml");
    }

    public void testConvertStringWithContentTypeApplicationJson() throws Exception
    {
        doTestTextMessage("[]","application/json");
    }

    public void testConvertStringWithContentTypeApplicationFooJson() throws Exception
    {
        doTestTextMessage("[]","application/foo+json");
    }

    public void testConvertStringWithContentTypeApplicationJavascript() throws Exception
    {
        doTestTextMessage("var foo","application/javascript");
    }

    public void testConvertStringWithContentTypeApplicationEcmascript() throws Exception
    {
        doTestTextMessage("var foo","application/ecmascript");
    }

    public void testConvertBytesMessageBody() throws Exception
    {
        doTestBytesMessage("helloworld".getBytes(), "application/octet-stream");
    }

    public void testConvertBytesMessageBodyNoContentType() throws Exception
    {
        final byte[] messageContent = "helloworld".getBytes();
        doTest(messageContent,
               null,
               DataSection.class,
               messageContent,
               null,
               null);
    }

    public void testConvertBytesMessageBodyUnknownContentType() throws Exception
    {
        final byte[] messageContent = "helloworld".getBytes();
        doTest(messageContent,
               "my/bytes",
               DataSection.class,
               messageContent,
               Symbol.valueOf("my/bytes"),
               null);
    }


    public void testConvertEmptyBytesMessageBody() throws Exception
    {
        doTestBytesMessage(new byte[0], "application/octet-stream");
    }

    public void testConvertJmsStreamMessageBody() throws Exception
    {
        final List<Object> expected = Lists.newArrayList("apple", 43, 31.42D);
        final byte[] messageBytes = getJmsStreamMessageBytes(expected);

        final String mimeType = "jms/stream-message";
        doTestStreamMessage(messageBytes, mimeType, expected, JmsMessageTypeAnnotation.STREAM_MESSAGE.getType());
    }

    public void testConvertJmsStreamMessageEmptyBody() throws Exception
    {
        final List<Object> expected = Collections.emptyList();

        doTestStreamMessage(null, "jms/stream-message", expected, JmsMessageTypeAnnotation.STREAM_MESSAGE.getType());
    }

    public void testConvertAmqpListMessageBody() throws Exception
    {
        final List<Object> expected = Lists.newArrayList("apple", 43, 31.42D);
        final byte[] messageBytes = new ListToAmqpListConverter().toMimeContent(expected);

        final String mimeType = "amqp/list";
        doTestStreamMessage(messageBytes, mimeType, expected, JmsMessageTypeAnnotation.STREAM_MESSAGE.getType());
    }

    public void testConvertAmqpListMessageBodyWithNonJmsContent() throws Exception
    {
        final List<Object> expected = Lists.newArrayList("apple", 43, 31.42D, Lists.newArrayList("nonJMSList"));
        final byte[] messageBytes = new ListToAmqpListConverter().toMimeContent(expected);

        final String mimeType = "amqp/list";
        doTestStreamMessage(messageBytes, mimeType, expected, null);
    }

    public void testConvertJmsMapMessageBody() throws Exception
    {
        final Map<String, Object> expected = Collections.singletonMap("key", "value");
        final byte[] messageBytes = getJmsMapMessageBytes(expected);

        doTestMapMessage(messageBytes, "jms/map-message", expected, JmsMessageTypeAnnotation.MAP_MESSAGE.getType());
    }

    public void testConvertJmsMapMessageEmptyBody() throws Exception
    {
        final Map<String, Object> expected = Collections.emptyMap();

        doTestMapMessage(null, "jms/map-message", expected, JmsMessageTypeAnnotation.MAP_MESSAGE.getType());
    }

    public void testConvertAmqpMapMessageBody() throws Exception
    {
        final Map<String, Object> expected = Collections.singletonMap("key", "value");
        final byte[] messageBytes = new MapToAmqpMapConverter().toMimeContent(expected);

        doTestMapMessage(messageBytes, "amqp/map", expected, JmsMessageTypeAnnotation.MAP_MESSAGE.getType());
    }

    public void testConvertAmqpMapMessageBodyWithNonJmsContent() throws Exception
    {
        final Map<String, Object> expected = Collections.singletonMap("key", Collections.singletonList("nonJmsList"));
        final byte[] messageBytes = new MapToAmqpMapConverter().toMimeContent(expected);

        doTestMapMessage(messageBytes, "amqp/map", expected, null);
    }

    public void testConvertObjectStreamMessageBody() throws Exception
    {
        final byte[] messageBytes = getObjectStreamMessageBytes(UUID.randomUUID());
        final byte[] expectedBytes = messageBytes;

        doTestObjectMessage(messageBytes, "application/java-object-stream", expectedBytes);
    }

    public void testConvertObjectStream2MessageBody() throws Exception
    {
        final byte[] messageBytes = getObjectStreamMessageBytes(UUID.randomUUID());
        final byte[] expectedBytes = messageBytes;

        doTestObjectMessage(messageBytes, "application/x-java-serialized-object", expectedBytes);
    }

    public void testConvertEmptyObjectStreamMessageBody() throws Exception
    {
        final byte[] messageBytes = null;
        final byte[] expectedBytes = getObjectStreamMessageBytes(messageBytes);
        final String mimeType = "application/java-object-stream";

        doTestObjectMessage(messageBytes, mimeType, expectedBytes);
    }

    public void testConvertEmptyMessageWithoutContentType() throws Exception
    {
        doTest(null, null, AmqpValueSection.class, null, null, JmsMessageTypeAnnotation.MESSAGE.getType());
    }

    public void testConvertEmptyMessageWithUnknownContentType() throws Exception
    {
        doTest(null, "foo/bar", DataSection.class, new byte[0], Symbol.valueOf("foo/bar"), null);
    }

    public void testConvertMessageWithoutContentType() throws Exception
    {
        final byte[] expectedContent = "someContent".getBytes(UTF_8);
        doTest(expectedContent, null, DataSection.class, expectedContent, null, null);
    }


    private byte[] getObjectStreamMessageBytes(final Serializable o) throws Exception
    {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos))
        {
            oos.writeObject(o);
            return bos.toByteArray();
        }
    }

    private byte[] getJmsStreamMessageBytes(List<Object> objects) throws Exception
    {
        TypedBytesContentWriter writer = new TypedBytesContentWriter();
        for (Object o : objects)
        {
            writer.writeObject(o);
        }
        return getBytes(writer);
    }

    private byte[] getJmsMapMessageBytes(Map<String, Object> map) throws Exception
    {
        TypedBytesContentWriter writer = new TypedBytesContentWriter();
        writer.writeIntImpl(map.size());
        for (Map.Entry<String, Object> entry : map.entrySet())
        {
            writer.writeNullTerminatedStringImpl(entry.getKey());
            writer.writeObject(entry.getValue());
        }
        return getBytes(writer);
    }

    private byte[] getBytes(final TypedBytesContentWriter writer)
    {
        ByteBuffer buf = writer.getData();
        final byte[] expected = new byte[buf.remaining()];
        buf.get(expected);
        return expected;
    }

    private List<EncodingRetainingSection<?>> getEncodingRetainingSections(final Collection<QpidByteBuffer> content,
                                                                           final int expectedNumberOfSections)
            throws Exception
    {
        SectionDecoder sectionDecoder = new SectionDecoderImpl(_typeRegistry.getSectionDecoderRegistry());
        final List<EncodingRetainingSection<?>> sections = sectionDecoder.parseAll(new ArrayList<>(content));
        assertEquals("Unexpected number of sections", expectedNumberOfSections, sections.size());
        return sections;
    }

    protected AMQMessage getAmqMessage(final byte[] expected, final String mimeType)
    {
        configureMessageContent(expected);
        configureMessageHeader(mimeType);

        return new AMQMessage(_handle);
    }

    private void configureMessageHeader(final String mimeType)
    {
        when(_header.getMimeType()).thenReturn(mimeType);
        when(_basicContentHeaderProperties.getContentTypeAsString()).thenReturn(mimeType);
    }

    private void configureMessageContent(byte[] section)
    {
        if (section == null)
        {
            section = new byte[0];
        }
        final QpidByteBuffer combined = QpidByteBuffer.wrap(section);
        when(_handle.getContentSize()).thenReturn(section.length);
        final ArgumentCaptor<Integer> offsetCaptor = ArgumentCaptor.forClass(Integer.class);
        final ArgumentCaptor<Integer> sizeCaptor = ArgumentCaptor.forClass(Integer.class);

        when(_handle.getContent(offsetCaptor.capture(),
                                sizeCaptor.capture())).then(new Answer<Collection<QpidByteBuffer>>()
        {
            @Override
            public Collection<QpidByteBuffer> answer(final InvocationOnMock invocation) throws Throwable
            {
                final QpidByteBuffer view = combined.view(offsetCaptor.getValue(), sizeCaptor.getValue());
                return Collections.singleton(view);
            }
        });
    }

    private Byte getJmsMessageTypeAnnotation(final Message_1_0 convertedMessage)
    {
        MessageAnnotationsSection messageAnnotationsSection = convertedMessage.getMessageAnnotationsSection();
        if (messageAnnotationsSection != null)
        {
            Map<Symbol, Object> messageAnnotations = messageAnnotationsSection.getValue();
            if (messageAnnotations != null)
            {
                Object annotation = messageAnnotations.get(Symbol.valueOf("x-opt-jms-msg-type"));
                if (annotation instanceof Byte)
                {
                    return ((Byte) annotation);
                }
            }
        }
        return null;
    }

    private void doTestTextMessage(final String originalContent, final String mimeType) throws Exception
    {
        final byte[] contentBytes = originalContent == null ? null : originalContent.getBytes(UTF_8);
        String expectedContent = originalContent == null ? "" : originalContent;
        doTest(contentBytes,
               mimeType,
               AmqpValueSection.class,
               expectedContent,
               Symbol.valueOf(mimeType),
               JmsMessageTypeAnnotation.TEXT_MESSAGE.getType());
    }


    private void doTestMapMessage(final byte[] messageBytes,
                                  final String mimeType,
                                  final Map<String, Object> expected,
                                  final Byte expectedJmsTypeAnnotation) throws Exception
    {
        doTest(messageBytes, mimeType, AmqpValueSection.class, expected, null, expectedJmsTypeAnnotation);
    }

    private void doTestBytesMessage(final byte[] messageContent, final String mimeType) throws Exception
    {
        doTest(messageContent,
               mimeType,
               DataSection.class,
               messageContent,
               Symbol.valueOf(mimeType),
               JmsMessageTypeAnnotation.BYTES_MESSAGE.getType());
    }

    private void doTestStreamMessage(final byte[] messageBytes,
                                     final String mimeType,
                                     final List<Object> expected,
                                     final Byte expectedJmsTypAnnotation) throws Exception
    {
        doTest(messageBytes, mimeType, AmqpSequenceSection.class, expected, null, expectedJmsTypAnnotation);
    }

    private void doTestObjectMessage(final byte[] messageBytes,
                                     final String mimeType,
                                     final byte[] expectedBytes)
            throws Exception
    {
        doTest(messageBytes,
               mimeType,
               DataSection.class,
               expectedBytes,
               Symbol.valueOf("application/x-java-serialized-object"),
               JmsMessageTypeAnnotation.OBJECT_MESSAGE.getType());
    }

    private void doTest(final byte[] messageBytes,
                        final String mimeType,
                        final Class<? extends EncodingRetainingSection<?>> expectedBodySection,
                        final Object expectedContent,
                        final Symbol expectedContentType,
                        final Byte expectedJmsTypeAnnotation) throws Exception
    {
        final AMQMessage sourceMessage = getAmqMessage(messageBytes, mimeType);
        final Message_1_0 convertedMessage = _converter.convert(sourceMessage, mock(NamedAddressSpace.class));
        final Collection<QpidByteBuffer> content = convertedMessage.getContent(0, (int) convertedMessage.getSize());

        List<EncodingRetainingSection<?>> sections = getEncodingRetainingSections(content, 1);
        EncodingRetainingSection<?> encodingRetainingSection = sections.get(0);
        assertEquals("Unexpected section type", expectedBodySection, encodingRetainingSection.getClass());

        if (expectedContent instanceof byte[])
        {
            assertArrayEquals("Unexpected content",
                              ((byte[]) expectedContent),
                              ((Binary) encodingRetainingSection.getValue()).getArray());
        }
        else
        {
            assertEquals("Unexpected content", expectedContent, encodingRetainingSection.getValue());
        }

        Symbol contentType = getContentType(convertedMessage);
        if (expectedContentType == null)
        {
            assertNull("Content type should be null", contentType);
        }
        else
        {
            assertEquals("Unexpected content type", expectedContentType, contentType);
        }

        Byte jmsMessageTypeAnnotation = getJmsMessageTypeAnnotation(convertedMessage);
        if (expectedJmsTypeAnnotation == null)
        {
            assertNull("Unexpected annotation 'x-opt-jms-msg-type'", jmsMessageTypeAnnotation);
        }
        else
        {
            assertEquals("Unexpected annotation 'x-opt-jms-msg-type'",
                         expectedJmsTypeAnnotation,
                         jmsMessageTypeAnnotation);
        }
    }
}
