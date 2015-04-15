/**
 * Copyright (c) MuleSoft, Inc. All rights reserved. http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.md file.
 */

package org.mule.module.hbase.api.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.hadoop.hbase.TableExistsException;
import org.apache.hadoop.hbase.client.Result;
import org.junit.Before;
import org.junit.Test;
import org.mule.module.hbase.api.BloomFilterType;
import org.mule.module.hbase.api.CompressionType;
import org.mule.module.hbase.api.HBaseServiceException;
import org.mule.wrapper.hbase.ResultWrapper;

/**
 * <p>
 * Testing the {@link RPCHBaseService} implementation.
 * </p>
 * <em>It requires an HBase 0.90.x server running on localhost with the default ports.</em>
 * 
 * @author Pablo Martin Grigolatto
 * @since Apr 12, 2011
 */
public class RPCHBaseServiceTestDriver
{

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final String SOME_TABLE_NAME = "some-table-name";
    private static final String SOME_COLUMN_FAMILY_NAME = "some-column-family-name";
    private static final String SOME_ROW_NAME = "some-row-name";
    private static final String SOME_COLUMN_QUALIFIER = "some-qualifier";

    // shared between all tests
    private static RPCHBaseService rpchBaseService = new RPCHBaseService();
    private static Map<String, String> properties;

    @Before
    public void before()
    {
        properties = new HashMap<String, String>();
        properties.put("hbase.zookeeper.quorum", "127.0.0.1");
        properties.put("hbase.zookeeper.peerport", "2888");
        properties.put("hbase.zookeeper.property.clientPort", "2181");
        properties.put("hbase.zookeeper.leaderport", "3888");
        properties.put("hbase.master.port", "60000");
        properties.put("hbase.master.info.port", "60010");
        properties.put("hbase.regionserver.port", "60020");
        properties.put("hbase.regionserver.info.port", "60030");
        properties.put("hbase.rest.port", "8080");
        properties.put("zookeeper.znode.parent", "/hbase");

        properties.put("ipc.client.connect.max.retries", "2");
        properties.put("hbase.client.retries.number", "2");
        properties.put("hbase.client.rpc.maxattempts", "2");
        properties.put("hbase.rpc.timeout", "7000");
        properties.put("hbase.client.prefetch.limit", "3");

        properties.put("ipc.client.connection.maxidletime", "10000");
        properties.put("zookeeper.session.timeout", "10000");
        properties.put("hbase.zookeeper.property.maxClientCnxns", "3");

        properties.put("hbase.hstore.blockingWaitTime", "30000");

        // reset properties for each test
        rpchBaseService.addProperties(properties);

        // reset the database for each test
        if (rpchBaseService.existsTable(SOME_TABLE_NAME))
        {
            rpchBaseService.deleteTable(SOME_TABLE_NAME);
        }
    }

    // ------------ Admin Operations

    @Test
    public void testAlive()
    {
        assertTrue(rpchBaseService.alive());
    }

    /** should fail because the server is running at 2181 by default */
    @Test
    public void testNotAlive()
    {
        properties.put("hbase.zookeeper.peerport", "2889");
        properties.put("hbase.zookeeper.property.clientPort", "2182");
        properties.put("hbase.zookeeper.leaderport", "3889");
        properties.put("hbase.master.port", "60001");
        properties.put("hbase.master.info.port", "60011");
        properties.put("hbase.regionserver.port", "60021");
        properties.put("hbase.regionserver.info.port", "60031");
        properties.put("hbase.rest.port", "8081");
        properties.put("zookeeper.znode.parent", "/anotherpath");

        rpchBaseService.addProperties(properties);
        assertFalse(rpchBaseService.alive());
    }

    /** table management */
    @Test
    public void testTableAdmin()
    {
        // creates a new table
        assertFalse(rpchBaseService.existsTable(SOME_TABLE_NAME));
        rpchBaseService.createTable(SOME_TABLE_NAME);
        assertTrue(rpchBaseService.existsTable(SOME_TABLE_NAME));

        // table already exists
        try
        {
            rpchBaseService.createTable(SOME_TABLE_NAME);
            fail("table should exist");
        }
        catch (HBaseServiceException e)
        {
            if (!(e.getCause() instanceof TableExistsException))
            {
                fail("unexpected exception: " + e.getCause());
            }
        }

        // delete the table
        rpchBaseService.deleteTable(SOME_TABLE_NAME);
        assertFalse(rpchBaseService.existsTable(SOME_TABLE_NAME));
    }

    /** table enable/disable */
    @Test
    public void testTableEnable()
    {
        rpchBaseService.createTable(SOME_TABLE_NAME);
        assertFalse(rpchBaseService.isDisabledTable(SOME_TABLE_NAME));

        rpchBaseService.disabeTable(SOME_TABLE_NAME);
        assertTrue(rpchBaseService.isDisabledTable(SOME_TABLE_NAME));

        rpchBaseService.enableTable(SOME_TABLE_NAME);
        assertFalse(rpchBaseService.isDisabledTable(SOME_TABLE_NAME));
    }

    /** a table is not disabled even if it does not exists */
    @Test
    public void testTableNotDisabled()
    {
        assertFalse(rpchBaseService.isDisabledTable("another-table-name"));
    }

    /** table column management */
    @Test
    public void testColumnAdmin()
    {
        rpchBaseService.createTable(SOME_TABLE_NAME);
        assertFalse(rpchBaseService.existsColumn(SOME_TABLE_NAME, SOME_COLUMN_FAMILY_NAME));

        rpchBaseService.addColumn(SOME_TABLE_NAME, SOME_COLUMN_FAMILY_NAME, 5, false, null);
        assertTrue(rpchBaseService.existsColumn(SOME_TABLE_NAME, SOME_COLUMN_FAMILY_NAME));

        Map<String, String> map = new HashMap<String, String>();
        map.put("some-key", "some value");
        rpchBaseService.modifyColumn(SOME_TABLE_NAME, SOME_COLUMN_FAMILY_NAME, 7, 2048, null, CompressionType.GZ, false,
            123456, false, BloomFilterType.ROW, 1, map);

        rpchBaseService.deleteColumn(SOME_TABLE_NAME, SOME_COLUMN_FAMILY_NAME);
        assertFalse(rpchBaseService.existsColumn(SOME_TABLE_NAME, SOME_COLUMN_FAMILY_NAME));
    }

    // ------------ Row Operations

    @Test
    public void testRow()
    {
        rpchBaseService.createTable(SOME_TABLE_NAME);
        rpchBaseService.addColumn(SOME_TABLE_NAME, SOME_COLUMN_FAMILY_NAME, null, null, null);
        rpchBaseService.addColumn(SOME_TABLE_NAME, "family2", null, null, null);
        rpchBaseService.addColumn(SOME_TABLE_NAME, "family3", null, null, null);

        ResultWrapper ret0 = rpchBaseService.get(SOME_TABLE_NAME, SOME_ROW_NAME, null, null, null, null);
        assertTrue(ret0.isEmpty());
        assertFalse(rpchBaseService.exists(SOME_TABLE_NAME, SOME_ROW_NAME, null, null));

        rpchBaseService.put(SOME_TABLE_NAME, SOME_ROW_NAME, SOME_COLUMN_FAMILY_NAME, "q1", null, "value1",
            false);
        ResultWrapper ret1 = rpchBaseService.get(SOME_TABLE_NAME, SOME_ROW_NAME, null, null, null, null);
        assertEquals(1, ret1.list().size());
        assertTrue(rpchBaseService.exists(SOME_TABLE_NAME, SOME_ROW_NAME, null, null));
        long ret1timestamp = ret1.list().get(0).getTimestamp();
        assertTrue(rpchBaseService.exists(SOME_TABLE_NAME, SOME_ROW_NAME, null, ret1timestamp));

        rpchBaseService.put(SOME_TABLE_NAME, SOME_ROW_NAME, SOME_COLUMN_FAMILY_NAME, "q2", null, "value2",
            false);
        rpchBaseService.put(SOME_TABLE_NAME, SOME_ROW_NAME, SOME_COLUMN_FAMILY_NAME, "q3", null, "value3",
            false);
        rpchBaseService.put(SOME_TABLE_NAME, SOME_ROW_NAME, "family2", "q4", null, "value4", false);
        rpchBaseService.put(SOME_TABLE_NAME, SOME_ROW_NAME, "family2", "q5", null, "value5", false);
        rpchBaseService.put(SOME_TABLE_NAME, SOME_ROW_NAME, "family3", "q6", null, "value6", false);
        ResultWrapper ret2 = rpchBaseService.get(SOME_TABLE_NAME, SOME_ROW_NAME, null, null, null, null);
        assertEquals(6, ret2.list().size()); // every family

        rpchBaseService.put(SOME_TABLE_NAME, SOME_ROW_NAME, SOME_COLUMN_FAMILY_NAME, "q2", null, "value2-2",
            false);
        ResultWrapper ret3 = rpchBaseService.get(SOME_TABLE_NAME, SOME_ROW_NAME, null, null, 10, null);
        assertEquals(7, ret3.list().size()); // 6 + 1 old version

        ResultWrapper ret4 = rpchBaseService.get(SOME_TABLE_NAME, SOME_ROW_NAME, null, null, 10, ret1timestamp);
        assertEquals(1, ret4.list().size()); // the first version

        rpchBaseService.delete(SOME_TABLE_NAME, SOME_ROW_NAME, SOME_COLUMN_FAMILY_NAME, "q2", null, true);
        ResultWrapper ret5 = rpchBaseService.get(SOME_TABLE_NAME, SOME_ROW_NAME, null, null, 10, null);
        assertEquals(5, ret5.list().size()); // q1 + q3 + q4 + q5 + q6

        rpchBaseService.delete(SOME_TABLE_NAME, SOME_ROW_NAME, SOME_COLUMN_FAMILY_NAME, null, null, true);
        ResultWrapper ret6 = rpchBaseService.get(SOME_TABLE_NAME, SOME_ROW_NAME, null, null, 10, null);
        assertEquals(3, ret6.list().size()); // q4 + q5 + q6

        rpchBaseService.delete(SOME_TABLE_NAME, SOME_ROW_NAME, null, null, null, true);
        ResultWrapper ret7 = rpchBaseService.get(SOME_TABLE_NAME, SOME_ROW_NAME, null, null, 10, null);
        assertTrue(ret7.isEmpty());
    }

    @Test
    public void testScanRow()
    {
        rpchBaseService.createTable(SOME_TABLE_NAME);
        rpchBaseService.addColumn(SOME_TABLE_NAME, "family1", null, null, null);
        rpchBaseService.addColumn(SOME_TABLE_NAME, "family2", null, null, null);
        rpchBaseService.addColumn(SOME_TABLE_NAME, "family3", null, null, null);
        rpchBaseService.addColumn(SOME_TABLE_NAME, "family4", null, null, null);

        Iterable<Result> ret1 = rpchBaseService.scan(SOME_TABLE_NAME, null, null, null, null, null, true, 1,
            null, null, 50);
        assertFalse(ret1.iterator().hasNext());

        rpchBaseService.put(SOME_TABLE_NAME, SOME_ROW_NAME, "family1", "q1", null, "value1", false);
        ResultWrapper row1 = rpchBaseService.get(SOME_TABLE_NAME, SOME_ROW_NAME, null, null, null, null);
        final long r1Timestamp = row1.list().get(0).getTimestamp();
        rpchBaseService.put(SOME_TABLE_NAME, SOME_ROW_NAME, "family1", "q2", null, "value2", false);
        rpchBaseService.put(SOME_TABLE_NAME, SOME_ROW_NAME, "family1", "q3", null, "value3", false);
        rpchBaseService.put(SOME_TABLE_NAME, SOME_ROW_NAME, "family2", "q4", null, "value4", false);
        rpchBaseService.put(SOME_TABLE_NAME, SOME_ROW_NAME, "family2", "q5", null, "value5", false);
        rpchBaseService.put(SOME_TABLE_NAME, SOME_ROW_NAME, "family3", "q6", null, "value6", false);

        rpchBaseService.put(SOME_TABLE_NAME, "r2", "family2", "q1", null, "r2f2q1value", false);
        rpchBaseService.put(SOME_TABLE_NAME, "r2", "family2", "q2", null, "r2f2q2value", false);
        ResultWrapper row2 = rpchBaseService.get(SOME_TABLE_NAME, "r2", null, null, null, null);
        final long r2Timestamp = row2.list().get(0).getTimestamp();

        rpchBaseService.put(SOME_TABLE_NAME, "r3", "family2", "q1", null, "r3f2q1value", false);
        rpchBaseService.put(SOME_TABLE_NAME, "r3", "family3", "q2", null, "r3f3q2value", false);
        ResultWrapper row3 = rpchBaseService.get(SOME_TABLE_NAME, "r3", null, null, null, null);
        final long r3Timestamp = row3.list().get(0).getTimestamp();

        rpchBaseService.put(SOME_TABLE_NAME, "r4", "family3", "q1", null, "r4f3q1value", false);
        rpchBaseService.put(SOME_TABLE_NAME, "r4", "family4", "q2", null, "r4f4q2value", false);

        try
        {
            rpchBaseService.scan(null, null, null, null, null, null, true, 1, null, null, 50);
            fail("table name is required");
        }
        catch (IllegalArgumentException e)
        {
            // ok
        }

        // no filters
        final Iterable<Result> ret2 = rpchBaseService.scan(SOME_TABLE_NAME, null, null, null, null, null, true,
            1, null, null, 50);
        assertEquals(4, count(ret2));

        // column family
        final Iterable<Result> ret3 = rpchBaseService.scan(SOME_TABLE_NAME, "family1", null, null, null, null,
            true, 1, null, null, 50);
        Iterator<Result> it3 = ret3.iterator();
        assertTrue(it3.hasNext());
        assertEquals("value1", new String(it3.next().getValue("family1".getBytes(UTF8), "q1".getBytes(UTF8)),
            UTF8));
        assertFalse(it3.hasNext());

        // column qualifier
        final Iterable<Result> ret4 = rpchBaseService.scan(SOME_TABLE_NAME, "family2", "q1", null, null, null,
            true, 1, null, null, 50);
        assertEquals(2, count(ret4));

        // exclusive stop
        final Iterable<Result> ret5 = rpchBaseService.scan(SOME_TABLE_NAME, "family2", "q1", null, null, null,
            true, 1, null, "r3", 50);
        assertEquals(1, count(ret5));

        // specific timestamp
        final Iterable<Result> ret6 = rpchBaseService.scan(SOME_TABLE_NAME, null, null, r1Timestamp, null, null,
            true, 1, null, null, 50);
        assertEquals(1, count(ret6));

        // max timestamp is exclusive
        final Iterable<Result> ret7 = rpchBaseService.scan(SOME_TABLE_NAME, null, null, r1Timestamp,
            r2Timestamp, null, true, 1, null, null, 50);
        assertEquals(1, count(ret7));

        // max timestamp is exclusive
        final Iterable<Result> ret8 = rpchBaseService.scan(SOME_TABLE_NAME, null, null, r1Timestamp,
            r3Timestamp, null, true, 1, null, null, 50);
        assertEquals(2, count(ret8));

        final Iterable<Result> ret9 = rpchBaseService.scan(SOME_TABLE_NAME, null, null, null, null, 5, true,
            1, null, null, 50);
        assertEquals(4, count(ret9));

        final Iterable<Result> ret10 = rpchBaseService.scan(SOME_TABLE_NAME, null, null, null, null, null, true,
            1, null, null, 50);
        assertEquals(4, count(ret10));

        final Iterable<Result> ret11 = rpchBaseService.scan(SOME_TABLE_NAME, null, null, null, null, null, true,
            1, null, null, 50);
        assertEquals(4, count(ret11));

        // more than one version
        assertEquals(1, rpchBaseService.scan(SOME_TABLE_NAME, "family4", "q2", null, null, null, true, 1,
            null, null, 50)
            .iterator()
            .next()
            .getColumn("family4".getBytes(UTF8), "q2".getBytes(UTF8))
            .size());
        rpchBaseService.put(SOME_TABLE_NAME, "r4", "family4", "q2", null, "r4f4q2value-v2", false);
        rpchBaseService.put(SOME_TABLE_NAME, "r4", "family4", "q2", null, "r4f4q2value-v3", false);
        rpchBaseService.put(SOME_TABLE_NAME, "r4", "family4", "q2", null, "r4f4q2value-v4", false);
        assertEquals(2, rpchBaseService.scan(SOME_TABLE_NAME, "family4", "q2", null, null, null, true, 2,
            null, null, 50)
            .iterator()
            .next()
            .getColumn("family4".getBytes(UTF8), "q2".getBytes(UTF8))
            .size());

        // all versions
        assertEquals(3, rpchBaseService.scan(SOME_TABLE_NAME, "family4", "q2", null, null, null, true, 10,
            null, null, 50)
            .iterator()
            .next()
            .getColumn("family4".getBytes(UTF8), "q2".getBytes(UTF8))
            .size());

        // exclusive stop row
        assertEquals(1, count(rpchBaseService.scan(SOME_TABLE_NAME, null, null, null, null, null, true, 10,
            "r2", "r3", 50)));
        assertEquals(2, count(rpchBaseService.scan(SOME_TABLE_NAME, null, null, null, null, null, true, 10,
            "r2", "r4", 50)));
        assertEquals(1, count(rpchBaseService.scan(SOME_TABLE_NAME, null, null, null, null, null, true, 10,
            null, "r3", 50)));
    }

    @Test
    public void testIncrementValue()
    {
        rpchBaseService.createTable(SOME_TABLE_NAME);
        rpchBaseService.addColumn(SOME_TABLE_NAME, SOME_COLUMN_FAMILY_NAME, null, null, null);

        assertEquals(5, rpchBaseService.increment(SOME_TABLE_NAME, SOME_ROW_NAME, SOME_COLUMN_FAMILY_NAME,
            SOME_COLUMN_QUALIFIER, 5, false));
        assertEquals(3, rpchBaseService.increment(SOME_TABLE_NAME, SOME_ROW_NAME, SOME_COLUMN_FAMILY_NAME,
            SOME_COLUMN_QUALIFIER, -2, false));
        assertEquals(4, rpchBaseService.increment(SOME_TABLE_NAME, SOME_ROW_NAME, SOME_COLUMN_FAMILY_NAME,
            SOME_COLUMN_QUALIFIER, 1, false));
    }

    @Test
    public void testCheckOperations()
    {
        rpchBaseService.createTable(SOME_TABLE_NAME);
        rpchBaseService.addColumn(SOME_TABLE_NAME, "f1", null, null, null);
        rpchBaseService.addColumn(SOME_TABLE_NAME, "f2", null, null, null);

        assertFalse(rpchBaseService.checkAndDelete(SOME_TABLE_NAME, "r1", "f1", "q1", "v1", "f2", "q2", null,
            true));
        assertFalse(rpchBaseService.checkAndPut(SOME_TABLE_NAME, "r1", "f1", "q1", "v1", "f2", "q2", null,
            "v2", false));

        rpchBaseService.put(SOME_TABLE_NAME, "r1", "f1", "q1", null, "v1", false);
        assertTrue(rpchBaseService.checkAndPut(SOME_TABLE_NAME, "r1", "f1", "q1", "v1", "f2", "q2", null,
            "v2", false));

        ResultWrapper r2 = rpchBaseService.get(SOME_TABLE_NAME, "r1", null, null, null, null);
        assertEquals("v2", new String(
            r2.getColumnLatest("f2".getBytes(UTF8), "q2".getBytes(UTF8)).getValue(), UTF8));

        assertTrue(rpchBaseService.get(SOME_TABLE_NAME, "r1", null, null, null, null).containsColumn("f2".getBytes(UTF8),
            "q2".getBytes(UTF8)));
        assertFalse(rpchBaseService.checkAndDelete(SOME_TABLE_NAME, "r1", "f2", "q1", "v1", "f2", "q2", null,
            true));
        assertTrue(rpchBaseService.get(SOME_TABLE_NAME, "r1", null, null, null, null).containsColumn("f2".getBytes(UTF8),
            "q2".getBytes(UTF8)));
        assertFalse(rpchBaseService.checkAndDelete(SOME_TABLE_NAME, "r1", "f1", "q2", "v1", "f2", "q2", null,
            true));
        assertTrue(rpchBaseService.get(SOME_TABLE_NAME, "r1", null, null, null, null).containsColumn("f2".getBytes(UTF8),
            "q2".getBytes(UTF8)));
        assertFalse(rpchBaseService.checkAndDelete(SOME_TABLE_NAME, "r1", "f1", "q1", "v2", "f2", "q2", null,
            true));
        assertTrue(rpchBaseService.get(SOME_TABLE_NAME, "r1", null, null, null, null).containsColumn("f2".getBytes(UTF8),
            "q2".getBytes(UTF8)));
        assertTrue(rpchBaseService.checkAndDelete(SOME_TABLE_NAME, "r1", "f1", "q1", "v1", "f2", "q2", null,
            true));
        assertFalse(rpchBaseService.get(SOME_TABLE_NAME, "r1", null, null, null, null).containsColumn(
            "f2".getBytes(UTF8), "q2".getBytes(UTF8)));
    }

    @Test
    public void testLock()
    {
        rpchBaseService.createTable(SOME_TABLE_NAME);
        rpchBaseService.addColumn(SOME_TABLE_NAME, "f1", null, null, null);
        rpchBaseService.put(SOME_TABLE_NAME, "r1", "f1", "q1", null, "v1", false);
        rpchBaseService.put(SOME_TABLE_NAME, "r1", "f1", "q2", null, "v2", false);

        // lock
        
        

        // locked for 30s
        try
        {
            rpchBaseService.put(SOME_TABLE_NAME, "r1", "f1", "q1", null, "v2", false);
            fail();
        }
        catch (HBaseServiceException e)
        {
            assertEquals("v1", new String(rpchBaseService.get(SOME_TABLE_NAME, "r1", null, null, null, null)
                .getColumnLatest("f1".getBytes(UTF8), "q1".getBytes(UTF8))
                .getValue(), UTF8));
        }

        // put with lock
        rpchBaseService.put(SOME_TABLE_NAME, "r1", "f1", "q1", null, "v3", false);
        assertEquals("v3", new String(rpchBaseService.get(SOME_TABLE_NAME, "r1", null, null, null, null).getColumnLatest(
            "f1".getBytes(UTF8), "q1".getBytes(UTF8)).getValue(), UTF8));

        // delete locked row
        try
        {
            rpchBaseService.delete(SOME_TABLE_NAME, "r1", "f1", "q2", null, true);
            fail();
        }
        catch (HBaseServiceException e)
        {
            assertEquals("v2", new String(rpchBaseService.get(SOME_TABLE_NAME, "r1", null, null, null, null)
                .getColumnLatest("f1".getBytes(UTF8), "q2".getBytes(UTF8))
                .getValue(), UTF8));
        }

        // delete with lock
        rpchBaseService.delete(SOME_TABLE_NAME, "r1", "f1", "q2", null, true);
        assertFalse(rpchBaseService.get(SOME_TABLE_NAME, "r1", null, null, null, null).containsColumn(
            "f1".getBytes(UTF8), "q2".getBytes(UTF8)));

        

        // unlocked put
        rpchBaseService.put(SOME_TABLE_NAME, "r1", "f1", "q1", null, "v2", false);
        assertEquals("v2", new String(rpchBaseService.get(SOME_TABLE_NAME, "r1", null, null, null, null).getColumnLatest(
            "f1".getBytes(UTF8), "q1".getBytes(UTF8)).getValue(), UTF8));
    }

    private <T> int count(Iterable<T> iterator)
    {
        int aux = 0;
        for (T t : iterator)
        {
            aux++;
        }
        return aux;
    }
}
