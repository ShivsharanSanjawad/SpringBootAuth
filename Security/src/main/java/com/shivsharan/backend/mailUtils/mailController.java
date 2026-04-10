package com.shivsharan.backend.mailUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api")
public class mailController {

    @Autowired
    public mailService mailService;

    @GetMapping("/trySending")
    public ResponseEntity<?> send() {
        if (mailService.sendOTP("shivsharansanjawad@gmail.com", 1234)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.noContent().build();
    }
}
