package com.quantumledger.app.health;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class PingController {

    private final JdbcTemplate jdbcTemplate;

    public PingController(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/ping")
    public Map<String, String> ping(){
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        String db = (result != null && result == 1) ? "up" : "down";
        return Map.of("service","quantum ledger","database", db);
    }
}
