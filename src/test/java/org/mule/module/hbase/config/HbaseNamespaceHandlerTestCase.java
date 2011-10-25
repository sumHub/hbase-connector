/**
 * Mule HBase Cloud Connector
 *
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/**
 * This file was automatically generated by the Mule Cloud Connector Development Kit
 */

package org.mule.module.hbase.config;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transport.PropertyScope;
import org.mule.module.hbase.api.HBaseService;
import org.mule.tck.FunctionalTestCase;

import java.util.HashMap;

import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.RowLock;

public class HbaseNamespaceHandlerTestCase extends FunctionalTestCase
{
    private HBaseService mockService;

    @Override
    protected String getConfigResources()
    {
        return "hbase-namespace-config.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        mockService = muleContext.getRegistry().lookupObject("mockFacade");
        reset(mockService);
    }

    public void testFlowGet() throws Exception
    {
        final Result mockResult = new Result();
        when(mockService.get(eq("t1"), eq("r1"), anyInt(), anyLong())).thenReturn(mockResult);

        final MessageProcessor flow = lookupFlowConstruct("flowGet");
        final MuleEvent event = getTestEvent(null);
        final MuleEvent responseEvent = flow.process(event);

        final Result response = responseEvent.getMessage().getPayload(Result.class);
        assertEquals(mockResult, response);
        verify(mockService).get(eq("t1"), eq("r1"), anyInt(), anyLong());
    }

    public void testFlowPut() throws Exception
    {
        final MessageProcessor flow = lookupFlowConstruct("flowPut");
        final MuleEvent event = getTestEvent(new Exception());
        flow.process(event);

        verify(mockService).put(eq("t1"), eq("r1"), eq("f1"), eq("q1"), anyLong(), eq("v1"), anyBoolean(),
            any(RowLock.class));
    }

    private MessageProcessor lookupFlowConstruct(String name)
    {
        return (MessageProcessor) muleContext.getRegistry().lookupFlowConstruct(name);
    }
}
