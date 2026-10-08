package com.kisanbandhu;

import com.kisanbandhu.repo.UserRepo;
import com.kisanbandhu.service.IvrService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:kbtest;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class IvrServiceTests {

    @Autowired IvrService ivrService;
    @Autowired UserRepo userRepo;

    @Test
    void statusForUnknownCallerAutoProvisionsInsteadOfThrowing() {
        String phone = "9000000096";
        IvrService.Response r = ivrService.handle(phone, "menu_hi", "2");
        assertEquals("done", r.node(), r.prompt());
        assertTrue(userRepo.findByPhone(phone).isPresent(), "unknown caller should be auto-provisioned");
    }

    @Test
    void shortPhoneDoesNotCrashWaitlistPath() {
        IvrService.Response r = ivrService.handle("123", "menu", "4");
        assertEquals("done", r.node(), r.prompt());
    }

    @Test
    void nullPhoneDoesNotCrash() {
        IvrService.Response r = ivrService.handle(null, "menu", "2");
        assertEquals("done", r.node());
    }

    @Test
    void welcomeRepromptsOnUnknownDigit() {
        IvrService.Response r = ivrService.handle("9000000095", "welcome", "5");
        assertEquals("welcome", r.node(), "invalid language key should re-prompt, not switch language");
    }

    @Test
    void ivrProvisionedUserGetsWellFormedPasswordHash() {
        String phone = "9000000094";
        ivrService.handle(phone, "menu", "4");
        var user = userRepo.findByPhone(phone).orElseThrow();
        assertNotNull(user.getPasswordHash());
        assertTrue(user.getPasswordHash().startsWith("$2a$"), "hash must be a real bcrypt encoding");
        assertEquals(60, user.getPasswordHash().length());
    }
}
