import java.nio.file.*;
import java.security.SecureRandom;
import java.util.HexFormat;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
public class Bootstrap {
    public static void main(String[] args) throws Exception {
        Path root=Path.of("/opt/atlas/runtime");var lines=new StringBuilder();
        String[][] users={{"atlas_registrar","TRINO_REGISTRAR_PASSWORD"},{"atlas_reader","TRINO_READER_PASSWORD"}};
        for(String[] user:users) {
            byte[] salt=new byte[16];new SecureRandom().nextBytes(salt);
            String password=System.getenv(user[1]);if(password==null||password.length()<12)throw new IllegalStateException("Configure distinct Trino passwords of at least 12 characters.");
            var spec=new PBEKeySpec(password.toCharArray(),salt,100000,256);
            // Trino 477's password-file authenticator specifies PBKDF2-HMAC-SHA1.
            byte[] hash=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).getEncoded();spec.clearPassword();
            lines.append(user[0]).append(":100000:").append(HexFormat.of().formatHex(salt)).append(':').append(HexFormat.of().formatHex(hash)).append('\n');
        }
        Files.writeString(root.resolve("password.db"),lines.toString());
        if(!Files.exists(root.resolve("server.p12"))) {
            int exit=new ProcessBuilder("keytool","-genkeypair","-alias","trino","-keyalg","RSA","-validity","3650","-storetype","PKCS12","-keystore",root.resolve("server.p12").toString(),
                "-storepass:env","TRINO_KEYSTORE_PASSWORD","-dname","CN=trino","-ext","SAN=dns:trino,dns:localhost,ip:127.0.0.1")
                .redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start().waitFor();
            if(exit!=0)throw new IllegalStateException("Local TLS keystore generation failed.");
        }
    }
}
