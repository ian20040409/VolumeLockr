import java.lang.reflect.Method;
public class InspectAdbCrypto {
    public static void main(String[] args) throws Exception {
        Class<?> clazz = Class.forName("com.tananaev.adblib.AdbCrypto");
        for (Method m : clazz.getDeclaredMethods()) {
            System.out.println(m.toString());
        }
    }
}
