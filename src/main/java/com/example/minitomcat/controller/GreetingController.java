package com.example.minitomcat.controller;

import com.example.minitomcat.annotation.Controller;
import com.example.minitomcat.annotation.RequestMapping;

@Controller
public class GreetingController {
    @RequestMapping("/controller/hello")
    public String hello() {
        return "Hello World";
    }
}
