package ru.sgp.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

@Slf4j
@Component
@PropertySource("classpath:mail.properties")
public class MailHelper {
    @Value("${mail.smtp.host:sco1-msg-01.sgp.ru}")
    private String host;
    @Value("${mail.smtp.port:587}")
    private String port;
    @Value("${mail.smtp.user:sgp\\MOSS_mail}")
    private String user;
    @Value("${mail.smtp.password:MOSSmail2014}")
    private String password;
    @Value("${mail.smtp.auth:true}")
    private boolean auth;
    @Value("${mail.smtp.ehlo:true}")
    private boolean ehlo;
    @Value("${subject.charset:UTF-8}")
    private String subjectCharset;
    @Value("${body.type:text/html; charset=UTF-8}")
    private String bodyType;
    @Value("${mail.debug:true}")
    private boolean debug;
    private Session session;

    //
    public boolean close() {
        return true;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public void setAuth(boolean auth) {
        this.auth = auth;
    }

    public void setEhlo(boolean ehlo) {
        this.ehlo = ehlo;
    }

    public Session getSession() {
        if (session == null)
            configureSession();
        return session;
    }

    private Properties getProperties() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", auth);
        props.put("mail.smtp.starttls.enable", true);
        props.put("mail.smtp.ssl.trust", host);
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.ehlo", ehlo); // во избежания: No authentication mechanisms supported by both server and client
        props.put("mail.debug", debug);
        return props;
    }

    public Session configureSession() {
        session = Session.getInstance(getProperties(), new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return auth ? new PasswordAuthentication(user, password) : null;
            }
        });
        session.setDebug(debug);
        return session;
    }

    private void setHeaderEncode(MimeMessage message, String name, String value, String suffix)
            throws UnsupportedEncodingException, MessagingException {
        String header = MimeUtility.fold(9, MimeUtility.encodeText(value, "UTF-8", "B"));
        if (suffix != null && !suffix.equals(""))
            header += ' ' + suffix;
        message.setHeader(name, header);
    }

    public String sendMail(String addressesTo, final String subject, String body) {
        Transport transport = null;
        try {
            MimeMessage message = new MimeMessage(getSession());
            // эти функции не кодируют русские буквы достаточно правильно, заменяем на setHeaderEncode
            Address[] addresses = InternetAddress.parse(addressesTo);
            message.setRecipients(Message.RecipientType.TO, addresses);
            setHeaderEncode(message, "From", "ИС Полет", "<flight@surgut.gazprom.ru>");
            setHeaderEncode(message, "To", "addressToTitle вав", '<' + addressesTo + '>');
            message.setSubject(subject, subjectCharset);
            message.setContent(body, bodyType);
            transport = session.getTransport("smtp");
            transport.connect();
            message.saveChanges();
            transport.sendMessage(message, message.getAllRecipients());
            return null; // нет ошибок
        } catch (AddressException ex) {
            log.error("Ошибка отправки почты, адрес", ex);
            return ex.getMessage();
        } catch (UnsupportedEncodingException ex) {
            log.error("Ошибка отправки почты, кодировка", ex);
            return ex.getMessage();
        } catch (MessagingException ex) {
            log.error("Ошибка отправки почты, транспорт addressesTo='{}'", addressesTo, ex);
            String error = ex.getMessage();
            if (ex.getNextException() != null)
                error += '\n' + ex.getNextException().getMessage();
            return error;
        } finally {
            try {
                if (transport != null)
                    transport.close();
            } catch (MessagingException ignored) {
            }
        }
    }
}