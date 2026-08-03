package com.example.minitomcat;

import com.example.minitomcat.server.HttpServer;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class Main {
    public static void main(String[] args) {
        HttpServer httpServer = new HttpServer(18080);

        httpServer.start();


    }
}
