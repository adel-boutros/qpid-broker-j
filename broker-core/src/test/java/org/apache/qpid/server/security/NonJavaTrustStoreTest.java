/*
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
 */

package org.apache.qpid.server.security;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.qpid.server.configuration.IllegalConfigurationException;
import org.apache.qpid.server.configuration.updater.CurrentThreadTaskExecutor;
import org.apache.qpid.server.configuration.updater.TaskExecutor;
import org.apache.qpid.server.logging.EventLogger;
import org.apache.qpid.server.model.Broker;
import org.apache.qpid.server.model.BrokerModel;
import org.apache.qpid.server.model.ConfiguredObjectFactory;
import org.apache.qpid.server.model.Model;
import org.apache.qpid.server.model.TrustStore;
import org.apache.qpid.server.transport.network.security.ssl.SSLUtil;
import org.apache.qpid.test.utils.QpidTestCase;
import org.apache.qpid.test.utils.TestSSLConstants;


public class NonJavaTrustStoreTest extends QpidTestCase
{
    private final Broker<?> _broker = mock(Broker.class);
    private final TaskExecutor _taskExecutor = CurrentThreadTaskExecutor.newStartedInstance();
    private final Model _model = BrokerModel.getInstance();
    private final ConfiguredObjectFactory _factory = _model.getObjectFactory();

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        when(_broker.getTaskExecutor()).thenReturn(_taskExecutor);
        when(_broker.getChildExecutor()).thenReturn(_taskExecutor);
        when(_broker.getModel()).thenReturn(_model);
        when(_broker.getEventLogger()).thenReturn(new EventLogger());
        when(((Broker) _broker).getCategoryClass()).thenReturn(Broker.class);
    }

    public void testCreationOfTrustStoreFromValidCertificate() throws Exception
    {
        Map<String,Object> attributes = new HashMap<>();
        attributes.put(NonJavaTrustStore.NAME, "myTestTrustStore");
        attributes.put(NonJavaTrustStore.CERTIFICATES_URL, getClass().getResource("/java_broker.crt").toExternalForm());
        attributes.put(NonJavaTrustStore.TYPE, "NonJavaTrustStore");

        TrustStore trustStore = _factory.create(TrustStore.class, attributes, _broker);

        TrustManager[] trustManagers = trustStore.getTrustManagers();
        assertNotNull(trustManagers);
        assertEquals("Unexpected number of trust managers", 1, trustManagers.length);
        assertNotNull("Trust manager unexpected null", trustManagers[0]);
    }

    public void testUseOfExpiredTrustAnchorDenied() throws Exception
    {
        Map<String,Object> attributes = new HashMap<>();
        attributes.put(NonJavaTrustStore.NAME, "myTestTrustStore");
        attributes.put(NonJavaTrustStore.TRUST_ANCHOR_VALIDITY_ENFORCED, true);
        attributes.put(NonJavaTrustStore.CERTIFICATES_URL, getClass().getResource("/expired.crt").toExternalForm());
        attributes.put(NonJavaTrustStore.TYPE, "NonJavaTrustStore");

        TrustStore trustStore = _factory.create(TrustStore.class, attributes, _broker);

        TrustManager[] trustManagers = trustStore.getTrustManagers();
        assertNotNull(trustManagers);
        assertEquals("Unexpected number of trust managers", 1, trustManagers.length);
        assertTrue("Unexpected trust manager type",trustManagers[0] instanceof X509TrustManager);
        X509TrustManager trustManager = (X509TrustManager) trustManagers[0];

        KeyStore clientStore = SSLUtil.getInitializedKeyStore(TestSSLConstants.EXPIRED_KEYSTORE,
                                                              TestSSLConstants.KEYSTORE_PASSWORD,
                                                              KeyStore.getDefaultType());
        String alias = clientStore.aliases().nextElement();
        X509Certificate certificate = (X509Certificate) clientStore.getCertificate(alias);

        try
        {
            trustManager.checkClientTrusted(new X509Certificate[] {certificate}, "NULL");
            fail("Exception not thrown");
        }
        catch (CertificateException e)
        {
            if (e instanceof CertificateExpiredException || "Certificate expired".equals(e.getMessage()))
            {
                // IBMJSSE2 does not throw CertificateExpiredException, it throws a CertificateException
                // PASS
            }
            else
            {
                throw e;
            }

        }
    }

    public void testCreationOfTrustStoreFromNonCertificate() throws Exception
    {
        Map<String,Object> attributes = new HashMap<>();
        attributes.put(NonJavaTrustStore.NAME, "myTestTrustStore");
        attributes.put(NonJavaTrustStore.CERTIFICATES_URL, getClass().getResource("/java_broker.req").toExternalForm());
        attributes.put(NonJavaTrustStore.TYPE, "NonJavaTrustStore");

        try
        {
            _factory.create(TrustStore.class, attributes, _broker);
            fail("Trust store is created from certificate request file");
        }
        catch (IllegalConfigurationException e)
        {
            // pass
        }
    }

}
