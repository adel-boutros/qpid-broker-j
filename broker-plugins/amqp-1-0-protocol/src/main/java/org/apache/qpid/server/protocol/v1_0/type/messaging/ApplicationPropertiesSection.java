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

package org.apache.qpid.server.protocol.v1_0.type.messaging;

import java.util.Map;

import org.apache.qpid.server.protocol.v1_0.codec.AbstractDescribedTypeConstructor;
import org.apache.qpid.server.protocol.v1_0.type.messaging.codec.ApplicationPropertiesConstructor;

public class ApplicationPropertiesSection extends AbstractSection<Map<String,Object>, ApplicationProperties>
{

    public ApplicationPropertiesSection()
    {
        super();
    }

    ApplicationPropertiesSection(final ApplicationProperties section)
    {
        super(section);
    }

    ApplicationPropertiesSection(final ApplicationPropertiesSection applicationPropertiesSection)
    {
        super(applicationPropertiesSection);
    }

    @Override
    public ApplicationPropertiesSection copy()
    {
        return new ApplicationPropertiesSection(this);
    }

    @Override
    protected AbstractDescribedTypeConstructor<ApplicationProperties> createNonEncodingRetainingSectionConstructor()
    {
        return  new ApplicationPropertiesConstructor();
    }
}
