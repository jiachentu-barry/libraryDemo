package com.example.demo5.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String rootToLogin() {
        return "redirect:/login.html";
    }
}