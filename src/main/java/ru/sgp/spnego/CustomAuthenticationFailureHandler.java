package ru.sgp.spnego;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * перехватчик ошибок при авторизации
 * просто редиректим на страницу регистрации с признаком ошибки
 */
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) throws IOException, ServletException {

        String errorMessage = ex.getMessage();
        System.err.println("Ошибка регистрации в домене\n" + errorMessage);

        //if (errorMessage.equals("Kerberos authentication failed"))
        //    errorMessage = "Ошибка регистрации, повторите попытку";
        //response.sendRedirect("login.html?error=" + URLEncoder.encode(errorMessage, UTF_8.toString()));

        // возвращаем на страницу регистрации и передаем параметром признак ошибки
        response.sendRedirect("login.html?error=true");
    }
}
