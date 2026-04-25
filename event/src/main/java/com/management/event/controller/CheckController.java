package com.management.event.controller;


import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/check")
public class CheckController {

    @RequestMapping()
    public String healthCheck() {
        return "OK This means your coockie works fine";
    }
}
