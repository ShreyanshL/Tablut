package tablut;

import org.junit.Test;
import ucb.junit.textui;

import static org.junit.Assert.assertTrue;

/**
 * The suite of all JUnit tests for the enigma package.
 *
 * @author Shreyansh Loharuka
 */
public class UnitTest {

    /**
     * Run the JUnit tests in this package. Add xxxTest.class entries to
     * the arguments of runClasses to run other JUnit tests.
     */
    public static void main(String[] ignored) {
        textui.runClasses(UnitTest.class);
    }

    /**
     * A dummy test as a placeholder for real ones.
     */
    @Test
    public void dummyTest() {
        assertTrue("There are no unit tests!", true);
    }

}



