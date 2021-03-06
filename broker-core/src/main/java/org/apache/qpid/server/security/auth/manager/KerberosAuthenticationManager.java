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
package org.apache.qpid.server.security.auth.manager;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.qpid.server.model.Container;
import org.apache.qpid.server.model.ManagedObject;
import org.apache.qpid.server.model.ManagedObjectFactoryConstructor;
import org.apache.qpid.server.model.NamedAddressSpace;
import org.apache.qpid.server.security.auth.sasl.SaslNegotiator;
import org.apache.qpid.server.security.auth.sasl.SaslSettings;
import org.apache.qpid.server.security.auth.sasl.kerberos.KerberosNegotiator;

@ManagedObject( category = false, type = "Kerberos" )
public class KerberosAuthenticationManager extends AbstractAuthenticationManager<KerberosAuthenticationManager>
{
    public static final String PROVIDER_TYPE = "Kerberos";
    public static final String GSSAPI_MECHANISM = "GSSAPI";

    @ManagedObjectFactoryConstructor
    protected KerberosAuthenticationManager(final Map<String, Object> attributes, final Container<?> container)
    {
        super(attributes, container);
    }

    @Override
    public List<String> getMechanisms()
    {
        return Collections.singletonList(GSSAPI_MECHANISM);
    }

    @Override
    public SaslNegotiator createSaslNegotiator(final String mechanism,
                                               final SaslSettings saslSettings,
                                               final NamedAddressSpace addressSpace)
    {
        if(GSSAPI_MECHANISM.equals(mechanism))
        {
            return new KerberosNegotiator(this, saslSettings.getLocalFQDN());
        }
        else
        {
            return null;
        }
    }
}
