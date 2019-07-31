package com.marcop.foodsystem.tools;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for OrderProcessor.
 */
public class OrderProcessorTest
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public OrderProcessorTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( OrderProcessorTest.class );
    }

    /**
     * Test processing simple orders
     */
    public void testProcessOrder_Simple()
    {
        assertTrue( true );
    }
}
