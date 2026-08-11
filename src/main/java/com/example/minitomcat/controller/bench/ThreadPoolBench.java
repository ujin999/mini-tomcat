package com.example.minitomcat.controller.bench;

import com.example.minitomcat.annotation.Controller;
import com.example.minitomcat.annotation.RequestMapping;

@Controller
public class ThreadPoolBench {
    @RequestMapping("/bench")
    public String bench() throws InterruptedException {
        Thread.sleep(100);
        return "done";
    }
}
