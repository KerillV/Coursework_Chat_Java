import java.io.*;

// Класс-обёртка вокруг стандартных потоков ввода-вывода
public class SystemWrapper {
    private OutputStream outputStream;
    private InputStream inputStream;

    public SystemWrapper(OutputStream out, InputStream in) {
        this.outputStream = out;
        this.inputStream = in;
    }

    public void println(Object obj) {
        PrintStream printStream = new PrintStream(outputStream);
        printStream.println(obj);
    }

    public byte readByte() throws IOException {
        return (byte)inputStream.read();
    }

    public char readChar() throws IOException {
        return (char)inputStream.read();
    }

    public int read() throws IOException {
        return inputStream.read();
    }

    public static SystemWrapper createDefault() {
        return new SystemWrapper(System.out, System.in);
    }

    public ByteArrayOutputStream captureOutput() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));
        return baos;
    }

    public ByteArrayInputStream mockInput(byte[] data) {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        System.setIn(bais);
        return bais;
    }
}