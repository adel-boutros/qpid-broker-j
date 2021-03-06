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

package org.apache.qpid.server.protocol.v1_0;

import static org.mockito.Mockito.mock;

import org.apache.qpid.server.protocol.LinkModel;
import org.apache.qpid.server.virtualhost.QueueManagingVirtualHost;
import org.apache.qpid.test.utils.QpidTestCase;

public class LinkRegistryTest extends QpidTestCase
{
    private LinkRegistryImpl _linkRegistry;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        final QueueManagingVirtualHost virtualHost = mock(QueueManagingVirtualHost.class);
        _linkRegistry = new LinkRegistryImpl(virtualHost);
    }

    public void testGetSendingLink() throws Exception
    {
        String remoteContainerId = "testRemoteContainerId";
        String linkName = "testLinkName";
        LinkModel link = _linkRegistry.getSendingLink(remoteContainerId, linkName);
        assertNotNull("LinkRegistryModel#getSendingLink should always return an object", link);
        LinkModel link2 = _linkRegistry.getSendingLink(remoteContainerId, linkName);
        assertNotNull("LinkRegistryModel#getSendingLink should always return an object", link2);
        assertSame("Two calls to LinkRegistryModel#getSendingLink should return the same object", link, link2);
    }

    public void testGetReceivingLink() throws Exception
    {
        String remoteContainerId = "testRemoteContainerId";
        String linkName = "testLinkName";
        LinkModel link = _linkRegistry.getReceivingLink(remoteContainerId, linkName);
        assertNotNull("LinkRegistryModel#getReceivingLink should always return an object", link);
        LinkModel link2 = _linkRegistry.getReceivingLink(remoteContainerId, linkName);
        assertNotNull("LinkRegistryModel#getReceivingLink should always return an object", link2);
        assertSame("Two calls to LinkRegistryModel#getReceivingLink should return the same object", link, link2);
    }
}
