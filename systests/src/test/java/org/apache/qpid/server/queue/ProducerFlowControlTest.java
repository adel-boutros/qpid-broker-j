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
package org.apache.qpid.server.queue;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.qpid.client.AMQDestination;
import org.apache.qpid.client.AMQSession;
import org.apache.qpid.server.logging.AbstractTestLogging;
import org.apache.qpid.server.model.ConfiguredObject;
import org.apache.qpid.server.model.LifetimePolicy;
import org.apache.qpid.server.model.OverflowPolicy;
import org.apache.qpid.systest.rest.RestTestHelper;
import org.apache.qpid.test.utils.TestBrokerConfiguration;

public class ProducerFlowControlTest extends AbstractTestLogging
{
    private static final Logger _logger = LoggerFactory.getLogger(ProducerFlowControlTest.class);

    private Connection _producerConnection;
    private Connection _consumerConnection;
    private Session _producerSession;
    private Session _consumerSession;
    private MessageProducer _producer;
    private MessageConsumer _consumer;
    private Queue _queue;
    private RestTestHelper _restTestHelper;

    private final AtomicInteger _sentMessages = new AtomicInteger(0);
    private int _messageSizeIncludingHeader;
    private Session _utilitySession;

    @Override
    public void setUp() throws Exception
    {
        getDefaultBrokerConfiguration().addHttpManagementConfiguration();
        super.setUp();
    }

    @Override
    public void startDefaultBroker()
    {
        // broker start-up is delegated to the tests
    }

    private void init() throws Exception
    {
        super.startDefaultBroker();
        _restTestHelper = new RestTestHelper(getDefaultBroker().getHttpPort());
        _monitor.markDiscardPoint();

        if (!isBroker10())
        {
            setSystemProperty("sync_publish", "all");
        }

        _producerConnection = getConnection();
        _producerSession = _producerConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        _producerConnection.start();

        _consumerConnection = getConnection();
        _consumerSession = _consumerConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        final Connection utilityConnection = getConnection();
        utilityConnection.start();
        _utilitySession = utilityConnection.createSession(true, Session.SESSION_TRANSACTED);
        String tmpQueueName = getTestQueueName() + "_Tmp";
        Queue tmpQueue = createTestQueue(_utilitySession, tmpQueueName);
        MessageProducer tmpQueueProducer= _utilitySession.createProducer(tmpQueue);
        tmpQueueProducer.send(nextMessage(0, _utilitySession));
        _utilitySession.commit();

        _messageSizeIncludingHeader = getQueueDepthBytes(tmpQueueName);
    }

    public void testCapacityExceededCausesBlock() throws Exception
    {
        init();
        String queueName = getTestQueueName();

        int capacity = _messageSizeIncludingHeader * 3 + _messageSizeIncludingHeader / 2;
        int resumeCapacity = _messageSizeIncludingHeader * 2;
        createAndBindQueueWithFlowControlEnabled(_producerSession, queueName, capacity, resumeCapacity);
        _producer = _producerSession.createProducer(_queue);

        // try to send 5 messages (should block after 4)
        CountDownLatch sendLatch = sendMessagesAsync(_producer, _producerSession, 5).getSendLatch();

        assertTrue("Flow is not stopped", awaitAttributeValue(queueName, "queueFlowStopped", true, 5000));
        assertEquals("Incorrect number of message sent before blocking", 4, _sentMessages.get());

        _consumer = _consumerSession.createConsumer(_queue);
        _consumerConnection.start();

        Message message = _consumer.receive(RECEIVE_TIMEOUT);
        assertNotNull("Message is not received", message);

        assertFalse("Flow is not stopped", awaitAttributeValue(queueName, "queueFlowStopped", false, 1000));

        assertEquals("Message incorrectly sent after one message received", 4, _sentMessages.get());

        Message message2 = _consumer.receive(RECEIVE_TIMEOUT);
        assertNotNull("Message is not received", message2);
        assertTrue("Message sending is not finished", sendLatch.await(1000, TimeUnit.MILLISECONDS));
        assertEquals("Message not sent after two messages received", 5, _sentMessages.get());
    }


    public void testBrokerLogMessages() throws Exception
    {
        init();
        String queueName = getTestQueueName();

        int capacity = _messageSizeIncludingHeader * 3 + _messageSizeIncludingHeader / 2;
        int resumeCapacity = _messageSizeIncludingHeader * 2;

        createAndBindQueueWithFlowControlEnabled(_producerSession, queueName, capacity, resumeCapacity);
        _producer = _producerSession.createProducer(_queue);

        // try to send 5 messages (should block after 4)
        sendMessagesAsync(_producer, _producerSession, 5);

        List<String> results = waitAndFindMatches("QUE-1003", 7000);

        assertEquals("Did not find correct number of QUE-1003 queue overfull messages", 1, results.size());

        _consumer = _consumerSession.createConsumer(_queue);
        _consumerConnection.start();


        while(_consumer.receive(1000) != null) {};

        results = waitAndFindMatches("QUE-1004");

        assertEquals("Did not find correct number of UNDERFULL queue underfull messages", 1, results.size());
    }

    public void testFlowControlOnCapacityResumeEqual() throws Exception
    {
        init();
        String queueName = getTestQueueName();

        int capacity = _messageSizeIncludingHeader * 3 + _messageSizeIncludingHeader / 2;
        createAndBindQueueWithFlowControlEnabled(_producerSession, queueName,
                                                 capacity,
                                                 capacity);
        _producer = _producerSession.createProducer(_queue);


        // try to send 5 messages (should block after 4)
        CountDownLatch sendLatch = sendMessagesAsync(_producer, _producerSession, 5).getSendLatch();

        assertTrue("Flow is not stopped", awaitAttributeValue(queueName, "queueFlowStopped", true,5000));

        assertEquals("Incorrect number of message sent before blocking", 4, _sentMessages.get());

        _consumer = _consumerSession.createConsumer(_queue);
        _consumerConnection.start();

        Message message = _consumer.receive(RECEIVE_TIMEOUT);
        assertNotNull("Message is not received", message);

        assertTrue("Message sending is not finished", sendLatch.await(1000, TimeUnit.MILLISECONDS));

        assertEquals("Message incorrectly sent after one message received", 5, _sentMessages.get());
        

    }


    public void testFlowControlSoak() throws Exception
    {
        init();
        String queueName = getTestQueueName();
        

        final int numProducers = 10;
        final int numMessages = 100;

        final int capacity = _messageSizeIncludingHeader * 20;

        createAndBindQueueWithFlowControlEnabled(_producerSession, queueName, capacity, capacity/2);

        _consumerConnection.start();

        Connection[] producers = new Connection[numProducers];
        for(int i = 0 ; i < numProducers; i ++)
        {

            producers[i] = getConnection();
            producers[i].start();
            Session session = producers[i].createSession(false, Session.AUTO_ACKNOWLEDGE);

            MessageProducer myproducer = session.createProducer(_queue);
            MessageSender sender = sendMessagesAsync(myproducer, session, numMessages);
        }

        _consumer = _consumerSession.createConsumer(_queue);
        _consumerConnection.start();

        for(int j = 0; j < numProducers * numMessages; j++)
        {
        
            Message msg = _consumer.receive(5000);
            assertNotNull("Message not received("+j+"), sent: "+_sentMessages.get(), msg);

        }



        Message msg = _consumer.receive(500);
        assertNull("extra message received", msg);


        for(int i = 0; i < numProducers; i++)
        {
            producers[i].close();
        }

    }

    public void testFlowControlAttributeModificationViaREST() throws Exception
    {
        init();
        String queueName = getTestQueueName();

        createAndBindQueueWithFlowControlEnabled(_producerSession, queueName, 0, 0);
        _producer = _producerSession.createProducer(_queue);
        
        String queueUrl = String.format("queue/%1$s/%1$s/%2$s", TestBrokerConfiguration.ENTRY_NAME_VIRTUAL_HOST, queueName);

        //check current attribute values are 0 as expected
        Map<String, Object> queueAttributes = _restTestHelper.getJsonAsMap(queueUrl);
        assertEquals("Capacity was not the expected value", 0,
                     ((Number) queueAttributes.get(org.apache.qpid.server.model.Queue.MAXIMUM_QUEUE_DEPTH_BYTES)).intValue());

        //set new values that will cause flow control to be active, and the queue to become overfull after 1 message is sent
        setFlowLimits(queueUrl, 250, 250);
        assertFalse("Queue should not be overfull", isFlowStopped(queueUrl));

        // try to send 2 messages (should block after 1)
        sendMessagesAsync(_producer, _producerSession, 2);

        waitForFlowControlAndMessageCount(queueUrl, 1, 2000);

        //check only 1 message was sent, and queue is overfull
        assertEquals("Incorrect number of message sent before blocking", 1, _sentMessages.get());
        assertTrue("Queue should be overfull", isFlowStopped(queueUrl));

        int queueDepthBytes = getQueueDepthBytes(queueName);
        //raise the attribute values, causing the queue to become underfull and allow the second message to be sent.
        setFlowLimits(queueUrl, queueDepthBytes + 200, queueDepthBytes);

        waitForFlowControlAndMessageCount(queueUrl, 2, 2000);

        //check second message was sent, and caused the queue to become overfull again
        assertEquals("Second message was not sent after lifting FlowResumeCapacity", 2, _sentMessages.get());
        assertTrue("Queue should be overfull", isFlowStopped(queueUrl));

        //raise capacity above queue depth, check queue remains overfull as FlowResumeCapacity still exceeded
        setFlowLimits(queueUrl, 2 * queueDepthBytes + 100, queueDepthBytes);
        assertTrue("Queue should be overfull", isFlowStopped(queueUrl));

        //receive a message, check queue becomes underfull
        
        _consumer = _consumerSession.createConsumer(_queue);
        _consumerConnection.start();
        
        assertNotNull("Should have received first message", _consumer.receive(RECEIVE_TIMEOUT));

        if(!isBroker10())
        {
            //perform a synchronous op on the connection
            ((AMQSession<?, ?>) _consumerSession).sync();
        }

        _restTestHelper.waitForAttributeChanged(queueUrl, org.apache.qpid.server.model.Queue.QUEUE_FLOW_STOPPED, false);

        assertNotNull("Should have received second message", _consumer.receive(RECEIVE_TIMEOUT));
    }

    public void testProducerFlowControlIsTriggeredOnEnqueue() throws Exception
    {
        long oneHourMilliseconds = 60 * 60 * 1000L;
        setSystemProperty("virtualhost.housekeepingCheckPeriod", String.valueOf(oneHourMilliseconds));
        super.startDefaultBroker();
        _restTestHelper = new RestTestHelper(getDefaultBroker().getHttpPort());

        Connection connection = getConnection();
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

        String queueName = getTestQueueName();
        createAndBindQueueWithFlowControlEnabled(session, queueName, 1, 0, true, false);

        sendMessage(session, _queue, 1);

        String queueUrl = String.format("queue/%1$s/%1$s/%2$s", TestBrokerConfiguration.ENTRY_NAME_VIRTUAL_HOST, queueName);
        waitForFlowControlAndMessageCount(queueUrl, 1, 2000);

        assertTrue("Message flow is not stopped", isFlowStopped(queueUrl));
    }

    private int getQueueDepthBytes(final String queueName) throws IOException
    {
        // On AMQP 1.0 the size of the message on the broker is not necessarily the size of the message we sent. Therefore, get the actual size from the broker
        final String requestUrl = String.format("queue/%1$s/%1$s/%2$s/getStatistics?statistics=[\"queueDepthBytes\"]", TestBrokerConfiguration.ENTRY_NAME_VIRTUAL_HOST, queueName);
        final Map<String, Object> queueAttributes = _restTestHelper.getJsonAsMap(requestUrl);
        return ((Number) queueAttributes.get("queueDepthBytes")).intValue();
    }

    private void waitForFlowControlAndMessageCount(final String queueUrl, final int messageCount, final int timeout) throws InterruptedException, IOException
    {
        int timeWaited = 0;
        while (timeWaited < timeout && (!isFlowStopped(queueUrl) || _sentMessages.get() != messageCount))
        {
            Thread.sleep(50);
            timeWaited += 50;
        }
    }

    private void setFlowLimits(final String queueUrl, final int blockValue, final int resumeValue) throws IOException
    {
        final Map<String, Object> attributes = new HashMap<>();
        attributes.put(org.apache.qpid.server.model.Queue.MAXIMUM_QUEUE_DEPTH_BYTES, blockValue);
        attributes.put(org.apache.qpid.server.model.Queue.OVERFLOW_POLICY, OverflowPolicy.PRODUCER_FLOW_CONTROL);
        String resumeLimit = getFlowResumeLimit(blockValue, resumeValue);
        Map<String, String> context = Collections.singletonMap(org.apache.qpid.server.model.Queue.QUEUE_FLOW_RESUME_LIMIT, resumeLimit);
        attributes.put(org.apache.qpid.server.model.Queue.CONTEXT, context);
        _restTestHelper.submitRequest(queueUrl, "PUT", attributes);
    }

    private String getFlowResumeLimit(final double blockValue, final double resumeValue)
    {
        return String.format("%.2f", resumeValue / blockValue * 100.0);
    }

    private boolean isFlowStopped(final String queueUrl) throws IOException
    {
        Map<String, Object> queueAttributes2 = _restTestHelper.getJsonAsMap(queueUrl);
        return (boolean) queueAttributes2.get(org.apache.qpid.server.model.Queue.QUEUE_FLOW_STOPPED);
    }

    public void testQueueDeleteWithBlockedFlow() throws Exception
    {
        init();
        String queueName = getTestQueueName();
        int capacity = _messageSizeIncludingHeader * 3 + _messageSizeIncludingHeader / 2;
        int resumeCapacity = _messageSizeIncludingHeader * 2;
        createAndBindQueueWithFlowControlEnabled(_producerSession, queueName, capacity, resumeCapacity, true, false);

        _producer = _producerSession.createProducer(_queue);

        // try to send 5 messages (should block after 4)
        sendMessagesAsync(_producer, _producerSession, 5);

        assertTrue("Flow is not stopped", awaitAttributeValue(queueName, "queueFlowStopped", true,5000));

        assertEquals("Incorrect number of message sent before blocking", 4, _sentMessages.get());

        if(!isBroker10())
        {
            // delete queue with a consumer session
            ((AMQSession<?, ?>) _utilitySession).sendQueueDelete(queueName);
        }
        else
        {
            deleteEntityUsingAmqpManagement(getTestQueueName(), _utilitySession, "org.apache.qpid.Queue");
            createTestQueue(_utilitySession);
        }
        _consumer = _consumerSession.createConsumer(_queue);
        _consumerConnection.start();

        Message message = _consumer.receive(1000l);
        assertNull("Unexpected message", message);
    }

    private void createAndBindQueueWithFlowControlEnabled(Session session, String queueName, int capacity, int resumeCapacity) throws Exception
    {
        createAndBindQueueWithFlowControlEnabled(session, queueName, capacity, resumeCapacity, false, true);
    }

    private void createAndBindQueueWithFlowControlEnabled(Session session, String queueName, int capacity, int resumeCapacity, boolean durable, boolean autoDelete) throws Exception
    {
        if(isBroker10())
        {
            final Map<String, Object> attributes = new HashMap<>();
            if (capacity != 0)
            {
                attributes.put(org.apache.qpid.server.model.Queue.CONTEXT,
                               Collections.singletonMap(org.apache.qpid.server.model.Queue.QUEUE_FLOW_RESUME_LIMIT,
                                                        getFlowResumeLimit(capacity, resumeCapacity)));
            }
            attributes.put(org.apache.qpid.server.model.Queue.MAXIMUM_QUEUE_DEPTH_BYTES, capacity);
            attributes.put(org.apache.qpid.server.model.Queue.OVERFLOW_POLICY, OverflowPolicy.PRODUCER_FLOW_CONTROL);
            attributes.put(org.apache.qpid.server.model.Queue.DURABLE, durable);
            attributes.put(ConfiguredObject.LIFETIME_POLICY, autoDelete ? LifetimePolicy.DELETE_ON_NO_OUTBOUND_LINKS.name() : LifetimePolicy.PERMANENT.name());
            String queueUrl = String.format("queue/%1$s/%1$s/%2$s", TestBrokerConfiguration.ENTRY_NAME_VIRTUAL_HOST, queueName);
            _restTestHelper.submitRequest(queueUrl, "PUT", attributes, 201);
            _queue = session.createQueue(queueName);
        }
        else
        {
            final Map<String, Object> arguments = new HashMap<String, Object>();
            arguments.put("x-qpid-capacity", capacity);
            arguments.put("x-qpid-flow-resume-capacity", resumeCapacity);
            ((AMQSession<?, ?>) session).createQueue(queueName, autoDelete, durable, false, arguments);
            _queue = session.createQueue("direct://amq.direct/"
                                         + queueName
                                         + "/"
                                         + queueName
                                         + "?durable='"
                                         + durable
                                         + "'&autodelete='"
                                         + autoDelete
                                         + "'");
            ((AMQSession<?, ?>) session).declareAndBind((AMQDestination) _queue);
        }
    }

    private MessageSender sendMessagesAsync(final MessageProducer producer,
                                            final Session producerSession,
                                            final int numMessages)
    {
        MessageSender sender = new MessageSender(producer, producerSession, numMessages);
        new Thread(sender).start();
        return sender;
    }


    private class MessageSender implements Runnable
    {
        private final MessageProducer _senderProducer;
        private final Session _senderSession;
        private final int _numMessages;
        private volatile JMSException _exception;
        private CountDownLatch _sendLatch = new CountDownLatch(1);

        public MessageSender(MessageProducer producer, Session producerSession, int numMessages)
        {
            _senderProducer = producer;
            _senderSession = producerSession;
            _numMessages = numMessages;
        }

        @Override
        public void run()
        {
            try
            {
                sendMessages(_senderProducer, _senderSession, _numMessages);
            }
            catch (JMSException e)
            {
                _exception = e;
            }
            finally
            {
                _sendLatch.countDown();
            }
        }

        public CountDownLatch getSendLatch()
        {
            return _sendLatch;
        }

        private void sendMessages(MessageProducer producer, Session producerSession, int numMessages)
                throws JMSException
        {

            for (int msg = 0; msg < numMessages; msg++)
            {
                producer.send(nextMessage(msg, producerSession));
                _sentMessages.incrementAndGet();

                // Cause work that causes a synchronous interaction on the wire.  We need to be
                // sure that the client has received the flow/message.stop etc.
                producerSession.createTemporaryQueue().delete();
            }
        }

    }

    private final byte[] BYTE_300 = new byte[300];

    private Message nextMessage(int msg, Session producerSession) throws JMSException
    {
        BytesMessage send = producerSession.createBytesMessage();
        send.writeBytes(BYTE_300);
        send.setIntProperty("msg", msg);
        return send;
    }

    private boolean awaitAttributeValue(String queueName, String attributeName, Object expectedValue, long timeout)
            throws JMSException, InterruptedException
    {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + timeout;
        boolean found = false;
        do
        {
            Map<String, Object> attributes =
                    managementReadObject(_utilitySession, "org.apache.qpid.SortedQueue", queueName, false);
            Object actualValue = attributes.get(attributeName);
            if (expectedValue == null)
            {
                found = actualValue == null;
            }
            else if (actualValue != null)
            {
                if (actualValue.getClass() == expectedValue.getClass())
                {
                    found = expectedValue.equals(actualValue);
                }
                else
                {
                    found = String.valueOf(expectedValue).equals(String.valueOf(actualValue));
                }
            }

            if (!found)
            {
                Thread.sleep(50);
            }
        } while (!found && System.currentTimeMillis() <= endTime);
        return found;
    }
}
