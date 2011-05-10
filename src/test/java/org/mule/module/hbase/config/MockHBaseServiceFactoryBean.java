/**
 * Mule HBase Cloud Connector
 *
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.hbase.config;

import static org.mockito.Mockito.*;

import org.mule.module.hbase.api.HBaseService;

/**
 * Creates mock services for testing
 * 
 * @author Pablo Martin Grigolatto
 * @since Apr 20, 2011
 */
public class MockHBaseServiceFactoryBean {

    public static HBaseService getObject() throws Exception {
        return mock(HBaseService.class);
    }

}
