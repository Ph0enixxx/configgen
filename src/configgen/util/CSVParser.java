package configgen.util;

import java.nio.file.Path;
import java.util.ArrayList;
//import java.util.HashMap;
import java.util.List;

public final class CSVParser {

    private static final char comma = ',';
    private static final char quote = '"';
    private static final char cr = '\r';
    private static final char lf = '\n';

    private enum State {
        START, NO_QUOTE, QUOTE, QUOTE2, CR,
    }


    public static List<List<String>> readFromFile(Path path, String encoding) {
        try {
            //使用reader很费内存
            //Reader reader = new UnicodeReader(new BufferedInputStream(new FileInputStream(file)), encoding)
            int nread = FileReadUtils.readAllBytes(path);
            byte[] buf = FileReadUtils.getBuf();

            BomChecker.Res bom = BomChecker.checkBom(buf, nread, encoding);
            String fileStr = new String(buf, bom.bomSize, nread - bom.bomSize, bom.encoding);
            return parse(fileStr);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private static ArrayList<String> emptyRecord = new ArrayList<>();
    private static StringBuilder field = new StringBuilder(128); //这里假设是单线程

    //https://tools.ietf.org/html/rfc4180
    public static List<List<String>> parse(String source) {
        ArrayList<List<String>> result = new ArrayList<>();
        ArrayList<String> record = new ArrayList<>();
        State state = State.START;

        field.setLength(0);

        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);

            switch (state) {
                case START:
                    switch (c) {
                        case comma:
                            record.add("");
                            break;
                        case quote:
                            field.setLength(0);
                            state = State.QUOTE;
                            break;
                        case cr:
                            field.setLength(0);
                            state = State.CR;
                            break;
                        default:
                            field.setLength(0);
                            field.append(c);
                            state = State.NO_QUOTE;
                            break;
                    }
                    break;

                case NO_QUOTE:
                    switch (c) {
                        case comma:
                            addField(record, field);
                            state = State.START;
                            break;
                        case cr:
                            state = State.CR;
                            break;
                        default:
                            field.append(c);
                            break;
                    }
                    break;

                case QUOTE:
                    switch (c) {
                        case quote:
                            state = State.QUOTE2;
                            break;
                        default:
                            field.append(c);
                            break;
                    }
                    break;

                case QUOTE2:
                    switch (c) {
                        case comma:
                            addField(record, field);
                            state = State.START;
                            break;
                        case quote:
                            field.append(quote);
                            state = State.QUOTE;
                            break;
                        case cr:
                            state = State.CR;
                            break;
                        default:
                            field.append(c); //忽略了"，
                            state = State.NO_QUOTE;
                            break;
                    }
                    break;

                case CR:
                    switch (c) {
                        case comma:
                            field.append(cr);
                            addField(record, field);
                            state = State.START;
                            break;
                        case lf:
                            addField(record, field);
                            addRecord(result, record);

                            record = new ArrayList<>(record.size()); //优化下存储
                            state = State.START;
                            break;
                        default:
                            field.append(cr);
                            field.append(c);
                            state = State.NO_QUOTE;
                            break;
                    }
                    break;
            }
        }

        switch (state) {
            case START:
                if (!record.isEmpty()) {
                    record.add("");
                    addRecord(result, record);
                }
                break;
            case CR:
                field.append(cr);
                addField(record, field);
                addRecord(result, record);
                break;
            default:
                addField(record, field);
                addRecord(result, record);
                break;
        }

        result.trimToSize();
        return result;
    }

//    private static HashMap<String, String> stringCache = new HashMap<>(1024);

    private static void addField(ArrayList<String> record, StringBuilder field) {
        String s = field.toString();

        if (s.length() < 5) {//与速度和内存间取个平衡吧
//            String c = stringCache.get(s);
//            if (c == null) {
//                stringCache.put(s, s);
//            } else {
//                s = c;
//            }

            s = s.intern();
        }
        record.add(s);
    }

    private static void addRecord(ArrayList<List<String>> result, ArrayList<String> record) {
        if (!checkRecordHasContent(record)) {
            record = emptyRecord; //作为空行标记
        }

        result.add(record);
    }

    private static boolean checkRecordHasContent(List<String> record) {
        for (String s : record) {
            if (!s.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEmptyRecord(List<String> record) {
        return record == emptyRecord;
    }



    public static boolean parseBoolean(String s) {
        String t = s.trim();
        return t.equals("1") || t.equalsIgnoreCase("true");
    }

    public static float parseFloat(String s) {
        String t = s.trim();
        return t.isEmpty() ? 0.f : Float.parseFloat(t);
    }

    public static int parseInt(String s) {
        String t = s.trim();
        return t.isEmpty() ? 0 : Integer.decode(t);
    }

    public static long parseLong(String s) {
        String t = s.trim();
        return t.isEmpty() ? 0 : Long.decode(t);
    }

}
