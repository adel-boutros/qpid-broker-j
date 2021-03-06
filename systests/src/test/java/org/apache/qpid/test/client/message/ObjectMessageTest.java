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
package org.apache.qpid.test.client.message;

import java.util.UUID;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;

import org.apache.qpid.test.utils.QpidBrokerTestCase;

public class ObjectMessageTest extends QpidBrokerTestCase
{
    private Connection _connection;
    private Session _session;
    private MessageConsumer _consumer;
    private MessageProducer _producer;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        //Create Connection
        _connection = getConnection();


        //Create Session
        _session = _connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        //Create Queue
        Queue queue = createTestQueue(_session);

        //Create Consumer
        _consumer = _session.createConsumer(queue);

        //Create Producer
        _producer = _session.createProducer(queue);

        _connection.start();
    }

    @Override
    public void tearDown() throws Exception
    {
        //clean up
        _connection.close();

        super.tearDown();
    }

    public void testGetAndSend() throws JMSException
    {
        //Create Sample Message using UUIDs
        UUID test = UUID.randomUUID();

        ObjectMessage testMessage = _session.createObjectMessage(test);

        Object o = testMessage.getObject();

        assertNotNull("Object was null", o);

        sendAndTest(testMessage, test);
    }

     public void testSend() throws JMSException
    {
        //Create Sample Message using UUIDs
        UUID test = UUID.randomUUID();

        ObjectMessage testMessage = _session.createObjectMessage(test);

        sendAndTest(testMessage, test);
    }

    public void testTostringAndSend() throws JMSException
    {
        //Create Sample Message using UUIDs
        UUID test = UUID.randomUUID();

        ObjectMessage testMessage = _session.createObjectMessage(test);

        assertNotNull("Object was null", testMessage.toString());

        sendAndTest(testMessage, test);
    }

    public void testSendNull() throws JMSException
    {

        ObjectMessage testMessage = _session.createObjectMessage(null);

        assertNotNull("Object was null", testMessage.toString());

        sendAndTest(testMessage, null);
    }
    
    //***************** Helpers

    private void sendAndTest(ObjectMessage message, Object sent) throws JMSException
    {
        _producer.send(message);

        ObjectMessage receivedMessage = (ObjectMessage) _consumer.receive(1000);

        assertNotNull("Message was not received.", receivedMessage);

        UUID result = (UUID) receivedMessage.getObject();

        assertEquals("First read: UUIDs were not equal", sent, result);

        result = (UUID) receivedMessage.getObject();
        
        assertEquals("Second read: UUIDs were not equal", sent, result);
    }


    public void testSendEmptyObjectMessage() throws JMSException
    {
        ObjectMessage testMessage = _session.createObjectMessage();
        testMessage.setStringProperty("testProperty", "test-value");
        assertNotNull("Object was null", testMessage.toString());

        _producer.send(testMessage);

        ObjectMessage receivedMessage = (ObjectMessage) _consumer.receive(1000);

        assertNotNull("Message was not received.", receivedMessage);
        assertNull("No object was sent", receivedMessage.getObject());
        assertEquals("Unexpected property received", "test-value", receivedMessage.getStringProperty("testProperty"));
    }

}
