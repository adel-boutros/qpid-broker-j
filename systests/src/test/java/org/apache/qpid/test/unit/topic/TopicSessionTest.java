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
package org.apache.qpid.test.unit.topic;

import javax.jms.Connection;
import javax.jms.InvalidDestinationException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;

import org.apache.qpid.client.AMQQueue;
import org.apache.qpid.client.AMQSession;
import org.apache.qpid.test.utils.QpidBrokerTestCase;


public class TopicSessionTest extends QpidBrokerTestCase
{
    public void testTopicSubscriptionUnsubscription() throws Exception
    {

        TopicConnection con = (TopicConnection) getConnection();
        String topicName = "MyTopic";
        Topic topic = createTopic(con, topicName);
        TopicSession session1 = con.createTopicSession(true, AMQSession.NO_ACKNOWLEDGE);
        TopicSubscriber sub = session1.createDurableSubscriber(topic, "subscription0");
        TopicPublisher publisher = session1.createPublisher(topic);

        con.start();

        TextMessage tm = session1.createTextMessage("Hello");
        publisher.publish(tm);
        session1.commit();

        tm = (TextMessage) sub.receive(2000);
        assertNotNull(tm);
        session1.commit();
        sub.close();
        session1.unsubscribe("subscription0");

        try
        {
            session1.unsubscribe("not a subscription");
            fail("expected InvalidDestinationException when unsubscribing from unknown subscription");
        }
        catch (InvalidDestinationException e)
        {
            ; // PASS
        }
        catch (Exception e)
        {
            fail("expected InvalidDestinationException when unsubscribing from unknown subscription, got: " + e);
        }

        con.close();
    }

    public void testSubscriptionNameReuseForDifferentTopicSingleConnection() throws Exception
    {
        subscriptionNameReuseForDifferentTopic(false);
    }

    public void testSubscriptionNameReuseForDifferentTopicTwoConnections() throws Exception
    {
        subscriptionNameReuseForDifferentTopic(true);
    }

    private void subscriptionNameReuseForDifferentTopic(boolean shutdown) throws Exception
    {
        TopicConnection con = (TopicConnection) getConnection();
        Topic topic = createTopic(con, "MyTopic1" + String.valueOf(shutdown));
        Topic topic2 = createTopic(con, "MyOtherTopic1" + String.valueOf(shutdown));

        TopicSession session1 = con.createTopicSession(true, Session.AUTO_ACKNOWLEDGE);
        TopicSubscriber sub = session1.createDurableSubscriber(topic, "subscription0");
        TopicPublisher publisher = session1.createPublisher(null);

        con.start();

        publisher.publish(topic, session1.createTextMessage("hello"));
        session1.commit();
        TextMessage m = (TextMessage) sub.receive(2000);
        assertNotNull(m);
        session1.commit();

        if (shutdown)
        {
            session1.close();
            con.close();
            con = (TopicConnection) getConnection();
            con.start();
            session1 = con.createTopicSession(true, Session.AUTO_ACKNOWLEDGE);
            publisher = session1.createPublisher(null);
        }
        sub.close();
        TopicSubscriber sub2 = session1.createDurableSubscriber(topic2, "subscription0");
        publisher.publish(topic, session1.createTextMessage("hello"));
        session1.commit();
        if (!shutdown)
        {
            m = (TextMessage) sub2.receive(2000);
            assertNull(m);
            session1.commit();
        }
        publisher.publish(topic2, session1.createTextMessage("goodbye"));
        session1.commit();
        m = (TextMessage) sub2.receive(2000);
        assertNotNull(m);
        assertEquals("goodbye", m.getText());
        session1.unsubscribe("subscription0");
        con.close();
    }

    public void testUnsubscriptionAfterConnectionClose() throws Exception
    {
        TopicConnection con1 = (TopicConnection) getConnectionBuilder().build();
        Topic topic = createTopic(con1, "MyTopic3");

        TopicSession session1 = con1.createTopicSession(true, Session.AUTO_ACKNOWLEDGE);
        TopicPublisher publisher = session1.createPublisher(topic);

        TopicConnection con2 = (TopicConnection) getConnectionBuilder().setClientId("clientid").build();
        TopicSession session2 = con2.createTopicSession(true, Session.AUTO_ACKNOWLEDGE);
        TopicSubscriber sub = session2.createDurableSubscriber(topic, "subscription0");

        con2.start();

        publisher.publish(session1.createTextMessage("Hello"));
        session1.commit();
        TextMessage tm = (TextMessage) sub.receive(2000);
        session2.commit();
        assertNotNull(tm);
        con2.close();
        publisher.publish(session1.createTextMessage("Hello2"));
        session1.commit();
        con2 = (TopicConnection) getConnectionBuilder().setClientId("clientid").build();
        session2 = con2.createTopicSession(true,Session.AUTO_ACKNOWLEDGE);
        sub = session2.createDurableSubscriber(topic, "subscription0");
        con2.start();
        tm = (TextMessage) sub.receive(2000);
        session2.commit();
        assertNotNull(tm);
        assertEquals("Hello2", tm.getText());
        sub.close();
        session2.unsubscribe("subscription0");
        con1.close();
        con2.close();
    }

    public void testTextMessageCreation() throws Exception
    {

        TopicConnection con = (TopicConnection) getConnection();
        Topic topic = createTopic(con, "MyTopic4");
        TopicSession session1 = con.createTopicSession(true, AMQSession.AUTO_ACKNOWLEDGE);
        TopicPublisher publisher = session1.createPublisher(topic);
        MessageConsumer consumer1 = session1.createConsumer(topic);
        con.start();
        TextMessage tm = session1.createTextMessage("Hello");
        publisher.publish(tm);
        session1.commit();
        tm = (TextMessage) consumer1.receive(10000L);
        assertNotNull(tm);
        String msgText = tm.getText();
        assertEquals("Hello", msgText);
        tm = session1.createTextMessage();
        msgText = tm.getText();
        assertNull(msgText);
        publisher.publish(tm);
        session1.commit();
        tm = (TextMessage) consumer1.receive(10000L);
        assertNotNull(tm);
        session1.commit();
        msgText = tm.getText();
        assertNull(msgText);
        tm.clearBody();
        tm.setText("Now we are not null");
        publisher.publish(tm);
        session1.commit();
        tm = (TextMessage) consumer1.receive(2000);
        assertNotNull(tm);
        session1.commit();
        msgText = tm.getText();
        assertEquals("Now we are not null", msgText);

        tm = session1.createTextMessage("");
        msgText = tm.getText();
        assertEquals("Empty string not returned", "", msgText);
        publisher.publish(tm);
        session1.commit();
        tm = (TextMessage) consumer1.receive(2000);
        session1.commit();
        assertNotNull(tm);
        assertEquals("Empty string not returned", "", msgText);
        con.close();
    }

    public void testNoLocal() throws Exception
    {

        TopicConnection con = (TopicConnection) getConnection();

        Topic topic = createTopic(con, "testNoLocal");

        noLocalTest(con, topic);


        con.close();
    }


    public void testNoLocalDirectExchange() throws Exception
    {

        TopicConnection con = (TopicConnection) getConnection();

        Topic topic = createTopicOnDirect(con, "testNoLocal");

        noLocalTest(con, topic);


        con.close();
    }



    public void testNoLocalFanoutExchange() throws Exception
    {

        TopicConnection con = (TopicConnection) getConnection();

        Topic topic = createTopicOnFanout(con, "testNoLocal");

        noLocalTest(con, topic);

        con.close();
    }


    private void noLocalTest(TopicConnection con, Topic topic) throws Exception
    {
        TopicSession session1 = con.createTopicSession(true, AMQSession.AUTO_ACKNOWLEDGE);
        TopicSubscriber noLocal = session1.createSubscriber(topic,  "", true);

        TopicSubscriber select = session1.createSubscriber(topic,  "Selector = 'select'", false);
        TopicSubscriber normal = session1.createSubscriber(topic);


        TopicPublisher publisher = session1.createPublisher(topic);

        con.start();
        TextMessage m;
        TextMessage message;

        //send message to all consumers
        publisher.publish(session1.createTextMessage("hello-new2"));
        session1.commit();
        //test normal subscriber gets message
        m = (TextMessage) normal.receive(1000);
        assertNotNull(m);
        session1.commit();

        //test selector subscriber doesn't message
        m = (TextMessage) select.receive(1000);
        assertNull(m);
        session1.commit();

        //test nolocal subscriber doesn't message
        m = (TextMessage) noLocal.receive(1000);
        if (m != null)
        {
            _logger.info("Message:" + m.getText());
        }
        assertNull(m);

        //send message to all consumers
        message = session1.createTextMessage("hello2");
        message.setStringProperty("Selector", "select");

        publisher.publish(message);
        session1.commit();

        //test normal subscriber gets message
        m = (TextMessage) normal.receive(1000);
        assertNotNull(m);
        session1.commit();

        //test selector subscriber does get message
        m = (TextMessage) select.receive(1000);
        assertNotNull(m);
        session1.commit();

        //test nolocal subscriber doesn't message
        m = (TextMessage) noLocal.receive(100);
        assertNull(m);

        TopicConnection con2 = (TopicConnection) getClientConnection("guest", "guest", "foo");
        TopicSession session2 = con2.createTopicSession(true, AMQSession.AUTO_ACKNOWLEDGE);
        TopicPublisher publisher2 = session2.createPublisher(topic);


        message = session2.createTextMessage("hello2");
        message.setStringProperty("Selector", "select");

        publisher2.publish(message);
        session2.commit();

        //test normal subscriber gets message
        m = (TextMessage) normal.receive(1000);
        assertNotNull(m);
        session1.commit();

        //test selector subscriber does get message
        m = (TextMessage) select.receive(1000);
        assertNotNull(m);
        session1.commit();

        //test nolocal subscriber does message
        m = (TextMessage) noLocal.receive(1000);
        assertNotNull(m);
        con2.close();
    }

    /**
     * This tests was added to demonstrate QPID-3542.  The Java Client when used with the CPP Broker was failing to
     * ack messages received that did not match the selector.  This meant the messages remained indefinitely on the Broker.
     */
    public void testNonMatchingMessagesHandledCorrectly() throws Exception
    {
        final String topicName = getName();
        final String clientId = "clientId" + topicName;
        final Connection con1 = getConnection();
        final Session session1 = con1.createSession(false, Session.AUTO_ACKNOWLEDGE);
        final Topic topic1 = session1.createTopic(topicName);
        final AMQQueue internalNameOnBroker = new AMQQueue("amq.topic", "clientid" + ":" + clientId);

        // Setup subscriber with selector
        final TopicSubscriber subscriberWithSelector = session1.createDurableSubscriber(topic1, clientId, "Selector = 'select'", false);
        final MessageProducer publisher = session1.createProducer(topic1);

        con1.start();

        // Send non-matching message
        final Message sentMessage = session1.createTextMessage("hello");
        sentMessage.setStringProperty("Selector", "nonMatch");
        publisher.send(sentMessage);

        // Try to consume non-message, expect this to fail.
        final Message message1 = subscriberWithSelector.receive(1000);
        assertNull("should not have received message", message1);
        subscriberWithSelector.close();

        session1.close();

        // Now verify queue depth on broker.
        final Session session2 = con1.createSession(false, Session.AUTO_ACKNOWLEDGE);
        final long depth = ((AMQSession) session2).getQueueDepth(internalNameOnBroker);
        assertEquals("Expected queue depth of zero", 0, depth);
    }
}
