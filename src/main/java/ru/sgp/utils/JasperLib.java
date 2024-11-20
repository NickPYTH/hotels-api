package ru.sgp.utils;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.HtmlExporter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.export.Exporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleHtmlExporterOutput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.util.FileCopyUtils;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class JasperLib {

    private final static Logger log = LoggerFactory.getLogger(JasperLib.class);

    public enum exportType { exportTypeHtml, exportTypeXlsx, exportTypeDocx, exportTypePdf };

    private static final String extJrxml = ".jrxml";
    private static final String extJasper = ".jasper";

/*
    private static String getHtmlHeader() {
        try {
            JasperLib jsl = new JasperLib();
            InputStream path = jsl.getClass().getClassLoader().getResourceAsStream("report/header.html");
            String header = new BufferedReader(
                    new InputStreamReader(path, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
            return header;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
*/

    private static byte[] export(final JasperPrint print, exportType type) throws JRException {
        if (print == null)
            return null;

        final Exporter exporter;
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        boolean html = false;

        switch (type) {
            case exportTypeHtml:
                exporter = new HtmlExporter();

                //SimpleHtmlExporterConfiguration shec = new SimpleHtmlExporterConfiguration();
                //shec.setHtmlHeader(getHtmlHeader());
                //exporter.setConfiguration(shec);

                exporter.setExporterOutput(new SimpleHtmlExporterOutput(out));
                html = true;
                break;
/*
            case CSV:
                exporter = new JRCsvExporter();
                break;

            case XML:
                exporter = new JRXmlExporter();
                break;
*/
            case exportTypeXlsx:
                exporter = new JRXlsxExporter();
                break;

            case exportTypeDocx:
                exporter = new JRDocxExporter();
//                exporter.setParameter(JRDocxExporterParameter.FRAMES_AS_NESTED_TABLES,  Boolean.FALSE);
//                exporter.setParameter(JRDocxExporterParameter.FLEXIBLE_ROW_HEIGHT,  Boolean.TRUE);
                break;

            case exportTypePdf:
                exporter = new JRPdfExporter();
                break;

            default:
                throw new JRException("Unknown report format: " + type.toString());
        }

        if (!html) {
            exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
        }

        exporter.setExporterInput(new SimpleExporterInput(print));
        exporter.exportReport();

        return out.toByteArray();
    }

    private static JasperPrint getReport(ReportInfo reportInfo) {
        if (reportInfo.inputStream == null)
            return null;

        JasperPrint jasperPrint = null;

        try {

            if (reportInfo.reportFile.toLowerCase(Locale.ROOT).contains(".jrxml")) {
                JasperReport jasperReport = JasperCompileManager.compileReport(reportInfo.inputStream);
                jasperPrint = JasperFillManager.fillReport(jasperReport, reportInfo.parameters, reportInfo.connection);
            } else
                jasperPrint = JasperFillManager.fillReport(reportInfo.inputStream, reportInfo.parameters, reportInfo.connection);
        } catch (JRException ex) {
            log.error("Ошибка получения Jasper Report " + reportInfo.reportFile + "\n", ex);
        }
        return jasperPrint;
    }

    public static int exportHtml(ReportInfo reportInfo) throws IOException, JRException {
        if (reportInfo.inputStream == null)
            return -1;

        reportInfo.response.setContentType("text/html;charset=UTF-8");

        byte[] jasperBytes;
        if (reportInfo.inputStream == null) {
            jasperBytes = ("Не найден файл отчета " + reportInfo.reportFile).getBytes(StandardCharsets.UTF_8);
        } else {
            JasperPrint jasperPrint = getReport(reportInfo);
            jasperBytes = export(jasperPrint, exportType.exportTypeHtml);
        }
        FileCopyUtils.copy(jasperBytes, reportInfo.response.getOutputStream());
        return 0;
    }

    public static int exportXlsx(ReportInfo reportInfo) throws JRException, IOException {
        if (reportInfo.inputStream == null)
            return -1;

        JasperPrint jasperPrint = getReport(reportInfo); // prepare report - pass parameters and jdbc connection

        String fileName = "report.xlsx";
        String mimeType = ru.sgp.utils.Sgp.getMediaType(ru.sgp.utils.Sgp.getFileType(fileName)).toString();
        reportInfo.response.setContentType(mimeType);
        reportInfo.response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + fileName);

        byte[] jasperBytes = export(jasperPrint, exportType.exportTypeXlsx);
        FileCopyUtils.copy(jasperBytes, reportInfo.response.getOutputStream());

        return 0;
    }

    public static int exportDocx(ReportInfo reportInfo) throws JRException, IOException {
        if (reportInfo.inputStream == null)
            return -1;

        JasperPrint jasperPrint = getReport(reportInfo); // prepare report - passs parameters and jdbc connection

        String fileName = "report.docx";
        String mimeType = ru.sgp.utils.Sgp.getMediaType(ru.sgp.utils.Sgp.getFileType(fileName)).toString();
        reportInfo.response.setContentType(mimeType);
        reportInfo.response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + fileName);

        byte[] jasperBytes = export(jasperPrint, exportType.exportTypeDocx);
        FileCopyUtils.copy(jasperBytes, reportInfo.response.getOutputStream());

        return 0;
    }

    public static int exportPdf(ReportInfo reportInfo) throws IOException, JRException {
        if (reportInfo.inputStream == null)
            return -1;

        JasperPrint jasperPrint = getReport(reportInfo);
        if (jasperPrint == null)
            return -2;

        String fileName = "report.pdf";
        String mimeType = ru.sgp.utils.Sgp.getMediaType(ru.sgp.utils.Sgp.getFileType(fileName)).toString();
        reportInfo.response.setContentType(mimeType);
        reportInfo.response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + fileName);

        byte[] jasperBytes = export(jasperPrint, exportType.exportTypePdf);
        FileCopyUtils.copy(jasperBytes, reportInfo.response.getOutputStream());

        return 0;
    }

    public static int savePdf(ReportInfo reportInfo) {
        try {
            log.debug("JasperLib.savePdf {}", reportInfo.reportFile);
            JasperPrint jasperPrint = getReport(reportInfo);
            if (jasperPrint == null)
                return -2;

            byte[] jasperBytes = export(jasperPrint, exportType.exportTypePdf);
            log.debug("JasperLib.savePdf сохранение {}", reportInfo.outputFile.getPath());
            OutputStream outStream = new FileOutputStream(reportInfo.outputFile);
            outStream.write(jasperBytes);
            IOUtils.closeQuietly(outStream);

        } catch (JRException | IOException e) {
            log.error("Ошибка сохранения отчета PDF", e);
            return -1;
        }
        return 0;
    }

    public static int exportReport(ReportInfo reportInfo) throws IOException {

        String reportPath = reportInfo.reportFile;

        try {
            log.debug("JasperLib.exportReport {}", reportInfo.reportFile);
            if (!reportInfo.reportFile.contains(extJrxml) && !reportInfo.reportFile.contains(extJasper)) {
                reportPath = reportInfo.reportFile + extJasper;
                reportInfo.inputStream = JRLoader.getResourceInputStream(reportPath);
                if (reportInfo.inputStream == null) {
                    reportPath = reportInfo.reportFile + extJrxml;
                    reportInfo.inputStream = JRLoader.getResourceInputStream(reportPath);
                }
                reportInfo.reportFile = reportPath;
            } else
                reportInfo.inputStream = JRLoader.getResourceInputStream(reportPath);
        } catch (Exception ex) {
            return -1;
        }

        try {
            String format = reportInfo.getOrDefault("format", "pdf").toString().toLowerCase();
            switch (format.toLowerCase()) {
                case "xlsx":
                    return exportXlsx(reportInfo);
                case "docx":
                    return exportDocx(reportInfo);
                case "html":
                    return exportHtml(reportInfo);
                default:
                case "pdf":
                    return reportInfo.preview ? savePdf(reportInfo) : exportPdf(reportInfo);
            }
        } catch (Exception ex) {
            log.error("Ошибка экспорта Jasper Report " + reportPath + "\n", ex);
            return -1;
        }
    }
}
