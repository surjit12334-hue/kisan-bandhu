package com.kisanbandhu.config;

import com.kisanbandhu.domain.*;
import com.kisanbandhu.repo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;

@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    CommandLineRunner seed(UserRepo users, MandiRepo mandis, WeighbridgeRepo bridges,
                           MarketPriceRepo prices, PasswordEncoder encoder) {
        return args -> {
            if (users.count() > 0) return;

            User admin = new User();
            admin.setName("Mandi Administrator");
            admin.setPhone("9999999999");
            admin.setPasswordHash(encoder.encode("admin123"));
            admin.setRole(Role.ADMIN);
            users.save(admin);

            User demoFarmer = new User();
            demoFarmer.setName("Ramesh Patidar");
            demoFarmer.setPhone("9876543210");
            demoFarmer.setPasswordHash(encoder.encode("farmer123"));
            demoFarmer.setRole(Role.FARMER);
            demoFarmer.setLanguage("hi");
            users.save(demoFarmer);

            Mandi indore = mandi("IND-01", "Indore APMC Mandi", "Indore", "Madhya Pradesh", "1800-233-4455");
            Mandi bhopal = mandi("BPL-01", "Bhopal Kisan Mandi", "Bhopal", "Madhya Pradesh", "1800-233-4466");
            mandis.save(indore);
            mandis.save(bhopal);

            for (int i = 1; i <= 3; i++) {
                Weighbridge b = new Weighbridge();
                b.setMandi(indore);
                b.setCode("WB-" + i);
                bridges.save(b);
            }
            Weighbridge b4 = new Weighbridge();
            b4.setMandi(bhopal);
            b4.setCode("WB-1");
            bridges.save(b4);

            seedPrices(prices);
            log.info("Seeded demo data: admin/9999999999/admin123, farmer/9876543210/farmer123, 2 mandis, 4 weighbridges");
        };
    }

    private Mandi mandi(String code, String name, String district, String state, String tollFree) {
        Mandi m = new Mandi();
        m.setCode(code);
        m.setName(name);
        m.setDistrict(district);
        m.setState(state);
        m.setTollFreeNumber(tollFree);
        return m;
    }

    private void seedPrices(MarketPriceRepo prices) {
        LocalDate today = LocalDate.now();
        record P(String commodity, double price) {}
        List<P> list = List.of(
                new P("गेहूँ / Wheat", 2450), new P("सोयाबीन / Soybean", 4620),
                new P("मक्का / Maize", 2180), new P("चना / Gram", 5350),
                new P("प्याज़ / Onion", 1750), new P("आलू / Potato", 1420),
                new P("सरसों / Mustard", 5680), new P("धान / Paddy", 2310));
        for (P p : list) {
            MarketPrice mp = new MarketPrice();
            mp.setCommodity(p.commodity());
            mp.setMandiName("Indore APMC Mandi");
            mp.setState("Madhya Pradesh");
            mp.setPrice(p.price());
            mp.setPriceDate(today);
            mp.setSource("AGMARKNET");
            prices.save(mp);
        }
    }
}
