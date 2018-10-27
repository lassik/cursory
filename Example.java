import java.util.Scanner;

public class Example {
    public static void main(String[] args) throws Exception {
        if (!CursoryUnix.isatty(0)) {
            System.out.println("Not a terminal");
            return;
        }
        System.out.println("Is a terminal");
        try (CursoryUnix term = new CursoryUnix(0)) {
            CursoryUnix.XY size = term.getSize();
            System.out.format("Size is %dx%d\n", size.x, size.y);
            term.enableRawMode();
            Scanner input = new Scanner(System.in);
            while (input.hasNext()) {
                System.out.print(input.nextLine());
            }
        }
    }
}
