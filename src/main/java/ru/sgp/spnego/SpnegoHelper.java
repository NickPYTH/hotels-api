package ru.sgp.spnego;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.DirContextSource;
import org.springframework.stereotype.Component;
import ru.sgp.dto.UserDTO;

@Slf4j
@Component
public class SpnegoHelper {

    public static final String sgpPrefix = "sgp\\";
    // логин пользователя при отладке, требуется редеплой
    private static final String debugUserName = "SlabouzovKV";//""BelyaevMA";KurbarovDY

    public static String findUsernameByTabnum(Integer tabNumber) {
        try {
            DirContextSource contextSource = new DirContextSource();
            contextSource.setUrl("ldap://sco1-dc-01");
            contextSource.setUserDn("srv_app_service@SGP.RU");
            contextSource.setPassword("LEcF[keR1*Z8fKC56ISbn5xV");

            LdapTemplate template = new LdapTemplate(contextSource);
            template.setIgnorePartialResultException(true);
            contextSource.afterPropertiesSet();
            template.afterPropertiesSet();

            final String filter = "employeeNumber=" + tabNumber;
            UserDTO userDTO = new UserDTO();
            template.search("dc=sgp,dc=ru", filter,
                    (ContextMapper) arg0 -> {
                        DirContextAdapter ctx = (DirContextAdapter) arg0;
                        userDTO.setUsername(ctx.getStringAttribute("mail"));
                        return ctx.getNameInNamespace();
                    });
            return userDTO.getUsername();
        } catch (Exception e) {
            // что то пошло не так...
            log.error("findFromAd template.afterPropertiesSet {}", "userName", e);
            return "error";
        }
    }
}
