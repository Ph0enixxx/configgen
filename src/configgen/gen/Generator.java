package configgen.gen;

import configgen.util.CachedFileOutputStream;
import configgen.util.CachedIndentPrinter;

import java.io.File;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipOutputStream;

public abstract class Generator {
    protected final Parameter parameter;

    protected Generator(Parameter parameter) {
        this.parameter = parameter;
    }

    public abstract void generate(Context ctx) throws IOException;


    protected void require(boolean cond, String... str) {
        if (!cond)
            throw new AssertionError(getClass().getSimpleName() + ": " + String.join(",", str));
    }

    protected static CachedIndentPrinter createCode(File file, String encoding) {
        return new CachedIndentPrinter(file, encoding);
    }

    protected static CachedIndentPrinter createCode(File file, String encoding, StringBuilder dst, StringBuilder cache, StringBuilder tmp) {
        return new CachedIndentPrinter(file, encoding, dst, cache, tmp);
    }

    protected static ZipOutputStream createZip(File file) {
        return new ZipOutputStream(new CheckedOutputStream(new CachedFileOutputStream(file), new CRC32()));
    }

    protected static String upper1(String value) {
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

    protected static String upper1Only(String value) {
        return value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
    }

    protected static String lower1(String value) {
        return value.substring(0, 1).toLowerCase() + value.substring(1);
    }
}
