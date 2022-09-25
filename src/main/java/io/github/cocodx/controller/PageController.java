package io.github.cocodx.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @author amazfit
 * @date 2022-09-25 下午10:36
 **/
@Controller
@RequestMapping("/page")
public class PageController {

    @RequestMapping("/index")
    public String index(){
        return "index";
    }
}
