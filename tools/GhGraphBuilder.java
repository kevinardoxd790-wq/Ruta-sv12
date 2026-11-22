import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.Profile;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class GhGraphBuilder {
    private static final String MAPSFORGE_SV_URL =
        "https://download.mapsforge.org/maps/v5/central-america/el-salvador.map";
    private static final String GEOFABRIK_SV_PBF_URL =
        "https://download.geofabrik.de/central-america/el-salvador-latest.osm.pbf";

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: GhGraphBuilder <assetsDir> <tmpDir>");
            System.exit(1);
        }
        File assetsDir = new File(args[0]);
        File tmpDir = new File(args[1]);
        assetsDir.mkdirs();
        tmpDir.mkdirs();

        File mapFile = new File(assetsDir, "el-salvador.map");
        downloadIfMissing(MAPSFORGE_SV_URL, mapFile);

        new File(assetsDir, "el-salvador-gh.zip").delete();
        File ghZipFile = new File(assetsDir, "el-salvador-gh53.zip");
        if (!ghZipFile.exists() || ghZipFile.length() < 1_000_000L) {
            File pbfFile = new File(tmpDir, "el-salvador-latest.osm.pbf");
            downloadIfMissing(GEOFABRIK_SV_PBF_URL, pbfFile);

            File graphDir = new File(tmpDir, "el-salvador-gh");
            deleteRecursively(graphDir);
            graphDir.mkdirs();

            GraphHopper hopper = new GraphHopper();
            hopper.setOSMFile(pbfFile.getAbsolutePath());
            hopper.setGraphHopperLocation(graphDir.getAbsolutePath());
            hopper.setProfiles(
                new Profile("car").setVehicle("car").setWeighting("fastest"),
                new Profile("foot").setVehicle("foot").setWeighting("fastest")
            );
            hopper.getCHPreparationHandler().setCHProfiles(
                new CHProfile("car"),
                new CHProfile("foot")
            );
            hopper.importOrLoad();

            // Smoke test route: San Salvador (13.6929, -89.2182) -> Santa Ana (13.9942, -89.5597)
            GHResponse carTest = hopper.route(
                new GHRequest(13.6929, -89.2182, 13.9942, -89.5597).setProfile("car")
            );
            if (carTest.hasErrors()) {
                throw new RuntimeException("Car smoke test failed: " + carTest.getErrors());
            }
            System.out.println(
                "El Salvador GraphHopper smoke test OK! San Salvador -> Santa Ana: "
                    + Math.round(carTest.getBest().getDistance() / 1000.0)
                    + " km"
            );
            hopper.close();

            zipDirectory(graphDir, ghZipFile);
            System.out.println("Created " + ghZipFile.getName() + " (" + (ghZipFile.length() / 1024) + " KB)");
        } else {
            System.out.println("Using cached " + ghZipFile.getName() + " (" + (ghZipFile.length() / 1024) + " KB)");
        }
    }

    private static void downloadIfMissing(String urlStr, File dest) throws IOException {
        if (dest.exists() && dest.length() > 1_000_000L) {
            System.out.println("Using cached " + dest.getName() + " (" + (dest.length() / 1024) + " KB)");
            return;
        }
        System.out.println("Downloading " + urlStr + " -> " + dest.getAbsolutePath());
        HttpURLConnection conn = (HttpURLConnection) URI.create(urlStr).toURL().openConnection();
        conn.setInstanceFollowRedirects(true);
        conn.setConnectTimeout(30_000);
        conn.setReadTimeout(180_000);
        conn.setRequestProperty("User-Agent", "RutaSV-Offline-Builder/1.0");
        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new IOException("HTTP " + code + " downloading " + urlStr);
        }
        File part = new File(dest.getAbsolutePath() + ".part");
        try (InputStream in = new BufferedInputStream(conn.getInputStream());
             FileOutputStream out = new FileOutputStream(part)) {
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
        }
        if (dest.exists()) dest.delete();
        if (!part.renameTo(dest)) {
            throw new IOException("Could not rename " + part + " to " + dest);
        }
        System.out.println("Downloaded " + dest.getName() + " (" + (dest.length() / 1024) + " KB)");
    }

    private static void zipDirectory(File sourceDir, File zipFile) throws IOException {
        Path base = sourceDir.toPath();
        File part = new File(zipFile.getAbsolutePath() + ".part");
        try (ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(part)))) {
            zipWalk(base, sourceDir, zos);
        }
        if (zipFile.exists()) zipFile.delete();
        if (!part.renameTo(zipFile)) {
            throw new IOException("Could not rename " + part + " to " + zipFile);
        }
    }

    private static void zipWalk(Path base, File current, ZipOutputStream zos) throws IOException {
        File[] children = current.listFiles();
        if (children == null) return;
        byte[] buf = new byte[65536];
        for (File child : children) {
            if (child.isDirectory()) {
                zipWalk(base, child, zos);
            } else {
                String rel = base.relativize(child.toPath()).toString().replace('\\', '/');
                zos.putNextEntry(new ZipEntry(rel));
                try (FileInputStream fis = new FileInputStream(child)) {
                    int n;
                    while ((n = fis.read(buf)) != -1) {
                        zos.write(buf, 0, n);
                    }
                }
                zos.closeEntry();
            }
        }
    }

    private static void deleteRecursively(File file) {
        if (!file.exists()) return;
        File[] children = file.listFiles();
        if (children != null) {
            for (File c : children) deleteRecursively(c);
        }
        file.delete();
    }
}
