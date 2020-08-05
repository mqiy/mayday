package com.songhaozhi.mayday.web.controller.front;

import com.songhaozhi.mayday.util.ConvertBz;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @Autowired
    private ConvertBz convertBz;

    @GetMapping("convert")
    public void convert(){
        convertBz.convert();
    }
}
