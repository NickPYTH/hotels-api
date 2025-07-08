package ru.sgp.utils;

import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignTextField;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.InputStream;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

public class ReportInfo {
    public String reportFile;
    public File outputFile;
    public Map<String, Object> parameters;
    public Connection connection;
    public HttpServletResponse response;
    public InputStream inputStream;
    public boolean preview = false;

    public ReportInfo(boolean preview, String reportFile, File outputFile) {
        this.preview = preview;
        this.reportFile = reportFile;
        this.outputFile = outputFile;
    }

    public void addParameter(String key, Object value) {
        if (parameters == null)
            parameters = new HashMap<>();

        parameters.put(key, value);
    }

    public Object getOrDefault(String key, String defaultValue) {
        return parameters.getOrDefault(key, defaultValue);
    }

    public static JRDesignTextField createTextField(
            String expressionStr, int x, int y,
            int width, int height,
            float fontSize,
            float borderSize,
            boolean isBold
            )
    {
        JRDesignTextField textField = new JRDesignTextField();
        JRDesignExpression expression = new JRDesignExpression();
        expression.setText(expressionStr);
        textField.setExpression(expression);
        textField.getLineBox().getLeftPen().setLineWidth(borderSize);
        textField.getLineBox().getRightPen().setLineWidth(borderSize);
        textField.getLineBox().getTopPen().setLineWidth(borderSize);
        textField.getLineBox().getBottomPen().setLineWidth(borderSize);
        textField.setX(x);
        textField.setY(y);
        textField.setWidth(width);
        textField.setHeight(height);
        textField.setFontSize(fontSize);
        textField.setBold(isBold);

        return textField;
    }

}
