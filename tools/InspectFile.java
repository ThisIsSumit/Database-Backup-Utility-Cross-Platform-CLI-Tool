import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

public class InspectFile {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: InspectFile <path>");
            System.exit(2);
        }
        File f = new File(args[0]);
        if (!f.exists()) {
            System.err.println("File not found: " + args[0]);
            System.exit(3);
        }
        System.out.println("Path: " + f.getAbsolutePath());
        System.out.println("Size: " + f.length() + " bytes");

        int toRead = 512;
        byte[] buf = new byte[toRead];
        try (FileInputStream fis = new FileInputStream(f)) {
            int read = fis.read(buf);
            if (read <= 0) {
                System.out.println("No bytes read");
                return;
            }
            System.out.println("HEX:");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < read; i++) {
                sb.append(String.format("%02X ", buf[i]));
                if ((i + 1) % 16 == 0) sb.append('\n');
            }
            System.out.println(sb.toString());

            System.out.println("\nTEXT PREVIEW (UTF-8):");
            String text = new String(buf, 0, read, StandardCharsets.UTF_8);
            System.out.println(text);
        }
    }
}
