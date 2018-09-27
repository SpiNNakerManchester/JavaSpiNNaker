package uk.ac.manchester.spinnaker.messages.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 *
 * @author micro
 */
public class TestVersion {
       
    @Test
    public void testThreeUnquoted () {
        Version version = new Version("1.2.3");
        assertEquals(1, version.majorVersion);
        assertEquals(2, version.minorVersion);
        assertEquals(3, version.revision);
    }

    @Test
    public void testThreeQuoted () {
        Version version = new Version("\"1.2.3\"");
        assertEquals(1, version.majorVersion);
        assertEquals(2, version.minorVersion);
        assertEquals(3, version.revision);
    }

    @Test
    public void testTwoUnquoted () {
        Version version = new Version("1.2");
        assertEquals(1, version.majorVersion);
        assertEquals(2, version.minorVersion);
        assertEquals(0, version.revision);
    }

    @Test
    public void testOneQuoted () {
        Version version = new Version("\"1\"");
        assertEquals(1, version.majorVersion);
        assertEquals(0, version.minorVersion);
        assertEquals(0, version.revision);
    }
}
