package ru.sgp.utils;

import org.springframework.http.MediaType;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;

public class Sgp {

    //private static final Logger log = LoggerFactory.getLogger(Sgp.class);

    /**
     * ????????? ?????? ?? ??????? ? ??????(??? ???????? ???????) ????????
     * @param {String} ??????? ??????
     * @return true - ???? ?????? ??????
     */
    public static boolean isEmptyOrNull(final String string) {
        return string == null || string.trim().equals("");
    }

    /**
     * ???????? ?? ?????? ? ?????? ????????
     * @return
     */
    public static boolean isDebugging() {
        return java.lang.management.ManagementFactory.getRuntimeMXBean().
                getInputArguments().toString().indexOf("-agentlib:jdwp") > 0;
    }

    /**
     * ????????? ????? ?? ?????????? ???????? ??? ??????????????
     * @param {Integer} value
     * @return
     */
    public static boolean isValidId(Integer value) { return value != null && value > 0; }

    /**
     * ????????? ????? ?? ?????????? ???????? ??? ??????????????
     * @param {Long} value
     * @return
     */
    public static boolean isValidId(Long value) { return value != null && value > 0L; }

    private static final char dictL[] = {
            'f', ',', 'd', 'u', 'l', 't', '`', ';', 'p', 'b', 'q', 'r', 'k', 'v', 'y', 'j', 'g',
            'h', 'c', 'n', 'e', 'a', '[', 'w', 'x', 'i', 'o', ']', 's', 'm', '\'', '.', 'z',
            'F', '<', 'D', 'U', 'L', 'T', '~', ';', 'P', 'B', 'Q', 'R', 'K', 'V', 'Y', 'J', 'G',
            'H', 'C', 'N', 'E', 'A', '{', 'W', 'X', 'I', 'O', '}', 'S', 'M', '\"', '>', 'z',
    };
    private static final char dictC[] = {
            '?', '?', '?', '?', '?', '?', '?', '?', '?', '?', '?', '?', '?', '?', '?', '?', '?',
            '?', '?', '?', '?', '?', '?', '?', '?', '?', '?', '?', '?', '?', '?', '?', '?',
            '?', '?', '?', '?', '?', '?', '?', '?', '?', '?', '?', '?', '?', '?', '?', '?', '?',
            '?', '?', '?', '?', '?', '?', '?', '?', '?', '?', '?', '?', '?', '?', '?', '?',
    };
    private static final char dictSpec[] = { '.', ',', '[', ']', '{', '}', '"', '\'', '<', '>', '`', '~', ';' };

    /**
     * ?????? ????????? ???? ?? ??????? ?? ????????? "??????" ??????????
     * @param origin
     * @return
     */
    public static String translateKeyboardLatinToCyrillic(String origin, boolean force) {

        int countSpec = 0;
        int countCyril = 0;
        int countLatin = 0;
        if (force) {
            // "??????????, ???????????????" -> ?? ?????? ???? "??????????? ???????????????"
            for (int i = 0; i < origin.length(); i++) {
                for (int x = 0; x < dictSpec.length; x++) {
                    if (origin.charAt(i) == dictSpec[x])
                        countSpec++;
                }
                for (int x = 0; x < dictC.length; x++) {
                    if (origin.charAt(i) == dictC[x])
                        countCyril++;
                }
                for (int x = 0; x < dictL.length; x++) {
                    if (origin.charAt(i) == dictL[x])
                        countLatin++;
                }
            }
        } else {
            for (int i = 0; i < origin.length(); i++) {
                for (int x = 0; x < dictC.length; x++) {
                    if (origin.charAt(i) == dictC[x])
                        return origin;
                }
            }
        }

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < origin.length(); i++) {
            boolean missing = true;
            for (int x = 0; x < dictL.length; x++ ) {
                if (origin.charAt(i) == dictL[x] && (!force || countCyril == 0 || countLatin != countSpec))
                {
                    builder.append(dictC[x]);
                    missing = false;
                    break;
                }
            }
            if (missing)
                builder.append(origin.charAt(i));
        }

        return builder.toString();
    }

    /**
     * ?????? ??????? ???? ?? ???????? ?? ????????? "??????" ??????????
     * @param origin
     * @return
     */
    public static String translateKeyboardCyrillicToLatin(String origin, boolean force) {

        if (!force) {
            // ???? ???? ???? ???? ??????? ?????, ?????????? ???????????...
            for (int i = 0; i < origin.length(); i++) {
                for (int x = 0; x < dictL.length; x++) {
                    if (origin.charAt(i) == dictL[x])
                        return origin;
                }
            }
        }

        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < origin.length(); i++) {
            boolean missing = true;
            for (int x = 0; x < dictC.length; x++ ) {
                if (origin.charAt(i) == dictC[x]) {
                    builder.append(dictL[x]);
                    missing = false;
                    break;
                }
            }
            if (missing)
                builder.append(origin.charAt(i));
        }

        return builder.toString();
    }



    public static String getFileTypeByContent(byte[] bytes) {
        int[] is = new int[4];
        int i = 0;
        for (byte b : bytes) {
            is[i] = b & 0xFF;
            if (++i >= 4)
                break;
        }
        // byte[] { 137, 80, 78, 71 }; // PNG
        if (bytes.length >= 4 && is[0] == 137 && is[1] == 80 && is[2] == 78 && is[3] == 71)
            return "png";

        // byte[] { 255, 216, 255, 224 }; // jpeg
        // byte[] { 255, 216, 255, 225 }; // jpeg canon
        if (bytes.length >= 4 && is[0] == 255 && is[1] == 216 && is[2] == 255 && (is[3] == 224 || is[3] == 225))
            return "jpg";
/*
        var bmp = Encoding.ASCII.GetBytes("BM"); // BMP
        var gif = Encoding.ASCII.GetBytes("GIF"); // GIF
        var tiff = new byte[] { 73, 73, 42 }; // TIFF
        var tiff2 = new byte[] { 77, 77, 42 }; // TIFF
*/
        return null;
    }

    /**
     * ?????????? ? ?????? ?????? ????? ?? ???????? ??????
     * @param text ?????? ??? ??????????
     * @return ?????? ????
     */
    public static String getOnlyDigits(String text) {
        StringBuilder rc = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char chr = text.charAt(i);
            if (Character.isDigit(chr))
                rc.append(chr);
        }
        return rc.toString();
    }

    /**
     * ????????? ???? ????? ?? ?????
     * @param fileName ??? ????? ? ????? (data.xls)
     * @return ?????? ? ????? ????? (xls)
     */
    public static String getFileType(String fileName) {
        int indexDot = fileName.lastIndexOf('.');
        return indexDot >= 0 ? fileName.substring(indexDot + 1) : "";
    }

    public static MediaType getMediaType(String fileType) {
        String type = fileType.toLowerCase(Locale.ROOT);
        if (type.equals("xlsx") || type.equals("xls"))
            return new MediaType("application", "vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        if (type.equals("docx") || type.equals("doc"))
            return new MediaType("application", "vnd.openxmlformats-officedocument.wordprocessingml.document");
        if (type.equals("rar"))
            return new MediaType("application", "vnd.rar");
        if (type.equals("pdf"))
            return MediaType.APPLICATION_PDF;
        if (type.equals("jpeg") || type.equals("jpg"))
            return new MediaType("image", "jpeg");
        return new MediaType("application", type);
    }

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            try {
                request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            } catch (Exception ex) {
                return null;
            }
        }

        if (request != null) {
            String ip = request.getHeader("X-Forwarded-For");
            if (isEmptyOrNull(ip))
                ip = request.getRemoteAddr();
            return ip;
        }

        return null;
    }

}
