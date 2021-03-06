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
package org.apache.qpid.server.protocol.v1_0.codec;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.CharBuffer;
import java.util.List;

import org.apache.qpid.server.bytebuffer.QpidByteBuffer;
import org.apache.qpid.server.bytebuffer.QpidByteBufferUtils;
import org.apache.qpid.server.protocol.v1_0.type.AmqpErrorException;
import org.apache.qpid.server.protocol.v1_0.type.transport.AmqpError;

public class StringTypeConstructor extends VariableWidthTypeConstructor<String>
{


    public static StringTypeConstructor getInstance(int i)
    {
        return new StringTypeConstructor(i);
    }


    private StringTypeConstructor(int size)
    {
        super(size);
    }

    private String constructFromSingleBuffer(final QpidByteBuffer in, final int size)
    {
        int origPosition = in.position();

        QpidByteBuffer dup = in.duplicate();
        try
        {
            dup.limit(dup.position() + size);
            CharBuffer charBuf = dup.decode(UTF_8);

            String str = charBuf.toString();

            in.position(origPosition + size);

            return str;
        }
        catch (IllegalArgumentException e)
        {
            throw new IllegalArgumentException("position: "
                                               + dup.position()
                                               + "size: "
                                               + size
                                               + " capacity: "
                                               + dup.capacity());
        }
        finally
        {
            dup.dispose();
        }
    }

    @Override
    public String construct(final List<QpidByteBuffer> in, final ValueHandler handler) throws AmqpErrorException
    {
        int size;

        if (!QpidByteBufferUtils.hasRemaining(in, getSize()))
        {
            throw new AmqpErrorException(AmqpError.DECODE_ERROR, "Cannot construct string: insufficient input data");
        }

        if (getSize() == 1)
        {
            size = QpidByteBufferUtils.get(in) & 0xFF;
        }
        else
        {
            size = QpidByteBufferUtils.getInt(in);
        }

        if (!QpidByteBufferUtils.hasRemaining(in, size))
        {
            throw new AmqpErrorException(AmqpError.DECODE_ERROR, "Cannot construct string: insufficient input data");
        }

        for (int i = 0; i < in.size(); i++)
        {
            QpidByteBuffer buf = in.get(i);
            if (buf.hasRemaining())
            {
                if (buf.remaining() >= size)
                {
                    return constructFromSingleBuffer(buf, size);
                }
                break;
            }
        }

        byte[] data = new byte[size];
        QpidByteBufferUtils.get(in, data);

        return new String(data, UTF_8);
    }
}
