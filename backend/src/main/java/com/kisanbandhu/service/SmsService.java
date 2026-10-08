package com.kisanbandhu.service;

import com.kisanbandhu.domain.SmsOutbox;
import com.kisanbandhu.repo.SmsRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SmsService {

    private static final Logger log = LoggerFactory.getLogger(SmsService.class);

    private final SmsRepo smsRepo;
    private final String from;
    private final String provider;

    public SmsService(SmsRepo smsRepo,
                      @Value("${kisan.sms.provider}") String provider,
                      @Value("${kisan.sms.from}") String from) {
        this.smsRepo = smsRepo;
        this.provider = provider;
        this.from = from;
    }

    public SmsOutbox send(String toPhone, String body) {
        SmsOutbox msg = new SmsOutbox();
        msg.setToPhone(toPhone);
        msg.setBody(body);
        msg.setChannel(SmsOutbox.Channel.SMS);
        try {
            deliver(toPhone, body);
            msg.setStatus("SENT");
        } catch (Exception e) {
            log.warn("SMS delivery failed to {}: {}", toPhone, e.getMessage());
            msg.setStatus("FAILED");
        }
        return smsRepo.save(msg);
    }

    private void deliver(String toPhone, String body) {
        if ("mock".equals(provider)) {
            log.info("[MOCK SMS via {}] to={} body={}", from, toPhone, body);
            return;
        }
        throw new IllegalStateException("SMS provider '" + provider + "' not configured. Plug in MSG91/Twilio here.");
    }

    public List<SmsOutbox> recent() {
        return smsRepo.findTop50ByOrderByCreatedAtDesc();
    }
}
