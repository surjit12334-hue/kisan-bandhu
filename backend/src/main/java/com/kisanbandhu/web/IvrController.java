package com.kisanbandhu.web;

import com.kisanbandhu.service.IvrService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ivr")
public class IvrController {

    private final IvrService ivrService;

    public IvrController(IvrService ivrService) {
        this.ivrService = ivrService;
    }

    @PostMapping("/incoming")
    public IvrService.Response incoming(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        if (phone == null || phone.isBlank()) phone = "0000000000";
        String node = body.get("node");
        String digits = body.get("digits");
        return ivrService.handle(phone, node, digits);
    }

    @GetMapping("/config")
    public Map<String, String> config() {
        return Map.of("tollFree", "1800-233-4455", "provider", "pluggable (Exotel/Twilio/Knowlarity)");
    }
}
