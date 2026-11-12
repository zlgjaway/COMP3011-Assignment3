
package comp3011;

public class CommandLineControllerManualTest {

    public static void main(String[] args) {
        test("Single effect",
                new String[]{"-n", "pom.xml"}, 1);

        test("Stacked effects",
                new String[]{"-jfddn", "pom.xml"}, 5);

        test("Long effects",
                new String[]{"--black-and-white", "--dust", "pom.xml"}, 2);

        test("Unknown option",
                new String[]{"-q", "pom.xml"}, -1);

        test("Audio option",
                new String[]{"-a", "pom.xml"}, 0);

        System.out.println("Manual tests finished.");
    }

    private static void test(String name, String[] args, int expectedCount) {
        CommandLineController controller = new CommandLineController(args);

        if (expectedCount == -1) {
            boolean passed = controller.getErrorMessage() != null;
            System.out.println((passed ? "PASS" : "FAIL") + ": " + name);
            return;
        }

        int actualCount = controller.getFrameProcessors().size();
        boolean passed = actualCount == expectedCount
                && controller.getErrorMessage() == null;

        System.out.println(
                (passed ? "PASS" : "FAIL") + ": " + name
                + " (expected " + expectedCount
                + ", got " + actualCount + ")");
    }
}
