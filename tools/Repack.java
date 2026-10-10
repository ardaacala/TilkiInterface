import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import java.security.*;
class Repack {
 public static void main(String[] args) throws Exception {
  Path root=Path.of("resourcepack"),zip=Path.of("dist/TilkiInterface-ResourcePack.zip");
  try(var out=new ZipOutputStream(Files.newOutputStream(zip));var files=Files.walk(root)) {for(Path file:files.filter(Files::isRegularFile).sorted().toList()) {var entry=new ZipEntry(root.relativize(file).toString().replace('\\','/'));entry.setTime(0);out.putNextEntry(entry);Files.copy(file,out);out.closeEntry();}}
  String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(zip)));Files.writeString(Path.of("dist/resourcepack-sha1.txt"),hash+"\n");System.out.println(hash);
 }
}
